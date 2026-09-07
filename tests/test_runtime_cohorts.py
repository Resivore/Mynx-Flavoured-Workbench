from __future__ import annotations

import copy
import json
import unittest
from pathlib import Path

from tests.test_bootstrap import (
    TIME_1,
    TIME_2,
    TIME_3,
    adopted_rollback_unit,
    candidate,
    deployment_unit,
    planned_manifest,
    project_index,
    runtime_state,
    status_catalog,
)
from tools.runtime_slots import (
    _PHYSICAL_MANAGER_AUTHORITY,
    candidate_declaration,
    finalize_verified_profile_transition,
    migrate_runtime_state,
    plan_transition,
    render_title_state,
    resolve_profile,
    validate_runtime_state,
)
from tools.workbench import (
    ValidationError,
    current_release_deployment_comparison,
    validate_testing_slot_lifecycles,
)


ROOT = Path(__file__).resolve().parents[1]


def cohort_member(unit: dict, *, replacement: str | None = None, result: str = "UNTESTED") -> dict:
    ready = result != "UNTESTED"
    return {
        "unit": copy.deepcopy(unit),
        "replaces_accepted_deployment_id": replacement,
        "runtime_result": {
            "classification": result,
            "recorded_at": TIME_1 if ready else None,
            "evidence": {
                "passed": [f"{unit['project_id']} observed pass"] if result in {"PASS", "INCONCLUSIVE"} else [],
                "failed": [f"{unit['project_id']} observed failure"] if result in {"FAIL", "INCONCLUSIVE"} else [],
            },
        },
    }


def cohort_state(*members: dict, ready: bool = False, slot_b: dict | None = None) -> dict:
    state = runtime_state()
    state["schema_version"] = 2
    state["revision"] = 3 if members or slot_b else 0
    state["slots"]["A"] = None if not members else {
        "members": [copy.deepcopy(member) for member in members],
        "deployment": {
            "state": "READY_TO_TEST_VERIFIED" if ready else "NOT_DEPLOYED",
            "deployed_at": TIME_1 if ready else None,
            "ready_verified_at": TIME_1 if ready else None,
        },
    }
    state["slots"]["B"] = copy.deepcopy(slot_b)
    return state


def set_current_release(manifest: dict, unit: dict, deployment: str) -> None:
    artifact = unit["artifacts"][0]
    manifest["definition"]["lifecycle"] = "TESTING"
    manifest["state"]["releases"]["current"] = {
        "version": f"Repository label ({unit['version']})",
        "embedded_version": unit["version"],
        "artifact": {"filename": artifact["filename"], "sha256": artifact["sha256"]},
        "source_commit": unit["source_commit"],
    }
    manifest["state"]["validation"]["deployment"] = deployment


def revision_61_legacy_pair_state() -> tuple[dict, dict[str, str]]:
    """Embed the exact rev-61 BGE C57 / Trowel C9 slot slice.

    Only the three accepted members referenced by the two slots are needed to
    validate replacement and dependency-override links. Nothing is loaded from
    the mutable canonical runtime-state file.
    """

    accepted_bge = {
        "deployment_id": "3a3f0f77-d114-4b58-a039-302d625ec449",
        "project_uuid": "4b2342fc-7bdf-5ba6-9f37-d551109d214c",
        "project_id": "block-geometry-extensions",
        "project_identity_source": "CURRENT_MANIFEST",
        "version": "0.5.49-nibaru-cnm-canary1.39-native-directional-material-axis",
        "source_commit": "2e58d752af44f4b84e10bc5e53a41aea00a177ee",
        "artifacts": [
            {
                "artifact_id": "3d3f10bd-8481-417c-adc7-fe154e2766c7",
                "kind": "MOD",
                "filename": "cnm-nibaru-integration-0.5.49-nibaru-cnm-canary1.39-native-directional-material-axis.jar",
                "sha256": "0e84fb7b8c69e31c3c22a592d0667db66c2918c8fd9f461bd2c216d377722e69",
                "ownership_keys": ["mod:cnm_terrain_slabs_compat"],
                "source": {
                    "type": "REPOSITORY",
                    "path": "projects/block-geometry-extensions/artifacts/cnm-nibaru-integration-0.5.49-nibaru-cnm-canary1.39-native-directional-material-axis.jar",
                },
            }
        ],
    }
    accepted_nibaru = {
        "deployment_id": "e5eb4fcb-6c49-4ab2-86f9-1605ccd192ab",
        "project_uuid": "680476b9-5336-5422-a575-779f2efd1eff",
        "project_id": "nibaru",
        "project_identity_source": "CURRENT_MANIFEST",
        "version": "4.2.0+26.2-port-canary43-native-directional-material-axis",
        "source_commit": "2e58d752af44f4b84e10bc5e53a41aea00a177ee",
        "artifacts": [
            {
                "artifact_id": "b337b39a-f331-4e03-bbdc-e3c3c6925536",
                "kind": "MOD",
                "filename": "more-slabs-stairs-and-walls-4.2.0+26.2-port-canary43-native-directional-material-axis.jar",
                "sha256": "0a979a75101076e987a35807f8eb293631fe5e664b252e5db0aa263d4eedf07f",
                "ownership_keys": ["mod:more_slabs_stairs_and_walls"],
                "source": {
                    "type": "REPOSITORY",
                    "path": "projects/nibaru/artifacts/more-slabs-stairs-and-walls-4.2.0+26.2-port-canary43-native-directional-material-axis.jar",
                },
            }
        ],
    }
    accepted_trowel = {
        "deployment_id": "6c8c4d94-bce9-479a-916c-66b94eecc852",
        "project_uuid": "e28154da-0649-5da7-b6d5-3bff2891719e",
        "project_id": "shulker-trowel",
        "project_identity_source": "CURRENT_MANIFEST",
        "version": "0.1.0-canary4",
        "source_commit": "069bc055356c5a46218acfb846c97acc0c0c4789",
        "artifacts": [
            {
                "artifact_id": "80331cb2-02aa-446b-845e-001b24c27415",
                "kind": "MOD",
                "filename": "shulker-trowel-0.1.0-canary4-private.jar",
                "sha256": "73c012c08ca5567e795711cf11c8eaac625528e7350309114467129394f0afba",
                "ownership_keys": ["mod:shulker_trowel"],
                "source": {
                    "type": "ADOPTED_TARGET",
                    "path": "mods/shulker-trowel-0.1.0-canary4-private.jar",
                },
            }
        ],
    }
    bge_c57 = {
        "deployment_id": "3d111123-9043-4e31-8d73-61d3a735c0b6",
        "project_uuid": "4b2342fc-7bdf-5ba6-9f37-d551109d214c",
        "project_id": "block-geometry-extensions",
        "project_identity_source": "CURRENT_MANIFEST",
        "version": "4.2.1-bge.canary57.unified+26.2",
        "source_commit": "f907bdab139fd2ec68f8741f5a449b9ee0c973f4",
        "artifacts": [
            {
                "artifact_id": "57a7d2f8-a9df-4c7b-843a-c759281978e0",
                "kind": "MOD",
                "filename": "cnm-nibaru-integration-4.2.1-bge.canary57.unified+26.2.jar",
                "sha256": "7cd01479531ec26975326b882e0a18406c17de26695b1f4fae29b72edb76cbc2",
                "ownership_keys": [
                    "mod:cnm_terrain_slabs_compat",
                    "mod:more_slabs_stairs_and_walls",
                ],
                "source": {
                    "type": "REPOSITORY",
                    "path": "projects/block-geometry-extensions/artifacts/cnm-nibaru-integration-4.2.1-bge.canary57.unified+26.2.jar",
                },
            }
        ],
    }
    trowel_c9 = {
        "deployment_id": "3ec63f11-ff42-443a-afe3-5482d8081d4a",
        "project_uuid": "e28154da-0649-5da7-b6d5-3bff2891719e",
        "project_id": "shulker-trowel",
        "project_identity_source": "CURRENT_MANIFEST",
        "version": "0.1.0-canary8",
        "source_commit": "f907bdab139fd2ec68f8741f5a449b9ee0c973f4",
        "artifacts": [
            {
                "artifact_id": "4c70ce61-bc45-4355-88c5-47e7396d2851",
                "kind": "MOD",
                "filename": "shulker-trowel-0.1.0-canary8-private.jar",
                "sha256": "2e74d902c46cb3072ab33cdc56ac6ae55e2fd5ec0f4e5ee297cfebec338513ab",
                "ownership_keys": ["mod:shulker_trowel"],
                "source": {
                    "type": "REPOSITORY",
                    "path": "projects/shulker-trowel/test-builds/private/shulker-trowel/canary8/shulker-trowel-0.1.0-canary8-private.jar",
                },
            }
        ],
    }
    slot_a = {
        "unit": bge_c57,
        "replaces_accepted_deployment_id": "3a3f0f77-d114-4b58-a039-302d625ec449",
        "dependency_overrides": ["e5eb4fcb-6c49-4ab2-86f9-1605ccd192ab"],
        "deployment": {
            "state": "READY_TO_TEST_VERIFIED",
            "deployed_at": "2026-08-31T16:37:01Z",
            "ready_verified_at": "2026-08-31T16:37:02Z",
        },
        "runtime_result": {
            "classification": "FAIL",
            "recorded_at": "2026-09-01T05:34:21Z",
            "evidence": {
                "passed": [
                    "User-reported broad nonvisual behavior PASS for exact unified BGE C57; "
                    "no row-level observations were supplied or inferred."
                ],
                "failed": [
                    "User-reported existing glass-Corner visual/UV defect on exact unified BGE C57."
                ],
            },
        },
    }
    slot_b = {
        "unit": trowel_c9,
        "replaces_accepted_deployment_id": "6c8c4d94-bce9-479a-916c-66b94eecc852",
        "deployment": {
            "state": "READY_TO_TEST_VERIFIED",
            "deployed_at": "2026-08-31T16:37:04Z",
            "ready_verified_at": "2026-08-31T16:37:05Z",
        },
        "runtime_result": {
            "classification": "UNTESTED",
            "recorded_at": None,
            "evidence": {"passed": [], "failed": []},
        },
    }
    state = runtime_state(
        accepted=[
            {"unit": accepted_bge, "accepted_at": None},
            {"unit": accepted_nibaru, "accepted_at": None},
            {"unit": accepted_trowel, "accepted_at": None},
        ],
        slot_a=slot_a,
        slot_b=slot_b,
    )
    state.update(
        activation="ACTIVE",
        revision=61,
        updated_at="2026-09-01T05:34:21Z",
    )
    state["accepted_baseline"]["revision"] = 11
    state["accepted_baseline"]["provenance"].update(
        physical_disposition="TRANSITIONED",
        accepted_artifact_count=3,
    )
    return state, {
        accepted_bge["project_uuid"]: accepted_bge["project_id"],
        accepted_nibaru["project_uuid"]: accepted_nibaru["project_id"],
        accepted_trowel["project_uuid"]: accepted_trowel["project_id"],
    }


class RuntimeCohortContractTests(unittest.TestCase):
    def test_user_passed_successor_can_atomically_absorb_another_accepted_provider(self) -> None:
        predecessor = deployment_unit("unified", version="Canary 1")
        absorbed = deployment_unit("legacy-provider", version="Canary 1")
        successor = deployment_unit(
            "unified",
            version="Canary 2",
            ownership_keys=["mod:unified", "mod:legacy-provider"],
        )
        state = runtime_state(
            accepted=[
                {"unit": predecessor, "accepted_at": TIME_1},
                {"unit": absorbed, "accepted_at": TIME_1},
            ]
        )
        operation = {
            "type": "PROMOTE_USER_PASSED_BATCH",
            "authorization": "USER_REPORTED_EXACT_RUNTIME_PASS",
            "members": [
                {
                    "unit": successor,
                    "replaces_accepted_deployment_id": predecessor["deployment_id"],
                    "absorbs_accepted_deployment_ids": [absorbed["deployment_id"]],
                }
            ],
        }

        promoted = plan_transition(
            state,
            state["revision"],
            operation,
            TIME_2,
            project_index("unified", "legacy-provider"),
        )

        self.assertEqual(
            [successor["deployment_id"]],
            [member["unit"]["deployment_id"] for member in promoted["accepted_baseline"]["members"]],
        )
        self.assertEqual(
            state["accepted_baseline"]["revision"] + 2,
            promoted["accepted_baseline"]["revision"],
        )
        self.assertEqual(1, promoted["accepted_baseline"]["provenance"]["accepted_artifact_count"])

        missing_alias = copy.deepcopy(operation)
        missing_alias["members"][0]["unit"]["artifacts"][0]["ownership_keys"] = ["mod:unified"]
        with self.assertRaisesRegex(ValidationError, "own every absorbed ownership key"):
            plan_transition(
                state,
                state["revision"],
                missing_alias,
                TIME_2,
                project_index("unified", "legacy-provider"),
            )

        retained_state = copy.deepcopy(state)
        retained_state["accepted_baseline"]["members"][1]["retained_rollbacks"] = [
            {"unit": adopted_rollback_unit("legacy-provider"), "retained_at": TIME_1}
        ]
        with self.assertRaisesRegex(ValidationError, "cannot discard target-local retained rollback"):
            plan_transition(
                retained_state,
                retained_state["revision"],
                operation,
                TIME_2,
                project_index("unified", "legacy-provider"),
            )

        referenced_state = copy.deepcopy(state)
        legacy_successor = candidate(deployment_unit("legacy-provider", version="Canary 2"))
        legacy_successor["replaces_accepted_deployment_id"] = absorbed["deployment_id"]
        referenced_state["slots"]["A"] = legacy_successor
        with self.assertRaisesRegex(ValidationError, "still referenced by a managed Test Slot"):
            plan_transition(
                referenced_state,
                referenced_state["revision"],
                operation,
                TIME_2,
                project_index("unified", "legacy-provider"),
            )

    def test_exact_revision_61_bge_c57_and_trowel_c9_migrate_losslessly(self) -> None:
        legacy, exact_project_index = revision_61_legacy_pair_state()
        preimage = copy.deepcopy(legacy)

        validate_runtime_state(legacy, exact_project_index)
        migrated = migrate_runtime_state(legacy, exact_project_index)

        self.assertEqual(preimage, legacy)
        self.assertEqual(61, migrated["revision"])
        self.assertEqual("2026-09-01T05:34:21Z", migrated["updated_at"])
        self.assertEqual(preimage["accepted_baseline"], migrated["accepted_baseline"])
        self.assertEqual(
            {
                "3a3f0f77-d114-4b58-a039-302d625ec449",
                "e5eb4fcb-6c49-4ab2-86f9-1605ccd192ab",
                "6c8c4d94-bce9-479a-916c-66b94eecc852",
            },
            {
                member["unit"]["deployment_id"]
                for member in migrated["accepted_baseline"]["members"]
            },
        )

        before_a = preimage["slots"]["A"]
        before_b = preimage["slots"]["B"]
        slot_a = migrated["slots"]["A"]
        slot_b = migrated["slots"]["B"]
        member_a = slot_a["members"][0]
        member_b = slot_b["members"][0]

        self.assertEqual(before_a["unit"], member_a["unit"])
        self.assertEqual(before_b["unit"], member_b["unit"])
        self.assertEqual(before_a["deployment"], slot_a["deployment"])
        self.assertEqual(before_b["deployment"], slot_b["deployment"])
        self.assertEqual(before_a["runtime_result"], member_a["runtime_result"])
        self.assertEqual(before_b["runtime_result"], member_b["runtime_result"])

        self.assertEqual("3d111123-9043-4e31-8d73-61d3a735c0b6", member_a["unit"]["deployment_id"])
        self.assertEqual("3ec63f11-ff42-443a-afe3-5482d8081d4a", member_b["unit"]["deployment_id"])
        self.assertEqual(
            "7cd01479531ec26975326b882e0a18406c17de26695b1f4fae29b72edb76cbc2",
            member_a["unit"]["artifacts"][0]["sha256"],
        )
        self.assertEqual(
            "2e74d902c46cb3072ab33cdc56ac6ae55e2fd5ec0f4e5ee297cfebec338513ab",
            member_b["unit"]["artifacts"][0]["sha256"],
        )
        self.assertEqual(
            "3a3f0f77-d114-4b58-a039-302d625ec449",
            member_a["replaces_accepted_deployment_id"],
        )
        self.assertEqual(
            "6c8c4d94-bce9-479a-916c-66b94eecc852",
            member_b["replaces_accepted_deployment_id"],
        )
        self.assertEqual(
            ["e5eb4fcb-6c49-4ab2-86f9-1605ccd192ab"],
            member_a["dependency_overrides"],
        )
        self.assertNotIn("dependency_overrides", member_b)
        self.assertEqual(
            ("2026-08-31T16:37:01Z", "2026-08-31T16:37:02Z"),
            (slot_a["deployment"]["deployed_at"], slot_a["deployment"]["ready_verified_at"]),
        )
        self.assertEqual(
            ("2026-08-31T16:37:04Z", "2026-08-31T16:37:05Z"),
            (slot_b["deployment"]["deployed_at"], slot_b["deployment"]["ready_verified_at"]),
        )
        self.assertEqual("FAIL", member_a["runtime_result"]["classification"])
        self.assertEqual("2026-09-01T05:34:21Z", member_a["runtime_result"]["recorded_at"])
        self.assertEqual("UNTESTED", member_b["runtime_result"]["classification"])
        self.assertIsNone(member_b["runtime_result"]["recorded_at"])
        validate_runtime_state(migrated, exact_project_index)

    def test_legacy_state_validates_and_migrates_losslessly(self) -> None:
        alpha = deployment_unit("alpha")
        beta = deployment_unit("beta")
        slot_a = candidate(alpha, "FAIL")
        slot_a["replaces_accepted_deployment_id"] = None
        slot_a["dependency_overrides"] = []
        slot_b = candidate(beta, "PASS")
        legacy = runtime_state(slot_a=slot_a, slot_b=slot_b)

        validate_runtime_state(legacy, project_index("alpha", "beta"))
        migrated = migrate_runtime_state(legacy, project_index("alpha", "beta"))

        self.assertEqual(2, migrated["schema_version"])
        self.assertEqual(legacy["revision"], migrated["revision"])
        self.assertEqual(legacy["updated_at"], migrated["updated_at"])
        self.assertEqual(legacy["accepted_baseline"], migrated["accepted_baseline"])
        for label in ("A", "B"):
            before = legacy["slots"][label]
            after = migrated["slots"][label]
            self.assertEqual(before["deployment"], after["deployment"])
            self.assertEqual(before["unit"], after["members"][0]["unit"])
            self.assertEqual(
                before["replaces_accepted_deployment_id"],
                after["members"][0]["replaces_accepted_deployment_id"],
            )
            self.assertEqual(before["runtime_result"], after["members"][0]["runtime_result"])
            self.assertEqual(before.get("dependency_overrides", []), after["members"][0].get("dependency_overrides", []))
            self.assertEqual("dependency_overrides" in before, "dependency_overrides" in after["members"][0])
        validate_runtime_state(migrated, project_index("alpha", "beta"))

    def test_migration_is_deterministic_and_idempotent(self) -> None:
        legacy = runtime_state(slot_a=candidate(deployment_unit("alpha", version="Canary 2"), "PASS"))
        once = migrate_runtime_state(legacy, project_index("alpha"))
        twice = migrate_runtime_state(once, project_index("alpha"))
        self.assertEqual(
            json.dumps(once, sort_keys=True, separators=(",", ":")),
            json.dumps(twice, sort_keys=True, separators=(",", ":")),
        )

    def test_legacy_result_still_requires_ready_deployment(self) -> None:
        alpha = deployment_unit("alpha")
        legacy = runtime_state(slot_a=candidate(alpha, "PASS"))
        legacy["slots"]["A"]["deployment"] = {
            "state": "NOT_DEPLOYED",
            "deployed_at": None,
            "ready_verified_at": None,
        }

        with self.assertRaisesRegex(ValidationError, "runtime results require READY_TO_TEST_VERIFIED"):
            validate_runtime_state(legacy, project_index("alpha"))

    def test_two_members_share_one_slot_and_resolve_as_independent_units(self) -> None:
        alpha = deployment_unit("alpha")
        beta = deployment_unit("beta")
        state = cohort_state(cohort_member(alpha), cohort_member(beta))
        validate_runtime_state(state, project_index("alpha", "beta"))
        self.assertEqual(
            ["alpha", "beta"],
            [unit["project_id"] for unit in resolve_profile(state, project_index("alpha", "beta"))],
        )

        alpha_manifest = planned_manifest("alpha", "Alpha", uuid_value=alpha["project_uuid"])
        beta_manifest = planned_manifest("beta", "Beta", uuid_value=beta["project_uuid"])
        alpha_manifest["definition"]["lifecycle"] = "TESTING"
        beta_manifest["definition"]["lifecycle"] = "TESTING"
        validate_testing_slot_lifecycles(status_catalog(alpha_manifest, beta_manifest), state)

    def test_member_results_are_recorded_independently(self) -> None:
        alpha = deployment_unit("alpha")
        beta = deployment_unit("beta")
        state = cohort_state(cohort_member(alpha), cohort_member(beta), ready=True)
        one = plan_transition(
            state,
            state["revision"],
            {
                "type": "RECORD_RESULT",
                "slot": "A",
                "project_uuid": alpha["project_uuid"],
                "classification": "PASS",
                "evidence": {"passed": ["alpha pass"], "failed": []},
            },
            TIME_2,
            project_index("alpha", "beta"),
        )
        two = plan_transition(
            one,
            one["revision"],
            {
                "type": "RECORD_RESULT",
                "slot": "A",
                "project_uuid": beta["project_uuid"],
                "classification": "FAIL",
                "evidence": {"passed": [], "failed": ["beta failure"]},
            },
            TIME_3,
            project_index("alpha", "beta"),
        )
        results = {
            member["unit"]["project_id"]: member["runtime_result"]["classification"]
            for member in two["slots"]["A"]["members"]
        }
        self.assertEqual({"alpha": "PASS", "beta": "FAIL"}, results)
        with self.assertRaisesRegex(ValidationError, "requires project_uuid"):
            plan_transition(
                state,
                state["revision"],
                {
                    "type": "RECORD_RESULT",
                    "slot": "A",
                    "classification": "PASS",
                    "evidence": {"passed": ["ambiguous"], "failed": []},
                },
                TIME_2,
                project_index("alpha", "beta"),
            )

    def test_passed_successor_passthrough_rebuilds_accepted_unit_in_original_order(self) -> None:
        project_id = "quick-stack-nearby-compat"
        accepted = deployment_unit(
            project_id,
            version="0.1.0-canary6",
            filename="quick-stack-nearby-compat-0.1.0-canary6.jar",
            ownership="mod:quick_stack_nearby_compat",
        )
        core = deployment_unit(
            "quick-stack-nearby-core",
            version="0.4.0",
            filename="quick-stack-nearby-0.4.0.jar",
            ownership="mod:quick-stack-nearby",
        )["artifacts"][0]
        core["source"] = {
            "type": "ADOPTED_TARGET",
            "path": "mods/quick-stack-nearby-0.4.0.jar",
        }
        accepted["artifacts"].insert(0, core)

        successor = deployment_unit(
            project_id,
            version="0.1.0-canary8",
            filename="quick-stack-nearby-compat-0.1.0-canary8.jar",
            ownership="mod:quick_stack_nearby_compat",
        )
        successor["source_commit"] = "e" * 40
        successor["artifacts"][0]["sha256"] = "3" * 64
        slot_member = cohort_member(
            successor,
            replacement=accepted["deployment_id"],
            result="PASS",
        )
        slot_member["accepted_companion_artifacts"] = [copy.deepcopy(core)]
        state = cohort_state(slot_member, ready=True)
        state["accepted_baseline"]["revision"] = 1
        state["accepted_baseline"]["members"] = [
            {"unit": copy.deepcopy(accepted), "accepted_at": TIME_1}
        ]
        state["accepted_baseline"]["provenance"]["accepted_artifact_count"] = 2
        validate_runtime_state(state, project_index(project_id))

        promoted = plan_transition(
            state,
            state["revision"],
            {"type": "PROMOTE_SLOT", "slot": "A"},
            TIME_2,
            project_index(project_id),
        )

        accepted_after = promoted["accepted_baseline"]["members"][0]
        self.assertIsNone(promoted["slots"]["A"])
        self.assertEqual(state["revision"] + 1, promoted["revision"])
        self.assertEqual(
            state["accepted_baseline"]["revision"] + 1,
            promoted["accepted_baseline"]["revision"],
        )
        self.assertEqual(2, promoted["accepted_baseline"]["provenance"]["accepted_artifact_count"])
        self.assertEqual(successor["deployment_id"], accepted_after["unit"]["deployment_id"])
        self.assertEqual(successor["version"], accepted_after["unit"]["version"])
        self.assertEqual(successor["source_commit"], accepted_after["unit"]["source_commit"])
        self.assertEqual(
            [core, successor["artifacts"][0]],
            accepted_after["unit"]["artifacts"],
        )
        self.assertEqual(TIME_2, accepted_after["accepted_at"])

    def test_atomic_set_profile_builds_one_cohort_and_clears_other_slot_once(self) -> None:
        old_alpha = deployment_unit("alpha", version="Canary 1")
        old_beta = deployment_unit("beta", version="Canary 1")
        legacy = runtime_state(slot_a=candidate(old_alpha), slot_b=candidate(old_beta))
        state = migrate_runtime_state(legacy, project_index("alpha", "beta"))
        alpha = deployment_unit("alpha", version="Canary 2")
        beta = deployment_unit("beta", version="Canary 2")

        planned = plan_transition(
            state,
            state["revision"],
            {
                "type": "SET_PROFILE",
                "slots": {
                    "A": {
                        "members": [
                            candidate_declaration(alpha),
                            candidate_declaration(beta),
                        ]
                    },
                    "B": None,
                },
            },
            TIME_3,
            project_index("alpha", "beta"),
        )
        self.assertEqual(state["revision"] + 1, planned["revision"])
        self.assertEqual(["alpha", "beta"], [m["unit"]["project_id"] for m in planned["slots"]["A"]["members"]])
        self.assertEqual("NOT_DEPLOYED", planned["slots"]["A"]["deployment"]["state"])
        self.assertIsNone(planned["slots"]["B"])

        with self.assertRaisesRegex(ValidationError, "physical manager"):
            finalize_verified_profile_transition(
                planned,
                TIME_2,
                TIME_3,
                project_index("alpha", "beta"),
                changed_slots=["A", "B"],
                authority=object(),
            )
        verified = finalize_verified_profile_transition(
            planned,
            TIME_2,
            TIME_3,
            project_index("alpha", "beta"),
            changed_slots=["A", "B"],
            authority=_PHYSICAL_MANAGER_AUTHORITY,
        )
        self.assertEqual(planned["revision"], verified["revision"])
        self.assertEqual("READY_TO_TEST_VERIFIED", verified["slots"]["A"]["deployment"]["state"])

    def test_revision_order_allows_backward_host_clock_without_falsifying_runtime_evidence(self) -> None:
        """Regression for the revision-110 wall-clock skew recovery.

        The persisted state watermark was 36 minutes ahead of the observed
        host UTC time.  CAS revision ordering remains authoritative: the next
        valid result transition succeeds, preserves its real observation time,
        and retains the prior watermark rather than inventing a future one.
        """

        alpha = deployment_unit("alpha")
        state = cohort_state(cohort_member(alpha), ready=True)
        state["revision"] = 110
        state["updated_at"] = "2026-09-07T04:02:01Z"
        observed_at = "2026-09-07T03:25:41Z"
        validate_runtime_state(state, project_index("alpha"))

        transitioned = plan_transition(
            state,
            110,
            {
                "type": "RECORD_RESULT",
                "slot": "A",
                "classification": "INCONCLUSIVE",
                "evidence": {"passed": ["observed only"], "failed": []},
            },
            observed_at,
            project_index("alpha"),
        )

        self.assertEqual(111, transitioned["revision"])
        self.assertEqual("2026-09-07T04:02:01Z", transitioned["updated_at"])
        self.assertEqual(
            observed_at,
            transitioned["slots"]["A"]["members"][0]["runtime_result"]["recorded_at"],
        )

    def test_single_member_update_preserves_companion_and_other_slot(self) -> None:
        alpha = deployment_unit("alpha", version="Canary 1")
        alpha_v2 = deployment_unit("alpha", version="Canary 2")
        beta = deployment_unit("beta")
        gamma = deployment_unit("gamma")
        other = cohort_state(cohort_member(gamma))["slots"]["A"]
        state = cohort_state(
            cohort_member(alpha, result="PASS"),
            cohort_member(beta, result="FAIL"),
            ready=True,
            slot_b=other,
        )
        before_b = copy.deepcopy(state["slots"]["B"])
        before_companion = copy.deepcopy(state["slots"]["A"]["members"][1])

        updated = plan_transition(
            state,
            state["revision"],
            {"type": "UPDATE_SLOT", "slot": "A", "candidate": candidate_declaration(alpha_v2)},
            TIME_2,
            project_index("alpha", "beta", "gamma"),
        )
        self.assertEqual(["Canary 2", "Canary 1"], [m["unit"]["version"] for m in updated["slots"]["A"]["members"]])
        self.assertEqual("UNTESTED", updated["slots"]["A"]["members"][0]["runtime_result"]["classification"])
        self.assertEqual(before_companion, updated["slots"]["A"]["members"][1])
        self.assertEqual("NOT_DEPLOYED", updated["slots"]["A"]["deployment"]["state"])
        self.assertEqual(before_b, updated["slots"]["B"])

    def test_one_member_update_can_replace_the_whole_slot_with_another_project(self) -> None:
        alpha = deployment_unit("alpha")
        beta = deployment_unit("beta")
        state = cohort_state(cohort_member(alpha))

        updated = plan_transition(
            state,
            state["revision"],
            {"type": "UPDATE_SLOT", "slot": "A", "candidate": candidate_declaration(beta)},
            TIME_2,
            project_index("alpha", "beta"),
        )

        self.assertEqual("beta", updated["slots"]["A"]["members"][0]["unit"]["project_id"])
        self.assertEqual("UNTESTED", updated["slots"]["A"]["members"][0]["runtime_result"]["classification"])

    def test_multi_member_candidate_update_rejects_an_unmatched_project(self) -> None:
        alpha = deployment_unit("alpha")
        beta = deployment_unit("beta")
        gamma = deployment_unit("gamma")
        state = cohort_state(cohort_member(alpha), cohort_member(beta))

        with self.assertRaisesRegex(ValidationError, "identify exactly one existing cohort member"):
            plan_transition(
                state,
                state["revision"],
                {"type": "UPDATE_SLOT", "slot": "A", "candidate": candidate_declaration(gamma)},
                TIME_2,
                project_index("alpha", "beta", "gamma"),
            )

    def test_verified_finalizer_never_mutates_an_untouched_slot(self) -> None:
        alpha = deployment_unit("alpha")
        beta = deployment_unit("beta")
        slot_b = cohort_state(cohort_member(beta))["slots"]["A"]
        slot_b["deployment"] = {
            "state": "DEPLOYED",
            "deployed_at": TIME_1,
            "ready_verified_at": None,
        }
        planned = cohort_state(cohort_member(alpha), slot_b=slot_b)
        planned["updated_at"] = TIME_3
        before_b = copy.deepcopy(planned["slots"]["B"])

        verified = finalize_verified_profile_transition(
            planned,
            TIME_2,
            TIME_3,
            project_index("alpha", "beta"),
            changed_slots=["A"],
            authority=_PHYSICAL_MANAGER_AUTHORITY,
        )

        self.assertEqual("READY_TO_TEST_VERIFIED", verified["slots"]["A"]["deployment"]["state"])
        self.assertEqual(before_b, verified["slots"]["B"])

    def test_title_projection_enumerates_every_cohort_member(self) -> None:
        alpha = deployment_unit("alpha", version="2.0-canary58")
        beta = deployment_unit("beta", version="0.1.0-canary9")
        state = cohort_state(cohort_member(alpha), cohort_member(beta))
        state["accepted_baseline"]["revision"] = 1
        rendered = render_title_state(
            state,
            {alpha["project_uuid"]: "BGE", beta["project_uuid"]: "Shulker Trowel"},
            project_index("alpha", "beta"),
        )
        self.assertEqual(
            "Slot A: BGE - Canary 58 + Shulker Trowel - Canary 9",
            rendered["slots"]["A"]["line"],
        )
        self.assertEqual(2, len(rendered["slots"]["A"]["members"]))

    def test_current_release_claim_rejects_older_slot_member(self) -> None:
        old = deployment_unit("alpha", version="Canary 1")
        current = deployment_unit("alpha", version="Canary 2")
        state = cohort_state(cohort_member(old), ready=True)
        manifest = planned_manifest("alpha", "Alpha", uuid_value=old["project_uuid"])
        set_current_release(manifest, current, "READY_TO_TEST_VERIFIED")

        self.assertEqual("OLDER_RELEASE_DEPLOYED", current_release_deployment_comparison(manifest, state))
        with self.assertRaisesRegex(ValidationError, "current release is not the exact member"):
            validate_testing_slot_lifecycles(status_catalog(manifest), state)
        manifest["state"]["validation"]["deployment"] = "NOT_DEPLOYED"
        validate_testing_slot_lifecycles(status_catalog(manifest), state)

    def test_current_release_match_requires_shared_deployment_state(self) -> None:
        current = deployment_unit("alpha", version="Canary 2")
        state = cohort_state(cohort_member(current), ready=True)
        manifest = planned_manifest("alpha", "Alpha", uuid_value=current["project_uuid"])
        set_current_release(manifest, current, "NOT_DEPLOYED")
        self.assertEqual("CURRENT_RELEASE_DEPLOYED", current_release_deployment_comparison(manifest, state))
        with self.assertRaisesRegex(ValidationError, "shared cohort state"):
            validate_testing_slot_lifecycles(status_catalog(manifest), state)

    def test_current_release_cannot_claim_ready_while_slot_is_not_deployed(self) -> None:
        current = deployment_unit("alpha", version="Canary 2")
        state = cohort_state(cohort_member(current))
        manifest = planned_manifest("alpha", "Alpha", uuid_value=current["project_uuid"])
        set_current_release(manifest, current, "READY_TO_TEST_VERIFIED")

        self.assertEqual("CURRENT_RELEASE_NOT_DEPLOYED", current_release_deployment_comparison(manifest, state))
        with self.assertRaisesRegex(ValidationError, "shared cohort is NOT_DEPLOYED"):
            validate_testing_slot_lifecycles(status_catalog(manifest), state)

        manifest["state"]["validation"]["deployment"] = "NOT_DEPLOYED"
        validate_testing_slot_lifecycles(status_catalog(manifest), state)


class RuntimeCohortSchemaTests(unittest.TestCase):
    def test_schema_documents_distinct_v1_and_v2_slot_shapes(self) -> None:
        schema = json.loads((ROOT / "schemas" / "runtime-state.schema.json").read_text(encoding="utf-8"))
        self.assertEqual([1, 2], schema["properties"]["schema_version"]["enum"])
        self.assertIn("legacySlot", schema["$defs"])
        self.assertIn("cohortSlot", schema["$defs"])
        self.assertEqual(["members", "deployment"], schema["$defs"]["cohortSlot"]["required"])
        cohort_companions = schema["$defs"]["cohortMember"]["properties"][
            "accepted_companion_artifacts"
        ]
        self.assertEqual(1, cohort_companions["minItems"])
        self.assertEqual("#/$defs/artifact", cohort_companions["items"]["$ref"])
        self.assertNotIn(
            "accepted_companion_artifacts",
            schema["$defs"]["legacySlot"]["properties"],
        )


if __name__ == "__main__":
    unittest.main()
