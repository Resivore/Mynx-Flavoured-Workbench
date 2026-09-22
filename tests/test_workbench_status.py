from __future__ import annotations

import copy
import unittest
from pathlib import Path
from unittest import mock

from tools.workbench import (
    _release_identity,
    load_json,
    validate_public_text,
    validate_repository,
    validate_repository_transitions,
    validate_status,
    validate_status_transition,
    validate_status_transition_from_base,
)


ROOT = Path(__file__).resolve().parents[1]


class WorkbenchStatusTests(unittest.TestCase):
    def _transition_fixture(self) -> dict[str, object]:
        manifest = copy.deepcopy(load_json(ROOT / "projects" / "block-geometry-extensions" / "WORKBENCH_STATUS.json"))
        manifest["schema_version"] = 3
        manifest["definition"]["lifecycle"] = "ACTIVE"  # type: ignore[index]
        manifest["state"]["blocker"] = None  # type: ignore[index]
        manifest["state"]["releases"] = {  # type: ignore[index]
            "current": {
                "canary": 1,
                "version": "C1",
                "artifact": {"filename": "fixture.jar", "sha256": "a" * 64},
                "source_commit": "a" * 40,
                "summary": "Fixture artifact.",
                "runtime_dependency_policy": {"contract": "CAPABILITY_OR_PROVIDER", "exceptions": []},
            },
            "accepted": None,
            "rollback": None,
            "accepted_current": "NO_ACCEPTED",
            "accepted_rollback": "NO_ROLLBACK",
        }
        manifest["synchronization"] = {  # type: ignore[index]
            "revision": 1,
            "activity_at": "2026-09-16T05:10:00Z",
            "updated_at": "2026-09-16T05:10:00Z",
            "last_codex_at": "2026-09-16T05:10:00Z",
            "source_commit": "a" * 40,
        }
        return manifest

    @staticmethod
    def _next_transition(previous: dict[str, object]) -> dict[str, object]:
        current = copy.deepcopy(previous)
        current["synchronization"] = {  # type: ignore[index]
            "revision": 2,
            "activity_at": "2026-09-16T05:11:00Z",
            "updated_at": "2026-09-16T05:11:00Z",
            "last_codex_at": "2026-09-16T05:11:00Z",
            "source_commit": "b" * 40,
        }
        return current

    def test_repository_layout_validates_without_a_runtime_profile_controller(self) -> None:
        statuses = validate_repository(ROOT)
        self.assertGreater(len(statuses), 0)
        self.assertFalse((ROOT / "tools" / "sheet_sync.py").exists())
        self.assertFalse((ROOT / "tools" / "sheet_sync").exists())
        self.assertFalse((ROOT / ".github" / "workflows" / "publish-project-status.yml").exists())

    def test_testing_lifecycle_is_rejected_and_runtime_evidence_is_lifecycle_neutral(self) -> None:
        manifest = copy.deepcopy(load_json(ROOT / "projects" / "block-geometry-extensions" / "WORKBENCH_STATUS.json"))
        manifest["definition"]["lifecycle"] = "TESTING"
        with self.assertRaisesRegex(ValueError, "lifecycle"):
            validate_status(manifest, ROOT / "projects" / "block-geometry-extensions")

        manifest = copy.deepcopy(load_json(ROOT / "projects" / "block-geometry-extensions" / "WORKBENCH_STATUS.json"))
        manifest["definition"]["lifecycle"] = "ACTIVE"
        manifest["state"]["validation"]["runtime"] = "RUNTIME_FAIL"
        validate_status(manifest, ROOT / "projects" / "block-geometry-extensions")

    def test_built_at_accepts_only_rfc3339_utc(self) -> None:
        manifest = self._transition_fixture()
        release = manifest["state"]["releases"]["current"]  # type: ignore[index]
        release["built_at"] = "2026-09-16T05:10:00.123456789Z"
        validate_status(manifest)
        release["built_at"] = "2026-09-16T05:10:00+00:00"
        with self.assertRaisesRegex(ValueError, "built_at.*RFC 3339 UTC"):
            validate_status(manifest)

    def test_changed_current_artifact_requires_built_at(self) -> None:
        previous = self._transition_fixture()
        current = self._next_transition(previous)
        release = current["state"]["releases"]["current"]  # type: ignore[index]
        release["artifact"]["sha256"] = "b" * 64
        release["canary"] = 2
        with self.assertRaisesRegex(ValueError, "built_at.*required"):
            validate_status_transition(previous, current)
        release["built_at"] = "2026-09-16T05:11:00Z"
        validate_status_transition(previous, current)

    def test_unchanged_historical_artifact_and_truthful_built_at_backfill_are_grandfathered(self) -> None:
        previous = self._transition_fixture()
        previous["state"]["releases"]["current"].pop("built_at", None)  # type: ignore[index]
        current = self._next_transition(previous)
        validate_status_transition(previous, current)
        current["state"]["releases"]["current"]["built_at"] = "2026-09-16T05:10:00Z"  # type: ignore[index]
        validate_status_transition(previous, current)

    def test_built_at_and_summary_are_excluded_from_release_artifact_identity(self) -> None:
        release = self._transition_fixture()["state"]["releases"]["current"]  # type: ignore[index]
        enriched = copy.deepcopy(release)
        enriched["built_at"] = "2026-09-16T05:10:00Z"
        enriched["summary"] = "Updated presentation only."
        self.assertEqual(_release_identity(release), _release_identity(enriched))

    def test_current_release_requires_a_positive_integer_canary(self) -> None:
        manifest = self._transition_fixture()
        release = manifest["state"]["releases"]["current"]  # type: ignore[index]
        release.pop("canary")
        with self.assertRaisesRegex(ValueError, r"current\.canary.*required"):
            validate_status(manifest)

        for malformed in (True, 0, -1, 1.5, "1"):
            with self.subTest(malformed=malformed):
                manifest = self._transition_fixture()
                manifest["state"]["releases"]["current"]["canary"] = malformed  # type: ignore[index]
                with self.assertRaisesRegex(ValueError, r"current\.canary.*integer >= 1"):
                    validate_status(manifest)

    def test_new_current_release_advances_canary_exactly_once(self) -> None:
        previous = self._transition_fixture()
        previous["state"]["releases"]["current"]["canary"] = 9  # type: ignore[index]
        current = self._next_transition(previous)
        release = current["state"]["releases"]["current"]  # type: ignore[index]
        release["version"] = "internal-dev.10"
        release["artifact"] = {"filename": "fixture-10.jar", "sha256": "b" * 64}
        release["source_commit"] = "b" * 40
        release["canary"] = 10
        release["built_at"] = "2026-09-16T05:11:00Z"
        validate_status_transition(previous, current)

        release["canary"] = 9
        with self.assertRaisesRegex(ValueError, r"current\.canary.*must be C10"):
            validate_status_transition(previous, current)

        release["canary"] = 11
        with self.assertRaisesRegex(ValueError, r"current\.canary.*must be C10"):
            validate_status_transition(previous, current)

    def test_unchanged_current_release_and_admin_changes_retain_canary(self) -> None:
        previous = self._transition_fixture()
        previous["state"]["releases"]["current"]["canary"] = 9  # type: ignore[index]
        current = self._next_transition(previous)
        current["definition"]["lifecycle"] = "PARKED"  # type: ignore[index]
        validate_status_transition(previous, current)

        current["state"]["releases"]["current"]["canary"] = 10  # type: ignore[index]
        with self.assertRaisesRegex(ValueError, r"current\.canary.*must be C9"):
            validate_status_transition(previous, current)

    def test_metadata_correction_on_unchanged_bytes_retains_canary(self) -> None:
        previous = self._transition_fixture()
        previous["state"]["releases"]["current"]["canary"] = 9  # type: ignore[index]
        current = self._next_transition(previous)
        release = current["state"]["releases"]["current"]  # type: ignore[index]
        release["version"] = "corrected-internal-version"
        release["artifact"]["filename"] = "corrected-filename.jar"
        release["built_at"] = "2026-09-16T05:10:00Z"
        validate_status_transition(previous, current)

        release["canary"] = 10
        with self.assertRaisesRegex(ValueError, r"current\.canary.*must be C9"):
            validate_status_transition(previous, current)

    def test_first_current_release_starts_at_canary_one(self) -> None:
        previous = self._transition_fixture()
        first_release = copy.deepcopy(previous["state"]["releases"]["current"])  # type: ignore[index]
        previous["state"]["releases"]["current"] = None  # type: ignore[index]
        current = self._next_transition(previous)
        current["state"]["releases"]["current"] = first_release  # type: ignore[index]
        current["state"]["releases"]["current"]["built_at"] = "2026-09-16T05:10:00Z"  # type: ignore[index]
        current["state"]["releases"]["current"]["canary"] = 2  # type: ignore[index]
        with self.assertRaisesRegex(ValueError, r"current\.canary.*C1.*first current release"):
            validate_status_transition(previous, current)

        current["state"]["releases"]["current"]["canary"] = 1  # type: ignore[index]
        validate_status_transition(previous, current)

    def test_known_release_canary_is_consistent_across_slots(self) -> None:
        manifest = self._transition_fixture()
        current = manifest["state"]["releases"]["current"]  # type: ignore[index]
        accepted = copy.deepcopy(current)
        accepted["canary"] = 2
        manifest["state"]["releases"]["accepted"] = accepted  # type: ignore[index]
        manifest["state"]["releases"]["accepted_current"] = "CURRENT_DIFFERS_FROM_ACCEPTED"  # type: ignore[index]
        with self.assertRaisesRegex(ValueError, r"accepted\.canary.*already recorded"):
            validate_status(manifest)

    def test_historical_release_reselection_restores_its_original_canary(self) -> None:
        previous = self._transition_fixture()
        previous_current = previous["state"]["releases"]["current"]  # type: ignore[index]
        previous_current["canary"] = 9
        historical = copy.deepcopy(previous_current)
        historical["canary"] = 8
        historical["version"] = "historical-internal-version"
        historical["artifact"] = {"filename": "historical.jar", "sha256": "8" * 64}
        historical["built_at"] = "2026-09-16T05:08:00Z"
        previous["state"]["releases"]["accepted"] = historical  # type: ignore[index]
        previous["state"]["releases"]["rollback"] = copy.deepcopy(historical)  # type: ignore[index]
        previous["state"]["releases"]["accepted_current"] = "CURRENT_DIFFERS_FROM_ACCEPTED"  # type: ignore[index]
        previous["state"]["releases"]["accepted_rollback"] = "ACCEPTED_IS_ROLLBACK"  # type: ignore[index]

        current = self._next_transition(previous)
        current["state"]["releases"]["current"] = copy.deepcopy(historical)  # type: ignore[index]
        current["state"]["releases"]["accepted_current"] = "CURRENT_IS_ACCEPTED"  # type: ignore[index]
        validate_status_transition(previous, current)

        current["state"]["releases"]["current"]["canary"] = 10  # type: ignore[index]
        current["state"]["releases"]["accepted"]["canary"] = 10  # type: ignore[index]
        current["state"]["releases"]["rollback"]["canary"] = 10  # type: ignore[index]
        with self.assertRaisesRegex(ValueError, r"canary.*C8"):
            validate_status_transition(previous, current)

    def test_v2_to_v3_canary_metadata_migration_does_not_require_a_project_revision(self) -> None:
        previous = self._transition_fixture()
        previous["schema_version"] = 2
        previous["state"]["releases"]["current"].pop("canary")  # type: ignore[index]
        current = copy.deepcopy(previous)
        current["schema_version"] = 3
        current["state"]["releases"]["current"]["canary"] = 9  # type: ignore[index]

        self.assertEqual(validate_status_transition_from_base(previous, current), "migration")

        current["definition"]["lifecycle"] = "PARKED"  # type: ignore[index]
        with self.assertRaisesRegex(ValueError, r"permits only schema_version and release\.canary metadata"):
            validate_status_transition_from_base(previous, current)

    def test_v2_migration_cannot_hide_a_release_transition(self) -> None:
        previous = self._transition_fixture()
        previous["schema_version"] = 2
        previous_release = previous["state"]["releases"]["current"]  # type: ignore[index]
        previous_release.pop("canary")
        previous_release["version"] = "0.1.0-canary9"

        current = self._next_transition(previous)
        current["schema_version"] = 3
        release = current["state"]["releases"]["current"]  # type: ignore[index]
        release["canary"] = 10
        release["version"] = "0.1.0-canary10"
        release["artifact"] = {"filename": "fixture-10.jar", "sha256": "b" * 64}
        release["source_commit"] = "b" * 40
        release["built_at"] = "2026-09-16T05:11:00Z"

        with self.assertRaisesRegex(ValueError, r"permits only schema_version and release\.canary metadata"):
            validate_status_transition_from_base(previous, current)

    def test_new_project_must_begin_with_canary_one(self) -> None:
        current = self._transition_fixture()
        project_uuid = current["identity"]["uuid"]  # type: ignore[index]
        statuses = {project_uuid: (ROOT / "projects" / "new" / "WORKBENCH_STATUS.json", current)}
        with (
            mock.patch("tools.workbench.load_git_statuses", return_value=("a" * 40, {})),
            mock.patch("tools.workbench.load_repository_statuses", return_value=statuses),
        ):
            counts = validate_repository_transitions(ROOT, "base")
            self.assertEqual(counts["added"], 1)

            current["state"]["releases"]["current"]["canary"] = 2  # type: ignore[index]
            with self.assertRaisesRegex(ValueError, r"newly added project must begin with C1"):
                validate_repository_transitions(ROOT, "base")

    def test_repository_transition_gate_rejects_a_real_canary_jump(self) -> None:
        previous = self._transition_fixture()
        previous["state"]["releases"]["current"]["canary"] = 9  # type: ignore[index]
        current = self._next_transition(previous)
        release = current["state"]["releases"]["current"]  # type: ignore[index]
        release["version"] = "internal-dev.10"
        release["artifact"] = {"filename": "fixture-10.jar", "sha256": "b" * 64}
        release["source_commit"] = "b" * 40
        release["built_at"] = "2026-09-16T05:11:00Z"
        release["canary"] = 11
        project_uuid = current["identity"]["uuid"]  # type: ignore[index]
        base_path = Path("projects/example/WORKBENCH_STATUS.json")
        current_path = ROOT / base_path

        with (
            mock.patch(
                "tools.workbench.load_git_statuses",
                return_value=("a" * 40, {project_uuid: (base_path, previous)}),
            ),
            mock.patch(
                "tools.workbench.load_repository_statuses",
                return_value={project_uuid: (current_path, current)},
            ),
        ):
            with self.assertRaisesRegex(ValueError, r"current\.canary.*must be C10"):
                validate_repository_transitions(ROOT, "base")

            release["canary"] = 10
            counts = validate_repository_transitions(ROOT, "base")
            self.assertEqual(counts["transition"], 1)

    def test_ci_validates_real_manifest_transitions_against_the_event_base(self) -> None:
        workflow = (ROOT / ".github" / "workflows" / "validate.yml").read_text(encoding="utf-8")
        self.assertIn("fetch-depth: 0", workflow)
        self.assertIn("github.event.pull_request.base.sha", workflow)
        self.assertIn("github.event.before", workflow)
        self.assertIn("github.ref_type == 'branch'", workflow)
        self.assertIn("github.event.deleted != true", workflow)
        self.assertIn("validate-transitions --root . --base-ref", workflow)

    def test_public_control_text_rejects_local_paths_without_rejecting_urls_or_relative_paths(self) -> None:
        for value in (
            r"C:\Users\alice\AppData\Roaming\profile",
            r"D:/worktrees/project/build/libs/mod.jar",
            "/home/alice/worktrees/project",
            "/Users/alice/worktrees/project",
            r"Use \Users\alice\AppData\Local\Temp",
        ):
            with self.subTest(value=value):
                with self.assertRaisesRegex(ValueError, "absolute/local filesystem path"):
                    validate_public_text(value, "fixture")

        for value in (
            "https://example.invalid/Users/alice",
            "projects/example/artifacts/example.jar",
            "minecraft:oak_planks",
            "net.fabricmc:fabric-loader:0.16.0",
        ):
            with self.subTest(value=value):
                validate_public_text(value, "fixture")


if __name__ == "__main__":
    unittest.main()
