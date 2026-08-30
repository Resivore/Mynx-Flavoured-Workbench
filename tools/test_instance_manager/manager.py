#!/usr/bin/env python3
"""Fail-closed physical adapter for the V2 two-slot runtime state.

The pure state machine lives in :mod:`tools.runtime_slots`.  This module owns
only the dedicated profile's ``mods`` directory, a target-local lock/ledger,
and the repository runtime-state commit which follows a verified physical
transition.  Unrelated profile files are never part of a mutation plan.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
import re
import shutil
import stat
import tempfile
import uuid
import zipfile
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath, PureWindowsPath
from typing import Any, Callable, Iterable

try:
    from ..runtime_slots import (
        plan_transition,
        render_title_state,
        resolve_profile,
        state_digest,
        validate_runtime_state,
    )
    from ..workbench import ValidationError, load_repository_statuses
except ImportError:  # Direct execution from tools/test_instance_manager/.
    import sys

    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
    from runtime_slots import (  # type: ignore
        plan_transition,
        render_title_state,
        resolve_profile,
        state_digest,
        validate_runtime_state,
    )
    from workbench import ValidationError, load_repository_statuses  # type: ignore


DEFAULT_CONFIG = Path(__file__).with_name("config.json")
LEDGER_SCHEMA_V2 = "mynx-test-instance-manager-ledger-v2"
LEDGER_SCHEMA = "mynx-test-instance-manager-ledger-v3"
TITLE_PROJECTION_SCHEMA = "mynx-runtime-title-state-v1"
LEGACY_MARKER_NAME = ".workbench-instance-manager.json"
RETIRED_LEGACY_MARKER_NAME = ".workbench-instance-manager.v1-retired.json"
FailureInjector = Callable[[str], None]


class ManagerError(RuntimeError):
    """A physical preflight, ownership, serialization, or recovery failure."""


@dataclass(frozen=True)
class MarkerArtifactConfig:
    filename: str
    sha256: str
    mod_id: str
    source: str | None = None

    @classmethod
    def load(cls, value: Any, label: str, *, source_required: bool) -> "MarkerArtifactConfig":
        required = {"filename", "sha256", "mod_id"}
        if source_required:
            required.add("source")
        if not isinstance(value, dict) or set(value) != required:
            raise ManagerError(f"{label} must contain exactly {sorted(required)}")
        filename = _safe_basename(value["filename"], f"{label}.filename")
        if not filename.casefold().endswith(".jar"):
            raise ManagerError(f"{label}.filename must end in .jar")
        sha256 = value["sha256"]
        if not isinstance(sha256, str) or not re.fullmatch(r"[0-9a-fA-F]{64}", sha256):
            raise ManagerError(f"{label}.sha256 must be a SHA-256 hex digest")
        mod_id = value["mod_id"]
        if not isinstance(mod_id, str) or not re.fullmatch(r"[a-z][a-z0-9_.-]*", mod_id):
            raise ManagerError(f"{label}.mod_id must be a lowercase Fabric mod id")
        source = None
        if source_required:
            source = _safe_relative(value["source"], f"{label}.source", allow_nested=True)
            source_parts = PurePosixPath(source.replace("\\", "/")).parts
            if source_parts[0].casefold() == "originals" or source_parts[-1].casefold() != filename.casefold():
                raise ManagerError(f"{label}.source must be a repository path ending in {filename}")
        return cls(filename=filename, sha256=sha256.lower(), mod_id=mod_id, source=source)


@dataclass(frozen=True)
class TitleDisplayConfig:
    projection_file: str
    marker: MarkerArtifactConfig
    predecessors: tuple[MarkerArtifactConfig, ...]

    @classmethod
    def load(cls, value: Any) -> "TitleDisplayConfig":
        if not isinstance(value, dict) or set(value) != {"projection_file", "marker", "predecessors"}:
            raise ManagerError("title_display must contain exactly marker, predecessors, and projection_file")
        projection_file = _safe_basename(value["projection_file"], "title_display.projection_file")
        if projection_file in {LEGACY_MARKER_NAME, RETIRED_LEGACY_MARKER_NAME}:
            raise ManagerError("title display projection cannot reuse a legacy V1 marker filename")
        marker = MarkerArtifactConfig.load(value["marker"], "title_display.marker", source_required=True)
        raw_predecessors = value["predecessors"]
        if not isinstance(raw_predecessors, list):
            raise ManagerError("title_display.predecessors must be an array")
        predecessors = tuple(
            MarkerArtifactConfig.load(item, f"title_display.predecessors[{index}]", source_required=False)
            for index, item in enumerate(raw_predecessors)
        )
        names = [marker.filename.casefold(), *(item.filename.casefold() for item in predecessors)]
        if len(names) != len(set(names)):
            raise ManagerError("title marker and predecessor filenames must be unique")
        if any(item.mod_id != marker.mod_id for item in predecessors):
            raise ManagerError("all title marker predecessors must preserve the current marker mod id")
        return cls(projection_file=projection_file, marker=marker, predecessors=predecessors)


@dataclass(frozen=True)
class ManagerConfig:
    repository_root: Path
    runtime_state: Path
    dedicated_profile: Path
    protected_profile: Path
    mods_directory: str
    ledger_file: str
    lock_file: str
    title_display: TitleDisplayConfig

    @classmethod
    def load(cls, path: Path) -> "ManagerConfig":
        path = path.resolve()
        try:
            raw = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            raise ManagerError(f"cannot load manager config {path}: {exc}") from exc
        required = {
            "schema_version",
            "repository_root",
            "runtime_state",
            "dedicated_profile",
            "protected_profile",
            "mods_directory",
            "ledger_file",
            "lock_file",
            "title_display",
        }
        if not isinstance(raw, dict) or set(raw) != required:
            raise ManagerError(f"manager config must contain exactly {sorted(required)}")
        if raw["schema_version"] != 3:
            raise ManagerError("manager config schema_version must equal 3")

        repository_root = _configured_path(path.parent, raw["repository_root"], "repository_root")
        runtime_state = _contained_configured_path(repository_root, raw["runtime_state"], "runtime_state")
        dedicated_profile = _absolute_configured_path(raw["dedicated_profile"], "dedicated_profile")
        protected_profile = _absolute_configured_path(raw["protected_profile"], "protected_profile")
        if _paths_equal(dedicated_profile, protected_profile) or _is_within(dedicated_profile, protected_profile) or _is_within(
            protected_profile, dedicated_profile
        ):
            raise ManagerError("dedicated and protected profiles must be disjoint")

        mods_directory = _safe_relative(raw["mods_directory"], "mods_directory", allow_nested=False)
        ledger_file = _safe_basename(raw["ledger_file"], "ledger_file")
        lock_file = _safe_basename(raw["lock_file"], "lock_file")
        title_display = TitleDisplayConfig.load(raw["title_display"])
        if ledger_file.casefold() == lock_file.casefold():
            raise ManagerError("ledger_file and lock_file must be distinct")
        if title_display.projection_file.casefold() in {ledger_file.casefold(), lock_file.casefold()}:
            raise ManagerError("title display projection, ledger, and lock filenames must be distinct")
        if _is_within(runtime_state, repository_root / "originals"):
            raise ManagerError("runtime_state cannot be under originals/")
        if _paths_equal(runtime_state, protected_profile) or _is_within(runtime_state, protected_profile):
            raise ManagerError("runtime_state cannot resolve into the protected gameplay profile")
        return cls(
            repository_root=repository_root,
            runtime_state=runtime_state,
            dedicated_profile=dedicated_profile,
            protected_profile=protected_profile,
            mods_directory=mods_directory,
            ledger_file=ledger_file,
            lock_file=lock_file,
            title_display=title_display,
        )


@dataclass(frozen=True)
class ManagedArtifact:
    deployment_id: str
    artifact_id: str
    project_id: str
    filename: str
    sha256: str
    mod_id: str
    relative_path: str
    active: bool
    source: dict[str, str]

    def ledger_record(self) -> dict[str, Any]:
        return {
            "deployment_id": self.deployment_id,
            "artifact_id": self.artifact_id,
            "project_id": self.project_id,
            "path": self.relative_path,
            "sha256": self.sha256,
            "mod_id": self.mod_id,
            "active": self.active,
        }


@dataclass(frozen=True)
class FileAction:
    destination: Path
    relative_path: str
    artifact: ManagedArtifact
    source: Path


@dataclass(frozen=True)
class PhysicalPlan:
    mode: str
    current_state: dict[str, Any] | None
    desired_state: dict[str, Any]
    current_artifacts: tuple[ManagedArtifact, ...]
    desired_artifacts: tuple[ManagedArtifact, ...]
    writes: tuple[FileAction, ...]
    removals: tuple[Path, ...]
    unchanged: tuple[str, ...]
    title_projection: dict[str, Any]

    def summary(self, *, dry_run: bool) -> dict[str, Any]:
        return {
            "mode": self.mode,
            "dry_run": dry_run,
            "target_state_revision": self.desired_state["revision"],
            "target_state_digest": state_digest(self.desired_state),
            "activation": self.desired_state["activation"],
            "writes": [
                {
                    "path": action.relative_path,
                    "sha256": action.artifact.sha256,
                    "source": str(action.source),
                }
                for action in self.writes
            ],
            "removals": [path.name for path in self.removals],
            "unchanged": list(self.unchanged),
            "managed_files": [artifact.ledger_record() for artifact in self.desired_artifacts],
            "title_projection": copy.deepcopy(self.title_projection),
        }


class _ExclusiveTargetLock:
    def __init__(self, path: Path):
        self.path = path
        self.token = f"{os.getpid()}:{uuid.uuid4()}"
        self.acquired = False

    def __enter__(self) -> "_ExclusiveTargetLock":
        try:
            descriptor = os.open(self.path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
            with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as handle:
                handle.write(self.token + "\n")
                handle.flush()
                os.fsync(handle.fileno())
        except FileExistsError as exc:
            raise ManagerError(f"physical target is locked: {self.path}") from exc
        except OSError as exc:
            raise ManagerError(f"cannot acquire physical target lock {self.path}: {exc}") from exc
        self.acquired = True
        return self

    def __exit__(self, exc_type: Any, exc: Any, traceback: Any) -> None:
        if not self.acquired:
            return
        try:
            if self.path.read_text(encoding="utf-8").strip() == self.token:
                self.path.unlink()
        except OSError:
            # Never delete a lock which can no longer be proved to be ours.
            pass


class PhysicalManager:
    """Serialized filesystem adapter for one exactly configured profile."""

    def __init__(
        self,
        config: ManagerConfig,
        target: Path | None = None,
        project_index: dict[str, str] | None = None,
        project_display_names: dict[str, str] | None = None,
    ):
        self.config = config
        requested = config.dedicated_profile if target is None else Path(target)
        requested = Path(os.path.abspath(requested))
        _assert_no_reparse_components(requested, "configured dedicated profile")
        self.target = requested.resolve(strict=False)
        self._assert_safe_target()
        raw_mods = self.target / config.mods_directory
        raw_ledger = self.target / config.ledger_file
        raw_lock = self.target / config.lock_file
        raw_legacy_marker = self.target / LEGACY_MARKER_NAME
        raw_retired_legacy_marker = self.target / RETIRED_LEGACY_MARKER_NAME
        raw_title_projection = self.target / config.title_display.projection_file
        _assert_no_reparse_components(raw_mods, "mods directory", root=self.target)
        _assert_no_reparse_components(raw_ledger, "target-local ledger", root=self.target)
        _assert_no_reparse_components(raw_lock, "target-local lock", root=self.target)
        _assert_no_reparse_components(raw_legacy_marker, "legacy V1 marker", root=self.target)
        _assert_no_reparse_components(raw_retired_legacy_marker, "retired legacy V1 marker", root=self.target)
        _assert_no_reparse_components(raw_title_projection, "V2 title display projection", root=self.target)
        self.mods = raw_mods.resolve(strict=False)
        self.ledger_path = raw_ledger.resolve(strict=False)
        self.lock_path = raw_lock.resolve(strict=False)
        self.legacy_marker_path = raw_legacy_marker.resolve(strict=False)
        self.retired_legacy_marker_path = raw_retired_legacy_marker.resolve(strict=False)
        self.title_projection_path = raw_title_projection.resolve(strict=False)
        for path, label in (
            (self.mods, "mods directory"),
            (self.ledger_path, "ledger"),
            (self.lock_path, "lock"),
            (self.legacy_marker_path, "legacy V1 marker"),
            (self.retired_legacy_marker_path, "retired legacy V1 marker"),
            (self.title_projection_path, "V2 title display projection"),
        ):
            self._assert_target_containment(path, label)
        statuses: dict[str, tuple[Path, dict[str, Any]]] | None = None
        if project_index is None or project_display_names is None:
            try:
                statuses = load_repository_statuses(self.config.repository_root)
            except ValidationError as exc:
                if project_index is None:
                    raise ManagerError(f"cannot load repository project identities: {exc}") from exc
        if project_index is None:
            project_index = {
                project_uuid: manifest["identity"]["project_id"]
                for project_uuid, (_, manifest) in (statuses or {}).items()
            }
        if project_display_names is None:
            if statuses is not None:
                project_display_names = {
                    project_uuid: manifest["identity"]["name"]
                    for project_uuid, (_, manifest) in statuses.items()
                }
            else:
                project_display_names = copy.deepcopy(project_index)
        self.project_index = copy.deepcopy(project_index)
        self.project_display_names = copy.deepcopy(project_display_names)

    @classmethod
    def from_config(
        cls,
        config_path: Path = DEFAULT_CONFIG,
        target: Path | None = None,
        project_index: dict[str, str] | None = None,
        project_display_names: dict[str, str] | None = None,
    ) -> "PhysicalManager":
        return cls(
            ManagerConfig.load(Path(config_path)),
            target=target,
            project_index=project_index,
            project_display_names=project_display_names,
        )

    def _assert_safe_target(self) -> None:
        protected = self.config.protected_profile.resolve(strict=False)
        if _paths_equal(self.target, protected) or _is_within(self.target, protected) or _is_within(protected, self.target):
            raise ManagerError(f"protected gameplay profile is permanently off limits: {protected}")
        if not _paths_equal(self.target, self.config.dedicated_profile.resolve(strict=False)):
            raise ManagerError(
                f"target must resolve exactly to configured dedicated profile {self.config.dedicated_profile.resolve(strict=False)}"
            )
        if not self.target.is_dir():
            raise ManagerError(f"configured dedicated profile does not exist or is not a directory: {self.target}")

    def _assert_target_containment(self, path: Path, label: str) -> None:
        protected = self.config.protected_profile.resolve(strict=False)
        if not _is_within(path, self.target):
            raise ManagerError(f"{label} escapes the dedicated profile: {path}")
        if _paths_equal(path, protected) or _is_within(path, protected):
            raise ManagerError(f"{label} resolves into protected gameplay profile: {path}")

    def load_repository_state(self) -> dict[str, Any]:
        try:
            state = json.loads(self.config.runtime_state.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            raise ManagerError(f"cannot load runtime state {self.config.runtime_state}: {exc}") from exc
        try:
            return validate_runtime_state(state, self.project_index)
        except ValidationError as exc:
            raise ManagerError(f"invalid runtime state: {exc}") from exc

    def _assert_no_transaction_residue(self) -> None:
        try:
            residues = sorted(
                (
                    path
                    for path in self.target.iterdir()
                    if path.name.startswith(".mynx-runtime-v2-transaction-")
                ),
                key=lambda path: path.name.casefold(),
            )
        except OSError as exc:
            raise ManagerError(f"cannot inspect dedicated target for transaction residue: {exc}") from exc
        if residues:
            names = ", ".join(path.name for path in residues)
            raise ManagerError(
                "unfinished V2 transaction residue requires explicit inspected recovery before any operation: " + names
            )

    def _assert_no_legacy_marker(self) -> None:
        _assert_no_reparse_components(self.legacy_marker_path, "legacy V1 marker", root=self.target)
        if self.legacy_marker_path.exists():
            raise ManagerError(
                f"legacy V1 display marker must be retired before verification or transition: {self.legacy_marker_path}"
            )

    def derive_inventory(self, state: dict[str, Any]) -> tuple[ManagedArtifact, ...]:
        """Resolve runtime-state mods and canonical disabled replacements."""

        try:
            validate_runtime_state(state, self.project_index)
            active_units = resolve_profile(state, self.project_index)
        except ValidationError as exc:
            raise ManagerError(f"invalid runtime state: {exc}") from exc

        active_deployments = {unit["deployment_id"] for unit in active_units}
        artifacts: list[ManagedArtifact] = []
        seen_paths: set[str] = set()

        for member in state["accepted_baseline"]["members"]:
            unit = member["unit"]
            active = unit["deployment_id"] in active_deployments
            artifacts.extend(self._unit_artifacts(unit, active, seen_paths))
        for label in ("A", "B"):
            slot = state["slots"][label]
            if slot is not None:
                artifacts.extend(self._unit_artifacts(slot["unit"], True, seen_paths))

        return tuple(artifacts)

    def _marker_artifact(self, marker: MarkerArtifactConfig) -> ManagedArtifact:
        identity = f"{marker.filename}:{marker.sha256}"
        source = (
            {"type": "REPOSITORY", "path": marker.source}
            if marker.source is not None
            else {"type": "ADOPTED_TARGET", "path": f"{self.config.mods_directory}/{marker.filename}"}
        )
        return ManagedArtifact(
            deployment_id=str(uuid.uuid5(uuid.NAMESPACE_URL, "mynx-title-marker-deployment:" + identity)),
            artifact_id=str(uuid.uuid5(uuid.NAMESPACE_URL, "mynx-title-marker-artifact:" + identity)),
            project_id="workbench-test-marker",
            filename=marker.filename,
            sha256=marker.sha256,
            mod_id=marker.mod_id,
            relative_path=PurePosixPath(self.config.mods_directory, marker.filename).as_posix(),
            active=True,
            source=source,
        )

    def _desired_managed_inventory(self, state: dict[str, Any]) -> tuple[ManagedArtifact, ...]:
        return (*self.derive_inventory(state), self._marker_artifact(self.config.title_display.marker))

    def _legacy_managed_inventory(self, state: dict[str, Any]) -> tuple[ManagedArtifact, ...]:
        candidates = (
            self.config.title_display.marker,
            *self.config.title_display.predecessors,
        )
        present: list[ManagedArtifact] = []
        for candidate in candidates:
            artifact = self._marker_artifact(candidate)
            if self._destination(artifact).exists():
                self._verify_artifact_file(self._destination(artifact), artifact)
                present.append(artifact)
        if len(present) != 1:
            names = ", ".join(item.filename for item in present) or "none"
            raise ManagerError(
                "legacy V2 ledger requires exactly one configured title marker artifact; found " + names
            )
        return (*self.derive_inventory(state), present[0])

    def _managed_inventory_for_ledger(
        self,
        state: dict[str, Any],
        ledger: dict[str, Any],
    ) -> tuple[ManagedArtifact, ...]:
        if ledger["schema_version"] == 3:
            return self._desired_managed_inventory(state)
        return self._legacy_managed_inventory(state)

    def _render_title(self, state: dict[str, Any]) -> dict[str, Any]:
        try:
            return render_title_state(state, self.project_display_names, self.project_index)
        except ValidationError as exc:
            raise ManagerError(f"cannot render canonical title state: {exc}") from exc

    def _title_projection(self, state: dict[str, Any]) -> dict[str, Any]:
        title = self._render_title(state)
        return {
            "$schema": TITLE_PROJECTION_SCHEMA,
            "schema_version": 1,
            "state_revision": state["revision"],
            "state_digest": state_digest(state),
            "baseline": copy.deepcopy(title["baseline"]),
            "slots": copy.deepcopy(title["slots"]),
            "lines": copy.deepcopy(title["lines"]),
        }

    def _verify_title_projection(self, state: dict[str, Any]) -> dict[str, Any]:
        try:
            projection = json.loads(self.title_projection_path.read_text(encoding="utf-8"))
        except FileNotFoundError as exc:
            raise ManagerError(f"V2 title display projection is missing: {self.title_projection_path}") from exc
        except (OSError, json.JSONDecodeError) as exc:
            raise ManagerError(f"cannot load V2 title display projection {self.title_projection_path}: {exc}") from exc
        expected = self._title_projection(state)
        if projection != expected:
            raise ManagerError("V2 title display projection does not match canonical runtime state")
        return {
            "path": self.title_projection_path.name,
            "sha256": _sha256(self.title_projection_path),
            "state_revision": projection["state_revision"],
            "lines": copy.deepcopy(projection["lines"]),
        }

    def _unit_artifacts(self, unit: dict[str, Any], active: bool, seen_paths: set[str]) -> list[ManagedArtifact]:
        result: list[ManagedArtifact] = []
        for raw in unit["artifacts"]:
            if raw["kind"] != "MOD":
                raise ManagerError(
                    f"physical adapter is MOD-only; {unit['project_id']} artifact {raw['filename']} has kind {raw['kind']}"
                )
            filename = raw["filename"]
            if not filename.casefold().endswith(".jar"):
                raise ManagerError(f"managed MOD filename must end in .jar: {filename}")
            ownership = raw["ownership_keys"]
            if len(ownership) != 1 or not isinstance(ownership[0], str) or not ownership[0].startswith("mod:"):
                raise ManagerError(f"managed MOD {filename} must have exactly one mod:<fabric_mod_id> ownership key")
            mod_id = ownership[0][4:]
            if not mod_id:
                raise ManagerError(f"managed MOD {filename} has an empty Fabric mod id")
            source = raw.get("source")
            if not isinstance(source, dict) or set(source) != {"type", "path"}:
                raise ManagerError(f"managed MOD {filename} requires exactly source.type and source.path")
            if source["type"] not in {"REPOSITORY", "ADOPTED_TARGET"} or not isinstance(source["path"], str):
                raise ManagerError(f"managed MOD {filename} has an invalid source descriptor")
            relative_path = PurePosixPath(self.config.mods_directory, filename + ("" if active else ".disabled")).as_posix()
            normalized = relative_path.casefold()
            if normalized in seen_paths:
                raise ManagerError(f"managed physical path collision: {relative_path}")
            seen_paths.add(normalized)
            result.append(
                ManagedArtifact(
                    deployment_id=unit["deployment_id"],
                    artifact_id=raw["artifact_id"],
                    project_id=unit["project_id"],
                    filename=filename,
                    sha256=raw["sha256"].lower(),
                    mod_id=mod_id,
                    relative_path=relative_path,
                    active=active,
                    source={"type": source["type"], "path": source["path"]},
                )
            )
        return result

    def _destination(self, artifact: ManagedArtifact) -> Path:
        destination = self.target / Path(artifact.relative_path)
        _assert_no_reparse_components(
            destination,
            f"managed artifact destination {artifact.relative_path}",
            root=self.target,
        )
        resolved = destination.resolve(strict=False)
        self._assert_target_containment(resolved, f"artifact destination {artifact.relative_path}")
        if resolved.parent != self.mods or resolved.name.casefold() != destination.name.casefold():
            raise ManagerError(f"managed artifact destination must be directly under {self.mods}: {destination}")
        return destination

    def _source_path(self, artifact: ManagedArtifact) -> Path:
        descriptor = artifact.source
        label = f"source for {artifact.project_id}/{artifact.filename}"
        if descriptor["type"] == "REPOSITORY":
            relative = _safe_relative(descriptor["path"], label, allow_nested=True)
            if PurePosixPath(relative.replace("\\", "/")).parts[0].casefold() == "originals":
                raise ManagerError(f"{label} cannot use originals/")
            source = (self.config.repository_root / Path(relative)).resolve(strict=False)
            if not _is_within(source, self.config.repository_root):
                raise ManagerError(f"{label} escapes repository root")
            if _is_within(source, self.config.repository_root / "originals"):
                raise ManagerError(f"{label} cannot use originals/")
            protected = self.config.protected_profile.resolve(strict=False)
            if _paths_equal(source, protected) or _is_within(source, protected):
                raise ManagerError(f"{label} resolves into the protected gameplay profile")
            if source.name.casefold() != artifact.filename.casefold():
                raise ManagerError(f"repository {label} basename must equal {artifact.filename}")
            return source

        relative = _safe_relative(descriptor["path"], label, allow_nested=True)
        source = self.target / Path(relative)
        _assert_no_reparse_components(source, f"adopted-target {label}", root=self.target)
        self._assert_target_containment(source.resolve(strict=False), label)
        allowed_names = {artifact.filename.casefold(), (artifact.filename + ".disabled").casefold()}
        if source.name.casefold() not in allowed_names:
            raise ManagerError(f"adopted-target {label} basename must be {artifact.filename} or {artifact.filename}.disabled")
        return source

    def _verify_artifact_file(self, path: Path, artifact: ManagedArtifact) -> frozenset[str]:
        if not path.exists() or not path.is_file():
            raise ManagerError(f"missing artifact {path}")
        if path.is_symlink():
            raise ManagerError(f"managed artifacts cannot be symbolic links: {path}")
        resolved = path.resolve(strict=True)
        if artifact.source["type"] == "ADOPTED_TARGET" or _is_within(resolved, self.target):
            self._assert_target_containment(resolved, f"artifact {path.name}")
        actual_hash = _sha256(path)
        if actual_hash != artifact.sha256:
            raise ManagerError(f"SHA-256 mismatch for {path}: expected {artifact.sha256}, found {actual_hash}")
        ids = _fabric_mod_ids(path)
        if artifact.mod_id not in ids:
            raise ManagerError(
                f"Fabric mod ownership mismatch for {path}: expected primary id {artifact.mod_id}, found {sorted(ids)}"
            )
        primary = next(iter(ids))  # _fabric_mod_ids returns primary first only conceptually; re-read below for exactness.
        try:
            with zipfile.ZipFile(path) as archive:
                manifest = json.loads(archive.read("fabric.mod.json").decode("utf-8"))
            primary = manifest["id"]
        except (OSError, KeyError, UnicodeDecodeError, json.JSONDecodeError, zipfile.BadZipFile) as exc:
            raise ManagerError(f"cannot verify Fabric manifest in {path}: {exc}") from exc
        if primary != artifact.mod_id:
            raise ManagerError(
                f"Fabric primary mod id mismatch for {path}: expected {artifact.mod_id}, found {primary}"
            )
        return ids

    def _verify_inventory(
        self,
        state: dict[str, Any],
        artifacts: tuple[ManagedArtifact, ...] | None = None,
        *,
        conflict_mod_ids: set[str] | None = None,
        prior_ledger: dict[str, Any] | None = None,
    ) -> tuple[ManagedArtifact, ...]:
        expected = self.derive_inventory(state) if artifacts is None else artifacts
        expected_paths = {artifact.relative_path.casefold(): artifact for artifact in expected}
        manifest_ids_by_active_path: dict[str, str] = {}
        for artifact in expected:
            path = self._destination(artifact)
            ids = self._verify_artifact_file(path, artifact)
            if artifact.active:
                for mod_id in ids:
                    previous = manifest_ids_by_active_path.get(mod_id)
                    if previous is not None:
                        raise ManagerError(f"active Fabric mod id collision {mod_id}: {previous} and {artifact.relative_path}")
                    manifest_ids_by_active_path[mod_id] = artifact.relative_path

        managed_ids = set(manifest_ids_by_active_path)
        if conflict_mod_ids:
            managed_ids.update(conflict_mod_ids)
        prior_paths: set[str] = set()
        if prior_ledger is not None:
            records = prior_ledger.get("managed_files")
            if isinstance(records, list):
                prior_paths = {
                    record.get("path", "").casefold()
                    for record in records
                    if isinstance(record, dict) and isinstance(record.get("path"), str)
                }

        try:
            entries = tuple(self.mods.iterdir())
        except OSError as exc:
            raise ManagerError(f"cannot scan managed mods directory {self.mods}: {exc}") from exc
        for path in entries:
            if not path.is_file() or not (path.name.casefold().endswith(".jar") or path.name.casefold().endswith(".jar.disabled")):
                continue
            if path.is_symlink():
                raise ManagerError(f"mods inventory contains a symbolic link: {path}")
            relative = PurePosixPath(self.config.mods_directory, path.name).as_posix()
            normalized = relative.casefold()
            if normalized in expected_paths:
                continue
            if normalized in prior_paths:
                raise ManagerError(f"unexpected stale managed artifact remains: {relative}")
            if path.name.casefold().endswith(".jar.disabled"):
                # Legacy/user rollback fallbacks do not participate in Fabric's
                # enabled ownership. Preserve them unless a V2 ledger owned the
                # exact path, in which case it is a stale managed artifact.
                continue
            try:
                ids = _fabric_mod_ids(path)
            except ManagerError:
                # A foreign non-Fabric jar is unrelated unless it was ledger-owned.
                continue
            overlap = ids.intersection(managed_ids)
            if overlap:
                raise ManagerError(
                    f"unmanaged conflicting mod artifact {relative} owns {', '.join(sorted(overlap))}"
                )
        return expected

    def _read_ledger(self) -> dict[str, Any]:
        try:
            ledger = json.loads(self.ledger_path.read_text(encoding="utf-8"))
        except FileNotFoundError as exc:
            raise ManagerError(f"target-local V2 ledger is missing: {self.ledger_path}") from exc
        except (OSError, json.JSONDecodeError) as exc:
            raise ManagerError(f"cannot load target-local V2 ledger {self.ledger_path}: {exc}") from exc
        v2_required = {
            "$schema",
            "schema_version",
            "target",
            "state_revision",
            "state_digest",
            "managed_files",
            "runtime_state",
        }
        if not isinstance(ledger, dict):
            raise ManagerError("target-local V2 ledger has an invalid shape")
        if ledger.get("$schema") == LEDGER_SCHEMA_V2 and ledger.get("schema_version") == 2:
            required = v2_required
        elif ledger.get("$schema") == LEDGER_SCHEMA and ledger.get("schema_version") == 3:
            required = {*v2_required, "title_projection"}
        else:
            raise ManagerError("target-local ledger is not a supported V2/V3 ledger")
        if set(ledger) != required:
            raise ManagerError("target-local manager ledger has an invalid shape")
        if not isinstance(ledger["target"], str) or not _paths_equal(Path(ledger["target"]), self.target):
            raise ManagerError("target-local ledger names a different profile")
        try:
            validate_runtime_state(ledger["runtime_state"], self.project_index)
        except ValidationError as exc:
            raise ManagerError(f"target-local ledger contains invalid runtime state: {exc}") from exc
        if ledger["state_digest"] != state_digest(ledger["runtime_state"]):
            raise ManagerError("target-local ledger runtime-state digest mismatch")
        if ledger["state_revision"] != ledger["runtime_state"]["revision"]:
            raise ManagerError("target-local ledger runtime-state revision mismatch")
        expected_inventory = (
            self.derive_inventory(ledger["runtime_state"])
            if ledger["schema_version"] == 2
            else self._desired_managed_inventory(ledger["runtime_state"])
        )
        expected_records = [item.ledger_record() for item in expected_inventory]
        if ledger["managed_files"] != expected_records:
            raise ManagerError("target-local ledger managed-file inventory does not match its runtime state")
        if ledger["schema_version"] == 3 and ledger["title_projection"] != self._title_projection(ledger["runtime_state"]):
            raise ManagerError("target-local ledger title projection does not match its runtime state")
        return ledger

    def _ledger(self, state: dict[str, Any], artifacts: Iterable[ManagedArtifact]) -> dict[str, Any]:
        return {
            "$schema": LEDGER_SCHEMA,
            "schema_version": 3,
            "target": str(self.target),
            "state_revision": state["revision"],
            "state_digest": state_digest(state),
            "managed_files": [artifact.ledger_record() for artifact in artifacts],
            "runtime_state": copy.deepcopy(state),
            "title_projection": self._title_projection(state),
        }

    def _assert_ledger_matches_repository(self, ledger: dict[str, Any], state: dict[str, Any]) -> None:
        if state["activation"] != "ACTIVE":
            raise ManagerError("physical transitions require repository runtime-state activation ACTIVE")
        if ledger["state_digest"] != state_digest(state) or ledger["runtime_state"] != state:
            raise ManagerError("repository runtime state and target-local V2 ledger have diverged")

    def _physical_verification_report(
        self,
        state: dict[str, Any],
        artifacts: tuple[ManagedArtifact, ...],
        title_projection_evidence: dict[str, Any] | None,
    ) -> dict[str, Any]:
        records: list[dict[str, Any]] = []
        for artifact in artifacts:
            path = self._destination(artifact)
            actual_sha256 = _sha256(path)
            if actual_sha256 != artifact.sha256:
                raise ManagerError(
                    f"SHA-256 changed while producing physical evidence for {path}: "
                    f"expected {artifact.sha256}, found {actual_sha256}"
                )
            records.append(
                {
                    "deployment_id": artifact.deployment_id,
                    "artifact_id": artifact.artifact_id,
                    "project_id": artifact.project_id,
                    "filename": artifact.filename,
                    "path": artifact.relative_path,
                    "sha256": actual_sha256,
                    "disposition": "ACTIVE" if artifact.active else "DISABLED",
                }
            )
        records.sort(key=lambda item: item["path"].casefold())

        def inventory_digest(items: list[dict[str, Any]]) -> str:
            payload = json.dumps(items, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
            return hashlib.sha256(payload).hexdigest()

        records_by_deployment: dict[str, list[dict[str, Any]]] = {}
        for record in records:
            records_by_deployment.setdefault(record["deployment_id"], []).append(record)

        accepted_deployments = {
            member["unit"]["deployment_id"] for member in state["accepted_baseline"]["members"]
        }
        accepted_records = [record for record in records if record["deployment_id"] in accepted_deployments]
        title = self._render_title(state)
        slot_evidence: dict[str, dict[str, Any] | None] = {}
        for label in ("A", "B"):
            slot = state["slots"][label]
            if slot is None:
                slot_evidence[label] = None
                continue
            unit = slot["unit"]
            slot_evidence[label] = {
                "deployment_id": unit["deployment_id"],
                "project_uuid": unit["project_uuid"],
                "project_id": unit["project_id"],
                "project_display_name": title["slots"][label]["project_display_name"],
                "version": unit["version"],
                "canary": title["slots"][label]["canary"],
                "deployment_state": slot["deployment"]["state"],
                "runtime_result": slot["runtime_result"]["classification"],
                "artifacts": copy.deepcopy(records_by_deployment.get(unit["deployment_id"], [])),
            }

        return {
            "status": "PHYSICAL_STATE_VERIFIED",
            "target": str(self.target),
            "state_revision": state["revision"],
            "state_digest": state_digest(state),
            "managed_file_count": len(records),
            "physical_inventory_digest": inventory_digest(records),
            "accepted_baseline": {
                "revision": state["accepted_baseline"]["revision"],
                "stack_version": state["accepted_baseline"]["revision"],
                "stack_label": title["baseline"]["stack_label"],
                "member_count": len(state["accepted_baseline"]["members"]),
                "artifact_count": len(accepted_records),
                "active_artifact_count": sum(record["disposition"] == "ACTIVE" for record in accepted_records),
                "disabled_artifact_count": sum(record["disposition"] == "DISABLED" for record in accepted_records),
                "inventory_digest": inventory_digest(accepted_records),
            },
            "slots": slot_evidence,
            "title_display": {
                "status": "SYNCHRONIZED" if title_projection_evidence is not None else "LEGACY_MIGRATION_REQUIRED",
                "projection": copy.deepcopy(title_projection_evidence),
                "lines": copy.deepcopy(title["lines"]),
            },
            "infrastructure": {
                "title_marker": copy.deepcopy(
                    next(record for record in records if record["project_id"] == "workbench-test-marker")
                )
            },
        }

    def _verify_current_physical_state(self, *, allow_legacy_marker: bool = False) -> dict[str, Any]:
        self._assert_no_transaction_residue()
        if not allow_legacy_marker:
            self._assert_no_legacy_marker()
        state = self.load_repository_state()
        ledger = self._read_ledger()
        self._assert_ledger_matches_repository(ledger, state)
        artifacts = self._managed_inventory_for_ledger(state, ledger)
        artifacts = self._verify_inventory(state, artifacts, prior_ledger=ledger)
        projection = self._verify_title_projection(state) if ledger["schema_version"] == 3 else None
        return self._physical_verification_report(state, artifacts, projection)

    def adoption_plan(self, state: dict[str, Any] | None = None) -> PhysicalPlan:
        self._assert_no_transaction_residue()
        state = self.load_repository_state() if state is None else copy.deepcopy(state)
        try:
            validate_runtime_state(state, self.project_index)
        except ValidationError as exc:
            raise ManagerError(f"invalid runtime state: {exc}") from exc
        if state["activation"] != "GATED":
            raise ManagerError("initial adoption requires activation GATED")
        if not state["accepted_baseline"]["members"] or state["slots"]["A"] is None or state["slots"]["B"] is None:
            raise ManagerError("initial adoption requires a populated accepted baseline and both test slots")
        if self.ledger_path.exists():
            raise ManagerError(f"initial adoption refuses an existing target-local ledger: {self.ledger_path}")
        current_artifacts = self._legacy_managed_inventory(state)
        for artifact in current_artifacts:
            source = self._source_path(artifact)
            if source.exists():
                self._verify_artifact_file(source, artifact)
            elif artifact.source["type"] == "REPOSITORY":
                raise ManagerError(f"initial adoption requires repository source {source}")
        self._verify_inventory(state, current_artifacts)
        active = copy.deepcopy(state)
        active["activation"] = "ACTIVE"
        active["revision"] += 1
        active["updated_at"] = _utc_now()
        active["accepted_baseline"]["provenance"]["physical_disposition"] = "ADOPTED"
        validate_runtime_state(active, self.project_index)
        desired_artifacts = self._desired_managed_inventory(active)
        writes, removals, unchanged = self._plan_delta(current_artifacts, desired_artifacts)
        return PhysicalPlan(
            mode="ADOPT",
            current_state=state,
            desired_state=active,
            current_artifacts=current_artifacts,
            desired_artifacts=desired_artifacts,
            writes=writes,
            removals=removals,
            unchanged=unchanged,
            title_projection=self._title_projection(active),
        )

    def adopt(self, *, dry_run: bool = True, failure_injector: FailureInjector | None = None) -> dict[str, Any]:
        """Verify an exact populated GATED profile, then activate and ledger it."""

        if dry_run:
            return self.adoption_plan().summary(dry_run=True)
        with _ExclusiveTargetLock(self.lock_path):
            plan = self.adoption_plan()
            self._commit_plan(plan, failure_injector=failure_injector)
            return plan.summary(dry_run=False)

    def transition_plan(
        self,
        *,
        operation: dict[str, Any] | None = None,
        desired_state: dict[str, Any] | None = None,
        expected_revision: int | None = None,
        at: str | None = None,
    ) -> PhysicalPlan:
        self._assert_no_transaction_residue()
        self._assert_no_legacy_marker()
        if (operation is None) == (desired_state is None):
            raise ManagerError("provide exactly one of operation or desired_state")
        current = self.load_repository_state()
        ledger = self._read_ledger()
        self._assert_ledger_matches_repository(ledger, current)
        current_artifacts = self._managed_inventory_for_ledger(current, ledger)

        if operation is not None:
            if expected_revision is None or at is None:
                raise ManagerError("operation transitions require expected_revision and at")
            try:
                desired = plan_transition(current, expected_revision, operation, at, self.project_index)
            except ValidationError as exc:
                raise ManagerError(f"invalid runtime transition: {exc}") from exc
        else:
            desired = copy.deepcopy(desired_state)
            try:
                validate_runtime_state(desired, self.project_index)
            except ValidationError as exc:
                raise ManagerError(f"invalid desired runtime state: {exc}") from exc
            if desired["activation"] != "ACTIVE":
                raise ManagerError("desired physical runtime state must remain ACTIVE")
            if desired["revision"] != current["revision"] + 1:
                raise ManagerError("desired runtime state must be a prevalidated single next revision")
            self._assert_desired_is_pure_transition(current, desired)

        desired_artifacts = self._desired_managed_inventory(desired)
        union_mod_ids = {item.mod_id for item in current_artifacts}
        for artifact in desired_artifacts:
            if not artifact.active:
                continue
            source = self._source_path(artifact)
            if source.exists():
                union_mod_ids.update(self._verify_artifact_file(source, artifact))
                continue
            destination = self._destination(artifact)
            if destination.exists():
                union_mod_ids.update(self._verify_artifact_file(destination, artifact))
                continue
            union_mod_ids.add(artifact.mod_id)
        self._verify_inventory(
            current,
            current_artifacts,
            conflict_mod_ids=union_mod_ids,
            prior_ledger=ledger,
        )
        writes, removals, unchanged = self._plan_delta(current_artifacts, desired_artifacts)
        return PhysicalPlan(
            mode="TRANSITION",
            current_state=current,
            desired_state=desired,
            current_artifacts=current_artifacts,
            desired_artifacts=desired_artifacts,
            writes=writes,
            removals=removals,
            unchanged=unchanged,
            title_projection=self._title_projection(desired),
        )

    def _assert_desired_is_pure_transition(self, current: dict[str, Any], desired: dict[str, Any]) -> None:
        """Prove that an externally planned next state is one legal pure transition."""

        def declaration(slot: dict[str, Any]) -> dict[str, Any]:
            result = {
                "unit": copy.deepcopy(slot["unit"]),
                "replaces_accepted_deployment_id": slot["replaces_accepted_deployment_id"],
            }
            if slot.get("dependency_overrides"):
                result["dependency_overrides"] = copy.deepcopy(slot["dependency_overrides"])
            return result

        operations: list[dict[str, Any]] = []
        for label in ("A", "B"):
            desired_slot = desired["slots"][label]
            if desired_slot is not None:
                operations.append({"type": "ASSIGN_SLOT", "candidate": declaration(desired_slot)})
                operations.append({"type": "UPDATE_SLOT", "slot": label, "candidate": declaration(desired_slot)})
            operations.extend(
                {"type": operation_type, "slot": label}
                for operation_type in ("MARK_DEPLOYED", "MARK_READY", "REMOVE_SLOT", "PROMOTE_SLOT")
            )
            if desired_slot is not None:
                operations.append(
                    {
                        "type": "RECORD_RESULT",
                        "slot": label,
                        "classification": desired_slot["runtime_result"]["classification"],
                        "evidence": copy.deepcopy(desired_slot["runtime_result"]["evidence"]),
                    }
                )
        if desired["slots"]["A"] is None and desired["slots"]["B"] is None:
            operations.append({"type": "SET_PROFILE", "candidates": []})
        elif desired["slots"]["A"] is not None:
            candidates = [declaration(desired["slots"]["A"])]
            if desired["slots"]["B"] is not None:
                candidates.append(declaration(desired["slots"]["B"]))
            operations.append({"type": "SET_PROFILE", "candidates": candidates})
        operations.extend(
            {
                "type": "REMOVE_ACCEPTED",
                "project_uuid": member["unit"]["project_uuid"],
            }
            for member in current["accepted_baseline"]["members"]
        )
        current_accepted_by_project = {
            member["unit"]["project_uuid"]: member["unit"]
            for member in current["accepted_baseline"]["members"]
        }
        for member in desired["accepted_baseline"]["members"]:
            desired_unit = member["unit"]
            current_unit = current_accepted_by_project.get(desired_unit["project_uuid"])
            if current_unit is None or current_unit == desired_unit:
                continue
            operations.append(
                {
                    "type": "PROMOTE_UNTESTED_CANDIDATE",
                    "authorization": "USER_APPROVED_UNTESTED_PROMOTION",
                    "candidate": {
                        "unit": copy.deepcopy(desired_unit),
                        "replaces_accepted_deployment_id": current_unit["deployment_id"],
                    },
                }
            )

        for operation in operations:
            try:
                planned = plan_transition(
                    current,
                    current["revision"],
                    operation,
                    desired["updated_at"],
                    self.project_index,
                )
            except ValidationError:
                continue
            if planned == desired:
                return
        raise ManagerError("desired runtime state is not the exact output of one legal pure runtime transition")

    def _plan_delta(
        self,
        current: tuple[ManagedArtifact, ...],
        desired: tuple[ManagedArtifact, ...],
    ) -> tuple[tuple[FileAction, ...], tuple[Path, ...], tuple[str, ...]]:
        current_by_path = {item.relative_path.casefold(): item for item in current}
        current_by_artifact = {item.artifact_id: item for item in current}
        desired_by_path = {item.relative_path.casefold(): item for item in desired}
        writes: list[FileAction] = []
        unchanged: list[str] = []
        for artifact in desired:
            destination = self._destination(artifact)
            existing = current_by_path.get(artifact.relative_path.casefold())
            if existing is None and destination.exists():
                raise ManagerError(f"desired managed path is occupied by an unmanaged file: {artifact.relative_path}")
            if existing is not None and existing.sha256 == artifact.sha256 and destination.exists():
                self._verify_artifact_file(destination, artifact)
                unchanged.append(artifact.relative_path)
                continue

            source: Path | None = None
            same_artifact = current_by_artifact.get(artifact.artifact_id)
            if same_artifact is not None and same_artifact.sha256 == artifact.sha256:
                candidate = self._destination(same_artifact)
                if candidate.exists():
                    self._verify_artifact_file(candidate, artifact)
                    source = candidate
            if source is None:
                candidate = self._source_path(artifact)
                if not candidate.exists():
                    raise ManagerError(f"missing artifact source for {artifact.project_id}/{artifact.filename}: {candidate}")
                self._verify_artifact_file(candidate, artifact)
                source = candidate
            writes.append(FileAction(destination, artifact.relative_path, artifact, source))

        removals = tuple(
            self._destination(artifact)
            for artifact in current
            if artifact.relative_path.casefold() not in desired_by_path
            or desired_by_path[artifact.relative_path.casefold()].sha256 != artifact.sha256
        )
        return tuple(writes), removals, tuple(unchanged)

    def transition(
        self,
        *,
        operation: dict[str, Any] | None = None,
        desired_state: dict[str, Any] | None = None,
        expected_revision: int | None = None,
        at: str | None = None,
        dry_run: bool = True,
        failure_injector: FailureInjector | None = None,
    ) -> dict[str, Any]:
        if dry_run:
            return self.transition_plan(
                operation=operation,
                desired_state=desired_state,
                expected_revision=expected_revision,
                at=at,
            ).summary(dry_run=True)
        with _ExclusiveTargetLock(self.lock_path):
            plan = self.transition_plan(
                operation=operation,
                desired_state=desired_state,
                expected_revision=expected_revision,
                at=at,
            )
            self._commit_plan(plan, failure_injector=failure_injector)
            return plan.summary(dry_run=False)

    def verify(self) -> dict[str, Any]:
        with _ExclusiveTargetLock(self.lock_path):
            return self._verify_current_physical_state()

    def _read_marker_bytes(self, path: Path, label: str) -> bytes:
        _assert_no_reparse_components(path, label, root=self.target)
        if not path.exists() or not path.is_file():
            raise ManagerError(f"{label} is missing or is not a regular file: {path}")
        if path.is_symlink():
            raise ManagerError(f"{label} cannot be a symbolic link: {path}")
        self._assert_target_containment(path.resolve(strict=True), label)
        try:
            return path.read_bytes()
        except OSError as exc:
            raise ManagerError(f"cannot read {label} {path}: {exc}") from exc

    def retire_legacy_marker(self, *, dry_run: bool = True) -> dict[str, Any]:
        """Retire exact V1 display metadata only after a live V2 physical verification."""

        with _ExclusiveTargetLock(self.lock_path):
            verification = self._verify_current_physical_state(allow_legacy_marker=True)
            marker = self._read_marker_bytes(self.legacy_marker_path, "legacy V1 marker")
            marker_sha256 = hashlib.sha256(marker).hexdigest()
            _assert_no_reparse_components(
                self.retired_legacy_marker_path,
                "retired legacy V1 marker",
                root=self.target,
            )
            retired_exists = self.retired_legacy_marker_path.exists()
            if retired_exists:
                retired = self._read_marker_bytes(self.retired_legacy_marker_path, "retired legacy V1 marker")
                if retired != marker:
                    raise ManagerError("retired legacy V1 marker exists with differing bytes; refusing overwrite")

            result = {
                "mode": "RETIRE_LEGACY_MARKER",
                "dry_run": dry_run,
                "status": "LEGACY_MARKER_RETIREMENT_READY" if dry_run else "LEGACY_MARKER_RETIRED",
                "source": LEGACY_MARKER_NAME,
                "retired": RETIRED_LEGACY_MARKER_NAME,
                "sha256": marker_sha256,
                "byte_count": len(marker),
                "already_preserved": retired_exists,
                "physical_verification": verification,
            }
            if dry_run:
                return result

            try:
                if retired_exists:
                    self.legacy_marker_path.unlink()
                else:
                    os.replace(self.legacy_marker_path, self.retired_legacy_marker_path)
            except OSError as exc:
                raise ManagerError(f"cannot retire legacy V1 marker: {exc}") from exc

            retired = self._read_marker_bytes(self.retired_legacy_marker_path, "retired legacy V1 marker")
            if retired != marker or self.legacy_marker_path.exists():
                raise ManagerError("legacy V1 marker retirement did not preserve the exact bytes and remove the active marker")
            result["physical_verification"] = self._verify_current_physical_state()
            return result

    def _commit_plan(self, plan: PhysicalPlan, *, failure_injector: FailureInjector | None) -> None:
        injector = failure_injector or (lambda _stage: None)
        transaction = self.target / f".mynx-runtime-v2-transaction-{uuid.uuid4()}"
        _assert_no_reparse_components(transaction, "transaction backup", root=self.target)
        self._assert_target_containment(transaction.resolve(strict=False), "transaction backup")
        touched = sorted(
            {action.destination for action in plan.writes}.union(plan.removals).union({self.title_projection_path}),
            key=lambda item: str(item).casefold(),
        )
        snapshots: dict[Path, Path | None] = {}
        repo_snapshot: Path | None = None
        ledger_snapshot: Path | None = None
        ledger_written = False
        committed = False
        preserve_transaction = False
        try:
            transaction.mkdir(exist_ok=False)
            target_backup = transaction / "target"
            source_backup = transaction / "sources"
            target_backup.mkdir()
            source_backup.mkdir()

            for index, path in enumerate(touched):
                self._assert_target_containment(path.resolve(strict=False), f"transaction target {path.name}")
                if path.exists():
                    backup = target_backup / f"{index:04d}-{path.name}"
                    shutil.copy2(path, backup)
                    snapshots[path] = backup
                else:
                    snapshots[path] = None

            if self.ledger_path.exists():
                ledger_candidate = transaction / "ledger-before.json"
                _atomic_copy(self.ledger_path, ledger_candidate)
                ledger_snapshot = ledger_candidate
            repo_candidate = transaction / "runtime-state-before.json"
            _atomic_copy(self.config.runtime_state, repo_candidate)
            repo_snapshot = repo_candidate

            staged: dict[Path, Path] = {}
            for index, action in enumerate(plan.writes):
                stage = source_backup / f"{index:04d}-{action.artifact.filename}"
                shutil.copy2(action.source, stage)
                self._verify_artifact_file(stage, action.artifact)
                staged[action.destination] = stage
            injector("after_backup")

            for index, path in enumerate(plan.removals):
                if path.exists():
                    path.unlink()
                injector(f"after_remove_{index + 1}")
            for index, action in enumerate(plan.writes):
                action.destination.parent.mkdir(parents=False, exist_ok=True)
                _atomic_copy(staged[action.destination], action.destination)
                injector(f"after_write_{index + 1}")
            _atomic_write_json(self.title_projection_path, plan.title_projection)
            injector("after_title_projection_write")
            injector("after_physical_apply")

            self._verify_inventory(
                plan.desired_state,
                plan.desired_artifacts,
                conflict_mod_ids={item.mod_id for item in plan.desired_artifacts},
            )
            self._verify_title_projection(plan.desired_state)
            injector("after_physical_verify")

            # Recheck the compare-and-swap source immediately before committing
            # either ledger.  The target lock serializes all manager writers.
            live = self.load_repository_state()
            if live != plan.current_state:
                raise ManagerError("repository runtime state changed during physical transition")

            ledger = self._ledger(plan.desired_state, plan.desired_artifacts)
            _atomic_write_json(self.ledger_path, ledger)
            ledger_written = True
            injector("after_ledger_write")
            _atomic_write_json(self.config.runtime_state, plan.desired_state)
            injector("after_state_write")

            final_state = self.load_repository_state()
            final_ledger = self._read_ledger()
            self._assert_ledger_matches_repository(final_ledger, final_state)
            self._verify_inventory(final_state, plan.desired_artifacts, prior_ledger=final_ledger)
            self._verify_title_projection(final_state)
            injector("after_post_verify")
            committed = True
        except Exception as exc:
            recovery_error: Exception | None = None
            try:
                for path, backup in snapshots.items():
                    if backup is None:
                        if path.exists():
                            path.unlink()
                    else:
                        _atomic_copy(backup, path)
                if ledger_snapshot is not None:
                    _atomic_copy(ledger_snapshot, self.ledger_path)
                elif ledger_written and self.ledger_path.exists():
                    self.ledger_path.unlink()
                if repo_snapshot is not None:
                    _atomic_copy(repo_snapshot, self.config.runtime_state)
            except Exception as rollback_exc:  # pragma: no cover - catastrophic I/O path
                recovery_error = rollback_exc
            if recovery_error is not None:
                preserve_transaction = True
                raise ManagerError(f"physical apply failed ({exc}); rollback also failed ({recovery_error})") from exc
            if isinstance(exc, ManagerError):
                raise
            raise ManagerError(f"physical apply failed and was rolled back: {exc}") from exc
        finally:
            if transaction.exists() and not preserve_transaction:
                try:
                    shutil.rmtree(transaction)
                except OSError:
                    if committed:
                        raise ManagerError(f"transition committed but transaction backup cleanup failed: {transaction}")


def _configured_path(base: Path, value: Any, label: str) -> Path:
    if not isinstance(value, str) or not value.strip():
        raise ManagerError(f"{label} must be a nonblank path")
    path = Path(value)
    return (path if path.is_absolute() else base / path).resolve(strict=False)


def _absolute_configured_path(value: Any, label: str) -> Path:
    if not isinstance(value, str) or not value.strip() or not Path(value).is_absolute():
        raise ManagerError(f"{label} must be an absolute path")
    return Path(os.path.abspath(value))


def _contained_configured_path(root: Path, value: Any, label: str) -> Path:
    relative = _safe_relative(value, label, allow_nested=True)
    path = (root / Path(relative)).resolve(strict=False)
    if not _is_within(path, root):
        raise ManagerError(f"{label} escapes repository_root")
    return path


def _safe_basename(value: Any, label: str) -> str:
    result = _safe_relative(value, label, allow_nested=False)
    if result in {".", ".."}:
        raise ManagerError(f"{label} must be a safe basename")
    return result


def _safe_relative(value: Any, label: str, *, allow_nested: bool) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ManagerError(f"{label} must be a nonblank relative path")
    if Path(value).is_absolute() or PureWindowsPath(value).is_absolute() or PurePosixPath(value).is_absolute():
        raise ManagerError(f"{label} must be relative")
    windows = PureWindowsPath(value)
    posix = PurePosixPath(value.replace("\\", "/"))
    if windows.drive or any(part in {"", ".", ".."} for part in posix.parts):
        raise ManagerError(f"{label} contains unsafe path components")
    if not allow_nested and len(posix.parts) != 1:
        raise ManagerError(f"{label} must be a basename")
    if any(character in value for character in '<>:"|?*'):
        raise ManagerError(f"{label} contains Windows-unsafe characters")
    return str(Path(*posix.parts))


def _paths_equal(left: Path, right: Path) -> bool:
    return os.path.normcase(str(left.resolve(strict=False))).casefold() == os.path.normcase(
        str(right.resolve(strict=False))
    ).casefold()


def _is_within(child: Path, parent: Path) -> bool:
    child_text = os.path.normcase(str(child.resolve(strict=False))).casefold()
    parent_text = os.path.normcase(str(parent.resolve(strict=False))).casefold()
    try:
        return os.path.commonpath([child_text, parent_text]) == parent_text
    except ValueError:
        return False


def _assert_no_reparse_components(path: Path, label: str, *, root: Path | None = None) -> None:
    """Reject symlinks, junctions, and reparse points in an existing path prefix."""

    absolute = Path(os.path.abspath(path))
    floor = absolute if root is None else Path(os.path.abspath(root))
    try:
        relative_parts = absolute.relative_to(floor).parts
    except ValueError as exc:
        raise ManagerError(f"{label} is not lexically contained by its managed root") from exc
    reparse_flag = getattr(stat, "FILE_ATTRIBUTE_REPARSE_POINT", 0x400)
    current = floor
    for index in range(len(relative_parts) + 1):
        try:
            metadata = os.lstat(current)
        except FileNotFoundError:
            break
        except OSError as exc:
            raise ManagerError(f"cannot inspect {label} path component {current}: {exc}") from exc
        attributes = getattr(metadata, "st_file_attributes", 0)
        if stat.S_ISLNK(metadata.st_mode) or attributes & reparse_flag:
            raise ManagerError(f"{label} cannot traverse a symlink, junction, or reparse point: {current}")
        if index < len(relative_parts):
            current /= relative_parts[index]


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    try:
        with path.open("rb") as handle:
            for chunk in iter(lambda: handle.read(1024 * 1024), b""):
                digest.update(chunk)
    except OSError as exc:
        raise ManagerError(f"cannot hash artifact {path}: {exc}") from exc
    return digest.hexdigest()


def _utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="microseconds").replace("+00:00", "Z")


def _fabric_mod_ids(path: Path) -> frozenset[str]:
    try:
        with zipfile.ZipFile(path) as archive:
            matches = [item for item in archive.infolist() if item.filename == "fabric.mod.json"]
            if len(matches) != 1:
                raise ManagerError(f"{path} must contain exactly one root fabric.mod.json")
            if matches[0].file_size > 1024 * 1024:
                raise ManagerError(f"fabric.mod.json is unreasonably large in {path}")
            manifest = json.loads(archive.read(matches[0]).decode("utf-8"))
    except ManagerError:
        raise
    except (OSError, UnicodeDecodeError, json.JSONDecodeError, zipfile.BadZipFile) as exc:
        raise ManagerError(f"cannot read Fabric manifest from {path}: {exc}") from exc
    if not isinstance(manifest, dict) or not isinstance(manifest.get("id"), str) or not manifest["id"]:
        raise ManagerError(f"fabric.mod.json in {path} has no valid id")
    identifiers = {manifest["id"]}
    provides = manifest.get("provides", [])
    if isinstance(provides, list):
        identifiers.update(item for item in provides if isinstance(item, str) and item)
    return frozenset(identifiers)


def _atomic_copy(source: Path, destination: Path) -> None:
    temporary: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="wb", prefix=destination.name + ".", suffix=".tmp", dir=destination.parent, delete=False
        ) as handle:
            temporary = Path(handle.name)
            with source.open("rb") as source_handle:
                shutil.copyfileobj(source_handle, handle)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, destination)
        temporary = None
    finally:
        if temporary is not None and temporary.exists():
            temporary.unlink()


def _atomic_write_json(path: Path, value: dict[str, Any]) -> None:
    temporary: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w", encoding="utf-8", newline="\n", prefix=path.name + ".", suffix=".tmp", dir=path.parent, delete=False
        ) as handle:
            json.dump(value, handle, indent=2, ensure_ascii=False)
            handle.write("\n")
            handle.flush()
            os.fsync(handle.fileno())
            temporary = Path(handle.name)
        os.replace(temporary, path)
        temporary = None
    finally:
        if temporary is not None and temporary.exists():
            temporary.unlink()


def _load_json_argument(path: str) -> dict[str, Any]:
    try:
        value = json.loads(Path(path).read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ManagerError(f"cannot load JSON argument {path}: {exc}") from exc
    if not isinstance(value, dict):
        raise ManagerError(f"JSON argument must contain an object: {path}")
    return value


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, default=DEFAULT_CONFIG)
    subparsers = parser.add_subparsers(dest="command", required=True)
    adopt_parser = subparsers.add_parser("adopt", help="verify/adopt the exact populated GATED physical state")
    adopt_parser.add_argument("--apply", action="store_true", help="commit after preflight; default is dry-run")
    subparsers.add_parser("verify", help="verify ACTIVE repository, target ledger, and managed inventory")
    retire_parser = subparsers.add_parser(
        "retire-legacy-marker",
        help="verify V2 physical state and retire stale V1 display metadata",
    )
    retire_parser.add_argument("--apply", action="store_true", help="commit marker retirement; default is dry-run")
    transition_parser = subparsers.add_parser("transition", help="plan or apply one pure runtime-state transition")
    transition_parser.add_argument("--operation", required=True, help="path to operation JSON")
    transition_parser.add_argument("--expected-revision", type=int, required=True)
    transition_parser.add_argument("--at", required=True)
    transition_parser.add_argument("--apply", action="store_true", help="commit after preflight; default is dry-run")
    args = parser.parse_args(argv)

    try:
        manager = PhysicalManager.from_config(args.config)
        if args.command == "adopt":
            result = manager.adopt(dry_run=not args.apply)
        elif args.command == "verify":
            result = manager.verify()
        elif args.command == "retire-legacy-marker":
            result = manager.retire_legacy_marker(dry_run=not args.apply)
        else:
            result = manager.transition(
                operation=_load_json_argument(args.operation),
                expected_revision=args.expected_revision,
                at=args.at,
                dry_run=not args.apply,
            )
        print(json.dumps(result, indent=2, ensure_ascii=False))
        return 0
    except (ManagerError, ValidationError) as exc:
        parser.exit(2, f"error: {exc}\n")


if __name__ == "__main__":
    raise SystemExit(main())
