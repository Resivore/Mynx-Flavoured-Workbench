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
import sqlite3
import stat
import subprocess
import tempfile
import uuid
import zipfile
from dataclasses import dataclass, replace as dataclass_replace
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath, PureWindowsPath
from typing import Any, Callable, Iterable, Sequence

try:
    from ..runtime_slots import (
        _PHYSICAL_MANAGER_AUTHORITY,
        finalize_verified_profile_transition,
        migrate_runtime_state,
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
        _PHYSICAL_MANAGER_AUTHORITY,
        finalize_verified_profile_transition,
        migrate_runtime_state,
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
JavaPropertyProbe = Callable[[Path], dict[str, str]]


class ManagerError(RuntimeError):
    """A physical preflight, ownership, serialization, or recovery failure."""


def _slot_members(slot: dict[str, Any]) -> list[dict[str, Any]]:
    """Return normalized member records while accepting a legacy one-unit slot."""

    members = slot.get("members")
    if isinstance(members, list):
        return members
    if isinstance(slot.get("unit"), dict):
        member = {
            "unit": slot["unit"],
            "replaces_accepted_deployment_id": slot.get("replaces_accepted_deployment_id"),
            "runtime_result": slot.get("runtime_result"),
        }
        if slot.get("dependency_overrides"):
            member["dependency_overrides"] = slot["dependency_overrides"]
        return [member]
    raise ManagerError("occupied runtime slot has neither members nor a legacy unit")


def _slot_physical_identity(slot: dict[str, Any] | None) -> Any:
    """Return only the member composition which can change physical bytes."""

    if slot is None:
        return None
    return [
        {
            "unit": copy.deepcopy(member["unit"]),
            "replaces_accepted_deployment_id": member["replaces_accepted_deployment_id"],
            "dependency_overrides": copy.deepcopy(member.get("dependency_overrides", [])),
        }
        for member in _slot_members(slot)
    ]


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
    project_uuid: str
    project_id: str
    filename: str
    sha256: str
    mod_id: str
    ownership_mod_ids: tuple[str, ...]
    relative_path: str
    active: bool
    source: dict[str, str]
    expected_fabric_version: str | None = None
    retained_rollback: bool = False

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
class RetainedPredecessorMove:
    source: Path
    destination: Path
    relative_path: str
    artifact: ManagedArtifact


@dataclass(frozen=True)
class FabricModDescriptor:
    """Exact root Fabric descriptor for one enabled JAR."""

    path: Path
    relative_path: str
    filename: str
    primary_id: str
    provides: tuple[str, ...]
    version: str
    depends: dict[str, tuple[str, ...]]
    managed_project_uuid: str | None = None
    managed_project_id: str | None = None
    managed_deployment_id: str | None = None
    managed_artifact_id: str | None = None

    @property
    def ownership_ids(self) -> tuple[str, ...]:
        return (self.primary_id, *self.provides)


@dataclass(frozen=True)
class FabricPlatformProvider:
    """Exact Fabric builtin provider derived from current launch authority."""

    mod_id: str
    version: str
    authority: str


@dataclass(frozen=True)
class PlatformAttestation:
    """One coherent snapshot of the target's authoritative launch inputs."""

    providers: tuple[FabricPlatformProvider, ...]
    evidence: dict[str, Any]
    fingerprint: str

    @property
    def provider_map(self) -> dict[str, FabricPlatformProvider]:
        return {provider.mod_id: provider for provider in self.providers}

    def receipt(self) -> dict[str, Any]:
        return {
            "status": "FABRIC_PLATFORM_PROVIDERS_ATTESTED",
            "fingerprint": self.fingerprint,
            "providers": [
                {
                    "id": provider.mod_id,
                    "version": provider.version,
                    "authority": provider.authority,
                }
                for provider in self.providers
            ],
            "evidence": copy.deepcopy(self.evidence),
        }


@dataclass(frozen=True)
class PhysicalPlan:
    mode: str
    current_state: dict[str, Any] | None
    desired_state: dict[str, Any]
    current_artifacts: tuple[ManagedArtifact, ...]
    desired_artifacts: tuple[ManagedArtifact, ...]
    writes: tuple[FileAction, ...]
    retained_predecessor_moves: tuple[RetainedPredecessorMove, ...]
    removals: tuple[Path, ...]
    unchanged: tuple[str, ...]
    title_projection: dict[str, Any]
    dependency_resolution: dict[str, Any]
    profile_use_preflight: dict[str, Any]
    batch_operation: dict[str, Any] | None
    finalize_verified_profile: bool = False
    transition_at: str | None = None
    changed_slots: tuple[str, ...] = ()

    def summary(self, *, dry_run: bool) -> dict[str, Any]:
        def slots_by_project(state: dict[str, Any] | None) -> dict[str, str]:
            if state is None:
                return {}
            return {
                member["unit"]["project_uuid"]: label
                for label in ("A", "B")
                if state["slots"][label] is not None
                for member in _slot_members(state["slots"][label])
            }

        def slot_snapshot(
            state: dict[str, Any] | None,
            label: str,
            *,
            finalized_preview: bool = False,
        ) -> dict[str, Any] | None:
            if state is None or state["slots"][label] is None:
                return None
            slot = state["slots"][label]
            return {
                "deployment_state": (
                    "READY_TO_TEST_VERIFIED"
                    if finalized_preview
                    else slot["deployment"]["state"]
                ),
                "members": [
                    {
                        "project_uuid": member["unit"]["project_uuid"],
                        "project_id": member["unit"]["project_id"],
                        "deployment_id": member["unit"]["deployment_id"],
                        "version": member["unit"]["version"],
                        "runtime_result": member["runtime_result"]["classification"],
                        "replaces_accepted_deployment_id": member["replaces_accepted_deployment_id"],
                        "dependency_overrides": copy.deepcopy(member.get("dependency_overrides", [])),
                    }
                    for member in _slot_members(slot)
                ],
            }

        def slot_member_results(state: dict[str, Any] | None) -> dict[str, dict[str, Any]]:
            if state is None:
                return {}
            result: dict[str, dict[str, Any]] = {}
            for label in ("A", "B"):
                slot = state["slots"][label]
                if slot is None:
                    continue
                for member in _slot_members(slot):
                    unit = member["unit"]
                    result[unit["deployment_id"]] = {
                        "slot": label,
                        "project_uuid": unit["project_uuid"],
                        "project_id": unit["project_id"],
                        "deployment_id": unit["deployment_id"],
                        "version": unit["version"],
                        "runtime_result": copy.deepcopy(member["runtime_result"]),
                    }
            return result

        current_slots = slots_by_project(self.current_state)
        desired_slots = slots_by_project(self.desired_state)
        projects = sorted(set(current_slots).union(desired_slots))
        current_results = slot_member_results(self.current_state)
        desired_results = slot_member_results(self.desired_state)
        desired_accepted_deployments = {
            member["unit"]["deployment_id"]
            for member in self.desired_state["accepted_baseline"]["members"]
        }
        displaced_result_preimages: list[dict[str, Any]] = []
        for deployment_id, record in current_results.items():
            if deployment_id in desired_results:
                continue
            if deployment_id in desired_accepted_deployments:
                disposition = "PROMOTED"
            elif record["project_uuid"] in desired_slots:
                disposition = "REPLACED"
            else:
                disposition = "REMOVED"
            displaced_result_preimages.append(
                {
                    **copy.deepcopy(record),
                    "disposition": disposition,
                }
            )
        preserved_companion_results = [
            {
                "project_uuid": before["project_uuid"],
                "project_id": before["project_id"],
                "deployment_id": deployment_id,
                "version": before["version"],
                "before_slot": before["slot"],
                "after_slot": desired_results[deployment_id]["slot"],
                "runtime_result": copy.deepcopy(desired_results[deployment_id]["runtime_result"]),
            }
            for deployment_id, before in current_results.items()
            if deployment_id in desired_results
            and before["runtime_result"] == desired_results[deployment_id]["runtime_result"]
        ]
        deferred_final_digest = dry_run and self.finalize_verified_profile
        title_projection = copy.deepcopy(self.title_projection)
        if deferred_final_digest:
            title_projection["state_digest"] = None
            title_projection["state_digest_status"] = "DEFERRED_UNTIL_POST_DEPLOYMENT_VERIFICATION"
        return {
            "mode": self.mode,
            "dry_run": dry_run,
            "target_state_revision": self.desired_state["revision"],
            "target_state_digest": None if deferred_final_digest else state_digest(self.desired_state),
            "preverification_state_digest": (
                state_digest(self.desired_state) if deferred_final_digest else None
            ),
            "target_state_digest_status": (
                "DEFERRED_UNTIL_POST_DEPLOYMENT_VERIFICATION"
                if deferred_final_digest
                else "FINAL"
            ),
            "activation": self.desired_state["activation"],
            "writes": [
                {
                    "path": action.relative_path,
                    "sha256": action.artifact.sha256,
                    "source": str(action.source),
                    "project_uuid": action.artifact.project_uuid,
                    "project_id": action.artifact.project_id,
                    "deployment_id": action.artifact.deployment_id,
                    "artifact_id": action.artifact.artifact_id,
                    "primary_mod_id": action.artifact.mod_id,
                    "ownership_mod_ids": list(action.artifact.ownership_mod_ids),
                    "disposition": "ACTIVE" if action.artifact.active else "DISABLED",
                }
                for action in self.writes
            ],
            "retained_predecessor_moves": [
                {
                    "source": str(move.source),
                    "path": move.relative_path,
                    "sha256": move.artifact.sha256,
                }
                for move in self.retained_predecessor_moves
            ],
            "removals": [path.name for path in self.removals],
            "removal_details": [
                {
                    "path": artifact.relative_path,
                    "sha256": artifact.sha256,
                    "project_uuid": artifact.project_uuid,
                    "project_id": artifact.project_id,
                    "deployment_id": artifact.deployment_id,
                    "artifact_id": artifact.artifact_id,
                    "ownership_mod_ids": list(artifact.ownership_mod_ids),
                    "prior_disposition": "ACTIVE" if artifact.active else "DISABLED",
                }
                for path in self.removals
                for artifact in self.current_artifacts
                if os.path.normcase(str(path)).casefold()
                == os.path.normcase(str(Path(artifact.relative_path))).casefold()
                or path.name.casefold() == Path(artifact.relative_path).name.casefold()
            ],
            "unchanged": list(self.unchanged),
            "managed_files": [artifact.ledger_record() for artifact in self.desired_artifacts],
            "dependency_resolution": copy.deepcopy(self.dependency_resolution),
            "profile_use_preflight": copy.deepcopy(self.profile_use_preflight),
            "slot_changes": {
                label: {
                    "before": slot_snapshot(self.current_state, label),
                    "after": slot_snapshot(
                        self.desired_state,
                        label,
                        finalized_preview=(
                            self.finalize_verified_profile and label in self.changed_slots
                        ),
                    ),
                }
                for label in ("A", "B")
            },
            "lifecycle_changes": [
                {
                    "project_uuid": project_uuid,
                    "before": "TESTING" if project_uuid in current_slots else "ACTIVE",
                    "after": "TESTING" if project_uuid in desired_slots else "ACTIVE",
                    "before_slot": current_slots.get(project_uuid),
                    "after_slot": desired_slots.get(project_uuid),
                }
                for project_uuid in projects
            ],
            "displaced_member_runtime_result_preimages": displaced_result_preimages,
            "preserved_companion_runtime_results": preserved_companion_results,
            "retained_rollbacks": [
                {
                    **artifact.ledger_record(),
                    "source": copy.deepcopy(artifact.source),
                    "enabled_sibling": PurePosixPath(
                        PurePosixPath(artifact.relative_path).parent,
                        artifact.filename,
                    ).as_posix(),
                }
                for artifact in self.desired_artifacts
                if artifact.retained_rollback
            ],
            "title_projection": title_projection,
            "finalization": (
                {
                    "mode": "AFTER_PHYSICAL_HASH_AND_DEPENDENCY_VERIFICATION",
                    "target_deployment_state": "READY_TO_TEST_VERIFIED",
                    "state_revision_delta": 1,
                    "timestamp": self.transition_at,
                    "changed_slots": list(self.changed_slots),
                }
                if self.finalize_verified_profile
                else None
            ),
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
        java_property_probe: JavaPropertyProbe | None = None,
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
        self._java_property_probe = java_property_probe or _probe_java_properties
        # Supplying both catalogs explicitly is the narrow test-only escape
        # hatch for isolated fixtures which have no project manifests. Normal
        # CLI construction always retains the authoritative manifest catalog.
        self.repository_statuses = copy.deepcopy(statuses)

    @classmethod
    def from_config(
        cls,
        config_path: Path = DEFAULT_CONFIG,
        target: Path | None = None,
        project_index: dict[str, str] | None = None,
        project_display_names: dict[str, str] | None = None,
        java_property_probe: JavaPropertyProbe | None = None,
    ) -> "PhysicalManager":
        return cls(
            ManagerConfig.load(Path(config_path)),
            target=target,
            project_index=project_index,
            project_display_names=project_display_names,
            java_property_probe=java_property_probe,
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

    def _assert_profile_not_in_use(self) -> dict[str, Any]:
        """Fail closed when a Minecraft process names the dedicated profile."""

        normalized_parts = {part.casefold() for part in self.target.parts}
        if not {"modrinthapp", "profiles"}.issubset(normalized_parts):
            # Isolated test fixtures deliberately use temporary directories;
            # production configuration is always a Modrinth profile.
            return {
                "status": "NOT_APPLICABLE_NON_MODRINTH_FIXTURE",
                "target": str(self.target),
                "matching_processes": [],
            }
        matches = _windows_processes_using_profile(self.target)
        if matches:
            details = ", ".join(
                f"{item['name']} (PID {item['pid']})" for item in matches
            )
            raise ManagerError(
                "dedicated Minecraft profile is actively in use; physical mutation refused: "
                f"{self.target}; matching processes: {details}"
            )
        return {
            "status": "DEDICATED_PROFILE_NOT_IN_USE",
            "target": str(self.target),
            "matching_processes": [],
        }

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
            for retained in member.get("retained_rollbacks", []):
                artifacts.extend(
                    self._unit_artifacts(
                        retained["unit"],
                        False,
                        seen_paths,
                        retained_rollback=True,
                    )
                )
        for label in ("A", "B"):
            slot = state["slots"][label]
            if slot is not None:
                for member in _slot_members(slot):
                    unit = member["unit"]
                    if state["schema_version"] == 2 and len(unit["artifacts"]) != 1:
                        if slot["deployment"]["state"] == "READY_TO_TEST_VERIFIED":
                            raise ManagerError(
                                f"schema-v2 ready slot {label} member {unit['project_id']} must have exactly one "
                                "artifact so its visible version can be bound to one embedded Fabric version"
                            )
                        expected_fabric_version = None
                    else:
                        expected_fabric_version = unit["version"] if state["schema_version"] == 2 else None
                    artifacts.extend(
                        self._unit_artifacts(
                            unit,
                            True,
                            seen_paths,
                            expected_fabric_version=expected_fabric_version,
                        )
                    )

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
            project_uuid=str(uuid.uuid5(uuid.NAMESPACE_URL, "mynx-title-marker-project")),
            project_id="workbench-test-marker",
            filename=marker.filename,
            sha256=marker.sha256,
            mod_id=marker.mod_id,
            ownership_mod_ids=(marker.mod_id,),
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

    def _legacy_schema_v1_title_projection(self, state: dict[str, Any]) -> dict[str, Any]:
        """Reproduce the exact pre-cohort projection for a legacy V1 state."""

        if state.get("schema_version") != 1:
            raise ManagerError("legacy title projection can only be derived from runtime-state schema_version 1")
        projection = self._title_projection(state)
        for label in ("A", "B"):
            slot = projection["slots"][label]
            if slot["occupied"]:
                # The historical one-project renderer exposed these member
                # fields directly and had neither the cohort array nor the
                # newly explicit canonical version field.
                del slot["members"]
                del slot["version"]
        return projection

    def _title_projection_status(self, projection: Any, state: dict[str, Any]) -> str | None:
        """Classify only exact current or exact schema-V1 legacy projections."""

        if projection == self._title_projection(state):
            return "SYNCHRONIZED"
        if (
            state.get("schema_version") == 1
            and projection == self._legacy_schema_v1_title_projection(state)
        ):
            return "LEGACY_MIGRATION_REQUIRED"
        return None

    def _verify_title_projection(self, state: dict[str, Any]) -> dict[str, Any]:
        try:
            projection = json.loads(self.title_projection_path.read_text(encoding="utf-8"))
        except FileNotFoundError as exc:
            raise ManagerError(f"V2 title display projection is missing: {self.title_projection_path}") from exc
        except (OSError, json.JSONDecodeError) as exc:
            raise ManagerError(f"cannot load V2 title display projection {self.title_projection_path}: {exc}") from exc
        status = self._title_projection_status(projection, state)
        if status is None:
            raise ManagerError("V2 title display projection does not match canonical runtime state")
        return {
            "status": status,
            "path": self.title_projection_path.name,
            "sha256": _sha256(self.title_projection_path),
            "state_revision": projection["state_revision"],
            "lines": copy.deepcopy(projection["lines"]),
        }

    def _unit_artifacts(
        self,
        unit: dict[str, Any],
        active: bool,
        seen_paths: set[str],
        *,
        expected_fabric_version: str | None = None,
        retained_rollback: bool = False,
    ) -> list[ManagedArtifact]:
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
            if not isinstance(ownership, list) or not ownership:
                raise ManagerError(f"managed MOD {filename} must have at least one mod:<fabric_mod_id> ownership key")
            ownership_mod_ids: list[str] = []
            for ownership_key in ownership:
                if not isinstance(ownership_key, str) or not ownership_key.startswith("mod:"):
                    raise ManagerError(f"managed MOD {filename} has an invalid mod:<fabric_mod_id> ownership key")
                ownership_mod_id = ownership_key[4:]
                if not ownership_mod_id:
                    raise ManagerError(f"managed MOD {filename} has an empty Fabric mod id")
                ownership_mod_ids.append(ownership_mod_id)
            # Preserve the V3 ledger contract: the first key is the primary
            # Fabric ID and any later keys are exact `provides` aliases.
            mod_id = ownership_mod_ids[0]
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
                    project_uuid=unit["project_uuid"],
                    project_id=unit["project_id"],
                    filename=filename,
                    sha256=raw["sha256"].lower(),
                    mod_id=mod_id,
                    ownership_mod_ids=tuple(ownership_mod_ids),
                    relative_path=relative_path,
                    active=active,
                    source={"type": source["type"], "path": source["path"]},
                    expected_fabric_version=expected_fabric_version,
                    retained_rollback=retained_rollback,
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
        declared_ids = frozenset(artifact.ownership_mod_ids)
        descriptor = _read_fabric_descriptor(
            path,
            strict_provides=len(declared_ids) > 1,
            relative_path=artifact.relative_path,
            artifact=artifact,
        )
        if (
            artifact.expected_fabric_version is not None
            and descriptor.version != artifact.expected_fabric_version
        ):
            raise ManagerError(
                f"embedded Fabric version mismatch for {path}: expected slot member version "
                f"{artifact.expected_fabric_version}, found {descriptor.version}"
            )
        ids = frozenset(descriptor.ownership_ids)
        if artifact.mod_id not in ids:
            raise ManagerError(
                f"Fabric mod ownership mismatch for {path}: expected primary id {artifact.mod_id}, found {sorted(ids)}"
            )
        primary = descriptor.primary_id
        if primary != artifact.mod_id:
            raise ManagerError(
                f"Fabric primary mod id mismatch for {path}: expected {artifact.mod_id}, found {primary}"
            )
        if primary not in declared_ids:
            raise ManagerError(f"Fabric primary mod id {primary} is absent from declared ownership for {path}")
        if len(declared_ids) > 1 and ids != declared_ids:
            missing = sorted(declared_ids - ids)
            undeclared = sorted(ids - declared_ids)
            raise ManagerError(
                f"Fabric ownership declaration mismatch for {path}: "
                f"missing provided aliases {missing}, undeclared manifest ids {undeclared}"
            )
        return ids

    def _planned_enabled_descriptors(
        self,
        current_artifacts: Sequence[ManagedArtifact],
        desired_artifacts: Sequence[ManagedArtifact],
    ) -> tuple[FabricModDescriptor, ...]:
        """Build the exact proposed enabled-JAR graph without mutating the target."""

        current_paths = {artifact.relative_path.casefold() for artifact in current_artifacts}
        desired_active_paths = {
            artifact.relative_path.casefold()
            for artifact in desired_artifacts
            if artifact.active
        }
        planned_disabled_adopted_sources = {
            PurePosixPath(artifact.source["path"]).as_posix().casefold()
            for artifact in desired_artifacts
            if not artifact.active and artifact.source["type"] == "ADOPTED_TARGET"
        }
        current_by_artifact = {artifact.artifact_id: artifact for artifact in current_artifacts}
        descriptors: list[FabricModDescriptor] = []
        for artifact in desired_artifacts:
            if not artifact.active:
                continue
            source: Path | None = None
            prior = current_by_artifact.get(artifact.artifact_id)
            if prior is not None and prior.sha256 == artifact.sha256:
                prior_path = self._destination(prior)
                if prior_path.exists():
                    source = prior_path
            if source is None:
                source = self._source_path(artifact)
            if not source.exists():
                source = self._destination(artifact)
            if not source.exists():
                raise ManagerError(
                    f"missing proposed artifact bytes for {artifact.project_id}/{artifact.filename}: {source}"
                )
            self._verify_artifact_file(source, artifact)
            descriptors.append(
                _read_fabric_descriptor(
                    source,
                    strict_provides=len(artifact.ownership_mod_ids) > 1,
                    relative_path=artifact.relative_path,
                    artifact=artifact,
                )
            )

        try:
            entries = tuple(self.mods.iterdir())
        except OSError as exc:
            raise ManagerError(f"cannot scan managed mods directory {self.mods}: {exc}") from exc
        for path in entries:
            if not path.is_file() or not path.name.casefold().endswith(".jar"):
                continue
            relative = PurePosixPath(self.config.mods_directory, path.name).as_posix()
            normalized = relative.casefold()
            if (
                normalized in current_paths
                or normalized in desired_active_paths
                or normalized in planned_disabled_adopted_sources
            ):
                continue
            if path.is_symlink():
                raise ManagerError(f"mods inventory contains a symbolic link: {path}")
            try:
                descriptors.append(_read_fabric_descriptor(path, relative_path=relative))
            except ManagerError as exc:
                if "root fabric.mod.json" in str(exc) or "cannot read Fabric manifest" in str(exc):
                    # Preserve foreign non-Fabric libraries exactly as the
                    # inventory verifier does; they cannot own Fabric IDs.
                    continue
                raise
        return tuple(descriptors)

    def _physical_enabled_descriptors(
        self,
        desired_artifacts: Sequence[ManagedArtifact],
    ) -> tuple[FabricModDescriptor, ...]:
        """Read every physically enabled root Fabric descriptor after apply."""

        managed_by_path = {
            artifact.relative_path.casefold(): artifact
            for artifact in desired_artifacts
            if artifact.active
        }
        descriptors: list[FabricModDescriptor] = []
        try:
            entries = tuple(self.mods.iterdir())
        except OSError as exc:
            raise ManagerError(f"cannot scan managed mods directory {self.mods}: {exc}") from exc
        for path in entries:
            if not path.is_file() or not path.name.casefold().endswith(".jar"):
                continue
            if path.is_symlink():
                raise ManagerError(f"mods inventory contains a symbolic link: {path}")
            relative = PurePosixPath(self.config.mods_directory, path.name).as_posix()
            artifact = managed_by_path.get(relative.casefold())
            try:
                descriptors.append(
                    _read_fabric_descriptor(
                        path,
                        strict_provides=(artifact is not None and len(artifact.ownership_mod_ids) > 1),
                        relative_path=relative,
                        artifact=artifact,
                    )
                )
            except ManagerError as exc:
                if artifact is None and (
                    "root fabric.mod.json" in str(exc) or "cannot read Fabric manifest" in str(exc)
                ):
                    continue
                raise
        return tuple(descriptors)

    @staticmethod
    def _required_platform_ids(
        descriptors: Sequence[FabricModDescriptor],
    ) -> frozenset[str]:
        dependency_ids = {
            dependency_id
            for descriptor in descriptors
            if descriptor.managed_project_id is not None
            for dependency_id in descriptor.depends
            if dependency_id in _FABRIC_PLATFORM_DEPENDENCIES
        }
        managed_ownership_ids = {
            ownership_id
            for descriptor in descriptors
            if descriptor.managed_project_id is not None
            for ownership_id in descriptor.ownership_ids
            if ownership_id in _FABRIC_PLATFORM_DEPENDENCIES
        }
        return frozenset(dependency_ids.union(managed_ownership_ids))

    def _attest_platform_providers(
        self,
        required_ids: Iterable[str],
    ) -> PlatformAttestation | None:
        """Derive exact Fabric builtins from the configured target's launch authority.

        This is intentionally a single-row, parameterized read of Modrinth's
        current applied content set.  It never enumerates profiles.  Java is
        independently probed through the executable selected for that exact
        content set, without launching Minecraft.
        """

        required = frozenset(required_ids)
        unknown = required.difference(_FABRIC_PLATFORM_DEPENDENCIES)
        if unknown:
            raise ManagerError(f"unsupported Fabric platform provider IDs: {sorted(unknown)}")
        if not required:
            return None
        if self.target.parent.name.casefold() != "profiles":
            raise ManagerError(
                "cannot attest Fabric platform providers: configured dedicated profile "
                "is not an exact Modrinth profiles/<profile> target"
            )
        app_root = self.target.parent.parent.resolve(strict=False)
        app_database = app_root / "app.db"
        if not app_database.exists() or not app_database.is_file() or app_database.is_symlink():
            raise ManagerError(
                "cannot attest Fabric platform providers: Modrinth launch authority "
                f"is missing or unsafe: {app_database}"
            )

        try:
            connection = sqlite3.connect(
                app_database.resolve(strict=True).as_uri() + "?mode=ro",
                uri=True,
            )
        except (OSError, sqlite3.Error) as exc:
            raise ManagerError(f"cannot open Modrinth launch authority read-only: {exc}") from exc
        try:
            connection.row_factory = sqlite3.Row
            connection.execute("PRAGMA query_only=ON")
            required_tables = {
                "instances",
                "instance_content_sets",
                "instance_launch_overrides",
                "java_versions",
            }
            present_tables = {
                row[0]
                for row in connection.execute(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name IN (?, ?, ?, ?)",
                    tuple(sorted(required_tables)),
                )
            }
            if present_tables != required_tables:
                raise ManagerError(
                    "cannot attest Fabric platform providers: unsupported or incomplete "
                    f"Modrinth launch-authority schema (missing {sorted(required_tables - present_tables)})"
                )
            try:
                rows = connection.execute(
                    """
                    SELECT
                        i.id AS instance_id,
                        i.path AS instance_path,
                        i.install_stage AS install_stage,
                        i.applied_content_set_id AS applied_content_set_id,
                        cs.id AS content_set_id,
                        cs.status AS content_set_status,
                        cs.game_version AS game_version,
                        cs.loader AS loader,
                        cs.loader_version AS loader_version,
                        cs.modified AS content_set_modified,
                        json_extract(o.overrides, '$.java_path') AS java_path
                    FROM instances AS i
                    JOIN instance_content_sets AS cs
                      ON cs.id = i.applied_content_set_id
                     AND cs.instance_id = i.id
                    LEFT JOIN instance_launch_overrides AS o
                      ON o.instance_id = i.id
                    WHERE i.path = ?
                    """,
                    (self.target.name,),
                ).fetchall()
            except sqlite3.Error as exc:
                raise ManagerError(
                    "cannot attest Fabric platform providers: Modrinth launch-authority "
                    f"schema/query failed: {exc}"
                ) from exc
            if len(rows) != 1:
                raise ManagerError(
                    "cannot attest Fabric platform providers: exact configured dedicated "
                    f"profile identity {self.target.name!r} matched {len(rows)} applied content sets"
                )
            row = rows[0]
            if row["instance_path"] != self.target.name:
                raise ManagerError("Modrinth launch authority returned a mismatched profile identity")
            if row["install_stage"] != "installed":
                raise ManagerError(
                    "cannot attest Fabric platform providers: exact dedicated profile is not installed "
                    f"(install_stage={row['install_stage']!r})"
                )
            if row["content_set_status"] != "available":
                raise ManagerError(
                    "cannot attest Fabric platform providers: exact applied content set is not available "
                    f"(status={row['content_set_status']!r})"
                )
            game_version = row["game_version"]
            loader = row["loader"]
            loader_version = row["loader_version"]
            if loader != "fabric":
                raise ManagerError(
                    "cannot attest Fabric platform providers: exact dedicated profile loader "
                    f"is {loader!r}, not 'fabric'"
                )
            if isinstance(loader_version, str) and loader_version.casefold() in {"latest", "stable"}:
                raise ManagerError(
                    "cannot attest Fabric platform providers: Fabric Loader selection is an unresolved alias"
                )
            for value, label in (
                (game_version, "Minecraft game version"),
                (loader_version, "Fabric Loader version"),
            ):
                parsed = _parse_fabric_semantic_version(value, store_wildcards=False) if isinstance(value, str) else None
                if parsed is None or parsed.prerelease is not None or parsed.build is not None:
                    raise ManagerError(
                        f"cannot attest Fabric platform providers: {label} is not an exact release semantic version: {value!r}"
                    )
            version_id = f"{game_version}-{loader_version}"
            if re.fullmatch(r"[0-9A-Za-z._+~-]+", version_id) is None:
                raise ManagerError("cannot attest Fabric platform providers: unsafe cached metadata identity")
            metadata_path = app_root / "meta" / "versions" / version_id / f"{version_id}.json"
            if not metadata_path.exists() or not metadata_path.is_file() or metadata_path.is_symlink():
                raise ManagerError(
                    "cannot attest Fabric platform providers: exact cached launch metadata is missing or unsafe: "
                    + str(metadata_path)
                )
            try:
                metadata_bytes = metadata_path.read_bytes()
            except OSError as exc:
                raise ManagerError(f"cannot read exact cached launch metadata {metadata_path}: {exc}") from exc
            if len(metadata_bytes) > 16 * 1024 * 1024:
                raise ManagerError("exact cached launch metadata is unreasonably large")
            try:
                metadata = json.loads(metadata_bytes.decode("utf-8"))
            except (UnicodeDecodeError, json.JSONDecodeError) as exc:
                raise ManagerError(f"exact cached launch metadata is invalid JSON: {exc}") from exc
            if not isinstance(metadata, dict) or metadata.get("id") != version_id:
                raise ManagerError(
                    "cannot attest Fabric platform providers: cached launch metadata identity "
                    f"does not equal {version_id!r}"
                )
            java_metadata = metadata.get("javaVersion")
            required_java_major = java_metadata.get("majorVersion") if isinstance(java_metadata, dict) else None
            if not isinstance(required_java_major, int) or isinstance(required_java_major, bool) or required_java_major < 1:
                raise ManagerError(
                    "cannot attest Fabric platform providers: cached launch metadata has no exact positive Java major"
                )
            libraries = metadata.get("libraries")
            if not isinstance(libraries, list):
                raise ManagerError("cannot attest Fabric platform providers: cached launch metadata libraries is not an array")
            loader_coordinates = [
                item["name"]
                for item in libraries
                if isinstance(item, dict)
                and isinstance(item.get("name"), str)
                and item["name"].startswith("net.fabricmc:fabric-loader:")
            ]
            expected_loader_coordinate = f"net.fabricmc:fabric-loader:{loader_version}"
            if loader_coordinates != [expected_loader_coordinate]:
                raise ManagerError(
                    "cannot attest Fabric platform providers: cached Fabric Loader coordinate "
                    f"does not uniquely equal {expected_loader_coordinate!r}; found {loader_coordinates}"
                )

            override_java = row["java_path"]
            configured_java_source: str
            configured_java_full_version: str | None
            if isinstance(override_java, str) and override_java.strip():
                selected_java = Path(override_java)
                configured_java_source = "INSTANCE_LAUNCH_OVERRIDE"
                configured_java_full_version = None
            elif override_java is None or override_java == "":
                try:
                    java_rows = connection.execute(
                        "SELECT full_version, path FROM java_versions WHERE major_version = ?",
                        (required_java_major,),
                    ).fetchall()
                except sqlite3.Error as exc:
                    raise ManagerError(f"cannot resolve configured Modrinth Java runtime: {exc}") from exc
                if len(java_rows) != 1:
                    raise ManagerError(
                        "cannot attest Fabric platform providers: required Java major "
                        f"{required_java_major} resolves to {len(java_rows)} configured runtimes"
                    )
                selected_java = Path(java_rows[0]["path"])
                configured_java_source = "MODRINTH_JAVA_VERSION"
                configured_java_full_version = java_rows[0]["full_version"]
            else:
                raise ManagerError("cannot attest Fabric platform providers: configured Java override is not a path string")
        finally:
            connection.close()

        def validated_java_path(candidate: Path, label: str, *, allowed_names: frozenset[str]) -> Path:
            """Validate one launch-authority executable before resolving or reading it."""

            if not candidate.is_absolute():
                raise ManagerError(
                    f"cannot attest Fabric platform providers: {label} path is not absolute"
                )
            lexical = Path(os.path.abspath(candidate))
            protected = Path(os.path.abspath(self.config.protected_profile))
            if _lexical_paths_equal(lexical, protected) or _is_lexically_within(lexical, protected):
                raise ManagerError(
                    "cannot attest Fabric platform providers: selected Java executable "
                    "resolves lexically into the protected gameplay profile"
                )
            if lexical.name.casefold() not in allowed_names:
                raise ManagerError(
                    "cannot attest Fabric platform providers: selected Java executable must be "
                    + " or ".join(sorted(allowed_names))
                )

            # The executable may legitimately be installed anywhere, including
            # outside Modrinth's app root. Walk from its filesystem anchor so no
            # intervening symlink, junction, or other reparse point is trusted.
            anchor = Path(lexical.anchor)
            if not lexical.anchor:
                raise ManagerError(
                    f"cannot attest Fabric platform providers: {label} has no filesystem anchor"
                )
            _assert_no_reparse_components(lexical, label, root=anchor)
            if not lexical.exists() or not lexical.is_file():
                raise ManagerError(
                    f"cannot attest Fabric platform providers: {label} is missing or unsafe: {lexical}"
                )
            try:
                resolved = lexical.resolve(strict=True)
            except OSError as exc:
                raise ManagerError(
                    f"cannot attest Fabric platform providers: cannot resolve {label}: {exc}"
                ) from exc
            protected_resolved = self.config.protected_profile.resolve(strict=False)
            if _paths_equal(resolved, protected_resolved) or _is_within(resolved, protected_resolved):
                raise ManagerError(
                    "cannot attest Fabric platform providers: selected Java executable "
                    "resolves into the protected gameplay profile"
                )
            return resolved

        selected_names = (
            frozenset({"java.exe", "javaw.exe"})
            if os.name == "nt"
            else frozenset({"java"})
        )
        selected_java = validated_java_path(
            selected_java,
            "selected Java executable",
            allowed_names=selected_names,
        )
        probe_java = selected_java
        if selected_java.name.casefold() == "javaw.exe":
            probe_java = validated_java_path(
                selected_java.with_name("java.exe"),
                "selected javaw.exe sibling java.exe",
                allowed_names=frozenset({"java.exe"}),
            )
        selected_java_sha256 = _sha256(selected_java)
        probe_java_sha256 = _sha256(probe_java)
        properties = self._java_property_probe(probe_java)
        if (
            _sha256(selected_java) != selected_java_sha256
            or _sha256(probe_java) != probe_java_sha256
        ):
            raise ManagerError(
                "cannot attest Fabric platform providers: selected Java executable bytes changed during probing"
            )
        java_specification_version = properties.get("java.specification.version")
        java_full_version = properties.get("java.version")
        java_home = properties.get("java.home")
        if not all(isinstance(value, str) and value.strip() for value in (
            java_specification_version,
            java_full_version,
            java_home,
        )):
            raise ManagerError(
                "cannot attest Fabric platform providers: Java probe did not report "
                "java.specification.version, java.version, and java.home"
            )
        normalized_java_specification = re.sub(r"^1\.", "", java_specification_version)
        parsed_java_specification = _parse_fabric_semantic_version(
            normalized_java_specification,
            store_wildcards=False,
        )
        if (
            parsed_java_specification is None
            or parsed_java_specification.prerelease is not None
            or parsed_java_specification.build is not None
            or not parsed_java_specification.components
            or parsed_java_specification.components[0] != required_java_major
        ):
            raise ManagerError(
                "cannot attest Fabric platform providers: probed Java specification version "
                f"{java_specification_version!r} disagrees with required major {required_java_major}"
            )
        if (
            configured_java_full_version is not None
            and configured_java_full_version
            not in {java_full_version, normalized_java_specification}
        ):
            raise ManagerError(
                "cannot attest Fabric platform providers: configured Java version identity "
                f"{configured_java_full_version!r} agrees with neither probed specification "
                f"{normalized_java_specification!r} nor full version {java_full_version!r}"
            )
        probed_java_home = Path(java_home).resolve(strict=False)
        selected_java_home = selected_java.parent.parent.resolve(strict=False)
        if not _paths_equal(probed_java_home, selected_java_home):
            raise ManagerError(
                "cannot attest Fabric platform providers: probed java.home "
                f"{probed_java_home} does not own selected executable {selected_java}"
            )

        evidence = {
            "source_classification": "MODRINTH_EXACT_DEDICATED_PROFILE_LAUNCH_AUTHORITY",
            "required_provider_ids": sorted(required),
            "app_database": {
                "path": str(app_database.resolve(strict=True)),
                "schema": "MODERN_APPLIED_CONTENT_SET",
                "query_scope": "EXACT_PROFILE_DIRECTORY_NAME",
            },
            "target": {
                "profile_path": str(self.target),
                "profile_directory_name": self.target.name,
                "instance_id": _sqlite_identity(row["instance_id"]),
                "install_stage": row["install_stage"],
                "applied_content_set_id": _sqlite_identity(row["applied_content_set_id"]),
                "content_set_id": _sqlite_identity(row["content_set_id"]),
                "content_set_status": row["content_set_status"],
                "content_set_modified": row["content_set_modified"],
            },
            "minecraft": {
                "version": game_version,
                "authority": "APPLIED_CONTENT_SET_GAME_VERSION",
            },
            "fabricloader": {
                "version": loader_version,
                "authority": "APPLIED_CONTENT_SET_AND_CACHED_LOADER_COORDINATE",
                "loader": loader,
                "coordinate": expected_loader_coordinate,
            },
            "cached_launch_metadata": {
                "path": str(metadata_path.resolve(strict=True)),
                "id": version_id,
                "sha256": hashlib.sha256(metadata_bytes).hexdigest(),
                "required_java_major": required_java_major,
            },
            "java": {
                "version": normalized_java_specification,
                "authority": "PROBED_SELECTED_JAVA_SPECIFICATION_VERSION",
                "configured_source": configured_java_source,
                "selected_executable": str(selected_java),
                "selected_executable_sha256": selected_java_sha256,
                "probe_executable": str(probe_java.resolve(strict=True)),
                "probe_executable_sha256": probe_java_sha256,
                "configured_full_version": configured_java_full_version,
                "probed_full_version": java_full_version,
                "probed_home": str(probed_java_home),
            },
        }
        fingerprint = hashlib.sha256(
            json.dumps(evidence, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
        ).hexdigest()
        return PlatformAttestation(
            providers=(
                FabricPlatformProvider("fabricloader", loader_version, "MODRINTH_APPLIED_CONTENT_SET"),
                FabricPlatformProvider("java", normalized_java_specification, "SELECTED_JAVA_PROBE"),
                FabricPlatformProvider("minecraft", game_version, "MODRINTH_APPLIED_CONTENT_SET"),
            ),
            evidence=evidence,
            fingerprint=fingerprint,
        )

    def _resolve_dependency_graph(
        self,
        descriptors: Sequence[FabricModDescriptor],
        *,
        managed_ownership_ids: set[str],
        phase: str,
        platform_attestation: PlatformAttestation | None = None,
    ) -> dict[str, Any]:
        failure_phase = (
            "before physical mutation"
            if phase.startswith("PREFLIGHT") or phase == "TEST"
            else "during post-deployment physical verification"
        )
        platform_providers = (
            platform_attestation.provider_map
            if platform_attestation is not None
            else {}
        )
        managed_dependency_ids = {
            dependency_id
            for descriptor in descriptors
            if descriptor.managed_project_id is not None
            for dependency_id in descriptor.depends
        }
        managed_descriptor_ownership_ids = {
            ownership_id
            for descriptor in descriptors
            if descriptor.managed_project_id is not None
            for ownership_id in descriptor.ownership_ids
        }
        enforced_ownership_ids = (
            set(managed_ownership_ids)
            .union(managed_dependency_ids)
            .union(managed_descriptor_ownership_ids)
        )
        ownership_groups: dict[str, list[FabricModDescriptor]] = {}
        for descriptor in descriptors:
            for ownership_id in descriptor.ownership_ids:
                ownership_groups.setdefault(ownership_id, []).append(descriptor)

        providers: dict[str, FabricModDescriptor] = {}
        observed_out_of_scope_duplicate_groups: list[dict[str, Any]] = []
        for ownership_id, owners in sorted(ownership_groups.items()):
            platform_provider = platform_providers.get(ownership_id)
            owner_count = len(owners) + (1 if platform_provider is not None else 0)
            if owner_count > 1:
                owner_labels = [
                    f"{owner.relative_path}@{owner.version}"
                    for owner in owners
                ]
                if platform_provider is not None:
                    owner_labels.insert(
                        0,
                        f"Fabric builtin provider@{platform_provider.version}",
                    )
                if ownership_id in enforced_ownership_ids:
                    raise ManagerError(
                        "duplicate enabled Fabric ownership "
                        f"{ownership_id}: " + " and ".join(owner_labels)
                    )
                observed_owners = [
                    {
                        "classification": (
                            "MANAGED_ENABLED_JAR"
                            if owner.managed_project_id is not None
                            else "EXTERNAL_ENABLED_JAR"
                        ),
                        "filename": owner.filename,
                        "path": owner.relative_path,
                        "primary_id": owner.primary_id,
                        "provides": list(owner.provides),
                        "version": owner.version,
                        "managed_project_uuid": owner.managed_project_uuid,
                        "managed_project_id": owner.managed_project_id,
                        "managed_deployment_id": owner.managed_deployment_id,
                        "managed_artifact_id": owner.managed_artifact_id,
                    }
                    for owner in sorted(owners, key=lambda item: item.relative_path.casefold())
                ]
                if platform_provider is not None:
                    observed_owners.insert(
                        0,
                        {
                            "classification": "ATTESTED_PLATFORM_PROVIDER",
                            "filename": None,
                            "path": None,
                            "primary_id": platform_provider.mod_id,
                            "provides": [],
                            "version": platform_provider.version,
                            "managed_project_uuid": None,
                            "managed_project_id": None,
                            "managed_deployment_id": None,
                            "managed_artifact_id": None,
                            "authority": platform_provider.authority,
                        },
                    )
                observed_out_of_scope_duplicate_groups.append(
                    {
                        "classification": "OBSERVED_EXTERNAL_DUPLICATE_NOT_EVALUATED",
                        "ownership_id": ownership_id,
                        "reason": "OUTSIDE_MANAGED_OWNERSHIP_AND_DEPENDENCY_GRAPH",
                        "provider_resolution": "EXCLUDED_FROM_PROVIDER_SET",
                        "owners": observed_owners,
                    }
                )
                continue
            if owners:
                providers[ownership_id] = owners[0]

        resolutions: list[dict[str, Any]] = []
        for consumer in descriptors:
            if consumer.managed_project_id is None:
                continue
            for dependency_id, predicates in sorted(consumer.depends.items()):
                provider = providers.get(dependency_id)
                platform_provider = platform_providers.get(dependency_id)
                base = {
                    "consumer": {
                        "project_uuid": consumer.managed_project_uuid,
                        "project_id": consumer.managed_project_id,
                        "deployment_id": consumer.managed_deployment_id,
                        "artifact_id": consumer.managed_artifact_id,
                        "filename": consumer.filename,
                        "primary_id": consumer.primary_id,
                        "version": consumer.version,
                    },
                    "dependency_id": dependency_id,
                    "predicates": list(predicates),
                }
                if provider is None and platform_provider is None:
                    ownership = "managed" if dependency_id in managed_ownership_ids else "external"
                    raise ManagerError(
                        f"missing {ownership} Fabric dependency {failure_phase}: "
                        f"{consumer.managed_project_id}@{consumer.version} requires {dependency_id} "
                        f"({list(predicates)}); no exact enabled provider owns that ID. "
                        "Supply the compatible provider/companion in the same atomic cohort operation "
                        "or ensure its unmanaged provider JAR is enabled before planning"
                    )

                provider_version = (
                    platform_provider.version
                    if platform_provider is not None
                    else provider.version
                )
                matched_predicates = [
                    predicate
                    for predicate in predicates
                    if _fabric_predicate_matches(provider_version, predicate)
                ]
                if platform_provider is not None:
                    provider_record = {
                        "project_uuid": None,
                        "project_id": None,
                        "deployment_id": None,
                        "artifact_id": None,
                        "filename": None,
                        "path": None,
                        "primary_id": platform_provider.mod_id,
                        "provides": [],
                        "version": platform_provider.version,
                        "authority": platform_provider.authority,
                    }
                    provider_label = platform_provider.mod_id
                    classification = "RESOLVED_ATTESTED_PLATFORM_PROVIDER"
                else:
                    provider_record = {
                        "project_uuid": provider.managed_project_uuid,
                        "project_id": provider.managed_project_id,
                        "deployment_id": provider.managed_deployment_id,
                        "artifact_id": provider.managed_artifact_id,
                        "filename": provider.filename,
                        "path": provider.relative_path,
                        "primary_id": provider.primary_id,
                        "provides": list(provider.provides),
                        "version": provider.version,
                    }
                    provider_label = provider.managed_project_id or provider.primary_id
                    classification = (
                        "RESOLVED_MANAGED_PROVIDER"
                        if provider.managed_project_id is not None
                        else "RESOLVED_EXTERNAL_ENABLED_PROVIDER"
                    )
                if not matched_predicates:
                    remedy = (
                        "select an exact compatible dedicated-profile launch provider version"
                        if platform_provider is not None
                        else "supply a compatible companion project/version in the same atomic cohort operation"
                    )
                    raise ManagerError(
                        f"unsatisfied Fabric dependency {failure_phase}: "
                        f"{consumer.managed_project_id}@{consumer.version} requires {dependency_id} "
                        f"{list(predicates)}, but proposed {provider_label}@{provider_version} does not satisfy it; "
                        + remedy
                    )
                resolutions.append(
                    {
                        **base,
                        "classification": classification,
                        "satisfied": True,
                        "matched_predicates": matched_predicates,
                        "provider": provider_record,
                    }
                )

        enabled = [
            {
                "filename": descriptor.filename,
                "path": descriptor.relative_path,
                "primary_id": descriptor.primary_id,
                "provides": list(descriptor.provides),
                "embedded_version": descriptor.version,
                "managed_project_uuid": descriptor.managed_project_uuid,
                "managed_project_id": descriptor.managed_project_id,
                "managed_deployment_id": descriptor.managed_deployment_id,
                "managed_artifact_id": descriptor.managed_artifact_id,
            }
            for descriptor in sorted(descriptors, key=lambda item: item.relative_path.casefold())
        ]
        return {
            "status": "FABRIC_DEPENDENCY_GRAPH_VERIFIED",
            "phase": phase,
            "enabled_fabric_jar_count": len(enabled),
            "enabled_fabric_jars": enabled,
            "platform_attestation": (
                platform_attestation.receipt()
                if platform_attestation is not None
                else None
            ),
            "observed_out_of_scope_duplicate_ownership_groups": observed_out_of_scope_duplicate_groups,
            "resolutions": resolutions,
        }

    def _planned_dependency_report(
        self,
        current_artifacts: Sequence[ManagedArtifact],
        desired_artifacts: Sequence[ManagedArtifact],
    ) -> dict[str, Any]:
        descriptors = self._planned_enabled_descriptors(current_artifacts, desired_artifacts)
        managed_ids = {
            mod_id
            for artifact in (*tuple(current_artifacts), *tuple(desired_artifacts))
            for mod_id in artifact.ownership_mod_ids
        }
        platform_attestation = self._attest_platform_providers(
            self._required_platform_ids(descriptors)
        )
        return self._resolve_dependency_graph(
            descriptors,
            managed_ownership_ids=managed_ids,
            phase="PREFLIGHT_PROPOSED_ENABLED_SET",
            platform_attestation=platform_attestation,
        )

    def _physical_dependency_report(
        self,
        artifacts: Sequence[ManagedArtifact],
    ) -> dict[str, Any]:
        descriptors = self._physical_enabled_descriptors(artifacts)
        managed_ids = {
            mod_id
            for artifact in artifacts
            for mod_id in artifact.ownership_mod_ids
        }
        platform_attestation = self._attest_platform_providers(
            self._required_platform_ids(descriptors)
        )
        return self._resolve_dependency_graph(
            descriptors,
            managed_ownership_ids=managed_ids,
            phase="POST_DEPLOYMENT_ENABLED_SET",
            platform_attestation=platform_attestation,
        )

    @staticmethod
    def _assert_platform_attestation_unchanged(
        expected_report: dict[str, Any],
        actual_report: dict[str, Any],
        *,
        phase: str,
    ) -> None:
        expected = expected_report.get("platform_attestation")
        actual = actual_report.get("platform_attestation")
        if expected is None and actual is None:
            return
        expected_fingerprint = expected.get("fingerprint") if isinstance(expected, dict) else None
        actual_fingerprint = actual.get("fingerprint") if isinstance(actual, dict) else None
        if (
            not isinstance(expected_fingerprint, str)
            or not isinstance(actual_fingerprint, str)
            or expected_fingerprint != actual_fingerprint
        ):
            raise ManagerError(
                "Fabric platform launch authority changed "
                f"{phase}: expected attestation {expected_fingerprint!r}, "
                f"found {actual_fingerprint!r}; no runtime state may be committed"
            )

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
            if artifact.retained_rollback:
                enabled_sibling = self.mods / artifact.filename
                _assert_no_reparse_components(
                    enabled_sibling,
                    f"retained rollback enabled sibling {artifact.filename}",
                    root=self.target,
                )
                self._assert_target_containment(
                    enabled_sibling.resolve(strict=False),
                    f"retained rollback enabled sibling {artifact.filename}",
                )
                if enabled_sibling.exists():
                    raise ManagerError(
                        "retained rollback must remain disabled and its enabled sibling must be absent: "
                        + str(enabled_sibling)
                    )
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
        if (
            ledger["schema_version"] == 3
            and self._title_projection_status(ledger["title_projection"], ledger["runtime_state"]) is None
        ):
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

    def _current_release_comparison(
        self,
        unit: dict[str, Any],
        *,
        deployment_state: str,
        statuses: dict[str, tuple[Path, dict[str, Any]]] | None,
    ) -> dict[str, Any]:
        status_entry = statuses.get(unit["project_uuid"]) if statuses is not None else None
        if status_entry is None:
            # Explicit identity catalogs are the documented test-only mode;
            # the slot unit is the only available current release authority.
            exact = True
            current_release = {
                "version": unit["version"],
                "source_commit": unit["source_commit"],
                "artifacts": [
                    {
                        "filename": artifact["filename"],
                        "sha256": artifact["sha256"].lower(),
                    }
                    for artifact in unit["artifacts"]
                ],
                "identity_source": "EXPLICIT_TEST_CATALOG",
            }
        else:
            _, manifest = status_entry
            release = manifest["state"]["releases"]["current"]
            artifact = release["artifact"]
            current_release = {
                "version": release["version"],
                "source_commit": release["source_commit"],
                "artifacts": [
                    {
                        "filename": artifact["filename"],
                        "sha256": artifact["sha256"].lower(),
                    }
                ],
                "identity_source": "CURRENT_MANIFEST",
            }
            slot_artifacts = [
                {
                    "filename": item["filename"],
                    "sha256": item["sha256"].lower(),
                }
                for item in unit["artifacts"]
            ]
            exact = (
                unit["source_commit"] == release["source_commit"]
                and slot_artifacts == current_release["artifacts"]
            )
        if not exact:
            comparison = "OLDER_RELEASE_DEPLOYED"
        elif deployment_state == "NOT_DEPLOYED":
            comparison = "CURRENT_RELEASE_NOT_DEPLOYED"
        else:
            comparison = "CURRENT_RELEASE_DEPLOYED"
        return {
            "classification": comparison,
            "slot_matches_current_release": exact,
            "current_release": current_release,
        }

    def _physical_verification_report(
        self,
        state: dict[str, Any],
        artifacts: tuple[ManagedArtifact, ...],
        title_projection_evidence: dict[str, Any] | None,
        dependency_resolution: dict[str, Any],
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
                    "project_uuid": artifact.project_uuid,
                    "project_id": artifact.project_id,
                    "filename": artifact.filename,
                    "path": artifact.relative_path,
                    "sha256": actual_sha256,
                    "expected_fabric_version": artifact.expected_fabric_version,
                    "embedded_fabric_version": _read_fabric_descriptor(
                        path,
                        strict_provides=len(artifact.ownership_mod_ids) > 1,
                    ).version,
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
        retained_deployments = {
            retained["unit"]["deployment_id"]
            for member in state["accepted_baseline"]["members"]
            for retained in member.get("retained_rollbacks", [])
        }
        retained_records = [record for record in records if record["deployment_id"] in retained_deployments]
        title = self._render_title(state)
        statuses = self._current_repository_statuses()
        slot_evidence: dict[str, dict[str, Any] | None] = {}
        for label in ("A", "B"):
            slot = state["slots"][label]
            if slot is None:
                slot_evidence[label] = None
                continue
            title_members = title["slots"][label].get("members", [])
            title_by_uuid = {item["project_uuid"]: item for item in title_members}
            members: list[dict[str, Any]] = []
            for slot_member in _slot_members(slot):
                unit = slot_member["unit"]
                rendered = title_by_uuid.get(unit["project_uuid"], title["slots"][label])
                member_artifacts = copy.deepcopy(records_by_deployment.get(unit["deployment_id"], []))
                members.append(
                    {
                        "slot": label,
                        "deployment_id": unit["deployment_id"],
                        "project_uuid": unit["project_uuid"],
                        "project_id": unit["project_id"],
                        "project_display_name": rendered["project_display_name"],
                        "version": unit["version"],
                        "canary": rendered["canary"],
                        "source_commit": unit["source_commit"],
                        "deployment_state": slot["deployment"]["state"],
                        "runtime_result": slot_member["runtime_result"]["classification"],
                        "current_release_comparison": self._current_release_comparison(
                            unit,
                            deployment_state=slot["deployment"]["state"],
                            statuses=statuses,
                        ),
                        "artifacts": member_artifacts,
                    }
                )
            evidence = {
                "cohort_member_count": len(members),
                "deployment_state": slot["deployment"]["state"],
                "deployed_at": slot["deployment"]["deployed_at"],
                "ready_verified_at": slot["deployment"]["ready_verified_at"],
                "members": members,
            }
            if len(members) == 1:
                # Preserve the V3 one-member receipt surface while making
                # members authoritative for every slot.
                evidence.update(members[0])
            slot_evidence[label] = evidence

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
                "retained_rollbacks": {
                    "artifact_count": len(retained_records),
                    "inventory_digest": inventory_digest(retained_records),
                    "artifacts": copy.deepcopy(retained_records),
                    "enabled_siblings_absent": all(
                        not (self.mods / record["filename"]).exists() for record in retained_records
                    ),
                },
            },
            "slots": slot_evidence,
            "fabric_dependency_graph": copy.deepcopy(dependency_resolution),
            "title_display": {
                "status": (
                    title_projection_evidence["status"]
                    if title_projection_evidence is not None
                    else "LEGACY_MIGRATION_REQUIRED"
                ),
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
        dependency_resolution = self._physical_dependency_report(artifacts)
        projection = self._verify_title_projection(state) if ledger["schema_version"] == 3 else None
        return self._physical_verification_report(state, artifacts, projection, dependency_resolution)

    def _current_repository_statuses(self) -> dict[str, tuple[Path, dict[str, Any]]] | None:
        """Reload the authoritative manifest catalog when production binding is enabled."""

        if self.repository_statuses is None:
            return None
        try:
            return load_repository_statuses(self.config.repository_root)
        except ValidationError as exc:
            raise ManagerError(f"cannot reload repository project identities: {exc}") from exc

    def _validate_batch_manifest_identities(
        self,
        operation: dict[str, Any],
        *,
        refresh: bool = False,
    ) -> None:
        """Bind production direct-pass promotions to current project manifests.

        Isolated unit tests may construct a manager with both identity catalogs
        supplied explicitly. Such fixtures intentionally have no manifests;
        normal CLI construction never takes that bypass.
        """

        if operation.get("type") != "PROMOTE_USER_PASSED_BATCH":
            return
        statuses = self._current_repository_statuses() if refresh else self.repository_statuses
        if statuses is None:
            return
        members = operation.get("members")
        if not isinstance(members, list) or not members:
            raise ManagerError("user-passed batch requires a nonempty members array")
        for index, member in enumerate(members):
            label = f"batch member {index}"
            if not isinstance(member, dict) or not isinstance(member.get("unit"), dict):
                raise ManagerError(f"{label} requires a unit")
            unit = member["unit"]
            project_uuid = unit.get("project_uuid")
            status_entry = statuses.get(project_uuid)
            if status_entry is None:
                raise ManagerError(f"{label} project UUID does not resolve to a current manifest: {project_uuid}")
            manifest_path, manifest = status_entry
            identity = manifest["identity"]
            if unit.get("project_id") != identity["project_id"]:
                raise ManagerError(
                    f"{label} project_id does not match current manifest: "
                    f"expected {identity['project_id']}, found {unit.get('project_id')}"
                )
            if unit.get("project_identity_source") != "CURRENT_MANIFEST":
                raise ManagerError(f"{label} candidate must use CURRENT_MANIFEST identity")
            self._assert_unit_matches_manifest_release(
                unit,
                manifest["state"]["releases"]["current"],
                manifest_path,
                label=f"{label} candidate",
                repository_source=True,
            )

            retained_rollbacks = member.get("retained_rollbacks", [])
            if not isinstance(retained_rollbacks, list):
                raise ManagerError(f"{label}.retained_rollbacks must be an array")
            rollback_release = manifest["state"]["releases"]["rollback"]
            if len(retained_rollbacks) > 1:
                raise ManagerError(f"{label} may supply at most one retained rollback unit")
            if retained_rollbacks and rollback_release is None:
                raise ManagerError(
                    f"{label} supplies a retained rollback, but the current manifest has no rollback release"
                )
            if not retained_rollbacks:
                continue
            rollback_unit = retained_rollbacks[0]
            if not isinstance(rollback_unit, dict):
                raise ManagerError(f"{label} retained rollback must be a unit object")
            if (
                rollback_unit.get("project_uuid") != project_uuid
                or rollback_unit.get("project_id") != identity["project_id"]
            ):
                raise ManagerError(f"{label} retained rollback must use the candidate project identity")
            if rollback_unit.get("project_identity_source") != "FROZEN_LEGACY":
                raise ManagerError(f"{label} retained rollback must use FROZEN_LEGACY identity")
            self._assert_unit_matches_manifest_release(
                rollback_unit,
                rollback_release,
                manifest_path,
                label=f"{label} retained rollback",
                repository_source=False,
            )

    def _validate_deploy_profile_manifest_identities(
        self,
        operation: dict[str, Any],
        *,
        refresh: bool = False,
    ) -> None:
        """Bind every atomic cohort member to its current manifest release."""

        if operation.get("type") != "DEPLOY_PROFILE":
            return
        statuses = self._current_repository_statuses() if refresh else self.repository_statuses
        if statuses is None:
            return
        raw_slots = operation.get("slots")
        if not isinstance(raw_slots, dict):
            raise ManagerError("DEPLOY_PROFILE slots must be an object")
        for label in ("A", "B"):
            desired = raw_slots.get(label)
            if desired is None:
                continue
            if not isinstance(desired, dict):
                raise ManagerError(f"DEPLOY_PROFILE slot {label} must be an object or null")
            if set(desired) == {"candidate"}:
                declarations = [desired["candidate"]]
            elif set(desired) == {"members"} and isinstance(desired["members"], list):
                declarations = desired["members"]
            else:
                raise ManagerError(
                    f"DEPLOY_PROFILE slot {label} requires exactly candidate or members"
                )
            for index, declaration in enumerate(declarations):
                member_label = f"DEPLOY_PROFILE slot {label} member {index}"
                if not isinstance(declaration, dict) or not isinstance(declaration.get("unit"), dict):
                    raise ManagerError(f"{member_label} requires a unit")
                unit = declaration["unit"]
                project_uuid = unit.get("project_uuid")
                status_entry = statuses.get(project_uuid)
                if status_entry is None:
                    raise ManagerError(
                        f"{member_label} project UUID does not resolve to a current manifest: {project_uuid}"
                    )
                manifest_path, manifest = status_entry
                identity = manifest["identity"]
                if unit.get("project_id") != identity["project_id"]:
                    raise ManagerError(
                        f"{member_label} project_id does not match current manifest: "
                        f"expected {identity['project_id']}, found {unit.get('project_id')}"
                    )
                if unit.get("project_identity_source") != "CURRENT_MANIFEST":
                    raise ManagerError(f"{member_label} requires CURRENT_MANIFEST identity")
                release = manifest["state"]["releases"]["current"]
                release_artifact = release["artifact"]
                raw_artifacts = unit.get("artifacts")
                if not isinstance(raw_artifacts, list) or len(raw_artifacts) != 1:
                    raise ManagerError(f"{member_label} requires exactly one current release artifact")
                artifact = raw_artifacts[0]
                expected = {
                    "source_commit": release["source_commit"],
                    "filename": release_artifact["filename"],
                    "sha256": release_artifact["sha256"].lower(),
                }
                actual = {
                    "source_commit": unit.get("source_commit"),
                    "filename": artifact.get("filename"),
                    "sha256": (
                        artifact.get("sha256", "").lower()
                        if isinstance(artifact.get("sha256"), str)
                        else None
                    ),
                }
                if actual != expected:
                    raise ManagerError(
                        f"{member_label} does not exactly match current manifest release identity: "
                        f"expected {expected}, found {actual}"
                    )
                source = artifact.get("source")
                if not isinstance(source, dict) or source.get("type") != "REPOSITORY":
                    raise ManagerError(f"{member_label} must use its canonical repository/private build source")
                relative = _safe_relative(
                    source.get("path"),
                    f"{member_label} source",
                    allow_nested=True,
                )
                source_path = (self.config.repository_root / relative).resolve(strict=False)
                project_root = manifest_path.parent.resolve(strict=False)
                if not _is_within(source_path, project_root) or source_path.name.casefold() != str(
                    artifact["filename"]
                ).casefold():
                    raise ManagerError(
                        f"{member_label} source must stay inside its canonical project directory and end in "
                        f"{artifact['filename']}"
                    )

    def _validate_operation_manifest_identities(
        self,
        operation: dict[str, Any],
        *,
        refresh: bool = False,
    ) -> None:
        self._validate_batch_manifest_identities(operation, refresh=refresh)
        self._validate_deploy_profile_manifest_identities(operation, refresh=refresh)

    def _assert_unit_matches_manifest_release(
        self,
        unit: dict[str, Any],
        release: dict[str, Any],
        manifest_path: Path,
        *,
        label: str,
        repository_source: bool,
    ) -> None:
        artifact = release["artifact"]
        expected_identity = {
            "version": release["version"],
            "source_commit": release["source_commit"],
            "filename": artifact["filename"],
            "sha256": artifact["sha256"].lower(),
        }
        raw_artifacts = unit.get("artifacts")
        actual_artifact = raw_artifacts[0] if isinstance(raw_artifacts, list) and len(raw_artifacts) == 1 else None
        actual_identity = {
            "version": unit.get("version"),
            "source_commit": unit.get("source_commit"),
            "filename": actual_artifact.get("filename") if isinstance(actual_artifact, dict) else None,
            "sha256": (
                actual_artifact.get("sha256", "").lower()
                if isinstance(actual_artifact, dict) and isinstance(actual_artifact.get("sha256"), str)
                else None
            ),
        }
        if actual_identity != expected_identity:
            raise ManagerError(
                f"{label} does not exactly match manifest release identity: "
                f"expected {expected_identity}, found {actual_identity}"
            )
        assert isinstance(actual_artifact, dict)
        expected_source = (
            {
                "type": "REPOSITORY",
                "path": PurePosixPath(
                    manifest_path.parent.relative_to(self.config.repository_root),
                    "artifacts",
                    artifact["filename"],
                ).as_posix(),
            }
            if repository_source
            else {
                "type": "ADOPTED_TARGET",
                "path": PurePosixPath(self.config.mods_directory, artifact["filename"]).as_posix(),
            }
        )
        if actual_artifact.get("source") != expected_source:
            raise ManagerError(
                f"{label} source is not canonical: expected {expected_source}, "
                f"found {actual_artifact.get('source')}"
            )

    def adoption_plan(self, state: dict[str, Any] | None = None) -> PhysicalPlan:
        self._assert_no_transaction_residue()
        profile_use_preflight = self._assert_profile_not_in_use()
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
        writes, retained_moves, removals, unchanged = self._plan_delta(current_artifacts, desired_artifacts)
        dependency_resolution = self._planned_dependency_report(current_artifacts, desired_artifacts)
        return PhysicalPlan(
            mode="ADOPT",
            current_state=state,
            desired_state=active,
            current_artifacts=current_artifacts,
            desired_artifacts=desired_artifacts,
            writes=writes,
            retained_predecessor_moves=retained_moves,
            removals=removals,
            unchanged=unchanged,
            title_projection=self._title_projection(active),
            dependency_resolution=dependency_resolution,
            profile_use_preflight=profile_use_preflight,
            batch_operation=None,
        )

    def adopt(self, *, dry_run: bool = True, failure_injector: FailureInjector | None = None) -> dict[str, Any]:
        """Verify an exact populated GATED profile, then activate and ledger it."""

        if dry_run:
            return self.adoption_plan().summary(dry_run=True)
        self._assert_profile_not_in_use()
        with _ExclusiveTargetLock(self.lock_path):
            plan = self.adoption_plan()
            committed_state, dependency_resolution = self._commit_plan(
                plan,
                failure_injector=failure_injector,
            )
            committed_plan = dataclass_replace(
                plan,
                desired_state=committed_state,
                title_projection=self._title_projection(committed_state),
                dependency_resolution=dependency_resolution,
            )
            return committed_plan.summary(dry_run=False)

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
        profile_use_preflight = self._assert_profile_not_in_use()
        if (operation is None) == (desired_state is None):
            raise ManagerError("provide exactly one of operation or desired_state")
        current = self.load_repository_state()
        ledger = self._read_ledger()
        self._assert_ledger_matches_repository(ledger, current)
        if ledger["schema_version"] == 3:
            # A transition may migrate the exact historical schema-V1
            # projection, but it must never overwrite arbitrary or tampered
            # physical display state as though that were a valid preimage.
            self._verify_title_projection(current)
        current_artifacts = self._managed_inventory_for_ledger(current, ledger)
        finalize_verified_profile = False

        if operation is not None:
            if expected_revision is None or at is None:
                raise ManagerError("operation transitions require expected_revision and at")
            self._validate_operation_manifest_identities(operation, refresh=True)
            pure_operation = copy.deepcopy(operation)
            planning_state = current
            if operation.get("type") == "DEPLOY_PROFILE":
                if set(operation) != {"type", "slots"}:
                    raise ManagerError("DEPLOY_PROFILE requires exactly type and slots")
                pure_operation = {"type": "SET_PROFILE", "slots": copy.deepcopy(operation["slots"])}
                try:
                    planning_state = migrate_runtime_state(current, self.project_index)
                except ValidationError as exc:
                    raise ManagerError(f"cannot migrate legacy runtime state for cohort deployment: {exc}") from exc
                finalize_verified_profile = True
            try:
                desired = plan_transition(planning_state, expected_revision, pure_operation, at, self.project_index)
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
        changed_slots = tuple(
            label
            for label in ("A", "B")
            if _slot_physical_identity(current["slots"][label])
            != _slot_physical_identity(desired["slots"][label])
        )
        if finalize_verified_profile and not any(
            desired["slots"][label] is not None for label in changed_slots
        ):
            raise ManagerError("DEPLOY_PROFILE requires at least one changed occupied slot to verify")
        union_mod_ids = {
            mod_id
            for item in current_artifacts
            for mod_id in item.ownership_mod_ids
        }
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
            union_mod_ids.update(artifact.ownership_mod_ids)
        self._verify_inventory(
            current,
            current_artifacts,
            conflict_mod_ids=union_mod_ids,
            prior_ledger=ledger,
        )
        writes, retained_moves, removals, unchanged = self._plan_delta(current_artifacts, desired_artifacts)
        dependency_resolution = self._planned_dependency_report(current_artifacts, desired_artifacts)
        return PhysicalPlan(
            mode="TRANSITION",
            current_state=current,
            desired_state=desired,
            current_artifacts=current_artifacts,
            desired_artifacts=desired_artifacts,
            writes=writes,
            retained_predecessor_moves=retained_moves,
            removals=removals,
            unchanged=unchanged,
            title_projection=self._title_projection(desired),
            dependency_resolution=dependency_resolution,
            profile_use_preflight=profile_use_preflight,
            batch_operation=(
                copy.deepcopy(operation)
                if operation is not None
                and operation.get("type") in {"PROMOTE_USER_PASSED_BATCH", "DEPLOY_PROFILE"}
                else None
            ),
            finalize_verified_profile=finalize_verified_profile,
            transition_at=at,
            changed_slots=changed_slots,
        )

    def _assert_desired_is_pure_transition(self, current: dict[str, Any], desired: dict[str, Any]) -> None:
        """Prove that an externally planned next state is one legal pure transition."""

        def declaration(member: dict[str, Any]) -> dict[str, Any]:
            result = {
                "unit": copy.deepcopy(member["unit"]),
                "replaces_accepted_deployment_id": member["replaces_accepted_deployment_id"],
            }
            if member.get("dependency_overrides"):
                result["dependency_overrides"] = copy.deepcopy(member["dependency_overrides"])
            return result

        def assignment(slot: dict[str, Any]) -> dict[str, Any]:
            members = _slot_members(slot)
            if len(members) == 1 and "unit" in slot:
                return {"candidate": declaration(members[0])}
            return {"members": [declaration(member) for member in members]}

        operations: list[dict[str, Any]] = []
        for label in ("A", "B"):
            desired_slot = desired["slots"][label]
            if desired_slot is not None:
                operations.append({"type": "ASSIGN_SLOT", **assignment(desired_slot)})
                operations.append({"type": "UPDATE_SLOT", "slot": label, **assignment(desired_slot)})
            operations.extend(
                {"type": operation_type, "slot": label}
                for operation_type in ("MARK_DEPLOYED", "MARK_READY", "REMOVE_SLOT", "PROMOTE_SLOT")
            )
            if desired_slot is not None:
                for member in _slot_members(desired_slot):
                    result_operation = {
                        "type": "RECORD_RESULT",
                        "slot": label,
                        "classification": member["runtime_result"]["classification"],
                        "evidence": copy.deepcopy(member["runtime_result"]["evidence"]),
                    }
                    if len(_slot_members(desired_slot)) > 1:
                        result_operation["project_uuid"] = member["unit"]["project_uuid"]
                    operations.append(result_operation)
        if desired["slots"]["A"] is None and desired["slots"]["B"] is None:
            operations.append({"type": "SET_PROFILE", "candidates": []})
        elif desired["slots"]["A"] is not None:
            if desired["schema_version"] == 1:
                candidates = [declaration(_slot_members(desired["slots"]["A"])[0])]
                if desired["slots"]["B"] is not None:
                    candidates.append(declaration(_slot_members(desired["slots"]["B"])[0]))
                operations.append({"type": "SET_PROFILE", "candidates": candidates})
            else:
                operations.append(
                    {
                        "type": "SET_PROFILE",
                        "slots": {
                            label: (
                                {
                                    "members": [
                                        declaration(member)
                                        for member in _slot_members(desired["slots"][label])
                                    ]
                                }
                                if desired["slots"][label] is not None
                                else None
                            )
                            for label in ("A", "B")
                        },
                    }
                )
        operations.extend(
            {
                "type": "REMOVE_ACCEPTED",
                "project_uuid": member["unit"]["project_uuid"],
            }
            for member in current["accepted_baseline"]["members"]
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
    ) -> tuple[
        tuple[FileAction, ...],
        tuple[RetainedPredecessorMove, ...],
        tuple[Path, ...],
        tuple[str, ...],
    ]:
        current_by_path = {item.relative_path.casefold(): item for item in current}
        current_by_artifact = {item.artifact_id: item for item in current}
        desired_by_path = {item.relative_path.casefold(): item for item in desired}
        writes: list[FileAction] = []
        retained_moves: list[RetainedPredecessorMove] = []
        unchanged: list[str] = []
        for artifact in desired:
            destination = self._destination(artifact)
            relative_key = artifact.relative_path.casefold()
            existing = current_by_path.get(relative_key)
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

            new_retained_predecessor = (
                artifact.retained_rollback
                and artifact.source["type"] == "ADOPTED_TARGET"
                and artifact.artifact_id not in current_by_artifact
                and relative_key not in current_by_path
            )
            if new_retained_predecessor:
                expected_source = self.mods / artifact.filename
                if not _paths_equal(source, expected_source):
                    raise ManagerError(
                        f"new retained rollback source must be mods/{artifact.filename}: {source}"
                    )
                if _paths_equal(source, destination):
                    raise ManagerError("new retained rollback source must be its exact enabled predecessor")
                enabled_relative = PurePosixPath(
                    self.config.mods_directory,
                    artifact.filename,
                ).as_posix()
                if enabled_relative.casefold() in current_by_path:
                    raise ManagerError(
                        "new retained rollback predecessor must be unmanaged: " + enabled_relative
                    )
                if enabled_relative.casefold() in desired_by_path:
                    raise ManagerError(
                        "retained rollback enabled predecessor cannot also be a desired managed path: "
                        + enabled_relative
                    )
                retained_moves.append(
                    RetainedPredecessorMove(
                        source=source,
                        destination=destination,
                        relative_path=artifact.relative_path,
                        artifact=artifact,
                    )
                )
                continue
            writes.append(FileAction(destination, artifact.relative_path, artifact, source))

        removal_candidates = [
            self._destination(artifact)
            for artifact in current
            if artifact.relative_path.casefold() not in desired_by_path
            or desired_by_path[artifact.relative_path.casefold()].sha256 != artifact.sha256
        ]
        removals_by_path: dict[str, Path] = {}
        for path in removal_candidates:
            removals_by_path.setdefault(os.path.normcase(str(path)).casefold(), path)
        removals = tuple(removals_by_path.values())
        return tuple(writes), tuple(retained_moves), removals, tuple(unchanged)

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
        self._assert_profile_not_in_use()
        with _ExclusiveTargetLock(self.lock_path):
            plan = self.transition_plan(
                operation=operation,
                desired_state=desired_state,
                expected_revision=expected_revision,
                at=at,
            )
            committed_state, dependency_resolution = self._commit_plan(
                plan,
                failure_injector=failure_injector,
            )
            committed_plan = dataclass_replace(
                plan,
                desired_state=committed_state,
                title_projection=self._title_projection(committed_state),
                dependency_resolution=dependency_resolution,
            )
            return committed_plan.summary(dry_run=False)

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

    def _commit_plan(
        self,
        plan: PhysicalPlan,
        *,
        failure_injector: FailureInjector | None,
    ) -> tuple[dict[str, Any], dict[str, Any]]:
        injector = failure_injector or (lambda _stage: None)
        self._assert_profile_not_in_use()
        under_lock_dependency_resolution = self._planned_dependency_report(
            plan.current_artifacts,
            plan.desired_artifacts,
        )
        self._assert_platform_attestation_unchanged(
            plan.dependency_resolution,
            under_lock_dependency_resolution,
            phase="between planning and the under-lock pre-mutation check",
        )
        transaction = self.target / f".mynx-runtime-v2-transaction-{uuid.uuid4()}"
        _assert_no_reparse_components(transaction, "transaction backup", root=self.target)
        self._assert_target_containment(transaction.resolve(strict=False), "transaction backup")
        touched = sorted(
            {action.destination for action in plan.writes}
            .union(plan.removals)
            .union(
                path
                for move in plan.retained_predecessor_moves
                for path in (move.source, move.destination)
            )
            .union({self.title_projection_path}),
            key=lambda item: str(item).casefold(),
        )
        snapshots: dict[Path, Path | None] = {}
        repo_snapshot: Path | None = None
        ledger_snapshot: Path | None = None
        ledger_written = False
        state_written = False
        committed = False
        preserve_transaction = False
        committed_state = plan.desired_state
        committed_dependency_resolution = under_lock_dependency_resolution
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
            for index, move in enumerate(plan.retained_predecessor_moves):
                stage = source_backup / f"move-{index:04d}-{move.artifact.filename}"
                shutil.copy2(move.source, stage)
                self._verify_artifact_file(stage, move.artifact)
            injector("after_backup")

            for index, path in enumerate(plan.removals):
                if path.exists():
                    path.unlink()
                injector(f"after_remove_{index + 1}")
            for index, move in enumerate(plan.retained_predecessor_moves):
                if move.destination.exists():
                    raise ManagerError(
                        "retained rollback destination became occupied before atomic move: "
                        + str(move.destination)
                    )
                self._verify_artifact_file(move.source, move.artifact)
                os.replace(move.source, move.destination)
                injector(f"after_move_{index + 1}")
            for index, action in enumerate(plan.writes):
                action.destination.parent.mkdir(parents=False, exist_ok=True)
                _atomic_copy(staged[action.destination], action.destination)
                injector(f"after_write_{index + 1}")
            injector("after_physical_apply")

            self._verify_inventory(
                plan.desired_state,
                plan.desired_artifacts,
                conflict_mod_ids={
                    mod_id
                    for item in plan.desired_artifacts
                    for mod_id in item.ownership_mod_ids
                },
            )
            committed_dependency_resolution = self._physical_dependency_report(plan.desired_artifacts)
            self._assert_platform_attestation_unchanged(
                under_lock_dependency_resolution,
                committed_dependency_resolution,
                phase="between the under-lock pre-mutation check and post-write verification",
            )
            if plan.finalize_verified_profile:
                if plan.transition_at is None:
                    raise ManagerError("verified profile finalization requires a transition timestamp")
                try:
                    committed_state = finalize_verified_profile_transition(
                        plan.desired_state,
                        plan.transition_at,
                        plan.transition_at,
                        self.project_index,
                        changed_slots=plan.changed_slots,
                        authority=_PHYSICAL_MANAGER_AUTHORITY,
                    )
                except ValidationError as exc:
                    raise ManagerError(f"cannot finalize physically verified profile state: {exc}") from exc
            final_title_projection = self._title_projection(committed_state)
            _atomic_write_json(self.title_projection_path, final_title_projection)
            injector("after_title_projection_write")
            self._verify_title_projection(committed_state)
            injector("after_physical_verify")

            # Re-read batch-bound manifest releases and recheck the runtime-state
            # compare-and-swap source immediately before committing either
            # ledger. The target lock serializes all manager writers.
            if plan.batch_operation is not None:
                self._validate_operation_manifest_identities(plan.batch_operation, refresh=True)
            live = self.load_repository_state()
            if live != plan.current_state:
                raise ManagerError("repository runtime state changed during physical transition")

            ledger = self._ledger(committed_state, plan.desired_artifacts)
            _atomic_write_json(self.ledger_path, ledger)
            ledger_written = True
            injector("after_ledger_write")
            _atomic_write_json(self.config.runtime_state, committed_state)
            state_written = True
            injector("after_state_write")

            final_state = self.load_repository_state()
            final_ledger = self._read_ledger()
            self._assert_ledger_matches_repository(final_ledger, final_state)
            self._verify_inventory(final_state, plan.desired_artifacts, prior_ledger=final_ledger)
            committed_dependency_resolution = self._physical_dependency_report(plan.desired_artifacts)
            self._assert_platform_attestation_unchanged(
                under_lock_dependency_resolution,
                committed_dependency_resolution,
                phase="before final post-commit verification completed",
            )
            self._verify_title_projection(final_state)
            injector("after_post_verify")
            # Make launch authority the final observed external input before
            # the transaction is declared committed. A drift injected after
            # the earlier post-write scan must still restore the full preimage.
            committed_dependency_resolution = self._physical_dependency_report(plan.desired_artifacts)
            self._assert_platform_attestation_unchanged(
                under_lock_dependency_resolution,
                committed_dependency_resolution,
                phase="at the final post-verification commit boundary",
            )
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
                if state_written and repo_snapshot is not None:
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
        return committed_state, committed_dependency_resolution


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


def _lexical_paths_equal(left: Path, right: Path) -> bool:
    """Compare absolute normalized spellings without resolving filesystem links."""

    return os.path.normcase(os.path.abspath(left)).casefold() == os.path.normcase(
        os.path.abspath(right)
    ).casefold()


def _is_lexically_within(child: Path, parent: Path) -> bool:
    """Check lexical containment without touching either filesystem path."""

    child_text = os.path.normcase(os.path.abspath(child)).casefold()
    parent_text = os.path.normcase(os.path.abspath(parent)).casefold()
    try:
        return os.path.commonpath([child_text, parent_text]) == parent_text
    except ValueError:
        return False


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


def _windows_processes_using_profile(profile: Path) -> list[dict[str, Any]]:
    """Return Minecraft JVM evidence whose command line contains the profile path."""

    if os.name != "nt":
        return []
    script = (
        "Get-CimInstance Win32_Process | "
        "Select-Object ProcessId,Name,ExecutablePath,CommandLine | "
        "ConvertTo-Json -Compress -Depth 3"
    )
    creation_flags = getattr(subprocess, "CREATE_NO_WINDOW", 0)
    try:
        completed = subprocess.run(
            ["powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", script],
            check=False,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=15,
            creationflags=creation_flags,
        )
    except (OSError, subprocess.SubprocessError) as exc:
        raise ManagerError(f"cannot prove dedicated profile is inactive from Windows process state: {exc}") from exc
    if completed.returncode != 0:
        detail = (completed.stderr or completed.stdout).strip()
        raise ManagerError(
            "cannot prove dedicated profile is inactive from Windows process state"
            + (f": {detail}" if detail else "")
        )
    try:
        raw = json.loads(completed.stdout) if completed.stdout.strip() else []
    except json.JSONDecodeError as exc:
        raise ManagerError("cannot parse Windows process evidence for dedicated profile use") from exc
    records = raw if isinstance(raw, list) else [raw]
    target = os.path.normcase(str(profile.resolve(strict=False))).casefold()
    matches: list[dict[str, Any]] = []
    for record in records:
        if not isinstance(record, dict):
            continue
        command_line = record.get("CommandLine")
        process_id = record.get("ProcessId")
        process_name = record.get("Name")
        if not isinstance(process_name, str) or process_name.casefold() not in {"java.exe", "javaw.exe"}:
            continue
        if not isinstance(command_line, str) or target not in os.path.normcase(command_line).casefold():
            continue
        if process_id == os.getpid():
            continue
        matches.append(
            {
                "pid": process_id,
                "name": process_name,
                "executable_path": record.get("ExecutablePath"),
                "command_line": command_line,
            }
        )
    matches.sort(key=lambda item: (str(item["name"]).casefold(), int(item["pid"] or 0)))
    return matches


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


def _sqlite_identity(value: Any) -> str:
    """Render an opaque SQLite identity without assuming Modrinth's storage type."""

    if isinstance(value, str) and value:
        return value
    if isinstance(value, int) and not isinstance(value, bool):
        return str(value)
    if isinstance(value, memoryview):
        value = value.tobytes()
    if isinstance(value, bytes) and value:
        return "hex:" + value.hex()
    raise ManagerError(f"Modrinth launch authority contains an invalid opaque identity: {value!r}")


def _probe_java_properties(executable: Path) -> dict[str, str]:
    """Probe only JVM properties; this never invokes a Minecraft launch."""

    try:
        completed = subprocess.run(
            [str(executable), "-XshowSettings:properties", "-version"],
            check=False,
            capture_output=True,
            text=True,
            timeout=15,
            shell=False,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
        )
    except (OSError, subprocess.SubprocessError) as exc:
        raise ManagerError(f"cannot probe selected Java executable {executable}: {exc}") from exc
    if completed.returncode != 0:
        raise ManagerError(
            f"selected Java executable probe failed with exit code {completed.returncode}: {executable}"
        )
    properties: dict[str, str] = {}
    for line in (completed.stdout + "\n" + completed.stderr).splitlines():
        match = re.match(r"^\s*([A-Za-z0-9_.-]+)\s*=\s*(.*?)\s*$", line)
        if match is not None:
            properties.setdefault(match.group(1), match.group(2))
    return properties


@dataclass(frozen=True)
class _FabricSemanticVersion:
    components: tuple[int | None, ...]
    prerelease: str | None
    build: str | None


_FABRIC_PRERELEASE_RE = re.compile(r"(?:|[-0-9A-Za-z]+(?:\.[-0-9A-Za-z]+)*)$")
_FABRIC_PRERELEASE_INTEGER_RE = re.compile(r"(?:0|[1-9][0-9]*)$")
_FABRIC_OPERATORS = (">=", "<=", ">", "<", "=", "~", "^")
_FABRIC_PLATFORM_DEPENDENCIES = frozenset({"java", "minecraft", "fabricloader"})


def _parse_fabric_semantic_version(value: str, *, store_wildcards: bool) -> _FabricSemanticVersion | None:
    """Parse Fabric Loader's SemanticVersionImpl superset, or return None."""

    if not isinstance(value, str) or not value:
        return None
    core_and_prerelease, separator, build = value.partition("+")
    build_value = build if separator else None
    core, prerelease_separator, prerelease = core_and_prerelease.partition("-")
    prerelease_value = prerelease if prerelease_separator else None
    if prerelease_value is not None and _FABRIC_PRERELEASE_RE.fullmatch(prerelease_value) is None:
        return None
    if core.startswith(".") or core.endswith("."):
        return None
    raw_components = core.split(".")
    if not raw_components or any(component == "" for component in raw_components):
        return None
    components: list[int | None] = []
    first_wildcard: int | None = None
    for index, component in enumerate(raw_components):
        if store_wildcards and component in {"x", "X", "*"}:
            if prerelease_value is not None:
                return None
            if index == 0:
                return None
            if first_wildcard is None:
                first_wildcard = index
            components.append(None)
            continue
        if first_wildcard is not None or not re.fullmatch(r"[0-9]+", component):
            return None
        try:
            parsed = int(component)
        except ValueError:
            return None
        if parsed > 2_147_483_647:
            return None
        components.append(parsed)
    if first_wildcard is not None:
        components = components[: first_wildcard + 1]
    return _FabricSemanticVersion(tuple(components), prerelease_value, build_value)


def _fabric_component(version: _FabricSemanticVersion, index: int) -> int | None:
    if index < len(version.components):
        return version.components[index]
    return None if version.components[-1] is None else 0


def _compare_fabric_semantic_versions(left: _FabricSemanticVersion, right: _FabricSemanticVersion) -> int:
    for index in range(max(len(left.components), len(right.components))):
        left_component = _fabric_component(left, index)
        right_component = _fabric_component(right, index)
        if left_component is None or right_component is None:
            continue
        if left_component != right_component:
            return -1 if left_component < right_component else 1

    left_pre = left.prerelease
    right_pre = right.prerelease
    if left_pre is None and right_pre is None:
        return 0
    if left_pre is None:
        return 0 if any(component is None for component in left.components) else 1
    if right_pre is None:
        return 0 if any(component is None for component in right.components) else -1
    left_parts = left_pre.split(".")
    right_parts = right_pre.split(".")
    for index in range(max(len(left_parts), len(right_parts))):
        if index >= len(left_parts):
            return -1
        if index >= len(right_parts):
            return 1
        left_part = left_parts[index]
        right_part = right_parts[index]
        left_numeric = _FABRIC_PRERELEASE_INTEGER_RE.fullmatch(left_part) is not None
        right_numeric = _FABRIC_PRERELEASE_INTEGER_RE.fullmatch(right_part) is not None
        if left_numeric and right_numeric and len(left_part) != len(right_part):
            return -1 if len(left_part) < len(right_part) else 1
        if left_numeric != right_numeric:
            return -1 if left_numeric else 1
        if left_part != right_part:
            return -1 if left_part < right_part else 1
    return 0


def _fabric_predicate_matches(version: str, predicate: str) -> bool:
    """Evaluate one Fabric Loader VersionPredicate string.

    Space-delimited terms are ANDed. Metadata arrays are handled by the graph
    resolver as ORs, matching ModDependencyImpl.
    """

    actual_semantic = _parse_fabric_semantic_version(version, store_wildcards=False)
    for raw_term in predicate.split(" "):
        term = raw_term.strip()
        if not term or term == "*":
            continue
        operator = "="
        for candidate in _FABRIC_OPERATORS:
            if term.startswith(candidate):
                operator = candidate
                term = term[len(candidate) :]
                break
        if not term:
            raise ManagerError(f"invalid Fabric version predicate {predicate!r}: empty reference version")
        reference_semantic = _parse_fabric_semantic_version(term, store_wildcards=True)
        if reference_semantic is None:
            if operator in {">", "<"}:
                raise ManagerError(
                    f"invalid Fabric version predicate {predicate!r}: exclusive ranges require semantic versions"
                )
            if version != term:
                return False
            continue

        if any(component is None for component in reference_semantic.components):
            if operator != "=":
                raise ManagerError(
                    f"invalid Fabric version predicate {predicate!r}: wildcard ranges require equality or no operator"
                )
            component_count = len(reference_semantic.components)
            concrete_components = tuple(
                component for component in reference_semantic.components[:-1] if component is not None
            )
            reference_semantic = _FabricSemanticVersion(
                concrete_components,
                "",
                reference_semantic.build,
            )
            if component_count == 2:
                operator = "^"
            elif component_count == 3:
                operator = "~"
            else:
                # Fabric Loader represents a.b.c.x (and any longer X-range)
                # as >=a.b.c- and <a.b.(c+1)-. Keep this explicit instead of
                # approximating it with the two/three-component shorthands.
                if actual_semantic is None:
                    return False
                if not concrete_components or concrete_components[-1] == 2_147_483_647:
                    raise ManagerError(
                        f"invalid Fabric version predicate {predicate!r}: wildcard upper bound overflows"
                    )
                upper_components = (*concrete_components[:-1], concrete_components[-1] + 1)
                upper = _FabricSemanticVersion(upper_components, "", None)
                if (
                    _compare_fabric_semantic_versions(actual_semantic, reference_semantic) < 0
                    or _compare_fabric_semantic_versions(actual_semantic, upper) >= 0
                ):
                    return False
                continue

        if actual_semantic is None:
            # Fabric treats non-semantic versions as exact-only for inclusive
            # operators and never matches them against a semantic reference.
            return False
        comparison = _compare_fabric_semantic_versions(actual_semantic, reference_semantic)
        if operator == "=" and comparison != 0:
            return False
        if operator == ">=" and comparison < 0:
            return False
        if operator == "<=" and comparison > 0:
            return False
        if operator == ">" and comparison <= 0:
            return False
        if operator == "<" and comparison >= 0:
            return False
        if operator == "~" and not (
            comparison >= 0
            and _fabric_component(actual_semantic, 0) == _fabric_component(reference_semantic, 0)
            and _fabric_component(actual_semantic, 1) == _fabric_component(reference_semantic, 1)
        ):
            return False
        if operator == "^" and not (
            comparison >= 0
            and _fabric_component(actual_semantic, 0) == _fabric_component(reference_semantic, 0)
        ):
            return False
    return True


def _read_fabric_descriptor(
    path: Path,
    *,
    strict_provides: bool = False,
    relative_path: str | None = None,
    artifact: ManagedArtifact | None = None,
) -> FabricModDescriptor:
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
    if not isinstance(manifest, dict):
        raise ManagerError(f"fabric.mod.json in {path} must be an object")
    primary_id = manifest.get("id")
    version = manifest.get("version")
    if not isinstance(primary_id, str) or not primary_id:
        raise ManagerError(f"fabric.mod.json in {path} has no valid id")
    if not isinstance(version, str) or not version:
        raise ManagerError(f"fabric.mod.json in {path} has no valid version")
    provides_value = manifest.get("provides", [])
    provides: list[str] = []
    if strict_provides and not isinstance(provides_value, list):
        raise ManagerError(f"fabric.mod.json provides in {path} must be an array")
    if isinstance(provides_value, list):
        seen: set[str] = set()
        for index, item in enumerate(provides_value):
            if not isinstance(item, str) or not item:
                if strict_provides:
                    raise ManagerError(
                        f"fabric.mod.json provides[{index}] in {path} must be a non-empty string"
                    )
                continue
            if item == primary_id:
                raise ManagerError(f"fabric.mod.json provides in {path} must not repeat primary id {item}")
            if item in seen:
                raise ManagerError(f"fabric.mod.json provides in {path} contains duplicate alias {item}")
            seen.add(item)
            provides.append(item)

    depends_value = manifest.get("depends", {})
    if not isinstance(depends_value, dict):
        raise ManagerError(f"fabric.mod.json depends in {path} must be an object")
    depends: dict[str, tuple[str, ...]] = {}
    for dependency_id, raw_predicates in depends_value.items():
        if not isinstance(dependency_id, str) or not dependency_id:
            raise ManagerError(f"fabric.mod.json depends in {path} has an invalid dependency id")
        if isinstance(raw_predicates, str):
            predicates = (raw_predicates,)
        elif isinstance(raw_predicates, list) and raw_predicates and all(
            isinstance(item, str) for item in raw_predicates
        ):
            predicates = tuple(raw_predicates)
        else:
            raise ManagerError(
                f"fabric.mod.json dependency {dependency_id!r} in {path} must be a string or nonempty string array"
            )
        # Parse every predicate even when no matching provider is currently
        # visible; malformed Loader metadata is never silently accepted.
        for predicate in predicates:
            _fabric_predicate_matches("0.0.0", predicate)
        depends[dependency_id] = predicates

    return FabricModDescriptor(
        path=path,
        relative_path=relative_path or path.name,
        filename=path.name.removesuffix(".disabled"),
        primary_id=primary_id,
        provides=tuple(provides),
        version=version,
        depends=depends,
        managed_project_uuid=artifact.project_uuid if artifact is not None else None,
        managed_project_id=artifact.project_id if artifact is not None else None,
        managed_deployment_id=artifact.deployment_id if artifact is not None else None,
        managed_artifact_id=artifact.artifact_id if artifact is not None else None,
    )


def _fabric_mod_ids(path: Path, *, strict_provides: bool = False) -> frozenset[str]:
    descriptor = _read_fabric_descriptor(path, strict_provides=strict_provides)
    return frozenset(descriptor.ownership_ids)


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
