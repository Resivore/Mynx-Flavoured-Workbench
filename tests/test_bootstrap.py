from __future__ import annotations

import copy
import base64
import hashlib
import hmac
import http.client
import io
import json
import re
import tempfile
import unittest
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path
from unittest.mock import patch
from uuid import NAMESPACE_URL, uuid5

from tools.runtime_slots import (
    candidate_declaration,
    commit_state,
    plan_transition,
    render_title_state,
    resolve_profile,
    state_digest,
    validate_runtime_state,
)
from tools.sheet_sync import (
    HUMAN_FIELDS,
    RECEIVER_RESPONSE_TIMEOUT_SECONDS,
    ZERO_COMMIT,
    _migration_adoption_uuids_at,
    build_events,
    flatten_manifest,
    main as sheet_sync_main,
    make_current_state_plan,
    migration_adoption_uuids,
    publish_plan,
    signed_wrapper,
    validate_project_push_contract,
)
from tools.workbench import (
    BUILD_STATES,
    DEPLOYMENT_STATES,
    LIFECYCLES,
    RUNTIME_STATES,
    ValidationError,
    current_release_deployment_comparison,
    load_json,
    load_repository_statuses,
    validate_log_append,
    validate_project_change_scope,
    validate_repository,
    validate_status,
    validate_status_transition,
    validate_testing_slot_lifecycles,
)


ROOT = Path(__file__).resolve().parents[1]
TIME_1 = "2026-08-29T12:00:00Z"
TIME_2 = "2026-08-29T12:01:00Z"
TIME_3 = "2026-08-29T12:02:00Z"


def stable_uuid(value: str) -> str:
    return str(uuid5(NAMESPACE_URL, "mynx-test:" + value))


def planned_manifest(project_id: str = "mossy-stone", name: str = "Mossy Stone", *, uuid_value: str | None = None) -> dict:
    return {
        "$schema": "../../schemas/workbench-status.schema.json",
        "schema_version": 1,
        "identity": {
            "uuid": uuid_value or stable_uuid(project_id),
            "name": name,
            "project_id": project_id,
            "aliases": [],
            "legacy_names": [],
            "legacy_ids": [],
        },
        "definition": {
            "lifecycle": "PLANNED",
            "goals": [f"Define the intended {name} project."],
            "scope": ["Project identity and future implementation."],
            "boundaries": {"owned_paths": [f"projects/{project_id}"], "exclusions": ["No implementation exists yet."]},
            "dependencies": [],
        },
        "state": {
            "milestone": "Official project initialized; implementation has not started.",
            "releases": {
                "current": None,
                "accepted": None,
                "rollback": None,
                "accepted_current": "NO_ACCEPTED",
                "accepted_rollback": "NO_ROLLBACK",
            },
            "validation": {"build": "NOT_RUN", "deployment": "NOT_DEPLOYED", "runtime": "RUNTIME_UNTESTED"},
            "blocker": None,
        },
        "synchronization": {
            "revision": 1,
            "activity_at": TIME_1,
            "updated_at": TIME_1,
            "last_codex_at": TIME_1,
            "source_commit": "a" * 40,
            "google_sheet": {"participates": True, "exclusion_reason": None},
        },
    }


def advance_manifest(manifest: dict) -> dict:
    result = copy.deepcopy(manifest)
    result["synchronization"].update(
        revision=manifest["synchronization"]["revision"] + 1,
        activity_at=TIME_2,
        updated_at=TIME_2,
        last_codex_at=TIME_2,
        source_commit="b" * 40,
    )
    return result


def sheet_manifest(
    project_id: str,
    name: str,
    *,
    revision: int,
    participates: bool = True,
    collection: str = "projects",
) -> dict:
    manifest = planned_manifest(project_id, name)
    manifest["definition"]["boundaries"]["owned_paths"] = [f"{collection}/{project_id}"]
    manifest["synchronization"]["revision"] = revision
    manifest["synchronization"]["google_sheet"] = {
        "participates": participates,
        "exclusion_reason": None if participates else "No human tracking value.",
    }
    return manifest


def incremental_plan(config: dict, events: list[dict] | None = None) -> dict:
    return {
        "contract_version": config["contract_version"],
        "plan_kind": "incremental",
        "source": {
            "repository": config["repository"],
            "ref": config["authoritative_ref"],
            "before": ZERO_COMMIT,
            "after": "c" * 40,
        },
        "events": [] if events is None else events,
    }


def publication_environment(config: dict) -> dict[str, str]:
    return {
        config["cutover_environment_variable"]: config["cutover_required_value"],
        config["receiver_url_environment_variable"]: "https://receiver.invalid/",
        config["hmac_environment_variable"]: "s" * 32,
    }


def ordinary_publication_event(config: dict) -> dict:
    path = "projects/mossy-stone/WORKBENCH_STATUS.json"
    return build_events(
        {},
        {path: planned_manifest()},
        repository=config["repository"],
        ref=config["authoritative_ref"],
        publication_commit="c" * 40,
        config=config,
    )[0]


def ordinary_publication_events(config: dict) -> list[dict]:
    manifests = {
        "projects/alpha/WORKBENCH_STATUS.json": planned_manifest("alpha", "Alpha"),
        "projects/beta/WORKBENCH_STATUS.json": planned_manifest("beta", "Beta"),
    }
    return build_events(
        {},
        manifests,
        repository=config["repository"],
        ref=config["authoritative_ref"],
        publication_commit="c" * 40,
        config=config,
    )


class JsonResponse:
    status = 200

    def __init__(self, body: dict):
        self.body = json.dumps(body).encode()

    def __enter__(self):
        return self

    def __exit__(self, *_args):
        return False

    def read(self):
        return self.body


class RawResponse(JsonResponse):
    def __init__(self, body: bytes):
        self.body = body


class ReadFailureResponse(JsonResponse):
    def read(self):
        raise http.client.IncompleteRead(b"partial", 10)


def request_event(request) -> dict:
    wrapper = json.loads(request.data)
    padded = wrapper["payload"] + "=" * (-len(wrapper["payload"]) % 4)
    return json.loads(base64.urlsafe_b64decode(padded).decode("utf-8"))


def codex_entry(manifest: dict, summary: str = "Initialize project") -> str:
    synchronization = manifest["synchronization"]
    return (
        f"## {synchronization['last_codex_at']} — {summary}\n"
        f"- Revision: {synchronization['revision']}\n"
        f"- Source checkpoint: `{synchronization['source_commit']}`\n"
        "- Changes: Updated the canonical project checkpoint.\n"
        "- Build/static: Not run.\n"
        "- Runtime: Not performed.\n"
        "- Artifact: None.\n"
        "- Result: Project state recorded.\n"
        "- Next state: Continue the bounded project lifecycle.\n"
    )


def codex_log(manifest: dict) -> str:
    return "# Codex Log\n\n" + codex_entry(manifest)


def write_project(root: Path, manifest: dict, log: str | None = None) -> Path:
    project_id = manifest["identity"]["project_id"]
    directory = root / "projects" / project_id
    directory.mkdir(parents=True)
    (directory / "WORKBENCH_STATUS.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    (directory / "TESTING.md").write_text("# Testing\n\nNo runtime procedure yet.\n", encoding="utf-8")
    (directory / "CODEX_LOG.md").write_text(log or codex_log(manifest), encoding="utf-8")
    return directory


def deployment_unit(
    project_id: str,
    *,
    version: str = "Canary 1",
    filename: str | None = None,
    ownership: str | None = None,
    ownership_keys: list[str] | None = None,
) -> dict:
    seed = project_id + ":" + version
    artifact_filename = filename or f"{project_id}-{version.lower().replace(' ', '-')}.jar"
    return {
        "deployment_id": stable_uuid("deployment:" + seed),
        "project_uuid": stable_uuid("project:" + project_id),
        "project_id": project_id,
        "project_identity_source": "CURRENT_MANIFEST",
        "version": version,
        "source_commit": ("c" if version == "Canary 1" else "d") * 40,
        "artifacts": [
            {
                "artifact_id": stable_uuid("artifact:" + seed),
                "kind": "MOD",
                "filename": artifact_filename,
                "sha256": ("1" if version == "Canary 1" else "2") * 64,
                "ownership_keys": ownership_keys or [ownership or f"mod:{project_id}"],
                "source": {
                    "type": "REPOSITORY",
                    "path": f"projects/{project_id}/artifacts/{artifact_filename}",
                },
            }
        ],
    }


def adopted_rollback_unit(project_id: str, *, version: str = "Legacy rollback") -> dict:
    unit = deployment_unit(project_id, version=version)
    unit["project_identity_source"] = "FROZEN_LEGACY"
    unit["source_commit"] = None
    for artifact in unit["artifacts"]:
        artifact["source"] = {
            "type": "ADOPTED_TARGET",
            "path": f"mods/{artifact['filename']}",
        }
    return unit


def candidate(unit: dict, result: str = "UNTESTED") -> dict:
    ready = result != "UNTESTED"
    return {
        "unit": copy.deepcopy(unit),
        "replaces_accepted_deployment_id": None,
        "deployment": {
            "state": "READY_TO_TEST_VERIFIED" if ready else "NOT_DEPLOYED",
            "deployed_at": TIME_1 if ready else None,
            "ready_verified_at": TIME_1 if ready else None,
        },
        "runtime_result": {
            "classification": result,
            "recorded_at": TIME_1 if ready else None,
            "evidence": {
                "passed": ["observed pass"] if ready else [],
                "failed": ["observed failure"] if result == "FAIL" else [],
            },
        },
    }


def runtime_state(accepted: list[dict] | None = None, slot_a: dict | None = None, slot_b: dict | None = None) -> dict:
    accepted = accepted or []
    return {
        "$schema": "../../schemas/runtime-state.schema.json",
        "schema_version": 1,
        "activation": "GATED",
        "revision": 3 if (accepted or slot_a or slot_b) else 0,
        "updated_at": TIME_1,
        "accepted_baseline": {
            "revision": 1 if accepted else 0,
            "provenance": {
                "source_repository": "Resivore/Minecraft-26.2-Workbench",
                "source_commit": "f" * 40,
                "source_profile": "fixture",
                "legacy_ledger_sha256": "a" * 64,
                "accepted_artifact_count": sum(len(member["unit"]["artifacts"]) for member in accepted),
                "overlay_order": [],
                "readiness": "READY_TO_TEST_VERIFIED",
                "diagnostics": 0,
                "physical_disposition": "PENDING",
            },
            "members": copy.deepcopy(accepted),
        },
        "slots": {"A": copy.deepcopy(slot_a), "B": copy.deepcopy(slot_b)},
    }


def project_index(*project_ids: str) -> dict[str, str]:
    return {stable_uuid("project:" + project_id): project_id for project_id in project_ids}


def status_catalog(*manifests: dict) -> dict[str, tuple[Path, dict]]:
    return {
        manifest["identity"]["uuid"]: (
            Path("projects") / manifest["identity"]["project_id"] / "WORKBENCH_STATUS.json",
            manifest,
        )
        for manifest in manifests
    }


class StatusContractTests(unittest.TestCase):
    def test_minimal_planned_project_needs_no_source_or_artifact(self) -> None:
        manifest = planned_manifest()
        validate_status(manifest)
        self.assertIsNone(manifest["state"]["releases"]["current"])

    def test_versioned_dot_project_id_matches_its_directory(self) -> None:
        manifest = planned_manifest("ribbits-26.2", "Ribbits 26.2 Port")
        validate_status(manifest)
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            write_project(root, manifest)
            statuses = load_repository_statuses(root)
            loaded = next(iter(statuses.values()))[1]
            self.assertEqual("ribbits-26.2", loaded["identity"]["project_id"])

    def test_schema_shape_and_enums_fail_closed(self) -> None:
        manifest = planned_manifest()
        manifest["unexpected"] = True
        with self.assertRaises(ValidationError):
            validate_status(manifest)

    def test_schema_enums_match_the_authoritative_validator(self) -> None:
        schema = load_json(ROOT / "schemas" / "workbench-status.schema.json")
        properties = schema["properties"]
        self.assertEqual(LIFECYCLES, set(properties["definition"]["properties"]["lifecycle"]["enum"]))
        validation = properties["state"]["properties"]["validation"]["properties"]
        self.assertEqual(BUILD_STATES, set(validation["build"]["enum"]))
        self.assertEqual(DEPLOYMENT_STATES, set(validation["deployment"]["enum"]))
        self.assertEqual(RUNTIME_STATES, set(validation["runtime"]["enum"]))
        manifest = planned_manifest()
        manifest["state"]["validation"]["runtime"] = "STATIC_PASS"
        with self.assertRaises(ValidationError):
            validate_status(manifest)

    def test_uuid_is_immutable_and_revision_advances_exactly_once(self) -> None:
        before = planned_manifest()
        after = advance_manifest(before)
        validate_status_transition(before, after)
        changed_uuid = copy.deepcopy(after)
        changed_uuid["identity"]["uuid"] = stable_uuid("replacement")
        with self.assertRaisesRegex(ValidationError, "immutable"):
            validate_status_transition(before, changed_uuid)
        skipped = copy.deepcopy(after)
        skipped["synchronization"]["revision"] = 3
        with self.assertRaisesRegex(ValidationError, "exactly once"):
            validate_status_transition(before, skipped)

    def test_duplicate_uuid_and_ambiguous_identity_are_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            first = planned_manifest("alpha", "Alpha")
            second = planned_manifest("beta", "Beta", uuid_value=first["identity"]["uuid"])
            write_project(root, first)
            write_project(root, second)
            with self.assertRaisesRegex(ValidationError, "duplicate project UUID"):
                load_repository_statuses(root)
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            first = planned_manifest("alpha", "Alpha")
            first["identity"]["aliases"] = ["Shared Name"]
            second = planned_manifest("beta", "shared name")
            write_project(root, first)
            write_project(root, second)
            with self.assertRaisesRegex(ValidationError, "ambiguous"):
                load_repository_statuses(root)

    def test_required_project_controls_and_planned_layout(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            directory = write_project(root, planned_manifest())
            statuses = load_repository_statuses(root)
            self.assertEqual(1, len(statuses))
            self.assertEqual({"WORKBENCH_STATUS.json", "TESTING.md", "CODEX_LOG.md"}, {path.name for path in directory.iterdir()})
            (directory / "TESTING.md").unlink()
            with self.assertRaisesRegex(ValidationError, "missing required"):
                load_repository_statuses(root)

    def test_change_scope_and_append_only_log(self) -> None:
        validate_project_change_scope(
            ["projects/alpha/src/Main.java", "projects/alpha/WORKBENCH_STATUS.json"],
            ["alpha"],
        )
        with self.assertRaisesRegex(ValidationError, "outside"):
            validate_project_change_scope(["projects/beta/src/Main.java"], ["alpha"])
        with self.assertRaisesRegex(ValidationError, "immutable"):
            validate_project_change_scope(["originals/reference.jar"], ["alpha"], ["originals"])
        validate_project_change_scope(["tools/shared.py"], ["alpha"], ["tools/shared.py"])
        before = planned_manifest("alpha", "Alpha")
        after = advance_manifest(before)
        previous_log = codex_log(before)
        current_log = previous_log + "\n" + codex_entry(after, "Advance project")
        validate_log_append(previous_log, current_log, after)
        with self.assertRaisesRegex(ValidationError, "append-only"):
            validate_log_append(previous_log, codex_log(after), after)

    def test_testing_lifecycle_requires_actual_slot_occupancy(self) -> None:
        manifest = planned_manifest("alpha", "Alpha")
        manifest["definition"]["lifecycle"] = "TESTING"
        validate_status(manifest)
        state = runtime_state()
        validate_runtime_state(state, project_index={})
        with self.assertRaisesRegex(ValidationError, "lifecycle TESTING.*Test Slot A or B"):
            validate_testing_slot_lifecycles(status_catalog(manifest), state)

    def test_occupied_slot_requires_testing_lifecycle(self) -> None:
        unit = deployment_unit("alpha")
        manifest = planned_manifest("alpha", "Alpha", uuid_value=unit["project_uuid"])
        manifest["definition"]["lifecycle"] = "ACTIVE"
        state = runtime_state(slot_a=candidate(unit))
        validate_runtime_state(state, project_index("alpha"))
        with self.assertRaisesRegex(ValidationError, "occupies Test Slot A.*must be TESTING"):
            validate_testing_slot_lifecycles(status_catalog(manifest), state)

    def test_active_ready_candidate_outside_slots_is_valid(self) -> None:
        manifest = planned_manifest("alpha", "Alpha")
        manifest["definition"]["lifecycle"] = "ACTIVE"
        manifest["state"]["milestone"] = "Implementation is statically verified and ready for runtime testing."
        manifest["state"]["validation"]["build"] = "STATIC_PASS"
        validate_status(manifest)
        state = runtime_state()
        validate_runtime_state(state, project_index={})
        validate_testing_slot_lifecycles(status_catalog(manifest), state)

    def test_active_exact_candidate_accepts_external_runtime_results_without_deployment(self) -> None:
        unit = deployment_unit("alpha")
        current_release = {
            "version": unit["version"],
            "artifact": {
                "filename": unit["artifacts"][0]["filename"],
                "sha256": unit["artifacts"][0]["sha256"],
            },
            "source_commit": unit["source_commit"],
        }
        for result in ("RUNTIME_PASS", "RUNTIME_FAIL", "INCONCLUSIVE"):
            with self.subTest(result=result):
                manifest = planned_manifest(
                    "alpha",
                    "Alpha",
                    uuid_value=unit["project_uuid"],
                )
                manifest["definition"]["lifecycle"] = "ACTIVE"
                manifest["state"]["releases"]["current"] = copy.deepcopy(current_release)
                manifest["state"]["validation"].update(
                    deployment="NOT_DEPLOYED",
                    runtime=result,
                )
                validate_status(manifest)
                state = runtime_state()
                validate_runtime_state(state, project_index={})
                validate_testing_slot_lifecycles(status_catalog(manifest), state)
                self.assertEqual("ACTIVE", manifest["definition"]["lifecycle"])
                self.assertEqual("NOT_DEPLOYED", manifest["state"]["validation"]["deployment"])

    def test_external_runtime_result_transition_binds_to_preexisting_exact_current_release(self) -> None:
        unit = deployment_unit("alpha")
        before = planned_manifest("alpha", "Alpha", uuid_value=unit["project_uuid"])
        before["definition"]["lifecycle"] = "ACTIVE"
        before["state"]["releases"]["current"] = {
            "version": unit["version"],
            "artifact": {
                "filename": unit["artifacts"][0]["filename"],
                "sha256": unit["artifacts"][0]["sha256"],
            },
            "source_commit": unit["source_commit"],
        }

        for result in ("RUNTIME_PASS", "RUNTIME_FAIL", "INCONCLUSIVE"):
            with self.subTest(result=result):
                after = advance_manifest(before)
                after["state"]["validation"]["runtime"] = result
                validate_status_transition(before, after)

        missing_before = planned_manifest("alpha", "Alpha", uuid_value=unit["project_uuid"])
        missing_before["definition"]["lifecycle"] = "ACTIVE"
        missing_after = advance_manifest(missing_before)
        missing_after["state"]["validation"]["runtime"] = "RUNTIME_PASS"
        with self.assertRaises(ValidationError):
            validate_status_transition(missing_before, missing_after)

        artifactless_before = copy.deepcopy(before)
        artifactless_before["state"]["releases"]["current"]["artifact"] = None
        artifactless_after = advance_manifest(artifactless_before)
        artifactless_after["state"]["validation"]["runtime"] = "RUNTIME_PASS"
        with self.assertRaisesRegex(ValidationError, "artifact/version/hash/source identity"):
            validate_status_transition(artifactless_before, artifactless_after)

        mutations = {
            "version": lambda release: release.__setitem__("version", "Canary 2"),
            "filename": lambda release: release["artifact"].__setitem__("filename", "alpha-canary-2.jar"),
            "sha256": lambda release: release["artifact"].__setitem__("sha256", "2" * 64),
            "source_commit": lambda release: release.__setitem__("source_commit", "d" * 40),
        }
        for field, mutate in mutations.items():
            with self.subTest(changed_identity_field=field):
                changed = advance_manifest(before)
                changed["state"]["validation"]["runtime"] = "RUNTIME_PASS"
                mutate(changed["state"]["releases"]["current"])
                with self.assertRaises(ValidationError):
                    validate_status_transition(before, changed)

    def test_changed_artifact_requires_release_scoped_runtime_dependency_policy(self) -> None:
        before = planned_manifest("alpha", "Alpha")
        before["definition"]["lifecycle"] = "ACTIVE"
        before["state"]["releases"]["current"] = {
            "version": "0.1.0-canary1",
            "artifact": {"filename": "alpha-c1.jar", "sha256": "1" * 64},
            "source_commit": "c" * 40,
        }
        after = advance_manifest(before)
        after["state"]["releases"]["current"] = {
            "version": "0.1.0-canary2",
            "artifact": {"filename": "alpha-c2.jar", "sha256": "2" * 64},
            "source_commit": "d" * 40,
        }
        with self.assertRaisesRegex(ValidationError, "runtime_dependency_policy.*required"):
            validate_status_transition(before, after)

        after["state"]["releases"]["current"]["runtime_dependency_policy"] = {
            "contract": "CAPABILITY_OR_PROVIDER",
            "exceptions": [],
        }
        validate_status_transition(before, after)

        removal = copy.deepcopy(after)
        removal["synchronization"].update(
            revision=after["synchronization"]["revision"] + 1,
            activity_at=TIME_3,
            updated_at=TIME_3,
            last_codex_at=TIME_3,
            source_commit="e" * 40,
        )
        del removal["state"]["releases"]["current"]["runtime_dependency_policy"]
        with self.assertRaisesRegex(ValidationError, "cannot be removed"):
            validate_status_transition(after, removal)

    def test_unchanged_historical_bytes_remain_grandfathered_during_promotion(self) -> None:
        before = planned_manifest("alpha", "Alpha")
        before["definition"]["lifecycle"] = "TESTING"
        release = {
            "version": "0.1.0-canary1",
            "artifact": {"filename": "alpha-c1.jar", "sha256": "1" * 64},
            "source_commit": "c" * 40,
        }
        before["state"]["releases"]["current"] = copy.deepcopy(release)
        after = advance_manifest(before)
        after["definition"]["lifecycle"] = "ACCEPTED"
        after["state"]["releases"].update(
            accepted=copy.deepcopy(release),
            accepted_current="CURRENT_IS_ACCEPTED",
        )
        validate_status_transition(before, after)

    def test_additive_embedded_version_preserves_grandfathering_and_external_identity(self) -> None:
        before = planned_manifest("alpha", "Alpha")
        before["definition"]["lifecycle"] = "ACTIVE"
        before["state"]["releases"]["current"] = {
            "version": "Private Canary 10",
            "artifact": {"filename": "alpha-c10-private.jar", "sha256": "1" * 64},
            "source_commit": "c" * 40,
        }
        after = advance_manifest(before)
        after["state"]["releases"]["current"]["embedded_version"] = "0.1.0-canary9"
        after["state"]["validation"]["runtime"] = "RUNTIME_PASS"

        validate_status_transition(before, after)
        self.assertNotIn(
            "runtime_dependency_policy",
            after["state"]["releases"]["current"],
        )

    def test_runtime_dependency_exception_shape_requires_exact_regression_evidence(self) -> None:
        manifest = planned_manifest("alpha", "Alpha")
        manifest["definition"]["lifecycle"] = "ACTIVE"
        manifest["state"]["releases"]["current"] = {
            "version": "0.1.0-canary2",
            "artifact": {"filename": "alpha-c2.jar", "sha256": "2" * 64},
            "source_commit": "d" * 40,
            "runtime_dependency_policy": {
                "contract": "CAPABILITY_OR_PROVIDER",
                "exceptions": [
                    {
                        "consumer_id": "alpha",
                        "relationship": "depends",
                        "dependency_id": "provider_api",
                        "predicate": "<2.0.0-",
                        "reason": "Provider 2 removed the required API.",
                        "regression_evidence": ["Focused provider-2 fixture fails initialization."],
                    }
                ],
            },
        }
        validate_status(manifest)
        manifest["state"]["releases"]["current"]["runtime_dependency_policy"]["exceptions"][0][
            "regression_evidence"
        ] = []
        with self.assertRaisesRegex(ValidationError, "regression_evidence"):
            validate_status(manifest)

    def test_runtime_dependency_policy_requires_concrete_artifact_in_validator_and_schema(self) -> None:
        manifest = planned_manifest("alpha", "Alpha")
        manifest["definition"]["lifecycle"] = "ACTIVE"
        manifest["state"]["releases"]["current"] = {
            "version": "0.1.0-canary1",
            "artifact": None,
            "source_commit": "c" * 40,
            "runtime_dependency_policy": {
                "contract": "CAPABILITY_OR_PROVIDER",
                "exceptions": [],
            },
        }
        with self.assertRaisesRegex(ValidationError, "requires a concrete artifact"):
            validate_status(manifest)

        release_schema = load_json(ROOT / "schemas" / "workbench-status.schema.json")["$defs"]["release"]
        self.assertEqual(
            {
                "if": {"required": ["runtime_dependency_policy"]},
                "then": {"properties": {"artifact": {"$ref": "#/$defs/artifact"}}},
            },
            release_schema["allOf"][0],
        )

    def test_slot_runtime_result_does_not_change_testing_lifecycle(self) -> None:
        unit = deployment_unit("alpha")
        manifest = planned_manifest("alpha", "Alpha", uuid_value=unit["project_uuid"])
        manifest["definition"]["lifecycle"] = "TESTING"
        for result in ("UNTESTED", "PASS"):
            with self.subTest(result=result):
                state = runtime_state(slot_a=candidate(unit, result))
                validate_runtime_state(state, project_index("alpha"))
                validate_testing_slot_lifecycles(status_catalog(manifest), state)

    def test_accepted_release_can_coexist_with_testing_successor(self) -> None:
        accepted_unit = deployment_unit("alpha", version="Canary 1")
        current_unit = deployment_unit("alpha", version="Canary 2")
        manifest = planned_manifest("alpha", "Alpha", uuid_value=current_unit["project_uuid"])
        manifest["definition"]["lifecycle"] = "TESTING"
        manifest["state"]["releases"].update(
            current={
                "version": current_unit["version"],
                "artifact": {
                    "filename": current_unit["artifacts"][0]["filename"],
                    "sha256": current_unit["artifacts"][0]["sha256"],
                },
                "source_commit": current_unit["source_commit"],
            },
            accepted={
                "version": accepted_unit["version"],
                "artifact": {
                    "filename": accepted_unit["artifacts"][0]["filename"],
                    "sha256": accepted_unit["artifacts"][0]["sha256"],
                },
                "source_commit": accepted_unit["source_commit"],
            },
            accepted_current="CURRENT_DIFFERS_FROM_ACCEPTED",
        )
        validate_status(manifest)
        successor = candidate(current_unit)
        successor["replaces_accepted_deployment_id"] = accepted_unit["deployment_id"]
        state = runtime_state(
            accepted=[{"unit": accepted_unit, "accepted_at": TIME_1}],
            slot_a=successor,
        )
        validate_runtime_state(state, project_index("alpha"))
        validate_testing_slot_lifecycles(status_catalog(manifest), state)

    def test_repository_layout_validates(self) -> None:
        statuses = validate_repository(ROOT)
        self.assertIsInstance(statuses, dict)


class RuntimeContractTests(unittest.TestCase):
    def test_zero_one_and_two_independent_slots(self) -> None:
        validate_runtime_state(runtime_state(), project_index={})
        alpha = deployment_unit("alpha")
        beta = deployment_unit("beta")
        one = runtime_state(slot_a=candidate(alpha))
        validate_runtime_state(one, project_index("alpha"))
        two = runtime_state(slot_a=candidate(alpha, "PASS"), slot_b=candidate(beta, "FAIL"))
        validate_runtime_state(two, project_index("alpha", "beta"))
        self.assertEqual("PASS", two["slots"]["A"]["runtime_result"]["classification"])
        self.assertEqual("FAIL", two["slots"]["B"]["runtime_result"]["classification"])

    def test_b_only_is_valid_and_more_than_two_are_rejected(self) -> None:
        alpha = deployment_unit("alpha")
        state = runtime_state(slot_b=candidate(alpha))
        validate_runtime_state(state, project_index("alpha"))
        self.assertIsNone(state["slots"]["A"])
        self.assertEqual("alpha", state["slots"]["B"]["unit"]["project_id"])
        with self.assertRaisesRegex(ValidationError, "more than two"):
            plan_transition(runtime_state(), 0, {"type": "SET_PROFILE", "candidates": [candidate_declaration(alpha)] * 3}, TIME_2)
        state = runtime_state()
        state["slots"]["C"] = None
        with self.assertRaisesRegex(ValidationError, "unknown"):
            validate_runtime_state(state)

    def test_effective_artifact_and_ownership_collisions_are_rejected(self) -> None:
        alpha = deployment_unit("alpha", filename="shared.jar")
        beta = deployment_unit("beta", filename="SHARED.jar")
        with self.assertRaisesRegex(ValidationError, "filename collision"):
            validate_runtime_state(runtime_state(slot_a=candidate(alpha), slot_b=candidate(beta)), project_index("alpha", "beta"))
        beta = deployment_unit("beta", ownership="mod:alpha")
        with self.assertRaisesRegex(ValidationError, "ownership collision"):
            validate_runtime_state(runtime_state(slot_a=candidate(alpha), slot_b=candidate(beta)), project_index("alpha", "beta"))

    def test_assignment_initializes_evidence_and_rejects_fabricated_pass(self) -> None:
        alpha = deployment_unit("alpha")
        state = runtime_state()
        with self.assertRaisesRegex(ValidationError, "unknown"):
            plan_transition(state, 0, {"type": "ASSIGN_SLOT", "candidate": candidate(alpha, "PASS")}, TIME_2, project_index("alpha"))
        assigned = plan_transition(
            state,
            0,
            {"type": "ASSIGN_SLOT", "candidate": candidate_declaration(alpha)},
            TIME_2,
            project_index("alpha"),
        )
        self.assertEqual("NOT_DEPLOYED", assigned["slots"]["A"]["deployment"]["state"])
        self.assertEqual("UNTESTED", assigned["slots"]["A"]["runtime_result"]["classification"])

        empty_pass = runtime_state(slot_a=candidate(alpha, "PASS"))
        empty_pass["slots"]["A"]["runtime_result"]["evidence"]["passed"] = []
        with self.assertRaisesRegex(ValidationError, "PASS requires"):
            validate_runtime_state(empty_pass, project_index("alpha"))

    def test_activation_identity_and_source_gates_fail_closed(self) -> None:
        state = runtime_state()
        state["activation"] = "ACTIVE"
        with self.assertRaisesRegex(ValidationError, "adopted physical state"):
            validate_runtime_state(state)
        state["accepted_baseline"]["provenance"]["physical_disposition"] = "ADOPTED"
        state["accepted_baseline"]["provenance"]["diagnostics"] = 1
        with self.assertRaisesRegex(ValidationError, "zero diagnostics"):
            validate_runtime_state(state)

        alpha = deployment_unit("alpha")
        alpha["project_identity_source"] = "FROZEN_LEGACY"
        with self.assertRaisesRegex(ValidationError, "runtime slots require CURRENT_MANIFEST"):
            validate_runtime_state(runtime_state(slot_a=candidate(alpha)), project_index("alpha"))
        alpha = deployment_unit("alpha")
        alpha["source_commit"] = None
        with self.assertRaisesRegex(ValidationError, "exact source commit"):
            validate_runtime_state(runtime_state(slot_a=candidate(alpha)), project_index("alpha"))

    def test_accepted_baseline_collision_cannot_be_hidden_by_upgrade(self) -> None:
        alpha = deployment_unit("alpha", filename="shared.jar")
        beta = deployment_unit("beta", filename="SHARED.jar")
        alpha_v2 = deployment_unit("alpha", version="Canary 2")
        upgrade = candidate(alpha_v2)
        upgrade["replaces_accepted_deployment_id"] = alpha["deployment_id"]
        accepted = [{"unit": alpha, "accepted_at": TIME_1}, {"unit": beta, "accepted_at": TIME_1}]
        with self.assertRaisesRegex(ValidationError, "accepted baseline"):
            validate_runtime_state(runtime_state(accepted, upgrade), project_index("alpha", "beta"))

    def test_windows_unsafe_artifact_names_and_time_regression_are_rejected(self) -> None:
        alpha = deployment_unit("alpha", filename="alpha.jar:stream")
        with self.assertRaisesRegex(ValidationError, "Windows-safe"):
            validate_runtime_state(runtime_state(slot_a=candidate(alpha)), project_index("alpha"))
        alpha = deployment_unit("alpha")
        alpha["artifacts"][0]["source"]["path"] = "C:/alpha-canary-1.jar"
        with self.assertRaisesRegex(ValidationError, "normalized relative POSIX"):
            validate_runtime_state(runtime_state(slot_a=candidate(alpha)), project_index("alpha"))
        alpha = deployment_unit("alpha")
        alpha["artifacts"][0]["source"]["path"] = "originals/alpha-canary-1.jar"
        with self.assertRaisesRegex(ValidationError, "originals"):
            validate_runtime_state(runtime_state(slot_a=candidate(alpha)), project_index("alpha"))
        alpha = deployment_unit("alpha")
        alpha["artifacts"][0]["kind"] = "RESOURCE_PACK"
        with self.assertRaisesRegex(ValidationError, "must be one of: MOD"):
            validate_runtime_state(runtime_state(slot_a=candidate(alpha)), project_index("alpha"))
        state = runtime_state()
        with self.assertRaisesRegex(ValidationError, "cannot precede"):
            plan_transition(
                state,
                0,
                {"type": "ASSIGN_SLOT", "candidate": candidate_declaration(deployment_unit("beta"))},
                "2026-08-29T11:59:59Z",
                project_index("beta"),
            )

    def test_promote_a_preserves_b_in_its_independent_slot(self) -> None:
        base = deployment_unit("base")
        alpha = deployment_unit("alpha")
        beta = deployment_unit("beta")
        accepted = [{"unit": base, "accepted_at": TIME_1}]
        slot_a = candidate(alpha, "PASS")
        slot_b = candidate(beta, "FAIL")
        state = runtime_state(accepted, slot_a, slot_b)
        prior_b = copy.deepcopy(state["slots"]["B"])
        next_state = plan_transition(state, state["revision"], {"type": "PROMOTE_SLOT", "slot": "A"}, TIME_2, project_index("base", "alpha", "beta"))
        self.assertIsNone(next_state["slots"]["A"])
        self.assertEqual(prior_b, next_state["slots"]["B"])
        self.assertEqual(state["revision"] + 1, next_state["revision"])
        self.assertEqual(state["accepted_baseline"]["revision"] + 1, next_state["accepted_baseline"]["revision"])
        self.assertEqual({"base", "alpha", "beta"}, {unit["project_id"] for unit in resolve_profile(next_state, project_index("base", "alpha", "beta"))})

    def test_user_passed_batch_promotes_three_members_once_and_preserves_both_slots(self) -> None:
        base = deployment_unit("base")
        slot_a = candidate(deployment_unit("slot-alpha"), "PASS")
        slot_b = candidate(deployment_unit("slot-beta"), "FAIL")
        state = runtime_state(
            accepted=[{"unit": base, "accepted_at": TIME_1}],
            slot_a=slot_a,
            slot_b=slot_b,
        )
        prior_slots = json.dumps(state["slots"], separators=(",", ":"))
        alpha = deployment_unit("alpha")
        beta = deployment_unit("beta")
        gamma = deployment_unit("gamma")
        gamma_rollback = adopted_rollback_unit("gamma")
        promoted = plan_transition(
            state,
            state["revision"],
            {
                "type": "PROMOTE_USER_PASSED_BATCH",
                "authorization": "USER_REPORTED_EXACT_RUNTIME_PASS",
                "members": [
                    {"unit": alpha},
                    {"unit": beta},
                    {"unit": gamma, "retained_rollbacks": [gamma_rollback]},
                ],
            },
            TIME_2,
            project_index("base", "slot-alpha", "slot-beta", "alpha", "beta", "gamma"),
        )

        self.assertEqual(prior_slots, json.dumps(promoted["slots"], separators=(",", ":")))
        self.assertEqual(state["revision"] + 1, promoted["revision"])
        self.assertEqual(
            state["accepted_baseline"]["revision"] + 3,
            promoted["accepted_baseline"]["revision"],
        )
        self.assertEqual(4, promoted["accepted_baseline"]["provenance"]["accepted_artifact_count"])
        self.assertEqual(
            [TIME_2, TIME_2, TIME_2],
            [member["accepted_at"] for member in promoted["accepted_baseline"]["members"][-3:]],
        )
        retained = promoted["accepted_baseline"]["members"][-1]["retained_rollbacks"]
        self.assertEqual([{"unit": gamma_rollback, "retained_at": TIME_2}], retained)
        self.assertEqual(
            {"base", "alpha", "beta", "gamma", "slot-alpha", "slot-beta"},
            {
                unit["project_id"]
                for unit in resolve_profile(
                    promoted,
                    project_index("base", "slot-alpha", "slot-beta", "alpha", "beta", "gamma"),
                )
            },
        )

    def test_user_passed_batch_requires_exact_authorization_and_current_manifest_members(self) -> None:
        state = runtime_state()
        alpha = deployment_unit("alpha")
        operation = {
            "type": "PROMOTE_USER_PASSED_BATCH",
            "authorization": "USER_REPORTED_RUNTIME_PASS",
            "members": [{"unit": alpha}],
        }
        with self.assertRaisesRegex(ValidationError, "USER_REPORTED_EXACT_RUNTIME_PASS"):
            plan_transition(state, 0, operation, TIME_2, project_index("alpha"))

        operation["authorization"] = "USER_REPORTED_EXACT_RUNTIME_PASS"
        operation["members"][0]["unit"]["project_identity_source"] = "FROZEN_LEGACY"
        operation["members"][0]["unit"]["source_commit"] = None
        with self.assertRaisesRegex(ValidationError, "requires CURRENT_MANIFEST identity"):
            plan_transition(state, 0, operation, TIME_2, project_index("alpha"))

    def test_user_passed_batch_replaces_exact_accepted_predecessor_and_preserves_slots(self) -> None:
        alpha_v1 = deployment_unit("alpha", version="Canary 1")
        alpha_v2 = deployment_unit("alpha", version="Canary 2")
        slot_a = candidate(deployment_unit("slot-alpha"), "PASS")
        slot_b = candidate(deployment_unit("slot-beta"), "FAIL")
        state = runtime_state(
            accepted=[{"unit": alpha_v1, "accepted_at": TIME_1}],
            slot_a=slot_a,
            slot_b=slot_b,
        )
        prior_slots = json.dumps(state["slots"], separators=(",", ":"))
        promoted = plan_transition(
            state,
            state["revision"],
            {
                "type": "PROMOTE_USER_PASSED_BATCH",
                "authorization": "USER_REPORTED_EXACT_RUNTIME_PASS",
                "members": [
                    {
                        "unit": alpha_v2,
                        "replaces_accepted_deployment_id": alpha_v1["deployment_id"],
                    }
                ],
            },
            TIME_2,
            project_index("alpha", "slot-alpha", "slot-beta"),
        )

        self.assertEqual(prior_slots, json.dumps(promoted["slots"], separators=(",", ":")))
        self.assertEqual(state["revision"] + 1, promoted["revision"])
        self.assertEqual(
            state["accepted_baseline"]["revision"] + 1,
            promoted["accepted_baseline"]["revision"],
        )
        self.assertEqual(1, len(promoted["accepted_baseline"]["members"]))
        accepted = promoted["accepted_baseline"]["members"][0]
        self.assertEqual(alpha_v2, accepted["unit"])
        self.assertEqual(TIME_2, accepted["accepted_at"])

    def test_user_passed_batch_byte_identical_successor_updates_exact_identity_without_stack_bump(self) -> None:
        alpha_v1 = deployment_unit("alpha", version="Canary 1")
        alpha_v2 = deployment_unit("alpha", version="Canary 2")
        alpha_v2["artifacts"][0]["sha256"] = alpha_v1["artifacts"][0]["sha256"]
        slot_a = candidate(deployment_unit("slot-alpha"), "PASS")
        slot_b = candidate(deployment_unit("slot-beta"), "FAIL")
        state = runtime_state(
            accepted=[{"unit": alpha_v1, "accepted_at": TIME_1}],
            slot_a=slot_a,
            slot_b=slot_b,
        )
        prior_slots = copy.deepcopy(state["slots"])

        promoted = plan_transition(
            state,
            state["revision"],
            {
                "type": "PROMOTE_USER_PASSED_BATCH",
                "authorization": "USER_REPORTED_EXACT_RUNTIME_PASS",
                "members": [
                    {
                        "unit": alpha_v2,
                        "replaces_accepted_deployment_id": alpha_v1["deployment_id"],
                    }
                ],
            },
            TIME_2,
            project_index("alpha", "slot-alpha", "slot-beta"),
        )

        self.assertEqual(state["revision"] + 1, promoted["revision"])
        self.assertEqual(
            state["accepted_baseline"]["revision"],
            promoted["accepted_baseline"]["revision"],
        )
        self.assertEqual(prior_slots, promoted["slots"])
        self.assertEqual(alpha_v2, promoted["accepted_baseline"]["members"][0]["unit"])
        self.assertEqual(TIME_2, promoted["accepted_baseline"]["members"][0]["accepted_at"])

    def test_user_passed_batch_replacement_identity_and_slot_occupancy_fail_closed(self) -> None:
        alpha_v1 = deployment_unit("alpha", version="Canary 1")
        alpha_v2 = deployment_unit("alpha", version="Canary 2")
        accepted_state = runtime_state(accepted=[{"unit": alpha_v1, "accepted_at": TIME_1}])

        def replacement_operation(replacement_id: str | None, *, include: bool = True) -> dict:
            member = {"unit": copy.deepcopy(alpha_v2)}
            if include:
                member["replaces_accepted_deployment_id"] = replacement_id
            return {
                "type": "PROMOTE_USER_PASSED_BATCH",
                "authorization": "USER_REPORTED_EXACT_RUNTIME_PASS",
                "members": [member],
            }

        for operation in (
            replacement_operation(None, include=False),
            replacement_operation(None),
            replacement_operation(stable_uuid("wrong accepted deployment")),
        ):
            with self.subTest(operation=operation):
                with self.assertRaisesRegex(ValidationError, "exact accepted predecessor deployment"):
                    plan_transition(
                        accepted_state,
                        accepted_state["revision"],
                        operation,
                        TIME_2,
                        project_index("alpha"),
                    )

        duplicate_deployment = replacement_operation(alpha_v1["deployment_id"])
        duplicate_deployment["members"][0]["unit"]["deployment_id"] = alpha_v1["deployment_id"]
        with self.assertRaisesRegex(ValidationError, "deployment_id must be distinct"):
            plan_transition(
                accepted_state,
                accepted_state["revision"],
                duplicate_deployment,
                TIME_2,
                project_index("alpha"),
            )

        duplicate_artifact = replacement_operation(alpha_v1["deployment_id"])
        duplicate_artifact["members"][0]["unit"]["artifacts"][0]["artifact_id"] = (
            alpha_v1["artifacts"][0]["artifact_id"]
        )
        with self.assertRaisesRegex(ValidationError, "artifact UUIDs distinct"):
            plan_transition(
                accepted_state,
                accepted_state["revision"],
                duplicate_artifact,
                TIME_2,
                project_index("alpha"),
            )

        no_predecessor = runtime_state()
        with self.assertRaisesRegex(ValidationError, "null or omitted"):
            plan_transition(
                no_predecessor,
                0,
                replacement_operation(alpha_v1["deployment_id"]),
                TIME_2,
                project_index("alpha"),
            )

        for label in ("A", "B"):
            with self.subTest(occupied_slot=label):
                occupied_candidate = candidate(alpha_v2)
                occupied_candidate["replaces_accepted_deployment_id"] = alpha_v1["deployment_id"]
                occupied = runtime_state(
                    accepted=[{"unit": alpha_v1, "accepted_at": TIME_1}],
                    slot_a=occupied_candidate if label == "A" else None,
                    slot_b=occupied_candidate if label == "B" else None,
                )
                with self.assertRaisesRegex(ValidationError, "absent from both managed Test Slots"):
                    plan_transition(
                        occupied,
                        occupied["revision"],
                        replacement_operation(alpha_v1["deployment_id"]),
                        TIME_2,
                        project_index("alpha"),
                    )

    def test_user_passed_batch_rejects_duplicate_and_occupied_projects(self) -> None:
        alpha = deployment_unit("alpha")
        base_operation = {
            "type": "PROMOTE_USER_PASSED_BATCH",
            "authorization": "USER_REPORTED_EXACT_RUNTIME_PASS",
            "members": [{"unit": alpha}],
        }

        duplicate_operation = copy.deepcopy(base_operation)
        duplicate_operation["members"].append({"unit": copy.deepcopy(alpha)})
        with self.assertRaisesRegex(ValidationError, "duplicates a project"):
            plan_transition(
                runtime_state(),
                0,
                duplicate_operation,
                TIME_2,
                project_index("alpha"),
            )

        occupied = runtime_state(slot_b=candidate(alpha))
        with self.assertRaisesRegex(ValidationError, "absent from both managed Test Slots"):
            plan_transition(
                occupied,
                occupied["revision"],
                base_operation,
                TIME_2,
                project_index("alpha"),
            )

    def test_user_passed_batch_validates_retained_rollback_identity_source_and_ids(self) -> None:
        state = runtime_state()
        alpha = deployment_unit("alpha")
        rollback = adopted_rollback_unit("alpha")

        def operation_with(*retained: dict) -> dict:
            return {
                "type": "PROMOTE_USER_PASSED_BATCH",
                "authorization": "USER_REPORTED_EXACT_RUNTIME_PASS",
                "members": [{"unit": copy.deepcopy(alpha), "retained_rollbacks": list(retained)}],
            }

        with self.assertRaisesRegex(ValidationError, "at most one retained rollback"):
            plan_transition(
                state,
                0,
                operation_with(rollback, copy.deepcopy(rollback)),
                TIME_2,
                project_index("alpha"),
            )

        other_project = adopted_rollback_unit("beta")
        with self.assertRaisesRegex(ValidationError, "promoted project UUID and project ID"):
            plan_transition(
                state,
                0,
                operation_with(other_project),
                TIME_2,
                project_index("alpha", "beta"),
            )

        repository_rollback = copy.deepcopy(rollback)
        repository_filename = repository_rollback["artifacts"][0]["filename"]
        repository_rollback["artifacts"][0]["source"] = {
            "type": "REPOSITORY",
            "path": f"projects/alpha/artifacts/{repository_filename}",
        }
        with self.assertRaisesRegex(ValidationError, "ADOPTED_TARGET"):
            plan_transition(
                state,
                0,
                operation_with(repository_rollback),
                TIME_2,
                project_index("alpha"),
            )

        current_manifest_rollback = copy.deepcopy(rollback)
        current_manifest_rollback["project_identity_source"] = "CURRENT_MANIFEST"
        current_manifest_rollback["source_commit"] = "3" * 40
        with self.assertRaisesRegex(ValidationError, "FROZEN_LEGACY"):
            plan_transition(
                state,
                0,
                operation_with(current_manifest_rollback),
                TIME_2,
                project_index("alpha"),
            )

        duplicate_deployment = copy.deepcopy(rollback)
        duplicate_deployment["deployment_id"] = alpha["deployment_id"]
        with self.assertRaisesRegex(ValidationError, "distinct from the promoted deployment UUID"):
            plan_transition(
                state,
                0,
                operation_with(duplicate_deployment),
                TIME_2,
                project_index("alpha"),
            )

        duplicate_artifact = copy.deepcopy(rollback)
        duplicate_artifact["artifacts"][0]["artifact_id"] = alpha["artifacts"][0]["artifact_id"]
        with self.assertRaisesRegex(ValidationError, "artifact UUIDs distinct"):
            plan_transition(
                state,
                0,
                operation_with(duplicate_artifact),
                TIME_2,
                project_index("alpha"),
            )

    def test_update_and_clear_one_slot_preserve_the_other(self) -> None:
        alpha = deployment_unit("alpha")
        alpha_v2 = deployment_unit("alpha", version="Canary 2")
        beta = deployment_unit("beta")
        state = runtime_state(slot_a=candidate(alpha, "FAIL"), slot_b=candidate(beta, "PASS"))
        prior_b = copy.deepcopy(state["slots"]["B"])
        updated = plan_transition(
            state,
            state["revision"],
            {"type": "UPDATE_SLOT", "slot": "A", "candidate": candidate_declaration(alpha_v2)},
            TIME_2,
            project_index("alpha", "beta"),
        )
        self.assertEqual("UNTESTED", updated["slots"]["A"]["runtime_result"]["classification"])
        self.assertEqual(prior_b, updated["slots"]["B"])
        cleared = plan_transition(
            updated,
            updated["revision"],
            {"type": "REMOVE_SLOT", "slot": "A"},
            TIME_3,
            project_index("alpha", "beta"),
        )
        self.assertIsNone(cleared["slots"]["A"])
        self.assertEqual(prior_b, cleared["slots"]["B"])

    def test_successor_update_preserves_accepted_replacement_suppression(self) -> None:
        alpha_v1 = deployment_unit("alpha", version="Canary 1")
        alpha_v2 = deployment_unit("alpha", version="Canary 2")
        alpha_v3 = deployment_unit("alpha", version="Canary 3")
        beta = deployment_unit("beta")
        slot_a = candidate(alpha_v2, "FAIL")
        slot_a["replaces_accepted_deployment_id"] = alpha_v1["deployment_id"]
        state = runtime_state(
            accepted=[{"unit": alpha_v1, "accepted_at": TIME_1}],
            slot_a=slot_a,
            slot_b=candidate(beta, "PASS"),
        )
        prior_b = copy.deepcopy(state["slots"]["B"])
        updated = plan_transition(
            state,
            state["revision"],
            {"type": "UPDATE_SLOT", "slot": "A", "candidate": candidate_declaration(alpha_v3)},
            TIME_2,
            project_index("alpha", "beta"),
        )
        self.assertEqual(alpha_v1["deployment_id"], updated["slots"]["A"]["replaces_accepted_deployment_id"])
        self.assertEqual(prior_b, updated["slots"]["B"])
        self.assertEqual({"alpha", "beta"}, {unit["project_id"] for unit in resolve_profile(updated, project_index("alpha", "beta"))})

    def test_slot_dependency_override_is_temporary_and_requires_full_ownership_coverage(self) -> None:
        alpha_v1 = deployment_unit("alpha", version="Canary 1")
        alpha_v2 = deployment_unit("alpha", version="Canary 2")
        dependency_v1 = deployment_unit("dependency", version="Canary 1")
        dependency_v2 = deployment_unit("dependency", version="Canary 2")
        alpha_v2["artifacts"].append(copy.deepcopy(dependency_v2["artifacts"][0]))
        accepted = [
            {"unit": alpha_v1, "accepted_at": TIME_1},
            {"unit": dependency_v1, "accepted_at": TIME_1},
        ]
        assigned = plan_transition(
            runtime_state(accepted),
            3,
            {
                "type": "ASSIGN_SLOT",
                "candidate": candidate_declaration(
                    alpha_v2,
                    alpha_v1["deployment_id"],
                    [dependency_v1["deployment_id"]],
                ),
            },
            TIME_2,
            project_index("alpha", "dependency"),
        )
        self.assertEqual([dependency_v1["deployment_id"]], assigned["slots"]["A"]["dependency_overrides"])
        effective = resolve_profile(assigned, project_index("alpha", "dependency"))
        self.assertEqual(["alpha"], [unit["project_id"] for unit in effective])
        self.assertEqual(
            {"mod:alpha", "mod:dependency"},
            {key for artifact in effective[0]["artifacts"] for key in artifact["ownership_keys"]},
        )
        restored = plan_transition(
            assigned,
            assigned["revision"],
            {"type": "REMOVE_SLOT", "slot": "A"},
            TIME_3,
            project_index("alpha", "dependency"),
        )
        self.assertEqual(
            {"alpha", "dependency"},
            {unit["project_id"] for unit in resolve_profile(restored, project_index("alpha", "dependency"))},
        )

        alpha_v3 = deployment_unit("alpha", version="Canary 3")
        cleared_override = plan_transition(
            assigned,
            assigned["revision"],
            {
                "type": "UPDATE_SLOT",
                "slot": "A",
                "candidate": candidate_declaration(alpha_v3, dependency_overrides=[]),
            },
            TIME_3,
            project_index("alpha", "dependency"),
        )
        self.assertNotIn("dependency_overrides", cleared_override["slots"]["A"])
        self.assertEqual(
            {"alpha", "dependency"},
            {unit["project_id"] for unit in resolve_profile(cleared_override, project_index("alpha", "dependency"))},
        )

        incomplete = candidate(alpha_v2)
        incomplete["replaces_accepted_deployment_id"] = alpha_v1["deployment_id"]
        incomplete["dependency_overrides"] = [dependency_v1["deployment_id"]]
        incomplete["unit"]["artifacts"].pop()
        with self.assertRaisesRegex(ValidationError, "every ownership key"):
            validate_runtime_state(
                runtime_state(accepted, incomplete),
                project_index("alpha", "dependency"),
            )

    def test_one_unified_bge_artifact_covers_nibaru_override_and_collides_by_alias(self) -> None:
        bge_v1 = deployment_unit(
            "block-geometry-extensions",
            version="Canary 56",
            ownership="mod:cnm_terrain_slabs_compat",
        )
        nibaru_v1 = deployment_unit(
            "nibaru",
            version="Canary 46",
            ownership="mod:more_slabs_stairs_and_walls",
        )
        unified = deployment_unit(
            "block-geometry-extensions",
            version="Canary 57",
            ownership_keys=[
                "mod:cnm_terrain_slabs_compat",
                "mod:more_slabs_stairs_and_walls",
            ],
        )
        unified_slot = candidate(unified)
        unified_slot["replaces_accepted_deployment_id"] = bge_v1["deployment_id"]
        unified_slot["dependency_overrides"] = [nibaru_v1["deployment_id"]]
        accepted = [
            {"unit": bge_v1, "accepted_at": TIME_1},
            {"unit": nibaru_v1, "accepted_at": TIME_1},
        ]
        state = runtime_state(accepted, unified_slot)

        validate_runtime_state(
            state,
            project_index("block-geometry-extensions", "nibaru"),
        )
        effective = resolve_profile(
            state,
            project_index("block-geometry-extensions", "nibaru"),
        )
        self.assertEqual(["block-geometry-extensions"], [unit["project_id"] for unit in effective])
        self.assertEqual(1, len(effective[0]["artifacts"]))
        self.assertEqual(
            {
                "mod:cnm_terrain_slabs_compat",
                "mod:more_slabs_stairs_and_walls",
            },
            set(effective[0]["artifacts"][0]["ownership_keys"]),
        )

        conflicting = deployment_unit(
            "legacy-nibaru-owner",
            ownership="mod:more_slabs_stairs_and_walls",
        )
        state["slots"]["B"] = candidate(conflicting)
        with self.assertRaisesRegex(ValidationError, "ownership collision.*more_slabs_stairs_and_walls"):
            validate_runtime_state(
                state,
                project_index(
                    "block-geometry-extensions",
                    "nibaru",
                    "legacy-nibaru-owner",
                ),
            )

    def test_promotion_requires_pass_and_upgrade_replaces_baseline(self) -> None:
        alpha_v1 = deployment_unit("alpha", version="Canary 1")
        alpha_v2 = deployment_unit("alpha", version="Canary 2")
        accepted = [{"unit": alpha_v1, "accepted_at": TIME_1}]
        upgrade = candidate(alpha_v2, "PASS")
        upgrade["replaces_accepted_deployment_id"] = alpha_v1["deployment_id"]
        state = runtime_state(accepted, upgrade)
        validate_runtime_state(state, project_index("alpha"))
        self.assertEqual(["Canary 2"], [unit["version"] for unit in resolve_profile(state, project_index("alpha"))])
        promoted = plan_transition(state, state["revision"], {"type": "PROMOTE_SLOT", "slot": "A"}, TIME_2, project_index("alpha"))
        self.assertEqual(["Canary 2"], [member["unit"]["version"] for member in promoted["accepted_baseline"]["members"]])
        untested = runtime_state(slot_a=candidate(deployment_unit("beta")))
        with self.assertRaisesRegex(ValidationError, "requires an explicit"):
            plan_transition(untested, untested["revision"], {"type": "PROMOTE_SLOT", "slot": "A"}, TIME_2, project_index("beta"))

    def test_user_approved_untested_successor_promotion_replaces_baseline_and_clears_only_untested_slot(self) -> None:
        alpha_v1 = deployment_unit("alpha", version="Canary 1")
        alpha_v2 = deployment_unit("alpha", version="Canary 2")
        alpha_v3 = deployment_unit("alpha", version="Canary 3")
        accepted = [{"unit": alpha_v1, "accepted_at": TIME_1}]
        old_slot = candidate(alpha_v2)
        old_slot["replaces_accepted_deployment_id"] = alpha_v1["deployment_id"]
        state = runtime_state(accepted, old_slot)
        operation = {
            "type": "PROMOTE_UNTESTED_CANDIDATE",
            "authorization": "USER_APPROVED_UNTESTED_PROMOTION",
            "candidate": candidate_declaration(alpha_v3, alpha_v1["deployment_id"]),
        }
        promoted = plan_transition(
            state,
            state["revision"],
            operation,
            TIME_2,
            project_index("alpha"),
        )
        self.assertIsNone(promoted["slots"]["A"])
        self.assertEqual("Canary 3", promoted["accepted_baseline"]["members"][0]["unit"]["version"])
        self.assertEqual(
            state["accepted_baseline"]["revision"] + 1,
            promoted["accepted_baseline"]["revision"],
        )
        self.assertEqual(state["revision"] + 1, promoted["revision"])

        unauthorized = copy.deepcopy(operation)
        unauthorized["authorization"] = ""
        with self.assertRaisesRegex(ValidationError, "explicit USER_APPROVED"):
            plan_transition(
                state,
                state["revision"],
                unauthorized,
                TIME_2,
                project_index("alpha"),
            )

        passed_slot = candidate(alpha_v2, "PASS")
        passed_slot["replaces_accepted_deployment_id"] = alpha_v1["deployment_id"]
        with self.assertRaisesRegex(ValidationError, "cannot discard a recorded slot result"):
            plan_transition(
                runtime_state(accepted, passed_slot),
                3,
                operation,
                TIME_2,
                project_index("alpha"),
            )

        legacy_candidate = copy.deepcopy(operation)
        legacy_candidate["candidate"]["unit"]["project_identity_source"] = "FROZEN_LEGACY"
        legacy_candidate["candidate"]["unit"]["source_commit"] = None
        with self.assertRaisesRegex(ValidationError, "requires CURRENT_MANIFEST identity"):
            plan_transition(
                state,
                state["revision"],
                legacy_candidate,
                TIME_2,
                project_index("alpha"),
            )

    def test_repo_only_commit_path_is_always_disabled(self) -> None:
        alpha = deployment_unit("alpha")
        state = runtime_state()
        state["activation"] = "ACTIVE"
        state["accepted_baseline"]["provenance"]["physical_disposition"] = "ADOPTED"
        with self.assertRaisesRegex(ValidationError, "V2 physical manager"):
            commit_state(
                Path("runtime-state.json"),
                0,
                state_digest(state),
                {"type": "ASSIGN_SLOT", "candidate": candidate_declaration(alpha)},
                TIME_2,
                project_index("alpha"),
            )

    def test_tracked_runtime_state_declares_current_active_slot_contract(self) -> None:
        # Repository metadata is the expected-state contract only. Physical
        # proof requires the live Test Instance Manager verifier and cannot be
        # inferred by this bootstrap test.
        tracked = load_json(ROOT / "tools" / "test_instance_manager" / "runtime-state.json")
        self.assertEqual("ACTIVE", tracked["activation"])
        self.assertEqual(2, tracked["schema_version"])
        self.assertEqual(90, tracked["revision"])
        self.assertEqual("2026-09-04T23:23:00Z", tracked["updated_at"])
        self.assertEqual(
            "3cc3a9d83c34aa763263478fb7d54c1fb6aa1aee384ae48af3e815130f4a43b5",
            state_digest(tracked),
        )
        self.assertEqual(18, tracked["accepted_baseline"]["revision"])
        self.assertEqual(33, tracked["accepted_baseline"]["provenance"]["accepted_artifact_count"])
        self.assertEqual("TRANSITIONED", tracked["accepted_baseline"]["provenance"]["physical_disposition"])
        self.assertEqual(30, len(tracked["accepted_baseline"]["members"]))

        accepted_by_uuid = {
            member["unit"]["project_uuid"]: member
            for member in tracked["accepted_baseline"]["members"]
        }
        expected_user_passed = {
            "8ff207a2-9049-4519-ad3f-3100a80abcc2": (
                "0.1.1-canary2",
                "xaero-discovery-radius-0.1.1-canary2.jar",
                "a7d0125dbe13bbcd5418aac39e0e51a32ea79d508d9dc47f2ebd86526a794c99",
            ),
            "2371b6eb-a4fb-4f1e-8203-580b82ce846b": (
                "0.1.2-canary3",
                "coal-consolidation-0.1.2-canary3.jar",
                "f1d36dfad276927d527cc911d92f47ee979ef7600479f950c76ed25dd18eb871",
            ),
            "6b8b6166-fd59-4e4e-ab1d-d1140cdbfab8": (
                "0.1.0-canary1",
                "rooted-dirt-qol-0.1.0-canary1.jar",
                "3642f55e0cef611f2dd56478b8aaf2491fd81d370023a04ca71e6852b37307ee",
            ),
            "dda2c3b4-5099-4842-a755-29e964cd2192": (
                "0.2.0-canary2",
                "matcha-vanilla-village-restoration-0.2.0-canary2.jar",
                "5d5164a89881239400cd2b6776b9b54177b699e2a16a1c97b761cde06e7d8bcd",
            ),
            "850b2838-7f90-4685-8ba6-3ec282693003": (
                "0.1.0-canary2",
                "matcha-frost-protection-0.1.0-canary2.jar",
                "88ae708cbf2a9b1f5fe3b322579c3e7b507c7e0420cadb40aca796dba5b4a742",
            ),
            "2dc7b47a-f3b4-5fbe-a1fa-53e4f0aaa446": (
                "C3",
                "sweet-berry-horse-immunity-0.1.0-canary3.jar",
                "ab947253c5a6bc1910f05367ca9cc5ed7b5388f1d343b4889605d7b15a10060b",
            ),
            "4d983099-3bfc-5df1-9788-32683ddfc6db": (
                "C3 (legacy 0.2.0-nonrecipe-discovery-canary1)",
                "matcha-jei-integration-0.2.0-nonrecipe-discovery-canary1.jar",
                "1e4941e2353c1696505f243e86eb58f0239dd1144d544897370b021f6748a455",
            ),
        }
        for project_uuid, (version, filename, sha256) in expected_user_passed.items():
            member = accepted_by_uuid[project_uuid]
            self.assertEqual(version, member["unit"]["version"])
            self.assertEqual([(filename, sha256)], [
                (artifact["filename"], artifact["sha256"])
                for artifact in member["unit"]["artifacts"]
            ])

        authorized_untested_heart = accepted_by_uuid["937d7ccc-44c9-55cb-8d33-0dc0bff5fe45"]
        self.assertEqual("0.1.10-canary11", authorized_untested_heart["unit"]["version"])
        self.assertEqual(
            [
                (
                    "matcha-heart-death-compat-0.1.10-canary11.jar",
                    "a0570179f85d32ec6740d9136ad50890b9c797500661cd2ed2325da5b6b1e4a9",
                )
            ],
            [
                (artifact["filename"], artifact["sha256"])
                for artifact in authorized_untested_heart["unit"]["artifacts"]
            ],
        )

        rooted = accepted_by_uuid["6b8b6166-fd59-4e4e-ab1d-d1140cdbfab8"]
        self.assertEqual(1, len(rooted["retained_rollbacks"]))
        rooted_rollback = rooted["retained_rollbacks"][0]["unit"]
        self.assertEqual("FROZEN_LEGACY", rooted_rollback["project_identity_source"])
        self.assertEqual("107.1", rooted_rollback["version"])
        self.assertEqual(
            [("craft-rooted-dirt-107.1.jar", "a5fc656fc345b416fc01281146d568de664c587e2c168e6bfd69f3ded7e42845")],
            [
                (artifact["filename"], artifact["sha256"])
                for artifact in rooted_rollback["artifacts"]
            ],
        )

        bge = accepted_by_uuid["4b2342fc-7bdf-5ba6-9f37-d551109d214c"]
        self.assertEqual("128c78b7-a80d-4b3f-8605-6cdf31bbebf1", bge["unit"]["deployment_id"])
        self.assertEqual("4.2.2-bge.canary58.glass-corner-uv+26.2", bge["unit"]["version"])
        self.assertEqual(
            "d1d753a86b21467141bd39a0bcc1270f7827a8d7",
            bge["unit"]["source_commit"],
        )
        self.assertEqual(
            [
                (
                    "cnm-nibaru-integration-4.2.2-bge.canary58.glass-corner-uv+26.2.jar",
                    "1a4e4d1cd9c8709720ec84975e70caffb5552ac676537b9bbae42dca96567e87",
                ),
            ],
            [(artifact["filename"], artifact["sha256"]) for artifact in bge["unit"]["artifacts"]],
        )
        self.assertEqual(
            "7c990388-34b7-4225-8f14-a0b9b5bfae94",
            bge["unit"]["artifacts"][0]["artifact_id"],
        )
        self.assertEqual(
            ["mod:cnm_terrain_slabs_compat", "mod:more_slabs_stairs_and_walls"],
            bge["unit"]["artifacts"][0]["ownership_keys"],
        )
        self.assertNotIn("680476b9-5336-5422-a575-779f2efd1eff", accepted_by_uuid)

        trowel = accepted_by_uuid["e28154da-0649-5da7-b6d5-3bff2891719e"]
        self.assertEqual("3f15446c-95c2-405b-b1e5-e78de5f5f638", trowel["unit"]["deployment_id"])
        self.assertEqual("0.1.0-canary9", trowel["unit"]["version"])
        self.assertEqual(
            "844bccbb6f9efdfb36749c719d1fb396f71b626f",
            trowel["unit"]["source_commit"],
        )
        self.assertEqual(
            [
                (
                    "shulker-trowel-0.1.0-canary9-private.jar",
                    "b78679eaf6eaf6f7ff75a32ffae024e45515de3af38bf2ed92ac8727a5138df8",
                )
            ],
            [(artifact["filename"], artifact["sha256"]) for artifact in trowel["unit"]["artifacts"]],
        )
        self.assertEqual(
            "efbc5761-9899-4a3c-ae5d-7640b8a3022b",
            trowel["unit"]["artifacts"][0]["artifact_id"],
        )
        self.assertEqual(1, len(trowel["retained_rollbacks"]))
        trowel_rollback = trowel["retained_rollbacks"][0]["unit"]
        self.assertEqual("6c8c4d94-bce9-479a-916c-66b94eecc852", trowel_rollback["deployment_id"])
        self.assertEqual("FROZEN_LEGACY", trowel_rollback["project_identity_source"])
        self.assertEqual("0.1.0-canary4", trowel_rollback["version"])

        resolved_uuids = [unit["project_uuid"] for unit in resolve_profile(tracked)]
        self.assertIn("4b2342fc-7bdf-5ba6-9f37-d551109d214c", resolved_uuids)
        self.assertNotIn("680476b9-5336-5422-a575-779f2efd1eff", resolved_uuids)

        csr = accepted_by_uuid["3ab36584-8732-554f-840e-28a75c422660"]
        self.assertEqual("8211900d-913c-40f2-9829-68509e0d71fe", csr["unit"]["deployment_id"])
        self.assertEqual("3ab36584-8732-554f-840e-28a75c422660", csr["unit"]["project_uuid"])
        self.assertEqual("container-slot-reservations", csr["unit"]["project_id"])
        self.assertEqual("0.1.0-canary4", csr["unit"]["version"])
        self.assertEqual("d09c3f7153d4e53a36f41fb1ea80d85b4cb2e28f", csr["unit"]["source_commit"])
        self.assertEqual(
            (
                "cc469fa7-e2a5-417e-8e5c-978355f052f3",
                "container-slot-reservations-0.1.0-canary4.jar",
                "006f2c01e3502bd19d67e26a53979ccc65d53dde08aa07313094017ca170df65",
                ["mod:container_slot_reservations"],
            ),
            (
                csr["unit"]["artifacts"][0]["artifact_id"],
                csr["unit"]["artifacts"][0]["filename"],
                csr["unit"]["artifacts"][0]["sha256"],
                csr["unit"]["artifacts"][0]["ownership_keys"],
            ),
        )
        self.assertEqual("2026-09-04T05:22:28Z", csr["accepted_at"])

        accepted_qsn = accepted_by_uuid["ffa96cc6-2989-5465-b158-1659a484366b"]
        self.assertEqual(
            "30a3d125-ecb6-4a7a-862b-ca2e2c1bbcfe",
            accepted_qsn["unit"]["deployment_id"],
        )
        self.assertEqual("0.1.0-canary8", accepted_qsn["unit"]["version"])
        self.assertEqual(
            "1f545721658cce803200fa277737052cead79b70",
            accepted_qsn["unit"]["source_commit"],
        )
        self.assertEqual(
            [
                (
                    "cb31d144-b6c4-41fb-ba44-35d896b228f6",
                    "quick-stack-nearby-0.4.0.jar",
                    "43f1130527f782a291231c682791b4fd3766a20916c691cbdb98f91fdcc47e53",
                ),
                (
                    "62f897e7-dc2e-4754-82f0-43240d4655be",
                    "quick-stack-nearby-compat-0.1.0-canary8.jar",
                    "e6aaf43d881f31202931c40b6f40762f2e2dab1f2ae8d0ce5fd7f213ec0cbba5",
                ),
            ],
            [
                (
                    artifact["artifact_id"],
                    artifact["filename"],
                    artifact["sha256"],
                )
                for artifact in accepted_qsn["unit"]["artifacts"]
            ],
        )
        self.assertEqual("2026-09-04T05:23:27Z", accepted_qsn["accepted_at"])

        slot_a = tracked["slots"]["A"]
        self.assertEqual(1, len(slot_a["members"]))
        csr_successor = slot_a["members"][0]
        self.assertEqual(
            "2764b293-5c8a-41c2-a4a5-66b2913b1642",
            csr_successor["unit"]["deployment_id"],
        )
        self.assertEqual("3ab36584-8732-554f-840e-28a75c422660", csr_successor["unit"]["project_uuid"])
        self.assertEqual("container-slot-reservations", csr_successor["unit"]["project_id"])
        self.assertEqual("0.1.0-canary7", csr_successor["unit"]["version"])
        self.assertEqual(
            "c373b663c99d7a217fc9147db5007d3f1057678f",
            csr_successor["unit"]["source_commit"],
        )
        self.assertEqual(
            (
                "9cbbc908-966c-4cd5-99d7-7244de6bf6d9",
                "container-slot-reservations-0.1.0-canary7.jar",
                "fafbdbc4b5f3c470f291183baa6fd161ec56604ad6a06fd64bf59556d037e239",
                ["mod:container_slot_reservations"],
            ),
            (
                csr_successor["unit"]["artifacts"][0]["artifact_id"],
                csr_successor["unit"]["artifacts"][0]["filename"],
                csr_successor["unit"]["artifacts"][0]["sha256"],
                csr_successor["unit"]["artifacts"][0]["ownership_keys"],
            ),
        )
        self.assertEqual(
            "8211900d-913c-40f2-9829-68509e0d71fe",
            csr_successor["replaces_accepted_deployment_id"],
        )
        self.assertEqual(
            {"classification": "UNTESTED", "recorded_at": None, "evidence": {"passed": [], "failed": []}},
            csr_successor["runtime_result"],
        )
        self.assertEqual("READY_TO_TEST_VERIFIED", slot_a["deployment"]["state"])
        self.assertEqual("2026-09-04T23:23:00Z", slot_a["deployment"]["deployed_at"])
        self.assertEqual("2026-09-04T23:23:00Z", slot_a["deployment"]["ready_verified_at"])

        sas = accepted_by_uuid["58086966-05a1-4237-9f4f-ffca6c05da87"]
        self.assertEqual("66cd7696-3d01-4d92-805e-9221c1b93e07", sas["unit"]["deployment_id"])
        self.assertEqual("58086966-05a1-4237-9f4f-ffca6c05da87", sas["unit"]["project_uuid"])
        self.assertEqual("stacks-are-stacks-container-fixes", sas["unit"]["project_id"])
        self.assertEqual("0.1.0-canary2", sas["unit"]["version"])
        self.assertEqual("382cb276455ffe74efc100ce65e853b499204b56", sas["unit"]["source_commit"])
        self.assertEqual(
            (
                "01661d77-c5d5-4838-8485-1cb4309265ce",
                "stacks-are-stacks-container-fixes-0.1.0-canary2.jar",
                "7c294ce614ccc6f889dde7db5b5bd474b74dc8b6a6c8e62faefd44e5811896f2",
                ["mod:stacksarestacks_container_fixes"],
            ),
            (
                sas["unit"]["artifacts"][0]["artifact_id"],
                sas["unit"]["artifacts"][0]["filename"],
                sas["unit"]["artifacts"][0]["sha256"],
                sas["unit"]["artifacts"][0]["ownership_keys"],
            ),
        )
        self.assertEqual("2026-09-04T20:16:38Z", sas["accepted_at"])

        slot_b = tracked["slots"]["B"]
        self.assertEqual(1, len(slot_b["members"]))
        ccar_successor = slot_b["members"][0]
        self.assertEqual("98fe1cc9-eeaa-4c07-8b72-17678bc07581", ccar_successor["unit"]["deployment_id"])
        self.assertEqual("fcb7d036-c756-5114-abe1-01c4331e7ea8", ccar_successor["unit"]["project_uuid"])
        self.assertEqual("0.3.7-csr-reservation-affinity-canary1", ccar_successor["unit"]["version"])
        self.assertEqual("c373b663c99d7a217fc9147db5007d3f1057678f", ccar_successor["unit"]["source_commit"])
        self.assertEqual("1b6d00e9-7fc4-45e3-83a5-f632636e2f26", ccar_successor["replaces_accepted_deployment_id"])
        self.assertEqual(
            ("2eb193ee-0975-4a2c-a725-c97a721c35b4",
             "carried-container-auto-routing-0.3.7-csr-reservation-affinity-canary1.jar",
             "f00b1e6bc64a0e63ac1199d89c08f7510a20d5bbb9a486b40bd97833df522a2f"),
            tuple(ccar_successor["unit"]["artifacts"][0][key] for key in ("artifact_id", "filename", "sha256")),
        )
        self.assertEqual(
            {"classification": "UNTESTED", "recorded_at": None, "evidence": {"passed": [], "failed": []}},
            ccar_successor["runtime_result"],
        )
        self.assertEqual(slot_a["deployment"], slot_b["deployment"])
        ccar_manifest = load_json(ROOT / "projects" / "carried-container-auto-routing" / "WORKBENCH_STATUS.json")
        self.assertEqual("TESTING", ccar_manifest["definition"]["lifecycle"])
        self.assertEqual("RUNTIME_UNTESTED", ccar_manifest["state"]["validation"]["runtime"])
        self.assertEqual("CURRENT_RELEASE_DEPLOYED", current_release_deployment_comparison(ccar_manifest, tracked))


        csr_manifest = load_json(ROOT / "projects" / "container-slot-reservations" / "WORKBENCH_STATUS.json")
        qsn_manifest = load_json(ROOT / "projects" / "quick-stack-nearby-compat" / "WORKBENCH_STATUS.json")
        sas_manifest = load_json(ROOT / "projects" / "stacks-are-stacks-container-fixes" / "WORKBENCH_STATUS.json")
        slab_manifest = load_json(ROOT / "projects" / "slab-decorations" / "WORKBENCH_STATUS.json")
        self.assertEqual("TESTING", csr_manifest["definition"]["lifecycle"])
        self.assertEqual("ACCEPTED", qsn_manifest["definition"]["lifecycle"])
        self.assertEqual("ACCEPTED", sas_manifest["definition"]["lifecycle"])
        for manifest in (csr_manifest, sas_manifest):
            self.assertEqual(
                "READY_TO_TEST_VERIFIED",
                manifest["state"]["validation"]["deployment"],
            )
        self.assertEqual(
            "CURRENT_RELEASE_DEPLOYED",
            current_release_deployment_comparison(csr_manifest, tracked),
        )
        self.assertEqual(
            "CURRENT_RELEASE_NOT_DEPLOYED",
            current_release_deployment_comparison(qsn_manifest, tracked),
        )
        self.assertEqual(
            "CURRENT_RELEASE_NOT_DEPLOYED",
            current_release_deployment_comparison(sas_manifest, tracked),
        )
        self.assertEqual("RUNTIME_UNTESTED", csr_manifest["state"]["validation"]["runtime"])
        self.assertEqual("RUNTIME_PASS", qsn_manifest["state"]["validation"]["runtime"])
        self.assertEqual("RUNTIME_PASS", sas_manifest["state"]["validation"]["runtime"])
        self.assertEqual("ACTIVE", slab_manifest["definition"]["lifecycle"])
        self.assertEqual("NOT_DEPLOYED", slab_manifest["state"]["validation"]["deployment"])
        self.assertEqual("RUNTIME_UNTESTED", slab_manifest["state"]["validation"]["runtime"])
        self.assertEqual(
            "CURRENT_RELEASE_NOT_DEPLOYED",
            current_release_deployment_comparison(slab_manifest, tracked),
        )

        title_state = render_title_state(
            tracked,
            {
                csr_manifest["identity"]["uuid"]: csr_manifest["identity"]["name"],
                ccar_manifest["identity"]["uuid"]: ccar_manifest["identity"]["name"],
                qsn_manifest["identity"]["uuid"]: qsn_manifest["identity"]["name"],
                sas_manifest["identity"]["uuid"]: sas_manifest["identity"]["name"],
            },
        )
        self.assertEqual(
            [
                "Baseline: Stack v18",
                "Slot A: Container Slot Reservations - Canary 7",
                "Slot B: Carried Container Auto-Routing - Canary 1",
            ],
            title_state["lines"],
        )


class CurrentStateBootstrapTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.config = load_json(ROOT / "tools" / "sheet_sync" / "publication.json")

    def prepare_plan(
        self,
        manifests: dict[str, dict],
        commit: str = "d" * 40,
        root: Path = ROOT,
    ) -> dict:
        with (
            patch("tools.sheet_sync._git", return_value=commit),
            patch("tools.sheet_sync.validate_repository"),
            patch("tools.sheet_sync._manifests_at", return_value=manifests),
        ):
            return make_current_state_plan(
                root,
                commit,
                self.config["repository"],
                self.config["authoritative_ref"],
                self.config,
            )

    def test_bootstrap_includes_all_participating_manifests_in_both_collections(self) -> None:
        manifests = {
            "resourcepacks/amber-ui/WORKBENCH_STATUS.json": sheet_manifest(
                "amber-ui", "Amber UI", revision=12, collection="resourcepacks"
            ),
            "projects/alpha/WORKBENCH_STATUS.json": sheet_manifest("alpha", "Alpha", revision=5),
        }
        plan = self.prepare_plan(manifests)
        self.assertEqual("current_state_bootstrap", plan["plan_kind"])
        self.assertEqual(
            {
                "repository": self.config["repository"],
                "ref": self.config["authoritative_ref"],
                "commit": "d" * 40,
            },
            plan["source"],
        )
        self.assertEqual(sorted(manifests), [event["source"]["manifest_path"] for event in plan["events"]])

    def test_bootstrap_excludes_nonparticipating_manifests(self) -> None:
        participating_path = "projects/alpha/WORKBENCH_STATUS.json"
        excluded_path = "resourcepacks/private-notes/WORKBENCH_STATUS.json"
        manifests = {
            participating_path: sheet_manifest("alpha", "Alpha", revision=5),
            excluded_path: sheet_manifest(
                "private-notes",
                "Private Notes",
                revision=9,
                participates=False,
                collection="resourcepacks",
            ),
        }
        plan = self.prepare_plan(manifests)
        self.assertEqual([participating_path], [event["source"]["manifest_path"] for event in plan["events"]])

    def test_bootstrap_preserves_exact_current_revisions(self) -> None:
        manifests = {
            "projects/alpha/WORKBENCH_STATUS.json": sheet_manifest("alpha", "Alpha", revision=5),
            "projects/beta/WORKBENCH_STATUS.json": sheet_manifest("beta", "Beta", revision=6),
            "resourcepacks/amber-ui/WORKBENCH_STATUS.json": sheet_manifest(
                "amber-ui", "Amber UI", revision=12, collection="resourcepacks"
            ),
        }
        plan = self.prepare_plan(manifests)
        revisions = {
            event["source"]["manifest_path"]: event["record"]["revision"]
            for event in plan["events"]
        }
        self.assertEqual({path: manifest["synchronization"]["revision"] for path, manifest in manifests.items()}, revisions)
        self.assertTrue(all(event["record"]["publication_commit"] == "d" * 40 for event in plan["events"]))

    def test_current_heart_r8_and_mossy_r6_authority_produces_exact_reconciliation_events(self) -> None:
        paths = (
            "projects/matcha-heart-death-compat/WORKBENCH_STATUS.json",
            "projects/mossy-stone/WORKBENCH_STATUS.json",
        )
        manifest_paths = [ROOT / path for path in paths]
        before = {path: path.read_bytes() for path in manifest_paths}
        manifests = {relative: load_json(path) for relative, path in zip(paths, manifest_paths, strict=True)}

        plan = self.prepare_plan(manifests)

        events = {event["record"]["project_uuid"]: event for event in plan["events"]}
        expected = {
            "937d7ccc-44c9-55cb-8d33-0dc0bff5fe45": {
                "revision": 8,
                "filename": "matcha-heart-death-compat-0.1.10-canary11.jar",
                "sha256": "a0570179f85d32ec6740d9136ad50890b9c797500661cd2ed2325da5b6b1e4a9",
            },
            "9f1c5aa4-09c1-4de3-9921-4b045e8abcd2": {
                "revision": 6,
                "filename": "mossy-stone-0.4.0-canary4.jar",
                "sha256": "b0e7be5651d622789b848ba1a48073af8078ba4d834e34c1255ec9ce72042f14",
            },
        }
        self.assertEqual(set(expected), set(events))
        for project_uuid, authority in expected.items():
            with self.subTest(project_uuid=project_uuid):
                record = events[project_uuid]["record"]
                self.assertEqual(authority["revision"], record["revision"])
                self.assertEqual(authority["filename"], record["current_artifact_filename"])
                self.assertEqual(authority["sha256"], record["current_artifact_sha256"])
        self.assertEqual(before, {path: path.read_bytes() for path in manifest_paths})

    def test_bootstrap_does_not_mutate_project_manifests(self) -> None:
        manifests = {
            "projects/alpha/WORKBENCH_STATUS.json": sheet_manifest("alpha", "Alpha", revision=5),
            "resourcepacks/amber-ui/WORKBENCH_STATUS.json": sheet_manifest(
                "amber-ui", "Amber UI", revision=12, collection="resourcepacks"
            ),
        }
        before_objects = copy.deepcopy(manifests)
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            manifest_bytes: dict[Path, bytes] = {}
            for relative, manifest in manifests.items():
                path = root / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
                manifest_bytes[path] = path.read_bytes()
            self.prepare_plan(manifests, root=root)
            self.assertEqual(manifest_bytes, {path: path.read_bytes() for path in manifest_bytes})
        self.assertEqual(before_objects, manifests)

    def test_bootstrap_refuses_non_authoritative_repository_and_ref(self) -> None:
        arguments = (
            ("Elsewhere/Mynx-Flavoured-Workbench", self.config["authoritative_ref"]),
            (self.config["repository"], "refs/heads/codex/feature"),
        )
        for repository, ref in arguments:
            with self.subTest(repository=repository, ref=ref):
                with self.assertRaisesRegex(ValidationError, "configured|authoritative|main|repository|ref"):
                    make_current_state_plan(ROOT, "d" * 40, repository, ref, self.config)

    def test_bootstrap_refuses_a_commit_other_than_checked_out_main(self) -> None:
        with patch("tools.sheet_sync._git", return_value="e" * 40):
            with self.assertRaisesRegex(ValidationError, "HEAD|main|commit"):
                make_current_state_plan(
                    ROOT,
                    "d" * 40,
                    self.config["repository"],
                    self.config["authoritative_ref"],
                    self.config,
                )

    def test_bootstrap_refuses_head_when_authoritative_main_has_advanced(self) -> None:
        with patch("tools.sheet_sync._git", side_effect=["d" * 40, "e" * 40]):
            with self.assertRaisesRegex(ValidationError, "authoritative ref"):
                make_current_state_plan(
                    ROOT,
                    "d" * 40,
                    self.config["repository"],
                    self.config["authoritative_ref"],
                    self.config,
                )

    def test_bootstrap_validates_repository_before_manifest_enumeration(self) -> None:
        order: list[str] = []

        def validate(*_args) -> None:
            order.append("validate")

        def enumerate_manifests(*_args) -> dict:
            order.append("enumerate")
            return {}

        with (
            patch("tools.sheet_sync._git", return_value="d" * 40),
            patch("tools.sheet_sync.validate_repository", side_effect=validate),
            patch("tools.sheet_sync._manifests_at", side_effect=enumerate_manifests),
        ):
            make_current_state_plan(
                ROOT,
                "d" * 40,
                self.config["repository"],
                self.config["authoritative_ref"],
                self.config,
            )
        self.assertEqual(["validate", "enumerate"], order)

    def test_bootstrap_uses_ordinary_signed_envelopes_and_replays_deterministically(self) -> None:
        manifests = {
            "projects/alpha/WORKBENCH_STATUS.json": sheet_manifest("alpha", "Alpha", revision=5),
            "resourcepacks/amber-ui/WORKBENCH_STATUS.json": sheet_manifest(
                "amber-ui", "Amber UI", revision=12, collection="resourcepacks"
            ),
        }
        first = self.prepare_plan(manifests)
        second = self.prepare_plan(copy.deepcopy(manifests))
        self.assertEqual(first, second)
        self.assertEqual(
            [event["event_id"] for event in first["events"]],
            [event["event_id"] for event in second["events"]],
        )
        for event in first["events"]:
            with self.subTest(path=event["source"]["manifest_path"]):
                self.assertEqual("project_status_upsert", event["operation"])
                self.assertEqual(
                    {"contract_version", "event_id", "operation", "source", "record", "ownership"},
                    set(event),
                )
                self.assertEqual({"sheet_preserves": ["Notes"]}, event["ownership"])
                record_keys = {key.casefold() for key in event["record"]}
                self.assertNotIn("notes", record_keys)
                self.assertNotIn("priority", record_keys)
                secret = "s" * 32
                wrapper = json.loads(signed_wrapper(event, secret))
                padded = wrapper["payload"] + "=" * (-len(wrapper["payload"]) % 4)
                self.assertEqual(event, json.loads(base64.urlsafe_b64decode(padded).decode("utf-8")))
                expected = hmac.new(secret.encode(), wrapper["payload"].encode("ascii"), hashlib.sha256).hexdigest()
                self.assertEqual("sha256=" + expected, wrapper["signature"])


class SheetPublisherTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.config = load_json(ROOT / "tools" / "sheet_sync" / "publication.json")

    def test_feature_branch_cannot_prepare_authoritative_events(self) -> None:
        manifest = planned_manifest()
        with self.assertRaisesRegex(ValidationError, "ref"):
            build_events(
                {},
                {"projects/mossy-stone/WORKBENCH_STATUS.json": manifest},
                repository=self.config["repository"],
                ref="refs/heads/codex/feature",
                publication_commit="c" * 40,
                config=self.config,
            )

    def test_complete_project_directory_deletion_satisfies_push_contract(self) -> None:
        before = "b" * 40
        after = "c" * 40
        project_root = "projects/retired"
        status_path = f"{project_root}/WORKBENCH_STATUS.json"
        log_path = f"{project_root}/CODEX_LOG.md"
        testing_path = f"{project_root}/TESTING.md"
        manifest = planned_manifest("retired", "Retired")
        previous = {
            status_path: json.dumps(manifest),
            log_path: codex_log(manifest),
            testing_path: "# Testing\n",
        }

        def git_text(_root: Path, commit: str, path: str) -> str | None:
            return previous.get(path) if commit == before else None

        with (
            patch("tools.sheet_sync._changed_paths", return_value=list(previous)),
            patch("tools.sheet_sync._migration_adoption_uuids_at", return_value=frozenset()),
            patch("tools.sheet_sync._git_text", side_effect=git_text),
            patch("tools.sheet_sync._tracked_paths_under", return_value=[]),
        ):
            validate_project_push_contract(ROOT, before, after)

    def test_project_deletion_rejects_partial_controls_or_tracked_remnants(self) -> None:
        before = "b" * 40
        after = "c" * 40
        project_root = "projects/retired"
        status_path = f"{project_root}/WORKBENCH_STATUS.json"
        log_path = f"{project_root}/CODEX_LOG.md"
        testing_path = f"{project_root}/TESTING.md"
        manifest = planned_manifest("retired", "Retired")
        previous = {
            status_path: json.dumps(manifest),
            log_path: codex_log(manifest),
            testing_path: "# Testing\n",
        }

        def partial_git_text(_root: Path, commit: str, path: str) -> str | None:
            if commit == before:
                return previous.get(path)
            return None if path == status_path else previous.get(path)

        common_patches = (
            patch("tools.sheet_sync._changed_paths", return_value=[status_path]),
            patch("tools.sheet_sync._migration_adoption_uuids_at", return_value=frozenset()),
        )
        with common_patches[0], common_patches[1], patch("tools.sheet_sync._git_text", side_effect=partial_git_text):
            with self.assertRaisesRegex(ValidationError, "all three project control files"):
                validate_project_push_contract(ROOT, before, after)

        def deleted_git_text(_root: Path, commit: str, path: str) -> str | None:
            return previous.get(path) if commit == before else None

        with (
            patch("tools.sheet_sync._changed_paths", return_value=list(previous)),
            patch("tools.sheet_sync._migration_adoption_uuids_at", return_value=frozenset()),
            patch("tools.sheet_sync._git_text", side_effect=deleted_git_text),
            patch("tools.sheet_sync._tracked_paths_under", return_value=[f"{project_root}/src/Remaining.java"]),
        ):
            with self.assertRaisesRegex(ValidationError, "entire project directory"):
                validate_project_push_contract(ROOT, before, after)

    def test_deleted_manifest_emits_no_event_while_survivor_publishes(self) -> None:
        retired_path = "projects/retired/WORKBENCH_STATUS.json"
        survivor_path = "projects/survivor/WORKBENCH_STATUS.json"
        retired = planned_manifest("retired", "Retired")
        survivor = planned_manifest("survivor", "Survivor")
        advanced_survivor = advance_manifest(survivor)
        events = build_events(
            {retired_path: retired, survivor_path: survivor},
            {survivor_path: advanced_survivor},
            repository=self.config["repository"],
            ref=self.config["authoritative_ref"],
            publication_commit="c" * 40,
            config=self.config,
        )
        self.assertEqual([survivor_path], [event["source"]["manifest_path"] for event in events])
        self.assertEqual([2], [event["record"]["revision"] for event in events])
        self.assertEqual(
            [],
            build_events(
                {retired_path: retired},
                {},
                repository=self.config["repository"],
                ref=self.config["authoritative_ref"],
                publication_commit="c" * 40,
                config=self.config,
            ),
        )

    def test_deleted_manifest_does_not_bypass_survivor_revision_gap(self) -> None:
        retired_path = "projects/retired/WORKBENCH_STATUS.json"
        survivor_path = "projects/survivor/WORKBENCH_STATUS.json"
        retired = planned_manifest("retired", "Retired")
        survivor = planned_manifest("survivor", "Survivor")
        skipped_survivor = advance_manifest(survivor)
        skipped_survivor["synchronization"]["revision"] = 3
        with self.assertRaisesRegex(ValidationError, "must advance exactly once"):
            build_events(
                {retired_path: retired, survivor_path: survivor},
                {survivor_path: skipped_survivor},
                repository=self.config["repository"],
                ref=self.config["authoritative_ref"],
                publication_commit="c" * 40,
                config=self.config,
            )

    def test_deleted_manifest_uuid_cannot_reappear_at_a_new_path(self) -> None:
        retired_path = "projects/retired/WORKBENCH_STATUS.json"
        replacement_path = "projects/replacement/WORKBENCH_STATUS.json"
        retired = planned_manifest("retired", "Retired")
        replacement = planned_manifest(
            "replacement",
            "Replacement",
            uuid_value=retired["identity"]["uuid"],
        )
        with self.assertRaisesRegex(ValidationError, "project UUID cannot move"):
            build_events(
                {retired_path: retired},
                {replacement_path: replacement},
                repository=self.config["repository"],
                ref=self.config["authoritative_ref"],
                publication_commit="c" * 40,
                config=self.config,
            )

    def test_metadata_only_revision_still_produces_distinct_event(self) -> None:
        path = "projects/mossy-stone/WORKBENCH_STATUS.json"
        first = planned_manifest()
        second = advance_manifest(first)
        events_1 = build_events(
            {}, {path: first}, repository=self.config["repository"], ref=self.config["authoritative_ref"], publication_commit="c" * 40, config=self.config
        )
        events_2 = build_events(
            {path: first}, {path: second}, repository=self.config["repository"], ref=self.config["authoritative_ref"], publication_commit="d" * 40, config=self.config
        )
        self.assertEqual(1, len(events_2))
        self.assertNotEqual(events_1[0]["event_id"], events_2[0]["event_id"])
        self.assertEqual(first["state"], second["state"])

    def test_revision_one_artifact_requires_runtime_dependency_policy_attestation(self) -> None:
        path = "projects/alpha/WORKBENCH_STATUS.json"
        manifest = planned_manifest("alpha", "Alpha")
        manifest["definition"]["lifecycle"] = "ACTIVE"
        manifest["state"]["releases"]["current"] = {
            "version": "0.1.0-canary1",
            "artifact": {"filename": "alpha-c1.jar", "sha256": "1" * 64},
            "source_commit": "c" * 40,
        }
        with self.assertRaisesRegex(ValidationError, "revision-1 current artifact.*runtime_dependency_policy"):
            build_events(
                {},
                {path: manifest},
                repository=self.config["repository"],
                ref=self.config["authoritative_ref"],
                publication_commit="d" * 40,
                config=self.config,
            )

        manifest["state"]["releases"]["current"]["runtime_dependency_policy"] = {
            "contract": "CAPABILITY_OR_PROVIDER",
            "exceptions": [],
        }
        events = build_events(
            {},
            {path: manifest},
            repository=self.config["repository"],
            ref=self.config["authoritative_ref"],
            publication_commit="d" * 40,
            config=self.config,
        )
        self.assertEqual([1], [event["record"]["revision"] for event in events])

    def test_only_frozen_projects_may_adopt_a_preserved_initial_revision(self) -> None:
        path = "projects/nibaru/WORKBENCH_STATUS.json"
        frozen_uuid = "680476b9-5336-5422-a575-779f2efd1eff"
        manifest = planned_manifest("nibaru", "Nibaru 26.2 Port", uuid_value=frozen_uuid)
        manifest["synchronization"]["revision"] = 5
        with self.assertRaisesRegex(ValidationError, "revision 1"):
            build_events(
                {}, {path: manifest}, repository=self.config["repository"], ref=self.config["authoritative_ref"], publication_commit="c" * 40, config=self.config
            )
        bge_path = "projects/block-geometry-extensions/WORKBENCH_STATUS.json"
        bge_uuid = "4b2342fc-7bdf-5ba6-9f37-d551109d214c"
        bge = planned_manifest("block-geometry-extensions", "Block Geometry Extensions (BGE)", uuid_value=bge_uuid)
        bge["synchronization"]["revision"] = 6
        events = build_events(
            {},
            {path: manifest, bge_path: bge},
            repository=self.config["repository"],
            ref=self.config["authoritative_ref"],
            publication_commit="c" * 40,
            config=self.config,
            migration_adoption_uuids=frozenset({frozen_uuid, bge_uuid}),
        )
        self.assertEqual([6, 5], [event["record"]["revision"] for event in events])

        unknown = planned_manifest("unknown", "Unknown")
        unknown["synchronization"]["revision"] = 5
        with self.assertRaisesRegex(ValidationError, "revision 1"):
            build_events(
                {},
                {"projects/unknown/WORKBENCH_STATUS.json": unknown},
                repository=self.config["repository"],
                ref=self.config["authoritative_ref"],
                publication_commit="c" * 40,
                config=self.config,
                migration_adoption_uuids=frozenset({frozen_uuid}),
            )

    def test_migration_freeze_parser_is_exact_and_rejects_duplicates(self) -> None:
        freeze_text = (ROOT / "MIGRATION_FREEZE.md").read_text(encoding="utf-8")
        uuids = migration_adoption_uuids(freeze_text)
        self.assertEqual(
            {
                "680476b9-5336-5422-a575-779f2efd1eff",
                "4b2342fc-7bdf-5ba6-9f37-d551109d214c",
                "c92ad4fe-c210-46c4-ba1d-59828d2bcbcd",
                "2dc7b47a-f3b4-5fbe-a1fa-53e4f0aaa446",
                "5d42f47f-b006-4125-840d-dec0d2728afa",
            },
            uuids,
        )
        duplicate = freeze_text.replace(
            "| Block Geometry Extensions |",
            "| Duplicate Nibaru | `projects/nibaru-copy` | `680476b9-5336-5422-a575-779f2efd1eff` | `codex/example` | `1111111111111111111111111111111111111111` |\n| Block Geometry Extensions |",
        )
        with self.assertRaisesRegex(ValidationError, "duplicate project UUID"):
            migration_adoption_uuids(duplicate)

    def test_migration_allowlist_uses_after_only_for_zero_before(self) -> None:
        freeze_text = (ROOT / "MIGRATION_FREEZE.md").read_text(encoding="utf-8")
        with patch("tools.sheet_sync._git_text", return_value=freeze_text) as read:
            self.assertIn("680476b9-5336-5422-a575-779f2efd1eff", _migration_adoption_uuids_at(ROOT, ZERO_COMMIT, "a" * 40))
            read.assert_called_once_with(ROOT, "a" * 40, "MIGRATION_FREEZE.md")
        with patch("tools.sheet_sync._git_text", return_value=freeze_text) as read:
            _migration_adoption_uuids_at(ROOT, "b" * 40, "a" * 40)
            read.assert_called_once_with(ROOT, "b" * 40, "MIGRATION_FREEZE.md")

    def test_existing_migration_freeze_is_immutable(self) -> None:
        with patch("tools.sheet_sync._changed_paths", return_value=["MIGRATION_FREEZE.md"]):
            with self.assertRaisesRegex(ValidationError, "immutable"):
                validate_project_push_contract(ROOT, "b" * 40, "a" * 40)

    def test_new_explicitly_excluded_project_is_not_published(self) -> None:
        manifest = planned_manifest()
        manifest["synchronization"]["google_sheet"] = {"participates": False, "exclusion_reason": "No human tracking value."}
        events = build_events(
            {},
            {"projects/mossy-stone/WORKBENCH_STATUS.json": manifest},
            repository=self.config["repository"],
            ref=self.config["authoritative_ref"],
            publication_commit="c" * 40,
            config=self.config,
        )
        self.assertEqual([], events)

    def test_tracked_activation_preserves_notes_and_omits_priority(self) -> None:
        self.assertTrue(self.config["enabled"])
        self.assertEqual(["Notes"], HUMAN_FIELDS)
        self.assertEqual(HUMAN_FIELDS, self.config["sheet_preserves"])
        record = flatten_manifest(planned_manifest(), "c" * 40)
        record_keys = {key.casefold() for key in record}
        self.assertTrue(all(field.casefold() not in record_keys for field in HUMAN_FIELDS))
        self.assertNotIn("priority", record_keys)
        path = "projects/mossy-stone/WORKBENCH_STATUS.json"
        events = build_events(
            {},
            {path: planned_manifest()},
            repository=self.config["repository"],
            ref=self.config["authoritative_ref"],
            publication_commit="c" * 40,
            config=self.config,
        )
        self.assertEqual({"sheet_preserves": ["Notes"]}, events[0]["ownership"])

    def test_receiver_lock_wait_budget_has_material_transport_headroom(self) -> None:
        receiver_source = (ROOT / "tools" / "sheet_sync" / "receiver" / "Code.gs").read_text(encoding="utf-8")
        match = re.search(r"var RECEIVER_LOCK_WAIT_MILLISECONDS = (\d+);", receiver_source)
        self.assertIsNotNone(match)
        lock_wait_milliseconds = int(match.group(1))
        transport_timeout_milliseconds = RECEIVER_RESPONSE_TIMEOUT_SECONDS * 1000
        self.assertLess(lock_wait_milliseconds, transport_timeout_milliseconds)
        self.assertGreaterEqual(transport_timeout_milliseconds - lock_wait_milliseconds, 30000)
        self.assertEqual(2, receiver_source.count("tryLock(RECEIVER_LOCK_WAIT_MILLISECONDS)"))
        self.assertNotIn("tryLock(30000)", receiver_source)

    def test_all_publication_gates_fail_before_transport(self) -> None:
        event = ordinary_publication_event(self.config)
        plan = incremental_plan(self.config, [event])
        enabled = copy.deepcopy(self.config)
        enabled["enabled"] = True
        disabled = copy.deepcopy(enabled)
        disabled["enabled"] = False
        valid_environment = publication_environment(enabled)
        missing_cutover = dict(valid_environment)
        missing_cutover.pop(enabled["cutover_environment_variable"])
        wrong_cutover = dict(valid_environment)
        wrong_cutover[enabled["cutover_environment_variable"]] = "not-authorized"
        missing_receiver = dict(valid_environment)
        missing_receiver.pop(enabled["receiver_url_environment_variable"])
        missing_secret = dict(valid_environment)
        missing_secret.pop(enabled["hmac_environment_variable"])
        short_secret = dict(valid_environment)
        short_secret[enabled["hmac_environment_variable"]] = "too-short"
        cases = (
            ("tracked disabled", disabled, valid_environment, "tracked-disabled"),
            ("cutover absent", enabled, missing_cutover, "cutover variable"),
            ("cutover wrong", enabled, wrong_cutover, "cutover variable"),
            ("receiver absent", enabled, missing_receiver, "receiver URL and HMAC secret"),
            ("secret absent", enabled, missing_secret, "receiver URL and HMAC secret"),
            ("secret too short", enabled, short_secret, "at least 32"),
        )
        for label, config, environment, error in cases:
            with self.subTest(gate=label), patch("urllib.request.urlopen") as transport:
                with self.assertRaisesRegex(ValidationError, error):
                    publish_plan(plan, config, environment)
                transport.assert_not_called()

    def test_http_200_receiver_rejection_is_not_counted_as_published(self) -> None:
        config = copy.deepcopy(self.config)
        config["enabled"] = True
        event = ordinary_publication_event(config)
        plan = incremental_plan(config, [event])
        environment = publication_environment(config)
        response = JsonResponse({"ok": False, "error": "write gate disabled"})
        failures = []
        with patch("urllib.request.urlopen", return_value=response):
            with self.assertRaisesRegex(ValidationError, "write gate disabled"):
                publish_plan(plan, config, environment, failure_logger=failures.append)
        self.assertEqual(
            [{
                "event_id": event["event_id"],
                "project_uuid": event["record"]["project_uuid"],
                "revision": event["record"]["revision"],
                "code": "rejected",
                "error": "write gate disabled",
            }],
            failures,
        )

    def test_first_failure_does_not_block_second_event_and_cli_fails_overall(self) -> None:
        config = copy.deepcopy(self.config)
        config["enabled"] = True
        events = ordinary_publication_events(config)
        plan = incremental_plan(config, events)
        environment = publication_environment(config)
        responses = [
            JsonResponse({"ok": False, "code": "conflict", "error": "same-revision conflict"}),
            JsonResponse({"ok": True, "changed": True, "event_id": events[1]["event_id"]}),
        ]
        output = io.StringIO()
        errors = io.StringIO()
        with tempfile.TemporaryDirectory() as temporary:
            plan_path = Path(temporary) / "publication-plan.json"
            plan_path.write_text(json.dumps(plan), encoding="utf-8")
            arguments = ["publish", "--root", str(ROOT), "--plan", str(plan_path)]
            with (
                patch("urllib.request.urlopen", side_effect=responses) as transport,
                patch.dict("tools.sheet_sync.os.environ", environment, clear=True),
                redirect_stdout(output),
                redirect_stderr(errors),
            ):
                self.assertEqual(1, sheet_sync_main(arguments))

        self.assertEqual(2, transport.call_count)
        self.assertEqual(events, [request_event(call.args[0]) for call in transport.call_args_list])
        failure = {
            "event_id": events[0]["event_id"],
            "project_uuid": events[0]["record"]["project_uuid"],
            "revision": events[0]["record"]["revision"],
            "code": "conflict",
            "error": "same-revision conflict",
        }
        acknowledgement = {
            "event_id": events[1]["event_id"],
            "project_uuid": events[1]["record"]["project_uuid"],
            "revision": events[1]["record"]["revision"],
            "changed": True,
        }
        self.assertEqual(
            [
                "Receiver failure: " + json.dumps(failure, sort_keys=True, separators=(",", ":")),
                "Receiver acknowledgement: " + json.dumps(acknowledgement, sort_keys=True, separators=(",", ":")),
            ],
            output.getvalue().splitlines(),
        )
        self.assertIn("Sheet publication failed for 1 of 2 event(s)", errors.getvalue())
        self.assertNotIn("Published 2 project status event(s).", output.getvalue())

    def test_transport_invalid_json_and_invalid_acknowledgement_failures_are_independent(self) -> None:
        config = copy.deepcopy(self.config)
        config["enabled"] = True
        events = ordinary_publication_events(config)
        plan = incremental_plan(config, events)
        environment = publication_environment(config)
        cases = (
            ("transport", OSError("network unavailable"), "transport_error"),
            ("truncated HTTP response", ReadFailureResponse({}), "transport_error"),
            ("invalid JSON", RawResponse(b"not-json"), "invalid_response"),
            (
                "invalid acknowledgement",
                JsonResponse({"ok": True, "changed": "yes", "event_id": events[0]["event_id"]}),
                "invalid_acknowledgement",
            ),
        )
        for label, first_result, expected_code in cases:
            with self.subTest(failure=label):
                acknowledgements = []
                failures = []
                responses = [
                    first_result,
                    JsonResponse({"ok": True, "changed": True, "event_id": events[1]["event_id"]}),
                ]
                with patch("urllib.request.urlopen", side_effect=responses) as transport:
                    with self.assertRaisesRegex(ValidationError, "1 of 2 event"):
                        publish_plan(
                            plan,
                            config,
                            environment,
                            acknowledgement_logger=acknowledgements.append,
                            failure_logger=failures.append,
                        )
                self.assertEqual(2, transport.call_count)
                self.assertEqual(events, [request_event(call.args[0]) for call in transport.call_args_list])
                self.assertEqual(events[0]["event_id"], failures[0]["event_id"])
                self.assertEqual(expected_code, failures[0]["code"])
                self.assertEqual(events[1]["event_id"], acknowledgements[0]["event_id"])

    def test_transport_timeout_is_ambiguous_not_retried_and_later_events_continue(self) -> None:
        config = copy.deepcopy(self.config)
        config["enabled"] = True
        events = ordinary_publication_events(config)
        plan = incremental_plan(config, events)
        environment = publication_environment(config)
        acknowledgements = []
        failures = []
        responses = [
            TimeoutError("receiver response deadline elapsed"),
            JsonResponse({"ok": True, "changed": True, "event_id": events[1]["event_id"]}),
        ]
        with (
            patch("urllib.request.urlopen", side_effect=responses) as transport,
            patch("tools.sheet_sync.time.sleep") as sleep,
        ):
            with self.assertRaisesRegex(ValidationError, "1 of 2 event"):
                publish_plan(
                    plan,
                    config,
                    environment,
                    acknowledgement_logger=acknowledgements.append,
                    failure_logger=failures.append,
                )
        self.assertEqual(2, transport.call_count)
        self.assertEqual(events, [request_event(call.args[0]) for call in transport.call_args_list])
        self.assertTrue(
            all(call.kwargs["timeout"] == RECEIVER_RESPONSE_TIMEOUT_SECONDS for call in transport.call_args_list)
        )
        self.assertEqual("transport_error", failures[0]["code"])
        self.assertEqual("Sheet receiver transport failed (TimeoutError)", failures[0]["error"])
        self.assertEqual(events[1]["event_id"], acknowledgements[0]["event_id"])
        sleep.assert_not_called()

    def test_all_successes_return_count_and_log_in_plan_order(self) -> None:
        config = copy.deepcopy(self.config)
        config["enabled"] = True
        events = ordinary_publication_events(config)
        plan = incremental_plan(config, events)
        environment = publication_environment(config)
        responses = [
            JsonResponse({"ok": True, "changed": True, "event_id": events[0]["event_id"]}),
            JsonResponse({"ok": True, "changed": False, "event_id": events[1]["event_id"]}),
        ]
        outcomes = []
        with patch("urllib.request.urlopen", side_effect=responses):
            count = publish_plan(
                plan,
                config,
                environment,
                acknowledgement_logger=lambda value: outcomes.append(("success", value)),
                failure_logger=lambda value: outcomes.append(("failure", value)),
            )
        self.assertEqual(2, count)
        self.assertEqual(["success", "success"], [kind for kind, _value in outcomes])
        self.assertEqual([event["event_id"] for event in events], [value["event_id"] for _kind, value in outcomes])
        self.assertEqual([True, False], [value["changed"] for _kind, value in outcomes])

    def test_signed_payload_is_exact_and_unicode_safe(self) -> None:
        event = {"event_id": "e" * 64, "message": "Mynx ünicode"}
        secret = "s" * 32
        wrapper = json.loads(signed_wrapper(event, secret))
        padded = wrapper["payload"] + "=" * (-len(wrapper["payload"]) % 4)
        self.assertEqual(event, json.loads(base64.urlsafe_b64decode(padded).decode("utf-8")))
        expected = hmac.new(secret.encode(), wrapper["payload"].encode("ascii"), hashlib.sha256).hexdigest()
        self.assertEqual("sha256=" + expected, wrapper["signature"])

    def test_publish_surfaces_first_write_and_idempotent_repeat_acknowledgements(self) -> None:
        config = copy.deepcopy(self.config)
        config["enabled"] = True
        event = ordinary_publication_event(config)
        plan = incremental_plan(config, [event])
        environment = publication_environment(config)
        responses = [
            JsonResponse({"ok": True, "changed": True, "event_id": event["event_id"]}),
            JsonResponse({"ok": True, "changed": False, "event_id": event["event_id"]}),
        ]
        output = io.StringIO()
        with tempfile.TemporaryDirectory() as temporary:
            plan_path = Path(temporary) / "publication-plan.json"
            plan_path.write_text(json.dumps(plan), encoding="utf-8")
            arguments = ["publish", "--root", str(ROOT), "--plan", str(plan_path)]
            with (
                patch("urllib.request.urlopen", side_effect=responses),
                patch.dict("tools.sheet_sync.os.environ", environment, clear=True),
                redirect_stdout(output),
            ):
                self.assertEqual(0, sheet_sync_main(arguments))
                self.assertEqual(0, sheet_sync_main(arguments))
        expected_base = {
            "event_id": event["event_id"],
            "project_uuid": event["record"]["project_uuid"],
            "revision": event["record"]["revision"],
        }
        for changed in (True, False):
            acknowledgement = {"changed": changed, **expected_base}
            line = "Receiver acknowledgement: " + json.dumps(acknowledgement, sort_keys=True, separators=(",", ":"))
            self.assertIn(line, output.getvalue())

    def test_success_acknowledgement_requires_boolean_changed(self) -> None:
        config = copy.deepcopy(self.config)
        config["enabled"] = True
        event = ordinary_publication_event(config)
        plan = incremental_plan(config, [event])
        environment = publication_environment(config)
        invalid_results = (
            {"ok": True, "event_id": event["event_id"]},
            {"ok": True, "changed": "false", "event_id": event["event_id"]},
        )
        for result in invalid_results:
            with self.subTest(result=result), patch("urllib.request.urlopen", return_value=JsonResponse(result)):
                with self.assertRaisesRegex(ValidationError, "acknowledgement|rejected event"):
                    publish_plan(plan, config, environment)

    def test_busy_is_retried_with_same_event(self) -> None:
        config = copy.deepcopy(self.config)
        config["enabled"] = True
        event = ordinary_publication_event(config)
        plan = incremental_plan(config, [event])
        environment = publication_environment(config)
        responses = [
            JsonResponse({"ok": False, "code": "busy", "error": "receiver mutation lock is busy"}),
            JsonResponse({"ok": True, "changed": True, "event_id": event["event_id"]}),
        ]
        with patch("urllib.request.urlopen", side_effect=responses) as request, patch("tools.sheet_sync.time.sleep") as sleep:
            self.assertEqual(1, publish_plan(plan, config, environment))
            self.assertEqual(2, request.call_count)
            self.assertIs(request.call_args_list[0].args[0], request.call_args_list[1].args[0])
            self.assertTrue(
                all(call.kwargs["timeout"] == RECEIVER_RESPONSE_TIMEOUT_SECONDS for call in request.call_args_list)
            )
            sleep.assert_called_once_with(2)

    def test_revision_rejections_are_permanent_and_not_retried(self) -> None:
        config = copy.deepcopy(self.config)
        config["enabled"] = True
        event = ordinary_publication_event(config)
        plan = incremental_plan(config, [event])
        environment = publication_environment(config)
        for code in ("stale", "conflict", "revision_gap"):
            response = JsonResponse({"ok": False, "code": code, "error": "permanent revision rejection"})
            with (
                self.subTest(code=code),
                patch("urllib.request.urlopen", return_value=response) as request,
                patch("tools.sheet_sync.time.sleep") as sleep,
            ):
                with self.assertRaisesRegex(ValidationError, code):
                    publish_plan(plan, config, environment)
                self.assertEqual(1, request.call_count)
                sleep.assert_not_called()


class SheetWorkflowAuthorityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.workflow = (ROOT / ".github" / "workflows" / "publish-project-status.yml").read_text(encoding="utf-8")

    def test_normal_push_remains_main_only_and_uses_incremental_plan(self) -> None:
        prepare = self.workflow.split("  prepare:", 1)[1].split("\n  publish:", 1)[0]
        publish = self.workflow.split("\n  publish:", 1)[1].split("\n  bootstrap-prepare:", 1)[0]
        fragments = (
            "push:\n    branches: [main]",
            "if: github.event_name == 'push'",
            "python tools/sheet_sync.py plan",
            '--before "${{ github.event.before }}"',
            '--after "${{ github.sha }}"',
            '--repository "${{ github.repository }}"',
            '--ref "${{ github.ref }}"',
        )
        for fragment in fragments:
            with self.subTest(fragment=fragment):
                self.assertIn(fragment, self.workflow if fragment.startswith("push:") else prepare)
        self.assertIn("github.event_name == 'push'", publish)
        self.assertIn("vars.MYNX_SHEET_CUTOVER == 'authorized'", publish)
        self.assertIn('- "tools/sheet_sync.py"', self.workflow)
        self.assertIn('- "tools/sheet_sync/**"', self.workflow)

    def test_dispatch_exposes_only_the_explicit_bootstrap_operation(self) -> None:
        dispatch = self.workflow.split("  workflow_dispatch:", 1)[1].split("\n\npermissions:", 1)[0]
        self.assertIn("inputs:", dispatch)
        self.assertIn("operation:", dispatch)
        self.assertIn("required: true", dispatch)
        self.assertIn("type: choice", dispatch)
        self.assertIn("- bootstrap-current-state", dispatch)
        for forbidden in ("repository:", "ref:", "commit:"):
            with self.subTest(forbidden=forbidden):
                self.assertNotIn(forbidden, dispatch)

    def test_bootstrap_dispatch_uses_exact_github_main_snapshot_and_validates_first(self) -> None:
        prepare = self.workflow.split("  bootstrap-prepare:", 1)[1].split("\n  bootstrap-publish:", 1)[0]
        fragments = (
            'test "$GITHUB_REPOSITORY" = "Resivore/Mynx-Flavoured-Workbench"',
            'test "$GITHUB_REF" = "refs/heads/main"',
            'test "$REQUESTED_OPERATION" = "bootstrap-current-state"',
            "ref: ${{ github.sha }}",
            "python tools/workbench.py validate-repository --root .",
            "python -m unittest discover -s tests -v",
            "node --test tests/test_sheet_receiver.mjs",
            "git fetch --no-tags origin refs/heads/main:refs/heads/main",
            "Prepare explicit current-state reconciliation plan",
            "python tools/sheet_sync.py current-state-plan",
            '--commit "${{ github.sha }}"',
            '--repository "${{ github.repository }}"',
            '--ref "${{ github.ref }}"',
        )
        for fragment in fragments:
            with self.subTest(fragment=fragment):
                self.assertIn(fragment, prepare)
        self.assertLess(prepare.index("validate-repository"), prepare.index("current-state-plan"))

    def test_bootstrap_publish_remains_exact_authority_and_production_secret_gated(self) -> None:
        publish = self.workflow.split("  bootstrap-publish:", 1)[1]
        fragments = (
            "github.event_name == 'workflow_dispatch'",
            "github.repository == 'Resivore/Mynx-Flavoured-Workbench'",
            "github.ref == 'refs/heads/main'",
            "inputs.operation == 'bootstrap-current-state'",
            "vars.MYNX_SHEET_CUTOVER == 'authorized'",
            "environment: sheet-production",
            "MYNX_SHEET_CUTOVER: ${{ vars.MYNX_SHEET_CUTOVER }}",
            "MYNX_SHEET_RECEIVER_URL: ${{ secrets.MYNX_SHEET_RECEIVER_URL }}",
            "MYNX_SHEET_HMAC_SECRET: ${{ secrets.MYNX_SHEET_HMAC_SECRET }}",
            "Publish authoritative current-state reconciliation",
            "python tools/sheet_sync.py publish",
        )
        for fragment in fragments:
            with self.subTest(fragment=fragment):
                self.assertIn(fragment, publish)
        self.assertEqual(2, self.workflow.count("environment: sheet-production"))
        self.assertEqual(2, self.workflow.count("MYNX_SHEET_RECEIVER_URL: ${{ secrets.MYNX_SHEET_RECEIVER_URL }}"))
        self.assertEqual(2, self.workflow.count("MYNX_SHEET_HMAC_SECRET: ${{ secrets.MYNX_SHEET_HMAC_SECRET }}"))


if __name__ == "__main__":
    unittest.main()
