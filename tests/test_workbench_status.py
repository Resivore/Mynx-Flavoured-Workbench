from __future__ import annotations

import copy
import unittest
from pathlib import Path

from tools.workbench import _release_identity, load_json, validate_repository, validate_status, validate_status_transition


ROOT = Path(__file__).resolve().parents[1]


class WorkbenchStatusTests(unittest.TestCase):
    def _transition_fixture(self) -> dict[str, object]:
        manifest = copy.deepcopy(load_json(ROOT / "projects" / "block-geometry-extensions" / "WORKBENCH_STATUS.json"))
        manifest["definition"]["lifecycle"] = "ACTIVE"  # type: ignore[index]
        manifest["state"]["blocker"] = None  # type: ignore[index]
        manifest["state"]["releases"] = {  # type: ignore[index]
            "current": {
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


if __name__ == "__main__":
    unittest.main()
