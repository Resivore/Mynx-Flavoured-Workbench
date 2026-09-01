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
    "PROMOTE_USER_PASSED_BATCH",
    "REMOVE_ACCEPTED",
}
UNTESTED_PROMOTION_AUTHORIZATION = "USER_APPROVED_UNTESTED_PROMOTION"
USER_PASSED_BATCH_AUTHORIZATION = "USER_REPORTED_EXACT_RUNTIME_PASS"
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
    if not isinstance(ownership_keys, list) or not ownership_keys:
        _fail(f"{path}.ownership_keys", "MOD artifacts require at least one mod:<fabric_id> key")
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
    required = {"unit", "accepted_at"}
    allowed = required | {"retained_rollbacks"}
    if not isinstance(value, dict):
        _fail(path, "must be an object")
    missing = required - set(value)
    unknown = set(value) - allowed
    if missing:
        _fail(path, f"missing required fields: {', '.join(sorted(missing))}")
    if unknown:
        _fail(path, f"unknown fields: {', '.join(sorted(unknown))}")
    member = value
    unit = _unit(member["unit"], f"{path}.unit", project_index)
    if member["accepted_at"] is not None:
        _timestamp(member["accepted_at"], f"{path}.accepted_at")
    retained_rollbacks = member.get("retained_rollbacks", [])
    if not isinstance(retained_rollbacks, list):
        _fail(f"{path}.retained_rollbacks", "must be an array")
    if len(retained_rollbacks) > 1:
        _fail(f"{path}.retained_rollbacks", "may contain at most one retained rollback")
    for index, retained in enumerate(retained_rollbacks):
        retained_path = f"{path}.retained_rollbacks[{index}]"
        retained = _object(retained, retained_path, {"unit", "retained_at"})
        rollback_unit = _unit(retained["unit"], f"{retained_path}.unit", project_index)
        _timestamp(retained["retained_at"], f"{retained_path}.retained_at")
        if rollback_unit["project_identity_source"] != "FROZEN_LEGACY":
            _fail(
                f"{retained_path}.unit.project_identity_source",
                "retained rollbacks require FROZEN_LEGACY identity",
            )
        if (
            rollback_unit["project_uuid"] != unit["project_uuid"]
            or rollback_unit["project_id"] != unit["project_id"]
        ):
            _fail(retained_path, "must retain the same accepted project UUID and project ID")
        for artifact_index, artifact in enumerate(rollback_unit["artifacts"]):
            if artifact["kind"] != "MOD" or artifact["source"]["type"] != "ADOPTED_TARGET":
                _fail(
                    f"{retained_path}.unit.artifacts[{artifact_index}]",
                    "retained rollbacks require MOD artifacts from ADOPTED_TARGET sources",
                )
    return member


def _deployment(value: Any, path: str) -> tuple[dict[str, Any], str, Any, Any]:
    deployment = _object(value, path, {"state", "deployed_at", "ready_verified_at"})
    deployment_state = _enum(deployment["state"], f"{path}.state", SLOT_DEPLOYMENT_STATES)
    deployed_at = None if deployment["deployed_at"] is None else _timestamp(deployment["deployed_at"], f"{path}.deployed_at")
    ready_at = None if deployment["ready_verified_at"] is None else _timestamp(deployment["ready_verified_at"], f"{path}.ready_verified_at")
    if deployment_state == "NOT_DEPLOYED" and (deployed_at is not None or ready_at is not None):
        _fail(path, "NOT_DEPLOYED requires null timestamps")
    if deployment_state == "DEPLOYED" and (deployed_at is None or ready_at is not None):
        _fail(path, "DEPLOYED requires deployed_at and no ready_verified_at")
    if deployment_state == "READY_TO_TEST_VERIFIED" and (deployed_at is None or ready_at is None):
        _fail(path, "READY_TO_TEST_VERIFIED requires both deployment timestamps")
    if deployed_at is not None and ready_at is not None and ready_at < deployed_at:
        _fail(path, "ready verification cannot precede deployment")
    return deployment, deployment_state, deployed_at, ready_at


def _runtime_result(
    value: Any,
    path: str,
    deployment_state: str,
    ready_at: Any,
    *,
    require_ready: bool,
) -> dict[str, Any]:
    runtime_result = _object(value, path, {"classification", "recorded_at", "evidence"})
    classification = _enum(runtime_result["classification"], f"{path}.classification", SLOT_RESULTS)
    recorded_at = None if runtime_result["recorded_at"] is None else _timestamp(runtime_result["recorded_at"], f"{path}.recorded_at")
    if classification == "UNTESTED" and recorded_at is not None:
        _fail(path, "UNTESTED requires recorded_at=null")
    if classification != "UNTESTED" and recorded_at is None:
        _fail(path, "a recorded result requires recorded_at")
    if (
        require_ready
        and classification != "UNTESTED"
        and deployment_state != "READY_TO_TEST_VERIFIED"
    ):
        _fail(path, "runtime results require READY_TO_TEST_VERIFIED deployment")
    if ready_at is not None and recorded_at is not None and recorded_at < ready_at:
        _fail(f"{path}.recorded_at", "cannot precede ready verification")
    evidence = _object(runtime_result["evidence"], f"{path}.evidence", {"passed", "failed"})
    evidence_sets: dict[str, set[str]] = {}
    for label in ("passed", "failed"):
        values = evidence[label]
        if not isinstance(values, list):
            _fail(f"{path}.evidence.{label}", "must be an array")
        normalized: set[str] = set()
        for index, item in enumerate(values):
            item = _nonblank(item, f"{path}.evidence.{label}[{index}]")
            key = item.casefold()
            if key in normalized:
                _fail(f"{path}.evidence.{label}", f"duplicate evidence: {item}")
            normalized.add(key)
        evidence_sets[label] = normalized
    overlap = evidence_sets["passed"].intersection(evidence_sets["failed"])
    if overlap:
        _fail(f"{path}.evidence", "the same check cannot be both passed and failed")
    if classification == "UNTESTED" and (evidence["passed"] or evidence["failed"]):
        _fail(f"{path}.evidence", "UNTESTED requires empty evidence")
    if classification == "PASS" and not evidence["passed"]:
        _fail(f"{path}.evidence.passed", "PASS requires at least one passed check")
    if classification == "PASS" and evidence["failed"]:
        _fail(f"{path}.evidence.failed", "PASS cannot retain failed evidence")
    if classification == "FAIL" and not evidence["failed"]:
        _fail(f"{path}.evidence.failed", "FAIL requires at least one failed check")
    if classification == "INCONCLUSIVE" and not (evidence["passed"] or evidence["failed"]):
        _fail(f"{path}.evidence", "INCONCLUSIVE requires observed evidence")
    return runtime_result


def _slot_member(
    value: Any,
    path: str,
    project_index: dict[str, str] | None,
    deployment_state: str,
    ready_at: Any,
    *,
    require_ready_for_recorded_result: bool,
) -> dict[str, Any]:
    required = {"unit", "replaces_accepted_deployment_id", "runtime_result"}
    allowed = required | {"dependency_overrides"}
    if not isinstance(value, dict):
        _fail(path, "must be an object")
    missing = required - set(value)
    unknown = set(value) - allowed
    if missing:
        _fail(path, f"missing required fields: {', '.join(sorted(missing))}")
    if unknown:
        _fail(path, f"unknown fields: {', '.join(sorted(unknown))}")
    member = value
    unit = _unit(member["unit"], f"{path}.unit", project_index)
    if unit["project_identity_source"] != "CURRENT_MANIFEST":
        _fail(f"{path}.unit.project_identity_source", "runtime slots require CURRENT_MANIFEST identity")
    if member["replaces_accepted_deployment_id"] is not None:
        _uuid(member["replaces_accepted_deployment_id"], f"{path}.replaces_accepted_deployment_id")
    dependency_overrides = member.get("dependency_overrides", [])
    if not isinstance(dependency_overrides, list):
        _fail(f"{path}.dependency_overrides", "must be an array")
    seen_overrides: set[str] = set()
    for index, deployment_id in enumerate(dependency_overrides):
        deployment_id = _uuid(deployment_id, f"{path}.dependency_overrides[{index}]")
        if deployment_id in seen_overrides:
            _fail(f"{path}.dependency_overrides", f"duplicate deployment UUID: {deployment_id}")
        seen_overrides.add(deployment_id)

    _runtime_result(
        member["runtime_result"],
        f"{path}.runtime_result",
        deployment_state,
        ready_at,
        require_ready=require_ready_for_recorded_result,
    )
    return member


def _slot(
    value: Any,
    path: str,
    project_index: dict[str, str] | None,
    schema_version: int,
) -> dict[str, Any]:
    if schema_version == 1:
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
        _, deployment_state, _, ready_at = _deployment(value["deployment"], f"{path}.deployment")
        legacy_member = {
            "unit": value["unit"],
            "replaces_accepted_deployment_id": value["replaces_accepted_deployment_id"],
            "runtime_result": value["runtime_result"],
        }
        if "dependency_overrides" in value:
            legacy_member["dependency_overrides"] = value["dependency_overrides"]
        _slot_member(
            legacy_member,
            path,
            project_index,
            deployment_state,
            ready_at,
            require_ready_for_recorded_result=True,
        )
        return value

    slot = _object(value, path, {"members", "deployment"})
    _, deployment_state, _, ready_at = _deployment(slot["deployment"], f"{path}.deployment")
    members = slot["members"]
    if not isinstance(members, list) or not members:
        _fail(f"{path}.members", "must be a nonempty array")
    for index, member in enumerate(members):
        _slot_member(
            member,
            f"{path}.members[{index}]",
            project_index,
            deployment_state,
            ready_at,
            require_ready_for_recorded_result=False,
        )
    return slot


def _slot_members_unchecked(slot: dict[str, Any]) -> list[dict[str, Any]]:
    if "members" in slot:
        return slot["members"]
    member = {
        "unit": slot["unit"],
        "replaces_accepted_deployment_id": slot["replaces_accepted_deployment_id"],
        "runtime_result": slot["runtime_result"],
    }
    if "dependency_overrides" in slot:
        member["dependency_overrides"] = slot["dependency_overrides"]
    return [member]


def _all_units(state: dict[str, Any]) -> list[tuple[str, dict[str, Any]]]:
    units: list[tuple[str, dict[str, Any]]] = []
    for index, member in enumerate(state["accepted_baseline"]["members"]):
        units.append((f"accepted[{index}]", member["unit"]))
        for rollback_index, retained in enumerate(member.get("retained_rollbacks", [])):
            units.append(
                (
                    f"accepted[{index}].retained_rollbacks[{rollback_index}]",
                    retained["unit"],
                )
            )
    for label in ("A", "B"):
        slot = state["slots"][label]
        if slot is None:
            continue
        for index, member in enumerate(_slot_members_unchecked(slot)):
            units.append((f"slot {label}.members[{index}]", member["unit"]))
    return units


def _resolved_units_unchecked(state: dict[str, Any]) -> list[dict[str, Any]]:
    suppressed: set[str] = set()
    for slot in state["slots"].values():
        if slot is None:
            continue
        for member in _slot_members_unchecked(slot):
            if member["replaces_accepted_deployment_id"] is not None:
                suppressed.add(member["replaces_accepted_deployment_id"])
            suppressed.update(member.get("dependency_overrides", []))
    result = [
        member["unit"]
        for member in state["accepted_baseline"]["members"]
        if member["unit"]["deployment_id"] not in suppressed
    ]
    for label in ("A", "B"):
        slot = state["slots"][label]
        if slot is not None:
            result.extend(member["unit"] for member in _slot_members_unchecked(slot))
    return result


def migrate_runtime_state(
    state: dict[str, Any],
    project_index: dict[str, str] | None = None,
) -> dict[str, Any]:
    """Losslessly normalize a validated legacy v1 state to cohort-capable v2.

    Migration is deliberately revision-neutral: the physical manager incorporates
    this deterministic representation change into the same CAS revision as the
    requested filesystem transition.
    """

    validate_runtime_state(state, project_index)
    migrated = copy.deepcopy(state)
    if migrated["schema_version"] == 2:
        return migrated
    migrated["schema_version"] = 2
    for label in ("A", "B"):
        legacy = migrated["slots"][label]
        if legacy is None:
            continue
        member = {
            "unit": legacy["unit"],
            "replaces_accepted_deployment_id": legacy["replaces_accepted_deployment_id"],
            "runtime_result": legacy["runtime_result"],
        }
        if "dependency_overrides" in legacy:
            member["dependency_overrides"] = legacy["dependency_overrides"]
        migrated["slots"][label] = {
            "members": [member],
            "deployment": legacy["deployment"],
        }
    validate_runtime_state(migrated, project_index)
    return migrated


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
    schema_version = _integer(state["schema_version"], "$.schema_version", 1)
    if schema_version not in {1, 2}:
        _fail("$.schema_version", "must equal 1 or 2")
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
        for rollback_index, retained in enumerate(member.get("retained_rollbacks", [])):
            if _timestamp(
                retained["retained_at"],
                f"$.accepted_baseline.members[{index}].retained_rollbacks[{rollback_index}].retained_at",
            ) > updated_at:
                _fail(
                    f"$.accepted_baseline.members[{index}].retained_rollbacks[{rollback_index}].retained_at",
                    "cannot be later than state updated_at",
                )
    if actual_artifact_count != accepted_artifact_count:
        _fail(
            "$.accepted_baseline.provenance.accepted_artifact_count",
            f"declares {accepted_artifact_count}, but members contain {actual_artifact_count} artifacts",
        )

    slots = _object(state["slots"], "$.slots", {"A", "B"})
    for label in ("A", "B"):
        if slots[label] is not None:
            _slot(slots[label], f"$.slots.{label}", project_index, schema_version)
            slot = slots[label]
            timestamp_fields = [
                slot["deployment"]["deployed_at"],
                slot["deployment"]["ready_verified_at"],
            ]
            timestamp_fields.extend(
                member["runtime_result"]["recorded_at"]
                for member in _slot_members_unchecked(slot)
            )
            for timestamp_field in timestamp_fields:
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
        for member_index, slot_member in enumerate(_slot_members_unchecked(slot)):
            member_path = f"$.slots.{label}.members[{member_index}]"
            unit = slot_member["unit"]
            if unit["project_uuid"] in slot_projects:
                _fail("$.slots", f"project {unit['project_uuid']} occupies more than one slot/cohort position")
            slot_projects.add(unit["project_uuid"])
            replacement_id = slot_member["replaces_accepted_deployment_id"]
            accepted_for_project = accepted_projects.get(unit["project_uuid"])
            if accepted_for_project is None and replacement_id is not None:
                _fail(f"{member_path}.replaces_accepted_deployment_id", "new project cannot replace an accepted deployment")
            if accepted_for_project is not None:
                expected_deployment_id, expected_project_id = accepted_for_project
                if replacement_id != expected_deployment_id or unit["project_id"] != expected_project_id:
                    _fail(f"{member_path}.replaces_accepted_deployment_id", "accepted project upgrade must reference its exact accepted deployment")
            if replacement_id is not None:
                if replacement_id not in accepted_by_deployment:
                    _fail(f"{member_path}.replaces_accepted_deployment_id", "does not name an accepted deployment")
                if replacement_id in replacement_ids:
                    _fail("$.slots", "two cohort members cannot replace the same accepted deployment")
                replacement_ids.add(replacement_id)
            slot_ownership = {
                ownership_key.casefold()
                for artifact in unit["artifacts"]
                for ownership_key in artifact["ownership_keys"]
            }
            for index, dependency_id in enumerate(slot_member.get("dependency_overrides", [])):
                dependency_path = f"{member_path}.dependency_overrides[{index}]"
                dependency_unit = accepted_by_deployment.get(dependency_id)
                if dependency_unit is None:
                    _fail(dependency_path, "does not name an accepted deployment")
                if dependency_unit["project_uuid"] == unit["project_uuid"]:
                    _fail(dependency_path, "the cohort member's accepted release must use replaces_accepted_deployment_id")
                if dependency_id in replacement_ids:
                    _fail("$.slots", "two cohort replacement paths cannot suppress the same accepted deployment")
                dependency_ownership = {
                    ownership_key.casefold()
                    for artifact in dependency_unit["artifacts"]
                    for ownership_key in artifact["ownership_keys"]
                }
                if not dependency_ownership.issubset(slot_ownership):
                    _fail(dependency_path, "cohort member artifacts must replace every ownership key of the accepted dependency")
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

        rendered_members: list[dict[str, Any]] = []
        member_labels: list[str] = []
        for index, member in enumerate(_slot_members_unchecked(slot)):
            unit = member["unit"]
            project_uuid = unit["project_uuid"]
            display_name = project_display_names.get(project_uuid)
            if display_name is None:
                raise ValidationError(
                    f"cannot render slot {label} member {index}: project {project_uuid} "
                    "has no canonical display name"
                )
            display_name = _nonblank(display_name, f"project_display_names[{project_uuid!r}]")
            if "\r" in display_name or "\n" in display_name:
                raise ValidationError(
                    f"cannot render slot {label}: canonical display name must occupy one line"
                )
            match = CANARY_VERSION_RE.search(unit["version"])
            if match is None:
                raise ValidationError(
                    f"cannot render slot {label}: canonical version {unit['version']!r} has no Canary number"
                )
            canary = int(match.group(1))
            member_labels.append(f"{display_name} - Canary {canary}")
            rendered_members.append(
                {
                    "project_uuid": project_uuid,
                    "project_display_name": display_name,
                    "version": unit["version"],
                    "canary": canary,
                }
            )
        line = f"Slot {label}: " + " + ".join(member_labels)
        slots[label] = {
            "occupied": True,
            "members": rendered_members,
            "line": line,
        }
        if len(rendered_members) == 1:
            slots[label].update(rendered_members[0])
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
    if dependency_overrides is not None:
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
    """Materialize the legacy v1 single-member slot representation."""

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


def _candidate_identity(declaration: dict[str, Any]) -> dict[str, Any]:
    return {
        "unit": declaration["unit"],
        "replaces_accepted_deployment_id": declaration["replaces_accepted_deployment_id"],
        "dependency_overrides": declaration.get("dependency_overrides", []),
    }


def _member_identity(member: dict[str, Any]) -> dict[str, Any]:
    return {
        "unit": member["unit"],
        "replaces_accepted_deployment_id": member["replaces_accepted_deployment_id"],
        "dependency_overrides": member.get("dependency_overrides", []),
    }


def _normalize_member_declarations(declarations: Any, path: str = "members") -> list[dict[str, Any]]:
    if not isinstance(declarations, list) or not declarations:
        _fail(path, "must be a nonempty array")
    normalized: list[dict[str, Any]] = []
    for index, declaration in enumerate(declarations):
        try:
            normalized.append(_normalize_candidate_declaration(declaration))
        except ValidationError as exc:
            raise ValidationError(f"{path}[{index}]: {exc}") from exc
    return normalized


def _new_cohort_member(declaration: dict[str, Any]) -> dict[str, Any]:
    """Materialize one untested member without borrowing companion evidence."""

    member = {
        "unit": copy.deepcopy(declaration["unit"]),
        "replaces_accepted_deployment_id": declaration["replaces_accepted_deployment_id"],
        "runtime_result": {
            "classification": "UNTESTED",
            "recorded_at": None,
            "evidence": {"passed": [], "failed": []},
        },
    }
    if declaration.get("dependency_overrides"):
        member["dependency_overrides"] = copy.deepcopy(declaration["dependency_overrides"])
    return member


def _materialize_cohort(declarations: Any, existing_slots: dict[str, Any]) -> dict[str, Any]:
    declarations = _normalize_member_declarations(declarations)
    identities = [_candidate_identity(declaration) for declaration in declarations]
    for label in ("A", "B"):
        existing = existing_slots[label]
        if existing is None:
            continue
        existing_members = _slot_members_unchecked(existing)
        if [_member_identity(member) for member in existing_members] == identities:
            if "members" in existing:
                return copy.deepcopy(existing)
            migrated_member = {
                "unit": copy.deepcopy(existing["unit"]),
                "replaces_accepted_deployment_id": existing["replaces_accepted_deployment_id"],
                "runtime_result": copy.deepcopy(existing["runtime_result"]),
            }
            if "dependency_overrides" in existing:
                migrated_member["dependency_overrides"] = copy.deepcopy(existing["dependency_overrides"])
            return {
                "members": [migrated_member],
                "deployment": copy.deepcopy(existing["deployment"]),
            }
    members = [_new_cohort_member(declaration) for declaration in declarations]
    return {
        "members": members,
        "deployment": {"state": "NOT_DEPLOYED", "deployed_at": None, "ready_verified_at": None},
    }


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
    schema_version = next_state["schema_version"]

    if operation_type == "ASSIGN_SLOT":
        if set(operation) not in ({"type", "candidate"}, {"type", "members"}):
            raise ValidationError("ASSIGN_SLOT requires exactly one of candidate or members")
        target = "A" if slots["A"] is None else "B" if slots["B"] is None else None
        if target is None:
            raise ValidationError("both runtime test slots are occupied")
        if "members" in operation:
            if schema_version != 2:
                raise ValidationError("ASSIGN_SLOT members require migrated runtime-state schema_version 2")
            slots[target] = _materialize_cohort(operation["members"], state["slots"])
        elif schema_version == 2:
            slots[target] = _materialize_cohort([operation["candidate"]], state["slots"])
        else:
            slots[target] = _materialize_candidate(operation["candidate"], state["slots"])
    elif operation_type == "UPDATE_SLOT":
        if set(operation) not in (
            {"type", "slot", "candidate"},
            {"type", "slot", "members"},
        ):
            raise ValidationError("UPDATE_SLOT requires slot and exactly one of candidate or members")
        label = operation["slot"]
        if label not in {"A", "B"}:
            raise ValidationError("operation.slot must be A or B")
        if slots[label] is None:
            raise ValidationError(f"slot {label} is empty")
        prior = slots[label]
        if "members" in operation:
            if schema_version != 2:
                raise ValidationError("UPDATE_SLOT members require migrated runtime-state schema_version 2")
            declarations = _normalize_member_declarations(operation["members"])
            slots[label] = _materialize_cohort(declarations, state["slots"])
        else:
            declaration = _normalize_candidate_declaration(operation["candidate"])
            prior_members = _slot_members_unchecked(prior)
            project_uuid = declaration["unit"].get("project_uuid") if isinstance(declaration["unit"], dict) else None
            matches = [
                (index, member)
                for index, member in enumerate(prior_members)
                if member["unit"]["project_uuid"] == project_uuid
            ]
            if len(prior_members) > 1 and len(matches) != 1:
                raise ValidationError(
                    "UPDATE_SLOT candidate must identify exactly one existing cohort member; "
                    "use members to replace the whole cohort"
                )
            if matches:
                _, prior_member = matches[0]
                if declaration["replaces_accepted_deployment_id"] is None:
                    declaration["replaces_accepted_deployment_id"] = prior_member["replaces_accepted_deployment_id"]
                if "dependency_overrides" not in declaration and prior_member.get("dependency_overrides"):
                    declaration["dependency_overrides"] = copy.deepcopy(prior_member["dependency_overrides"])
            if schema_version == 1:
                slots[label] = _materialize_candidate(declaration, state["slots"])
            elif len(prior_members) == 1 and not matches:
                # Preserve legacy one-member UPDATE_SLOT behavior: a candidate
                # for another project replaces the entire one-member slot.
                slots[label] = _materialize_cohort([declaration], state["slots"])
            else:
                matching_index, prior_member = matches[0]
                if _candidate_identity(declaration) == _member_identity(prior_member):
                    slots[label] = copy.deepcopy(prior)
                else:
                    members = copy.deepcopy(prior_members)
                    members[matching_index] = _new_cohort_member(declaration)
                    slots[label] = {
                        "members": members,
                        "deployment": {
                            "state": "NOT_DEPLOYED",
                            "deployed_at": None,
                            "ready_verified_at": None,
                        },
                    }
    elif operation_type == "SET_PROFILE":
        if set(operation) == {"type", "candidates"} and isinstance(operation["candidates"], list):
            if len(operation["candidates"]) > 2:
                raise ValidationError("runtime profile cannot contain more than two candidates")
            for index, label in enumerate(("A", "B")):
                if index >= len(operation["candidates"]):
                    slots[label] = None
                elif schema_version == 2:
                    slots[label] = _materialize_cohort([operation["candidates"][index]], state["slots"])
                else:
                    slots[label] = _materialize_candidate(operation["candidates"][index], state["slots"])
        elif set(operation) == {"type", "slots"}:
            if schema_version != 2:
                raise ValidationError("SET_PROFILE slots require migrated runtime-state schema_version 2")
            desired_slots = _object(operation["slots"], "operation.slots", {"A", "B"})
            for label in ("A", "B"):
                desired = desired_slots[label]
                if desired is None:
                    slots[label] = None
                    continue
                desired = _object(desired, f"operation.slots.{label}", set(desired))
                if set(desired) == {"candidate"}:
                    declarations = [desired["candidate"]]
                elif set(desired) == {"members"}:
                    declarations = desired["members"]
                else:
                    raise ValidationError(
                        f"operation.slots.{label} requires exactly one of candidate or members"
                    )
                slots[label] = _materialize_cohort(declarations, state["slots"])
        else:
            raise ValidationError("SET_PROFILE requires exactly candidates or slots")
    elif operation_type == "PROMOTE_USER_PASSED_BATCH":
        if set(operation) != {"type", "authorization", "members"}:
            raise ValidationError(
                "PROMOTE_USER_PASSED_BATCH requires exactly type, authorization, and members"
            )
        if operation["authorization"] != USER_PASSED_BATCH_AUTHORIZATION:
            raise ValidationError(
                "PROMOTE_USER_PASSED_BATCH requires exact "
                "USER_REPORTED_EXACT_RUNTIME_PASS authorization"
            )
        declarations = operation["members"]
        if not isinstance(declarations, list) or not declarations:
            raise ValidationError("PROMOTE_USER_PASSED_BATCH members must be a nonempty array")

        accepted_members = next_state["accepted_baseline"]["members"]
        accepted_by_uuid = {
            member["unit"]["project_uuid"]: (index, member["unit"])
            for index, member in enumerate(accepted_members)
        }
        accepted_by_project_id = {
            member["unit"]["project_id"].casefold(): (index, member["unit"])
            for index, member in enumerate(accepted_members)
        }
        slot_units = [
            member["unit"]
            for label in ("A", "B")
            if state["slots"][label] is not None
            for member in _slot_members_unchecked(state["slots"][label])
        ]
        occupied_project_uuids = {unit["project_uuid"] for unit in slot_units}
        occupied_project_ids = {unit["project_id"].casefold() for unit in slot_units}
        batch_project_uuids: set[str] = set()
        batch_project_ids: set[str] = set()
        promoted_members: list[tuple[int | None, dict[str, Any]]] = []
        stack_revision_delta = 0

        for index, declaration in enumerate(declarations):
            member_path = f"operation.members[{index}]"
            required = {"unit"}
            allowed = required | {"replaces_accepted_deployment_id", "retained_rollbacks"}
            if not isinstance(declaration, dict):
                raise ValidationError(f"{member_path} must be an object")
            missing = required - set(declaration)
            unknown = set(declaration) - allowed
            if missing:
                raise ValidationError(
                    f"{member_path} missing required fields: {', '.join(sorted(missing))}"
                )
            if unknown:
                raise ValidationError(
                    f"{member_path} unknown fields: {', '.join(sorted(unknown))}"
                )

            unit = copy.deepcopy(declaration["unit"])
            _unit(unit, f"{member_path}.unit", project_index)
            if unit["project_identity_source"] != "CURRENT_MANIFEST":
                raise ValidationError(
                    f"{member_path}.unit requires CURRENT_MANIFEST identity"
                )
            project_uuid = unit["project_uuid"]
            project_id_key = unit["project_id"].casefold()
            if project_uuid in occupied_project_uuids or project_id_key in occupied_project_ids:
                raise ValidationError(
                    f"{member_path}.unit project must be absent from both managed Test Slots"
                )
            if project_uuid in batch_project_uuids or project_id_key in batch_project_ids:
                raise ValidationError(
                    f"{member_path}.unit duplicates a project in this promotion batch"
                )
            batch_project_uuids.add(project_uuid)
            batch_project_ids.add(project_id_key)

            accepted_match = accepted_by_uuid.get(project_uuid)
            accepted_id_match = accepted_by_project_id.get(project_id_key)
            if (accepted_match is None) != (accepted_id_match is None) or (
                accepted_match is not None and accepted_id_match is not None
                and accepted_match[0] != accepted_id_match[0]
            ):
                raise ValidationError(
                    f"{member_path}.unit conflicts with an accepted project identity"
                )
            replacement_id = declaration.get("replaces_accepted_deployment_id")
            if replacement_id is not None:
                replacement_id = _uuid(
                    replacement_id,
                    f"{member_path}.replaces_accepted_deployment_id",
                )
            if accepted_match is None:
                if replacement_id is not None:
                    raise ValidationError(
                        f"{member_path}.replaces_accepted_deployment_id must be null or omitted "
                        "for a project absent from the accepted baseline"
                    )
                accepted_index = None
                accepted_unit = None
            else:
                accepted_index, accepted_unit = accepted_match
                if replacement_id != accepted_unit["deployment_id"]:
                    raise ValidationError(
                        f"{member_path}.replaces_accepted_deployment_id must name the exact "
                        "accepted predecessor deployment"
                    )
                if unit["deployment_id"] == accepted_unit["deployment_id"]:
                    raise ValidationError(
                        f"{member_path}.unit.deployment_id must be distinct from the accepted predecessor"
                    )
                predecessor_artifact_ids = {
                    artifact["artifact_id"] for artifact in accepted_unit["artifacts"]
                }
                successor_artifact_ids = {
                    artifact["artifact_id"] for artifact in unit["artifacts"]
                }
                if predecessor_artifact_ids.intersection(successor_artifact_ids):
                    raise ValidationError(
                        f"{member_path}.unit.artifacts must use artifact UUIDs distinct from the accepted predecessor"
                    )

            retained_units = declaration.get("retained_rollbacks", [])
            if not isinstance(retained_units, list):
                raise ValidationError(f"{member_path}.retained_rollbacks must be an array")
            if len(retained_units) > 1:
                raise ValidationError(
                    f"{member_path}.retained_rollbacks may contain at most one retained rollback"
                )
            retained_rollbacks: list[dict[str, Any]] = []
            for rollback_index, rollback_value in enumerate(retained_units):
                rollback_path = f"{member_path}.retained_rollbacks[{rollback_index}]"
                rollback_unit = copy.deepcopy(rollback_value)
                _unit(rollback_unit, rollback_path, project_index)
                if rollback_unit["project_identity_source"] != "FROZEN_LEGACY":
                    raise ValidationError(
                        f"{rollback_path}.project_identity_source requires FROZEN_LEGACY identity"
                    )
                if (
                    rollback_unit["project_uuid"] != project_uuid
                    or rollback_unit["project_id"] != unit["project_id"]
                ):
                    raise ValidationError(
                        f"{rollback_path} must have the promoted project UUID and project ID"
                    )
                for artifact_index, artifact in enumerate(rollback_unit["artifacts"]):
                    if artifact["kind"] != "MOD" or artifact["source"]["type"] != "ADOPTED_TARGET":
                        raise ValidationError(
                            f"{rollback_path}.artifacts[{artifact_index}] requires a MOD artifact "
                            "from an ADOPTED_TARGET source"
                        )
                if rollback_unit["deployment_id"] == unit["deployment_id"]:
                    raise ValidationError(
                        f"{rollback_path}.deployment_id must be distinct from the promoted deployment UUID"
                    )
                promoted_artifact_ids = {
                    artifact["artifact_id"] for artifact in unit["artifacts"]
                }
                rollback_artifact_ids = {
                    artifact["artifact_id"] for artifact in rollback_unit["artifacts"]
                }
                if promoted_artifact_ids.intersection(rollback_artifact_ids):
                    raise ValidationError(
                        f"{rollback_path}.artifacts must use artifact UUIDs distinct from the promoted unit"
                    )
                retained_rollbacks.append({"unit": rollback_unit, "retained_at": at})

            member = {"unit": unit, "accepted_at": at}
            if retained_rollbacks:
                member["retained_rollbacks"] = retained_rollbacks
            if accepted_unit is None:
                promoted_members.append((None, member))
                stack_revision_delta += 1
            else:
                promoted_members.append((accepted_index, member))
                if _byte_composition(accepted_unit) != _byte_composition(unit):
                    stack_revision_delta += 1

        for accepted_index, member in promoted_members:
            if accepted_index is None:
                accepted_members.append(member)
            else:
                accepted_members[accepted_index] = member
        next_state["accepted_baseline"]["revision"] += stack_revision_delta
        next_state["accepted_baseline"]["provenance"]["accepted_artifact_count"] = sum(
            len(existing["unit"]["artifacts"]) for existing in accepted_members
        )
        if promoted_members and next_state["activation"] == "ACTIVE":
            next_state["accepted_baseline"]["provenance"]["physical_disposition"] = "TRANSITIONED"
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
        if unit["project_identity_source"] != "CURRENT_MANIFEST":
            raise ValidationError("an untested direct promotion requires CURRENT_MANIFEST identity")
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
            if existing_slot is None:
                continue
            matching_indexes = [
                index
                for index, slot_member in enumerate(_slot_members_unchecked(existing_slot))
                if slot_member["unit"]["project_uuid"] == unit["project_uuid"]
            ]
            if not matching_indexes:
                continue
            if len(matching_indexes) != 1:
                raise ValidationError("untested direct promotion found duplicate cohort project members")
            slot_member = _slot_members_unchecked(existing_slot)[matching_indexes[0]]
            if slot_member["runtime_result"]["classification"] != "UNTESTED":
                raise ValidationError(
                    "untested direct promotion cannot discard a recorded slot result for a cohort member"
                )
            if "members" not in existing_slot:
                slots[label] = None
            else:
                del existing_slot["members"][matching_indexes[0]]
                if not existing_slot["members"]:
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
            allowed_keys = required_keys | {"project_uuid"}
        else:
            allowed_keys = required_keys
        if set(operation) not in (required_keys, allowed_keys):
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
            slot_members = _slot_members_unchecked(slot)
            if "project_uuid" in operation:
                project_uuid = _uuid(operation["project_uuid"], "operation.project_uuid")
                matches = [
                    member
                    for member in slot_members
                    if member["unit"]["project_uuid"] == project_uuid
                ]
                if len(matches) != 1:
                    raise ValidationError("RECORD_RESULT project_uuid must name exactly one slot member")
                result_member = matches[0]
            elif len(slot_members) == 1:
                result_member = slot_members[0]
            else:
                raise ValidationError("RECORD_RESULT requires project_uuid for a multi-member cohort")
            recorded_result = {
                "classification": classification,
                "recorded_at": None if classification == "UNTESTED" else at,
                "evidence": copy.deepcopy(operation["evidence"]),
            }
            if "members" in slot:
                result_member["runtime_result"] = recorded_result
            else:
                slot["runtime_result"] = recorded_result
        elif operation_type == "REMOVE_SLOT":
            slots[label] = None
        elif operation_type == "PROMOTE_SLOT":
            slot_members = _slot_members_unchecked(slot)
            if slot["deployment"]["state"] != "READY_TO_TEST_VERIFIED" or any(
                member["runtime_result"]["classification"] != "PASS"
                for member in slot_members
            ):
                raise ValidationError(
                    "PROMOTE_SLOT requires an explicit READY_TO_TEST_VERIFIED PASS for every cohort member"
                )
            if any(member.get("dependency_overrides") for member in slot_members):
                raise ValidationError("PROMOTE_SLOT cannot promote temporary accepted dependency overrides")
            for slot_member in slot_members:
                if _timestamp(at, "transition timestamp") < _timestamp(
                    slot_member["runtime_result"]["recorded_at"],
                    "runtime result timestamp",
                ):
                    raise ValidationError("promotion cannot precede a recorded member PASS")
            members = next_state["accepted_baseline"]["members"]
            stack_revision_delta = 0
            for slot_member in slot_members:
                replacement_id = slot_member["replaces_accepted_deployment_id"]
                accepted_member = {"unit": copy.deepcopy(slot_member["unit"]), "accepted_at": at}
                byte_identical_reconciliation = False
                if replacement_id is None:
                    members.append(accepted_member)
                else:
                    indexes = [
                        index
                        for index, existing in enumerate(members)
                        if existing["unit"]["deployment_id"] == replacement_id
                    ]
                    if len(indexes) != 1:
                        raise ValidationError("accepted replacement target is not unique")
                    accepted = members[indexes[0]]["unit"]
                    byte_identical_reconciliation = (
                        _byte_composition(accepted) == _byte_composition(slot_member["unit"])
                    )
                    if not byte_identical_reconciliation:
                        members[indexes[0]] = accepted_member
                if not byte_identical_reconciliation:
                    stack_revision_delta += 1
            if stack_revision_delta:
                next_state["accepted_baseline"]["provenance"]["accepted_artifact_count"] = sum(
                    len(existing["unit"]["artifacts"]) for existing in members
                )
                if next_state["activation"] == "ACTIVE":
                    next_state["accepted_baseline"]["provenance"]["physical_disposition"] = "TRANSITIONED"
                next_state["accepted_baseline"]["revision"] += stack_revision_delta
            slots[label] = None

    next_state["revision"] += 1
    next_state["updated_at"] = at
    validate_runtime_state(next_state, project_index)
    return next_state


_PHYSICAL_MANAGER_AUTHORITY = object()


def finalize_verified_profile_transition(
    planned_state: dict[str, Any],
    deployed_at: str,
    ready_verified_at: str,
    project_index: dict[str, str] | None = None,
    *,
    changed_slots: list[str] | tuple[str, ...] | set[str] | frozenset[str],
    authority: object,
) -> dict[str, Any]:
    """Attach physical READY evidence to an already planned revision in memory.

    The physical manager is the only caller allowed to hold the in-process
    authority sentinel. It must also identify the exact slots changed by the
    physical transaction; verification never rewrites an untouched slot.
    Serialized operations cannot inject deployment evidence, and direct
    repository commits remain disabled.
    """

    if authority is not _PHYSICAL_MANAGER_AUTHORITY:
        raise ValidationError("READY evidence may only be finalized by the physical manager")
    if (
        isinstance(changed_slots, (str, bytes))
        or not isinstance(changed_slots, (list, tuple, set, frozenset))
        or not changed_slots
    ):
        raise ValidationError("verified profile finalization requires explicit changed slot labels")
    labels: list[str] = []
    for label in changed_slots:
        if label not in {"A", "B"}:
            raise ValidationError("changed slot labels must be A or B")
        if label in labels:
            raise ValidationError(f"duplicate changed slot label: {label}")
        labels.append(label)
    validate_runtime_state(planned_state, project_index)
    if planned_state["schema_version"] != 2:
        raise ValidationError("verified profile finalization requires runtime-state schema_version 2")
    deployed_time = _timestamp(deployed_at, "physical deployment timestamp")
    ready_time = _timestamp(ready_verified_at, "physical ready-verification timestamp")
    if ready_time < deployed_time:
        raise ValidationError("physical ready verification cannot precede deployment")
    if ready_time > _timestamp(planned_state["updated_at"], "$.updated_at"):
        raise ValidationError("planned state updated_at must include physical ready verification")

    finalized = copy.deepcopy(planned_state)
    changed = False
    for label in labels:
        slot = finalized["slots"][label]
        if slot is None or slot["deployment"]["state"] == "READY_TO_TEST_VERIFIED":
            continue
        if slot["deployment"]["state"] not in {"NOT_DEPLOYED", "DEPLOYED"}:
            raise ValidationError(f"slot {label} has unsupported pre-verification deployment state")
        slot["deployment"] = {
            "state": "READY_TO_TEST_VERIFIED",
            "deployed_at": deployed_at,
            "ready_verified_at": ready_verified_at,
        }
        changed = True
    if not changed:
        raise ValidationError("verified profile finalization found no planned slot awaiting verification")
    validate_runtime_state(finalized, project_index)
    return finalized


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
