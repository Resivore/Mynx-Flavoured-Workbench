from __future__ import annotations

import copy
import hashlib
import json
import os
import re
import tempfile
import unittest
from dataclasses import replace
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote
from unittest import mock
from uuid import NAMESPACE_URL, uuid5

from tools import workbench_dashboard as dashboard


FIXED_TIME = "2026-09-16T05:10:00Z"
STATUS_COMMIT = "a" * 40
SOURCE_COMMIT = "b" * 40


def release(
    version: str = "1.0.0",
    filename: str | None = None,
    sha256: str | None = None,
    *,
    canary: int = 1,
) -> dict[str, object]:
    artifact = None if filename is None else {"filename": filename, "sha256": sha256 or "0" * 64}
    return {"canary": canary, "version": version, "artifact": artifact, "source_commit": STATUS_COMMIT}


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
        "schema_version": 3,
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
            "validation": {"build": "NOT_RUN", "runtime": "RUNTIME_UNTESTED"},
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
            self.assertEqual({record.name: record.record_kind for record in records}, {"Alpha": "project", "Fern Pack": "resource_pack"})

    def test_dashboard_payload_separates_canonical_record_kinds_and_defaults_to_projects(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            project = write_project(root, "alpha", "Alpha")
            resource_pack = write_project(root, "fern-pack", "Fern Pack", container="resourcepacks", lifecycle="PARKED")
            _, records = dashboard.discover_projects(root)

            html = dashboard.render_dashboard(records, generated_at=datetime(2026, 9, 16, 5, 10, tzinfo=timezone.utc))

        payload = json.loads(re.search(r'<script type="application/json" id="dashboard-data">(.*?)</script>', html, re.DOTALL).group(1))  # type: ignore[union-attr]
        self.assertEqual(payload["counts"], {"projects": 1, "resourcePacks": 1})
        self.assertEqual(payload["projects"][0]["uuid"], project[0])
        self.assertEqual(payload["projects"][0]["kind"], "project")
        self.assertEqual(payload["resourcePacks"][0]["uuid"], resource_pack[0])
        self.assertEqual(payload["resourcePacks"][0]["kind"], "resource_pack")
        self.assertNotIn("server", payload["resourcePacks"][0])
        self.assertNotIn("jarMtimeMs", payload["resourcePacks"][0])
        self.assertIn('id="projects-tab"', html)
        self.assertIn('role="tab" data-record-kind="project" aria-selected="true"', html)
        self.assertIn('role="tab" data-record-kind="resource_pack" aria-selected="false"', html)
        self.assertIn('resource_pack: { noun: "resource pack", plural: "resource packs"', html)
        self.assertIn('columns: ["name", "lifecycle", "version", "jar"]', html)
        self.assertIn('serverFilterWrap.hidden = state.kind !== "project"', html)
        self.assertIn('cell.colSpan = currentView().columns.length;', html)

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

    def test_canonical_built_at_renders_without_a_retained_jar_and_drives_recency(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            old_release = release("old", "old.jar", "a" * 64)
            old_release["built_at"] = "2026-09-16T05:10:00Z"
            new_release = release("new", "new.jar", "b" * 64)
            new_release["built_at"] = "2026-09-16T05:11:00.123456789Z"
            old = write_project(root, "old", "Old", current=old_release)
            new = write_project(root, "new", "New", current=new_release)

            records = dashboard.build_project_records(statuses_for(old, new), {})

            self.assertEqual([record.name for record in records], ["New", "Old"])
            self.assertEqual(records[0].jar_mtime_iso, "2026-09-16T05:11:00.123456789Z")
            self.assertIn("Canonical build timestamp", records[0].jar_note)
            self.assertIn("not retained locally", records[0].jar_note)

    def test_built_at_does_not_change_server_current_comparison(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            current = release("C1", "current.jar", "a" * 64)
            current["built_at"] = "2026-09-16T05:10:00Z"
            item = write_project(Path(temporary), "current", "Current", current=current)
            deployed = {"version": "C1", "artifact": {"filename": "current.jar", "sha256": "a" * 64}}
            record = dashboard.build_project_records(statuses_for(item), {item[0]: deployed})[0]
            self.assertEqual(record.server_status, "CURRENT")

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

    def test_unverified_local_bytes_are_not_used_when_built_at_is_absent(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            item = write_project(root, "wrong", "Wrong", current=release("C1", "wrong.jar", "0" * 64))
            make_artifact(item[1], "wrong.jar", b"different", 1_800_000_000_000_000_000)
            record = dashboard.build_project_records(statuses_for(item), {})[0]
            self.assertIsNone(record.jar_mtime_iso)
            self.assertIn("SHA-256", record.jar_note)

    def test_resource_pack_zip_uses_verified_canonical_timestamp(self) -> None:
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
            self.assertEqual(record.record_kind, "resource_pack")
            self.assertEqual(record.jar_mtime_ns, 1_800_000_000_000_000_000)
            self.assertIn("Verified current ZIP", record.jar_note)

    def test_resource_pack_zip_prefers_canonical_built_at_when_not_retained(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            current = release("Pack 1", "pack.zip", "a" * 64)
            current["built_at"] = "2026-09-16T05:11:00.123456789Z"
            item = write_project(Path(temporary), "pack", "Pack", container="resourcepacks", current=current)

            record = dashboard.build_project_records(statuses_for(item), {})[0]

            self.assertEqual(record.jar_mtime_iso, "2026-09-16T05:11:00.123456789Z")
            self.assertIn("current ZIP is not retained locally", record.jar_note)

    def test_server_release_states_require_exact_deployed_identity(self) -> None:
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
            self.assertEqual(states, {"Current": "CURRENT", "Outdated": "OUTDATED", "Absent": "NOT_DEPLOYED", "Accepted": "NOT_DEPLOYED"})

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

    def test_release_summary_is_escaped_and_available_for_expandable_rows(self) -> None:
        hostile = "Keeps <exact> data & avoids </script> injection."
        project = model("Alpha", "ACTIVE")
        project = replace(project, current_summary=hostile, current_version="internal-1.0.0", current_canary=1)
        html = dashboard.render_dashboard([project])
        self.assertNotIn("</script> injection", html)
        payload = json.loads(re.search(r'<script type="application/json" id="dashboard-data">(.*?)</script>', html, re.DOTALL).group(1))  # type: ignore[union-attr]
        self.assertEqual(payload["projects"][0]["summary"], hostile)
        self.assertIn("toggle.dataset.expand = project.uuid", html)
        self.assertIn("release-detail", html)

    def test_data_and_html_order_are_deterministic(self) -> None:
        alpha = model("Alpha", "ACTIVE")
        beta = model("Beta", "PLANNED")
        generated_at = datetime(2026, 9, 16, 5, 10, tzinfo=timezone.utc)
        first = dashboard.render_dashboard([beta, alpha], generated_at=generated_at)
        second = dashboard.render_dashboard([alpha, beta], generated_at=generated_at)
        self.assertEqual(first, second)
        self.assertLess(first.index(alpha.uuid), first.index(beta.uuid))

    def test_source_commit_is_safe_machine_readable_masthead_metadata(self) -> None:
        commit = "c" * 40
        html = dashboard.render_dashboard([model("Alpha", "ACTIVE")], source_commit=commit, source_label="main")
        payload = json.loads(re.search(r'<script type="application/json" id="dashboard-data">(.*?)</script>', html, re.DOTALL).group(1))  # type: ignore[union-attr]
        self.assertEqual(payload["source"], {"commit": commit, "label": "main"})
        self.assertIn('id="source-provenance"', html)
        self.assertIn('sourceProvenance.title = data.source.commit', html)
        self.assertIn('data.source.commit.slice(0, 7)', html)

    def test_malformed_source_commit_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            write_project(root, "alpha", "Alpha")
            self.assertEqual(dashboard.main(["--root", str(root), "--source-commit", "not-a-commit"]), 1)

    def test_pages_output_is_self_contained_and_has_no_local_paths(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            write_project(root, "alpha", "Alpha")
            output = root / "_site" / "index.html"
            projects = dashboard.generate_dashboard(root, output, source_commit="d" * 40, source_label="main")
            html = output.read_text(encoding="utf-8")
            self.assertEqual(len(projects), 1)
            self.assertTrue(output.is_file())
            self.assertNotIn(str(root), html)
            self.assertNotIn("C:\\\\Users\\\\", html)
            self.assertIn('"commit":"' + "d" * 40 + '"', html)

    def test_current_release_uses_explicit_canary_not_internal_release_strings(self) -> None:
        internal_version = "C99 (embedded 0.1.0-canary77)"
        current = release(internal_version, "project-0.1.0-canary66.jar", "f" * 64, canary=12)
        with tempfile.TemporaryDirectory() as temporary:
            item = write_project(Path(temporary), "explicit", "Explicit", current=current)
            record = dashboard.build_project_records(statuses_for(item), {})[0]

        self.assertEqual(record.current_version, internal_version)
        self.assertEqual(record.current_canary, 12)
        payload = dashboard._project_payload(record)
        self.assertEqual(payload["version"], "C12")
        self.assertEqual(payload["versionDisplay"], "C12")
        self.assertEqual(payload["versionCanary"], 12)

    def test_deployed_canary_requires_an_exact_canonical_release_match(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            current = release("internal-current", "current.jar", "b" * 64, canary=10)
            item = write_project(Path(temporary), "release-match", "Release Match", current=current)
            releases = item[2]["state"]["releases"]  # type: ignore[index]
            accepted = release("internal-accepted", "accepted.jar", "a" * 64, canary=9)
            releases["accepted"] = accepted
            releases["rollback"] = copy.deepcopy(accepted)
            releases["accepted_current"] = "CURRENT_DIFFERS_FROM_ACCEPTED"
            releases["accepted_rollback"] = "ACCEPTED_IS_ROLLBACK"

            deployed = dashboard.canonical_release_identity(accepted)
            record = dashboard.build_project_records(statuses_for(item), {item[0]: deployed})[0]
            payload = dashboard._project_payload(record)
            self.assertEqual(record.deployed_canary, 9)
            self.assertEqual(payload["deployedVersion"], "C9")
            self.assertEqual(payload["deployedVersionDisplay"], "C9")
            self.assertEqual(payload["serverDisplay"], "OUTDATED · C9")

            unmatched = {"version": "0.1.0-canary88", "artifact": {"filename": "old.jar", "sha256": "8" * 64}}
            record = dashboard.build_project_records(statuses_for(item), {item[0]: unmatched})[0]
            payload = dashboard._project_payload(record)
            self.assertIsNone(record.deployed_canary)
            self.assertIsNone(payload["deployedVersion"])
            self.assertIsNone(payload["deployedVersionDisplay"])
            self.assertEqual(payload["serverDisplay"], "OUTDATED")

    def test_current_main_bbb_dev7_is_c9_and_preserves_dev6_as_c8(self) -> None:
        root = Path(__file__).resolve().parents[1]
        statuses, records = dashboard.discover_projects(root)
        bbb_uuid = "5d42f47f-b006-4125-840d-dec0d2728afa"
        bbb_releases = statuses[bbb_uuid][1]["state"]["releases"]
        bbb_current = bbb_releases["current"]
        bbb = next(record for record in records if record.uuid == bbb_uuid)
        version = "2.0pre4+26.2-enderscape-dev.7"

        self.assertEqual(bbb_current["version"], version)
        self.assertEqual(
            bbb_current["artifact"]["filename"],
            "bbb-fabric-26.2-2.0pre4+26.2-enderscape-dev.7.jar",
        )
        self.assertEqual(
            bbb_current["artifact"]["sha256"],
            "57ddb5dfe62f2eb9f4a2ce22fbeeb5cce4386bbd93aab3f7df0dd8e6d19ddaf0",
        )
        self.assertEqual(bbb_current["canary"], 9)
        self.assertEqual(bbb_releases["accepted"]["canary"], 8)
        self.assertEqual(bbb_releases["rollback"]["canary"], 8)
        self.assertEqual(bbb.current_version, version)
        self.assertEqual(bbb.current_canary, 9)
        self.assertEqual(bbb.deployed_canary, 8)

        with tempfile.TemporaryDirectory() as temporary:
            output = Path(temporary) / "dashboard.html"
            dashboard.generate_dashboard(root, output)
            html = output.read_text(encoding="utf-8")

        payload_match = re.search(r'<script type="application/json" id="dashboard-data">(.*?)</script>', html, re.DOTALL)
        self.assertIsNotNone(payload_match)
        payload = json.loads(payload_match.group(1))  # type: ignore[union-attr]
        bbb_payload = next(project for project in payload["projects"] if project["uuid"] == bbb_uuid)
        self.assertEqual(bbb_payload["version"], "C9")
        self.assertEqual(bbb_payload["versionDisplay"], "C9")
        self.assertEqual(bbb_payload["versionCanary"], 9)
        self.assertEqual(bbb_payload["deployedVersion"], "C8")
        self.assertEqual(bbb_payload["deployedVersionDisplay"], "C8")
        self.assertEqual(bbb_payload["deployedCanary"], 8)
        self.assertEqual(bbb_payload["serverDisplay"], "OUTDATED · C8")

    def test_current_main_dashboard_exposes_only_compact_canary_version_labels(self) -> None:
        root = Path(__file__).resolve().parents[1]
        _, records = dashboard.discover_projects(root)
        html = dashboard.render_dashboard(records, generated_at=datetime(2026, 9, 16, 5, 10, tzinfo=timezone.utc))
        payload = json.loads(re.search(r'<script type="application/json" id="dashboard-data">(.*?)</script>', html, re.DOTALL).group(1))  # type: ignore[union-attr]

        for project in payload["projects"]:
            for field in ("version", "versionDisplay", "deployedVersion", "deployedVersionDisplay"):
                with self.subTest(project=project["projectId"], field=field):
                    label = project[field]
                    self.assertTrue(label is None or re.fullmatch(r"C[1-9]\d*", label), label)

    def test_current_main_lifecycles_server_identity_and_no_testing_group(self) -> None:
        root = Path(__file__).resolve().parents[1]
        paths = [
            root / "projects" / project_id / "WORKBENCH_STATUS.json"
            for project_id in ("mynx-regions-unexplored", "slab-decorations", "block-geometry-extensions")
        ]
        statuses = {
            manifest["identity"]["uuid"]: (path, manifest)
            for path in paths
            for manifest in [dashboard.load_json(path)]
        }
        server_state = dashboard.load_json(root / dashboard.SERVER_STATE_FILENAME)["projects"]
        records = dashboard.build_project_records(statuses, server_state)
        by_id = {record.project_id: record for record in records}
        self.assertEqual(by_id["mynx-regions-unexplored"].lifecycle, "ACTIVE")
        self.assertEqual(by_id["slab-decorations"].lifecycle, "ACTIVE")
        bge = by_id["block-geometry-extensions"]
        bge_current = statuses[bge.uuid][1]["state"]["releases"]["current"]
        bge_accepted = statuses[bge.uuid][1]["state"]["releases"]["accepted"]
        expected_deployed_release = dashboard.canonical_release_identity(bge_accepted)
        self.assertEqual(bge.lifecycle, "ACTIVE")
        self.assertEqual(bge.current_version, bge_current["version"])
        self.assertEqual(bge.current_canary, bge_current["canary"])
        self.assertEqual(bge.deployed_canary, bge_accepted["canary"])
        self.assertEqual(server_state[bge.uuid], expected_deployed_release)
        self.assertNotEqual(expected_deployed_release, dashboard.canonical_release_identity(bge_current))
        self.assertEqual(bge.server_status, "OUTDATED")
        self.assertEqual(bge.deployed_release, expected_deployed_release)
        all_manifests = [
            dashboard.load_json(path)
            for container in ("projects", "resourcepacks")
            for path in (root / container).glob("*/WORKBENCH_STATUS.json")
        ]
        self.assertFalse(any(manifest["definition"]["lifecycle"] == "TESTING" for manifest in all_manifests))
        html = dashboard.render_dashboard(records, generated_at=datetime(2026, 9, 16, 5, 10, tzinfo=timezone.utc))
        payload = json.loads(re.search(r'<script type="application/json" id="dashboard-data">(.*?)</script>', html, re.DOTALL).group(1))  # type: ignore[union-attr]
        payload_by_id = {project["projectId"]: project for project in payload["projects"]}
        expected_bge_payload = dashboard._project_payload(bge)
        self.assertEqual(payload_by_id[bge.project_id]["version"], f'C{bge_current["canary"]}')
        self.assertEqual(payload_by_id[bge.project_id]["versionDisplay"], expected_bge_payload["versionDisplay"])
        self.assertFalse(any(project["lifecycle"] == "TESTING" for project in payload["projects"]))
        self.assertEqual(len(payload["projects"]), len(records))
        self.assertIn("<title>Mynx Dashboard</title>", html)
        self.assertIn("<h1>mynx dashboard</h1>", html)
        self.assertNotIn('data-lifecycle="TESTING"', html)
        accepted_with_artifacts = [
            manifest for manifest in all_manifests
            if manifest["definition"]["lifecycle"] == "ACCEPTED"
            and manifest["state"]["releases"]["current"]
            and manifest["state"]["releases"]["current"]["artifact"]
        ]
        self.assertTrue(all(manifest["identity"]["uuid"] in server_state for manifest in accepted_with_artifacts))

    def test_bootstrap_icons_are_inlined_from_the_vendored_sprite(self) -> None:
        html = dashboard.render_dashboard([model("Alpha", "ACTIVE")])
        self.assertIn('id="search"', html)
        self.assertIn('aria-label="Clear project search"', html)
        self.assertIn('title="Clear project search"', html)
        self.assertNotIn("<use", html)
        self.assertNotIn("icon-sprite", html)
        icons = json.loads(re.search(r'<script type="application/json" id="bootstrap-icon-data">(.*?)</script>', html).group(1))  # type: ignore[union-attr]
        for name in dashboard.DASHBOARD_ICON_NAMES:
            self.assertIn(name, icons)
            self.assertIn("viewBox", icons[name])
            self.assertIn("<path", icons[name]["body"])
        self.assertGreaterEqual(html.count('viewBox="0 0 16 16"'), 9)
        self.assertIn('col.project { width: 42%; }', html)
        self.assertIn('col.server { width: 19%; }', html)
        self.assertIn('col.jar { width: 18%; }', html)
        self.assertIn('col.server { width: 20%; }', html)
        self.assertIn('--controls-surface: #112a1e;', html)
        self.assertIn('--table-header-surface: #0c2118;', html)
        self.assertIn('--active-tab-surface: #153426;', html)
        controls = re.search(r"\.controls \{(?P<rules>.*?)\n    \}", html, re.DOTALL)
        results_bar = re.search(r"\.results-bar \{(?P<rules>.*?)\n    \}", html, re.DOTALL)
        active_tab = re.search(r"\.record-tab\[aria-selected=\"true\"\] \{(?P<rules>.*?)\n    \}", html, re.DOTALL)
        table_header = re.search(r"thead th \{(?P<rules>.*?)\n    \}", html, re.DOTALL)
        self.assertIsNotNone(controls)
        self.assertIsNotNone(results_bar)
        self.assertIsNotNone(active_tab)
        self.assertIsNotNone(table_header)
        self.assertIn('background: var(--controls-surface);', controls.group("rules"))  # type: ignore[union-attr]
        self.assertNotIn("border-bottom", controls.group("rules"))  # type: ignore[union-attr]
        self.assertIn('background: var(--controls-surface);', results_bar.group("rules"))  # type: ignore[union-attr]
        self.assertIn('border-bottom: 1px solid var(--line);', results_bar.group("rules"))  # type: ignore[union-attr]
        self.assertIn('background: var(--active-tab-surface);', active_tab.group("rules"))  # type: ignore[union-attr]
        self.assertIn('background: var(--table-header-surface);', table_header.group("rules"))  # type: ignore[union-attr]
        self.assertIn('thead th:not(:first-child) .sort-button { justify-content: center;', html)
        self.assertIn('.date-value time { display: inline-grid; justify-items: center;', html)
        self.assertIn('const formatted = formatLocalDateParts(project.lastEditMtimeMs);', html)
        self.assertIn('time.append(datePart, timePart);', html)
        self.assertIn('>Version <span class="sort-indicator"', html)
        self.assertIn('.group-chevron { width: 10px; margin-right: 10px;', html)

    def test_dynamic_icons_use_svg_namespace_aware_construction(self) -> None:
        source = (Path(__file__).resolve().parents[1] / "tools" / "workbench_dashboard.py").read_text(encoding="utf-8")
        icon_helper = re.search(r"function icon\(name\) \{(?P<body>.*?)\n      \}", source, re.DOTALL)
        self.assertIsNotNone(icon_helper)
        helper = icon_helper.group("body")  # type: ignore[union-attr]
        self.assertIn('const SVG_NS = "http://www.w3.org/2000/svg";', source)
        self.assertIn('document.createElementNS(SVG_NS, "svg")', helper)
        self.assertIn('svg.setAttribute("class", "bi")', helper)
        self.assertIn("svg.innerHTML = definition.body", helper)
        self.assertNotIn('element("svg"', helper)

    def test_generated_dashboard_embeds_full_cup_and_leaf_svg_favicon(self) -> None:
        html = dashboard.render_dashboard([model("Alpha", "ACTIVE")])
        match = re.search(r'<link rel="icon" type="image/svg\+xml" href="([^"]+)">', html)
        self.assertIsNotNone(match)
        href = match.group(1)  # type: ignore[union-attr]
        self.assertTrue(href.startswith("data:image/svg+xml,"))
        favicon = unquote(href.removeprefix("data:image/svg+xml,"))
        self.assertEqual(favicon, dashboard.DASHBOARD_FAVICON_SVG)
        self.assertIn('fill="#edf4ef" d="M8 25h27v11H8zM11 36h23v5H11z"', favicon)
        self.assertIn('fill="#edf4ef" d="M35 27h7v8h-7"', favicon)
        self.assertIn('fill="#70bb89"', favicon)
        self.assertNotIn('fill="#0c2118"', favicon)
        self.assertNotIn("class=", favicon)
        self.assertEqual(favicon.count("<path "), 4)

    def test_server_pill_labels_use_only_canonical_deployed_canaries(self) -> None:
        self.assertEqual(dashboard.server_pill_label("CURRENT", 10), "CURRENT")
        self.assertEqual(dashboard.server_pill_label("OUTDATED", 10), "OUTDATED · C10")
        self.assertEqual(dashboard.server_pill_label("NOT_DEPLOYED", 10), "NOT DEPLOYED")
        self.assertEqual(dashboard.server_pill_label("OUTDATED", None), "OUTDATED")

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
