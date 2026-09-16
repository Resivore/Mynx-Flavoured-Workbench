from __future__ import annotations

import copy
import hashlib
import json
import os
import re
import tempfile
import unittest
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from unittest import mock
from uuid import NAMESPACE_URL, uuid5

from tools import workbench_dashboard as dashboard


FIXED_TIME = "2026-09-16T05:10:00Z"
STATUS_COMMIT = "a" * 40
SOURCE_COMMIT = "b" * 40


def release(version: str = "1.0.0", filename: str | None = None, sha256: str | None = None) -> dict[str, object]:
    artifact = None if filename is None else {"filename": filename, "sha256": sha256 or "0" * 64}
    return {"version": version, "artifact": artifact, "source_commit": STATUS_COMMIT}


def write_project(
    root: Path,
    project_id: str,
    name: str,
    *,
    container: str = "projects",
    lifecycle: str = "ACTIVE",
    current: dict[str, object] | None = None,
) -> tuple[str, Path, dict[str, object]]:
    project_uuid = str(uuid5(NAMESPACE_URL, f"dashboard-test:{container}/{project_id}"))
    directory = root / container / project_id
    directory.mkdir(parents=True)
    if lifecycle == "ACCEPTED" and current is None:
        current = release()
    accepted = copy.deepcopy(current) if lifecycle == "ACCEPTED" else None
    manifest: dict[str, object] = {
        "$schema": "../../schemas/workbench-status.schema.json",
        "schema_version": 1,
        "identity": {
            "uuid": project_uuid,
            "name": name,
            "project_id": project_id,
            "aliases": [],
            "legacy_names": [],
            "legacy_ids": [],
        },
        "definition": {
            "lifecycle": lifecycle,
            "goals": ["Exercise the dashboard contract."],
            "scope": ["Dashboard test fixture."],
            "boundaries": {"owned_paths": [f"{container}/{project_id}"], "exclusions": []},
            "dependencies": [],
        },
        "state": {
            "milestone": "Dashboard fixture.",
            "releases": {
                "current": current,
                "accepted": accepted,
                "rollback": None,
                "accepted_current": "CURRENT_IS_ACCEPTED" if lifecycle == "ACCEPTED" else "NO_ACCEPTED",
                "accepted_rollback": "NO_ROLLBACK",
            },
            "validation": {"build": "NOT_RUN", "deployment": "NOT_DEPLOYED", "runtime": "RUNTIME_UNTESTED"},
            "blocker": (
                {"summary": "Fixture blocker.", "since": FIXED_TIME, "next_action": "Resume the fixture."}
                if lifecycle == "BLOCKED"
                else None
            ),
        },
        "synchronization": {
            "revision": 1,
            "activity_at": FIXED_TIME,
            "updated_at": FIXED_TIME,
            "last_codex_at": FIXED_TIME,
            "source_commit": SOURCE_COMMIT,
            "google_sheet": {"participates": True, "exclusion_reason": None},
        },
    }
    (directory / "WORKBENCH_STATUS.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    (directory / "TESTING.md").write_text("# Testing\n\nDashboard fixture.\n", encoding="utf-8")
    (directory / "CODEX_LOG.md").write_text(
        "# Codex Log\n\n"
        f"## {FIXED_TIME} — Dashboard fixture\n\n"
        "- Revision: 1\n"
        f"- Source checkpoint: `{SOURCE_COMMIT}`\n"
        "- Changes: Added the dashboard fixture.\n"
        "- Build/static: Not run.\n"
        "- Runtime: Not run.\n"
        "- Artifact: None.\n"
        "- Result: Fixture ready.\n"
        f"- Next state: {lifecycle}.\n",
        encoding="utf-8",
    )
    return project_uuid, directory, manifest


def statuses_for(*items: tuple[str, Path, dict[str, object]]) -> dict[str, tuple[Path, dict[str, object]]]:
    return {project_uuid: (directory / "WORKBENCH_STATUS.json", manifest) for project_uuid, directory, manifest in items}


def make_artifact(directory: Path, filename: str, content: bytes, mtime_ns: int) -> str:
    artifact_directory = directory / "artifacts"
    artifact_directory.mkdir(exist_ok=True)
    path = artifact_directory / filename
    path.write_bytes(content)
    os.utime(path, ns=(mtime_ns, mtime_ns))
    return hashlib.sha256(content).hexdigest()


def model(name: str, lifecycle: str, *, mtime_ns: int | None = None, project_uuid: str | None = None) -> dashboard.DashboardProject:
    return dashboard.DashboardProject(
        uuid=project_uuid or str(uuid5(NAMESPACE_URL, f"dashboard-model:{name}:{lifecycle}")),
        project_id=name.casefold().replace(" ", "-"),
        name=name,
        lifecycle=lifecycle,
        current_version=None,
        jar_mtime_ns=mtime_ns,
        jar_mtime_iso=None if mtime_ns is None else "2026-09-16T05:10:00.000000Z",
        jar_note="Fixture.",
        server_status="NOT_DEPLOYED",
        deployed_release=None,
    )


class TagCollector(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.tags: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        self.tags.append(tag)


class WorkbenchDashboardTests(unittest.TestCase):
    def test_discovers_projects_and_resourcepacks_through_canonical_loader(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            project = write_project(root, "alpha", "Alpha")
            resource_pack = write_project(root, "fern-pack", "Fern Pack", container="resourcepacks", lifecycle="PARKED")

            statuses, records = dashboard.discover_projects(root)

            self.assertEqual(set(statuses), {project[0], resource_pack[0]})
            self.assertEqual({record.name for record in records}, {"Alpha", "Fern Pack"})

    def test_default_order_uses_required_lifecycle_priority(self) -> None:
        projects = [model(lifecycle.title(), lifecycle) for lifecycle in reversed(dashboard.LIFECYCLE_ORDER)]
        ordered = dashboard.sort_projects_default(projects)
        self.assertEqual([project.lifecycle for project in ordered], list(dashboard.LIFECYCLE_ORDER))

    def test_same_lifecycle_orders_matching_jars_newest_first(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            old_bytes = b"old jar"
            new_bytes = b"new jar"
            old = write_project(
                root,
                "old",
                "Old",
                current=release("old", "old.jar", hashlib.sha256(old_bytes).hexdigest()),
            )
            new = write_project(
                root,
                "new",
                "New",
                current=release("new", "new.jar", hashlib.sha256(new_bytes).hexdigest()),
            )
            make_artifact(old[1], "old.jar", old_bytes, 1_700_000_000_000_000_000)
            make_artifact(new[1], "new.jar", new_bytes, 1_800_000_000_000_000_000)

            records = dashboard.build_project_records(statuses_for(old, new), {})

            self.assertEqual([record.name for record in records], ["New", "Old"])

    def test_untimestamped_projects_follow_timestamped_and_use_name_tiebreaker(self) -> None:
        projects = [
            model("Zulu", "ACTIVE"),
            model("Alpha", "ACTIVE"),
            model("Timed", "ACTIVE", mtime_ns=100),
        ]
        self.assertEqual([item.name for item in dashboard.sort_projects_default(projects)], ["Timed", "Alpha", "Zulu"])

    def test_missing_current_release_has_no_version_or_jar_timestamp(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            item = write_project(Path(temporary), "planned", "Planned", lifecycle="PLANNED", current=None)
            record = dashboard.build_project_records(statuses_for(item), {})[0]
            self.assertIsNone(record.current_version)
            self.assertIsNone(record.jar_mtime_ns)
            self.assertEqual(record.jar_note, "No current release.")

    def test_missing_canonical_artifact_does_not_use_build_decoy(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            content = b"decoy"
            item = write_project(
                root,
                "missing",
                "Missing",
                current=release("C1", "missing.jar", hashlib.sha256(content).hexdigest()),
            )
            decoy = item[1] / "build" / "libs"
            decoy.mkdir(parents=True)
            (decoy / "missing.jar").write_bytes(content)

            record = dashboard.build_project_records(statuses_for(item), {})[0]

            self.assertIsNone(record.jar_mtime_ns)
            self.assertIn("not retained locally", record.jar_note)

    def test_wrong_artifact_hash_is_unavailable(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            item = write_project(root, "wrong", "Wrong", current=release("C1", "wrong.jar", "0" * 64))
            make_artifact(item[1], "wrong.jar", b"different", 1_800_000_000_000_000_000)

            record = dashboard.build_project_records(statuses_for(item), {})[0]

            self.assertIsNone(record.jar_mtime_ns)
            self.assertIn("SHA-256", record.jar_note)

    def test_non_jar_current_artifact_is_unavailable_but_keeps_version(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            content = b"resource pack"
            item = write_project(
                root,
                "pack",
                "Pack",
                container="resourcepacks",
                current=release("Pack 1", "pack.zip", hashlib.sha256(content).hexdigest()),
            )
            make_artifact(item[1], "pack.zip", content, 1_800_000_000_000_000_000)
            record = dashboard.build_project_records(statuses_for(item), {})[0]
            self.assertEqual(record.current_version, "Pack 1")
            self.assertIsNone(record.jar_mtime_ns)
            self.assertIn("not a JAR", record.jar_note)

    def test_server_release_states_and_accepted_current_rule(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            current = write_project(root, "current", "Current", current=release("canary7", "current.jar", "1" * 64))
            outdated = write_project(root, "outdated", "Outdated", current=release("canary8", "outdated.jar", "2" * 64))
            absent = write_project(root, "absent", "Absent", current=release("1.0.0", "absent.jar", "3" * 64))
            accepted = write_project(root, "accepted", "Accepted", lifecycle="ACCEPTED", current=release("canary9", "accepted.jar", "4" * 64))
            records = dashboard.build_project_records(
                statuses_for(current, outdated, absent, accepted),
                {
                    current[0]: dashboard.canonical_release_identity(current[2]["state"]["releases"]["current"]),  # type: ignore[arg-type,index]
                    outdated[0]: dashboard.canonical_release_identity(release("canary7", "outdated.jar", "2" * 64)),
                },
            )
            states = {record.name: record.server_status for record in records}
            self.assertEqual(states, {"Current": "CURRENT", "Outdated": "OUTDATED", "Absent": "NOT_DEPLOYED", "Accepted": "CURRENT"})

    def test_new_current_release_makes_preserved_deployment_outdated_by_version_or_sha(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            item = write_project(Path(temporary), "fern", "Fern", current=release("canary8", "fern.jar", "b" * 64))
            prior = {"version": "canary7", "artifact": {"filename": "fern.jar", "sha256": "a" * 64}}
            record = dashboard.build_project_records(statuses_for(item), {item[0]: prior})[0]
            self.assertEqual(record.server_status, "OUTDATED")
            same_version_old_hash = {"version": "canary8", "artifact": {"filename": "fern.jar", "sha256": "a" * 64}}
            record = dashboard.build_project_records(statuses_for(item), {item[0]: same_version_old_hash})[0]
            self.assertEqual(record.server_status, "OUTDATED")

    def test_server_cli_persists_uuid_and_regenerates_dashboard(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            item = write_project(root, "fern", "Fern Project", current=release("canary1", "fern.jar", "a" * 64))

            result = dashboard.main(["--root", str(root), "server", "fern", "current"])

            self.assertEqual(result, 0)
            saved = json.loads((root / dashboard.SERVER_STATE_FILENAME).read_text(encoding="utf-8"))
            self.assertEqual(saved["schema_version"], 2)
            self.assertEqual(saved["projects"], {item[0]: {"version": "canary1", "artifact": {"filename": "fern.jar", "sha256": "a" * 64}}})
            self.assertNotIn("fern", saved["projects"])
            self.assertTrue((root / dashboard.DEFAULT_OUTPUT).is_file())

            result = dashboard.main(["--root", str(root), "server", "Fern Project", "no"])
            self.assertEqual(result, 0)
            saved = json.loads((root / dashboard.SERVER_STATE_FILENAME).read_text(encoding="utf-8"))
            self.assertEqual(saved["projects"], {})

    def test_open_regenerates_local_file_without_creating_server_state(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            write_project(root, "fern", "Fern Project")
            output = root / dashboard.DEFAULT_OUTPUT

            with mock.patch.object(dashboard.webbrowser, "open", return_value=True) as open_browser:
                result = dashboard.main(["--root", str(root), "--open"])

            self.assertEqual(result, 0)
            self.assertTrue(output.is_file())
            self.assertFalse((root / dashboard.SERVER_STATE_FILENAME).exists())
            open_browser.assert_called_once_with(output.as_uri(), new=2)

    def test_uuid_keyed_server_state_survives_display_rename(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            item = write_project(Path(temporary), "fern", "Old Name", current=release("1.0.0"))
            renamed = copy.deepcopy(item[2])
            renamed["identity"]["name"] = "New Name"  # type: ignore[index]
            records = dashboard.build_project_records(
                {item[0]: (item[1] / "WORKBENCH_STATUS.json", renamed)},
                {item[0]: {"version": "1.0.0", "artifact": None}},
            )
            self.assertEqual(records[0].name, "New Name")
            self.assertEqual(records[0].server_status, "CURRENT")

    def test_hostile_project_name_is_safely_embedded_and_round_trips(self) -> None:
        hostile = "A </script><img src=x onerror=alert(1)> & \" ' \u2028 \u2029"
        html = dashboard.render_dashboard(
            [model(hostile, "ACTIVE")],
            generated_at=datetime(2026, 9, 16, 5, 10, tzinfo=timezone.utc),
        )
        self.assertNotIn("</script><img", html)
        collector = TagCollector()
        collector.feed(html)
        self.assertNotIn("img", collector.tags)
        match = re.search(r'<script type="application/json" id="dashboard-data">(.*?)</script>', html, re.DOTALL)
        self.assertIsNotNone(match)
        payload = json.loads(match.group(1))  # type: ignore[union-attr]
        self.assertEqual(payload["projects"][0]["name"], hostile)

    def test_data_and_html_order_are_deterministic(self) -> None:
        alpha = model("Alpha", "ACTIVE")
        beta = model("Beta", "PLANNED")
        generated_at = datetime(2026, 9, 16, 5, 10, tzinfo=timezone.utc)
        first = dashboard.render_dashboard([beta, alpha], generated_at=generated_at)
        second = dashboard.render_dashboard([alpha, beta], generated_at=generated_at)
        self.assertEqual(first, second)
        self.assertLess(first.index(alpha.uuid), first.index(beta.uuid))

    def test_canary_display_is_conservative_and_numeric(self) -> None:
        self.assertEqual(dashboard.display_canary_version("canary1"), "C1")
        self.assertEqual(dashboard.display_canary_version("0.1.0-canary12"), "C12")
        self.assertEqual(dashboard.display_canary_version("release candidate"), "release candidate")
        self.assertLess(dashboard.canary_number("C2") or 0, dashboard.canary_number("C10") or 0)

    def test_bootstrap_sprite_is_embedded_and_icon_controls_are_labeled(self) -> None:
        html = dashboard.render_dashboard([model("Alpha", "ACTIVE")])
        self.assertIn('class="icon-sprite"', html)
        self.assertIn('id="search"', html)
        self.assertIn('href="#arrow-down-up"', html)
        self.assertIn('aria-label="Clear project search"', html)
        self.assertIn('title="Clear project search"', html)

    def test_generated_page_has_no_runtime_network_dependencies(self) -> None:
        html = dashboard.render_dashboard(
            [model("Alpha", "ACTIVE")],
            generated_at=datetime(2026, 9, 16, 5, 10, tzinfo=timezone.utc),
        )
        lowered = html.casefold()
        for forbidden in (
            "@import",
            "fetch(",
            "xmlhttprequest",
            "websocket",
            "eventsource",
            "sendbeacon",
        ):
            self.assertNotIn(forbidden, lowered)
        self.assertNotIn('href="http', lowered)
        self.assertNotIn('src="http', lowered)
        self.assertIn("connect-src 'none'", html)


if __name__ == "__main__":
    unittest.main()
