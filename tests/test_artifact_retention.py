from __future__ import annotations

import hashlib
import json
import os
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from tools import artifact_retention
from tools.artifact_retention import RetentionError, retain_current_artifact


ARTIFACT_BYTES = b"private-final-artifact\x00complete-bytes\n"
TIMESTAMP = "2026-09-02T12:00:00Z"


def sha256(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def manifest(container: str, project_id: str, filename: str, artifact_hash: str) -> dict:
    return {
        "$schema": "../../schemas/workbench-status.schema.json",
        "schema_version": 2,
        "identity": {
            "uuid": "9c9592a8-8692-48c5-9127-715825ce9c47",
            "name": "Private Retention Fixture",
            "project_id": project_id,
            "aliases": [],
            "legacy_names": [],
            "legacy_ids": [],
        },
        "definition": {
            "lifecycle": "ACTIVE",
            "goals": ["Exercise canonical local artifact retention."],
            "scope": ["Temporary test fixture only."],
            "boundaries": {
                "owned_paths": [f"{container}/{project_id}"],
                "exclusions": ["No runtime deployment."],
            },
            "dependencies": [],
        },
        "state": {
            "milestone": "Current private artifact finalized.",
            "releases": {
                "current": {
                    "version": "Canary 1",
                    "artifact": {"filename": filename, "sha256": artifact_hash},
                    "source_commit": "a" * 40,
                },
                "accepted": None,
                "rollback": None,
                "accepted_current": "NO_ACCEPTED",
                "accepted_rollback": "NO_ROLLBACK",
            },
            "validation": {
                "build": "STATIC_PASS",
                "runtime": "RUNTIME_UNTESTED",
            },
            "blocker": None,
        },
        "synchronization": {
            "revision": 1,
            "activity_at": TIMESTAMP,
            "updated_at": TIMESTAMP,
            "last_codex_at": TIMESTAMP,
            "source_commit": "b" * 40,
        },
    }


class RetentionRepository:
    def __init__(
        self,
        case: unittest.TestCase,
        *,
        container: str = "projects",
        filename: str = "private-canary.jar",
        expected_bytes: bytes = ARTIFACT_BYTES,
        source_bytes: bytes | None = None,
        source_name: str | None = None,
        ignored: bool = True,
    ) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        case.addCleanup(self.temporary.cleanup)
        temporary_root = Path(self.temporary.name)
        self.primary = temporary_root / "primary"
        self.task = temporary_root / "task-worktree"
        self.container = container
        self.project_id = "private-fixture"
        self.project = f"{container}/{self.project_id}"
        self.filename = filename
        self.relative = f"{self.project}/artifacts/{filename}"
        self.destination = self.primary / self.relative

        self._git(temporary_root, "init", "-b", "primary-stale", str(self.primary))
        self._git(self.primary, "config", "user.email", "retention-tests@example.invalid")
        self._git(self.primary, "config", "user.name", "Retention Tests")
        primary_project = self.primary / container / self.project_id
        primary_project.mkdir(parents=True)
        (primary_project / "WORKBENCH_STATUS.json").write_text(
            json.dumps(manifest(container, self.project_id, filename, sha256(expected_bytes)), indent=2) + "\n",
            encoding="utf-8",
        )
        self._git(self.primary, "add", ".")
        self._git(self.primary, "commit", "-m", "Initialize retention fixture")
        self._git(self.primary, "branch", "main")
        self._git(self.primary, "worktree", "add", "-b", "task", str(self.task), "main")

        task_project = self.task / container / self.project_id
        if ignored:
            (task_project / ".gitignore").write_text(f"/artifacts/{filename}\n", encoding="utf-8")
            self._git(self.task, "add", f"{self.project}/.gitignore")
            self._git(self.task, "commit", "-m", "Ignore finalized private artifact")
        source_parent = task_project / "artifacts"
        source_parent.mkdir()
        self.source = source_parent / (source_name or filename)
        self.source.write_bytes(expected_bytes if source_bytes is None else source_bytes)
        self.common_dir = self.primary / ".git"
        self.exclude = self.common_dir / "info" / "exclude"
        self.exclude_before = self.exclude.read_bytes()

    @staticmethod
    def _git(root: Path, *arguments: str, check: bool = True) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            ["git", "-c", f"safe.directory={root}", "-C", str(root), *arguments],
            check=check,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
        )

    def git(self, root: Path, *arguments: str, check: bool = True) -> subprocess.CompletedProcess[str]:
        return self._git(root, *arguments, check=check)

    @property
    def exclude_pattern(self) -> bytes:
        return ("/" + self.relative).encode("utf-8")


class ArtifactRetentionTests(unittest.TestCase):
    def test_real_linked_worktree_retains_projects_and_resourcepacks(self) -> None:
        for container in ("projects", "resourcepacks"):
            with self.subTest(container=container):
                fixture = RetentionRepository(self, container=container)
                self.assertEqual("primary-stale", fixture.git(fixture.primary, "branch", "--show-current").stdout.strip())
                self.assertNotEqual("main", fixture.git(fixture.task, "branch", "--show-current").stdout.strip())

                result = retain_current_artifact(fixture.task, fixture.project)

                self.assertEqual("copied", result["status"])
                self.assertEqual(fixture.primary.resolve(), Path(result["primary_checkout"]))
                self.assertEqual(ARTIFACT_BYTES, fixture.destination.read_bytes())
                self.assertEqual(ARTIFACT_BYTES, fixture.source.read_bytes())
                exclude_after = fixture.exclude.read_bytes()
                self.assertTrue(exclude_after.startswith(fixture.exclude_before))
                self.assertEqual(1, exclude_after.splitlines().count(fixture.exclude_pattern))
                for checkout in (fixture.task, fixture.primary):
                    self.assertEqual(
                        0,
                        fixture.git(
                            checkout,
                            "check-ignore",
                            "--quiet",
                            "--no-index",
                            "--",
                            fixture.relative,
                            check=False,
                        ).returncode,
                    )
                    self.assertEqual(
                        1,
                        fixture.git(
                            checkout,
                            "ls-files",
                            "--error-unmatch",
                            "--",
                            fixture.relative,
                            check=False,
                        ).returncode,
                    )

                replay = retain_current_artifact(fixture.task, fixture.project)
                self.assertEqual("already_retained", replay["status"])
                self.assertEqual(1, fixture.exclude.read_bytes().splitlines().count(fixture.exclude_pattern))

    def test_source_filename_and_hash_must_match_manifest(self) -> None:
        wrong_name = RetentionRepository(self, source_name="different-name.jar")
        with self.assertRaisesRegex(RetentionError, "missing finalized current artifact"):
            retain_current_artifact(wrong_name.task, wrong_name.project)
        self.assertFalse(wrong_name.destination.exists())
        self.assertNotIn(wrong_name.exclude_pattern, wrong_name.exclude.read_bytes().splitlines())

        wrong_hash = RetentionRepository(self, source_bytes=b"wrong artifact bytes")
        with self.assertRaisesRegex(RetentionError, "SHA-256 does not match"):
            retain_current_artifact(wrong_hash.task, wrong_hash.project)
        self.assertFalse(wrong_hash.destination.exists())
        self.assertNotIn(wrong_hash.exclude_pattern, wrong_hash.exclude.read_bytes().splitlines())

    def test_source_must_already_be_ignored(self) -> None:
        fixture = RetentionRepository(self, ignored=False)
        with self.assertRaisesRegex(RetentionError, "source artifact is not ignored"):
            retain_current_artifact(fixture.task, fixture.project)
        self.assertFalse(fixture.destination.exists())
        self.assertNotIn(fixture.exclude_pattern, fixture.exclude.read_bytes().splitlines())

    def test_source_and_primary_destination_must_both_remain_untracked(self) -> None:
        tracked_source = RetentionRepository(self)
        tracked_source.git(tracked_source.task, "add", "-f", tracked_source.relative)
        tracked_source.git(tracked_source.task, "commit", "-m", "Track source only in task branch")
        with self.assertRaisesRegex(RetentionError, "source artifact is tracked"):
            retain_current_artifact(tracked_source.task, tracked_source.project)
        self.assertFalse(tracked_source.destination.exists())

        tracked_primary = RetentionRepository(self)
        tracked_primary.destination.parent.mkdir(parents=True)
        tracked_primary.destination.write_bytes(ARTIFACT_BYTES)
        tracked_primary.git(tracked_primary.primary, "add", tracked_primary.relative)
        tracked_primary.git(tracked_primary.primary, "commit", "-m", "Track destination only in primary branch")
        with self.assertRaisesRegex(RetentionError, "destination path is tracked"):
            retain_current_artifact(tracked_primary.task, tracked_primary.project)
        self.assertEqual(ARTIFACT_BYTES, tracked_primary.destination.read_bytes())

    def test_ambient_git_index_override_cannot_hide_a_tracked_source(self) -> None:
        fixture = RetentionRepository(self)
        fixture.git(fixture.task, "add", "-f", fixture.relative)
        fixture.git(fixture.task, "commit", "-m", "Track source in real task index")
        alternate_index = fixture.primary.parent / "alternate-index"
        with patch.dict(os.environ, {"GIT_INDEX_FILE": str(alternate_index)}):
            with self.assertRaisesRegex(RetentionError, "source artifact is tracked"):
                retain_current_artifact(fixture.task, fixture.project)

    def test_linked_source_artifact_directory_is_rejected(self) -> None:
        fixture = RetentionRepository(self)
        source_parent = fixture.source.parent
        external_parent = fixture.primary.parent / "external-private-artifacts"
        source_parent.rename(external_parent)
        try:
            source_parent.symlink_to(external_parent, target_is_directory=True)
        except OSError as exc:
            self.skipTest(f"directory symlinks are unavailable: {exc}")
        with self.assertRaisesRegex(RetentionError, "symlink, junction, or reparse point"):
            retain_current_artifact(fixture.task, fixture.project)
        self.assertFalse(fixture.destination.exists())

    def test_short_local_exclude_write_is_completed_and_verified(self) -> None:
        fixture = RetentionRepository(self)
        real_write = os.write
        calls = 0

        def short_once(descriptor: int, content) -> int:
            nonlocal calls
            calls += 1
            if calls == 1 and len(content) > 1:
                return real_write(descriptor, content[: len(content) // 2])
            return real_write(descriptor, content)

        with patch.object(artifact_retention.os, "write", side_effect=short_once):
            result = retain_current_artifact(fixture.task, fixture.project)
        self.assertEqual("copied", result["status"])
        self.assertGreaterEqual(calls, 2)
        self.assertEqual(1, fixture.exclude.read_bytes().splitlines().count(fixture.exclude_pattern))

    def test_existing_identical_is_idempotent_and_different_is_preserved(self) -> None:
        identical = RetentionRepository(self)
        identical.destination.parent.mkdir(parents=True)
        identical.destination.write_bytes(ARTIFACT_BYTES)
        result = retain_current_artifact(identical.task, identical.project)
        self.assertEqual("already_retained", result["status"])
        self.assertEqual(ARTIFACT_BYTES, identical.destination.read_bytes())

        different = RetentionRepository(self)
        different.destination.parent.mkdir(parents=True)
        different.destination.write_bytes(b"pre-existing different private bytes")
        with self.assertRaisesRegex(RetentionError, "refusing to overwrite.*different SHA-256"):
            retain_current_artifact(different.task, different.project)
        self.assertEqual(b"pre-existing different private bytes", different.destination.read_bytes())

    def test_destination_that_wins_publish_race_is_never_overwritten(self) -> None:
        fixture = RetentionRepository(self)
        winner = b"concurrent winner with a different hash"

        def publish_winner(_stage: Path, destination: Path) -> None:
            Path(destination).write_bytes(winner)
            raise FileExistsError("simulated no-replace race")

        with patch.object(artifact_retention.os, "link", side_effect=publish_winner):
            with self.assertRaisesRegex(RetentionError, "refusing to overwrite.*different SHA-256"):
                retain_current_artifact(fixture.task, fixture.project)
        self.assertEqual(winner, fixture.destination.read_bytes())

    def test_corrupt_staging_fails_before_atomic_publication_and_cleans_up(self) -> None:
        fixture = RetentionRepository(self)

        def corrupt_copy(_source: Path, destination) -> tuple[str, int]:
            destination.write(b"truncated")
            destination.flush()
            os.fsync(destination.fileno())
            return sha256(ARTIFACT_BYTES), len(ARTIFACT_BYTES)

        with patch.object(artifact_retention, "_copy_to_open_file", side_effect=corrupt_copy):
            with self.assertRaisesRegex(RetentionError, "source artifact changed or staged bytes are incomplete"):
                retain_current_artifact(fixture.task, fixture.project)
        self.assertFalse(fixture.destination.exists())
        staging = fixture.common_dir / "mynx-artifact-retention-staging"
        self.assertEqual([], list(staging.iterdir()))

    def test_post_publication_corruption_fails_destination_verification(self) -> None:
        fixture = RetentionRepository(self)
        real_link = os.link

        def publish_then_corrupt(stage: Path, destination: Path) -> None:
            real_link(stage, destination)
            Path(destination).write_bytes(b"corrupted after atomic publication")

        with patch.object(artifact_retention.os, "link", side_effect=publish_then_corrupt):
            with self.assertRaisesRegex(RetentionError, "different SHA-256"):
                retain_current_artifact(fixture.task, fixture.project)
        self.assertEqual(b"corrupted after atomic publication", fixture.destination.read_bytes())


if __name__ == "__main__":
    unittest.main()
