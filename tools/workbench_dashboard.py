#!/usr/bin/env python3
"""Generate the local, self-contained Mynx dashboard."""

from __future__ import annotations

import argparse
import calendar
import hashlib
import json
import os
import re
import stat
import subprocess
import sys
import unicodedata
import webbrowser
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable, Mapping
from urllib.parse import quote
from uuid import UUID

try:
    from .workbench import ValidationError, load_json, load_repository_statuses
except ImportError:  # Direct execution from tools/.
    from workbench import ValidationError, load_json, load_repository_statuses  # type: ignore


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = "WORKBENCH_DASHBOARD.html"
SERVER_STATE_FILENAME = "WORKBENCH_SERVER_STATE.json"
BOOTSTRAP_SPRITE = ROOT / "third_party" / "bootstrap-icons" / "bootstrap-icons.svg"
DASHBOARD_FAVICON_SVG = (
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48">'
    '<path fill="#edf4ef" d="M8 25h27v11H8zM11 36h23v5H11z"/>'
    '<path fill="#edf4ef" d="M35 27h7v8h-7"/>'
    '<path fill="#70bb89" stroke="#9bc5aa" stroke-width="1.5" stroke-linejoin="round" d="M23 23C24 12 32 7 42 9c-2 10-8 16-19 14Z"/>'
    '<path fill="none" stroke="#0c2118" stroke-width="2" stroke-linecap="round" d="m21 25 9-10"/>'
    '</svg>'
)
DASHBOARD_FAVICON_DATA_URL = "data:image/svg+xml," + quote(DASHBOARD_FAVICON_SVG, safe="")
DASHBOARD_ICON_NAMES = frozenset(
    {
        "search",
        "x-lg",
        "arrow-down-up",
        "sort-up",
        "sort-down",
        "chevron-down",
        "chevron-right",
        "arrow-clockwise",
    }
)
SERVER_STATES = {"CURRENT", "OUTDATED", "NOT_DEPLOYED"}
SOURCE_COMMIT_RE = re.compile(r"^[0-9a-f]{40}$")
RFC3339_UTC_RE = re.compile(r"^(?P<whole>\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(?P<fraction>\d{1,9}))?Z$")
LIFECYCLE_ORDER = ("ACTIVE", "PLANNED", "ACCEPTED", "BLOCKED", "PARKED")
LIFECYCLE_PRIORITY = {lifecycle: index for index, lifecycle in enumerate(LIFECYCLE_ORDER)}
CHUNK_SIZE = 1024 * 1024
WINDOWS_REPARSE_POINT = 0x400


class DashboardError(ValidationError):
    """A deterministic dashboard data or command failure."""


@dataclass(frozen=True)
class DashboardProject:
    uuid: str
    project_id: str
    name: str
    lifecycle: str
    current_version: str | None
    jar_mtime_ns: int | None
    jar_mtime_iso: str | None
    jar_note: str
    server_status: str
    deployed_release: dict[str, Any] | None
    current_summary: str | None = None
    current_canary: int | None = None
    deployed_canary: int | None = None
    record_kind: str = "project"


def server_pill_label(server_status: str, deployed_canary: int | None) -> str:
    """Return the intentionally compact, release-aware server-state label."""

    if server_status not in SERVER_STATES:
        raise DashboardError(f"unsupported dashboard server status {server_status!r}")
    if server_status != "OUTDATED":
        return server_status.replace("_", " ")
    return "OUTDATED" if deployed_canary is None else f"OUTDATED · C{deployed_canary}"


def _release_identity(value: Mapping[str, Any], path: str) -> dict[str, Any]:
    """Validate and copy the durable release/artifact identity for server state."""

    expected = {"version", "artifact"}
    if set(value) != expected:
        raise DashboardError(f"{path}: must contain exactly version and artifact")
    version = value["version"]
    if not isinstance(version, str) or not version or version != version.strip():
        raise DashboardError(f"{path}.version: must be a non-empty trimmed string")
    artifact = value["artifact"]
    if artifact is None:
        return {"version": version, "artifact": None}
    if not isinstance(artifact, Mapping) or set(artifact) != {"filename", "sha256"}:
        raise DashboardError(f"{path}.artifact: must contain exactly filename and sha256")
    filename = artifact["filename"]
    checksum = artifact["sha256"]
    if not isinstance(filename, str) or not filename or filename != filename.strip():
        raise DashboardError(f"{path}.artifact.filename: must be a non-empty trimmed string")
    if not isinstance(checksum, str) or not re.fullmatch(r"[0-9a-f]{64}", checksum):
        raise DashboardError(f"{path}.artifact.sha256: must be a lowercase SHA-256 hex digest")
    return {"version": version, "artifact": {"filename": filename, "sha256": checksum}}


def canonical_release_identity(current_release: Mapping[str, Any] | None) -> dict[str, Any] | None:
    if current_release is None:
        return None
    return _release_identity(
        {"version": current_release["version"], "artifact": current_release["artifact"]},
        "current release",
    )


def deployed_canary_for_release(
    releases: Mapping[str, Any],
    deployed_release: Mapping[str, Any] | None,
) -> int | None:
    """Return canonical Canary metadata for an exact retained release identity."""

    if deployed_release is None:
        return None
    for slot in ("current", "accepted", "rollback"):
        release = releases[slot]
        if release is not None and canonical_release_identity(release) == deployed_release:
            return release.get("canary")
    return None


def _normalized_token(value: str) -> str:
    return unicodedata.normalize("NFKC", value).strip().casefold()


def _canonical_uuid(value: str, path: str) -> str:
    if not isinstance(value, str) or not value or value != value.strip():
        raise DashboardError(f"{path}: must be a canonical UUID string")
    try:
        parsed = UUID(value)
    except (ValueError, AttributeError) as exc:
        raise DashboardError(f"{path}: must be a UUID") from exc
    if str(parsed) != value:
        raise DashboardError(f"{path}: must use canonical lowercase UUID form")
    return value


def load_server_state(root: Path, known_uuids: Iterable[str]) -> dict[str, dict[str, Any]]:
    """Load human-owned deployed-release records; an absent record means not deployed."""

    path = root / SERVER_STATE_FILENAME
    if not path.is_file():
        return {}
    data = load_json(path)
    expected = {"schema_version", "projects"}
    missing = sorted(expected - set(data))
    extra = sorted(set(data) - expected)
    if missing:
        raise DashboardError(f"{path}: missing keys: {', '.join(missing)}")
    if extra:
        raise DashboardError(f"{path}: unknown keys: {', '.join(extra)}")
    if data["schema_version"] != 2:
        raise DashboardError(f"{path}.schema_version: must equal 2")
    projects = data["projects"]
    if not isinstance(projects, dict):
        raise DashboardError(f"{path}.projects: must be an object keyed by project UUID")

    known = set(known_uuids)
    result: dict[str, dict[str, Any]] = {}
    for project_uuid, value in projects.items():
        checked_uuid = _canonical_uuid(project_uuid, f"{path}.projects key")
        if checked_uuid not in known:
            raise DashboardError(f"{path}.projects: unknown project UUID {checked_uuid}")
        if not isinstance(value, Mapping):
            raise DashboardError(f"{path}.projects.{checked_uuid}: must be a deployed release object")
        result[checked_uuid] = _release_identity(value, f"{path}.projects.{checked_uuid}")
    return result


def _atomic_write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + ".tmp")
    try:
        temporary.write_text(content, encoding="utf-8", newline="\n")
        os.replace(temporary, path)
    finally:
        try:
            temporary.unlink(missing_ok=True)
        except OSError:
            pass


def write_server_state(root: Path, projects: Mapping[str, Mapping[str, Any]]) -> None:
    ordered: dict[str, dict[str, Any]] = {}
    for project_uuid in sorted(projects):
        _canonical_uuid(project_uuid, "server state project UUID")
        ordered[project_uuid] = _release_identity(projects[project_uuid], f"server state for {project_uuid}")
    payload = {"schema_version": 2, "projects": ordered}
    _atomic_write_text(
        root / SERVER_STATE_FILENAME,
        json.dumps(payload, indent=2, ensure_ascii=False) + "\n",
    )


def _sha256_and_mtime(path: Path) -> tuple[str, int] | None:
    """Hash one plain file and return its SHA-256 and stable open-handle mtime."""

    try:
        metadata = path.stat(follow_symlinks=False)
        if (
            not stat.S_ISREG(metadata.st_mode)
            or stat.S_ISLNK(metadata.st_mode)
            or bool(getattr(metadata, "st_file_attributes", 0) & WINDOWS_REPARSE_POINT)
        ):
            return None
        digest = hashlib.sha256()
        with path.open("rb") as artifact:
            before = os.fstat(artifact.fileno())
            if not stat.S_ISREG(before.st_mode):
                return None
            while chunk := artifact.read(CHUNK_SIZE):
                digest.update(chunk)
            after = os.fstat(artifact.fileno())
    except OSError:
        return None
    if before.st_size != after.st_size or before.st_mtime_ns != after.st_mtime_ns:
        return None
    return digest.hexdigest(), after.st_mtime_ns


def _exact_artifact_path(project_directory: Path, filename: str) -> Path | None:
    artifact_directory = project_directory / "artifacts"
    try:
        matches = [entry for entry in os.scandir(artifact_directory) if entry.name == filename]
    except OSError:
        return None
    if len(matches) != 1:
        return None
    return Path(matches[0].path)


def _built_at_timestamp(value: Any, path: str) -> tuple[int, str]:
    """Return a canonical UTC timestamp as integer nanoseconds for dashboard order."""

    if not isinstance(value, str):
        raise DashboardError(f"{path}: must be an RFC 3339 UTC timestamp ending in Z")
    match = RFC3339_UTC_RE.fullmatch(value)
    if match is None:
        raise DashboardError(f"{path}: must be an RFC 3339 UTC timestamp ending in Z")
    try:
        parsed = datetime.fromisoformat(match.group("whole") + "+00:00")
    except ValueError as exc:
        raise DashboardError(f"{path}: invalid timestamp") from exc
    fraction = (match.group("fraction") or "").ljust(9, "0")
    return calendar.timegm(parsed.utctimetuple()) * 1_000_000_000 + int(fraction or "0"), value


def _record_kind(manifest_path: Path) -> str:
    """Classify a canonical dashboard record from its manifest location alone."""

    container = manifest_path.parent.parent.name
    if container == "projects":
        return "project"
    if container == "resourcepacks":
        return "resource_pack"
    raise DashboardError(f"{manifest_path}: dashboard records must be under projects/ or resourcepacks/")


def _artifact_details(
    project_directory: Path,
    current_release: Mapping[str, Any] | None,
    record_kind: str,
) -> tuple[str | None, int | None, str | None, str]:
    artifact_label = "JAR" if record_kind == "project" else "ZIP"
    if current_release is None:
        return None, None, None, "No current release."

    version = current_release["version"]
    artifact = current_release["artifact"]
    if artifact is None:
        return version, None, None, f"The current release has no {artifact_label} artifact."
    filename = artifact["filename"]
    expected_extension = ".jar" if record_kind == "project" else ".zip"
    if not filename.casefold().endswith(expected_extension):
        return version, None, None, f"The current artifact is not a {artifact_label}."

    built_at = current_release.get("built_at")
    canonical_timestamp = None if built_at is None else _built_at_timestamp(built_at, "current release.built_at")
    artifact_path = _exact_artifact_path(project_directory, filename)
    if artifact_path is None:
        if canonical_timestamp is not None:
            return version, *canonical_timestamp, f"Canonical build timestamp; the exact current {artifact_label} is not retained locally."
        return version, None, None, f"The exact current {artifact_label} is not retained locally."
    fingerprint = _sha256_and_mtime(artifact_path)
    if fingerprint is None:
        if canonical_timestamp is not None:
            return version, *canonical_timestamp, f"Canonical build timestamp; the exact current {artifact_label} is unavailable or changed while being read."
        return version, None, None, f"The exact current {artifact_label} is unavailable or changed while being read."
    actual_hash, mtime_ns = fingerprint
    if actual_hash != artifact["sha256"]:
        if canonical_timestamp is not None:
            return version, *canonical_timestamp, f"Canonical build timestamp; the retained {artifact_label} does not match the canonical SHA-256."
        return version, None, None, f"The retained {artifact_label} does not match the canonical SHA-256."

    if canonical_timestamp is not None:
        return version, *canonical_timestamp, f"Canonical build timestamp; verified current {artifact_label}: {filename}"
    timestamp = datetime.fromtimestamp(mtime_ns / 1_000_000_000, tz=timezone.utc)
    timestamp_iso = timestamp.isoformat(timespec="microseconds").replace("+00:00", "Z")
    return version, mtime_ns, timestamp_iso, f"Verified current {artifact_label}: {filename}"


def _jar_details(
    project_directory: Path,
    current_release: Mapping[str, Any] | None,
) -> tuple[str | None, int | None, str | None, str]:
    """Compatibility wrapper for project JAR timestamp handling."""

    return _artifact_details(project_directory, current_release, "project")


def default_project_sort_key(project: DashboardProject) -> tuple[Any, ...]:
    return (
        LIFECYCLE_PRIORITY[project.lifecycle],
        project.jar_mtime_ns is None,
        -(project.jar_mtime_ns or 0),
        _normalized_token(project.name),
        project.name,
        project.uuid,
    )


def sort_projects_default(projects: Iterable[DashboardProject]) -> list[DashboardProject]:
    return sorted(projects, key=default_project_sort_key)


def build_project_records(
    statuses: Mapping[str, tuple[Path, dict[str, Any]]],
    server_state: Mapping[str, Mapping[str, Any]],
) -> list[DashboardProject]:
    projects: list[DashboardProject] = []
    for project_uuid, (manifest_path, manifest) in statuses.items():
        record_kind = _record_kind(manifest_path)
        identity = manifest["identity"]
        lifecycle = manifest["definition"]["lifecycle"]
        if lifecycle not in LIFECYCLE_PRIORITY:
            raise DashboardError(f"{manifest_path}: unsupported dashboard lifecycle {lifecycle!r}")
        releases = manifest["state"]["releases"]
        current = releases["current"]
        version, mtime_ns, mtime_iso, jar_note = _artifact_details(manifest_path.parent, current, record_kind)
        current_identity = canonical_release_identity(current)
        deployed_release = server_state.get(project_uuid) if record_kind == "project" else None
        if deployed_release is not None:
            deployed_release = _release_identity(deployed_release, f"server state for {project_uuid}")
        if deployed_release is None:
            server_status = "NOT_DEPLOYED"
        elif deployed_release == current_identity:
            server_status = "CURRENT"
        else:
            server_status = "OUTDATED"
        projects.append(
            DashboardProject(
                uuid=project_uuid,
                project_id=identity["project_id"],
                name=identity["name"],
                lifecycle=lifecycle,
                current_version=version,
                jar_mtime_ns=mtime_ns,
                jar_mtime_iso=mtime_iso,
                jar_note=jar_note,
                server_status=server_status,
                deployed_release=deployed_release,
                current_summary=None if current is None else current.get("summary"),
                current_canary=None if current is None else current["canary"],
                deployed_canary=deployed_canary_for_release(releases, deployed_release),
                record_kind=record_kind,
            )
        )
    return sort_projects_default(projects)


def discover_projects(root: Path) -> tuple[dict[str, tuple[Path, dict[str, Any]]], list[DashboardProject]]:
    statuses = load_repository_statuses(root)
    server_state = load_server_state(root, statuses)
    return statuses, build_project_records(statuses, server_state)


def resolve_project(
    query: str,
    statuses: Mapping[str, tuple[Path, dict[str, Any]]],
) -> tuple[str, dict[str, Any]]:
    stripped = query.strip()
    try:
        project_uuid = str(UUID(stripped))
    except (ValueError, AttributeError):
        project_uuid = ""
    if project_uuid in statuses:
        return project_uuid, statuses[project_uuid][1]

    token = _normalized_token(stripped)
    matches: dict[str, dict[str, Any]] = {}
    for candidate_uuid, (_, manifest) in statuses.items():
        identity = manifest["identity"]
        candidates = [
            identity["project_id"],
            *identity["legacy_ids"],
            identity["name"],
            *identity["aliases"],
            *identity["legacy_names"],
        ]
        if any(_normalized_token(candidate) == token for candidate in candidates):
            matches[candidate_uuid] = manifest
    if not matches:
        raise DashboardError(f"no project matches {query!r}")
    if len(matches) > 1:
        names = sorted(manifest["identity"]["name"] for manifest in matches.values())
        raise DashboardError(f"project reference {query!r} is ambiguous: {', '.join(names)}")
    return next(iter(matches.items()))


def _json_for_html(value: Any) -> str:
    serialized = json.dumps(
        value,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    )
    return (
        serialized.replace("&", "\\u0026")
        .replace("<", "\\u003c")
        .replace(">", "\\u003e")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")
    )


def _project_payload(project: DashboardProject) -> dict[str, Any]:
    current_canary = project.current_canary
    deployed_canary = project.deployed_canary
    current_label = None if current_canary is None else f"C{current_canary}"
    deployed_label = None if deployed_canary is None else f"C{deployed_canary}"
    payload = {
        "kind": project.record_kind,
        "uuid": project.uuid,
        "projectId": project.project_id,
        "name": project.name,
        "lifecycle": project.lifecycle,
        "version": current_label,
        "versionDisplay": current_label,
        "versionCanary": current_canary,
        "lastEditMtimeMs": None if project.jar_mtime_ns is None else project.jar_mtime_ns // 1_000_000,
        "lastEditMtimeIso": project.jar_mtime_iso,
        "lastEditNote": project.jar_note,
        "summary": project.current_summary,
    }
    if project.record_kind == "project":
        payload.update(
            {
                "jarMtimeMs": payload["lastEditMtimeMs"],
                "jarMtimeIso": payload["lastEditMtimeIso"],
                "jarNote": payload["lastEditNote"],
                "server": project.server_status,
                "serverDisplay": server_pill_label(project.server_status, deployed_canary),
                "deployedVersion": deployed_label,
                "deployedVersionDisplay": deployed_label,
                "deployedCanary": deployed_canary,
            }
        )
    return payload


def _source_commit(value: str | None) -> str | None:
    if value is None:
        return None
    if not SOURCE_COMMIT_RE.fullmatch(value):
        raise DashboardError("source commit must be an exact lowercase 40-hex commit")
    return value


def _source_label(value: str | None) -> str | None:
    if value is None:
        return None
    if not isinstance(value, str) or not value or value != value.strip() or "\n" in value or "\r" in value:
        raise DashboardError("source label must be a nonblank single line")
    return value


def resolve_source_commit(root: Path) -> str | None:
    """Resolve local HEAD without assigning it a branch name."""

    try:
        result = subprocess.run(
            ["git", "-C", str(root), "rev-parse", "HEAD"],
            check=False,
            capture_output=True,
            text=True,
        )
    except OSError:
        return None
    candidate = result.stdout.strip()
    return candidate if result.returncode == 0 and SOURCE_COMMIT_RE.fullmatch(candidate) else None


def _bootstrap_icon_geometries(sprite: str) -> dict[str, dict[str, str]]:
    """Extract the required Bootstrap symbol geometry for direct inline use.

    The vendored sprite remains the authoritative artwork, but the generated
    dashboard deliberately does not use SVG fragment references.  Browsers can
    otherwise fail to paint ``<use>`` targets in a local file when the source
    sprite is hidden or the dynamically inserted reference is resolved late.
    """

    symbols: dict[str, dict[str, str]] = {}
    for match in re.finditer(r"<symbol\b(?P<attributes>[^>]*)>(?P<body>.*?)</symbol>", sprite, re.DOTALL):
        attributes = match.group("attributes")
        name = re.search(r'\bid="([^"]+)"', attributes)
        view_box = re.search(r'\bviewBox="([^"]+)"', attributes)
        if name is not None and view_box is not None and name.group(1) in DASHBOARD_ICON_NAMES:
            symbols[name.group(1)] = {"viewBox": view_box.group(1), "body": match.group("body")}
    missing = sorted(DASHBOARD_ICON_NAMES - set(symbols))
    if missing:
        raise DashboardError(
            "Bootstrap Icons sprite is missing dashboard icon symbols: " + ", ".join(missing)
        )
    return symbols


def _inline_bootstrap_icon(icon: Mapping[str, str], class_name: str = "bi") -> str:
    """Render a self-contained SVG from geometry extracted from Bootstrap."""

    return f'<svg class="{class_name}" aria-hidden="true" viewBox="{icon["viewBox"]}">{icon["body"]}</svg>'


def render_dashboard(
    projects: Iterable[DashboardProject],
    generated_at: datetime | None = None,
    *,
    source_commit: str | None = None,
    source_label: str | None = None,
) -> str:
    ordered = sort_projects_default(projects)
    project_records = [project for project in ordered if project.record_kind == "project"]
    resource_pack_records = [project for project in ordered if project.record_kind == "resource_pack"]
    unknown_kinds = sorted({project.record_kind for project in ordered} - {"project", "resource_pack"})
    if unknown_kinds:
        raise DashboardError("unsupported dashboard record kind(s): " + ", ".join(unknown_kinds))
    generated = generated_at or datetime.now(timezone.utc)
    if generated.tzinfo is None:
        raise DashboardError("generated_at must be timezone-aware")
    generated_iso = generated.astimezone(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")
    source = _source_commit(source_commit)
    label = _source_label(source_label)
    if label is not None and source is None:
        raise DashboardError("source label requires a source commit")
    payload = {
        "generatedAt": generated_iso,
        "source": None if source is None else {"commit": source, "label": label},
        "projects": [_project_payload(project) for project in project_records],
        "resourcePacks": [_project_payload(project) for project in resource_pack_records],
        "counts": {"projects": len(project_records), "resourcePacks": len(resource_pack_records)},
    }
    try:
        sprite = BOOTSTRAP_SPRITE.read_text(encoding="utf-8")
    except OSError as exc:
        raise DashboardError(f"Bootstrap Icons sprite is unavailable: {BOOTSTRAP_SPRITE}") from exc
    icons = _bootstrap_icon_geometries(sprite)
    return (
        HTML_TEMPLATE.replace("__DASHBOARD_DATA__", _json_for_html(payload))
        .replace("__BOOTSTRAP_ICON_DATA__", _json_for_html(icons))
        .replace("__DASHBOARD_FAVICON_DATA_URL__", DASHBOARD_FAVICON_DATA_URL)
        .replace("__ICON_SEARCH__", _inline_bootstrap_icon(icons["search"], "bi search-icon"))
        .replace("__ICON_X_LG__", _inline_bootstrap_icon(icons["x-lg"]))
        .replace("__ICON_CHEVRON_DOWN__", _inline_bootstrap_icon(icons["chevron-down"], "bi select-chevron"))
        .replace("__ICON_ARROW_CLOCKWISE__", _inline_bootstrap_icon(icons["arrow-clockwise"]))
        .replace("__ICON_ARROW_DOWN_UP__", _inline_bootstrap_icon(icons["arrow-down-up"]))
    )


def generate_dashboard(
    root: Path,
    output: Path,
    *,
    generated_at: datetime | None = None,
    source_commit: str | None = None,
    source_label: str | None = None,
) -> list[DashboardProject]:
    _, projects = discover_projects(root)
    html = render_dashboard(
        projects,
        generated_at=generated_at,
        source_commit=resolve_source_commit(root) if source_commit is None else source_commit,
        source_label=source_label,
    )
    _atomic_write_text(output, html)
    return projects


def dashboard_record_counts(records: Iterable[DashboardProject]) -> tuple[int, int]:
    """Return the canonical project and resource-pack counts for CLI reporting."""

    materialized = list(records)
    return (
        sum(record.record_kind == "project" for record in materialized),
        sum(record.record_kind == "resource_pack" for record in materialized),
    )


def _output_path(root: Path, requested: Path) -> Path:
    return requested if requested.is_absolute() else root / requested


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT, help="Workbench repository root")
    parser.add_argument("--output", type=Path, default=Path(DEFAULT_OUTPUT), help="generated HTML path")
    parser.add_argument("--open", action="store_true", help="open the regenerated dashboard in the default browser")
    parser.add_argument("--source-commit", help="exact checked-out source commit for generated dashboard provenance")
    parser.add_argument("--source-label", help="optional source label, such as main, displayed with --source-commit")
    subparsers = parser.add_subparsers(dest="command")
    server = subparsers.add_parser("server", help="set human-owned real-server implementation state")
    server.add_argument("project", help="project UUID, ID, name, alias, or legacy identity")
    server.add_argument("status", type=str.lower, choices=("current", "no", "yes"))
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    root = args.root.resolve()
    output = _output_path(root, args.output).resolve()
    try:
        if args.command == "server":
            statuses = load_repository_statuses(root)
            project_uuid, manifest = resolve_project(args.project, statuses)
            if _record_kind(statuses[project_uuid][0]) != "project":
                raise DashboardError(f"{manifest['identity']['name']} is a resource pack and has no server state")
            server_state = load_server_state(root, statuses)
            if args.status in {"current", "yes"}:
                current = canonical_release_identity(manifest["state"]["releases"]["current"])
                if current is None:
                    raise DashboardError(f"{manifest['identity']['name']} has no current release to record")
                server_state[project_uuid] = current
                description = "CURRENT"
            else:
                server_state.pop(project_uuid, None)
                description = "NOT DEPLOYED"
            projects = build_project_records(statuses, server_state)
            html = render_dashboard(
                projects,
                source_commit=resolve_source_commit(root) if args.source_commit is None else args.source_commit,
                source_label=args.source_label,
            )
            write_server_state(root, server_state)
            _atomic_write_text(output, html)
            print(f"Set {manifest['identity']['name']} to {description} and regenerated {output}")
        else:
            projects = generate_dashboard(
                root,
                output,
                source_commit=args.source_commit,
                source_label=args.source_label,
            )
            project_count, resource_pack_count = dashboard_record_counts(projects)
            print(f"Generated {output} with {project_count} projects and {resource_pack_count} resource packs")
        if args.open:
            opened = webbrowser.open(output.as_uri(), new=2)
            if not opened:
                print(f"Dashboard generated, but the default browser did not accept {output.as_uri()}", file=sys.stderr)
        return 0
    except (DashboardError, ValidationError, OSError) as exc:
        print(f"Dashboard error: {exc}", file=sys.stderr)
        return 1


HTML_TEMPLATE = r'''<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="color-scheme" content="dark">
  <meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:; connect-src 'none'; font-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'">
  <title>Mynx Dashboard</title>
  <link rel="icon" type="image/svg+xml" href="__DASHBOARD_FAVICON_DATA_URL__">
  <style>
    :root {
      color-scheme: dark;
      --bg: #06100c;
      --bg-deep: #030906;
      --panel: rgba(12, 28, 21, 0.92);
      --panel-raised: rgba(17, 38, 29, 0.94);
      --line: rgba(149, 194, 168, 0.16);
      --line-strong: rgba(149, 194, 168, 0.28);
      --text: #edf4ef;
      --muted: #97a9a0;
      --quiet: #71837a;
      --accent: #9bc5aa;
      --active: #e4b75c;
      --planned: #71a9dc;
      --accepted: #70bb89;
      --blocked: #db7474;
      --parked: #909a96;
      --current: #70bb89;
      --outdated: #e4b75c;
      --not-deployed: var(--parked);
      --controls-surface: #112a1e;
      --table-header-surface: #0c2118;
      --active-tab-surface: #153426;
      --radius: 13px;
      --shadow: 0 22px 70px rgba(0, 0, 0, 0.32);
    }

    * { box-sizing: border-box; }
    html { min-width: 320px; scrollbar-gutter: stable; }
    body {
      margin: 0;
      min-height: 100vh;
      background:
        radial-gradient(circle at 12% -10%, rgba(52, 106, 75, 0.19), transparent 34rem),
        linear-gradient(180deg, var(--bg) 0%, var(--bg-deep) 100%);
      color: var(--text);
      font: 16px/1.45 "Aptos", "Segoe UI", system-ui, sans-serif;
    }

    button, input, select { font: inherit; }
    button, select { cursor: pointer; }
    button:focus-visible, input:focus-visible, select:focus-visible {
      outline: 2px solid #b9ddc5;
      outline-offset: 2px;
    }

    .shell { width: min(1500px, calc(100% - 40px)); margin: 0 auto; padding: 28px 0 42px; }
    .masthead {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      gap: 28px;
      margin-bottom: 22px;
      padding: 0 2px;
    }
    .eyebrow {
      margin: 0 0 4px;
      color: var(--accent);
      font-size: 0.76rem;
      font-weight: 700;
      letter-spacing: 0.25em;
      text-transform: uppercase;
    }
    h1 {
      margin: 0;
      font: 600 clamp(2rem, 4vw, 3.05rem)/1.04 Georgia, "Times New Roman", serif;
      font-style: italic;
      letter-spacing: 0.01em;
      text-transform: lowercase;
    }
    .masthead-meta { display: grid; justify-items: end; gap: 5px; color: var(--muted); font-size: 0.82rem; }
    .phrase { color: #799185; font-family: Georgia, "Times New Roman", serif; font-style: italic; letter-spacing: 0.03em; }

    .workspace {
      overflow: hidden;
      border: 1px solid var(--line);
      border-radius: var(--radius);
      background: linear-gradient(180deg, rgba(15, 34, 26, 0.97), rgba(8, 21, 15, 0.98));
      box-shadow: var(--shadow);
    }
    .controls { padding: 17px 18px 15px; background: var(--controls-surface); }
    .control-row { display: flex; align-items: center; gap: 12px; }
    .control-row + .control-row { margin-top: 13px; }
    .search-wrap { position: relative; flex: 1 1 360px; min-width: 220px; }
    .search-icon {
      position: absolute;
      left: 14px;
      top: 50%;
      width: 16px;
      height: 16px;
      color: var(--muted);
      transform: translateY(-50%);
      pointer-events: none;
    }
    #search {
      width: 100%;
      height: 42px;
      padding: 0 42px 0 40px;
      border: 1px solid var(--line-strong);
      border-radius: 9px;
      background: rgba(3, 12, 8, 0.72);
      color: var(--text);
    }
    #search::placeholder { color: #75877e; }
    #clear-search {
      position: absolute;
      right: 6px;
      top: 50%;
      width: 30px;
      height: 30px;
      border: 0;
      border-radius: 7px;
      background: transparent;
      color: var(--muted);
      transform: translateY(-50%);
    }
    #clear-search:hover { background: rgba(255, 255, 255, 0.06); color: var(--text); }
    .select-wrap { display: flex; align-items: center; gap: 8px; color: var(--muted); font-size: 0.86rem; white-space: nowrap; }
    .select-control { position: relative; display: inline-flex; }
    .select-control select {
      height: 42px;
      padding: 0 40px 0 11px;
      border: 1px solid var(--line-strong);
      border-radius: 9px;
      background: #0a1a13;
      color: var(--text);
      appearance: none;
    }
    .select-chevron {
      position: absolute;
      top: 50%;
      right: 13px;
      width: 0.85rem;
      height: 0.85rem;
      color: var(--muted);
      pointer-events: none;
      transform: translateY(-50%);
    }
    .order-button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 7px;
      height: 42px;
      padding: 0 13px;
      border: 1px solid var(--line-strong);
      border-radius: 9px;
      background: rgba(7, 20, 14, 0.72);
      color: var(--muted);
      white-space: nowrap;
    }
    .order-button:not(:disabled):hover { color: var(--text); border-color: rgba(155, 197, 170, 0.48); }
    .order-button:disabled { cursor: default; opacity: 0.52; }

    .filter-row { align-items: center; overflow-x: auto; padding: 2px 1px 4px; scrollbar-width: thin; }
    .filter-label { flex: 0 0 auto; color: var(--quiet); font-size: 0.78rem; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; }
    .chips { display: flex; gap: 7px; min-width: max-content; }
    .chip {
      display: inline-flex;
      align-items: center;
      gap: 7px;
      min-height: 33px;
      padding: 0 11px;
      border: 1px solid var(--line);
      border-radius: 999px;
      background: rgba(6, 17, 12, 0.56);
      color: var(--muted);
      font-size: 0.83rem;
      font-weight: 650;
    }
    .chip-count { min-width: 1.55em; color: var(--quiet); font-variant-numeric: tabular-nums; text-align: right; }
    .chip:hover { border-color: var(--line-strong); color: var(--text); }
    .chip[aria-selected="true"] { color: var(--text); border-color: rgba(155, 197, 170, 0.45); background: rgba(71, 120, 88, 0.2); }
    .chip[data-lifecycle="ACTIVE"][aria-selected="true"] { border-color: color-mix(in srgb, var(--active) 55%, transparent); }
    .chip[data-lifecycle="PLANNED"][aria-selected="true"] { border-color: color-mix(in srgb, var(--planned) 55%, transparent); }
    .chip[data-lifecycle="ACCEPTED"][aria-selected="true"] { border-color: color-mix(in srgb, var(--accepted) 55%, transparent); }
    .chip[data-lifecycle="BLOCKED"][aria-selected="true"] { border-color: color-mix(in srgb, var(--blocked) 55%, transparent); }
    .chip[data-lifecycle="PARKED"][aria-selected="true"] { border-color: color-mix(in srgb, var(--parked) 55%, transparent); }

    .results-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      min-height: 38px;
      padding: 0 18px;
      border-bottom: 1px solid var(--line);
      background: var(--controls-surface);
      color: var(--muted);
      font-size: 0.8rem;
    }
    .record-tabs { display: flex; align-self: stretch; }
    .record-tab {
      display: inline-flex;
      align-items: center;
      min-height: 38px;
      padding: 0 12px;
      border: 1px solid transparent;
      border-radius: 8px;
      background: transparent;
      color: var(--quiet);
      font: inherit;
      font-variant-numeric: tabular-nums;
    }
    .record-tab:hover { color: var(--text); }
    .record-tab[aria-selected="true"] {
      position: relative;
      z-index: 1;
      border-color: var(--line);
      background: var(--active-tab-surface);
      color: var(--text);
    }
    .record-tab + .record-tab { margin-left: 3px; }
    #sort-summary { color: var(--quiet); }

    .table-scroll { overflow-x: auto; scrollbar-gutter: stable; }
    table { width: 100%; min-width: 790px; border-collapse: separate; border-spacing: 0; table-layout: fixed; }
    col.project { width: 42%; }
    col.lifecycle { width: 13%; }
    col.version { width: 10%; }
    col.jar { width: 16%; }
    col.server { width: 19%; }
    table[data-record-kind="resource_pack"] { min-width: 650px; }
    table[data-record-kind="resource_pack"] col.project { width: 48%; }
    table[data-record-kind="resource_pack"] col.lifecycle { width: 17%; }
    table[data-record-kind="resource_pack"] col.version { width: 13%; }
    table[data-record-kind="resource_pack"] col.jar { width: 22%; }
    thead th {
      position: sticky;
      top: 0;
      z-index: 3;
      height: 43px;
      padding: 0 14px;
      border-bottom: 1px solid var(--line-strong);
      background: var(--table-header-surface);
      color: #91a69b;
      font-size: 0.75rem;
      font-weight: 750;
      letter-spacing: 0.075em;
      text-align: left;
      text-transform: uppercase;
    }
    .sort-button {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      width: 100%;
      height: 100%;
      padding: 0;
      border: 0;
      background: transparent;
      color: inherit;
      font: inherit;
      letter-spacing: inherit;
      text-align: left;
      text-transform: inherit;
    }
    thead th:not(:first-child), tbody td:not(:first-child) { text-align: center; }
    thead th:not(:first-child) .sort-button { justify-content: center; text-align: center; }
    .bi { display: inline-block; width: 1em; height: 1em; fill: currentColor; flex: 0 0 auto; }
    .sort-indicator { display: inline-flex; align-items: center; justify-content: center; width: 1em; height: 1em; color: #668075; font-size: 0.85rem; }
    th[aria-sort="ascending"] .sort-indicator,
    th[aria-sort="descending"] .sort-indicator { color: var(--accent); }

    tbody td {
      height: 49px;
      padding: 9px 14px;
      border-bottom: 1px solid rgba(149, 194, 168, 0.09);
      color: #dfe8e2;
      font-size: 0.89rem;
      vertical-align: middle;
    }
    tbody tr.project-row:hover td { background: rgba(123, 171, 140, 0.055); }
    .project-name { overflow: hidden; padding-left: 10px; color: var(--text); font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
    .project-toggle { display: inline-flex; align-items: center; max-width: 100%; padding: 0; border: 0; background: transparent; color: inherit; font: inherit; font-weight: inherit; text-align: left; }
    .project-toggle:hover { color: var(--accent); }
    .project-chevron { width: 0.76rem; height: 0.76rem; margin-right: 8px; color: var(--muted); transition: transform 120ms ease; }
    .project-toggle[aria-expanded="true"] .project-chevron { transform: rotate(90deg); }
    .release-detail td { height: auto; padding: 10px 50px; border-bottom: 1px solid rgba(149, 194, 168, 0.09); color: #b5c4bb; font-size: 0.69em; line-height: 1.4; }
    .release-detail .release-summary { display: block; max-width: 74rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .release-detail .release-summary-version { color: #d3ded7; font-weight: 650; }
    .version-value { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .date-value { color: #c3d0c8; font-variant-numeric: tabular-nums; white-space: nowrap; }
    .date-value .date-part::after { content: " "; }
    .muted { color: #687a71; }

    .group-row th {
      padding: 0;
      border-bottom: 1px solid rgba(149, 194, 168, 0.1);
      background: rgba(13, 30, 22, 0.98);
      text-align: left;
    }
    .group-button {
      display: flex;
      align-items: center;
      width: 100%;
      min-height: 39px;
      padding: 0 14px 0 12px;
      border: 0;
      border-left: 3px solid var(--group-color);
      background: transparent;
      color: var(--group-color);
      font-size: 0.78rem;
      font-weight: 800;
      letter-spacing: 0.105em;
      text-align: left;
    }
    .group-button:hover { background: rgba(255, 255, 255, 0.025); }
    .group-chevron { width: 10px; margin-right: 10px; color: var(--muted); font-size: 0.9rem; transform: rotate(0deg); }
    .group-button[aria-expanded="false"] .group-chevron { transform: rotate(-90deg); }
    .group-count { margin-left: 9px; color: var(--muted); font-weight: 600; letter-spacing: 0; text-transform: none; }
    .life-ACTIVE { --group-color: var(--active); }
    .life-PLANNED { --group-color: var(--planned); }
    .life-ACCEPTED { --group-color: var(--accepted); }
    .life-BLOCKED { --group-color: var(--blocked); }
    .life-PARKED { --group-color: var(--parked); }

    .pill {
      display: inline-flex;
      align-items: center;
      min-height: 25px;
      padding: 6px 12px;
      border: 1px solid color-mix(in srgb, var(--pill-color) 42%, transparent);
      border-radius: 999px;
      background: color-mix(in srgb, var(--pill-color) 10%, transparent);
      color: var(--pill-color);
      font-size: 0.65rem;
      font-weight: 600;
      letter-spacing: 0.045em;
      line-height: 1;
      white-space: nowrap;
      text-transform: uppercase;
    }
    .status-pill::before {
      content: "";
      width: 6px;
      height: 6px;
      margin-right: 6px;
      border-radius: 50%;
      background: currentColor;
    }
    .server-CURRENT { --pill-color: var(--current); }
    .server-OUTDATED { --pill-color: var(--outdated); }
    .server-NOT_DEPLOYED { --pill-color: var(--not-deployed); }

    .empty {
      display: grid;
      place-items: center;
      min-height: 250px;
      padding: 44px 20px;
      text-align: center;
    }
    .empty-mark { margin-bottom: 10px; color: #5f786b; font: 2.1rem Georgia, "Times New Roman", serif; }
    .empty h2 { margin: 0; font: 600 1.4rem Georgia, "Times New Roman", serif; }
    .empty p { max-width: 34rem; margin: 7px 0 17px; color: var(--muted); font-size: 0.9rem; }
    .empty button { padding: 8px 12px; border: 1px solid var(--line-strong); border-radius: 8px; background: #112a1e; color: var(--text); }
    [hidden] { display: none !important; }
    .sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
    @media (max-width: 760px) {
      .shell { width: min(100% - 20px, 1500px); padding-top: 18px; }
      .masthead { align-items: flex-start; margin-bottom: 16px; }
      .masthead-meta { justify-items: start; }
      .phrase { display: none; }
      .control-row { flex-wrap: wrap; }
      .search-wrap { flex-basis: 100%; }
      .select-wrap { flex: 1 1 auto; }
      select { width: 100%; }
      .order-button { flex: 0 0 auto; }
      .filter-row { flex-wrap: nowrap; }
    }
    @media (max-width: 520px) {
      .masthead { display: block; }
      .masthead-meta { margin-top: 10px; }
      .controls { padding: 13px 12px; }
      .results-bar { padding: 0 12px; }
      /* Keep the final two columns distinct while retaining the compact mobile table. */
      col.project { width: 39%; }
      col.lifecycle { width: 13%; }
      col.version { width: 10%; }
      col.jar { width: 18%; }
      col.server { width: 20%; }
      table[data-record-kind="resource_pack"] col.project { width: 43%; }
      table[data-record-kind="resource_pack"] col.lifecycle { width: 17%; }
      table[data-record-kind="resource_pack"] col.version { width: 14%; }
      table[data-record-kind="resource_pack"] col.jar { width: 26%; }
      thead th { padding-inline: 8px; }
      tbody td { padding-inline: 10px; }
      thead th:not(:first-child) .sort-button { gap: 3px; }
      .sort-indicator { font-size: 0.75rem; }
      .date-value time { display: inline-grid; justify-items: center; line-height: 1.2; }
      .date-value .date-part,
      .date-value .time-part { display: block; white-space: nowrap; }
      .date-value .date-part::after { content: none; }
    }
    @media (prefers-reduced-motion: no-preference) {
      button, input, select, tbody td { transition: border-color 120ms ease, background-color 120ms ease, color 120ms ease; }
      .group-chevron { transition: transform 120ms ease; }
    }
  </style>
</head>
<body>
  <main class="shell">
    <header class="masthead">
      <div>
        <p class="eyebrow">Project Tracker</p>
        <h1>mynx dashboard</h1>
      </div>
      <div class="masthead-meta">
        <span><span id="source-provenance" hidden></span><span id="generated-prefix">Last generated </span><time id="generated-at"></time></span>
        <span class="phrase">small changes · a quieter world</span>
      </div>
    </header>

    <section class="workspace" aria-label="Mynx dashboard records">
      <div class="controls">
        <div class="control-row">
          <div class="search-wrap">
            <label class="sr-only" for="search">Search projects</label>
            __ICON_SEARCH__
            <input id="search" type="search" autocomplete="off" placeholder="Search projects" spellcheck="false">
            <button id="clear-search" type="button" aria-label="Clear project search" title="Clear project search" hidden>__ICON_X_LG__</button>
          </div>
          <label id="server-filter-wrap" class="select-wrap" for="server-filter">
            <span>On server</span>
            <span class="select-control">
              <select id="server-filter">
                <option value="ALL">All</option>
                <option value="CURRENT">Current</option>
                <option value="OUTDATED">Outdated</option>
                <option value="NOT_DEPLOYED">Not deployed</option>
              </select>
              __ICON_CHEVRON_DOWN__
            </span>
          </label>
          <button id="reset-order" class="order-button" type="button" title="Restore Workbench order" disabled>__ICON_ARROW_CLOCKWISE__ Workbench order</button>
        </div>
        <div class="control-row filter-row">
          <span class="filter-label">Lifecycle</span>
          <div id="lifecycle-filters" class="chips" role="tablist" aria-label="Filter by lifecycle">
            <button class="chip" type="button" role="tab" data-lifecycle="ALL" aria-selected="true">All <span class="chip-count">0</span></button>
            <button class="chip" type="button" role="tab" data-lifecycle="ACTIVE" aria-selected="false">Active <span class="chip-count">0</span></button>
            <button class="chip" type="button" role="tab" data-lifecycle="PLANNED" aria-selected="false">Planned <span class="chip-count">0</span></button>
            <button class="chip" type="button" role="tab" data-lifecycle="ACCEPTED" aria-selected="false">Accepted <span class="chip-count">0</span></button>
            <button class="chip" type="button" role="tab" data-lifecycle="BLOCKED" aria-selected="false">Blocked <span class="chip-count">0</span></button>
            <button class="chip" type="button" role="tab" data-lifecycle="PARKED" aria-selected="false">Parked <span class="chip-count">0</span></button>
          </div>
        </div>
      </div>

      <div class="results-bar">
        <div class="record-tabs" role="tablist" aria-label="Dashboard record type">
          <button id="projects-tab" class="record-tab" type="button" role="tab" data-record-kind="project" aria-selected="true" aria-controls="table-region">0 Projects</button>
          <button id="resource-packs-tab" class="record-tab" type="button" role="tab" data-record-kind="resource_pack" aria-selected="false" aria-controls="table-region">0 Resource Packs</button>
        </div>
        <span id="sort-summary">Default Workbench order</span>
      </div>

      <div id="table-region" class="table-scroll">
        <table data-record-kind="project">
          <caption class="sr-only">Canonical Mynx dashboard project status</caption>
          <colgroup>
            <col class="project"><col class="lifecycle"><col class="version"><col class="jar"><col class="server">
          </colgroup>
          <thead>
            <tr>
              <th scope="col" data-sort-header="name"><button class="sort-button" type="button" data-sort="name">Project <span class="sort-indicator" aria-hidden="true">__ICON_ARROW_DOWN_UP__</span></button></th>
              <th scope="col" data-sort-header="lifecycle"><button class="sort-button" type="button" data-sort="lifecycle">Lifecycle <span class="sort-indicator" aria-hidden="true">__ICON_ARROW_DOWN_UP__</span></button></th>
              <th scope="col" data-sort-header="version"><button class="sort-button" type="button" data-sort="version">Version <span class="sort-indicator" aria-hidden="true">__ICON_ARROW_DOWN_UP__</span></button></th>
              <th scope="col" data-sort-header="jar"><button class="sort-button" type="button" data-sort="jar">Last JAR Edit <span class="sort-indicator" aria-hidden="true">__ICON_ARROW_DOWN_UP__</span></button></th>
              <th scope="col" data-sort-header="server"><button class="sort-button" type="button" data-sort="server">On Server <span class="sort-indicator" aria-hidden="true">__ICON_ARROW_DOWN_UP__</span></button></th>
            </tr>
          </thead>
          <tbody id="project-rows"></tbody>
        </table>
      </div>

      <div id="empty-state" class="empty" hidden>
        <div>
          <div class="empty-mark" aria-hidden="true">—</div>
          <h2>No matching projects</h2>
          <p>Try a different name, lifecycle, or server status.</p>
          <button id="clear-filters" type="button">Clear filters</button>
        </div>
      </div>
      <p id="live-region" class="sr-only" aria-live="polite"></p>
    </section>
  </main>

  <script type="application/json" id="dashboard-data">__DASHBOARD_DATA__</script>
  <script type="application/json" id="bootstrap-icon-data">__BOOTSTRAP_ICON_DATA__</script>
  <script>
    (() => {
      "use strict";
      const data = JSON.parse(document.getElementById("dashboard-data").textContent);
      const recordsByKind = {
        project: data.projects.map((project, defaultIndex) => ({ ...project, defaultIndex })),
        resource_pack: data.resourcePacks.map((project, defaultIndex) => ({ ...project, defaultIndex })),
      };
      const lifecycleOrder = ["ACTIVE", "PLANNED", "ACCEPTED", "BLOCKED", "PARKED"];
      const lifecycleLabels = { ACTIVE: "Active", PLANNED: "Planned", ACCEPTED: "Accepted", BLOCKED: "Blocked", PARKED: "Parked" };
      const serverRank = { CURRENT: 0, OUTDATED: 1, NOT_DEPLOYED: 2 };
      const views = {
        project: { noun: "project", plural: "projects", title: "Project", timestamp: "Last JAR Edit", columns: ["name", "lifecycle", "version", "jar", "server"] },
        resource_pack: { noun: "resource pack", plural: "resource packs", title: "Resource Pack", timestamp: "Last Edit", columns: ["name", "lifecycle", "version", "jar"] },
      };
      const bootstrapIcons = JSON.parse(document.getElementById("bootstrap-icon-data").textContent);
      const SVG_NS = "http://www.w3.org/2000/svg";
      const freshViewState = () => ({ lifecycle: "ALL", search: "", sortKey: "default", sortDirection: "asc", collapsed: new Set(), expanded: null });
      const state = { kind: "project", server: "ALL", view: { project: freshViewState(), resource_pack: freshViewState() } };

      const rowsElement = document.getElementById("project-rows");
      const tableRegion = document.getElementById("table-region");
      const table = tableRegion.querySelector("table");
      const columnGroup = table.querySelector("colgroup");
      const tableHead = table.querySelector("thead tr");
      const caption = table.querySelector("caption");
      const emptyState = document.getElementById("empty-state");
      const sortSummary = document.getElementById("sort-summary");
      const search = document.getElementById("search");
      const searchLabel = document.querySelector('label[for="search"]');
      const clearSearch = document.getElementById("clear-search");
      const serverFilter = document.getElementById("server-filter");
      const serverFilterWrap = document.getElementById("server-filter-wrap");
      const resetOrder = document.getElementById("reset-order");
      const lifecycleFilters = document.getElementById("lifecycle-filters");
      const liveRegion = document.getElementById("live-region");
      const recordTabs = document.querySelectorAll(".record-tab[data-record-kind]");

      function currentView() { return views[state.kind]; }
      function currentState() { return state.view[state.kind]; }
      function currentRecords() { return recordsByKind[state.kind]; }

      function formatLocalDate(milliseconds) {
        const { datePart, timePart } = formatLocalDateParts(milliseconds);
        return `${datePart} ${timePart}`;
      }

      function formatLocalDateParts(milliseconds) {
        const date = new Date(milliseconds);
        const datePart = date.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
        const timePart = date.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
        return { datePart, timePart };
      }

      const generatedDate = new Date(data.generatedAt);
      const generatedElement = document.getElementById("generated-at");
      const sourceProvenance = document.getElementById("source-provenance");
      const generatedPrefix = document.getElementById("generated-prefix");
      generatedElement.dateTime = data.generatedAt;
      generatedElement.title = data.generatedAt;
      generatedElement.textContent = formatLocalDate(generatedDate.getTime());
      if (data.source) {
        sourceProvenance.hidden = false;
        sourceProvenance.title = data.source.commit;
        sourceProvenance.textContent = `${data.source.label || "source"} · ${data.source.commit.slice(0, 7)} · `;
        generatedPrefix.textContent = "generated ";
      }

      function element(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined) node.textContent = text;
        return node;
      }

      function icon(name) {
        const svg = document.createElementNS(SVG_NS, "svg");
        svg.setAttribute("class", "bi");
        svg.setAttribute("aria-hidden", "true");
        const definition = bootstrapIcons[name];
        svg.setAttribute("viewBox", definition.viewBox);
        svg.innerHTML = definition.body;
        return svg;
      }

      function baseFilteredRecords() {
        const viewState = currentState();
        const needle = viewState.search.toLocaleLowerCase();
        return currentRecords().filter(project => {
          const matchesName = !needle || project.name.toLocaleLowerCase().includes(needle);
          const matchesServer = state.kind !== "project" || state.server === "ALL" || project.server === state.server;
          return matchesName && matchesServer;
        });
      }

      function compareNullable(left, right, direction, compare) {
        if (left === null && right === null) return 0;
        if (left === null) return 1;
        if (right === null) return -1;
        return direction * compare(left, right);
      }

      function projectComparator(left, right) {
        const viewState = currentState();
        if (viewState.sortKey === "default" || viewState.sortKey === "lifecycle") {
          return left.defaultIndex - right.defaultIndex;
        }
        const direction = viewState.sortDirection === "asc" ? 1 : -1;
        let result = 0;
        if (viewState.sortKey === "name") {
          result = direction * left.name.localeCompare(right.name, undefined, { sensitivity: "base", numeric: true });
        } else if (viewState.sortKey === "version") {
          result = compareNullable(left.version, right.version, direction, (a, b) => {
            if (left.versionCanary !== null && right.versionCanary !== null) return left.versionCanary - right.versionCanary;
            return a.localeCompare(b, undefined, { sensitivity: "base", numeric: true });
          });
        } else if (viewState.sortKey === "jar") {
          result = compareNullable(left.lastEditMtimeMs, right.lastEditMtimeMs, direction, (a, b) => a - b);
        } else if (viewState.sortKey === "server") {
          result = direction * (serverRank[left.server] - serverRank[right.server]);
        }
        return result || left.defaultIndex - right.defaultIndex;
      }

      function orderedLifecycles() {
        const viewState = currentState();
        if (viewState.sortKey === "lifecycle" && viewState.sortDirection === "desc") return [...lifecycleOrder].reverse();
        return lifecycleOrder;
      }

      function lifecyclePill(lifecycle) {
        const pill = element("span", `pill life-${lifecycle}`, lifecycleLabels[lifecycle].toLocaleLowerCase());
        pill.style.setProperty("--pill-color", `var(--${lifecycle.toLocaleLowerCase()})`);
        return pill;
      }

      function serverPill(project) {
        const pill = element("span", `pill status-pill server-${project.server}`, project.serverDisplay);
        if (project.deployedVersion) pill.title = project.deployedVersion;
        return pill;
      }

      function projectRow(project, lifecycle) {
        const viewState = currentState();
        const row = element("tr", "project-row");
        row.dataset.lifecycle = lifecycle;
        if (viewState.collapsed.has(lifecycle)) row.hidden = true;

        const nameCell = element("td", "project-name");
        const toggle = element("button", "project-toggle");
        toggle.type = "button";
        toggle.dataset.expand = project.uuid;
        toggle.setAttribute("aria-expanded", String(viewState.expanded === project.uuid));
        toggle.setAttribute("aria-controls", `release-detail-${project.uuid}`);
        toggle.title = project.name;
        toggle.appendChild(icon("chevron-right"));
        toggle.lastChild.classList.add("project-chevron");
        toggle.appendChild(document.createTextNode(project.name));
        nameCell.appendChild(toggle);
        row.appendChild(nameCell);

        const lifecycleCell = element("td", "lifecycle-value");
        lifecycleCell.appendChild(lifecyclePill(project.lifecycle));
        row.appendChild(lifecycleCell);

        const versionCell = element("td", project.version === null ? "version-value muted" : "version-value", project.versionDisplay ?? "—");
        if (project.version) versionCell.title = project.version;
        row.appendChild(versionCell);

        const jarCell = element("td", "date-value");
        if (project.lastEditMtimeMs === null) {
          const unavailable = element("span", "muted", "—");
          unavailable.title = project.lastEditNote;
          jarCell.appendChild(unavailable);
        } else {
          const formatted = formatLocalDateParts(project.lastEditMtimeMs);
          const time = element("time");
          time.dateTime = project.lastEditMtimeIso;
          time.title = `${project.lastEditMtimeIso} · ${project.lastEditNote}`;
          time.setAttribute("aria-label", `${formatted.datePart} ${formatted.timePart}`);
          const datePart = element("span", "date-part", formatted.datePart);
          const timePart = element("span", "time-part", formatted.timePart);
          datePart.setAttribute("aria-hidden", "true");
          timePart.setAttribute("aria-hidden", "true");
          time.append(datePart, timePart);
          jarCell.appendChild(time);
        }
        row.appendChild(jarCell);

        if (state.kind === "project") {
          const serverCell = element("td", "server-value");
          serverCell.appendChild(serverPill(project));
          row.appendChild(serverCell);
        }
        return row;
      }

      function releaseDetailRow(project, lifecycle) {
        const viewState = currentState();
        const row = element("tr", "release-detail");
        row.id = `release-detail-${project.uuid}`;
        row.dataset.lifecycle = lifecycle;
        if (viewState.collapsed.has(lifecycle)) row.hidden = true;
        const cell = document.createElement("td");
        cell.colSpan = currentView().columns.length;
        const line = element("span", "release-summary");
        const version = project.versionDisplay || project.version || "Current release";
        line.appendChild(element("span", "release-summary-version", `${version} — `));
        line.appendChild(document.createTextNode(project.summary || "Current release details are not recorded yet."));
        cell.appendChild(line);
        row.appendChild(cell);
        return row;
      }

      function groupRow(lifecycle, count) {
        const viewState = currentState();
        const row = element("tr", `group-row life-${lifecycle}`);
        const heading = document.createElement("th");
        heading.colSpan = currentView().columns.length;
        heading.scope = "rowgroup";
        const button = element("button", "group-button");
        button.type = "button";
        button.dataset.collapse = lifecycle;
        button.setAttribute("aria-expanded", String(!viewState.collapsed.has(lifecycle)));
        const chevron = icon("chevron-down");
        chevron.classList.add("group-chevron");
        button.appendChild(chevron);
        button.appendChild(document.createTextNode(lifecycle));
        button.appendChild(element("span", "group-count", `${count} ${count === 1 ? currentView().noun : currentView().plural}`));
        heading.appendChild(button);
        row.appendChild(heading);
        return row;
      }

      function updateCounts(baseRecords) {
        const viewState = currentState();
        const counts = Object.fromEntries(lifecycleOrder.map(lifecycle => [lifecycle, 0]));
        for (const project of baseRecords) counts[project.lifecycle] += 1;
        for (const chip of lifecycleFilters.querySelectorAll(".chip")) {
          const lifecycle = chip.dataset.lifecycle;
          chip.setAttribute("aria-selected", String(lifecycle === viewState.lifecycle));
          chip.querySelector(".chip-count").textContent = lifecycle === "ALL" ? baseRecords.length : counts[lifecycle];
        }
      }

      function configureTable() {
        const view = currentView();
        const labels = { name: view.title, lifecycle: "Lifecycle", version: "Version", jar: view.timestamp, server: "On Server" };
        table.dataset.recordKind = state.kind;
        caption.textContent = `Canonical Mynx dashboard ${view.noun} status`;
        columnGroup.replaceChildren(...view.columns.map(column => element("col", column === "name" ? "project" : column)));
        tableHead.replaceChildren(...view.columns.map(column => {
          const heading = document.createElement("th");
          heading.scope = "col";
          heading.dataset.sortHeader = column;
          const button = element("button", "sort-button");
          button.type = "button";
          button.dataset.sort = column;
          button.append(document.createTextNode(`${labels[column]} `));
          button.appendChild(element("span", "sort-indicator"));
          heading.appendChild(button);
          return heading;
        }));
      }

      function updateViewControls() {
        const view = currentView();
        const viewState = currentState();
        for (const tab of recordTabs) {
          const kind = tab.dataset.recordKind;
          const tabView = views[kind];
          const count = recordsByKind[kind].length;
          tab.setAttribute("aria-selected", String(kind === state.kind));
          tab.textContent = `${count} ${count === 1 ? tabView.noun[0].toUpperCase() + tabView.noun.slice(1) : tabView.plural.replace(/\b\w/g, character => character.toUpperCase())}`;
        }
        search.placeholder = `Search ${view.plural}`;
        searchLabel.textContent = `Search ${view.plural}`;
        clearSearch.setAttribute("aria-label", `Clear ${view.noun} search`);
        clearSearch.title = `Clear ${view.noun} search`;
        search.value = viewState.search;
        serverFilterWrap.hidden = state.kind !== "project";
        emptyState.querySelector("h2").textContent = `No matching ${view.plural}`;
        emptyState.querySelector("p").textContent = state.kind === "project"
          ? "Try a different name, lifecycle, or server status."
          : "Try a different name or lifecycle.";
      }

      function updateSortState() {
        const viewState = currentState();
        const labels = { name: currentView().title, lifecycle: "Lifecycle", version: "Version", jar: currentView().timestamp, server: "On Server" };
        for (const heading of document.querySelectorAll("[data-sort-header]")) {
          const key = heading.dataset.sortHeader;
          const indicator = heading.querySelector(".sort-indicator");
          if (key === viewState.sortKey) {
            heading.setAttribute("aria-sort", viewState.sortDirection === "asc" ? "ascending" : "descending");
            indicator.replaceChildren(icon(viewState.sortDirection === "asc" ? "sort-up" : "sort-down"));
          } else {
            heading.removeAttribute("aria-sort");
            indicator.replaceChildren(icon("arrow-down-up"));
          }
        }
        resetOrder.disabled = viewState.sortKey === "default";
        sortSummary.textContent = viewState.sortKey === "default"
          ? "Default Workbench order"
          : `${labels[viewState.sortKey]} · ${viewState.sortDirection === "asc" ? "ascending" : "descending"}`;
      }

      function render(focusLifecycle) {
        const viewState = currentState();
        const view = currentView();
        const baseRecords = baseFilteredRecords();
        updateCounts(baseRecords);
        const visible = baseRecords.filter(project => viewState.lifecycle === "ALL" || project.lifecycle === viewState.lifecycle);
        rowsElement.replaceChildren();

        for (const lifecycle of orderedLifecycles()) {
          const group = visible.filter(project => project.lifecycle === lifecycle).sort(projectComparator);
          if (!group.length) continue;
          rowsElement.appendChild(groupRow(lifecycle, group.length));
          for (const project of group) {
            rowsElement.appendChild(projectRow(project, lifecycle));
            if (viewState.expanded === project.uuid) rowsElement.appendChild(releaseDetailRow(project, lifecycle));
          }
        }

        const isEmpty = visible.length === 0;
        tableRegion.hidden = isEmpty;
        emptyState.hidden = !isEmpty;
        clearSearch.hidden = !viewState.search;
        updateSortState();
        liveRegion.textContent = isEmpty ? `No matching ${view.plural}` : `Showing ${visible.length} ${visible.length === 1 ? view.noun : view.plural}`;

        if (focusLifecycle) {
          const button = rowsElement.querySelector(`[data-collapse="${focusLifecycle}"]`);
          if (button) button.focus();
        }
      }

      search.addEventListener("input", () => {
        currentState().search = search.value.trim();
        render();
      });
      clearSearch.addEventListener("click", () => {
        search.value = "";
        currentState().search = "";
        render();
        search.focus();
      });
      serverFilter.addEventListener("change", () => {
        state.server = serverFilter.value;
        render();
      });
      lifecycleFilters.addEventListener("click", event => {
        const chip = event.target.closest("[data-lifecycle]");
        if (!chip) return;
        currentState().lifecycle = chip.dataset.lifecycle;
        render();
      });
      tableHead.addEventListener("click", event => {
        const button = event.target.closest("[data-sort]");
        if (!button) return;
        const key = button.dataset.sort;
        const viewState = currentState();
        if (viewState.sortKey === key) {
          viewState.sortDirection = viewState.sortDirection === "asc" ? "desc" : "asc";
        } else {
          viewState.sortKey = key;
          viewState.sortDirection = key === "jar" ? "desc" : "asc";
        }
        render();
      });
      resetOrder.addEventListener("click", () => {
        currentState().sortKey = "default";
        currentState().sortDirection = "asc";
        render();
      });
      rowsElement.addEventListener("click", event => {
        const button = event.target.closest("[data-collapse]");
        if (button) {
          const viewState = currentState();
          const lifecycle = button.dataset.collapse;
          if (viewState.collapsed.has(lifecycle)) viewState.collapsed.delete(lifecycle);
          else {
            viewState.collapsed.add(lifecycle);
            if (viewState.expanded && currentRecords().find(project => project.uuid === viewState.expanded)?.lifecycle === lifecycle) viewState.expanded = null;
          }
          render(lifecycle);
          return;
        }
        const projectButton = event.target.closest("[data-expand]");
        if (!projectButton) return;
        const uuid = projectButton.dataset.expand;
        currentState().expanded = currentState().expanded === uuid ? null : uuid;
        render();
      });
      document.getElementById("clear-filters").addEventListener("click", () => {
        currentState().lifecycle = "ALL";
        if (state.kind === "project") {
          state.server = "ALL";
          serverFilter.value = "ALL";
        }
        currentState().search = "";
        search.value = "";
        render();
        search.focus();
      });

      recordTabs.forEach(tab => tab.addEventListener("click", () => {
        state.kind = tab.dataset.recordKind;
        configureTable();
        updateViewControls();
        render();
      }));

      configureTable();
      updateViewControls();
      render();
    })();
  </script>
</body>
</html>
'''


if __name__ == "__main__":
    raise SystemExit(main())
