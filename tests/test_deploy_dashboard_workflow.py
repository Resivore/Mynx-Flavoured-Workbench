from __future__ import annotations

import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github" / "workflows" / "deploy-dashboard.yml"


class DeployDashboardWorkflowTests(unittest.TestCase):
    def test_pages_workflow_is_main_only_static_site_deployment(self) -> None:
        content = WORKFLOW.read_text(encoding="utf-8")
        lowered = content.casefold()
        self.assertIn("push:\n    branches: [main]", content)
        self.assertIn("workflow_dispatch:", content)
        self.assertNotIn("schedule:", lowered)
        self.assertNotIn("cron:", lowered)
        self.assertIn("contents: read", content)
        self.assertIn("pages: write", content)
        self.assertIn("id-token: write", content)
        self.assertIn("group: pages", content)
        self.assertIn("cancel-in-progress: false", content)
        self.assertIn("actions/checkout@v7", content)
        self.assertIn("actions/configure-pages@v5", content)
        self.assertIn("actions/upload-pages-artifact@v5", content)
        self.assertIn("actions/deploy-pages@v5", content)
        self.assertIn("name: github-pages", content)
        self.assertIn("--output _site/index.html", content)
        self.assertIn("--source-commit \"${GITHUB_SHA}\"", content)
        self.assertIn("path: _site", content)
        self.assertNotIn("gh-pages", lowered)
        self.assertNotIn("workbench_dashboard.html", lowered)

    def test_repository_validation_has_no_pages_deployment_gate(self) -> None:
        validator = (ROOT / "tools" / "workbench.py").read_text(encoding="utf-8")
        self.assertNotIn("deploy-dashboard.yml", validator)
        self.assertNotIn("pages deployment", validator.casefold())


if __name__ == "__main__":
    unittest.main()
