#!/usr/bin/env python3
"""Pure two-slot runtime-state validation and atomic transition planning."""

from __future__ import annotations

import copy
import hashlib
import json
import re
from pathlib import Path, PurePosixPath, PureWindowsPath
from typing import Any

try:
    from .workbench import (
        ValidationError,
        _commit,
        _enum,
        _fail,
        _integer,
        _nonblank,
        _object,
        _project_id,
        _sha256,
        _timestamp,
        _uuid,
    )
except ImportError:  # Direct execution/import from tools/.
    from workbench import (  # type: ignore
        ValidationError,
        _commit,
        _enum,
        _fail,
        _integer,
        _nonblank,
        _object,
        _project_id,
        _sha256,
        _timestamp,
        _uuid,
    )


RUNTIME_SCHEMA_REF = "../../schemas/runtime-state.schema.json"
ACTIVATION_STATES = {"GATED", "ACTIVE"}
ARTIFACT_KINDS = {"MOD"}
SLOT_DEPLOYMENT_STATES = {"NOT_DEPLOYED", "DEPLOYED", "READY_TO_TEST_VERIFIED"}
SLOT_RESULTS = {"UNTESTED", "PASS", "FAIL", "INCONCLUSIVE"}
ARTIFACT_SOURCE_TYPES = {"REPOSITORY", "ADOPTED_TARGET"}
BASELINE_DISPOSITIONS = {"PENDING", "ADOPTED", "TRANSITIONED"}
PROJECT_IDENTITY_SOURCES = {"CURRENT_MANIFEST", "FROZEN_LEGACY"}
TRANSITIONS = {
    "ASSIGN_SLOT",
    "UPDATE_SLOT",
    "SET_PROFILE",
    "MARK_DEPLOYED",
    "MARK_READY",
    "RECORD_RESULT",
    "REMOVE_SLOT",
    "PROMOTE_SLOT",
    "PROMOTE_UNTESTED_CANDIDATE",
    "REMOVE_ACCEPTED",
}
UNTESTED_PROMOTION_AUTHORIZATION = "USER_APPROVED_UNTESTED_PROMOTION"
OWNERSHIP_KEY_RE = re.compile(r"^mod:[a-z0-9_.-]+$")
WINDOWS_RESERVED_RE = re.compile(r"^(?:con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\.|$)", re.IGNORECASE)
CANARY_VERSION_RE = re.compile(r"(?:^|[^a-z0-9])canary[\s._-]*(\d+)(?:$|[^0-9])", re.IGNORECASE)


def _artifact(value: Any, path: str) -> dict[str, Any]:
    artifact = _object(value, path, {"artifact_id", "kind", "filename", "sha256", "ownership_keys", "source"})
    _uuid(artifact["artifact_id"], f"{path}.artifact_id")
    _enum(artifact["kind"], f"{path}.kind", ARTIFACT_KINDS)
    filename = _nonblank(artifact["filename"], f"{path}.filename")
    if (
        filename in {".", ".."}
        or PurePosixPath(filename).name != filename
        or any(character in filename for character in '<>:"/\\|?*')
        or any(ord(character) < 32 for character in filename)
        or filename.endswith((".", " "))
        or WINDOWS_RESERVED_RE.match(filename)
    ):
        _fail(f"{path}.filename", "must be a Windows-safe basename")
    _sha256(artifact["sha256"], f"{path}.sha256")
    ownership_keys = artifact["ownership_keys"]
    if not isinstance(ownership_keys, list) or len(ownership_keys) != 1:
        _fail(f"{path}.ownership_keys", "MOD artifacts require exactly one mod:<fabric_id> key")
    seen: set[str] = set()
    for index, ownership_key in enumerate(ownership_keys):
        ownership_key = _nonblank(ownership_key, f"{path}.ownership_keys[{index}]")
        if not OWNERSHIP_KEY_RE.fullmatch(ownership_key):
            _fail(f"{path}.ownership_keys[{index}]", "must be a lowercase namespaced ownership key")
        normalized = ownership_key.casefold()
        if normalized in seen:
            _fail(f"{path}.ownership_keys", f"duplicate ownership key: {ownership_key}")
        seen.add(normalized)

    source = _object(artifact["source"], f"{path}.source", {"type", "path"})
    source_type = _enum(source["type"], f"{path}.source.type", ARTIFACT_SOURCE_TYPES)
    source_path = _nonblank(source["path"], f"{path}.source.path")
    pure_path = PurePosixPath(source_path)
    if (
        pure_path.is_absolute()
        or bool(PureWindowsPath(source_path).drive)
        or source_path != pure_path.as_posix()
        or any(part in {"", ".", ".."} for part in pure_path.parts)
        or "\\" in source_path
        or ":" in source_path
    ):
        _fail(f"{path}.source.path", "must be a normalized relative POSIX path")
    if source_type == "REPOSITORY":
        if pure_path.parts[0].casefold() == "originals":
            _fail(f"{path}.source.path", "repository sources cannot be under originals/")
        if pure_path.name != filename:
            _fail(f"{path}.source.path", "repository source basename must match artifact filename")
    elif pure_path.parts != ("mods", filename):
        _fail(f"{path}.source.path", "adopted target sources must be mods/<artifact filename>")
    return artifact


def _unit(value: Any, path: str, project_index: dict[str, str] | None) -> dict[str, Any]:
    unit = _object(
        value,
        path,
        {
            "deployment_id",
            "project_uuid",
            "project_id",
            "project_identity_source",
            "version",
            "source_commit",
            "artifacts",
        },
    )
    _uuid(unit["deployment_id"], f"{path}.deployment_id")
    project_uuid = _uuid(unit["project_uuid"], f"{path}.project_uuid")
    project_id = _project_id(unit["project_id"], f"{path}.project_id")
    identity_source = _enum(
        unit["project_identity_source"],
        f"{path}.project_identity_source",
        PROJECT_IDENTITY_SOURCES,
    )
    _nonblank(unit["version"], f"{path}.version")
    if unit["source_commit"] is not None:
        _commit(unit["source_commit"], f"{path}.source_commit")
    elif identity_source == "CURRENT_MANIFEST":
        _fail(f"{path}.source_commit", "CURRENT_MANIFEST deployments require an exact source commit")
    artifacts = unit["artifacts"]
    if not isinstance(artifacts, list) or not artifacts:
        _fail(f"{path}.artifacts", "must be a nonempty array")
    for index, artifact in enumerate(artifacts):
        _artifact(artifact, f"{path}.artifacts[{index}]")
    if project_index is not None:
        expected_id = project_index.get(project_uuid)
        if expected_id is None and identity_source != "FROZEN_LEGACY":
            _fail(f"{path}.project_uuid", "does not resolve to a repository project manifest")
        if expected_id is not None and expected_id != project_id:
            _fail(f"{path}.project_id", f"does not match manifest project_id {expected_id}")
    return unit


def _accepted_member(value: Any, path: str, project_index: dict[str, str] | None) -> dict[str, Any]:
    member = _object(value, path, {"unit", "accepted_at"})
    _unit(member["unit"], f"{path}.unit", project_index)
    if member["accepted_at"] is not None:
        _timestamp(member["accepted_at"], f"{path}.accepted_at")
    return member


def _slot(value: Any, path: str, project_index: dict[str, str] | None) -> dict[str, Any]:
    required = {"unit", "replaces_accepted_deployment_id", "deployment", "runtime_result"}
    allowed = required | {"dependency_overrides"}
    if not isinstance(value, dict):
        _fail(path, "must be an object")
    missing = required - set(value)
    unknown = set(value) - allowed
    if missing:
        _fail(path, f"missing required fields: {', '.join(sorted(missing))}")
    if unknown:
        _fail(path, f"unknown fields: {', '.join(sorted(unknown))}")
    slot = value
    unit = _unit(slot["unit"], f"{path}.unit", project_index)
    if unit["project_identity_source"] != "CURRENT_MANIFEST":
        _fail(f"{path}.unit.project_identity_source", "runtime slots require CURRENT_MANIFEST identity")
    if slot["replaces_accepted_deployment_id"] is not None:
        _uuid(slot["replaces_accepted_deployment_id"], f"{path}.replaces_accepted_deployment_id")
    dependency_overrides = slot.get("dependency_overrides", [])
    if not isinstance(dependency_overrides, list):
        _fail(f"{path}.dependency_overrides", "must be an array")
    seen_overrides: set[str] = set()
    for index, deployment_id in enumerate(dependency_overrides):
        deployment_id = _uuid(deployment_id, f"{path}.dependency_overrides[{index}]")
        if deployment_id in seen_overrides:
            _fail(f"{path}.dependency_overrides", f"duplicate deployment UUID: {deployment_id}")
        seen_overrides.add(deployment_id)

    deployment = _object(slot["deployment"], f"{path}.deployment", {"state", "deployed_at", "ready_verified_at"})
    deployment_state = _enum(deployment["state"], f"{path}.deployment.state", SLOT_DEPLOYMENT_STATES)
    deployed_at = None if deployment["deployed_at"] is None else _timestamp(deployment["deployed_at"], f"{path}.deployment.deployed_at")
    ready_at = None if deployment["ready_verified_at"] is None else _timestamp(deployment["ready_verified_at"], f"{path}.deployment.ready_verified_at")
    if deployment_state == "NOT_DEPLOYED" and (deployed_at is not None or ready_at is not None):
        _fail(f"{path}.deployment", "NOT_DEPLOYED requires null timestamps")
    if deployment_state == "DEPLOYED" and (deployed_at is None or ready_at is not None):
        _fail(f"{path}.deployment", "DEPLOYED requires deployed_at and no ready_verified_at")
    if deployment_state == "READY_TO_TEST_VERIFIED" and (deployed_at is None or ready_at is None):
        _fail(f"{path}.deployment", "READY_TO_TEST_VERIFIED requires both deployment timestamps")
    if deployed_at is not None and ready_at is not None and ready_at < deployed_at:
        _fail(f"{path}.deployment", "ready verification cannot precede deployment")

    runtime_result = _object(
        slot["runtime_result"],
        f"{path}.runtime_result",
        {"classification", "recorded_at", "evidence"},
    )
    classification = _enum(runtime_result["classification"], f"{path}.runtime_result.classification", SLOT_RESULTS)
    recorded_at = None if runtime_result["recorded_at"] is None else _timestamp(runtime_result["recorded_at"], f"{path}.runtime_result.recorded_at")
    if classification == "UNTESTED" and recorded_at is not None:
        _fail(f"{path}.runtime_result", "UNTESTED requires recorded_at=null")
    if classification != "UNTESTED" and recorded_at is None:
        _fail(f"{path}.runtime_result", "a recorded result requires recorded_at")
    if classification != "UNTESTED" and deployment_state != "READY_TO_TEST_VERIFIED":
        _fail(f"{path}.runtime_result", "runtime results require READY_TO_TEST_VERIFIED deployment")
    if ready_at is not None and recorded_at is not None and recorded_at < ready_at:
        _fail(f"{path}.runtime_result.recorded_at", "cannot precede ready verification")
    evidence = _object(runtime_result["evidence"], f"{path}.runtime_result.evidence", {"passed", "failed"})
    evidence_sets: dict[str, set[str]] = {}
    for label in ("passed", "failed"):
        values = evidence[label]
        if not isinstance(values, list):
            _fail(f"{path}.runtime_result.evidence.{label}", "must be an array")
        normalized: set[str] = set()
        for index, item in enumerate(values):
            item = _nonblank(item, f"{path}.runtime_result.evidence.{label}[{index}]")
            key = item.casefold()
            if key in normalized:
                _fail(f"{path}.runtime_result.evidence.{label}", f"duplicate evidence: {item}")
            normalized.add(key)
        evidence_sets[label] = normalized
    overlap = evidence_sets["passed"].intersection(evidence_sets["failed"])
    if overlap:
        _fail(f"{path}.runtime_result.evidence", "the same check cannot be both passed and failed")
    if classification == "UNTESTED" and (evidence["passed"] or evidence["failed"]):
        _fail(f"{path}.runtime_result.evidence", "UNTESTED requires empty evidence")
    if classification == "PASS" and not evidence["passed"]:
        _fail(f"{path}.runtime_result.evidence.passed", "PASS requires at least one passed check")
    if classification == "PASS" and evidence["failed"]:
        _fail(f"{path}.runtime_result.evidence.failed", "PASS cannot retain failed evidence")
    if classification == "FAIL" and not evidence["failed"]:
        _fail(f"{path}.runtime_result.evidence.failed", "FAIL requires at least one failed check")
    if classification == "INCONCLUSIVE" and not (evidence["passed"] or evidence["failed"]):
        _fail(f"{path}.runtime_result.evidence", "INCONCLUSIVE requires observed evidence")
    return slot


def _all_units(state: dict[str, Any]) -> list[tuple[str, dict[str, Any]]]:
    units: list[tuple[str, dict[str, Any]]] = [
        (f"accepted[{index}]", member["unit"])
        for index, member in enumerate(state["accepted_baseline"]["members"])
    ]
    for label in ("A", "B"):
        if state["slots"][label] is not None:
            units.append((f"slot {label}", state["slots"][label]["unit"]))
    return units


def _resolved_units_unchecked(state: dict[str, Any]) -> list[dict[str, Any]]:
    suppressed: set[str] = set()
    for slot in state["slots"].values():
        if slot is None:
            continue
        if slot["replaces_accepted_deployment_id"] is not None:
            suppressed.add(slot["replaces_accepted_deployment_id"])
        suppressed.update(slot.get("dependency_overrides", []))
    result = [
        member["unit"]
        for member in state["accepted_baseline"]["members"]
        if member["unit"]["deployment_id"] not in suppressed
    ]
    result.extend(state["slots"][label]["unit"] for label in ("A", "B") if state["slots"][label] is not None)
    return result


def _validate_profile_ownership(units: list[dict[str, Any]], path: str) -> None:
    filenames: dict[tuple[str, str], str] = {}
    ownership: dict[str, str] = {}
    for unit in units:
        location = f"{unit['project_id']}@{unit['version']}"
        for artifact in unit["artifacts"]:
            filename_key = (artifact["kind"].casefold(), artifact["filename"].casefold())
            if filename_key in filenames:
                _fail(path, f"artifact filename collision {artifact['filename']!r}: {filenames[filename_key]} and {location}")
            filenames[filename_key] = location
            for ownership_key in artifact["ownership_keys"]:
                normalized = ownership_key.casefold()
                if normalized in ownership:
                    _fail(path, f"ownership collision {ownership_key!r}: {ownership[normalized]} and {location}")
                ownership[normalized] = location


def validate_runtime_state(state: dict[str, Any], project_index: dict[str, str] | None = None) -> dict[str, Any]:
    """Validate fixed two-slot state, replacement rules, and effective ownership."""

    state = _object(state, "$", {"$schema", "schema_version", "activation", "revision", "updated_at", "accepted_baseline", "slots"})
    if state["$schema"] != RUNTIME_SCHEMA_REF:
        _fail("$.$schema", f"must equal {RUNTIME_SCHEMA_REF}")
    if state["schema_version"] != 1:
        _fail("$.schema_version", "must equal 1")
    activation = _enum(state["activation"], "$.activation", ACTIVATION_STATES)
    state_revision = _integer(state["revision"], "$.revision", 0)
    updated_at = _timestamp(state["updated_at"], "$.updated_at")

    baseline = _object(state["accepted_baseline"], "$.accepted_baseline", {"revision", "provenance", "members"})
    baseline_revision = _integer(baseline["revision"], "$.accepted_baseline.revision", 0)
    if baseline_revision > state_revision:
        _fail("$.accepted_baseline.revision", "cannot exceed state revision")
    provenance = _object(
        baseline["provenance"],
        "$.accepted_baseline.provenance",
        {
            "source_repository",
            "source_commit",
            "source_profile",
            "legacy_ledger_sha256",
            "accepted_artifact_count",
            "overlay_order",
            "readiness",
            "diagnostics",
            "physical_disposition",
        },
    )
    _nonblank(provenance["source_repository"], "$.accepted_baseline.provenance.source_repository")
    _commit(provenance["source_commit"], "$.accepted_baseline.provenance.source_commit")
    _nonblank(provenance["source_profile"], "$.accepted_baseline.provenance.source_profile")
    _sha256(provenance["legacy_ledger_sha256"], "$.accepted_baseline.provenance.legacy_ledger_sha256")
    accepted_artifact_count = _integer(
        provenance["accepted_artifact_count"],
        "$.accepted_baseline.provenance.accepted_artifact_count",
        0,
    )
    overlay_order = provenance["overlay_order"]
    if not isinstance(overlay_order, list) or len(overlay_order) > 2:
        _fail("$.accepted_baseline.provenance.overlay_order", "must be an array of at most two project UUIDs")
    seen_overlay_projects: set[str] = set()
    for index, project_uuid in enumerate(overlay_order):
        project_uuid = _uuid(project_uuid, f"$.accepted_baseline.provenance.overlay_order[{index}]")
        if project_uuid in seen_overlay_projects:
            _fail("$.accepted_baseline.provenance.overlay_order", f"duplicate project UUID: {project_uuid}")
        seen_overlay_projects.add(project_uuid)
    if provenance["readiness"] != "READY_TO_TEST_VERIFIED":
        _fail("$.accepted_baseline.provenance.readiness", "must preserve READY_TO_TEST_VERIFIED")
    diagnostics = _integer(provenance["diagnostics"], "$.accepted_baseline.provenance.diagnostics", 0)
    physical_disposition = _enum(
        provenance["physical_disposition"],
        "$.accepted_baseline.provenance.physical_disposition",
        BASELINE_DISPOSITIONS,
    )
    if activation == "GATED" and physical_disposition != "PENDING":
        _fail("$.accepted_baseline.provenance.physical_disposition", "GATED state requires PENDING disposition")
    if activation == "ACTIVE":
        if physical_disposition == "PENDING":
            _fail("$.accepted_baseline.provenance.physical_disposition", "ACTIVE state requires adopted physical state")
        if diagnostics != 0:
            _fail("$.accepted_baseline.provenance.diagnostics", "ACTIVE state requires zero diagnostics")
    if not isinstance(baseline["members"], list):
        _fail("$.accepted_baseline.members", "must be an array")
    actual_artifact_count = 0
    for index, member in enumerate(baseline["members"]):
        _accepted_member(member, f"$.accepted_baseline.members[{index}]", project_index)
        actual_artifact_count += len(member["unit"]["artifacts"])
        if member["accepted_at"] is not None and _timestamp(
            member["accepted_at"], f"$.accepted_baseline.members[{index}].accepted_at"
        ) > updated_at:
            _fail(f"$.accepted_baseline.members[{index}].accepted_at", "cannot be later than state updated_at")
    if actual_artifact_count != accepted_artifact_count:
        _fail(
            "$.accepted_baseline.provenance.accepted_artifact_count",
            f"declares {accepted_artifact_count}, but members contain {actual_artifact_count} artifacts",
        )

    slots = _object(state["slots"], "$.slots", {"A", "B"})
    for label in ("A", "B"):
        if slots[label] is not None:
            _slot(slots[label], f"$.slots.{label}", project_index)
            slot = slots[label]
            for timestamp_field in (slot["deployment"]["deployed_at"], slot["deployment"]["ready_verified_at"], slot["runtime_result"]["recorded_at"]):
                if timestamp_field is not None and _timestamp(timestamp_field, f"$.slots.{label}.timestamp") > updated_at:
                    _fail(f"$.slots.{label}", "slot evidence timestamp cannot be later than state updated_at")

    deployment_ids: dict[str, str] = {}
    artifact_ids: dict[str, str] = {}
    project_ids_by_uuid: dict[str, str] = {}
    project_uuids_by_id: dict[str, str] = {}
    accepted_projects: dict[str, tuple[str, str]] = {}
    accepted_by_deployment: dict[str, dict[str, Any]] = {}
    for index, member in enumerate(baseline["members"]):
        unit = member["unit"]
        location = f"accepted[{index}]"
        project_key = unit["project_uuid"]
        if project_key in accepted_projects:
            _fail("$.accepted_baseline.members", f"duplicate accepted project UUID {project_key}")
        if unit["project_id"].casefold() in {item[1].casefold() for item in accepted_projects.values()}:
            _fail("$.accepted_baseline.members", f"duplicate accepted project ID {unit['project_id']}")
        accepted_projects[project_key] = (unit["deployment_id"], unit["project_id"])
        accepted_by_deployment[unit["deployment_id"]] = unit

    slot_projects: set[str] = set()
    replacement_ids: set[str] = set()
    for label in ("A", "B"):
        slot = slots[label]
        if slot is None:
            continue
        unit = slot["unit"]
        if unit["project_uuid"] in slot_projects:
            _fail("$.slots", f"project {unit['project_uuid']} occupies both slots")
        slot_projects.add(unit["project_uuid"])
        replacement_id = slot["replaces_accepted_deployment_id"]
        accepted_for_project = accepted_projects.get(unit["project_uuid"])
        if accepted_for_project is None and replacement_id is not None:
            _fail(f"$.slots.{label}.replaces_accepted_deployment_id", "new project cannot replace an accepted deployment")
        if accepted_for_project is not None:
            expected_deployment_id, expected_project_id = accepted_for_project
            if replacement_id != expected_deployment_id or unit["project_id"] != expected_project_id:
                _fail(f"$.slots.{label}.replaces_accepted_deployment_id", "accepted project upgrade must reference its exact accepted deployment")
        if replacement_id is not None:
            if replacement_id not in accepted_by_deployment:
                _fail(f"$.slots.{label}.replaces_accepted_deployment_id", "does not name an accepted deployment")
            if replacement_id in replacement_ids:
                _fail("$.slots", "two slots cannot replace the same accepted deployment")
            replacement_ids.add(replacement_id)
        slot_ownership = {
            ownership_key.casefold()
            for artifact in unit["artifacts"]
            for ownership_key in artifact["ownership_keys"]
        }
        for index, dependency_id in enumerate(slot.get("dependency_overrides", [])):
            dependency_path = f"$.slots.{label}.dependency_overrides[{index}]"
            dependency_unit = accepted_by_deployment.get(dependency_id)
            if dependency_unit is None:
                _fail(dependency_path, "does not name an accepted deployment")
            if dependency_unit["project_uuid"] == unit["project_uuid"]:
                _fail(dependency_path, "the slot project's accepted release must use replaces_accepted_deployment_id")
            if dependency_id in replacement_ids:
                _fail("$.slots", "two slot replacement paths cannot suppress the same accepted deployment")
            dependency_ownership = {
                ownership_key.casefold()
                for artifact in dependency_unit["artifacts"]
                for ownership_key in artifact["ownership_keys"]
            }
            if not dependency_ownership.issubset(slot_ownership):
                _fail(dependency_path, "slot artifacts must replace every ownership key of the accepted dependency")
            replacement_ids.add(dependency_id)

    for location, unit in _all_units(state):
        project_uuid = unit["project_uuid"]
        project_id = unit["project_id"]
        if project_uuid in project_ids_by_uuid and project_ids_by_uuid[project_uuid] != project_id:
            _fail(location, f"project UUID {project_uuid} claims multiple project IDs")
        id_key = project_id.casefold()
        if id_key in project_uuids_by_id and project_uuids_by_id[id_key] != project_uuid:
            _fail(location, f"project ID {project_id} claims multiple UUIDs")
        project_ids_by_uuid[project_uuid] = project_id
        project_uuids_by_id[id_key] = project_uuid
        deployment_id = unit["deployment_id"]
        if deployment_id in deployment_ids:
            _fail(location, f"duplicate deployment UUID also owned by {deployment_ids[deployment_id]}")
        deployment_ids[deployment_id] = location
        for artifact in unit["artifacts"]:
            artifact_id = artifact["artifact_id"]
            if artifact_id in artifact_ids:
                _fail(location, f"duplicate artifact UUID also owned by {artifact_ids[artifact_id]}")
            artifact_ids[artifact_id] = location

    _validate_profile_ownership([member["unit"] for member in baseline["members"]], "accepted baseline")
    _validate_profile_ownership(_resolved_units_unchecked(state), "effective profile")
    return state


def resolve_profile(state: dict[str, Any], project_index: dict[str, str] | None = None) -> list[dict[str, Any]]:
    """Return accepted members after replacement suppression, then A and B."""

    validate_runtime_state(state, project_index)
    return copy.deepcopy(_resolved_units_unchecked(state))


def state_digest(state: dict[str, Any]) -> str:
    encoded = json.dumps(state, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def render_title_state(
    state: dict[str, Any],
    project_display_names: dict[str, str],
    project_index: dict[str, str] | None = None,
) -> dict[str, Any]:
    """Render the title-screen identity solely from canonical V2 state.

    ``accepted_baseline.revision`` is the durable Stack version. Project names
    come from the repository manifest catalog supplied by the manager, and the
    Canary number comes from the slot unit's canonical version field. Legacy
    marker metadata is deliberately not an input.
    """

    validate_runtime_state(state, project_index)
    if not isinstance(project_display_names, dict):
        raise ValidationError("project display-name catalog must be an object")

    stack_version = state["accepted_baseline"]["revision"]
    if stack_version < 1:
        raise ValidationError("canonical title display requires initialized Stack v1 or newer")
    baseline_label = f"Stack v{stack_version}"
    lines = [f"Baseline: {baseline_label}"]
    slots: dict[str, dict[str, Any]] = {}
    for label in ("A", "B"):
        slot = state["slots"][label]
        if slot is None:
            line = f"Slot {label}: Empty"
            slots[label] = {"occupied": False, "line": line}
            lines.append(line)
            continue

        unit = slot["unit"]
        project_uuid = unit["project_uuid"]
        display_name = project_display_names.get(project_uuid)
        if display_name is None:
            raise ValidationError(
                f"cannot render slot {label}: project {project_uuid} has no canonical display name"
            )
        display_name = _nonblank(display_name, f"project_display_names[{project_uuid!r}]")
        if "\r" in display_name or "\n" in display_name:
            raise ValidationError(f"cannot render slot {label}: canonical display name must occupy one line")
        match = CANARY_VERSION_RE.search(unit["version"])
        if match is None:
            raise ValidationError(
                f"cannot render slot {label}: canonical version {unit['version']!r} has no Canary number"
            )
        canary = int(match.group(1))
        line = f"Slot {label}: {display_name} - Canary {canary}"
        slots[label] = {
            "occupied": True,
            "project_uuid": project_uuid,
            "project_display_name": display_name,
            "canary": canary,
            "line": line,
        }
        lines.append(line)

    return {
        "baseline": {
            "stack_version": stack_version,
            "stack_label": baseline_label,
            "line": lines[0],
        },
        "slots": slots,
        "lines": lines,
    }


def candidate_declaration(
    unit: dict[str, Any],
    replaces_accepted_deployment_id: str | None = None,
    dependency_overrides: list[str] | None = None,
) -> dict[str, Any]:
    """Build immutable candidate input; deployment evidence is intentionally absent."""

    declaration = {
        "unit": copy.deepcopy(unit),
        "replaces_accepted_deployment_id": replaces_accepted_deployment_id,
    }
    if dependency_overrides:
        declaration["dependency_overrides"] = copy.deepcopy(dependency_overrides)
    return declaration


def _normalize_candidate_declaration(declaration: Any) -> dict[str, Any]:
    required = {"unit", "replaces_accepted_deployment_id"}
    allowed = required | {"dependency_overrides"}
    if not isinstance(declaration, dict):
        _fail("candidate", "must be an object")
    missing = required - set(declaration)
    unknown = set(declaration) - allowed
    if missing:
        _fail("candidate", f"missing required fields: {', '.join(sorted(missing))}")
    if unknown:
        _fail("candidate", f"unknown fields: {', '.join(sorted(unknown))}")
    normalized = copy.deepcopy(declaration)
    if "dependency_overrides" in normalized and not isinstance(normalized["dependency_overrides"], list):
        _fail("candidate.dependency_overrides", "must be an array")
    return normalized


def _materialize_candidate(declaration: Any, existing_slots: dict[str, Any]) -> dict[str, Any]:
    declaration = _normalize_candidate_declaration(declaration)
    identity = {
        "unit": declaration["unit"],
        "replaces_accepted_deployment_id": declaration["replaces_accepted_deployment_id"],
        "dependency_overrides": declaration.get("dependency_overrides", []),
    }
    for label in ("A", "B"):
        existing = existing_slots[label]
        if existing is not None and {
            "unit": existing["unit"],
            "replaces_accepted_deployment_id": existing["replaces_accepted_deployment_id"],
            "dependency_overrides": existing.get("dependency_overrides", []),
        } == identity:
            return copy.deepcopy(existing)
    slot = {
        "unit": copy.deepcopy(declaration["unit"]),
        "replaces_accepted_deployment_id": declaration["replaces_accepted_deployment_id"],
        "deployment": {"state": "NOT_DEPLOYED", "deployed_at": None, "ready_verified_at": None},
        "runtime_result": {
            "classification": "UNTESTED",
            "recorded_at": None,
            "evidence": {"passed": [], "failed": []},
        },
    }
    if declaration.get("dependency_overrides"):
        slot["dependency_overrides"] = copy.deepcopy(declaration["dependency_overrides"])
    return slot


def _byte_composition(unit: dict[str, Any]) -> tuple[tuple[Any, ...], ...]:
    """Return project-owned artifact bytes/ownership, excluding rebuilt metadata."""

    return tuple(
        sorted(
            (
                artifact["kind"],
                artifact["sha256"].casefold(),
                tuple(sorted(key.casefold() for key in artifact["ownership_keys"])),
            )
            for artifact in unit["artifacts"]
        )
    )


def plan_transition(
    state: dict[str, Any],
    expected_revision: int,
    operation: dict[str, Any],
    at: str,
    project_index: dict[str, str] | None = None,
) -> dict[str, Any]:
    """Apply one pure CAS transition and return a fully validated next state."""

    validate_runtime_state(state, project_index)
    if state["revision"] != expected_revision:
        raise ValidationError(f"stale runtime-state revision: expected {expected_revision}, found {state['revision']}")
    transition_at = _timestamp(at, "transition timestamp")
    if transition_at < _timestamp(state["updated_at"], "$.updated_at"):
        raise ValidationError("transition timestamp cannot precede current state updated_at")
    if not isinstance(operation, dict) or "type" not in operation:
        raise ValidationError("operation must be an object with type")
    operation_type = _enum(operation["type"], "operation.type", TRANSITIONS)
    next_state = copy.deepcopy(state)
    slots = next_state["slots"]

    if operation_type == "ASSIGN_SLOT":
        if set(operation) != {"type", "candidate"}:
            raise ValidationError("ASSIGN_SLOT requires exactly type and candidate")
        target = "A" if slots["A"] is None else "B" if slots["B"] is None else None
        if target is None:
            raise ValidationError("both runtime test slots are occupied")
        slots[target] = _materialize_candidate(operation["candidate"], state["slots"])
    elif operation_type == "UPDATE_SLOT":
        if set(operation) != {"type", "slot", "candidate"}:
            raise ValidationError("UPDATE_SLOT requires exactly type, slot, and candidate")
        label = operation["slot"]
        if label not in {"A", "B"}:
            raise ValidationError("operation.slot must be A or B")
        if slots[label] is None:
            raise ValidationError(f"slot {label} is empty")
        declaration = _normalize_candidate_declaration(operation["candidate"])
        prior = slots[label]
        if (
            declaration["replaces_accepted_deployment_id"] is None
            and isinstance(declaration["unit"], dict)
            and declaration["unit"].get("project_uuid") == prior["unit"]["project_uuid"]
        ):
            declaration["replaces_accepted_deployment_id"] = prior["replaces_accepted_deployment_id"]
        if (
            "dependency_overrides" not in declaration
            and isinstance(declaration["unit"], dict)
            and declaration["unit"].get("project_uuid") == prior["unit"]["project_uuid"]
            and prior.get("dependency_overrides")
        ):
            declaration["dependency_overrides"] = copy.deepcopy(prior["dependency_overrides"])
        slots[label] = _materialize_candidate(declaration, state["slots"])
    elif operation_type == "SET_PROFILE":
        if set(operation) != {"type", "candidates"} or not isinstance(operation["candidates"], list):
            raise ValidationError("SET_PROFILE requires a candidates array")
        if len(operation["candidates"]) > 2:
            raise ValidationError("runtime profile cannot contain more than two candidates")
        slots["A"] = _materialize_candidate(operation["candidates"][0], state["slots"]) if operation["candidates"] else None
        slots["B"] = _materialize_candidate(operation["candidates"][1], state["slots"]) if len(operation["candidates"]) == 2 else None
    elif operation_type == "REMOVE_ACCEPTED":
        if set(operation) != {"type", "project_uuid"}:
            raise ValidationError("REMOVE_ACCEPTED requires exactly type and project_uuid")
        project_uuid = _uuid(operation["project_uuid"], "operation.project_uuid")
        members = next_state["accepted_baseline"]["members"]
        indexes = [
            index
            for index, member in enumerate(members)
            if member["unit"]["project_uuid"] == project_uuid
        ]
        if len(indexes) != 1:
            raise ValidationError("REMOVE_ACCEPTED requires exactly one accepted project match")
        del members[indexes[0]]
        next_state["accepted_baseline"]["provenance"]["accepted_artifact_count"] = sum(
            len(existing["unit"]["artifacts"]) for existing in members
        )
        if next_state["activation"] == "ACTIVE":
            next_state["accepted_baseline"]["provenance"]["physical_disposition"] = "TRANSITIONED"
        next_state["accepted_baseline"]["revision"] += 1
    elif operation_type == "PROMOTE_UNTESTED_CANDIDATE":
        if set(operation) != {"type", "candidate", "authorization"}:
            raise ValidationError(
                "PROMOTE_UNTESTED_CANDIDATE requires exactly type, candidate, and authorization"
            )
        if operation["authorization"] != UNTESTED_PROMOTION_AUTHORIZATION:
            raise ValidationError(
                "PROMOTE_UNTESTED_CANDIDATE requires explicit USER_APPROVED_UNTESTED_PROMOTION authorization"
            )
        declaration = _normalize_candidate_declaration(operation["candidate"])
        if declaration.get("dependency_overrides"):
            raise ValidationError("an untested direct promotion cannot include temporary dependency overrides")
        unit = copy.deepcopy(declaration["unit"])
        _unit(unit, "candidate.unit", project_index)
        replacement_id = declaration["replaces_accepted_deployment_id"]
        if replacement_id is None:
            raise ValidationError("an untested direct promotion must replace an existing accepted release")
        replacement_id = _uuid(replacement_id, "candidate.replaces_accepted_deployment_id")
        members = next_state["accepted_baseline"]["members"]
        indexes = [
            index
            for index, existing in enumerate(members)
            if existing["unit"]["deployment_id"] == replacement_id
        ]
        if len(indexes) != 1:
            raise ValidationError("untested direct promotion replacement target is not unique")
        accepted = members[indexes[0]]["unit"]
        if (
            unit["project_uuid"] != accepted["project_uuid"]
            or unit["project_id"] != accepted["project_id"]
        ):
            raise ValidationError("untested direct promotion must replace the same accepted project")
        for label in ("A", "B"):
            existing_slot = slots[label]
            if existing_slot is None or existing_slot["unit"]["project_uuid"] != unit["project_uuid"]:
                continue
            if existing_slot["runtime_result"]["classification"] != "UNTESTED":
                raise ValidationError("untested direct promotion cannot discard a recorded slot result")
            slots[label] = None
        byte_identical_reconciliation = _byte_composition(accepted) == _byte_composition(unit)
        if not byte_identical_reconciliation:
            members[indexes[0]] = {"unit": unit, "accepted_at": at}
            next_state["accepted_baseline"]["provenance"]["accepted_artifact_count"] = sum(
                len(existing["unit"]["artifacts"]) for existing in members
            )
            if next_state["activation"] == "ACTIVE":
                next_state["accepted_baseline"]["provenance"]["physical_disposition"] = "TRANSITIONED"
            next_state["accepted_baseline"]["revision"] += 1
    else:
        required_keys = {"type", "slot"}
        if operation_type == "RECORD_RESULT":
            required_keys.update({"classification", "evidence"})
        if set(operation) != required_keys:
            raise ValidationError(f"{operation_type} has invalid operation fields")
        label = operation["slot"]
        if label not in {"A", "B"}:
            raise ValidationError("operation.slot must be A or B")
        slot = slots[label]
        if slot is None:
            raise ValidationError(f"slot {label} is empty")
        if operation_type == "MARK_DEPLOYED":
            if slot["deployment"]["state"] != "NOT_DEPLOYED":
                raise ValidationError("MARK_DEPLOYED requires NOT_DEPLOYED")
            slot["deployment"] = {"state": "DEPLOYED", "deployed_at": at, "ready_verified_at": None}
        elif operation_type == "MARK_READY":
            if slot["deployment"]["state"] != "DEPLOYED":
                raise ValidationError("MARK_READY requires DEPLOYED")
            slot["deployment"]["state"] = "READY_TO_TEST_VERIFIED"
            slot["deployment"]["ready_verified_at"] = at
        elif operation_type == "RECORD_RESULT":
            classification = _enum(operation["classification"], "operation.classification", SLOT_RESULTS)
            if slot["deployment"]["state"] != "READY_TO_TEST_VERIFIED":
                raise ValidationError("RECORD_RESULT requires READY_TO_TEST_VERIFIED")
            slot["runtime_result"] = {
                "classification": classification,
                "recorded_at": None if classification == "UNTESTED" else at,
                "evidence": copy.deepcopy(operation["evidence"]),
            }
        elif operation_type == "REMOVE_SLOT":
            slots[label] = None
        elif operation_type == "PROMOTE_SLOT":
            if slot["deployment"]["state"] != "READY_TO_TEST_VERIFIED" or slot["runtime_result"]["classification"] != "PASS":
                raise ValidationError("PROMOTE_SLOT requires an explicit READY_TO_TEST_VERIFIED PASS")
            if slot.get("dependency_overrides"):
                raise ValidationError("PROMOTE_SLOT cannot promote temporary accepted dependency overrides")
            if _timestamp(at, "transition timestamp") < _timestamp(slot["runtime_result"]["recorded_at"], "runtime result timestamp"):
                raise ValidationError("promotion cannot precede the recorded PASS")
            replacement_id = slot["replaces_accepted_deployment_id"]
            member = {"unit": copy.deepcopy(slot["unit"]), "accepted_at": at}
            members = next_state["accepted_baseline"]["members"]
            byte_identical_reconciliation = False
            if replacement_id is None:
                members.append(member)
            else:
                indexes = [index for index, existing in enumerate(members) if existing["unit"]["deployment_id"] == replacement_id]
                if len(indexes) != 1:
                    raise ValidationError("accepted replacement target is not unique")
                accepted = members[indexes[0]]["unit"]
                byte_identical_reconciliation = _byte_composition(accepted) == _byte_composition(slot["unit"])
                if not byte_identical_reconciliation:
                    members[indexes[0]] = member
            if not byte_identical_reconciliation:
                next_state["accepted_baseline"]["provenance"]["accepted_artifact_count"] = sum(
                    len(existing["unit"]["artifacts"]) for existing in members
                )
                if next_state["activation"] == "ACTIVE":
                    next_state["accepted_baseline"]["provenance"]["physical_disposition"] = "TRANSITIONED"
                next_state["accepted_baseline"]["revision"] += 1
            slots[label] = None

    next_state["revision"] += 1
    next_state["updated_at"] = at
    validate_runtime_state(next_state, project_index)
    return next_state


def commit_state(
    path: Path,
    expected_revision: int,
    expected_digest: str,
    operation: dict[str, Any],
    at: str,
    project_index: dict[str, str],
) -> dict[str, Any]:
    """Refuse the obsolete repo-only mutation path.

    All physical/runtime transitions must use the target-local lock, ledger,
    verification, and rollback transaction in the V2 physical manager.
    """

    raise ValidationError("direct runtime-state commits are disabled; use the V2 physical manager")
