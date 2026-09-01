from __future__ import annotations

import copy
import hashlib
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

from tests.test_instance_manager import (
    ManagerFixture,
    artifact,
    candidate_declaration,
    stable_uuid,
    tree_snapshot,
    unit,
)
from tools.test_instance_manager.manager import (
    FabricModDescriptor,
    ManagerError,
    PhysicalManager,
    _fabric_predicate_matches,
)


C57 = "4.2.1-bge.canary57.unified+26.2"
C58 = "4.2.2-bge.canary58.glass-corner-uv+26.2"
COMPATIBLE_LATER = "4.2.9-bge.canary99.compatible+26.2"
PRE_UNIFIED = "4.2.0-bge.canary56.pre-unified+26.2"
UPPER_BOUND = "4.3.0-"
TROWEL_RANGE = ">=4.2.1-bge.canary57.unified+26.2 <4.3.0-"


def descriptor(
    filename: str,
    primary_id: str,
    version: str,
    *,
    provides: tuple[str, ...] = (),
    depends: dict[str, tuple[str, ...]] | None = None,
    project_id: str | None = None,
) -> FabricModDescriptor:
    return FabricModDescriptor(
        path=Path(filename),
        relative_path="mods/" + filename,
        filename=filename,
        primary_id=primary_id,
        provides=provides,
        version=version,
        depends=depends or {},
        managed_project_uuid=("00000000-0000-4000-8000-" + ("1" if project_id else "0") * 12),
        managed_project_id=project_id,
        managed_deployment_id=("00000000-0000-4000-8000-" + ("2" if project_id else "0") * 12),
        managed_artifact_id=("00000000-0000-4000-8000-" + ("3" if project_id else "0") * 12),
    )


def write_dependency_mod(
    path: Path,
    primary_id: str,
    version: str,
    *,
    provides: tuple[str, ...] = (),
    depends: dict[str, str | list[str]] | None = None,
) -> str:
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr(
            "fabric.mod.json",
            json.dumps(
                {
                    "schemaVersion": 1,
                    "id": primary_id,
                    "version": version,
                    "name": primary_id,
                    "provides": list(provides),
                    "depends": depends or {},
                }
            ),
        )
        archive.writestr("fixture.txt", version)
    return hashlib.sha256(path.read_bytes()).hexdigest()


class FabricPredicateTests(unittest.TestCase):
    def test_trowel_bounded_range_matches_fabric_prerelease_ordering(self) -> None:
        cases = {
            C57: True,
            C58: True,
            COMPATIBLE_LATER: True,
            PRE_UNIFIED: False,
            UPPER_BOUND: False,
            "4.3.0-bge.canary1.breaking+26.2": False,
        }
        for version, expected in cases.items():
            with self.subTest(version=version):
                self.assertEqual(expected, _fabric_predicate_matches(version, TROWEL_RANGE))

    def test_unknown_or_invalid_predicate_syntax_fails_closed(self) -> None:
        # Fabric deliberately falls back to exact StringVersion matching for
        # strings such as `1.x.2`; only forms rejected by Loader are errors.
        for predicate in (">", "<nonsense", ">=", ">=1.x"):
            with self.subTest(predicate=predicate):
                with self.assertRaises(ManagerError):
                    _fabric_predicate_matches(C58, predicate)

    def test_loader_four_component_wildcard_uses_exact_next_component_bounds(self) -> None:
        cases = {
            "1.2.3": True,
            "1.2.3.0": True,
            "1.2.3.99": True,
            "1.2.4-": False,
            "1.2.4-alpha": False,
            "1.3.0": False,
            "1.2.2.99": False,
        }
        for version, expected in cases.items():
            with self.subTest(version=version):
                self.assertEqual(expected, _fabric_predicate_matches(version, "1.2.3.x"))

    def test_loader_longer_component_wildcard_is_supported(self) -> None:
        self.assertTrue(_fabric_predicate_matches("1.2.3.4.99", "1.2.3.4.x"))
        self.assertFalse(_fabric_predicate_matches("1.2.3.5-", "1.2.3.4.x"))


class FabricDependencyGraphTests(unittest.TestCase):
    def setUp(self) -> None:
        self.bge_c58 = descriptor(
            "bge-c58.jar",
            "cnm_terrain_slabs_compat",
            C58,
            provides=("more_slabs_stairs_and_walls",),
            project_id="block-geometry-extensions",
        )
        self.trowel = descriptor(
            "trowel-canary9.jar",
            "shulker_trowel",
            "0.1.0-canary9",
            depends={"cnm_terrain_slabs_compat": (TROWEL_RANGE,)},
            project_id="shulker-trowel",
        )
        self.managed_ids = {
            "cnm_terrain_slabs_compat",
            "more_slabs_stairs_and_walls",
            "shulker_trowel",
        }

    def resolve(self, *items: FabricModDescriptor) -> dict:
        return PhysicalManager._resolve_dependency_graph(  # type: ignore[arg-type]
            None,
            items,
            managed_ownership_ids=set(self.managed_ids),
            phase="TEST",
        )

    def test_bounded_trowel_dependency_resolves_exact_c58_alias_owner(self) -> None:
        report = self.resolve(self.bge_c58, self.trowel)
        resolution = report["resolutions"][0]
        self.assertEqual("FABRIC_DEPENDENCY_GRAPH_VERIFIED", report["status"])
        self.assertEqual("RESOLVED_MANAGED_PROVIDER", resolution["classification"])
        self.assertEqual(C58, resolution["provider"]["version"])
        self.assertEqual(
            ["more_slabs_stairs_and_walls"],
            resolution["provider"]["provides"],
        )

    def test_metadata_array_predicates_are_or_not_and(self) -> None:
        trowel = descriptor(
            "trowel-or.jar",
            "shulker_trowel",
            "0.1.0-canary9",
            depends={"cnm_terrain_slabs_compat": ("=0.0.0-never", TROWEL_RANGE)},
            project_id="shulker-trowel",
        )
        resolution = self.resolve(self.bge_c58, trowel)["resolutions"][0]
        self.assertEqual([TROWEL_RANGE], resolution["matched_predicates"])

    def test_exact_c57_trowel_rejects_c58_and_names_companion(self) -> None:
        stale = descriptor(
            "trowel-canary8.jar",
            "shulker_trowel",
            "0.1.0-canary8",
            depends={"cnm_terrain_slabs_compat": ("=" + C57,)},
            project_id="shulker-trowel",
        )
        with self.assertRaisesRegex(
            ManagerError,
            r"shulker-trowel@0\.1\.0-canary8.*cnm_terrain_slabs_compat.*same atomic cohort",
        ):
            self.resolve(self.bge_c58, stale)

    def test_missing_managed_provider_fails_closed(self) -> None:
        with self.assertRaisesRegex(ManagerError, "missing managed Fabric dependency.*companion"):
            self.resolve(self.trowel)

    def test_missing_external_hard_dependency_also_fails_closed(self) -> None:
        consumer = descriptor(
            "external-consumer.jar",
            "external_consumer",
            "1.0.0",
            depends={"fabric-api": (">=0.100.0",)},
            project_id="external-consumer",
        )
        with self.assertRaisesRegex(
            ManagerError,
            r"missing external Fabric dependency.*fabric-api.*no exact enabled provider",
        ):
            self.resolve(consumer)

    def test_physically_present_unmanaged_external_provider_is_resolved(self) -> None:
        consumer = descriptor(
            "external-consumer.jar",
            "external_consumer",
            "1.0.0",
            depends={"fabric-api": (">=0.100.0",)},
            project_id="external-consumer",
        )
        fabric_api = descriptor("fabric-api.jar", "fabric-api", "0.120.0+26.2")
        resolution = self.resolve(consumer, fabric_api)["resolutions"][0]
        self.assertEqual("RESOLVED_EXTERNAL_ENABLED_PROVIDER", resolution["classification"])
        self.assertEqual("0.120.0+26.2", resolution["provider"]["version"])

    def test_c57_and_c58_duplicate_provider_fails_closed(self) -> None:
        bge_c57 = descriptor(
            "bge-c57.jar",
            "cnm_terrain_slabs_compat",
            C57,
            provides=("more_slabs_stairs_and_walls",),
            project_id="block-geometry-extensions",
        )
        with self.assertRaisesRegex(ManagerError, "duplicate enabled Fabric ownership cnm_terrain_slabs_compat"):
            self.resolve(bge_c57, self.bge_c58, self.trowel)

    def test_standalone_nibaru_beside_unified_bge_fails_closed(self) -> None:
        standalone = descriptor(
            "nibaru.jar",
            "more_slabs_stairs_and_walls",
            "1.0.0",
        )
        with self.assertRaisesRegex(ManagerError, "duplicate enabled Fabric ownership more_slabs_stairs_and_walls"):
            self.resolve(self.bge_c58, standalone, self.trowel)


class DependencyCohortTransitionTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.fixture = ManagerFixture(Path(self.temp.name))
        self.bge_uuid = stable_uuid("project:block-geometry-extensions")
        self.trowel_uuid = stable_uuid("project:shulker-trowel")
        self.fixture.project_index.update(
            {
                self.bge_uuid: "block-geometry-extensions",
                self.trowel_uuid: "shulker-trowel",
            }
        )
        self.fixture.project_display_names.update(
            {
                self.bge_uuid: "Block Geometry Extensions",
                self.trowel_uuid: "Shulker Trowel",
            }
        )
        self.fixture.manager.project_index = copy.deepcopy(self.fixture.project_index)
        self.fixture.manager.project_display_names = copy.deepcopy(self.fixture.project_display_names)

    def tearDown(self) -> None:
        self.temp.cleanup()

    def candidate(
        self,
        project_id: str,
        project_uuid: str,
        primary_id: str,
        version: str,
        *,
        provides: tuple[str, ...] = (),
        depends: dict[str, str | list[str]] | None = None,
    ) -> dict:
        filename = f"{project_id}-{version}.jar"
        source = self.fixture.repository / "artifacts" / filename
        sha256 = write_dependency_mod(
            source,
            primary_id,
            version,
            provides=provides,
            depends=depends,
        )
        return unit(
            project_id,
            version,
            artifact(
                filename,
                primary_id,
                sha256,
                ownership_mod_ids=[primary_id, *provides],
                source_type="REPOSITORY",
                source_path="artifacts/" + filename,
            ),
            project_uuid=project_uuid,
        )

    def provider(self, version: str) -> dict:
        return self.candidate(
            "block-geometry-extensions",
            self.bge_uuid,
            "cnm_terrain_slabs_compat",
            version,
            provides=("more_slabs_stairs_and_walls",),
        )

    def trowel(self, version: str, predicate: str) -> dict:
        return self.candidate(
            "shulker-trowel",
            self.trowel_uuid,
            "shulker_trowel",
            version,
            depends={"cnm_terrain_slabs_compat": predicate},
        )

    @staticmethod
    def deploy_operation(provider: dict, companion: dict) -> dict:
        return {
            "type": "DEPLOY_PROFILE",
            "slots": {
                "A": {"members": [candidate_declaration(provider), candidate_declaration(companion)]},
                "B": None,
            },
        }

    def test_exact_c57_pin_rejects_c58_in_real_dry_run_without_mutation(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        c58 = self.provider(C58)
        operation = self.deploy_operation(
            c58,
            self.trowel("0.1.0-canary8", "=" + C57),
        )
        before_state = self.fixture.repository_state()
        before_tree = tree_snapshot(self.fixture.root)

        with self.assertRaisesRegex(
            ManagerError,
            r"shulker-trowel@0\.1\.0-canary8.*cnm_terrain_slabs_compat.*same atomic cohort",
        ):
            self.fixture.manager.transition(
                operation=operation,
                expected_revision=before_state["revision"],
                at="2099-01-01T00:00:30Z",
                dry_run=True,
            )

        self.assertEqual(before_state, self.fixture.repository_state())
        self.assertEqual(before_tree, tree_snapshot(self.fixture.root))
        self.assertFalse((self.fixture.mods / c58["artifacts"][0]["filename"]).exists())

    def test_bounded_companion_allows_atomic_later_provider_update(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        c58 = self.provider(C58)
        companion = self.trowel("0.1.0-canary9", TROWEL_RANGE)
        state = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation=self.deploy_operation(c58, companion),
            expected_revision=state["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=False,
        )
        later = self.provider(COMPATIBLE_LATER)
        state = self.fixture.repository_state()
        before_companion = copy.deepcopy(state["slots"]["A"]["members"][1])
        result = self.fixture.manager.transition(
            operation={
                "type": "UPDATE_SLOT",
                "slot": "A",
                "candidate": candidate_declaration(later),
            },
            expected_revision=state["revision"],
            at="2099-01-01T00:00:31Z",
            dry_run=False,
        )

        after = self.fixture.repository_state()
        self.assertEqual(COMPATIBLE_LATER, after["slots"]["A"]["members"][0]["unit"]["version"])
        self.assertEqual(before_companion, after["slots"]["A"]["members"][1])
        resolution = next(
            item
            for item in result["dependency_resolution"]["resolutions"]
            if item["consumer"]["project_id"] == "shulker-trowel"
        )
        self.assertEqual(COMPATIBLE_LATER, resolution["provider"]["version"])
        self.assertEqual("PHYSICAL_STATE_VERIFIED", self.fixture.manager.verify()["status"])

    def test_upper_bound_provider_update_fails_without_mutation_unless_companion_is_replaced(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        c58 = self.provider(C58)
        companion = self.trowel("0.1.0-canary9", TROWEL_RANGE)
        state = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation=self.deploy_operation(c58, companion),
            expected_revision=state["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=False,
        )
        breaking = self.provider("4.3.0-bge.canary1.breaking+26.2")
        state = self.fixture.repository_state()
        before_tree = tree_snapshot(self.fixture.root)
        with self.assertRaisesRegex(ManagerError, r"shulker-trowel@0\.1\.0-canary9.*does not satisfy"):
            self.fixture.manager.transition(
                operation={
                    "type": "UPDATE_SLOT",
                    "slot": "A",
                    "candidate": candidate_declaration(breaking),
                },
                expected_revision=state["revision"],
                at="2099-01-01T00:00:31Z",
                dry_run=True,
            )
        self.assertEqual(state, self.fixture.repository_state())
        self.assertEqual(before_tree, tree_snapshot(self.fixture.root))

        compatible = self.trowel("0.1.0-canary10", ">=4.3.0- <5.0.0-")
        dry_run = self.fixture.manager.transition(
            operation={
                "type": "UPDATE_SLOT",
                "slot": "A",
                "members": [candidate_declaration(breaking), candidate_declaration(compatible)],
            },
            expected_revision=state["revision"],
            at="2099-01-01T00:00:31Z",
            dry_run=True,
        )
        self.assertEqual("FABRIC_DEPENDENCY_GRAPH_VERIFIED", dry_run["dependency_resolution"]["status"])


class AtomicCohortManagerTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.fixture = ManagerFixture(Path(self.temp.name))

    def tearDown(self) -> None:
        self.temp.cleanup()

    def operation(self) -> dict:
        c8, _ = self.fixture.add_repository_candidate()
        c3, _ = self.fixture.add_mossy_repository_candidate()
        return {
            "type": "DEPLOY_PROFILE",
            "slots": {
                "A": {
                    "members": [
                        candidate_declaration(c8, self.fixture.c5["deployment_id"]),
                        candidate_declaration(c3),
                    ]
                },
                "B": None,
            },
        }

    def test_one_revision_atomic_deploy_finalizes_shared_ready_cohort(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation = self.operation()
        before = self.fixture.repository_state()
        dry_run = self.fixture.manager.transition(
            operation=operation,
            expected_revision=before["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=True,
        )
        self.assertEqual(2, len(dry_run["slot_changes"]["A"]["after"]["members"]))
        self.assertIsNone(dry_run["slot_changes"]["B"]["after"])
        self.assertEqual("FABRIC_DEPENDENCY_GRAPH_VERIFIED", dry_run["dependency_resolution"]["status"])
        preimages = dry_run["displaced_member_runtime_result_preimages"]
        self.assertEqual(
            {self.fixture.c7["deployment_id"], self.fixture.mossy["deployment_id"]},
            {item["deployment_id"] for item in preimages},
        )
        self.assertTrue(all(item["runtime_result"]["classification"] == "FAIL" for item in preimages))
        self.assertTrue(all(item["runtime_result"]["evidence"]["failed"] for item in preimages))

        applied = self.fixture.manager.transition(
            operation=operation,
            expected_revision=before["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=False,
        )
        after = self.fixture.repository_state()
        self.assertEqual(before["revision"] + 1, after["revision"])
        self.assertEqual("READY_TO_TEST_VERIFIED", after["slots"]["A"]["deployment"]["state"])
        self.assertEqual(2, len(after["slots"]["A"]["members"]))
        self.assertIsNone(after["slots"]["B"])
        self.assertEqual("READY_TO_TEST_VERIFIED", applied["finalization"]["target_deployment_state"])

        receipt = self.fixture.manager.verify()
        self.assertEqual("PHYSICAL_STATE_VERIFIED", receipt["status"])
        self.assertEqual(2, receipt["slots"]["A"]["cohort_member_count"])
        self.assertEqual(
            {"matcha-heart-death-compat", "mossy-stone"},
            {member["project_id"] for member in receipt["slots"]["A"]["members"]},
        )
        for member in receipt["slots"]["A"]["members"]:
            self.assertEqual("A", member["slot"])
            self.assertEqual("READY_TO_TEST_VERIFIED", member["deployment_state"])
            self.assertEqual("UNTESTED", member["runtime_result"])
            self.assertEqual("CURRENT_RELEASE_DEPLOYED", member["current_release_comparison"]["classification"])
            self.assertEqual(1, len(member["artifacts"]))
            artifact_record = member["artifacts"][0]
            self.assertEqual(member["deployment_id"], artifact_record["deployment_id"])
            self.assertEqual(member["project_uuid"], artifact_record["project_uuid"])
            self.assertEqual(member["version"], artifact_record["expected_fabric_version"])
            self.assertEqual(member["version"], artifact_record["embedded_fabric_version"])
            self.assertEqual(64, len(artifact_record["sha256"]))
            self.assertTrue(artifact_record["filename"].endswith(".jar"))
        self.assertEqual(
            "Slot A: Matcha Death Rebalance - Canary 8 + Mossy Stone - Canary 3",
            receipt["title_display"]["lines"][1],
        )
        self.assertIsNone(receipt["slots"]["B"])

    def test_deploy_profile_rejects_declared_version_that_differs_from_embedded_version_before_mutation(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation = self.operation()
        operation["slots"]["A"]["members"][0]["unit"]["version"] = "0.1.7-canary999"
        before_state = self.fixture.repository_state()
        before_tree = tree_snapshot(self.fixture.root)

        with self.assertRaisesRegex(
            ManagerError,
            r"embedded Fabric version mismatch.*expected slot member version 0\.1\.7-canary999.*0\.1\.7-canary8",
        ):
            self.fixture.manager.transition(
                operation=operation,
                expected_revision=before_state["revision"],
                at="2099-01-01T00:00:30Z",
                dry_run=True,
            )

        self.assertEqual(before_state, self.fixture.repository_state())
        self.assertEqual(before_tree, tree_snapshot(self.fixture.root))

    def test_post_deployment_verification_rejects_slot_version_descriptor_mismatch(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation = self.operation()
        before = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation=operation,
            expected_revision=before["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=False,
        )

        state = self.fixture.repository_state()
        state["slots"]["A"]["members"][0]["unit"]["version"] = "0.1.7-canary999"
        self.fixture.state_path.write_text(json.dumps(state, indent=2) + "\n", encoding="utf-8")
        ledger = json.loads(self.fixture.manager.ledger_path.read_text(encoding="utf-8"))
        ledger["runtime_state"] = copy.deepcopy(state)
        ledger["state_digest"] = self.fixture.manager._title_projection(state)["state_digest"]
        ledger["title_projection"] = self.fixture.manager._title_projection(state)
        self.fixture.manager.ledger_path.write_text(json.dumps(ledger, indent=2) + "\n", encoding="utf-8")
        self.fixture.manager.title_projection_path.write_text(
            json.dumps(self.fixture.manager._title_projection(state), indent=2) + "\n",
            encoding="utf-8",
        )

        with self.assertRaisesRegex(ManagerError, "embedded Fabric version mismatch"):
            self.fixture.manager.verify()

    def test_member_update_preserves_and_receipts_unchanged_companion_result(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation = self.operation()
        before = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation=operation,
            expected_revision=before["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=False,
        )
        cohort = self.fixture.repository_state()["slots"]["A"]["members"]
        mossy_member = next(
            member for member in cohort if member["unit"]["project_uuid"] == self.fixture.mossy_uuid
        )
        state = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation={
                "type": "RECORD_RESULT",
                "slot": "A",
                "project_uuid": mossy_member["unit"]["project_uuid"],
                "classification": "PASS",
                "evidence": {"passed": ["companion result survives"], "failed": []},
            },
            expected_revision=state["revision"],
            at="2099-01-01T00:00:31Z",
            dry_run=False,
        )
        successor, _ = self.fixture.add_repository_candidate("0.1.8-canary9")
        state = self.fixture.repository_state()
        dry_run = self.fixture.manager.transition(
            operation={
                "type": "UPDATE_SLOT",
                "slot": "A",
                "candidate": candidate_declaration(successor, self.fixture.c5["deployment_id"]),
            },
            expected_revision=state["revision"],
            at="2099-01-01T00:00:32Z",
            dry_run=True,
        )
        preserved = dry_run["preserved_companion_runtime_results"]
        mossy_receipt = next(item for item in preserved if item["project_uuid"] == self.fixture.mossy_uuid)
        self.assertEqual("PASS", mossy_receipt["runtime_result"]["classification"])
        self.assertEqual(
            ["companion result survives"],
            mossy_receipt["runtime_result"]["evidence"]["passed"],
        )
        displaced = dry_run["displaced_member_runtime_result_preimages"]
        self.assertEqual(1, len(displaced))
        self.assertEqual("REPLACED", displaced[0]["disposition"])

    def test_cohort_write_failure_restores_every_member_and_state(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation = self.operation()
        before_state = self.fixture.repository_state()
        before_tree = tree_snapshot(self.fixture.root)

        def fail_after_first_write(stage: str) -> None:
            if stage == "after_write_1":
                raise RuntimeError("cohort fixture failure")

        with self.assertRaisesRegex(ManagerError, "rolled back"):
            self.fixture.manager.transition(
                operation=operation,
                expected_revision=before_state["revision"],
                at="2099-01-01T00:00:30Z",
                dry_run=False,
                failure_injector=fail_after_first_write,
            )
        self.assertEqual(before_tree, tree_snapshot(self.fixture.root))
        self.assertEqual(before_state, self.fixture.repository_state())

    def test_clearing_cohort_restores_accepted_predecessor_and_preserves_one_member_other_slot(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        c8, _ = self.fixture.add_repository_candidate()
        c3, _ = self.fixture.add_mossy_repository_candidate()
        gamma_uuid = stable_uuid("project:gamma-one-member")
        self.fixture.project_index[gamma_uuid] = "gamma-one-member"
        self.fixture.project_display_names[gamma_uuid] = "Gamma One Member"
        self.fixture.manager.project_index = copy.deepcopy(self.fixture.project_index)
        self.fixture.manager.project_display_names = copy.deepcopy(self.fixture.project_display_names)
        gamma_version = "0.1.0-canary1"
        gamma_name = f"gamma-one-member-{gamma_version}.jar"
        gamma_source = self.fixture.repository / "artifacts" / gamma_name
        gamma_hash = write_dependency_mod(gamma_source, "gamma_one_member", gamma_version)
        gamma = unit(
            "gamma-one-member",
            gamma_version,
            artifact(
                gamma_name,
                "gamma_one_member",
                gamma_hash,
                source_type="REPOSITORY",
                source_path="artifacts/" + gamma_name,
            ),
            project_uuid=gamma_uuid,
        )
        operation = {
            "type": "DEPLOY_PROFILE",
            "slots": {
                "A": {
                    "members": [
                        candidate_declaration(c8, self.fixture.c5["deployment_id"]),
                        candidate_declaration(c3),
                    ]
                },
                "B": {"candidate": candidate_declaration(gamma)},
            },
        }
        state = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation=operation,
            expected_revision=state["revision"],
            at="2099-01-01T00:00:30Z",
            dry_run=False,
        )
        deployed = self.fixture.repository_state()
        before_b = copy.deepcopy(deployed["slots"]["B"])
        gamma_bytes = (self.fixture.mods / gamma_name).read_bytes()
        c5_name = self.fixture.c5["artifacts"][0]["filename"]
        c8_name = c8["artifacts"][0]["filename"]
        self.assertTrue((self.fixture.mods / (c5_name + ".disabled")).is_file())
        self.assertFalse((self.fixture.mods / c5_name).exists())

        self.fixture.manager.transition(
            operation={"type": "REMOVE_SLOT", "slot": "A"},
            expected_revision=deployed["revision"],
            at="2099-01-01T00:00:31Z",
            dry_run=False,
        )

        cleared = self.fixture.repository_state()
        self.assertIsNone(cleared["slots"]["A"])
        self.assertEqual(before_b, cleared["slots"]["B"])
        self.assertEqual(self.fixture.c5_bytes, (self.fixture.mods / c5_name).read_bytes())
        self.assertFalse((self.fixture.mods / (c5_name + ".disabled")).exists())
        self.assertFalse((self.fixture.mods / c8_name).exists())
        self.assertEqual(gamma_bytes, (self.fixture.mods / gamma_name).read_bytes())
        receipt = self.fixture.manager.verify()
        self.assertEqual("PHYSICAL_STATE_VERIFIED", receipt["status"])
        self.assertEqual(1, receipt["slots"]["B"]["cohort_member_count"])
        self.assertEqual("gamma-one-member", receipt["slots"]["B"]["project_id"])


class ProfileUseGuardTests(unittest.TestCase):
    def test_named_profile_process_blocks_before_lock_or_other_mutation(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary) / "ModrinthApp" / "profiles"
            root.mkdir(parents=True)
            fixture = ManagerFixture(root)
            before = tree_snapshot(root)
            blocker = [{"pid": 4242, "name": "javaw.exe", "command_line": str(fixture.target)}]
            with patch(
                "tools.test_instance_manager.manager._windows_processes_using_profile",
                return_value=blocker,
            ):
                with self.assertRaisesRegex(ManagerError, r"actively in use.*javaw\.exe \(PID 4242\)"):
                    fixture.manager.adopt(dry_run=False)
            self.assertEqual(before, tree_snapshot(root))
            self.assertFalse(fixture.manager.lock_path.exists())


class CurrentReleaseComparisonTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.fixture = ManagerFixture(Path(self.temp.name))

    def tearDown(self) -> None:
        self.temp.cleanup()

    def test_older_slot_release_is_never_reported_as_current(self) -> None:
        unit = self.fixture.c7
        artifact = unit["artifacts"][0]
        current = {
            "identity": {"project_id": unit["project_id"]},
            "state": {
                "releases": {
                    "current": {
                        "version": "C8",
                        "source_commit": "f" * 40,
                        "artifact": {
                            "filename": "newer-current.jar",
                            "sha256": "e" * 64,
                        },
                    }
                }
            },
        }
        result = self.fixture.manager._current_release_comparison(
            unit,
            deployment_state="READY_TO_TEST_VERIFIED",
            statuses={unit["project_uuid"]: (Path("WORKBENCH_STATUS.json"), current)},
        )
        self.assertEqual("OLDER_RELEASE_DEPLOYED", result["classification"])
        self.assertFalse(result["slot_matches_current_release"])
        self.assertNotEqual(artifact["filename"], result["current_release"]["artifacts"][0]["filename"])

    def test_exact_current_identity_not_yet_deployed_is_explicit(self) -> None:
        unit = self.fixture.c7
        artifact = unit["artifacts"][0]
        current = {
            "identity": {"project_id": unit["project_id"]},
            "state": {
                "releases": {
                    "current": {
                        "version": "C7",
                        "source_commit": unit["source_commit"],
                        "artifact": {
                            "filename": artifact["filename"],
                            "sha256": artifact["sha256"],
                        },
                    }
                }
            },
        }
        result = self.fixture.manager._current_release_comparison(
            unit,
            deployment_state="NOT_DEPLOYED",
            statuses={unit["project_uuid"]: (Path("WORKBENCH_STATUS.json"), current)},
        )
        self.assertEqual("CURRENT_RELEASE_NOT_DEPLOYED", result["classification"])
        self.assertTrue(result["slot_matches_current_release"])


if __name__ == "__main__":
    unittest.main()
