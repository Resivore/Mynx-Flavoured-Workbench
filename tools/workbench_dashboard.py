#!/usr/bin/env python3
"""Generate the local, self-contained Mynx Workbench project dashboard."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import stat
import sys
import unicodedata
import webbrowser
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable, Mapping
from uuid import UUID

try:
    from .workbench import ValidationError, load_json, load_repository_statuses
except ImportError:  # Direct execution from tools/.
    from workbench import ValidationError, load_json, load_repository_statuses  # type: ignore


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = "WORKBENCH_DASHBOARD.html"
SERVER_STATE_FILENAME = "WORKBENCH_SERVER_STATE.json"
BOOTSTRAP_SPRITE = ROOT / "third_party" / "bootstrap-icons" / "bootstrap-icons.svg"
BOOTSTRAP_PREFIX = "bi-"
DASHBOARD_ICON_NAMES = frozenset(
    {
        "search",
        "x-lg",
        "arrow-down-up",
        "sort-up",
        "sort-down",
        "chevron-down",
        "arrow-clockwise",
    }
)
SERVER_STATES = {"CURRENT", "OUTDATED", "NOT_DEPLOYED"}
LIFECYCLE_ORDER = ("ACTIVE", "PLANNED", "ACCEPTED", "TESTING", "BLOCKED", "PARKED")
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
    current_canary: int | None = None


def _canary_number_in_text(value: str | None) -> int | None:
    """Find the public Canary ordinal in one release-identity text field.

    A leading/public ``C11`` is deliberately considered before any verbose
    Canary spelling.  This keeps provenance such as ``Private Canary 10`` or
    an embedded artifact version from replacing the human-facing C11.
    """

    if not value:
        return None
    public_portion = value.split("(", 1)[0]
    public = re.search(r"(?i)(?<![a-z0-9])c[._ -]?(\d+)(?!\d)", public_portion)
    if public:
        return int(public.group(1))
    canary = re.search(r"(?i)(?<![a-z0-9])canary[._ -]?(\d+)(?!\d)", value)
    if canary:
        return int(canary.group(1))
    return None


def resolve_canary_number(
    version: str | None,
    embedded_version: str | None = None,
    artifact_filename: str | None = None,
) -> int | None:
    """Resolve the display-only Canary ordinal from current release identity.

    The canonical human-facing version always has priority.  Embedded version
    and filename are deterministic fallbacks for releases whose canonical
    spelling does not carry the ordinal itself.
    """

    for value in (version, embedded_version, artifact_filename):
        number = _canary_number_in_text(value)
        if number is not None:
            return number
    return None


def display_canary_version(version: str | None) -> str | None:
    """Return a compact Canary label when the version carries an ordinal."""

    number = resolve_canary_number(version)
    return version if number is None else f"C{number}"


def canary_number(version: str | None) -> int | None:
    return resolve_canary_number(version)


def canary_number_for_release(current_release: Mapping[str, Any] | None) -> int | None:
    """Resolve a current release's visible Canary without altering its identity."""

    if current_release is None:
        return None
    artifact = current_release.get("artifact")
    filename = artifact.get("filename") if isinstance(artifact, Mapping) else None
    return resolve_canary_number(current_release.get("version"), current_release.get("embedded_version"), filename)


def server_pill_label(server_status: str, deployed_release: Mapping[str, Any] | None) -> str:
    """Return the intentionally compact, release-aware server-state label."""

    if server_status not in SERVER_STATES:
        raise DashboardError(f"unsupported dashboard server status {server_status!r}")
    if server_status != "OUTDATED":
        return server_status.replace("_", " ")
    deployed_version = None if deployed_release is None else deployed_release.get("version")
    deployed_canary = canary_number(deployed_version)
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


def _jar_details(
    project_directory: Path,
    current_release: Mapping[str, Any] | None,
) -> tuple[str | None, int | None, str | None, str]:
    if current_release is None:
        return None, None, None, "No current release."

    version = current_release["version"]
    artifact = current_release["artifact"]
    if artifact is None:
        return version, None, None, "The current release has no JAR artifact."
    filename = artifact["filename"]
    if not filename.casefold().endswith(".jar"):
        return version, None, None, "The current artifact is not a JAR."

    artifact_path = _exact_artifact_path(project_directory, filename)
    if artifact_path is None:
        return version, None, None, "The exact current JAR is not retained locally."
    fingerprint = _sha256_and_mtime(artifact_path)
    if fingerprint is None:
        return version, None, None, "The exact current JAR is unavailable or changed while being read."
    actual_hash, mtime_ns = fingerprint
    if actual_hash != artifact["sha256"]:
        return version, None, None, "The retained JAR does not match the canonical SHA-256."

    timestamp = datetime.fromtimestamp(mtime_ns / 1_000_000_000, tz=timezone.utc)
    timestamp_iso = timestamp.isoformat(timespec="microseconds").replace("+00:00", "Z")
    return version, mtime_ns, timestamp_iso, f"Verified current JAR: {filename}"


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
        identity = manifest["identity"]
        lifecycle = manifest["definition"]["lifecycle"]
        if lifecycle not in LIFECYCLE_PRIORITY:
            raise DashboardError(f"{manifest_path}: unsupported dashboard lifecycle {lifecycle!r}")
        current = manifest["state"]["releases"]["current"]
        version, mtime_ns, mtime_iso, jar_note = _jar_details(manifest_path.parent, current)
        current_identity = canonical_release_identity(current)
        deployed_release = server_state.get(project_uuid)
        if deployed_release is not None:
            deployed_release = _release_identity(deployed_release, f"server state for {project_uuid}")
        if lifecycle == "ACCEPTED" and current_identity is not None:
            server_status = "CURRENT"
        elif deployed_release is None:
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
                current_canary=canary_number_for_release(current),
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
    deployed_version = None if project.deployed_release is None else project.deployed_release["version"]
    current_canary = project.current_canary
    deployed_canary = canary_number(deployed_version)
    return {
        "uuid": project.uuid,
        "projectId": project.project_id,
        "name": project.name,
        "lifecycle": project.lifecycle,
        "version": project.current_version,
        "versionDisplay": (
            f"C{current_canary}"
            if current_canary is not None
            else project.current_version
        ),
        "versionCanary": current_canary,
        "jarMtimeMs": None if project.jar_mtime_ns is None else project.jar_mtime_ns // 1_000_000,
        "jarMtimeIso": project.jar_mtime_iso,
        "jarNote": project.jar_note,
        "server": project.server_status,
        "serverDisplay": server_pill_label(project.server_status, project.deployed_release),
        "deployedVersion": deployed_version,
        "deployedVersionDisplay": display_canary_version(deployed_version),
        "deployedCanary": deployed_canary,
    }


def _namespace_bootstrap_sprite(sprite: str) -> str:
    """Prefix vendored sprite IDs so SVG fragments cannot target page DOM IDs."""

    defined_ids = set(re.findall(r'\bid="([^"]+)"', sprite))
    symbol_ids = set(re.findall(r'<symbol\b[^>]*\bid="([^"]+)"', sprite))
    missing = sorted(DASHBOARD_ICON_NAMES - symbol_ids)
    if missing:
        raise DashboardError(
            "Bootstrap Icons sprite is missing dashboard icon symbols: " + ", ".join(missing)
        )

    def prefixed(match: re.Match[str]) -> str:
        return f'{match.group(1)}{BOOTSTRAP_PREFIX}{match.group(2)}{match.group(3)}'

    # Keep the upstream asset immutable.  Both defined IDs and internal
    # fragment references are rewritten in the generated local document.
    namespaced = re.sub(r'(\bid=")([^"]+)(")', prefixed, sprite)
    namespaced = re.sub(
        r'((?:xlink:)?href\s*=\s*["\'])#([^"\']+)(["\'])',
        lambda match: (
            f"{match.group(1)}#{BOOTSTRAP_PREFIX}{match.group(2)}{match.group(3)}"
            if match.group(2) in defined_ids
            else match.group(0)
        ),
        namespaced,
    )
    namespaced = re.sub(
        r'url\(\s*#([^)\s]+)\s*\)',
        lambda match: (
            f"url(#{BOOTSTRAP_PREFIX}{match.group(1)})"
            if match.group(1) in defined_ids
            else match.group(0)
        ),
        namespaced,
    )
    return namespaced.replace("<svg ", '<svg class="icon-sprite" aria-hidden="true" ', 1)


def render_dashboard(
    projects: Iterable[DashboardProject],
    generated_at: datetime | None = None,
) -> str:
    ordered = sort_projects_default(projects)
    generated = generated_at or datetime.now(timezone.utc)
    if generated.tzinfo is None:
        raise DashboardError("generated_at must be timezone-aware")
    generated_iso = generated.astimezone(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")
    payload = {
        "generatedAt": generated_iso,
        "projects": [_project_payload(project) for project in ordered],
    }
    try:
        sprite = BOOTSTRAP_SPRITE.read_text(encoding="utf-8")
    except OSError as exc:
        raise DashboardError(f"Bootstrap Icons sprite is unavailable: {BOOTSTRAP_SPRITE}") from exc
    sprite = _namespace_bootstrap_sprite(sprite)
    return (
        HTML_TEMPLATE.replace("__DASHBOARD_DATA__", _json_for_html(payload))
        .replace("__BOOTSTRAP_ICONS__", sprite)
    )


def generate_dashboard(
    root: Path,
    output: Path,
    *,
    generated_at: datetime | None = None,
) -> list[DashboardProject]:
    _, projects = discover_projects(root)
    html = render_dashboard(projects, generated_at=generated_at)
    _atomic_write_text(output, html)
    return projects


def _output_path(root: Path, requested: Path) -> Path:
    return requested if requested.is_absolute() else root / requested


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT, help="Workbench repository root")
    parser.add_argument("--output", type=Path, default=Path(DEFAULT_OUTPUT), help="generated HTML path")
    parser.add_argument("--open", action="store_true", help="open the regenerated dashboard in the default browser")
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
            html = render_dashboard(projects)
            write_server_state(root, server_state)
            _atomic_write_text(output, html)
            print(f"Set {manifest['identity']['name']} to {description} and regenerated {output}")
        else:
            projects = generate_dashboard(root, output)
            print(f"Generated {output} with {len(projects)} projects")
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
  <title>Mynx Workbench</title>
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
      --testing: #a88bd8;
      --blocked: #db7474;
      --parked: #909a96;
      --current: #70bb89;
      --outdated: #e4b75c;
      --not-deployed: #c97373;
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
    .controls { padding: 17px 18px 15px; border-bottom: 1px solid var(--line); background: rgba(19, 43, 32, 0.5); }
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
    .chip[data-lifecycle="TESTING"][aria-selected="true"] { border-color: color-mix(in srgb, var(--testing) 55%, transparent); }
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
      color: var(--muted);
      font-size: 0.8rem;
    }
    #visible-count { font-variant-numeric: tabular-nums; }
    #sort-summary { color: var(--quiet); }

    .table-scroll { overflow-x: auto; scrollbar-gutter: stable; }
    table { width: 100%; min-width: 790px; border-collapse: separate; border-spacing: 0; table-layout: fixed; }
    col.project { width: 39%; }
    col.lifecycle { width: 14%; }
    col.version { width: 19%; }
    col.jar { width: 19%; }
    col.server { width: 9%; }
    thead th {
      position: sticky;
      top: 0;
      z-index: 3;
      height: 43px;
      padding: 0 14px;
      border-bottom: 1px solid var(--line-strong);
      background: #0c2118;
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
    .project-name { overflow: hidden; color: var(--text); font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
    .version-value { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .date-value { color: #c3d0c8; font-variant-numeric: tabular-nums; white-space: nowrap; }
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
    .group-chevron { width: 18px; color: var(--muted); font-size: 0.9rem; transform: rotate(0deg); }
    .group-button[aria-expanded="false"] .group-chevron { transform: rotate(-90deg); }
    .group-count { margin-left: 9px; color: var(--muted); font-weight: 600; letter-spacing: 0; text-transform: none; }
    .life-ACTIVE { --group-color: var(--active); }
    .life-PLANNED { --group-color: var(--planned); }
    .life-ACCEPTED { --group-color: var(--accepted); }
    .life-TESTING { --group-color: var(--testing); }
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
    .icon-sprite { position: absolute; width: 0; height: 0; overflow: hidden; }

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
    }
    @media (prefers-reduced-motion: no-preference) {
      button, input, select, tbody td { transition: border-color 120ms ease, background-color 120ms ease, color 120ms ease; }
      .group-chevron { transition: transform 120ms ease; }
    }
  </style>
</head>
<body>
  __BOOTSTRAP_ICONS__
  <main class="shell">
    <header class="masthead">
      <div>
        <p class="eyebrow">Project Tracker</p>
        <h1>Mynx Workbench</h1>
      </div>
      <div class="masthead-meta">
        <span>Last generated <time id="generated-at"></time></span>
        <span class="phrase">small changes · a quieter world</span>
      </div>
    </header>

    <section class="workspace" aria-label="Mynx Workbench projects">
      <div class="controls">
        <div class="control-row">
          <div class="search-wrap">
            <label class="sr-only" for="search">Search projects</label>
            <svg class="bi search-icon" aria-hidden="true"><use href="#bi-search"></use></svg>
            <input id="search" type="search" autocomplete="off" placeholder="Search projects" spellcheck="false">
            <button id="clear-search" type="button" aria-label="Clear project search" title="Clear project search" hidden><svg class="bi" aria-hidden="true"><use href="#bi-x-lg"></use></svg></button>
          </div>
          <label class="select-wrap" for="server-filter">
            <span>On server</span>
            <span class="select-control">
              <select id="server-filter">
                <option value="ALL">All</option>
                <option value="CURRENT">Current</option>
                <option value="OUTDATED">Outdated</option>
                <option value="NOT_DEPLOYED">Not deployed</option>
              </select>
              <svg class="bi select-chevron" aria-hidden="true"><use href="#bi-chevron-down"></use></svg>
            </span>
          </label>
          <button id="reset-order" class="order-button" type="button" title="Restore Workbench order" disabled><svg class="bi" aria-hidden="true"><use href="#bi-arrow-clockwise"></use></svg> Workbench order</button>
        </div>
        <div class="control-row filter-row">
          <span class="filter-label">Lifecycle</span>
          <div id="lifecycle-filters" class="chips" role="tablist" aria-label="Filter by lifecycle">
            <button class="chip" type="button" role="tab" data-lifecycle="ALL" aria-selected="true">All <span class="chip-count">0</span></button>
            <button class="chip" type="button" role="tab" data-lifecycle="ACTIVE" aria-selected="false">Active <span class="chip-count">0</span></button>
            <button class="chip" type="button" role="tab" data-lifecycle="PLANNED" aria-selected="false">Planned <span class="chip-count">0</span></button>
            <button class="chip" type="button" role="tab" data-lifecycle="ACCEPTED" aria-selected="false">Accepted <span class="chip-count">0</span></button>
            <button class="chip" type="button" role="tab" data-lifecycle="TESTING" aria-selected="false">Testing <span class="chip-count">0</span></button>
            <button class="chip" type="button" role="tab" data-lifecycle="BLOCKED" aria-selected="false">Blocked <span class="chip-count">0</span></button>
            <button class="chip" type="button" role="tab" data-lifecycle="PARKED" aria-selected="false">Parked <span class="chip-count">0</span></button>
          </div>
        </div>
      </div>

      <div class="results-bar">
        <span id="visible-count">0 projects</span>
        <span id="sort-summary">Default Workbench order</span>
      </div>

      <div id="table-region" class="table-scroll">
        <table>
          <caption class="sr-only">Canonical Mynx Workbench project status</caption>
          <colgroup>
            <col class="project"><col class="lifecycle"><col class="version"><col class="jar"><col class="server">
          </colgroup>
          <thead>
            <tr>
              <th scope="col" data-sort-header="name"><button class="sort-button" type="button" data-sort="name">Project <span class="sort-indicator" aria-hidden="true"><svg class="bi"><use href="#bi-arrow-down-up"></use></svg></span></button></th>
              <th scope="col" data-sort-header="lifecycle"><button class="sort-button" type="button" data-sort="lifecycle">Lifecycle <span class="sort-indicator" aria-hidden="true"><svg class="bi"><use href="#bi-arrow-down-up"></use></svg></span></button></th>
              <th scope="col" data-sort-header="version"><button class="sort-button" type="button" data-sort="version">Current Version <span class="sort-indicator" aria-hidden="true"><svg class="bi"><use href="#bi-arrow-down-up"></use></svg></span></button></th>
              <th scope="col" data-sort-header="jar"><button class="sort-button" type="button" data-sort="jar">Last JAR Edit <span class="sort-indicator" aria-hidden="true"><svg class="bi"><use href="#bi-arrow-down-up"></use></svg></span></button></th>
              <th scope="col" data-sort-header="server"><button class="sort-button" type="button" data-sort="server">On Server <span class="sort-indicator" aria-hidden="true"><svg class="bi"><use href="#bi-arrow-down-up"></use></svg></span></button></th>
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
  <script>
    (() => {
      "use strict";
      const data = JSON.parse(document.getElementById("dashboard-data").textContent);
      const projects = data.projects.map((project, defaultIndex) => ({ ...project, defaultIndex }));
      const lifecycleOrder = ["ACTIVE", "PLANNED", "ACCEPTED", "TESTING", "BLOCKED", "PARKED"];
      const lifecycleLabels = { ACTIVE: "Active", PLANNED: "Planned", ACCEPTED: "Accepted", TESTING: "Testing", BLOCKED: "Blocked", PARKED: "Parked" };
      const serverRank = { CURRENT: 0, OUTDATED: 1, NOT_DEPLOYED: 2 };
      const sortLabels = { name: "Project", lifecycle: "Lifecycle", version: "Current Version", jar: "Last JAR Edit", server: "On Server" };
      const state = { lifecycle: "ALL", server: "ALL", search: "", sortKey: "default", sortDirection: "asc", collapsed: new Set() };

      const rowsElement = document.getElementById("project-rows");
      const tableRegion = document.getElementById("table-region");
      const emptyState = document.getElementById("empty-state");
      const visibleCount = document.getElementById("visible-count");
      const sortSummary = document.getElementById("sort-summary");
      const search = document.getElementById("search");
      const clearSearch = document.getElementById("clear-search");
      const serverFilter = document.getElementById("server-filter");
      const resetOrder = document.getElementById("reset-order");
      const lifecycleFilters = document.getElementById("lifecycle-filters");
      const liveRegion = document.getElementById("live-region");

      function formatLocalDate(milliseconds) {
        const date = new Date(milliseconds);
        const datePart = date.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
        const timePart = date.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
        return `${datePart} ${timePart}`;
      }

      const generatedDate = new Date(data.generatedAt);
      const generatedElement = document.getElementById("generated-at");
      generatedElement.dateTime = data.generatedAt;
      generatedElement.title = data.generatedAt;
      generatedElement.textContent = formatLocalDate(generatedDate.getTime());

      function element(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined) node.textContent = text;
        return node;
      }

      function icon(name) {
        const svg = element("svg", "bi");
        svg.setAttribute("aria-hidden", "true");
        const use = document.createElementNS("http://www.w3.org/2000/svg", "use");
        use.setAttribute("href", `#bi-${name}`);
        svg.appendChild(use);
        return svg;
      }

      function baseFilteredProjects() {
        const needle = state.search.toLocaleLowerCase();
        return projects.filter(project => {
          const matchesName = !needle || project.name.toLocaleLowerCase().includes(needle);
          const matchesServer = state.server === "ALL" || project.server === state.server;
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
        if (state.sortKey === "default" || state.sortKey === "lifecycle") {
          return left.defaultIndex - right.defaultIndex;
        }
        const direction = state.sortDirection === "asc" ? 1 : -1;
        let result = 0;
        if (state.sortKey === "name") {
          result = direction * left.name.localeCompare(right.name, undefined, { sensitivity: "base", numeric: true });
        } else if (state.sortKey === "version") {
          result = compareNullable(left.version, right.version, direction, (a, b) => {
            if (left.versionCanary !== null && right.versionCanary !== null) return left.versionCanary - right.versionCanary;
            return a.localeCompare(b, undefined, { sensitivity: "base", numeric: true });
          });
        } else if (state.sortKey === "jar") {
          result = compareNullable(left.jarMtimeMs, right.jarMtimeMs, direction, (a, b) => a - b);
        } else if (state.sortKey === "server") {
          result = direction * (serverRank[left.server] - serverRank[right.server]);
        }
        return result || left.defaultIndex - right.defaultIndex;
      }

      function orderedLifecycles() {
        if (state.sortKey === "lifecycle" && state.sortDirection === "desc") return [...lifecycleOrder].reverse();
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
        const row = element("tr", "project-row");
        row.dataset.lifecycle = lifecycle;
        if (state.collapsed.has(lifecycle)) row.hidden = true;

        const nameCell = element("td", "project-name", project.name);
        nameCell.title = project.name;
        row.appendChild(nameCell);

        const lifecycleCell = element("td");
        lifecycleCell.appendChild(lifecyclePill(project.lifecycle));
        row.appendChild(lifecycleCell);

        const versionCell = element("td", project.version === null ? "version-value muted" : "version-value", project.versionDisplay ?? "—");
        if (project.version) versionCell.title = project.version;
        row.appendChild(versionCell);

        const jarCell = element("td", "date-value");
        if (project.jarMtimeMs === null) {
          const unavailable = element("span", "muted", "—");
          unavailable.title = project.jarNote;
          jarCell.appendChild(unavailable);
        } else {
          const time = element("time", "", formatLocalDate(project.jarMtimeMs));
          time.dateTime = project.jarMtimeIso;
          time.title = `${project.jarMtimeIso} · ${project.jarNote}`;
          jarCell.appendChild(time);
        }
        row.appendChild(jarCell);

        const serverCell = element("td");
        serverCell.appendChild(serverPill(project));
        row.appendChild(serverCell);
        return row;
      }

      function groupRow(lifecycle, count) {
        const row = element("tr", `group-row life-${lifecycle}`);
        const heading = document.createElement("th");
        heading.colSpan = 5;
        heading.scope = "rowgroup";
        const button = element("button", "group-button");
        button.type = "button";
        button.dataset.collapse = lifecycle;
        button.setAttribute("aria-expanded", String(!state.collapsed.has(lifecycle)));
        const chevron = icon("chevron-down");
        chevron.classList.add("group-chevron");
        button.appendChild(chevron);
        button.appendChild(document.createTextNode(lifecycle));
        button.appendChild(element("span", "group-count", `${count} ${count === 1 ? "project" : "projects"}`));
        heading.appendChild(button);
        row.appendChild(heading);
        return row;
      }

      function updateCounts(baseProjects) {
        const counts = Object.fromEntries(lifecycleOrder.map(lifecycle => [lifecycle, 0]));
        for (const project of baseProjects) counts[project.lifecycle] += 1;
        for (const chip of lifecycleFilters.querySelectorAll(".chip")) {
          const lifecycle = chip.dataset.lifecycle;
          chip.setAttribute("aria-selected", String(lifecycle === state.lifecycle));
          chip.querySelector(".chip-count").textContent = lifecycle === "ALL" ? baseProjects.length : counts[lifecycle];
        }
      }

      function updateSortState() {
        for (const heading of document.querySelectorAll("[data-sort-header]")) {
          const key = heading.dataset.sortHeader;
          const indicator = heading.querySelector(".sort-indicator");
          if (key === state.sortKey) {
            heading.setAttribute("aria-sort", state.sortDirection === "asc" ? "ascending" : "descending");
            indicator.replaceChildren(icon(state.sortDirection === "asc" ? "sort-up" : "sort-down"));
          } else {
            heading.removeAttribute("aria-sort");
            indicator.replaceChildren(icon("arrow-down-up"));
          }
        }
        resetOrder.disabled = state.sortKey === "default";
        sortSummary.textContent = state.sortKey === "default"
          ? "Default Workbench order"
          : `${sortLabels[state.sortKey]} · ${state.sortDirection === "asc" ? "ascending" : "descending"}`;
      }

      function render(focusLifecycle) {
        const baseProjects = baseFilteredProjects();
        updateCounts(baseProjects);
        const visible = baseProjects.filter(project => state.lifecycle === "ALL" || project.lifecycle === state.lifecycle);
        rowsElement.replaceChildren();

        for (const lifecycle of orderedLifecycles()) {
          const group = visible.filter(project => project.lifecycle === lifecycle).sort(projectComparator);
          if (!group.length) continue;
          rowsElement.appendChild(groupRow(lifecycle, group.length));
          for (const project of group) rowsElement.appendChild(projectRow(project, lifecycle));
        }

        const isEmpty = visible.length === 0;
        tableRegion.hidden = isEmpty;
        emptyState.hidden = !isEmpty;
        visibleCount.textContent = `${visible.length} ${visible.length === 1 ? "project" : "projects"}`;
        clearSearch.hidden = !state.search;
        updateSortState();
        liveRegion.textContent = isEmpty ? "No matching projects" : `Showing ${visible.length} projects`;

        if (focusLifecycle) {
          const button = rowsElement.querySelector(`[data-collapse="${focusLifecycle}"]`);
          if (button) button.focus();
        }
      }

      search.addEventListener("input", () => {
        state.search = search.value.trim();
        render();
      });
      clearSearch.addEventListener("click", () => {
        search.value = "";
        state.search = "";
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
        state.lifecycle = chip.dataset.lifecycle;
        render();
      });
      document.querySelector("thead").addEventListener("click", event => {
        const button = event.target.closest("[data-sort]");
        if (!button) return;
        const key = button.dataset.sort;
        if (state.sortKey === key) {
          state.sortDirection = state.sortDirection === "asc" ? "desc" : "asc";
        } else {
          state.sortKey = key;
          state.sortDirection = key === "jar" ? "desc" : "asc";
        }
        render();
      });
      resetOrder.addEventListener("click", () => {
        state.sortKey = "default";
        state.sortDirection = "asc";
        render();
      });
      rowsElement.addEventListener("click", event => {
        const button = event.target.closest("[data-collapse]");
        if (!button) return;
        const lifecycle = button.dataset.collapse;
        if (state.collapsed.has(lifecycle)) state.collapsed.delete(lifecycle);
        else state.collapsed.add(lifecycle);
        render(lifecycle);
      });
      document.getElementById("clear-filters").addEventListener("click", () => {
        state.lifecycle = "ALL";
        state.server = "ALL";
        state.search = "";
        search.value = "";
        serverFilter.value = "ALL";
        render();
        search.focus();
      });

      render();
    })();
  </script>
</body>
</html>
'''


if __name__ == "__main__":
    raise SystemExit(main())
