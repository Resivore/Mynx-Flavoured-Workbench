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

from tools.runtime_slots import candidate_declaration, plan_transition, validate_runtime_state
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
            "schema_version": 2,
            "repository_root": ".",
            "runtime_state": "runtime-state.json",
            "dedicated_profile": str(self.target),
            "protected_profile": str(self.protected),
            "mods_directory": "mods",
            "ledger_file": ".mynx-runtime-v2-ledger.json",
            "lock_file": ".mynx-runtime-v2.lock",
        }
        self.config_path.write_text(json.dumps(config, indent=2) + "\n", encoding="utf-8")
        self.project_index = {
            self.base["project_uuid"]: self.base["project_id"],
            self.heart_uuid: "matcha-heart-death-compat",
            self.mossy_uuid: "mossy-stone",
        }
        self.manager = PhysicalManager.from_config(self.config_path, project_index=self.project_index)

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
        self.assertEqual("preserve me", (self.fixture.mods / "unrelated.txt").read_text(encoding="utf-8"))
        self.assertEqual("PHYSICAL_STATE_VERIFIED", self.fixture.manager.verify()["status"])

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
