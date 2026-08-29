#!/usr/bin/env python3
"""Dependency-free validators for Mynx Flavoured Workbench project state."""

from __future__ import annotations

import argparse
import json
import re
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
LIFECYCLES = {"PLANNED", "ACTIVE", "BLOCKED", "TESTING", "ACCEPTED", "PARKED"}
BUILD_STATES = {
    "NOT_RUN",
    "GENERATED",
    "STATIC_PASS",
    "STATIC_FAIL",
    "CONTROLLED_VALIDATION_PASS",
    "CONTROLLED_VALIDATION_FAIL",
    "INCONCLUSIVE",
}
DEPLOYMENT_STATES = {"NOT_DEPLOYED", "DEPLOYED", "READY_TO_TEST_VERIFIED", "DEPLOYMENT_FAILED"}
RUNTIME_STATES = {"RUNTIME_UNTESTED", "PARTIAL_RUNTIME_PASS", "RUNTIME_PASS", "RUNTIME_FAIL", "INCONCLUSIVE"}
DEPENDENCY_TYPES = {"PROJECT", "MOD", "RESOURCE_PACK", "DATA_PACK", "TOOL", "SERVICE", "OTHER"}
ACCEPTED_CURRENT_STATES = {"NO_ACCEPTED", "CURRENT_IS_ACCEPTED", "CURRENT_DIFFERS_FROM_ACCEPTED"}
ACCEPTED_ROLLBACK_STATES = {"NO_ROLLBACK", "ACCEPTED_IS_ROLLBACK", "ROLLBACK_DIFFERS_FROM_ACCEPTED"}

PROJECT_ID_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
COMMIT_RE = re.compile(r"^[0-9a-f]{40}$")
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
RFC3339_UTC_RE = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,6})?Z$")
WINDOWS_RESERVED_RE = re.compile(r"^(?:con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\.|$)", re.IGNORECASE)
LOG_HEADING_RE = re.compile(r"^## (?P<timestamp>\S+) — (?P<summary>\S(?:.*\S)?)$", re.MULTILINE)
LOG_BULLET_RE = re.compile(r"^- (?P<label>[^:\n]+): (?P<value>\S(?:.*\S)?)$", re.MULTILINE)
LOG_FIELDS = {"Revision", "Source checkpoint", "Changes", "Build/static", "Runtime", "Artifact", "Result", "Next state"}


class ValidationError(ValueError):
    """A deterministic contract validation failure."""


def _fail(path: str, message: str) -> None:
    raise ValidationError(f"{path}: {message}")


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
        _fail(path, "must be lowercase kebab-case")
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


def _release(value: Any, path: str) -> dict[str, Any] | None:
    if value is None:
        return None
    value = _object(value, path, {"version", "artifact", "source_commit"})
    _nonblank(value["version"], f"{path}.version")
    if value["artifact"] is not None:
        _artifact(value["artifact"], f"{path}.artifact")
    _commit(value["source_commit"], f"{path}.source_commit")
    return value


def validate_status(data: dict[str, Any], project_directory: Path | None = None) -> dict[str, Any]:
    """Validate exact manifest shape plus cross-field semantics."""

    root = _object(data, "$", {"$schema", "schema_version", "identity", "definition", "state", "synchronization"})
    if root["$schema"] != STATUS_SCHEMA_REF:
        _fail("$.$schema", f"must equal {STATUS_SCHEMA_REF}")
    if root["schema_version"] != 1:
        _fail("$.schema_version", "must equal 1")

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
    current = _release(releases["current"], "$.state.releases.current")
    accepted = _release(releases["accepted"], "$.state.releases.accepted")
    rollback = _release(releases["rollback"], "$.state.releases.rollback")
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

    validation = _object(state["validation"], "$.state.validation", {"build", "deployment", "runtime"})
    _enum(validation["build"], "$.state.validation.build", BUILD_STATES)
    _enum(validation["deployment"], "$.state.validation.deployment", DEPLOYMENT_STATES)
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
        {"revision", "activity_at", "updated_at", "last_codex_at", "source_commit", "google_sheet"},
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
    sheet = _object(synchronization["google_sheet"], "$.synchronization.google_sheet", {"participates", "exclusion_reason"})
    if not isinstance(sheet["participates"], bool):
        _fail("$.synchronization.google_sheet.participates", "must be boolean")
    if sheet["exclusion_reason"] is not None:
        _nonblank(sheet["exclusion_reason"], "$.synchronization.google_sheet.exclusion_reason")
    if sheet["participates"] == (sheet["exclusion_reason"] is not None):
        _fail("$.synchronization.google_sheet", "participation requires no exclusion reason; exclusion requires a reason")

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
            if control != "WORKBENCH_STATUS.json" and not path.read_text(encoding="utf-8").strip():
                raise ValidationError(f"{path}: control file must not be empty")
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


def _validate_publication_config(path: Path) -> None:
    config = load_json(path)
    keys = {
        "config_version",
        "enabled",
        "repository",
        "authoritative_ref",
        "contract_version",
        "cutover_environment_variable",
        "cutover_required_value",
        "receiver_url_environment_variable",
        "hmac_environment_variable",
        "sheet_preserves",
    }
    _object(config, str(path), keys)
    if config["config_version"] != 1 or config["contract_version"] != 1:
        raise ValidationError(f"{path}: unsupported config or contract version")
    if not isinstance(config["enabled"], bool):
        raise ValidationError(f"{path}: enabled must be boolean")
    if config["repository"] != "Resivore/Mynx-Flavoured-Workbench":
        raise ValidationError(f"{path}: repository guard is incorrect")
    if config["authoritative_ref"] != "refs/heads/main":
        raise ValidationError(f"{path}: authoritative ref must be refs/heads/main")
    if config["sheet_preserves"] != ["Notes"]:
        raise ValidationError(f"{path}: Notes must be the sole human-owned Sheet field")
    for field in keys - {"config_version", "contract_version", "enabled", "sheet_preserves"}:
        _nonblank(config[field], f"{path}.{field}")


def validate_repository(root: Path) -> dict[str, tuple[Path, dict[str, Any]]]:
    root = root.resolve()
    required = [
        "AGENTS.md",
        "README.md",
        "MIGRATION_FREEZE.md",
        "projects",
        "schemas/workbench-status.schema.json",
        "schemas/runtime-state.schema.json",
        "tools/workbench.py",
        "tools/runtime_slots.py",
        "tools/sheet_sync.py",
        "tools/sheet_sync/publication.json",
        "tools/sheet_sync/receiver/Core.gs",
        "tools/sheet_sync/receiver/Code.gs",
        "tools/test_instance_manager/runtime-state.json",
        ".github/workflows/validate.yml",
        ".github/workflows/publish-project-status.yml",
    ]
    missing = [relative for relative in required if not (root / relative).exists()]
    if missing:
        raise ValidationError(f"required bootstrap layout is incomplete: {', '.join(missing)}")
    for schema_name in ("workbench-status.schema.json", "runtime-state.schema.json"):
        schema = load_json(root / "schemas" / schema_name)
        if schema.get("$schema") != "https://json-schema.org/draft/2020-12/schema":
            raise ValidationError(f"schemas/{schema_name}: must declare JSON Schema Draft 2020-12")
        if schema.get("additionalProperties") is not False:
            raise ValidationError(f"schemas/{schema_name}: root must reject additional properties")
    _validate_publication_config(root / "tools" / "sheet_sync" / "publication.json")
    statuses = load_repository_statuses(root)
    try:
        from .runtime_slots import validate_runtime_state
    except ImportError:  # Direct execution from tools/.
        from runtime_slots import validate_runtime_state  # type: ignore

    runtime_state = load_json(root / "tools" / "test_instance_manager" / "runtime-state.json")
    project_index = {
        project_uuid: manifest["identity"]["project_id"]
        for project_uuid, (_, manifest) in statuses.items()
    }
    validate_runtime_state(runtime_state, project_index=project_index)
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
