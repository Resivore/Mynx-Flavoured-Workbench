from __future__ import annotations

import copy
import hashlib
import json
import stat
import tempfile
import unittest
import zipfile
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch
from uuid import NAMESPACE_URL, uuid5

from tools.runtime_slots import (
    candidate_declaration,
    plan_transition,
    render_title_state,
    state_digest,
    validate_runtime_state,
)
import tools.test_instance_manager.manager as manager_module
from tools.test_instance_manager.manager import ManagerError, PhysicalManager, _assert_no_reparse_components


T0 = "2026-08-29T12:00:00Z"
T1 = "2026-08-29T12:01:00Z"
T2 = "2026-08-29T12:02:00Z"
T3 = "2026-08-29T12:03:00Z"


def stable_uuid(value: str) -> str:
    return str(uuid5(NAMESPACE_URL, "physical-manager-test:" + value))


def write_mod(path: Path, mod_id: str, marker: str, *, provides: list[str] | None = None) -> str:
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr(
            "fabric.mod.json",
            json.dumps(
                {
                    "schemaVersion": 1,
                    "id": mod_id,
                    "version": marker,
                    "name": marker,
                    "provides": provides or [],
                }
            ),
        )
        archive.writestr("fixture.txt", marker)
    return hashlib.sha256(path.read_bytes()).hexdigest()


def artifact(
    name: str,
    mod_id: str,
    sha256: str,
    *,
    source_type: str = "ADOPTED_TARGET",
    source_path: str | None = None,
) -> dict:
    return {
        "artifact_id": stable_uuid("artifact:" + name + ":" + sha256),
        "kind": "MOD",
        "filename": name,
        "sha256": sha256,
        "ownership_keys": ["mod:" + mod_id],
        "source": {
            "type": source_type,
            "path": source_path or "mods/" + name,
        },
    }


def unit(project: str, version: str, item: dict, *, project_uuid: str | None = None) -> dict:
    return {
        "deployment_id": stable_uuid("deployment:" + project + ":" + version),
        "project_uuid": project_uuid or stable_uuid("project:" + project),
        "project_id": project,
        "project_identity_source": "CURRENT_MANIFEST",
        "version": version,
        "source_commit": hashlib.sha1((project + version).encode()).hexdigest(),
        "artifacts": [item],
    }


def failed_slot(item: dict, replacement: str | None = None) -> dict:
    return {
        "unit": item,
        "replaces_accepted_deployment_id": replacement,
        "deployment": {"state": "READY_TO_TEST_VERIFIED", "deployed_at": T0, "ready_verified_at": T1},
        "runtime_result": {
            "classification": "FAIL",
            "recorded_at": T2,
            "evidence": {"passed": ["implementation loads"], "failed": ["focused fixture failure"]},
        },
    }


class ManagerFixture:
    def __init__(self, root: Path):
        self.root = root
        self.repository = root / "repository"
        self.target = root / "Matcha Flavoured 26.2 Workbench"
        self.protected = root / "Matcha Flavoured 26.1.2"
        self.mods = self.target / "mods"
        self.repository.mkdir()
        self.mods.mkdir(parents=True)
        self.protected.mkdir()
        (self.mods / "unrelated.txt").write_text("preserve me", encoding="utf-8")

        base_name = "accepted-base.jar"
        c5_name = "matcha-heart-death-compat-0.1.4-canary5.jar"
        c7_name = "matcha-heart-death-compat-0.1.6-canary7.jar"
        mossy_name = "mossy-stone-0.2.0-canary2.jar"
        base_hash = write_mod(self.mods / base_name, "accepted_base", "base")
        c5_hash = write_mod(self.mods / (c5_name + ".disabled"), "matcha_heart_death_compat", "c5")
        c7_hash = write_mod(self.mods / c7_name, "matcha_heart_death_compat", "c7")
        mossy_hash = write_mod(self.mods / mossy_name, "mossy_stone", "c2")
        old_marker_name = "workbench-test-marker-0.1.1.jar"
        new_marker_name = "workbench-test-marker-0.2.0.jar"
        old_marker_hash = write_mod(self.mods / old_marker_name, "workbench_test_marker", "0.1.1")
        new_marker_path = self.repository / "infrastructure" / new_marker_name
        new_marker_hash = write_mod(new_marker_path, "workbench_test_marker", "0.2.0")
        self.base_bytes = (self.mods / base_name).read_bytes()
        self.c5_bytes = (self.mods / (c5_name + ".disabled")).read_bytes()
        self.c7_bytes = (self.mods / c7_name).read_bytes()
        self.c2_bytes = (self.mods / mossy_name).read_bytes()
        self.old_marker_name = old_marker_name
        self.new_marker_name = new_marker_name
        self.old_marker_hash = old_marker_hash
        self.new_marker_hash = new_marker_hash
        self.new_marker_bytes = new_marker_path.read_bytes()

        self.heart_uuid = stable_uuid("project:matcha-heart-death-compat")
        self.mossy_uuid = stable_uuid("project:mossy-stone")
        self.base = unit("accepted-base", "1.0.0", artifact(base_name, "accepted_base", base_hash))
        self.c5 = unit(
            "matcha-heart-death-compat",
            "0.1.4-canary5",
            artifact(c5_name, "matcha_heart_death_compat", c5_hash),
            project_uuid=self.heart_uuid,
        )
        self.c7 = unit(
            "matcha-heart-death-compat",
            "0.1.6-canary7",
            artifact(c7_name, "matcha_heart_death_compat", c7_hash),
            project_uuid=self.heart_uuid,
        )
        self.mossy = unit(
            "mossy-stone",
            "0.2.0-canary2",
            artifact(mossy_name, "mossy_stone", mossy_hash),
            project_uuid=self.mossy_uuid,
        )
        self.state = {
            "$schema": "../../schemas/runtime-state.schema.json",
            "schema_version": 1,
            "activation": "GATED",
            "revision": 10,
            "updated_at": T3,
            "accepted_baseline": {
                "revision": 4,
                "provenance": {
                    "source_repository": "Resivore/Minecraft-26.2-Workbench",
                    "source_commit": "f0101f38c446dddaa4226450386402c63597ba7c",
                    "source_profile": "Matcha Flavoured 26.2 Workbench",
                    "legacy_ledger_sha256": "1" * 64,
                    "accepted_artifact_count": 2,
                    "overlay_order": [self.heart_uuid, self.mossy_uuid],
                    "readiness": "READY_TO_TEST_VERIFIED",
                    "diagnostics": 0,
                    "physical_disposition": "PENDING",
                },
                "members": [
                    {"unit": self.base, "accepted_at": T0},
                    {"unit": self.c5, "accepted_at": T0},
                ],
            },
            "slots": {
                "A": failed_slot(self.c7, self.c5["deployment_id"]),
                "B": failed_slot(self.mossy),
            },
        }
        validate_runtime_state(self.state)
        self.state_path = self.repository / "runtime-state.json"
        self.state_path.write_text(json.dumps(self.state, indent=2) + "\n", encoding="utf-8")
        self.config_path = self.repository / "manager-config.json"
        config = {
            "schema_version": 3,
            "repository_root": ".",
            "runtime_state": "runtime-state.json",
            "dedicated_profile": str(self.target),
            "protected_profile": str(self.protected),
            "mods_directory": "mods",
            "ledger_file": ".mynx-runtime-v2-ledger.json",
            "lock_file": ".mynx-runtime-v2.lock",
            "title_display": {
                "projection_file": ".mynx-runtime-v2-title.json",
                "marker": {
                    "filename": new_marker_name,
                    "sha256": new_marker_hash,
                    "mod_id": "workbench_test_marker",
                    "source": "infrastructure/" + new_marker_name,
                },
                "predecessors": [
                    {
                        "filename": old_marker_name,
                        "sha256": old_marker_hash,
                        "mod_id": "workbench_test_marker",
                    }
                ],
            },
        }
        self.config_path.write_text(json.dumps(config, indent=2) + "\n", encoding="utf-8")
        self.project_index = {
            self.base["project_uuid"]: self.base["project_id"],
            self.heart_uuid: "matcha-heart-death-compat",
            self.mossy_uuid: "mossy-stone",
        }
        self.project_display_names = {
            self.base["project_uuid"]: "Accepted Base",
            self.heart_uuid: "Matcha Death Rebalance",
            self.mossy_uuid: "Mossy Stone",
        }
        self.manager = PhysicalManager.from_config(
            self.config_path,
            project_index=self.project_index,
            project_display_names=self.project_display_names,
        )
        self.operation_index = 0

    def repository_state(self) -> dict:
        return json.loads(self.state_path.read_text(encoding="utf-8"))

    def add_repository_candidate(self, version: str = "0.1.7-canary8") -> tuple[dict, Path]:
        filename = f"matcha-heart-death-compat-{version}.jar"
        path = self.repository / "artifacts" / filename
        sha256 = write_mod(path, "matcha_heart_death_compat", version)
        item = artifact(
            filename,
            "matcha_heart_death_compat",
            sha256,
            source_type="REPOSITORY",
            source_path="artifacts/" + filename,
        )
        return unit("matcha-heart-death-compat", version, item, project_uuid=self.heart_uuid), path

    def add_mossy_repository_candidate(self, version: str = "0.3.0-canary3") -> tuple[dict, Path]:
        filename = f"mossy-stone-{version}.jar"
        path = self.repository / "artifacts" / filename
        sha256 = write_mod(path, "mossy_stone", version)
        item = artifact(
            filename,
            "mossy_stone",
            sha256,
            source_type="REPOSITORY",
            source_path="artifacts/" + filename,
        )
        return unit("mossy-stone", version, item, project_uuid=self.mossy_uuid), path

    def add_user_passed_batch(
        self,
        *,
        predecessor: str = "exact",
    ) -> tuple[dict, dict, dict, dict, bytes]:
        first_uuid = stable_uuid("project:user-passed-first")
        rooted_uuid = stable_uuid("project:user-passed-rooted")
        self.project_index.update(
            {
                first_uuid: "user-passed-first",
                rooted_uuid: "user-passed-rooted",
            }
        )
        self.project_display_names.update(
            {
                first_uuid: "User Passed First",
                rooted_uuid: "User Passed Rooted",
            }
        )
        self.manager.project_index = copy.deepcopy(self.project_index)
        self.manager.project_display_names = copy.deepcopy(self.project_display_names)

        first_name = "user-passed-first-0.1.0-canary1.jar"
        first_source = self.repository / "artifacts" / first_name
        first_hash = write_mod(first_source, "user_passed_first", "first-c1")
        first = unit(
            "user-passed-first",
            "0.1.0-canary1",
            artifact(
                first_name,
                "user_passed_first",
                first_hash,
                source_type="REPOSITORY",
                source_path="artifacts/" + first_name,
            ),
            project_uuid=first_uuid,
        )

        rooted_name = "user-passed-rooted-0.1.0-canary1.jar"
        rooted_source = self.repository / "artifacts" / rooted_name
        rooted_hash = write_mod(rooted_source, "user_passed_rooted", "rooted-c1")
        rooted = unit(
            "user-passed-rooted",
            "0.1.0-canary1",
            artifact(
                rooted_name,
                "user_passed_rooted",
                rooted_hash,
                source_type="REPOSITORY",
                source_path="artifacts/" + rooted_name,
            ),
            project_uuid=rooted_uuid,
        )

        predecessor_name = "craft-rooted-dirt-107.1.jar"
        predecessor_path = self.mods / predecessor_name
        if predecessor == "exact":
            predecessor_hash = write_mod(predecessor_path, "mr_craft_rooteddirt", "107.1")
        else:
            exact_copy = self.root / "exact-predecessor.jar"
            predecessor_hash = write_mod(exact_copy, "mr_craft_rooteddirt", "107.1")
            if predecessor == "wrong":
                write_mod(predecessor_path, "mr_craft_rooteddirt", "wrong-predecessor")
            elif predecessor != "missing":
                raise AssertionError(f"unknown predecessor fixture mode: {predecessor}")
        rollback = unit(
            "user-passed-rooted",
            "107.1",
            artifact(predecessor_name, "mr_craft_rooteddirt", predecessor_hash),
            project_uuid=rooted_uuid,
        )
        rollback["project_identity_source"] = "FROZEN_LEGACY"
        predecessor_bytes = (
            predecessor_path.read_bytes()
            if predecessor == "exact"
            else (self.root / "exact-predecessor.jar").read_bytes()
        )
        operation = {
            "type": "PROMOTE_USER_PASSED_BATCH",
            "authorization": "USER_REPORTED_EXACT_RUNTIME_PASS",
            "members": [
                {"unit": first},
                {"unit": rooted, "retained_rollbacks": [rollback]},
            ],
        }
        return operation, first, rooted, rollback, predecessor_bytes

    def apply_operation(self, operation: dict) -> dict:
        self.operation_index += 1
        state = self.repository_state()
        at = f"2099-01-01T00:00:{self.operation_index:02d}Z"
        return self.manager.transition(
            operation=operation,
            expected_revision=state["revision"],
            at=at,
            dry_run=False,
        )

    def deploy_successor_pair(self, *, mark_ready: bool = True) -> tuple[dict, dict]:
        self.manager.adopt(dry_run=False)
        c8, _ = self.add_repository_candidate()
        c3, _ = self.add_mossy_repository_candidate()
        self.apply_operation(
            {
                "type": "UPDATE_SLOT",
                "slot": "A",
                "candidate": candidate_declaration(c8, self.c5["deployment_id"]),
            }
        )
        self.apply_operation(
            {
                "type": "UPDATE_SLOT",
                "slot": "B",
                "candidate": candidate_declaration(c3),
            }
        )
        self.apply_operation({"type": "MARK_DEPLOYED", "slot": "A"})
        self.apply_operation({"type": "MARK_DEPLOYED", "slot": "B"})
        if mark_ready:
            self.apply_operation({"type": "MARK_READY", "slot": "A"})
            self.apply_operation({"type": "MARK_READY", "slot": "B"})
        return c8, c3


def tree_snapshot(root: Path) -> dict[str, bytes]:
    return {
        path.relative_to(root).as_posix(): path.read_bytes()
        for path in sorted(root.rglob("*"))
        if path.is_file()
    }


class PhysicalManagerTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.fixture = ManagerFixture(Path(self.temporary.name))

    def test_protected_profile_is_refused_before_any_access(self) -> None:
        before = tree_snapshot(self.fixture.root)
        with self.assertRaisesRegex(ManagerError, "protected gameplay profile"):
            PhysicalManager.from_config(
                self.fixture.config_path,
                target=self.fixture.protected,
                project_index=self.fixture.project_index,
            )
        self.assertEqual(before, tree_snapshot(self.fixture.root))

    def test_reparse_target_is_refused(self) -> None:
        metadata = SimpleNamespace(st_mode=stat.S_IFDIR, st_file_attributes=0x400)
        with patch("tools.test_instance_manager.manager.os.lstat", return_value=metadata):
            with self.assertRaisesRegex(ManagerError, "symlink, junction, or reparse point"):
                _assert_no_reparse_components(self.fixture.target, "configured dedicated profile")

    def test_sha_mismatch_is_refused(self) -> None:
        candidate = self.fixture.mods / self.fixture.c7["artifacts"][0]["filename"]
        write_mod(candidate, "matcha_heart_death_compat", "corrupt-c7")
        with self.assertRaisesRegex(ManagerError, "SHA-256 mismatch"):
            self.fixture.manager.adopt(dry_run=True)
        self.assertFalse((self.fixture.target / ".mynx-runtime-v2-ledger.json").exists())
        self.assertEqual("GATED", self.fixture.repository_state()["activation"])

    def test_missing_repository_source_is_refused(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        missing_name = "matcha-heart-death-compat-0.1.7-canary8.jar"
        missing_hash = "a" * 64
        candidate = unit(
            "matcha-heart-death-compat",
            "0.1.7-canary8",
            artifact(
                missing_name,
                "matcha_heart_death_compat",
                missing_hash,
                source_type="REPOSITORY",
                source_path="artifacts/" + missing_name,
            ),
            project_uuid=self.fixture.heart_uuid,
        )
        state = self.fixture.repository_state()
        operation = {
            "type": "UPDATE_SLOT",
            "slot": "A",
            "candidate": candidate_declaration(candidate, self.fixture.c5["deployment_id"]),
        }
        with self.assertRaisesRegex(ManagerError, "missing artifact source"):
            self.fixture.manager.transition(operation=operation, expected_revision=state["revision"], at="2099-01-01T00:00:00Z")

    def test_live_transition_requires_current_manifest_identity(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        candidate, _ = self.fixture.add_repository_candidate()
        candidate["project_id"] = "mossy-stone"
        state = self.fixture.repository_state()
        operation = {
            "type": "UPDATE_SLOT",
            "slot": "A",
            "candidate": candidate_declaration(candidate),
        }
        with self.assertRaisesRegex(ManagerError, "does not match manifest project_id"):
            self.fixture.manager.transition(
                operation=operation,
                expected_revision=state["revision"],
                at="2099-01-01T00:00:00Z",
            )

    def test_dry_run_has_no_side_effects(self) -> None:
        before = tree_snapshot(self.fixture.root)
        result = self.fixture.manager.adopt(dry_run=True)
        self.assertTrue(result["dry_run"])
        self.assertEqual("ACTIVE", result["activation"])
        self.assertEqual(before, tree_snapshot(self.fixture.root))

    def test_transaction_residue_fails_closed(self) -> None:
        residue = self.fixture.target / ".mynx-runtime-v2-transaction-interrupted"
        residue.mkdir()
        with self.assertRaisesRegex(ManagerError, "explicit inspected recovery"):
            self.fixture.manager.adopt(dry_run=True)

    def test_adoption_records_target_ledger_and_suppresses_c5(self) -> None:
        slot_evidence = copy.deepcopy(self.fixture.state["slots"])
        result = self.fixture.manager.adopt(dry_run=False)
        state = self.fixture.repository_state()
        self.assertFalse(result["dry_run"])
        self.assertEqual("ACTIVE", state["activation"])
        self.assertEqual(self.fixture.state["revision"] + 1, state["revision"])
        self.assertEqual(self.fixture.state["accepted_baseline"]["revision"], state["accepted_baseline"]["revision"])
        self.assertEqual(slot_evidence, state["slots"])
        self.assertEqual("ADOPTED", state["accepted_baseline"]["provenance"]["physical_disposition"])
        c5 = self.fixture.c5["artifacts"][0]["filename"]
        c7 = self.fixture.c7["artifacts"][0]["filename"]
        mossy = self.fixture.mossy["artifacts"][0]["filename"]
        self.assertFalse((self.fixture.mods / c5).exists(), "accepted C5 must not remain active beside its candidate")
        self.assertTrue((self.fixture.mods / (c5 + ".disabled")).is_file())
        self.assertTrue((self.fixture.mods / c7).is_file())
        self.assertTrue((self.fixture.mods / mossy).is_file(), "Mossy Stone must overlay the baseline")
        self.assertFalse((self.fixture.mods / self.fixture.old_marker_name).exists())
        self.assertEqual(
            self.fixture.new_marker_bytes,
            (self.fixture.mods / self.fixture.new_marker_name).read_bytes(),
        )
        projection = json.loads((self.fixture.target / ".mynx-runtime-v2-title.json").read_text(encoding="utf-8"))
        self.assertEqual("Baseline: Stack v4", projection["lines"][0])
        self.assertEqual("Slot A: Matcha Death Rebalance - Canary 7", projection["lines"][1])
        self.assertEqual("Slot B: Mossy Stone - Canary 2", projection["lines"][2])
        self.assertEqual("preserve me", (self.fixture.mods / "unrelated.txt").read_text(encoding="utf-8"))
        self.assertEqual("PHYSICAL_STATE_VERIFIED", self.fixture.manager.verify()["status"])

    def test_legacy_v2_ledger_migrates_marker_and_projection_on_next_transition(self) -> None:
        active = self.fixture.repository_state()
        active["activation"] = "ACTIVE"
        active["revision"] += 1
        active["updated_at"] = "2026-08-29T12:04:00Z"
        active["accepted_baseline"]["provenance"]["physical_disposition"] = "ADOPTED"
        validate_runtime_state(active, self.fixture.project_index)
        self.fixture.state_path.write_text(json.dumps(active, indent=2) + "\n", encoding="utf-8")
        legacy_ledger = {
            "$schema": "mynx-test-instance-manager-ledger-v2",
            "schema_version": 2,
            "target": str(self.fixture.target),
            "state_revision": active["revision"],
            "state_digest": state_digest(active),
            "managed_files": [item.ledger_record() for item in self.fixture.manager.derive_inventory(active)],
            "runtime_state": copy.deepcopy(active),
        }
        (self.fixture.target / ".mynx-runtime-v2-ledger.json").write_text(
            json.dumps(legacy_ledger, indent=2) + "\n",
            encoding="utf-8",
        )
        self.assertFalse((self.fixture.target / ".mynx-runtime-v2-title.json").exists())
        c8, _ = self.fixture.add_repository_candidate()

        self.fixture.apply_operation(
            {
                "type": "UPDATE_SLOT",
                "slot": "A",
                "candidate": candidate_declaration(c8, self.fixture.c5["deployment_id"]),
            }
        )

        ledger = json.loads((self.fixture.target / ".mynx-runtime-v2-ledger.json").read_text(encoding="utf-8"))
        projection = json.loads((self.fixture.target / ".mynx-runtime-v2-title.json").read_text(encoding="utf-8"))
        self.assertEqual("mynx-test-instance-manager-ledger-v3", ledger["$schema"])
        self.assertEqual(3, ledger["schema_version"])
        self.assertFalse((self.fixture.mods / self.fixture.old_marker_name).exists())
        self.assertEqual(
            self.fixture.new_marker_bytes,
            (self.fixture.mods / self.fixture.new_marker_name).read_bytes(),
        )
        self.assertEqual("Baseline: Stack v4", projection["lines"][0])
        self.assertEqual("Slot A: Matcha Death Rebalance - Canary 8", projection["lines"][1])
        self.assertEqual("SYNCHRONIZED", self.fixture.manager.verify()["title_display"]["status"])

    def test_unmanaged_conflicting_candidate_is_refused(self) -> None:
        extra = self.fixture.mods / "unknown-heart-version.jar"
        write_mod(extra, "matcha_heart_death_compat", "unknown")
        with self.assertRaisesRegex(ManagerError, "unmanaged conflicting mod artifact"):
            self.fixture.manager.adopt(dry_run=True)
        self.assertTrue(extra.exists())

    def test_unmanaged_conflict_with_managed_provides_alias_is_refused(self) -> None:
        base_path = self.fixture.mods / self.fixture.base["artifacts"][0]["filename"]
        base_hash = write_mod(base_path, "accepted_base", "base-with-alias", provides=["accepted_alias"])
        self.fixture.base["artifacts"][0]["sha256"] = base_hash
        self.fixture.state["accepted_baseline"]["members"][0]["unit"]["artifacts"][0]["sha256"] = base_hash
        self.fixture.state_path.write_text(json.dumps(self.fixture.state, indent=2) + "\n", encoding="utf-8")
        write_mod(self.fixture.mods / "foreign-alias-owner.jar", "accepted_alias", "foreign")
        with self.assertRaisesRegex(ManagerError, "unmanaged conflicting mod artifact"):
            self.fixture.manager.adopt(dry_run=True)

    def test_unmanaged_disabled_fallback_is_preserved_and_not_an_active_conflict(self) -> None:
        fallback = self.fixture.mods / "unknown-heart-rollback.jar.disabled"
        sha256 = write_mod(fallback, "matcha_heart_death_compat", "legacy-rollback")
        before = tree_snapshot(self.fixture.root)
        result = self.fixture.manager.adopt(dry_run=True)
        self.assertTrue(result["dry_run"])
        self.assertEqual(before, tree_snapshot(self.fixture.root))
        self.fixture.manager.adopt(dry_run=False)
        self.assertTrue(fallback.is_file())
        self.assertEqual(sha256, hashlib.sha256(fallback.read_bytes()).hexdigest())

    def test_tracked_successors_with_old_physical_pair_fail_verification_and_readiness(self) -> None:
        c8, c3 = self.fixture.deploy_successor_pair(mark_ready=False)
        (self.fixture.mods / c8["artifacts"][0]["filename"]).unlink()
        (self.fixture.mods / c3["artifacts"][0]["filename"]).unlink()
        (self.fixture.mods / self.fixture.c7["artifacts"][0]["filename"]).write_bytes(self.fixture.c7_bytes)
        (self.fixture.mods / self.fixture.mossy["artifacts"][0]["filename"]).write_bytes(self.fixture.c2_bytes)

        with self.assertRaisesRegex(ManagerError, "missing artifact"):
            self.fixture.manager.verify()
        before = self.fixture.repository_state()
        with self.assertRaisesRegex(ManagerError, "missing artifact"):
            self.fixture.apply_operation({"type": "MARK_READY", "slot": "A"})
        after = self.fixture.repository_state()
        self.assertEqual(before, after)
        self.assertEqual("DEPLOYED", after["slots"]["A"]["deployment"]["state"])
        self.assertEqual("DEPLOYED", after["slots"]["B"]["deployment"]["state"])

    def test_exact_successor_pair_passes_with_physical_evidence_and_untouched_baseline(self) -> None:
        c8, c3 = self.fixture.deploy_successor_pair()
        result = self.fixture.manager.verify()

        self.assertEqual("PHYSICAL_STATE_VERIFIED", result["status"])
        self.assertEqual(5, result["managed_file_count"])
        self.assertRegex(result["physical_inventory_digest"], r"^[0-9a-f]{64}$")
        self.assertEqual(2, result["accepted_baseline"]["member_count"])
        self.assertEqual(2, result["accepted_baseline"]["artifact_count"])
        self.assertEqual(1, result["accepted_baseline"]["active_artifact_count"])
        self.assertEqual(1, result["accepted_baseline"]["disabled_artifact_count"])
        self.assertRegex(result["accepted_baseline"]["inventory_digest"], r"^[0-9a-f]{64}$")

        for label, expected in (("A", c8), ("B", c3)):
            evidence = result["slots"][label]
            self.assertEqual(expected["project_id"], evidence["project_id"])
            self.assertEqual(expected["version"], evidence["version"])
            self.assertEqual("READY_TO_TEST_VERIFIED", evidence["deployment_state"])
            self.assertEqual("UNTESTED", evidence["runtime_result"])
            self.assertEqual(1, len(evidence["artifacts"]))
            physical = evidence["artifacts"][0]
            self.assertEqual(expected["artifacts"][0]["filename"], physical["filename"])
            self.assertEqual(expected["artifacts"][0]["sha256"], physical["sha256"])
            self.assertEqual("mods/" + expected["artifacts"][0]["filename"], physical["path"])
            self.assertEqual("ACTIVE", physical["disposition"])

        base_name = self.fixture.base["artifacts"][0]["filename"]
        c5_name = self.fixture.c5["artifacts"][0]["filename"] + ".disabled"
        self.assertEqual(self.fixture.base_bytes, (self.fixture.mods / base_name).read_bytes())
        self.assertEqual(self.fixture.c5_bytes, (self.fixture.mods / c5_name).read_bytes())
        self.assertEqual("preserve me", (self.fixture.mods / "unrelated.txt").read_text(encoding="utf-8"))

    def test_stack_v1_and_empty_slots_render_without_unknown(self) -> None:
        state = copy.deepcopy(self.fixture.state)
        state["accepted_baseline"]["revision"] = 1
        state["slots"] = {"A": None, "B": None}

        rendered = render_title_state(
            state,
            self.fixture.project_display_names,
            self.fixture.project_index,
        )

        self.assertEqual(
            ["Baseline: Stack v1", "Slot A: Empty", "Slot B: Empty"],
            rendered["lines"],
        )
        self.assertNotIn("UNKNOWN", "\n".join(rendered["lines"]))

    def test_slot_replacement_refreshes_canonical_title_without_bumping_stack(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        before = self.fixture.repository_state()
        stack_version = before["accepted_baseline"]["revision"]
        c8, _ = self.fixture.add_repository_candidate()

        self.fixture.apply_operation(
            {
                "type": "UPDATE_SLOT",
                "slot": "A",
                "candidate": candidate_declaration(c8, self.fixture.c5["deployment_id"]),
            }
        )

        updated = self.fixture.repository_state()
        projection = json.loads((self.fixture.target / ".mynx-runtime-v2-title.json").read_text(encoding="utf-8"))
        self.assertEqual(stack_version, updated["accepted_baseline"]["revision"])
        self.assertEqual("Baseline: Stack v4", projection["lines"][0])
        self.assertEqual("Slot A: Matcha Death Rebalance - Canary 8", projection["lines"][1])
        self.assertEqual("Slot B: Mossy Stone - Canary 2", projection["lines"][2])

        self.fixture.apply_operation({"type": "MARK_DEPLOYED", "slot": "A"})
        self.fixture.apply_operation({"type": "MARK_READY", "slot": "A"})
        self.fixture.apply_operation(
            {
                "type": "RECORD_RESULT",
                "slot": "A",
                "classification": "FAIL",
                "evidence": {"passed": [], "failed": ["focused runtime defect"]},
            }
        )
        recorded = self.fixture.repository_state()
        self.assertEqual(stack_version, recorded["accepted_baseline"]["revision"])

        self.fixture.apply_operation({"type": "REMOVE_SLOT", "slot": "A"})
        cleared = self.fixture.repository_state()
        projection = json.loads((self.fixture.target / ".mynx-runtime-v2-title.json").read_text(encoding="utf-8"))
        self.assertEqual(stack_version, cleared["accepted_baseline"]["revision"])
        self.assertEqual("Slot A: Empty", projection["lines"][1])

    def test_genuine_promotion_increments_stack_and_projection_exactly_once(self) -> None:
        self.fixture.deploy_successor_pair()
        self.fixture.apply_operation(
            {
                "type": "RECORD_RESULT",
                "slot": "A",
                "classification": "PASS",
                "evidence": {"passed": ["focused runtime behavior passed"], "failed": []},
            }
        )
        before = self.fixture.repository_state()

        self.fixture.apply_operation({"type": "PROMOTE_SLOT", "slot": "A"})

        promoted = self.fixture.repository_state()
        projection = json.loads((self.fixture.target / ".mynx-runtime-v2-title.json").read_text(encoding="utf-8"))
        self.assertEqual(
            before["accepted_baseline"]["revision"] + 1,
            promoted["accepted_baseline"]["revision"],
        )
        self.assertEqual("TRANSITIONED", promoted["accepted_baseline"]["provenance"]["physical_disposition"])
        self.assertEqual("Baseline: Stack v5", projection["lines"][0])
        self.assertEqual("Slot A: Empty", projection["lines"][1])
        self.assertEqual("Slot B: Mossy Stone - Canary 3", projection["lines"][2])

    def test_byte_identical_promotion_preserves_accepted_identity_and_stack(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        filename = "matcha-heart-death-compat-byte-identical-canary8.jar"
        source = self.fixture.repository / "artifacts" / filename
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_bytes(self.fixture.c5_bytes)
        same_bytes = artifact(
            filename,
            "matcha_heart_death_compat",
            self.fixture.c5["artifacts"][0]["sha256"],
            source_type="REPOSITORY",
            source_path="artifacts/" + filename,
        )
        same_bytes["artifact_id"] = stable_uuid("artifact:byte-identical-heart-rebuild")
        candidate = unit(
            "matcha-heart-death-compat",
            "0.1.8-canary8",
            same_bytes,
            project_uuid=self.fixture.heart_uuid,
        )
        self.fixture.apply_operation(
            {
                "type": "UPDATE_SLOT",
                "slot": "A",
                "candidate": candidate_declaration(candidate, self.fixture.c5["deployment_id"]),
            }
        )
        self.fixture.apply_operation({"type": "MARK_DEPLOYED", "slot": "A"})
        self.fixture.apply_operation({"type": "MARK_READY", "slot": "A"})
        self.fixture.apply_operation(
            {
                "type": "RECORD_RESULT",
                "slot": "A",
                "classification": "PASS",
                "evidence": {"passed": ["byte-identical reconciliation confirmed"], "failed": []},
            }
        )
        before = self.fixture.repository_state()
        accepted_before = copy.deepcopy(before["accepted_baseline"])

        self.fixture.apply_operation({"type": "PROMOTE_SLOT", "slot": "A"})

        reconciled = self.fixture.repository_state()
        projection = json.loads((self.fixture.target / ".mynx-runtime-v2-title.json").read_text(encoding="utf-8"))
        self.assertEqual(accepted_before, reconciled["accepted_baseline"])
        self.assertIsNone(reconciled["slots"]["A"])
        self.assertEqual("Baseline: Stack v4", projection["lines"][0])
        self.assertEqual("Slot A: Empty", projection["lines"][1])
        self.assertEqual(
            self.fixture.c5_bytes,
            (self.fixture.mods / self.fixture.c5["artifacts"][0]["filename"]).read_bytes(),
        )
        self.assertFalse((self.fixture.mods / filename).exists())

    def test_intentional_accepted_removal_increments_stack_once(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        before = self.fixture.repository_state()

        self.fixture.apply_operation(
            {"type": "REMOVE_ACCEPTED", "project_uuid": self.fixture.base["project_uuid"]}
        )

        removed = self.fixture.repository_state()
        projection = json.loads((self.fixture.target / ".mynx-runtime-v2-title.json").read_text(encoding="utf-8"))
        self.assertEqual(
            before["accepted_baseline"]["revision"] + 1,
            removed["accepted_baseline"]["revision"],
        )
        self.assertNotIn(
            self.fixture.base["project_uuid"],
            {member["unit"]["project_uuid"] for member in removed["accepted_baseline"]["members"]},
        )
        self.assertEqual("Baseline: Stack v5", projection["lines"][0])

    def test_one_stale_slot_fails_physical_verification(self) -> None:
        _, c3 = self.fixture.deploy_successor_pair()
        (self.fixture.mods / c3["artifacts"][0]["filename"]).unlink()
        (self.fixture.mods / self.fixture.mossy["artifacts"][0]["filename"]).write_bytes(self.fixture.c2_bytes)
        with self.assertRaisesRegex(ManagerError, "missing artifact"):
            self.fixture.manager.verify()

    def test_superseded_old_artifact_remaining_active_fails(self) -> None:
        self.fixture.deploy_successor_pair()
        old = self.fixture.mods / self.fixture.c7["artifacts"][0]["filename"]
        old.write_bytes(self.fixture.c7_bytes)
        with self.assertRaisesRegex(ManagerError, "unmanaged conflicting mod artifact"):
            self.fixture.manager.verify()

    def test_correct_successor_filename_with_wrong_hash_fails(self) -> None:
        c8, _ = self.fixture.deploy_successor_pair()
        active = self.fixture.mods / c8["artifacts"][0]["filename"]
        write_mod(active, "matcha_heart_death_compat", "tampered-c8")
        with self.assertRaisesRegex(ManagerError, "SHA-256 mismatch"):
            self.fixture.manager.verify()

    def test_legacy_marker_blocks_normal_operations_and_retires_exactly(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        marker = self.fixture.target / ".workbench-instance-manager.json"
        retired = self.fixture.target / ".workbench-instance-manager.v1-retired.json"
        marker_bytes = b'{"test_set":"Heart C7 + Mossy C2"}\n'
        marker.write_bytes(marker_bytes)
        before_tree = tree_snapshot(self.fixture.root)
        before_mods = tree_snapshot(self.fixture.mods)
        before_state = self.fixture.state_path.read_bytes()
        before_ledger = (self.fixture.target / ".mynx-runtime-v2-ledger.json").read_bytes()

        with self.assertRaisesRegex(ManagerError, "legacy V1 display marker"):
            self.fixture.manager.verify()
        state = self.fixture.repository_state()
        with self.assertRaisesRegex(ManagerError, "legacy V1 display marker"):
            self.fixture.manager.transition(
                operation={"type": "REMOVE_SLOT", "slot": "A"},
                expected_revision=state["revision"],
                at="2099-01-01T00:00:59Z",
                dry_run=True,
            )

        dry_run = self.fixture.manager.retire_legacy_marker(dry_run=True)
        self.assertEqual("LEGACY_MARKER_RETIREMENT_READY", dry_run["status"])
        self.assertEqual(hashlib.sha256(marker_bytes).hexdigest(), dry_run["sha256"])
        self.assertEqual("PHYSICAL_STATE_VERIFIED", dry_run["physical_verification"]["status"])
        self.assertEqual(before_tree, tree_snapshot(self.fixture.root))

        applied = self.fixture.manager.retire_legacy_marker(dry_run=False)
        self.assertEqual("LEGACY_MARKER_RETIRED", applied["status"])
        self.assertFalse(marker.exists())
        self.assertEqual(marker_bytes, retired.read_bytes())
        self.assertEqual(before_mods, tree_snapshot(self.fixture.mods))
        self.assertEqual(before_state, self.fixture.state_path.read_bytes())
        self.assertEqual(before_ledger, (self.fixture.target / ".mynx-runtime-v2-ledger.json").read_bytes())
        verified = self.fixture.manager.verify()
        self.assertEqual("PHYSICAL_STATE_VERIFIED", verified["status"])
        self.assertEqual("SYNCHRONIZED", verified["title_display"]["status"])
        self.assertEqual("Baseline: Stack v4", verified["title_display"]["lines"][0])
        self.assertEqual("Slot A: Matcha Death Rebalance - Canary 7", verified["title_display"]["lines"][1])

    def test_legacy_marker_retirement_requires_matching_physical_inventory(self) -> None:
        c8, c3 = self.fixture.deploy_successor_pair(mark_ready=False)
        (self.fixture.mods / c8["artifacts"][0]["filename"]).unlink()
        (self.fixture.mods / c3["artifacts"][0]["filename"]).unlink()
        (self.fixture.mods / self.fixture.c7["artifacts"][0]["filename"]).write_bytes(self.fixture.c7_bytes)
        (self.fixture.mods / self.fixture.mossy["artifacts"][0]["filename"]).write_bytes(self.fixture.c2_bytes)
        marker = self.fixture.target / ".workbench-instance-manager.json"
        marker.write_bytes(b"stale display marker\n")
        before = tree_snapshot(self.fixture.root)

        with self.assertRaisesRegex(ManagerError, "missing artifact"):
            self.fixture.manager.retire_legacy_marker(dry_run=False)
        self.assertEqual(before, tree_snapshot(self.fixture.root))
        self.assertTrue(marker.is_file())
        self.assertFalse((self.fixture.target / ".workbench-instance-manager.v1-retired.json").exists())

    def test_legacy_marker_retirement_refuses_differing_preserved_bytes(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        marker = self.fixture.target / ".workbench-instance-manager.json"
        retired = self.fixture.target / ".workbench-instance-manager.v1-retired.json"
        marker.write_bytes(b"old active marker\n")
        retired.write_bytes(b"different retired marker\n")
        before = tree_snapshot(self.fixture.root)
        with self.assertRaisesRegex(ManagerError, "differing bytes"):
            self.fixture.manager.retire_legacy_marker(dry_run=False)
        self.assertEqual(before, tree_snapshot(self.fixture.root))

    def test_legacy_marker_retirement_reuses_identical_preserved_bytes(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        marker = self.fixture.target / ".workbench-instance-manager.json"
        retired = self.fixture.target / ".workbench-instance-manager.v1-retired.json"
        marker_bytes = b"already preserved marker\n"
        marker.write_bytes(marker_bytes)
        retired.write_bytes(marker_bytes)

        result = self.fixture.manager.retire_legacy_marker(dry_run=False)
        self.assertTrue(result["already_preserved"])
        self.assertFalse(marker.exists())
        self.assertEqual(marker_bytes, retired.read_bytes())
        self.assertEqual("PHYSICAL_STATE_VERIFIED", self.fixture.manager.verify()["status"])

    def test_single_slot_update_and_clear_preserve_slot_b_and_baseline(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        c8, _ = self.fixture.add_repository_candidate()
        before = self.fixture.repository_state()
        baseline = copy.deepcopy(before["accepted_baseline"])
        slot_b = copy.deepcopy(before["slots"]["B"])
        update = {
            "type": "UPDATE_SLOT",
            "slot": "A",
            "candidate": candidate_declaration(c8, self.fixture.c5["deployment_id"]),
        }
        self.fixture.manager.transition(
            operation=update,
            expected_revision=before["revision"],
            at="2099-01-01T00:00:00Z",
            dry_run=False,
        )
        updated = self.fixture.repository_state()
        self.assertEqual(slot_b, updated["slots"]["B"])
        self.assertEqual(baseline, updated["accepted_baseline"])
        self.assertEqual("UNTESTED", updated["slots"]["A"]["runtime_result"]["classification"])
        self.assertFalse((self.fixture.mods / self.fixture.c7["artifacts"][0]["filename"]).exists())
        self.assertTrue((self.fixture.mods / c8["artifacts"][0]["filename"]).exists())

        self.fixture.manager.transition(
            operation={"type": "REMOVE_SLOT", "slot": "A"},
            expected_revision=updated["revision"],
            at="2099-01-01T00:00:01Z",
            dry_run=False,
        )
        cleared = self.fixture.repository_state()
        self.assertIsNone(cleared["slots"]["A"])
        self.assertEqual(slot_b, cleared["slots"]["B"])
        self.assertEqual(baseline, cleared["accepted_baseline"])
        c5_name = self.fixture.c5["artifacts"][0]["filename"]
        self.assertTrue((self.fixture.mods / c5_name).exists())
        self.assertFalse((self.fixture.mods / (c5_name + ".disabled")).exists())
        self.assertEqual("PHYSICAL_STATE_VERIFIED", self.fixture.manager.verify()["status"])

    def test_adopted_rollback_can_share_successor_filename(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        filename = self.fixture.c5["artifacts"][0]["filename"]
        source = self.fixture.repository / "artifacts" / filename
        sha256 = write_mod(source, "matcha_heart_death_compat", "same-name-successor")
        candidate = unit(
            "matcha-heart-death-compat",
            "0.1.7-canary8",
            artifact(
                filename,
                "matcha_heart_death_compat",
                sha256,
                source_type="REPOSITORY",
                source_path="artifacts/" + filename,
            ),
            project_uuid=self.fixture.heart_uuid,
        )
        state = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation={"type": "UPDATE_SLOT", "slot": "A", "candidate": candidate_declaration(candidate)},
            expected_revision=state["revision"],
            at="2099-01-01T00:00:00Z",
            dry_run=False,
        )
        self.assertEqual(sha256, hashlib.sha256((self.fixture.mods / filename).read_bytes()).hexdigest())
        self.assertTrue((self.fixture.mods / (filename + ".disabled")).is_file())
        self.assertEqual("PHYSICAL_STATE_VERIFIED", self.fixture.manager.verify()["status"])

    def test_prevalidated_desired_state_must_be_exact_pure_transition_output(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        c8, _ = self.fixture.add_repository_candidate()
        current = self.fixture.repository_state()
        operation = {
            "type": "UPDATE_SLOT",
            "slot": "A",
            "candidate": candidate_declaration(c8, self.fixture.c5["deployment_id"]),
        }
        desired = plan_transition(current, current["revision"], operation, "2099-01-01T00:00:00Z")
        result = self.fixture.manager.transition(desired_state=desired, dry_run=True)
        self.assertTrue(result["dry_run"])
        self.assertEqual(current, self.fixture.repository_state())

        tampered = copy.deepcopy(desired)
        tampered["accepted_baseline"]["provenance"]["source_profile"] = "tampered but schema-valid"
        validate_runtime_state(tampered)
        with self.assertRaisesRegex(ManagerError, "exact output"):
            self.fixture.manager.transition(desired_state=tampered, dry_run=True)

    def test_prevalidated_desired_state_cannot_fabricate_untested_promotion_authorization(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        c8, _ = self.fixture.add_repository_candidate()
        self.fixture.apply_operation(
            {
                "type": "UPDATE_SLOT",
                "slot": "A",
                "candidate": candidate_declaration(c8, self.fixture.c5["deployment_id"]),
            }
        )
        c10, _ = self.fixture.add_repository_candidate("0.1.9-canary10")
        current = self.fixture.repository_state()
        operation = {
            "type": "PROMOTE_UNTESTED_CANDIDATE",
            "authorization": "USER_APPROVED_UNTESTED_PROMOTION",
            "candidate": candidate_declaration(c10, self.fixture.c5["deployment_id"]),
        }
        desired = plan_transition(
            current,
            current["revision"],
            operation,
            "2099-01-01T00:00:02Z",
            self.fixture.project_index,
        )
        with self.assertRaisesRegex(ManagerError, "exact output"):
            self.fixture.manager.transition(desired_state=desired, dry_run=True)
        self.assertEqual(current, self.fixture.repository_state())

    def test_prevalidated_desired_state_preserves_slot_dependency_overrides(self) -> None:
        current = self.fixture.repository_state()
        bge_uuid = stable_uuid("project:block-geometry-extensions")
        bge = unit(
            "block-geometry-extensions",
            "0.6.0-bge-canary53-layer",
            artifact(
                "bge-canary53.jar",
                "bge",
                "a" * 64,
                source_type="REPOSITORY",
                source_path="artifacts/bge-canary53.jar",
            ),
            project_uuid=bge_uuid,
        )
        bge["artifacts"].append(
            artifact(
                "heart-dependency-successor.jar",
                "matcha_heart_death_compat",
                "b" * 64,
                source_type="REPOSITORY",
                source_path="artifacts/heart-dependency-successor.jar",
            )
        )
        project_index = {**self.fixture.project_index, bge_uuid: "block-geometry-extensions"}
        desired = plan_transition(
            current,
            current["revision"],
            {
                "type": "SET_PROFILE",
                "candidates": [
                    candidate_declaration(
                        bge,
                        dependency_overrides=[self.fixture.c5["deployment_id"]],
                    )
                ],
            },
            "2099-01-01T00:00:00Z",
            project_index,
        )
        self.fixture.manager.project_index = project_index
        self.fixture.manager._assert_desired_is_pure_transition(current, desired)

    def test_user_passed_batch_dry_run_and_apply_preserve_slots_and_retain_rollback(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation, first, rooted, rollback, predecessor_bytes = self.fixture.add_user_passed_batch()
        before = self.fixture.repository_state()
        before_slots = copy.deepcopy(before["slots"])
        before_tree = tree_snapshot(self.fixture.root)
        predecessor_name = rollback["artifacts"][0]["filename"]
        predecessor_path = self.fixture.mods / predecessor_name
        disabled_path = self.fixture.mods / (predecessor_name + ".disabled")

        dry_run = self.fixture.manager.transition(
            operation=operation,
            expected_revision=before["revision"],
            at="2099-01-01T00:01:00Z",
            dry_run=True,
        )

        self.assertTrue(dry_run["dry_run"])
        self.assertEqual(before_tree, tree_snapshot(self.fixture.root))
        self.assertEqual(1, len(dry_run["retained_rollbacks"]))
        self.assertEqual("mods/" + predecessor_name, dry_run["retained_rollbacks"][0]["enabled_sibling"])
        self.assertNotIn(predecessor_name, dry_run["removals"])
        self.assertEqual(
            [
                {
                    "source": str(predecessor_path),
                    "path": "mods/" + predecessor_name + ".disabled",
                    "sha256": rollback["artifacts"][0]["sha256"],
                }
            ],
            dry_run["retained_predecessor_moves"],
        )
        self.assertIn(
            "mods/" + predecessor_name + ".disabled",
            {item["path"] for item in dry_run["managed_files"]},
        )

        original_replace = manager_module.os.replace
        with patch.object(manager_module.os, "replace", wraps=original_replace) as replace_spy:
            applied = self.fixture.manager.transition(
                operation=operation,
                expected_revision=before["revision"],
                at="2099-01-01T00:01:00Z",
                dry_run=False,
            )
        move_calls = [
            (Path(call.args[0]), Path(call.args[1]))
            for call in replace_spy.call_args_list
            if len(call.args) >= 2
        ]
        self.assertIn((predecessor_path, disabled_path), move_calls)

        after = self.fixture.repository_state()
        self.assertFalse(applied["dry_run"])
        self.assertEqual(before["revision"] + 1, after["revision"])
        self.assertEqual(before["accepted_baseline"]["revision"] + 2, after["accepted_baseline"]["revision"])
        self.assertEqual(
            len(before["accepted_baseline"]["members"]) + 2,
            len(after["accepted_baseline"]["members"]),
        )
        self.assertEqual(
            before["accepted_baseline"]["provenance"]["accepted_artifact_count"] + 2,
            after["accepted_baseline"]["provenance"]["accepted_artifact_count"],
        )
        self.assertEqual(before_slots, after["slots"])
        self.assertTrue((self.fixture.mods / first["artifacts"][0]["filename"]).is_file())
        self.assertTrue((self.fixture.mods / rooted["artifacts"][0]["filename"]).is_file())
        self.assertFalse(predecessor_path.exists())
        self.assertEqual(predecessor_bytes, disabled_path.read_bytes())
        projection = json.loads((self.fixture.target / ".mynx-runtime-v2-title.json").read_text(encoding="utf-8"))
        self.assertEqual("Baseline: Stack v6", projection["lines"][0])
        verification = self.fixture.manager.verify()
        retained = verification["accepted_baseline"]["retained_rollbacks"]
        self.assertEqual(1, retained["artifact_count"])
        self.assertTrue(retained["enabled_siblings_absent"])
        self.assertEqual("DISABLED", retained["artifacts"][0]["disposition"])

    def test_user_passed_batch_refuses_wrong_predecessor_bytes(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation, _, _, _, _ = self.fixture.add_user_passed_batch(predecessor="wrong")
        state = self.fixture.repository_state()
        with self.assertRaisesRegex(ManagerError, "SHA-256 mismatch"):
            self.fixture.manager.transition(
                operation=operation,
                expected_revision=state["revision"],
                at="2099-01-01T00:01:00Z",
                dry_run=True,
            )

    def test_user_passed_batch_refuses_missing_predecessor(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation, _, _, _, _ = self.fixture.add_user_passed_batch(predecessor="missing")
        state = self.fixture.repository_state()
        with self.assertRaisesRegex(ManagerError, "missing artifact source"):
            self.fixture.manager.transition(
                operation=operation,
                expected_revision=state["revision"],
                at="2099-01-01T00:01:00Z",
                dry_run=True,
            )

    def test_user_passed_batch_production_identity_is_bound_to_manifest_releases(self) -> None:
        operation, _, rooted, rollback, _ = self.fixture.add_user_passed_batch()
        member = copy.deepcopy(operation["members"][1])
        project_uuid = rooted["project_uuid"]
        project_directory = self.fixture.repository / "projects" / rooted["project_id"]
        manifest_path = project_directory / "WORKBENCH_STATUS.json"
        canonical_source = (
            "projects/" + rooted["project_id"] + "/artifacts/" + rooted["artifacts"][0]["filename"]
        )
        member["unit"]["artifacts"][0]["source"] = {
            "type": "REPOSITORY",
            "path": canonical_source,
        }
        manifest = {
            "identity": {
                "uuid": project_uuid,
                "project_id": rooted["project_id"],
            },
            "state": {
                "releases": {
                    "current": {
                        "version": rooted["version"],
                        "source_commit": rooted["source_commit"],
                        "artifact": {
                            "filename": rooted["artifacts"][0]["filename"],
                            "sha256": rooted["artifacts"][0]["sha256"],
                        },
                    },
                    "rollback": {
                        "version": rollback["version"],
                        "source_commit": rollback["source_commit"],
                        "artifact": {
                            "filename": rollback["artifacts"][0]["filename"],
                            "sha256": rollback["artifacts"][0]["sha256"],
                        },
                    },
                }
            },
        }
        self.fixture.manager.repository_statuses = {project_uuid: (manifest_path, manifest)}
        exact_operation = {
            "type": "PROMOTE_USER_PASSED_BATCH",
            "authorization": "USER_REPORTED_EXACT_RUNTIME_PASS",
            "members": [member],
        }
        self.fixture.manager._validate_batch_manifest_identities(exact_operation)

        no_manifest_rollback = copy.deepcopy(manifest)
        no_manifest_rollback["state"]["releases"]["rollback"] = None
        self.fixture.manager.repository_statuses = {project_uuid: (manifest_path, no_manifest_rollback)}
        with self.assertRaisesRegex(ManagerError, "manifest has no rollback release"):
            self.fixture.manager._validate_batch_manifest_identities(exact_operation)
        self.fixture.manager.repository_statuses = {project_uuid: (manifest_path, manifest)}

        missing_retained_rollback = copy.deepcopy(exact_operation)
        missing_retained_rollback["members"][0].pop("retained_rollbacks")
        self.fixture.manager._validate_batch_manifest_identities(missing_retained_rollback)

        wrong_candidate = copy.deepcopy(exact_operation)
        wrong_candidate["members"][0]["unit"]["version"] = "0.1.1-canary2"
        with self.assertRaisesRegex(ManagerError, "does not exactly match manifest release identity"):
            self.fixture.manager._validate_batch_manifest_identities(wrong_candidate)

    def test_user_passed_batch_manifest_drift_before_commit_aborts_and_rolls_back(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation, first, rooted, rollback, _ = self.fixture.add_user_passed_batch()
        manifest_path = self.fixture.repository / "WORKBENCH_STATUS.json"

        def manifest_for(current: dict, rollback_unit: dict | None = None) -> dict:
            current_artifact = current["artifacts"][0]
            rollback_release = None
            if rollback_unit is not None:
                rollback_artifact = rollback_unit["artifacts"][0]
                rollback_release = {
                    "version": rollback_unit["version"],
                    "source_commit": rollback_unit["source_commit"],
                    "artifact": {
                        "filename": rollback_artifact["filename"],
                        "sha256": rollback_artifact["sha256"],
                    },
                }
            return {
                "identity": {
                    "uuid": current["project_uuid"],
                    "project_id": current["project_id"],
                },
                "state": {
                    "releases": {
                        "current": {
                            "version": current["version"],
                            "source_commit": current["source_commit"],
                            "artifact": {
                                "filename": current_artifact["filename"],
                                "sha256": current_artifact["sha256"],
                            },
                        },
                        "rollback": rollback_release,
                    }
                },
            }

        initial_catalog = {
            first["project_uuid"]: (manifest_path, manifest_for(first)),
            rooted["project_uuid"]: (manifest_path, manifest_for(rooted, rollback)),
        }
        drifted_catalog = copy.deepcopy(initial_catalog)
        drifted_catalog[first["project_uuid"]][1]["state"]["releases"]["current"]["version"] = (
            "0.1.1-concurrent"
        )
        self.fixture.manager.repository_statuses = copy.deepcopy(initial_catalog)
        state = self.fixture.repository_state()
        before = tree_snapshot(self.fixture.root)

        with patch(
            "tools.test_instance_manager.manager.load_repository_statuses",
            side_effect=[copy.deepcopy(initial_catalog), drifted_catalog],
        ) as reload_statuses:
            with self.assertRaisesRegex(ManagerError, "does not exactly match manifest release identity"):
                self.fixture.manager.transition(
                    operation=operation,
                    expected_revision=state["revision"],
                    at="2099-01-01T00:01:00Z",
                    dry_run=False,
                )

        self.assertEqual(2, reload_statuses.call_count)
        self.assertEqual(before, tree_snapshot(self.fixture.root))
        self.assertEqual("PHYSICAL_STATE_VERIFIED", self.fixture.manager.verify()["status"])

    def test_runtime_state_cas_drift_is_not_overwritten_during_batch_rollback(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation, _, _, _, _ = self.fixture.add_user_passed_batch()
        state = self.fixture.repository_state()
        concurrent = plan_transition(
            state,
            state["revision"],
            {"type": "REMOVE_SLOT", "slot": "B"},
            "2099-01-01T00:00:59Z",
            self.fixture.project_index,
        )
        before_target = tree_snapshot(self.fixture.target)

        def inject_concurrent_runtime_state(stage: str) -> None:
            if stage == "after_physical_verify":
                self.fixture.state_path.write_text(
                    json.dumps(concurrent, indent=2) + "\n",
                    encoding="utf-8",
                )

        with self.assertRaisesRegex(ManagerError, "repository runtime state changed"):
            self.fixture.manager.transition(
                operation=operation,
                expected_revision=state["revision"],
                at="2099-01-01T00:01:00Z",
                dry_run=False,
                failure_injector=inject_concurrent_runtime_state,
            )

        self.assertEqual(concurrent, self.fixture.repository_state())
        self.assertEqual(before_target, tree_snapshot(self.fixture.target))

    def test_retained_rollback_active_reappearance_fails_verification(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation, _, _, rollback, _ = self.fixture.add_user_passed_batch()
        state = self.fixture.repository_state()
        self.fixture.manager.transition(
            operation=operation,
            expected_revision=state["revision"],
            at="2099-01-01T00:01:00Z",
            dry_run=False,
        )
        predecessor_name = rollback["artifacts"][0]["filename"]
        (self.fixture.mods / predecessor_name).write_bytes(
            (self.fixture.mods / (predecessor_name + ".disabled")).read_bytes()
        )
        with self.assertRaisesRegex(ManagerError, "enabled sibling must be absent"):
            self.fixture.manager.verify()

    def test_user_passed_batch_failure_after_atomic_move_restores_full_preimage(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        operation, _, _, _, _ = self.fixture.add_user_passed_batch()
        state = self.fixture.repository_state()
        before = tree_snapshot(self.fixture.root)

        def fail_after_predecessor_move(stage: str) -> None:
            if stage == "after_move_1":
                raise RuntimeError("injected batch failure")

        with self.assertRaisesRegex(ManagerError, "rolled back"):
            self.fixture.manager.transition(
                operation=operation,
                expected_revision=state["revision"],
                at="2099-01-01T00:01:00Z",
                dry_run=False,
                failure_injector=fail_after_predecessor_move,
            )
        self.assertEqual(before, tree_snapshot(self.fixture.root))
        self.assertEqual("PHYSICAL_STATE_VERIFIED", self.fixture.manager.verify()["status"])

    def test_injected_partial_failure_rolls_back_files_ledgers_and_state(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        c8, _ = self.fixture.add_repository_candidate()
        before = tree_snapshot(self.fixture.root)
        state = self.fixture.repository_state()
        operation = {
            "type": "UPDATE_SLOT",
            "slot": "A",
            "candidate": candidate_declaration(c8, self.fixture.c5["deployment_id"]),
        }

        def fail_after_first_write(stage: str) -> None:
            if stage == "after_write_1":
                raise RuntimeError("injected partial failure")

        with self.assertRaisesRegex(ManagerError, "rolled back"):
            self.fixture.manager.transition(
                operation=operation,
                expected_revision=state["revision"],
                at="2099-01-01T00:00:00Z",
                dry_run=False,
                failure_injector=fail_after_first_write,
            )
        self.assertEqual(before, tree_snapshot(self.fixture.root))
        self.assertEqual("PHYSICAL_STATE_VERIFIED", self.fixture.manager.verify()["status"])
        self.assertFalse(any(self.fixture.target.glob(".mynx-runtime-v2-transaction-*")))
        self.assertFalse((self.fixture.target / ".mynx-runtime-v2.lock").exists())

    def test_failed_rollback_preserves_transaction_backups(self) -> None:
        self.fixture.manager.adopt(dry_run=False)
        candidate, _ = self.fixture.add_repository_candidate()
        state = self.fixture.repository_state()
        operation = {"type": "UPDATE_SLOT", "slot": "A", "candidate": candidate_declaration(candidate)}
        original_atomic_copy = manager_module._atomic_copy

        def fail_during_rollback(source: Path, destination: Path) -> None:
            if source.parent.name == "target" and source.parent.parent.name.startswith(
                ".mynx-runtime-v2-transaction-"
            ):
                raise OSError("simulated rollback failure")
            original_atomic_copy(source, destination)

        def fail_after_apply(stage: str) -> None:
            if stage == "after_physical_apply":
                raise RuntimeError("simulated apply failure")

        with patch("tools.test_instance_manager.manager._atomic_copy", side_effect=fail_during_rollback):
            with self.assertRaisesRegex(ManagerError, "rollback also failed"):
                self.fixture.manager.transition(
                    operation=operation,
                    expected_revision=state["revision"],
                    at="2099-01-01T00:00:00Z",
                    dry_run=False,
                    failure_injector=fail_after_apply,
                )
        residues = list(self.fixture.target.glob(".mynx-runtime-v2-transaction-*"))
        self.assertEqual(1, len(residues))
        self.assertTrue((residues[0] / "runtime-state-before.json").is_file())


if __name__ == "__main__":
    unittest.main()
