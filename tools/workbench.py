#!/usr/bin/env python3
"""Dependency-free validators for Mynx Flavoured Workbench project state."""

from __future__ import annotations

import argparse
import copy
import json
import re
import subprocess
import sys
import unicodedata
from datetime import datetime
from pathlib import Path, PurePosixPath
from typing import Any, Iterable
from uuid import UUID


if __name__ == "__main__":
    # Let sibling tools reuse this exact module/class instance during direct CLI execution.
    sys.modules.setdefault("workbench", sys.modules[__name__])


STATUS_SCHEMA_REF = "../../schemas/workbench-status.schema.json"
LIFECYCLES = {"PLANNED", "ACTIVE", "BLOCKED", "ACCEPTED", "PARKED"}
BUILD_STATES = {
    "NOT_RUN",
    "GENERATED",
    "STATIC_PASS",
    "STATIC_FAIL",
    "CONTROLLED_VALIDATION_PASS",
    "CONTROLLED_VALIDATION_FAIL",
    "INCONCLUSIVE",
}
RUNTIME_STATES = {"RUNTIME_UNTESTED", "PARTIAL_RUNTIME_PASS", "RUNTIME_PASS", "RUNTIME_FAIL", "INCONCLUSIVE"}
DEPENDENCY_TYPES = {"PROJECT", "MOD", "RESOURCE_PACK", "DATA_PACK", "TOOL", "SERVICE", "OTHER"}
ACCEPTED_CURRENT_STATES = {"NO_ACCEPTED", "CURRENT_IS_ACCEPTED", "CURRENT_DIFFERS_FROM_ACCEPTED"}
ACCEPTED_ROLLBACK_STATES = {"NO_ROLLBACK", "ACCEPTED_IS_ROLLBACK", "ROLLBACK_DIFFERS_FROM_ACCEPTED"}
CURRENT_RELEASE_DEPLOYMENT_STATES = {
    "CURRENT_RELEASE_DEPLOYED",
    "OLDER_RELEASE_DEPLOYED",
    "CURRENT_RELEASE_NOT_DEPLOYED",
}

PROJECT_ID_RE = re.compile(r"^[a-z0-9]+(?:[.-][a-z0-9]+)*$")
FABRIC_MOD_ID_RE = re.compile(r"^[a-z][a-z0-9_.-]*$")
COMMIT_RE = re.compile(r"^[0-9a-f]{40}$")
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
RFC3339_UTC_RE = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?Z$")
WINDOWS_RESERVED_RE = re.compile(r"^(?:con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\.|$)", re.IGNORECASE)
LOG_HEADING_RE = re.compile(r"^## (?P<timestamp>\S+) — (?P<summary>\S(?:.*\S)?)$", re.MULTILINE)
LOG_BULLET_RE = re.compile(r"^- (?P<label>[^:\n]+): (?P<value>\S(?:.*\S)?)$", re.MULTILINE)
LOG_FIELDS = {"Revision", "Source checkpoint", "Changes", "Build/static", "Runtime", "Artifact", "Result", "Next state"}
LOCAL_PATH_LEAK_RE = re.compile(
    r"(?<![A-Za-z0-9])(?:[A-Za-z]:[\\/]|\\Users\\[^\\/\s<>]+|/(?:home|Users)/[^/\s<>]+(?:/|$))",
    re.IGNORECASE,
)
PUBLIC_TEXT_ROOT_FILES = ("AGENTS.md", "README.md", "MIGRATION_FREEZE.md", "WORKBENCH_SERVER_STATE.json")


class ValidationError(ValueError):
    """A deterministic contract validation failure."""


def _fail(path: str, message: str) -> None:
    raise ValidationError(f"{path}: {message}")


def validate_public_text(text: str, path: str) -> None:
    """Reject machine-specific absolute paths in canonical public-facing text."""

    match = LOCAL_PATH_LEAK_RE.search(text)
    if match:
        _fail(path, "contains a machine-specific absolute/local filesystem path")


def _reject_duplicate_keys(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise ValidationError(f"duplicate JSON key: {key}")
        result[key] = value
    return result


def load_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=_reject_duplicate_keys)
    except (OSError, json.JSONDecodeError) as exc:
        raise ValidationError(f"{path}: cannot read valid JSON: {exc}") from exc
    if not isinstance(value, dict):
        raise ValidationError(f"{path}: root must be an object")
    return value


def load_json_text(text: str, source: str = "JSON") -> dict[str, Any]:
    try:
        value = json.loads(text, object_pairs_hook=_reject_duplicate_keys)
    except json.JSONDecodeError as exc:
        raise ValidationError(f"{source}: cannot read valid JSON: {exc}") from exc
    if not isinstance(value, dict):
        raise ValidationError(f"{source}: root must be an object")
    return value


def _object(value: Any, path: str, keys: Iterable[str]) -> dict[str, Any]:
    if not isinstance(value, dict):
        _fail(path, "must be an object")
    expected = set(keys)
    actual = set(value)
    missing = sorted(expected - actual)
    extra = sorted(actual - expected)
    if missing:
        _fail(path, f"missing keys: {', '.join(missing)}")
    if extra:
        _fail(path, f"unknown keys: {', '.join(extra)}")
    return value


def _integer(value: Any, path: str, minimum: int = 0) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value < minimum:
        _fail(path, f"must be an integer >= {minimum}")
    return value


def _nonblank(value: Any, path: str) -> str:
    if not isinstance(value, str) or not value or value != value.strip():
        _fail(path, "must be a nonblank, trimmed string")
    return value


def _enum(value: Any, path: str, allowed: set[str]) -> str:
    value = _nonblank(value, path)
    if value not in allowed:
        _fail(path, f"must be one of: {', '.join(sorted(allowed))}")
    return value


def _uuid(value: Any, path: str) -> str:
    value = _nonblank(value, path)
    try:
        parsed = UUID(value)
    except (ValueError, AttributeError) as exc:
        raise ValidationError(f"{path}: must be a UUID") from exc
    if str(parsed) != value:
        _fail(path, "must use canonical lowercase UUID form")
    return value


def _project_id(value: Any, path: str) -> str:
    value = _nonblank(value, path)
    if not PROJECT_ID_RE.fullmatch(value):
        _fail(path, "must use lowercase alphanumeric segments separated by hyphens or dots")
    return value


def _fabric_mod_id(value: Any, path: str) -> str:
    value = _nonblank(value, path)
    if not FABRIC_MOD_ID_RE.fullmatch(value):
        _fail(path, "must be a lowercase Fabric mod ID")
    return value


def _commit(value: Any, path: str) -> str:
    value = _nonblank(value, path)
    if not COMMIT_RE.fullmatch(value):
        _fail(path, "must be an exact lowercase 40-hex commit")
    return value


def _sha256(value: Any, path: str) -> str:
    value = _nonblank(value, path)
    if not SHA256_RE.fullmatch(value):
        _fail(path, "must be an exact lowercase SHA-256")
    return value


def _timestamp(value: Any, path: str) -> datetime:
    value = _nonblank(value, path)
    if not RFC3339_UTC_RE.fullmatch(value):
        _fail(path, "must be an RFC 3339 UTC timestamp ending in Z")
    try:
        return datetime.fromisoformat(value[:-1] + "+00:00")
    except ValueError as exc:
        raise ValidationError(f"{path}: invalid timestamp") from exc


def _normalized_token(value: str) -> str:
    return unicodedata.normalize("NFKC", value).strip().casefold()


def _text_list(value: Any, path: str, *, minimum: int = 0, project_ids: bool = False) -> list[str]:
    if not isinstance(value, list) or len(value) < minimum:
        _fail(path, f"must be an array with at least {minimum} item(s)")
    result: list[str] = []
    seen: set[str] = set()
    for index, item in enumerate(value):
        checked = _project_id(item, f"{path}[{index}]") if project_ids else _nonblank(item, f"{path}[{index}]")
        token = _normalized_token(checked)
        if token in seen:
            _fail(path, f"contains duplicate value: {checked}")
        seen.add(token)
        result.append(checked)
    return result


def _repository_path(value: Any, path: str) -> str:
    value = _nonblank(value, path)
    if "\\" in value or any(character in value for character in "*?"):
        _fail(path, "must be a normalized relative repository path without globs")
    parsed = PurePosixPath(value)
    if parsed.is_absolute() or any(part in {"", ".", ".."} for part in parsed.parts):
        _fail(path, "must be a normalized relative repository path")
    if parsed.as_posix() != value:
        _fail(path, "must be normalized")
    return value


def _artifact(value: Any, path: str) -> dict[str, Any]:
    value = _object(value, path, {"filename", "sha256"})
    filename = _nonblank(value["filename"], f"{path}.filename")
    if (
        filename in {".", ".."}
        or PurePosixPath(filename).name != filename
        or any(character in filename for character in '<>:"/\\|?*')
        or any(ord(character) < 32 for character in filename)
        or filename.endswith((".", " "))
        or WINDOWS_RESERVED_RE.match(filename)
    ):
        _fail(f"{path}.filename", "must be a Windows-safe basename")
    _sha256(value["sha256"], f"{path}.sha256")
    return value


def _runtime_dependency_policy(value: Any, path: str) -> dict[str, Any]:
    value = _object(value, path, {"contract", "exceptions"})
    if value["contract"] != "CAPABILITY_OR_PROVIDER":
        _fail(f"{path}.contract", "must equal CAPABILITY_OR_PROVIDER")
    exceptions = value["exceptions"]
    if not isinstance(exceptions, list):
        _fail(f"{path}.exceptions", "must be an array")
    seen: set[tuple[str, str, str, str]] = set()
    for index, exception in enumerate(exceptions):
        exception_path = f"{path}.exceptions[{index}]"
        exception = _object(
            exception,
            exception_path,
            {"consumer_id", "relationship", "dependency_id", "predicate", "reason", "regression_evidence"},
        )
        consumer_id = _fabric_mod_id(exception["consumer_id"], f"{exception_path}.consumer_id")
        relationship = _enum(
            exception["relationship"],
            f"{exception_path}.relationship",
            {"depends", "recommends", "suggests"},
        )
        dependency_id = _fabric_mod_id(exception["dependency_id"], f"{exception_path}.dependency_id")
        predicate = _nonblank(exception["predicate"], f"{exception_path}.predicate")
        _nonblank(exception["reason"], f"{exception_path}.reason")
        _text_list(exception["regression_evidence"], f"{exception_path}.regression_evidence", minimum=1)
        key = (consumer_id, relationship, dependency_id, predicate)
        if key in seen:
            _fail(f"{path}.exceptions", f"contains duplicate predicate exception: {key}")
        seen.add(key)
    return value


def _release(value: Any, path: str, *, require_canary: bool = False) -> dict[str, Any] | None:
    if value is None:
        return None
    if not isinstance(value, dict):
        _fail(path, "must be an object")
    required = {"version", "artifact", "source_commit"}
    allowed = required | {"canary", "embedded_version", "summary", "built_at", "runtime_dependency_policy"}
    missing = sorted(required - set(value))
    extra = sorted(set(value) - allowed)
    if missing:
        _fail(path, f"missing keys: {', '.join(missing)}")
    if extra:
        _fail(path, f"unknown keys: {', '.join(extra)}")
    if require_canary and "canary" not in value:
        _fail(f"{path}.canary", "is required for every current release")
    if "canary" in value:
        _integer(value["canary"], f"{path}.canary", 1)
    _nonblank(value["version"], f"{path}.version")
    if "embedded_version" in value:
        _nonblank(value["embedded_version"], f"{path}.embedded_version")
    if "summary" in value:
        summary = _nonblank(value["summary"], f"{path}.summary")
        if "\n" in summary or "\r" in summary:
            _fail(f"{path}.summary", "must be one concise line")
    if "built_at" in value:
        _timestamp(value["built_at"], f"{path}.built_at")
    if value["artifact"] is not None:
        _artifact(value["artifact"], f"{path}.artifact")
    _commit(value["source_commit"], f"{path}.source_commit")
    if "runtime_dependency_policy" in value:
        if value["artifact"] is None:
            _fail(f"{path}.runtime_dependency_policy", "requires a concrete artifact")
        _runtime_dependency_policy(value["runtime_dependency_policy"], f"{path}.runtime_dependency_policy")
    return value


def _release_identity(value: dict[str, Any] | None) -> tuple[Any, Any, Any] | None:
    """Return the pre-existing artifact identity used for grandfathering.

    ``canary``, ``embedded_version``, ``built_at``, and the source checkpoint
    are additive release metadata/provenance. Adding truthful metadata does not
    create a new exact release identity and therefore must not turn a historical
    artifact into a newly built candidate which requires release-artifact
    metadata.
    """

    if value is None:
        return None
    artifact = value["artifact"]
    return (
        value["version"],
        None if artifact is None else artifact["filename"],
        None if artifact is None else artifact["sha256"],
    )


def _canary_release_identity(value: dict[str, Any]) -> tuple[Any, ...]:
    """Return the byte identity which owns one Workbench Canary ordinal.

    A filename, internal version, source checkpoint, or other metadata correction
    must not create a new Canary when the finalized bytes are unchanged. Releases
    without a concrete artifact fall back to the broader release identity.
    """

    artifact = value["artifact"]
    if artifact is not None:
        return ("sha256", artifact["sha256"])
    return ("release", *_release_identity(value))


def _release_canaries(releases: dict[str, Any]) -> dict[tuple[Any, ...], int]:
    """Return known byte-identity Canary ordinals and reject conflicts.

    Historical accepted/rollback releases may predate the Workbench Canary
    convention and therefore omit ``canary``. Once an ordinal is known for an
    exact identity in any slot, every other retained copy must preserve it.
    """

    known: dict[tuple[Any, ...], tuple[int, str]] = {}
    missing: list[tuple[tuple[Any, ...], str]] = []
    for slot in ("current", "accepted", "rollback"):
        release = releases[slot]
        if release is None:
            continue
        identity = _canary_release_identity(release)
        path = f"$.state.releases.{slot}.canary"
        canary = release.get("canary")
        if canary is None:
            missing.append((identity, path))
            continue
        previous = known.get(identity)
        if previous is not None and previous[0] != canary:
            _fail(path, f"must equal C{previous[0]} already recorded for these exact artifact bytes")
        known[identity] = (canary, path)
    for identity, path in missing:
        previous = known.get(identity)
        if previous is not None:
            _fail(path, f"must preserve C{previous[0]} already recorded for these exact artifact bytes")
    return {identity: canary for identity, (canary, _) in known.items()}


def validate_status(data: dict[str, Any], project_directory: Path | None = None) -> dict[str, Any]:
    """Validate exact manifest shape plus cross-field semantics."""

    root = _object(data, "$", {"$schema", "schema_version", "identity", "definition", "state", "synchronization"})
    if root["$schema"] != STATUS_SCHEMA_REF:
        _fail("$.$schema", f"must equal {STATUS_SCHEMA_REF}")
    if root["schema_version"] != 3:
        _fail("$.schema_version", "must equal 3")

    identity = _object(root["identity"], "$.identity", {"uuid", "name", "project_id", "aliases", "legacy_names", "legacy_ids"})
    project_uuid = _uuid(identity["uuid"], "$.identity.uuid")
    _nonblank(identity["name"], "$.identity.name")
    project_id = _project_id(identity["project_id"], "$.identity.project_id")
    _text_list(identity["aliases"], "$.identity.aliases")
    _text_list(identity["legacy_names"], "$.identity.legacy_names")
    _text_list(identity["legacy_ids"], "$.identity.legacy_ids", project_ids=True)

    definition = _object(root["definition"], "$.definition", {"lifecycle", "goals", "scope", "boundaries", "dependencies"})
    lifecycle = _enum(definition["lifecycle"], "$.definition.lifecycle", LIFECYCLES)
    _text_list(definition["goals"], "$.definition.goals", minimum=1)
    _text_list(definition["scope"], "$.definition.scope", minimum=1)
    boundaries = _object(definition["boundaries"], "$.definition.boundaries", {"owned_paths", "exclusions"})
    if not isinstance(boundaries["owned_paths"], list) or not boundaries["owned_paths"]:
        _fail("$.definition.boundaries.owned_paths", "must be a nonempty array")
    owned_paths: list[str] = []
    for index, item in enumerate(boundaries["owned_paths"]):
        owned_paths.append(_repository_path(item, f"$.definition.boundaries.owned_paths[{index}]"))
    if len({_normalized_token(item) for item in owned_paths}) != len(owned_paths):
        _fail("$.definition.boundaries.owned_paths", "contains duplicate paths")
    _text_list(boundaries["exclusions"], "$.definition.boundaries.exclusions")

    if not isinstance(definition["dependencies"], list):
        _fail("$.definition.dependencies", "must be an array")
    for index, dependency in enumerate(definition["dependencies"]):
        dep_path = f"$.definition.dependencies[{index}]"
        dependency = _object(dependency, dep_path, {"type", "identifier", "requirement", "reason"})
        dep_type = _enum(dependency["type"], f"{dep_path}.type", DEPENDENCY_TYPES)
        identifier = _nonblank(dependency["identifier"], f"{dep_path}.identifier")
        if dep_type == "PROJECT":
            dependency_uuid = _uuid(identifier, f"{dep_path}.identifier")
            if dependency_uuid == project_uuid:
                _fail(f"{dep_path}.identifier", "project cannot depend on itself")
        if dependency["requirement"] is not None:
            _nonblank(dependency["requirement"], f"{dep_path}.requirement")
        _nonblank(dependency["reason"], f"{dep_path}.reason")

    state = _object(root["state"], "$.state", {"milestone", "releases", "validation", "blocker"})
    _nonblank(state["milestone"], "$.state.milestone")
    releases = _object(state["releases"], "$.state.releases", {"current", "accepted", "rollback", "accepted_current", "accepted_rollback"})
    current = _release(releases["current"], "$.state.releases.current", require_canary=True)
    accepted = _release(releases["accepted"], "$.state.releases.accepted")
    rollback = _release(releases["rollback"], "$.state.releases.rollback")
    _release_canaries(releases)
    accepted_current = _enum(releases["accepted_current"], "$.state.releases.accepted_current", ACCEPTED_CURRENT_STATES)
    accepted_rollback = _enum(releases["accepted_rollback"], "$.state.releases.accepted_rollback", ACCEPTED_ROLLBACK_STATES)
    if accepted_current == "NO_ACCEPTED" and accepted is not None:
        _fail("$.state.releases", "NO_ACCEPTED requires accepted=null")
    if accepted_current == "CURRENT_IS_ACCEPTED" and (current is None or accepted is None or current != accepted):
        _fail("$.state.releases", "CURRENT_IS_ACCEPTED requires equal non-null current and accepted releases")
    if accepted_current == "CURRENT_DIFFERS_FROM_ACCEPTED" and (current is None or accepted is None or current == accepted):
        _fail("$.state.releases", "CURRENT_DIFFERS_FROM_ACCEPTED requires unequal non-null current and accepted releases")
    if accepted_rollback == "NO_ROLLBACK" and rollback is not None:
        _fail("$.state.releases", "NO_ROLLBACK requires rollback=null")
    if accepted_rollback == "ACCEPTED_IS_ROLLBACK" and (accepted is None or rollback is None or accepted != rollback):
        _fail("$.state.releases", "ACCEPTED_IS_ROLLBACK requires equal non-null accepted and rollback releases")
    if accepted_rollback == "ROLLBACK_DIFFERS_FROM_ACCEPTED" and (accepted is None or rollback is None or accepted == rollback):
        _fail("$.state.releases", "ROLLBACK_DIFFERS_FROM_ACCEPTED requires unequal non-null accepted and rollback releases")
    if rollback is not None and accepted is None:
        _fail("$.state.releases.rollback", "rollback cannot exist without an accepted release")
    if lifecycle == "ACCEPTED" and accepted_current != "CURRENT_IS_ACCEPTED":
        _fail("$.definition.lifecycle", "ACCEPTED requires CURRENT_IS_ACCEPTED")

    validation = _object(state["validation"], "$.state.validation", {"build", "runtime"})
    _enum(validation["build"], "$.state.validation.build", BUILD_STATES)
    _enum(validation["runtime"], "$.state.validation.runtime", RUNTIME_STATES)

    blocker = state["blocker"]
    if blocker is not None:
        blocker = _object(blocker, "$.state.blocker", {"summary", "since", "next_action"})
        _nonblank(blocker["summary"], "$.state.blocker.summary")
        _timestamp(blocker["since"], "$.state.blocker.since")
        _nonblank(blocker["next_action"], "$.state.blocker.next_action")
    if (lifecycle == "BLOCKED") != (blocker is not None):
        _fail("$.state.blocker", "must be non-null exactly when lifecycle is BLOCKED")

    synchronization = _object(
        root["synchronization"],
        "$.synchronization",
        {"revision", "activity_at", "updated_at", "last_codex_at", "source_commit"},
    )
    _integer(synchronization["revision"], "$.synchronization.revision", 1)
    activity_at = _timestamp(synchronization["activity_at"], "$.synchronization.activity_at")
    updated_at = _timestamp(synchronization["updated_at"], "$.synchronization.updated_at")
    last_codex_at = _timestamp(synchronization["last_codex_at"], "$.synchronization.last_codex_at")
    if activity_at > updated_at:
        _fail("$.synchronization", "activity_at cannot be later than updated_at")
    if last_codex_at > updated_at:
        _fail("$.synchronization", "last_codex_at cannot be later than updated_at")
    _commit(synchronization["source_commit"], "$.synchronization.source_commit")

    if project_directory is not None:
        project_directory = Path(project_directory)
        if project_directory.name != project_id:
            _fail("$.identity.project_id", f"must match directory name {project_directory.name}")
        container = project_directory.parent.name
        if container not in {"projects", "resourcepacks"}:
            _fail("project directory", "must be directly under projects/ or resourcepacks/")
        expected_root = f"{container}/{project_id}"
        if owned_paths[0] != expected_root:
            _fail("$.definition.boundaries.owned_paths[0]", f"must equal {expected_root}")
        for owned_path in owned_paths:
            if owned_path != expected_root and not owned_path.startswith(expected_root + "/"):
                _fail("$.definition.boundaries.owned_paths", f"path is outside project root: {owned_path}")
    return data


def validate_status_transition(previous: dict[str, Any], current: dict[str, Any]) -> None:
    """Require one immutable-identity synchronization step."""

    validate_status(previous)
    validate_status(current)
    before_identity = previous["identity"]
    after_identity = current["identity"]
    if before_identity["uuid"] != after_identity["uuid"]:
        _fail("$.identity.uuid", "UUID is immutable")
    before_revision = previous["synchronization"]["revision"]
    after_revision = current["synchronization"]["revision"]
    if after_revision != before_revision + 1:
        _fail("$.synchronization.revision", f"must advance exactly once from {before_revision} to {before_revision + 1}")
    for field in ("updated_at", "last_codex_at"):
        before_time = _timestamp(previous["synchronization"][field], f"previous.{field}")
        after_time = _timestamp(current["synchronization"][field], f"current.{field}")
        if after_time <= before_time:
            _fail(f"$.synchronization.{field}", "must advance for every task")
    if _timestamp(current["synchronization"]["activity_at"], "current.activity_at") <= _timestamp(
        previous["synchronization"]["activity_at"], "previous.activity_at"
    ):
        _fail("$.synchronization.activity_at", "must advance for every task")
    if current["synchronization"]["source_commit"] == previous["synchronization"]["source_commit"]:
        _fail("$.synchronization.source_commit", "must identify a new coherent source checkpoint")
    if before_identity["project_id"] != after_identity["project_id"] and before_identity["project_id"] not in after_identity["legacy_ids"]:
        _fail("$.identity.legacy_ids", "a renamed project must retain its previous project_id")
    if before_identity["name"] != after_identity["name"] and before_identity["name"] not in after_identity["legacy_names"]:
        _fail("$.identity.legacy_names", "a renamed project must retain its previous name")

    before_release = previous["state"]["releases"]["current"]
    after_release = current["state"]["releases"]["current"]
    before_canaries = _release_canaries(previous["state"]["releases"])
    if after_release is not None:
        after_canary = after_release["canary"]
        historical_canary = before_canaries.get(_canary_release_identity(after_release))
        if historical_canary is not None:
            expected_canary = historical_canary
            relationship = "previously recorded exact artifact bytes"
        elif before_release is None:
            expected_canary = 1
            relationship = "the first current release"
        else:
            expected_canary = before_release["canary"] + 1
            relationship = "new finalized artifact bytes"
        if after_canary != expected_canary:
            _fail(
                "$.state.releases.current.canary",
                f"must be C{expected_canary} for {relationship}",
            )

    for slot in ("current", "accepted", "rollback"):
        release = current["state"]["releases"][slot]
        if release is None:
            continue
        identity = _canary_release_identity(release)
        previous_canary = before_canaries.get(identity)
        if previous_canary is not None and release.get("canary") != previous_canary:
            _fail(
                f"$.state.releases.{slot}.canary",
                f"must preserve C{previous_canary} already recorded for these exact artifact bytes",
            )
    if after_release is not None and after_release["artifact"] is not None:
        policy_present = "runtime_dependency_policy" in after_release
        artifact_changed = _release_identity(before_release) != _release_identity(after_release)
        if artifact_changed and not policy_present:
            _fail(
                "$.state.releases.current.runtime_dependency_policy",
                "is required whenever the exact current artifact identity changes",
            )
        if artifact_changed and "summary" not in after_release:
            _fail(
                "$.state.releases.current.summary",
                "is required whenever the exact current artifact identity changes",
            )
        if artifact_changed and "built_at" not in after_release:
            _fail(
                "$.state.releases.current.built_at",
                "is required whenever the exact current artifact identity changes",
            )
        if (
            before_release is not None
            and "runtime_dependency_policy" in before_release
            and not policy_present
        ):
            _fail(
                "$.state.releases.current.runtime_dependency_policy",
                "cannot be removed from an attested current release",
            )

    before_validation = previous["state"]["validation"]
    after_validation = current["state"]["validation"]
    externally_recordable_results = {"RUNTIME_PASS", "RUNTIME_FAIL", "INCONCLUSIVE"}
    if (
        before_validation["runtime"] != after_validation["runtime"]
        and after_validation["runtime"] in externally_recordable_results
    ):
        before_release = previous["state"]["releases"]["current"]
        after_release = current["state"]["releases"]["current"]
        if (
            before_release is None
            or before_release["artifact"] is None
            or _release_identity(after_release) != _release_identity(before_release)
        ):
            _fail(
                "$.state.releases.current",
                "an external runtime result requires one exact pre-existing artifact/version/hash/source identity",
            )


def _without_v3_canary_metadata(manifest: dict[str, Any]) -> dict[str, Any]:
    normalized = copy.deepcopy(manifest)
    normalized["schema_version"] = 2
    releases = normalized.get("state", {}).get("releases", {})
    if isinstance(releases, dict):
        for slot in ("current", "accepted", "rollback"):
            release = releases.get(slot)
            if isinstance(release, dict):
                release.pop("canary", None)
    return normalized


def validate_status_transition_from_base(previous: dict[str, Any], current: dict[str, Any]) -> str:
    """Validate one status change, including the additive-only V2 migration.

    Returns ``migration`` or ``transition`` for the caller's integration summary.
    """

    previous_schema = previous.get("schema_version")
    current_schema = current.get("schema_version")
    if previous_schema == 2 and current_schema == 3:
        validate_status(current)
        if _without_v3_canary_metadata(current) == previous:
            return "migration"
        _fail(
            "$.schema_version",
            "the V2-to-V3 exception permits only schema_version and release.canary metadata; "
            "project transitions require a V3 base",
        )
    validate_status_transition(previous, current)
    return "transition"


def _git_text(root: Path, *arguments: str) -> str:
    try:
        result = subprocess.run(
            ["git", "-C", str(root), *arguments],
            check=False,
            capture_output=True,
            text=True,
            encoding="utf-8",
        )
    except OSError as exc:
        raise ValidationError(f"cannot run git: {exc}") from exc
    if result.returncode != 0:
        detail = result.stderr.strip() or result.stdout.strip() or f"exit {result.returncode}"
        raise ValidationError(f"git {' '.join(arguments[:2])} failed: {detail}")
    return result.stdout


def _json_from_text(text: str, source: str) -> dict[str, Any]:
    try:
        value = json.loads(text, object_pairs_hook=_reject_duplicate_keys)
    except json.JSONDecodeError as exc:
        raise ValidationError(f"{source}: cannot read valid JSON: {exc}") from exc
    if not isinstance(value, dict):
        raise ValidationError(f"{source}: root must be an object")
    return value


def load_git_statuses(
    root: Path,
    base_ref: str,
) -> tuple[str, dict[str, tuple[PurePosixPath, dict[str, Any]]]]:
    """Load canonical project manifests from one immutable Git commit."""

    root = root.resolve()
    commit = _git_text(root, "rev-parse", "--verify", "--end-of-options", f"{base_ref}^{{commit}}").strip()
    if not COMMIT_RE.fullmatch(commit):
        raise ValidationError(f"base ref {base_ref!r} did not resolve to one commit")
    listing = _git_text(
        root,
        "ls-tree",
        "-r",
        "--name-only",
        "-z",
        commit,
        "--",
        "projects",
        "resourcepacks",
    )
    statuses: dict[str, tuple[PurePosixPath, dict[str, Any]]] = {}
    for raw_path in listing.split("\0"):
        path = PurePosixPath(raw_path)
        if not raw_path or len(path.parts) != 3 or path.name != "WORKBENCH_STATUS.json":
            continue
        manifest = _json_from_text(
            _git_text(root, "show", f"{commit}:{path.as_posix()}"),
            f"{commit}:{path.as_posix()}",
        )
        identity = manifest.get("identity")
        project_uuid = identity.get("uuid") if isinstance(identity, dict) else None
        if not isinstance(project_uuid, str):
            raise ValidationError(f"{commit}:{path.as_posix()}: missing project UUID")
        if project_uuid in statuses:
            raise ValidationError(
                f"{commit}: duplicate project UUID {project_uuid}: "
                f"{statuses[project_uuid][0]} and {path.as_posix()}"
            )
        statuses[project_uuid] = (path, manifest)
    return commit, statuses


def validate_repository_transitions(root: Path, base_ref: str) -> dict[str, int | str]:
    """Validate one integration step per changed project against a stable base.

    A base-to-worktree range containing multiple revisions of the same project is
    deliberately rejected; integrate those status transitions as separate ranges.
    """

    root = root.resolve()
    commit, previous_statuses = load_git_statuses(root, base_ref)
    current_statuses = load_repository_statuses(root)
    removed = sorted(set(previous_statuses) - set(current_statuses))
    if removed:
        descriptions = [
            f"{uuid} ({previous_statuses[uuid][0].as_posix()})"
            for uuid in removed
        ]
        raise ValidationError("project status deletion has no transition policy: " + ", ".join(descriptions))

    added = sorted(set(current_statuses) - set(previous_statuses))
    for project_uuid in added:
        current_path, current = current_statuses[project_uuid]
        release = current["state"]["releases"]["current"]
        if release is not None and release["canary"] != 1:
            raise ValidationError(
                f"{current_path}: a newly added project must begin with C1; "
                "no higher-Canary import exception is defined"
            )

    counts = {
        "base_commit": commit,
        "added": len(added),
        "transition": 0,
        "migration": 0,
    }
    shared = sorted(
        set(previous_statuses) & set(current_statuses),
        key=lambda uuid: str(current_statuses[uuid][0]).casefold(),
    )
    for project_uuid in shared:
        _, previous = previous_statuses[project_uuid]
        current_path, current = current_statuses[project_uuid]
        if previous == current:
            continue
        try:
            kind = validate_status_transition_from_base(previous, current)
        except ValidationError as exc:
            raise ValidationError(f"{current_path} against {commit}: {exc}") from exc
        counts[kind] = int(counts[kind]) + 1
    return counts


def _parse_codex_log(log: str) -> list[dict[str, str]]:
    if not log.startswith("# Codex Log\n"):
        raise ValidationError("CODEX_LOG.md must begin with '# Codex Log'")
    headings = list(LOG_HEADING_RE.finditer(log))
    if not headings:
        raise ValidationError("CODEX_LOG.md must contain at least one structured entry")
    entries: list[dict[str, str]] = []
    for index, heading in enumerate(headings):
        end = headings[index + 1].start() if index + 1 < len(headings) else len(log)
        block = log[heading.end():end]
        fields: dict[str, str] = {}
        for bullet in LOG_BULLET_RE.finditer(block):
            label = bullet.group("label")
            if label in fields:
                raise ValidationError(f"CODEX_LOG.md entry has duplicate field {label}")
            fields[label] = bullet.group("value")
        if set(fields) != LOG_FIELDS:
            missing = sorted(LOG_FIELDS - set(fields))
            extra = sorted(set(fields) - LOG_FIELDS)
            details = []
            if missing:
                details.append("missing " + ", ".join(missing))
            if extra:
                details.append("unknown " + ", ".join(extra))
            raise ValidationError("CODEX_LOG.md entry fields are invalid: " + "; ".join(details))
        _timestamp(heading.group("timestamp"), "CODEX_LOG.md entry timestamp")
        try:
            revision = int(fields["Revision"])
        except ValueError as exc:
            raise ValidationError("CODEX_LOG.md Revision must be an integer") from exc
        if revision < 1:
            raise ValidationError("CODEX_LOG.md Revision must be positive")
        checkpoint = fields["Source checkpoint"]
        if not (checkpoint.startswith("`") and checkpoint.endswith("`")):
            raise ValidationError("CODEX_LOG.md Source checkpoint must be backtick-delimited")
        _commit(checkpoint[1:-1], "CODEX_LOG.md Source checkpoint")
        entries.append(
            {
                "timestamp": heading.group("timestamp"),
                "summary": heading.group("summary"),
                "revision": str(revision),
                "source_commit": checkpoint[1:-1],
            }
        )
    return entries


def validate_codex_log(log: str, manifest: dict[str, Any]) -> None:
    entries = _parse_codex_log(log)
    latest = entries[-1]
    synchronization = manifest["synchronization"]
    if int(latest["revision"]) != synchronization["revision"]:
        raise ValidationError("latest CODEX_LOG.md Revision must match WORKBENCH_STATUS.json")
    if latest["timestamp"] != synchronization["last_codex_at"]:
        raise ValidationError("latest CODEX_LOG.md timestamp must match last_codex_at")
    if latest["source_commit"] != synchronization["source_commit"]:
        raise ValidationError("latest CODEX_LOG.md checkpoint must match source_commit")


def validate_log_append(previous: str, current: str, manifest: dict[str, Any] | None = None) -> None:
    if not current.startswith(previous):
        raise ValidationError("CODEX_LOG.md must be append-only")
    if not current[len(previous):].strip():
        raise ValidationError("every project revision requires a new CODEX_LOG.md entry")
    previous_entries = _parse_codex_log(previous)
    current_entries = _parse_codex_log(current)
    if len(current_entries) != len(previous_entries) + 1:
        raise ValidationError("every project revision must append exactly one CODEX_LOG.md entry")
    if manifest is not None:
        validate_codex_log(current, manifest)


def project_directories(root: Path) -> list[Path]:
    result: list[Path] = []
    for container_name in ("projects", "resourcepacks"):
        container = root / container_name
        if not container.exists():
            continue
        for child in sorted(container.iterdir(), key=lambda item: item.name.casefold()):
            if child.is_dir() and not child.name.startswith("."):
                result.append(child)
    return result


def load_repository_statuses(root: Path) -> dict[str, tuple[Path, dict[str, Any]]]:
    statuses: dict[str, tuple[Path, dict[str, Any]]] = {}
    uuid_paths: dict[str, Path] = {}
    id_paths: dict[str, Path] = {}
    name_namespace: dict[str, tuple[str, Path]] = {}
    id_namespace: dict[str, tuple[str, Path]] = {}

    for directory in project_directories(root):
        for control in ("WORKBENCH_STATUS.json", "TESTING.md", "CODEX_LOG.md"):
            path = directory / control
            if not path.is_file():
                raise ValidationError(f"{directory}: missing required control file {control}")
            text = path.read_text(encoding="utf-8")
            if control != "WORKBENCH_STATUS.json" and not text.strip():
                raise ValidationError(f"{path}: control file must not be empty")
            validate_public_text(text, str(path))
        manifest_path = directory / "WORKBENCH_STATUS.json"
        manifest = load_json(manifest_path)
        validate_status(manifest, directory)
        validate_codex_log((directory / "CODEX_LOG.md").read_text(encoding="utf-8"), manifest)
        project_uuid = manifest["identity"]["uuid"]
        project_id = manifest["identity"]["project_id"]
        if project_uuid in uuid_paths:
            raise ValidationError(f"duplicate project UUID {project_uuid}: {uuid_paths[project_uuid]} and {manifest_path}")
        id_key = project_id.casefold()
        if id_key in id_paths:
            raise ValidationError(f"duplicate project ID {project_id}: {id_paths[id_key]} and {manifest_path}")
        uuid_paths[project_uuid] = manifest_path
        id_paths[id_key] = manifest_path
        statuses[project_uuid] = (manifest_path, manifest)

        identity = manifest["identity"]
        for name in [identity["name"], *identity["aliases"], *identity["legacy_names"]]:
            token = _normalized_token(name)
            existing = name_namespace.get(token)
            if existing and existing[0] != project_uuid:
                raise ValidationError(f"ambiguous project name/alias {name!r}: {existing[1]} and {manifest_path}")
            name_namespace[token] = (project_uuid, manifest_path)
        for identifier in [project_id, *identity["legacy_ids"]]:
            token = _normalized_token(identifier)
            existing = id_namespace.get(token)
            if existing and existing[0] != project_uuid:
                raise ValidationError(f"ambiguous project ID/legacy ID {identifier!r}: {existing[1]} and {manifest_path}")
            id_namespace[token] = (project_uuid, manifest_path)

    known_uuids = set(statuses)
    for project_uuid, (path, manifest) in statuses.items():
        for dependency in manifest["definition"]["dependencies"]:
            if dependency["type"] == "PROJECT" and dependency["identifier"] not in known_uuids:
                raise ValidationError(f"{path}: unknown project dependency UUID {dependency['identifier']}")
            if dependency["type"] == "PROJECT" and dependency["identifier"] == project_uuid:
                raise ValidationError(f"{path}: project cannot depend on itself")
    return statuses


def validate_repository(root: Path) -> dict[str, tuple[Path, dict[str, Any]]]:
    root = root.resolve()
    required = [
        "AGENTS.md",
        "README.md",
        "MIGRATION_FREEZE.md",
        "projects",
        "schemas/workbench-status.schema.json",
        "tools/workbench.py",
        "tools/artifact_retention.py",
        ".github/workflows/validate.yml",
    ]
    missing = [relative for relative in required if not (root / relative).exists()]
    if missing:
        raise ValidationError(f"required bootstrap layout is incomplete: {', '.join(missing)}")
    for schema_name in ("workbench-status.schema.json",):
        schema = load_json(root / "schemas" / schema_name)
        if schema.get("$schema") != "https://json-schema.org/draft/2020-12/schema":
            raise ValidationError(f"schemas/{schema_name}: must declare JSON Schema Draft 2020-12")
        if schema.get("additionalProperties") is not False:
            raise ValidationError(f"schemas/{schema_name}: root must reject additional properties")
    for relative in PUBLIC_TEXT_ROOT_FILES:
        path = root / relative
        validate_public_text(path.read_text(encoding="utf-8"), str(path))
    statuses = load_repository_statuses(root)
    return statuses


def validate_project_change_scope(
    changed_paths: Iterable[str],
    owned_project_ids: Iterable[str],
    allowed_shared_paths: Iterable[str] = (),
) -> None:
    owned = set(owned_project_ids)
    for project_id in owned:
        _project_id(project_id, "owned project ID")
    shared = [_repository_path(path, "allowed shared path") for path in allowed_shared_paths]
    violations: list[str] = []
    for raw_path in changed_paths:
        path = _repository_path(raw_path.replace("\\", "/"), "changed path")
        parts = PurePosixPath(path).parts
        if parts[0] == "originals":
            violations.append(f"{path} (originals is immutable)")
            continue
        if parts[0] in {"projects", "resourcepacks"} and len(parts) >= 2 and parts[1] in owned:
            continue
        if any(path == prefix or path.startswith(prefix + "/") for prefix in shared):
            continue
        violations.append(path)
    if violations:
        raise ValidationError("project task changed paths outside its ownership: " + ", ".join(violations))


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)
    repository = subparsers.add_parser("validate-repository", help="validate repository layout and all project manifests")
    repository.add_argument("--root", type=Path, default=Path("."))
    transitions = subparsers.add_parser(
        "validate-transitions",
        help="validate one status step per changed project against an immutable Git base",
    )
    transitions.add_argument("--root", type=Path, default=Path("."))
    transitions.add_argument("--base-ref", required=True)
    status = subparsers.add_parser("validate-status", help="validate one WORKBENCH_STATUS.json")
    status.add_argument("path", type=Path)
    scope = subparsers.add_parser("check-scope", help="validate explicit project path ownership")
    scope.add_argument("--project", action="append", required=True)
    scope.add_argument("--changed-file", action="append", required=True)
    scope.add_argument("--allow-shared", action="append", default=[])
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    try:
        if args.command == "validate-repository":
            statuses = validate_repository(args.root)
            print(f"Workbench validation passed ({len(statuses)} project manifest(s)).")
        elif args.command == "validate-transitions":
            counts = validate_repository_transitions(args.root, args.base_ref)
            print(
                "Workbench transition validation passed "
                f"against {counts['base_commit']} "
                f"({counts['transition']} strict transition(s), "
                f"{counts['migration']} metadata migration(s), "
                f"{counts['added']} added project(s))."
            )
        elif args.command == "validate-status":
            validate_status(load_json(args.path), args.path.parent)
            print(f"Status validation passed: {args.path}")
        elif args.command == "check-scope":
            validate_project_change_scope(args.changed_file, args.project, args.allow_shared)
            print("Project change scope passed.")
        else:  # pragma: no cover
            parser.error("unknown command")
    except ValidationError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
