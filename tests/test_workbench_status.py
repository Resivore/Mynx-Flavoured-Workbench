from __future__ import annotations

import copy
import unittest
from pathlib import Path

from tools.workbench import load_json, validate_repository, validate_status


ROOT = Path(__file__).resolve().parents[1]


class WorkbenchStatusTests(unittest.TestCase):
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


if __name__ == "__main__":
    unittest.main()
