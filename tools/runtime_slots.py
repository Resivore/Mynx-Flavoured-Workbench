#!/usr/bin/env python3
"""Pure two-slot runtime-state validation and atomic transition planning."""

from __future__ import annotations

import copy
import hashlib
import json
import os
import re
import secrets
import tempfile
from pathlib import Path, PurePosixPath
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
        load_json,
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
        load_json,
    )


RUNTIME_SCHEMA_REF = "../../schemas/runtime-state.schema.json"
ACTIVATION_STATES = {"GATED", "ACTIVE"}
ARTIFACT_KINDS = {"MOD", "RESOURCE_PACK"}
SLOT_DEPLOYMENT_STATES = {"NOT_DEPLOYED", "DEPLOYED", "READY_TO_TEST_VERIFIED"}
SLOT_RESULTS = {"UNTESTED", "PASS", "FAIL", "INCONCLUSIVE"}
TRANSITIONS = {"ASSIGN_SLOT", "SET_PROFILE", "MARK_DEPLOYED", "MARK_READY", "RECORD_RESULT", "REMOVE_SLOT", "PROMOTE_SLOT"}
OWNERSHIP_KEY_RE = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
WINDOWS_RESERVED_RE = re.compile(r"^(?:con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\.|$)", re.IGNORECASE)


def _artifact(value: Any, path: str) -> dict[str, Any]:
    artifact = _object(value, path, {"artifact_id", "kind", "filename", "sha256", "ownership_keys"})
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
        _fail(f"{path}.ownership_keys", "must be a nonempty array")
    seen: set[str] = set()
    for index, ownership_key in enumerate(ownership_keys):
        ownership_key = _nonblank(ownership_key, f"{path}.ownership_keys[{index}]")
        if not OWNERSHIP_KEY_RE.fullmatch(ownership_key):
            _fail(f"{path}.ownership_keys[{index}]", "must be a lowercase namespaced ownership key")
        normalized = ownership_key.casefold()
        if normalized in seen:
            _fail(f"{path}.ownership_keys", f"duplicate ownership key: {ownership_key}")
        seen.add(normalized)
    return artifact


def _unit(value: Any, path: str, project_index: dict[str, str] | None) -> dict[str, Any]:
    unit = _object(value, path, {"deployment_id", "project_uuid", "project_id", "version", "source_commit", "artifacts"})
    _uuid(unit["deployment_id"], f"{path}.deployment_id")
    project_uuid = _uuid(unit["project_uuid"], f"{path}.project_uuid")
    project_id = _project_id(unit["project_id"], f"{path}.project_id")
    _nonblank(unit["version"], f"{path}.version")
    _commit(unit["source_commit"], f"{path}.source_commit")
    artifacts = unit["artifacts"]
    if not isinstance(artifacts, list) or not artifacts:
        _fail(f"{path}.artifacts", "must be a nonempty array")
    for index, artifact in enumerate(artifacts):
        _artifact(artifact, f"{path}.artifacts[{index}]")
    if project_index is not None:
        expected_id = project_index.get(project_uuid)
        if expected_id is None:
            _fail(f"{path}.project_uuid", "does not resolve to a repository project manifest")
        if expected_id != project_id:
            _fail(f"{path}.project_id", f"does not match manifest project_id {expected_id}")
    return unit


def _accepted_member(value: Any, path: str, project_index: dict[str, str] | None) -> dict[str, Any]:
    member = _object(value, path, {"unit", "accepted_at"})
    _unit(member["unit"], f"{path}.unit", project_index)
    _timestamp(member["accepted_at"], f"{path}.accepted_at")
    return member


def _slot(value: Any, path: str, project_index: dict[str, str] | None) -> dict[str, Any]:
    slot = _object(value, path, {"unit", "replaces_accepted_deployment_id", "deployment", "runtime_result"})
    _unit(slot["unit"], f"{path}.unit", project_index)
    if slot["replaces_accepted_deployment_id"] is not None:
        _uuid(slot["replaces_accepted_deployment_id"], f"{path}.replaces_accepted_deployment_id")

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

    runtime_result = _object(slot["runtime_result"], f"{path}.runtime_result", {"classification", "recorded_at"})
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
    suppressed = {
        slot["replaces_accepted_deployment_id"]
        for slot in state["slots"].values()
        if slot is not None and slot["replaces_accepted_deployment_id"] is not None
    }
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
    _enum(state["activation"], "$.activation", ACTIVATION_STATES)
    state_revision = _integer(state["revision"], "$.revision", 0)
    updated_at = _timestamp(state["updated_at"], "$.updated_at")

    baseline = _object(state["accepted_baseline"], "$.accepted_baseline", {"revision", "members"})
    baseline_revision = _integer(baseline["revision"], "$.accepted_baseline.revision", 0)
    if baseline_revision > state_revision:
        _fail("$.accepted_baseline.revision", "cannot exceed state revision")
    if not isinstance(baseline["members"], list):
        _fail("$.accepted_baseline.members", "must be an array")
    for index, member in enumerate(baseline["members"]):
        _accepted_member(member, f"$.accepted_baseline.members[{index}]", project_index)
        if _timestamp(member["accepted_at"], f"$.accepted_baseline.members[{index}].accepted_at") > updated_at:
            _fail(f"$.accepted_baseline.members[{index}].accepted_at", "cannot be later than state updated_at")

    slots = _object(state["slots"], "$.slots", {"A", "B"})
    if slots["A"] is None and slots["B"] is not None:
        _fail("$.slots", "slots must be left-packed; B cannot be occupied while A is empty")
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


def _pack_slots(slots: dict[str, Any]) -> None:
    if slots["A"] is None and slots["B"] is not None:
        slots["A"] = slots["B"]
        slots["B"] = None


def candidate_declaration(unit: dict[str, Any], replaces_accepted_deployment_id: str | None = None) -> dict[str, Any]:
    """Build immutable candidate input; deployment evidence is intentionally absent."""

    return {"unit": copy.deepcopy(unit), "replaces_accepted_deployment_id": replaces_accepted_deployment_id}


def _materialize_candidate(declaration: Any, existing_slots: dict[str, Any]) -> dict[str, Any]:
    declaration = _object(declaration, "candidate", {"unit", "replaces_accepted_deployment_id"})
    identity = {
        "unit": declaration["unit"],
        "replaces_accepted_deployment_id": declaration["replaces_accepted_deployment_id"],
    }
    for label in ("A", "B"):
        existing = existing_slots[label]
        if existing is not None and {
            "unit": existing["unit"],
            "replaces_accepted_deployment_id": existing["replaces_accepted_deployment_id"],
        } == identity:
            return copy.deepcopy(existing)
    return {
        "unit": copy.deepcopy(declaration["unit"]),
        "replaces_accepted_deployment_id": declaration["replaces_accepted_deployment_id"],
        "deployment": {"state": "NOT_DEPLOYED", "deployed_at": None, "ready_verified_at": None},
        "runtime_result": {"classification": "UNTESTED", "recorded_at": None},
    }


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
    elif operation_type == "SET_PROFILE":
        if set(operation) != {"type", "candidates"} or not isinstance(operation["candidates"], list):
            raise ValidationError("SET_PROFILE requires a candidates array")
        if len(operation["candidates"]) > 2:
            raise ValidationError("runtime profile cannot contain more than two candidates")
        slots["A"] = _materialize_candidate(operation["candidates"][0], state["slots"]) if operation["candidates"] else None
        slots["B"] = _materialize_candidate(operation["candidates"][1], state["slots"]) if len(operation["candidates"]) == 2 else None
    else:
        required_keys = {"type", "slot"}
        if operation_type == "RECORD_RESULT":
            required_keys.add("classification")
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
            }
        elif operation_type == "REMOVE_SLOT":
            slots[label] = None
            _pack_slots(slots)
        elif operation_type == "PROMOTE_SLOT":
            if slot["deployment"]["state"] != "READY_TO_TEST_VERIFIED" or slot["runtime_result"]["classification"] != "PASS":
                raise ValidationError("PROMOTE_SLOT requires an explicit READY_TO_TEST_VERIFIED PASS")
            if _timestamp(at, "transition timestamp") < _timestamp(slot["runtime_result"]["recorded_at"], "runtime result timestamp"):
                raise ValidationError("promotion cannot precede the recorded PASS")
            replacement_id = slot["replaces_accepted_deployment_id"]
            member = {"unit": copy.deepcopy(slot["unit"]), "accepted_at": at}
            members = next_state["accepted_baseline"]["members"]
            if replacement_id is None:
                members.append(member)
            else:
                indexes = [index for index, existing in enumerate(members) if existing["unit"]["deployment_id"] == replacement_id]
                if len(indexes) != 1:
                    raise ValidationError("accepted replacement target is not unique")
                members[indexes[0]] = member
            next_state["accepted_baseline"]["revision"] += 1
            slots[label] = None
            _pack_slots(slots)

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
    """Atomically commit a planned transition; live use fails closed while gated."""

    path = path.resolve()
    lock_path = path.with_name(path.name + ".lock")
    temporary_path: Path | None = None
    lock_acquired = False
    lock_token = secrets.token_hex(32)
    try:
        with lock_path.open("x", encoding="utf-8") as lock:
            lock.write(f"{os.getpid()}:{lock_token}")
        lock_acquired = True
        current = load_json(path)
        validate_runtime_state(current, project_index)
        if current["activation"] == "GATED":
            raise ValidationError("live runtime-state commit is disabled while activation is GATED")
        if state_digest(current) != expected_digest:
            raise ValidationError("stale runtime-state digest")
        next_state = plan_transition(current, expected_revision, operation, at, project_index)
        with tempfile.NamedTemporaryFile(
            mode="w",
            encoding="utf-8",
            newline="\n",
            prefix=path.name + ".",
            suffix=".tmp",
            dir=path.parent,
            delete=False,
        ) as temporary:
            json.dump(next_state, temporary, indent=2, ensure_ascii=False)
            temporary.write("\n")
            temporary.flush()
            os.fsync(temporary.fileno())
            temporary_path = Path(temporary.name)
        os.replace(temporary_path, path)
        temporary_path = None
        return next_state
    finally:
        if temporary_path is not None and temporary_path.exists():
            temporary_path.unlink()
        if lock_acquired and lock_path.exists():
            try:
                if lock_path.read_text(encoding="utf-8") == f"{os.getpid()}:{lock_token}":
                    lock_path.unlink()
            except OSError:
                pass
