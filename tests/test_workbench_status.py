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

    def test_testing_lifecycle_is_valid_without_slot_state(self) -> None:
        manifest = copy.deepcopy(load_json(ROOT / "projects" / "block-geometry-extensions" / "WORKBENCH_STATUS.json"))
        manifest["definition"]["lifecycle"] = "TESTING"
        validate_status(manifest, ROOT / "projects" / "block-geometry-extensions")


if __name__ == "__main__":
    unittest.main()
