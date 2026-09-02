#!/usr/bin/env python3
"""Retain one finalized ignored artifact in the repository's primary checkout."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import stat
import subprocess
import sys
import tempfile
from pathlib import Path, PurePosixPath
from typing import BinaryIO

try:
    from .workbench import ValidationError, load_json, validate_status
except ImportError:  # Direct execution from tools/.
    from workbench import ValidationError, load_json, validate_status  # type: ignore


CHUNK_SIZE = 1024 * 1024
WINDOWS_REPARSE_POINT = 0x400


class RetentionError(ValidationError):
    """A fail-closed canonical artifact retention failure."""


def _run_git(root: Path, *arguments: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    command = ["git", "-c", f"safe.directory={root}", "-C", str(root), *arguments]
    environment = {key: value for key, value in os.environ.items() if not key.upper().startswith("GIT_")}
    try:
        result = subprocess.run(
            command,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            env=environment,
        )
    except OSError as exc:
        raise RetentionError(f"cannot execute Git in {root}: {exc}") from exc
    if check and result.returncode != 0:
        detail = result.stderr.strip() or result.stdout.strip() or f"exit code {result.returncode}"
        raise RetentionError(f"Git {' '.join(arguments)} failed in {root}: {detail}")
    return result


def _same_path(left: Path, right: Path) -> bool:
    try:
        return os.path.samefile(left, right)
    except OSError:
        return os.path.normcase(str(left.resolve())) == os.path.normcase(str(right.resolve()))


def _is_link_or_reparse(path: Path) -> bool:
    try:
        metadata = path.lstat()
    except OSError as exc:
        raise RetentionError(f"cannot inspect path component {path}: {exc}") from exc
    return stat.S_ISLNK(metadata.st_mode) or bool(
        getattr(metadata, "st_file_attributes", 0) & WINDOWS_REPARSE_POINT
    )


def _assert_plain_components(
    boundary: Path,
    target: Path,
    label: str,
    *,
    allow_missing: bool = False,
) -> None:
    """Reject path redirection between one trusted boundary and target."""

    try:
        relative = target.relative_to(boundary)
    except ValueError as exc:
        raise RetentionError(f"{label} is outside its required boundary: {target}") from exc
    current = boundary
    missing = False
    for part in (Path("."), *relative.parts):
        if part != Path("."):
            current /= part
        if missing or not os.path.lexists(current):
            if allow_missing:
                missing = True
                continue
            raise RetentionError(f"missing {label} path component: {current}")
        if _is_link_or_reparse(current):
            raise RetentionError(f"{label} path must not contain a symlink, junction, or reparse point: {current}")


def _git_path(root: Path, *arguments: str) -> Path:
    value = _run_git(root, *arguments).stdout.strip()
    if not value:
        raise RetentionError(f"Git {' '.join(arguments)} returned an empty path")
    path = Path(value)
    if not path.is_absolute():
        path = root / path
    try:
        return path.resolve(strict=True)
    except OSError as exc:
        raise RetentionError(f"Git path does not resolve: {path}: {exc}") from exc


def resolve_checkouts(root: Path) -> tuple[Path, Path, Path]:
    """Return exact task root, common Git directory, and derived primary root."""

    try:
        task_root = root.resolve(strict=True)
    except OSError as exc:
        raise RetentionError(f"task root does not resolve: {root}: {exc}") from exc
    task_top = _git_path(task_root, "rev-parse", "--path-format=absolute", "--show-toplevel")
    if not _same_path(task_root, task_top):
        raise RetentionError(f"--root must be the exact task worktree root: {task_top}")

    common_dir = _git_path(task_root, "rev-parse", "--path-format=absolute", "--git-common-dir")
    if not common_dir.is_dir():
        raise RetentionError(f"Git common directory is not a directory: {common_dir}")
    primary_root = common_dir.parent.resolve()
    primary_marker = primary_root / ".git"
    if (
        not primary_marker.is_dir()
        or _is_link_or_reparse(primary_marker)
        or not _same_path(primary_marker, common_dir)
    ):
        raise RetentionError(
            "cannot derive a primary checkout from the Git common directory; "
            "bare and separate-git-dir layouts are unsupported"
        )
    primary_top = _git_path(primary_root, "rev-parse", "--path-format=absolute", "--show-toplevel")
    if not _same_path(primary_root, primary_top):
        raise RetentionError(f"derived primary checkout is not its Git top level: {primary_root}")
    primary_common = _git_path(primary_root, "rev-parse", "--path-format=absolute", "--git-common-dir")
    if not _same_path(common_dir, primary_common):
        raise RetentionError("derived primary checkout does not share the task worktree's Git common directory")
    return task_root, common_dir, primary_root


def _normalize_project(project: str) -> tuple[str, str, str]:
    normalized = project.replace("\\", "/")
    parsed = PurePosixPath(normalized)
    if (
        normalized != parsed.as_posix()
        or parsed.is_absolute()
        or len(parsed.parts) != 2
        or parsed.parts[0] not in {"projects", "resourcepacks"}
        or any(part in {"", ".", ".."} for part in parsed.parts)
    ):
        raise RetentionError("project must be exactly projects/<project> or resourcepacks/<project>")
    return normalized, parsed.parts[0], parsed.parts[1]


def _exact_child(parent: Path, name: str, *, required: bool) -> Path | None:
    try:
        entries = list(os.scandir(parent))
    except OSError as exc:
        if not required and isinstance(exc, FileNotFoundError):
            return None
        raise RetentionError(f"cannot inspect {parent}: {exc}") from exc
    exact = [entry for entry in entries if entry.name == name]
    variants = [entry.name for entry in entries if entry.name.casefold() == name.casefold() and entry.name != name]
    if variants:
        raise RetentionError(f"filename casing differs from WORKBENCH_STATUS.json: {variants[0]!r} != {name!r}")
    if len(exact) > 1:
        raise RetentionError(f"more than one exact directory entry exists for {parent / name}")
    if exact:
        return Path(exact[0].path)
    if required:
        raise RetentionError(f"missing finalized current artifact: {parent / name}")
    return None


def _require_regular_file(path: Path, label: str) -> None:
    try:
        metadata = path.lstat()
    except OSError as exc:
        raise RetentionError(f"cannot inspect {label} {path}: {exc}") from exc
    if (
        stat.S_ISLNK(metadata.st_mode)
        or bool(getattr(metadata, "st_file_attributes", 0) & WINDOWS_REPARSE_POINT)
        or not stat.S_ISREG(metadata.st_mode)
    ):
        raise RetentionError(f"{label} must be a regular file, not a link or reparse point: {path}")


def _fingerprint(path: Path) -> tuple[str, int]:
    digest = hashlib.sha256()
    size = 0
    try:
        with path.open("rb") as source:
            while chunk := source.read(CHUNK_SIZE):
                digest.update(chunk)
                size += len(chunk)
    except OSError as exc:
        raise RetentionError(f"cannot hash artifact {path}: {exc}") from exc
    return digest.hexdigest(), size


def _files_equal(left: Path, right: Path) -> bool:
    try:
        with left.open("rb") as left_file, right.open("rb") as right_file:
            while True:
                left_chunk = left_file.read(CHUNK_SIZE)
                right_chunk = right_file.read(CHUNK_SIZE)
                if left_chunk != right_chunk:
                    return False
                if not left_chunk:
                    return True
    except OSError as exc:
        raise RetentionError(f"cannot compare complete artifact bytes: {exc}") from exc


def _git_boolean(root: Path, *arguments: str) -> bool:
    result = _run_git(root, *arguments, check=False)
    if result.returncode not in {0, 1}:
        detail = result.stderr.strip() or result.stdout.strip() or f"exit code {result.returncode}"
        raise RetentionError(f"Git {' '.join(arguments)} failed in {root}: {detail}")
    return result.returncode == 0


def _is_tracked(root: Path, relative_path: str) -> bool:
    return _git_boolean(root, "ls-files", "--error-unmatch", "--", relative_path)


def _is_ignored(root: Path, relative_path: str) -> bool:
    return _git_boolean(root, "check-ignore", "--quiet", "--no-index", "--", relative_path)


def _exclude_pattern(relative_path: str) -> str:
    escaped = "".join("\\" + character if character in "\\[]*?" else character for character in relative_path)
    return "/" + escaped


def _ensure_common_exclude(common_dir: Path, relative_path: str) -> None:
    """Append one exact local ignore rule without replacing existing exclude content."""

    info_dir = common_dir / "info"
    _assert_plain_components(common_dir, info_dir, "Git info directory", allow_missing=True)
    try:
        info_dir.mkdir(parents=True, exist_ok=True)
    except OSError as exc:
        raise RetentionError(f"cannot create Git info directory {info_dir}: {exc}") from exc
    _assert_plain_components(common_dir, info_dir, "Git info directory")
    exclude_path = info_dir / "exclude"
    lock_path = info_dir / "mynx-artifact-retention.lock"
    if os.path.lexists(exclude_path):
        _require_regular_file(exclude_path, "Git exclude file")
    if os.path.lexists(lock_path) and _is_link_or_reparse(lock_path):
        raise RetentionError(f"Git exclude lock must not be a link or reparse point: {lock_path}")
    pattern = _exclude_pattern(relative_path).encode("utf-8")
    try:
        lock_descriptor = os.open(lock_path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    except FileExistsError as exc:
        raise RetentionError(f"another canonical artifact retention is updating {exclude_path}; retry later") from exc
    except OSError as exc:
        raise RetentionError(f"cannot lock Git exclude file {exclude_path}: {exc}") from exc
    try:
        os.close(lock_descriptor)
    except OSError as exc:
        try:
            lock_path.unlink()
        except OSError:
            pass
        raise RetentionError(f"cannot initialize Git exclude lock {lock_path}: {exc}") from exc
    primary_error: BaseException | None = None
    try:
        try:
            existing = exclude_path.read_bytes() if exclude_path.exists() else b""
        except OSError as exc:
            raise RetentionError(f"cannot read Git exclude file {exclude_path}: {exc}") from exc
        if pattern in existing.splitlines():
            return
        separator = b"" if not existing or existing.endswith((b"\n", b"\r")) else b"\n"
        payload = separator + pattern + b"\n"
        try:
            descriptor = os.open(exclude_path, os.O_CREAT | os.O_WRONLY | os.O_APPEND, 0o600)
            try:
                remaining = memoryview(payload)
                while remaining:
                    written = os.write(descriptor, remaining)
                    if written <= 0:
                        raise OSError("Git exclude append made no forward progress")
                    remaining = remaining[written:]
                os.fsync(descriptor)
            finally:
                os.close(descriptor)
        except OSError as exc:
            raise RetentionError(f"cannot append exact local ignore rule to {exclude_path}: {exc}") from exc
        try:
            updated = exclude_path.read_bytes()
        except OSError as exc:
            raise RetentionError(f"cannot verify Git exclude file {exclude_path}: {exc}") from exc
        if not updated.startswith(existing) or pattern not in updated.splitlines():
            raise RetentionError(f"exact local ignore rule could not be verified in {exclude_path}")
    except BaseException as exc:
        primary_error = exc
        raise
    finally:
        try:
            lock_path.unlink()
        except OSError as exc:
            cleanup_error = RetentionError(f"cannot release Git exclude lock {lock_path}: {exc}")
            if primary_error is None:
                raise cleanup_error from exc
            primary_error.add_note(str(cleanup_error))


def _copy_to_open_file(source: Path, destination: BinaryIO) -> tuple[str, int]:
    digest = hashlib.sha256()
    size = 0
    try:
        with source.open("rb") as source_file:
            while chunk := source_file.read(CHUNK_SIZE):
                destination.write(chunk)
                digest.update(chunk)
                size += len(chunk)
        destination.flush()
        os.fsync(destination.fileno())
    except OSError as exc:
        raise RetentionError(f"cannot stage canonical artifact bytes from {source}: {exc}") from exc
    return digest.hexdigest(), size


def _verify_destination(source: Path, destination: Path, expected_hash: str, expected_size: int) -> None:
    _require_regular_file(destination, "canonical destination")
    destination_hash, destination_size = _fingerprint(destination)
    if destination_hash != expected_hash:
        raise RetentionError(
            f"refusing to overwrite canonical destination with different SHA-256: "
            f"expected {expected_hash}, found {destination_hash}: {destination}"
        )
    if destination_size != expected_size or not _files_equal(source, destination):
        raise RetentionError(f"canonical destination does not contain the complete source bytes: {destination}")


def retain_current_artifact(root: Path, project: str) -> dict[str, object]:
    task_root, common_dir, primary_root = resolve_checkouts(root)
    project_relative, container, project_id = _normalize_project(project)
    project_directory = task_root / container / project_id
    manifest_path = project_directory / "WORKBENCH_STATUS.json"
    _assert_plain_components(task_root, project_directory, "task project directory")
    _assert_plain_components(task_root, manifest_path, "task project manifest")
    manifest = load_json(manifest_path)
    validate_status(manifest, project_directory)
    current = manifest["state"]["releases"]["current"]
    if current is None or current["artifact"] is None:
        raise RetentionError(f"{manifest_path}: current release has no finalized artifact")
    filename = current["artifact"]["filename"]
    expected_hash = current["artifact"]["sha256"]
    relative_path = f"{project_relative}/artifacts/{filename}"

    source_parent = project_directory / "artifacts"
    _assert_plain_components(task_root, source_parent, "source artifact directory")
    source = _exact_child(source_parent, filename, required=True)
    assert source is not None
    _assert_plain_components(task_root, source, "source artifact")
    _require_regular_file(source, "source artifact")
    actual_hash, source_size = _fingerprint(source)
    if actual_hash != expected_hash:
        raise RetentionError(
            f"source artifact SHA-256 does not match WORKBENCH_STATUS.json: "
            f"expected {expected_hash}, found {actual_hash}: {source}"
        )

    if _is_tracked(task_root, relative_path):
        raise RetentionError(f"source artifact is tracked by Git; local retention is only for untracked artifacts: {relative_path}")
    if not _is_ignored(task_root, relative_path):
        raise RetentionError(f"source artifact is not ignored by Git: {relative_path}")
    if _is_tracked(primary_root, relative_path):
        raise RetentionError(f"canonical destination path is tracked in the primary checkout: {relative_path}")

    _ensure_common_exclude(common_dir, relative_path)
    for checkout in (task_root, primary_root):
        if not _is_ignored(checkout, relative_path):
            raise RetentionError(f"exact local ignore rule is not effective in {checkout}: {relative_path}")

    destination_parent = primary_root / container / project_id / "artifacts"
    _assert_plain_components(primary_root, destination_parent, "canonical artifact directory", allow_missing=True)
    try:
        destination_parent.mkdir(parents=True, exist_ok=True)
        _assert_plain_components(primary_root, destination_parent, "canonical artifact directory")
        resolved_destination_parent = destination_parent.resolve(strict=True)
        resolved_destination_parent.relative_to(primary_root)
    except (OSError, ValueError) as exc:
        raise RetentionError(f"canonical artifact directory is unsafe or unavailable: {destination_parent}: {exc}") from exc
    destination = _exact_child(destination_parent, filename, required=False)
    if destination is not None:
        _verify_destination(source, destination, expected_hash, source_size)
        status_value = "already_retained"
    else:
        staging_directory = common_dir / "mynx-artifact-retention-staging"
        _assert_plain_components(common_dir, staging_directory, "private artifact staging directory", allow_missing=True)
        try:
            staging_directory.mkdir(exist_ok=True)
            _assert_plain_components(common_dir, staging_directory, "private artifact staging directory")
            descriptor, stage_name = tempfile.mkstemp(prefix="artifact-", suffix=".tmp", dir=staging_directory)
        except OSError as exc:
            raise RetentionError(f"cannot create canonical artifact staging file: {exc}") from exc
        stage = Path(stage_name)
        primary_error = None
        try:
            with os.fdopen(descriptor, "wb") as stage_file:
                staged_hash, staged_size = _copy_to_open_file(source, stage_file)
            if staged_hash != expected_hash or staged_size != source_size:
                raise RetentionError("staged artifact hash or byte count differs from the verified source")
            verified_stage_hash, verified_stage_size = _fingerprint(stage)
            verified_source_hash, verified_source_size = _fingerprint(source)
            if (
                verified_stage_hash != expected_hash
                or verified_stage_size != source_size
                or verified_source_hash != expected_hash
                or verified_source_size != source_size
                or not _files_equal(source, stage)
            ):
                raise RetentionError("source artifact changed or staged bytes are incomplete before atomic publication")
            try:
                os.link(stage, destination_parent / filename)
            except FileExistsError:
                destination = _exact_child(destination_parent, filename, required=True)
                assert destination is not None
                _verify_destination(source, destination, expected_hash, source_size)
                status_value = "already_retained"
            except OSError as exc:
                raced_destination = _exact_child(destination_parent, filename, required=False)
                if raced_destination is not None:
                    _verify_destination(source, raced_destination, expected_hash, source_size)
                    status_value = "already_retained"
                else:
                    raise RetentionError(f"cannot atomically publish canonical artifact without overwrite: {exc}") from exc
            else:
                destination = _exact_child(destination_parent, filename, required=True)
                assert destination is not None
                status_value = "copied"
            _verify_destination(source, destination, expected_hash, source_size)
        except BaseException as exc:
            primary_error = exc
            raise
        finally:
            try:
                stage.unlink(missing_ok=True)
            except OSError as exc:
                cleanup_error = RetentionError(f"cannot remove private artifact staging file {stage}: {exc}")
                if primary_error is None:
                    raise cleanup_error from exc
                primary_error.add_note(str(cleanup_error))

    if _is_tracked(task_root, relative_path) or _is_tracked(primary_root, relative_path):
        raise RetentionError(f"retained artifact became tracked unexpectedly: {relative_path}")
    if not _is_ignored(task_root, relative_path) or not _is_ignored(primary_root, relative_path):
        raise RetentionError(f"retained artifact is not ignored in every local checkout: {relative_path}")
    _verify_destination(source, destination, expected_hash, source_size)
    return {
        "artifact": relative_path,
        "destination": str(destination),
        "primary_checkout": str(primary_root),
        "sha256": expected_hash,
        "size": source_size,
        "status": status_value,
    }


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("project", help="exact repository path: projects/<project> or resourcepacks/<project>")
    parser.add_argument("--root", type=Path, default=Path("."), help="task worktree root (default: current directory)")
    return parser


def main(argv: list[str] | None = None) -> int:
    args = _build_parser().parse_args(argv)
    try:
        result = retain_current_artifact(args.root, args.project)
    except ValidationError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
