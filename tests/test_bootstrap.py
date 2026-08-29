from __future__ import annotations

import copy
import base64
import hashlib
import hmac
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from uuid import NAMESPACE_URL, uuid5

from tools.runtime_slots import candidate_declaration, commit_state, plan_transition, resolve_profile, state_digest, validate_runtime_state
from tools.sheet_sync import (
    HUMAN_FIELDS,
    ZERO_COMMIT,
    _migration_adoption_uuids_at,
    build_events,
    flatten_manifest,
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
    load_json,
    load_repository_statuses,
    validate_log_append,
    validate_project_change_scope,
    validate_repository,
    validate_status,
    validate_status_transition,
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


def deployment_unit(project_id: str, *, version: str = "Canary 1", filename: str | None = None, ownership: str | None = None) -> dict:
    seed = project_id + ":" + version
    return {
        "deployment_id": stable_uuid("deployment:" + seed),
        "project_uuid": stable_uuid("project:" + project_id),
        "project_id": project_id,
        "version": version,
        "source_commit": ("c" if version == "Canary 1" else "d") * 40,
        "artifacts": [
            {
                "artifact_id": stable_uuid("artifact:" + seed),
                "kind": "MOD",
                "filename": filename or f"{project_id}-{version.lower().replace(' ', '-')}.jar",
                "sha256": ("1" if version == "Canary 1" else "2") * 64,
                "ownership_keys": [ownership or f"mod:{project_id}"],
            }
        ],
    }


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
        "runtime_result": {"classification": result, "recorded_at": TIME_1 if ready else None},
    }


def runtime_state(accepted: list[dict] | None = None, slot_a: dict | None = None, slot_b: dict | None = None) -> dict:
    accepted = accepted or []
    return {
        "$schema": "../../schemas/runtime-state.schema.json",
        "schema_version": 1,
        "activation": "GATED",
        "revision": 3 if (accepted or slot_a or slot_b) else 0,
        "updated_at": TIME_1,
        "accepted_baseline": {"revision": 1 if accepted else 0, "members": copy.deepcopy(accepted)},
        "slots": {"A": copy.deepcopy(slot_a), "B": copy.deepcopy(slot_b)},
    }


def project_index(*project_ids: str) -> dict[str, str]:
    return {stable_uuid("project:" + project_id): project_id for project_id in project_ids}


class StatusContractTests(unittest.TestCase):
    def test_minimal_planned_project_needs_no_source_or_artifact(self) -> None:
        manifest = planned_manifest()
        validate_status(manifest)
        self.assertIsNone(manifest["state"]["releases"]["current"])

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

    def test_b_only_and_more_than_two_are_rejected(self) -> None:
        alpha = deployment_unit("alpha")
        state = runtime_state(slot_b=candidate(alpha))
        with self.assertRaisesRegex(ValidationError, "left-packed"):
            validate_runtime_state(state, project_index("alpha"))
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
        state = runtime_state()
        with self.assertRaisesRegex(ValidationError, "cannot precede"):
            plan_transition(
                state,
                0,
                {"type": "ASSIGN_SLOT", "candidate": candidate_declaration(deployment_unit("beta"))},
                "2026-08-29T11:59:59Z",
                project_index("beta"),
            )

    def test_promote_a_preserves_and_left_packs_b(self) -> None:
        base = deployment_unit("base")
        alpha = deployment_unit("alpha")
        beta = deployment_unit("beta")
        accepted = [{"unit": base, "accepted_at": TIME_1}]
        slot_a = candidate(alpha, "PASS")
        slot_b = candidate(beta, "FAIL")
        state = runtime_state(accepted, slot_a, slot_b)
        prior_b = copy.deepcopy(state["slots"]["B"])
        next_state = plan_transition(state, state["revision"], {"type": "PROMOTE_SLOT", "slot": "A"}, TIME_2, project_index("base", "alpha", "beta"))
        self.assertEqual(prior_b, next_state["slots"]["A"])
        self.assertIsNone(next_state["slots"]["B"])
        self.assertEqual(state["revision"] + 1, next_state["revision"])
        self.assertEqual(state["accepted_baseline"]["revision"] + 1, next_state["accepted_baseline"]["revision"])
        self.assertEqual({"base", "alpha", "beta"}, {unit["project_id"] for unit in resolve_profile(next_state, project_index("base", "alpha", "beta"))})

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

    def test_atomic_cas_is_gated_and_preserves_foreign_lock(self) -> None:
        alpha = deployment_unit("alpha")
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "runtime-state.json"
            state = runtime_state()
            path.write_text(json.dumps(state), encoding="utf-8")
            with self.assertRaisesRegex(ValidationError, "disabled"):
                commit_state(
                    path,
                    0,
                    state_digest(state),
                    {"type": "ASSIGN_SLOT", "candidate": candidate_declaration(alpha)},
                    TIME_2,
                    project_index("alpha"),
                )
            self.assertEqual(state_digest(state), state_digest(load_json(path)))
            state["activation"] = "ACTIVE"
            path.write_text(json.dumps(state), encoding="utf-8")
            next_state = commit_state(
                path,
                0,
                state_digest(state),
                {"type": "ASSIGN_SLOT", "candidate": candidate_declaration(alpha)},
                TIME_2,
                project_index("alpha"),
            )
            self.assertEqual(1, next_state["revision"])
            lock_path = path.with_name(path.name + ".lock")
            lock_path.write_text("foreign", encoding="utf-8")
            with self.assertRaises(FileExistsError):
                commit_state(path, 1, state_digest(next_state), {"type": "REMOVE_SLOT", "slot": "A"}, TIME_3, project_index("alpha"))
            self.assertEqual("foreign", lock_path.read_text(encoding="utf-8"))

    def test_tracked_runtime_state_remains_gated(self) -> None:
        tracked = load_json(ROOT / "tools" / "test_instance_manager" / "runtime-state.json")
        self.assertEqual("GATED", tracked["activation"])


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

    def test_payload_excludes_human_fields_and_live_publication_is_gated(self) -> None:
        self.assertFalse(self.config["enabled"])
        record = flatten_manifest(planned_manifest(), "c" * 40)
        self.assertTrue(all(field.casefold() not in {key.casefold() for key in record} for field in HUMAN_FIELDS))
        plan = {
            "contract_version": 1,
            "source": {"repository": self.config["repository"], "ref": self.config["authoritative_ref"], "before": "0" * 40, "after": "c" * 40},
            "events": [],
        }
        with self.assertRaisesRegex(ValidationError, "tracked-disabled"):
            publish_plan(plan, self.config, {})

    def test_http_200_receiver_rejection_is_not_counted_as_published(self) -> None:
        config = copy.deepcopy(self.config)
        config["enabled"] = True
        event = {"event_id": "e" * 64}
        plan = {
            "contract_version": 1,
            "source": {"repository": config["repository"], "ref": config["authoritative_ref"], "before": "0" * 40, "after": "c" * 40},
            "events": [event],
        }
        environment = {
            config["cutover_environment_variable"]: config["cutover_required_value"],
            config["receiver_url_environment_variable"]: "https://receiver.invalid/",
            config["hmac_environment_variable"]: "s" * 32,
        }

        class Response:
            status = 200

            def __enter__(self):
                return self

            def __exit__(self, *_args):
                return False

            def read(self):
                return b'{"ok":false,"error":"write gate disabled"}'

        with patch("urllib.request.urlopen", return_value=Response()):
            with self.assertRaisesRegex(ValidationError, "rejected event"):
                publish_plan(plan, config, environment)

    def test_signed_payload_is_exact_and_unicode_safe(self) -> None:
        event = {"event_id": "e" * 64, "message": "Mynx ünicode"}
        secret = "s" * 32
        wrapper = json.loads(signed_wrapper(event, secret))
        padded = wrapper["payload"] + "=" * (-len(wrapper["payload"]) % 4)
        self.assertEqual(event, json.loads(base64.urlsafe_b64decode(padded).decode("utf-8")))
        expected = hmac.new(secret.encode(), wrapper["payload"].encode("ascii"), hashlib.sha256).hexdigest()
        self.assertEqual("sha256=" + expected, wrapper["signature"])

    def test_revision_gap_is_retried_with_same_event(self) -> None:
        config = copy.deepcopy(self.config)
        config["enabled"] = True
        event = {"event_id": "e" * 64}
        plan = {
            "contract_version": 1,
            "source": {"repository": config["repository"], "ref": config["authoritative_ref"], "before": "0" * 40, "after": "c" * 40},
            "events": [event],
        }
        environment = {
            config["cutover_environment_variable"]: config["cutover_required_value"],
            config["receiver_url_environment_variable"]: "https://receiver.invalid/",
            config["hmac_environment_variable"]: "s" * 32,
        }

        class Response:
            status = 200

            def __init__(self, body: dict):
                self.body = json.dumps(body).encode()

            def __enter__(self):
                return self

            def __exit__(self, *_args):
                return False

            def read(self):
                return self.body

        responses = [
            Response({"ok": False, "code": "revision_gap", "error": "stale or skipped Sheet revision"}),
            Response({"ok": True, "changed": True, "event_id": event["event_id"]}),
        ]
        with patch("urllib.request.urlopen", side_effect=responses) as request, patch("tools.sheet_sync.time.sleep") as sleep:
            self.assertEqual(1, publish_plan(plan, config, environment))
            self.assertEqual(2, request.call_count)
            sleep.assert_called_once_with(2)


if __name__ == "__main__":
    unittest.main()
