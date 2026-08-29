#!/usr/bin/env python3
"""Prepare and, only after explicit cutover, publish main project revisions."""

from __future__ import annotations

import argparse
import base64
import hashlib
import hmac
import json
import os
import re
import subprocess
import sys
import time
import urllib.request
from pathlib import Path, PurePosixPath
from typing import Any

try:
    from .workbench import ValidationError, load_json, load_json_text, validate_codex_log, validate_log_append, validate_repository, validate_status, validate_status_transition
except ImportError:  # Direct execution from tools/.
    from workbench import ValidationError, load_json, load_json_text, validate_codex_log, validate_log_append, validate_repository, validate_status, validate_status_transition  # type: ignore


ZERO_COMMIT = "0" * 40
HUMAN_FIELDS = ["Priority", "Notes"]
FREEZE_UUID_RE = re.compile(r"^`([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})`$")
RECORD_KEYS = {
    "project_uuid",
    "project_name",
    "project_id",
    "aliases",
    "legacy_names",
    "legacy_ids",
    "lifecycle",
    "goals",
    "scope",
    "boundaries",
    "dependencies",
    "milestone",
    "current_version",
    "current_artifact_filename",
    "current_artifact_sha256",
    "current_source_commit",
    "accepted_version",
    "accepted_artifact_filename",
    "accepted_artifact_sha256",
    "accepted_source_commit",
    "rollback_version",
    "rollback_artifact_filename",
    "rollback_artifact_sha256",
    "rollback_source_commit",
    "accepted_current",
    "accepted_rollback",
    "build_validation",
    "deployment_validation",
    "runtime_validation",
    "blocker",
    "revision",
    "activity_at",
    "updated_at",
    "last_codex_at",
    "source_commit",
    "sheet_participates",
    "sheet_exclusion_reason",
    "publication_commit",
}


def migration_adoption_uuids(freeze_text: str) -> frozenset[str]:
    """Read the immutable project UUID allowlist from the migration-freeze table."""

    in_overrides = False
    in_table = False
    result: set[str] = set()
    for raw_line in freeze_text.splitlines():
        line = raw_line.strip()
        if line == "## Project-scoped overrides":
            in_overrides = True
            continue
        if not in_overrides:
            continue
        if line.startswith("## "):
            break
        if not line.startswith("|"):
            if in_table and line:
                break
            continue
        cells = [cell.strip() for cell in line.strip("|").split("|")]
        if len(cells) != 5:
            raise ValidationError("MIGRATION_FREEZE.md project override table must have exactly five columns")
        in_table = True
        uuid_cell = cells[2]
        if uuid_cell == "UUID" or set(uuid_cell) <= {"-", ":"}:
            continue
        match = FREEZE_UUID_RE.fullmatch(uuid_cell)
        if not match:
            raise ValidationError("MIGRATION_FREEZE.md project override UUID must be canonical lowercase and backtick-delimited")
        project_uuid = match.group(1)
        if project_uuid in result:
            raise ValidationError(f"MIGRATION_FREEZE.md contains duplicate project UUID {project_uuid}")
        result.add(project_uuid)
    return frozenset(result)


def _validate_initial_revision(manifest: dict[str, Any], path: str, adoption_uuids: frozenset[str]) -> None:
    revision = manifest["synchronization"]["revision"]
    if revision == 1 or manifest["identity"]["uuid"] in adoption_uuids:
        return
    raise ValidationError(f"{path}: a new project must begin at revision 1 unless its UUID was frozen for migration adoption")


def _migration_adoption_uuids_at(root: Path, before: str, after: str) -> frozenset[str]:
    freeze_commit = after if before == ZERO_COMMIT else before
    freeze_text = _git_text(root, freeze_commit, "MIGRATION_FREEZE.md") or ""
    return migration_adoption_uuids(freeze_text)


def _git(root: Path, *arguments: str, check: bool = True) -> str:
    completed = subprocess.run(
        ["git", "-C", str(root), *arguments],
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    if check and completed.returncode != 0:
        raise ValidationError(f"git {' '.join(arguments)} failed: {completed.stderr.strip()}")
    return completed.stdout


def _git_text(root: Path, commit: str, path: str) -> str | None:
    if commit == ZERO_COMMIT:
        return None
    completed = subprocess.run(
        ["git", "-C", str(root), "show", f"{commit}:{path}"],
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    if completed.returncode == 0:
        return completed.stdout
    return None


def _manifest_paths_at(root: Path, commit: str) -> list[str]:
    if commit == ZERO_COMMIT:
        return []
    paths = _git(root, "ls-tree", "-r", "--name-only", commit).splitlines()
    result: list[str] = []
    for path in paths:
        parts = PurePosixPath(path).parts
        if len(parts) == 3 and parts[0] in {"projects", "resourcepacks"} and parts[2] == "WORKBENCH_STATUS.json":
            result.append(path)
    return sorted(result)


def _manifests_at(root: Path, commit: str) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for path in _manifest_paths_at(root, commit):
        text = _git_text(root, commit, path)
        if text is None:  # pragma: no cover - ls-tree/show inconsistency
            raise ValidationError(f"cannot read {path} at {commit}")
        manifest = load_json_text(text, f"{commit}:{path}")
        validate_status(manifest)
        result[path] = manifest
    return result


def _release_fields(release: dict[str, Any] | None) -> tuple[Any, Any, Any, Any]:
    if release is None:
        return None, None, None, None
    artifact = release["artifact"]
    return (
        release["version"],
        None if artifact is None else artifact["filename"],
        None if artifact is None else artifact["sha256"],
        release["source_commit"],
    )


def flatten_manifest(manifest: dict[str, Any], publication_commit: str) -> dict[str, Any]:
    validate_status(manifest)
    identity = manifest["identity"]
    definition = manifest["definition"]
    state = manifest["state"]
    synchronization = manifest["synchronization"]
    current_version, current_filename, current_sha, current_source = _release_fields(state["releases"]["current"])
    accepted_version, accepted_filename, accepted_sha, accepted_source = _release_fields(state["releases"]["accepted"])
    rollback_version, rollback_filename, rollback_sha, rollback_source = _release_fields(state["releases"]["rollback"])
    record = {
        "project_uuid": identity["uuid"],
        "project_name": identity["name"],
        "project_id": identity["project_id"],
        "aliases": identity["aliases"],
        "legacy_names": identity["legacy_names"],
        "legacy_ids": identity["legacy_ids"],
        "lifecycle": definition["lifecycle"],
        "goals": definition["goals"],
        "scope": definition["scope"],
        "boundaries": definition["boundaries"],
        "dependencies": definition["dependencies"],
        "milestone": state["milestone"],
        "current_version": current_version,
        "current_artifact_filename": current_filename,
        "current_artifact_sha256": current_sha,
        "current_source_commit": current_source,
        "accepted_version": accepted_version,
        "accepted_artifact_filename": accepted_filename,
        "accepted_artifact_sha256": accepted_sha,
        "accepted_source_commit": accepted_source,
        "rollback_version": rollback_version,
        "rollback_artifact_filename": rollback_filename,
        "rollback_artifact_sha256": rollback_sha,
        "rollback_source_commit": rollback_source,
        "accepted_current": state["releases"]["accepted_current"],
        "accepted_rollback": state["releases"]["accepted_rollback"],
        "build_validation": state["validation"]["build"],
        "deployment_validation": state["validation"]["deployment"],
        "runtime_validation": state["validation"]["runtime"],
        "blocker": state["blocker"],
        "revision": synchronization["revision"],
        "activity_at": synchronization["activity_at"],
        "updated_at": synchronization["updated_at"],
        "last_codex_at": synchronization["last_codex_at"],
        "source_commit": synchronization["source_commit"],
        "sheet_participates": synchronization["google_sheet"]["participates"],
        "sheet_exclusion_reason": synchronization["google_sheet"]["exclusion_reason"],
        "publication_commit": publication_commit,
    }
    if set(record) != RECORD_KEYS:
        raise AssertionError("publisher record contract drift")
    lowered = {key.casefold() for key in record}
    if any(field.casefold() in lowered for field in HUMAN_FIELDS):
        raise ValidationError("publisher record must not contain human-owned fields")
    return record


def build_events(
    previous: dict[str, dict[str, Any]],
    current: dict[str, dict[str, Any]],
    *,
    repository: str,
    ref: str,
    publication_commit: str,
    config: dict[str, Any],
    migration_adoption_uuids: frozenset[str] = frozenset(),
) -> list[dict[str, Any]]:
    if repository != config["repository"]:
        raise ValidationError(f"authoritative publication repository must be {config['repository']}")
    if ref != config["authoritative_ref"]:
        raise ValidationError(f"authoritative publication ref must be {config['authoritative_ref']}")
    if set(previous) - set(current):
        raise ValidationError("project manifest deletion cannot be published as a normal status update")
    events: list[dict[str, Any]] = []
    current_uuids: dict[str, str] = {}
    for path in sorted(current):
        manifest = current[path]
        validate_status(manifest)
        project_uuid = manifest["identity"]["uuid"]
        if project_uuid in current_uuids:
            raise ValidationError(f"duplicate project UUID in publication set: {current_uuids[project_uuid]} and {path}")
        current_uuids[project_uuid] = path
        before = previous.get(path)
        if before == manifest:
            continue
        if before is None:
            _validate_initial_revision(manifest, path, migration_adoption_uuids)
        else:
            validate_status_transition(before, manifest)
        participates = manifest["synchronization"]["google_sheet"]["participates"]
        previously_participated = before is not None and before["synchronization"]["google_sheet"]["participates"]
        if not participates and not previously_participated:
            continue
        revision = manifest["synchronization"]["revision"]
        identity = f"{repository}\n{ref}\n{publication_commit}\n{project_uuid}\n{revision}".encode("utf-8")
        event_id = hashlib.sha256(identity).hexdigest()
        events.append(
            {
                "contract_version": config["contract_version"],
                "event_id": event_id,
                "operation": "project_status_upsert",
                "source": {
                    "repository": repository,
                    "ref": ref,
                    "publication_commit": publication_commit,
                    "manifest_path": path,
                },
                "record": flatten_manifest(manifest, publication_commit),
                "ownership": {"sheet_preserves": list(config["sheet_preserves"])},
            }
        )
    return events


def _changed_paths(root: Path, before: str, after: str) -> list[str]:
    if before == ZERO_COMMIT:
        return _git(root, "ls-tree", "-r", "--name-only", after).splitlines()
    return _git(root, "diff", "--name-only", before, after).splitlines()


def validate_project_push_contract(root: Path, before: str, after: str) -> None:
    changed = set(_changed_paths(root, before, after))
    if before != ZERO_COMMIT and "MIGRATION_FREEZE.md" in changed:
        raise ValidationError("MIGRATION_FREEZE.md is immutable")
    adoption_uuids = _migration_adoption_uuids_at(root, before, after)
    project_roots: set[str] = set()
    for path in changed:
        parts = PurePosixPath(path).parts
        if parts and parts[0] == "originals":
            raise ValidationError("originals/ is immutable")
        if len(parts) >= 3 and parts[0] in {"projects", "resourcepacks"}:
            project_roots.add("/".join(parts[:2]))
    for project_root in project_roots:
        status_path = f"{project_root}/WORKBENCH_STATUS.json"
        log_path = f"{project_root}/CODEX_LOG.md"
        testing_path = f"{project_root}/TESTING.md"
        current_status_text = _git_text(root, after, status_path)
        current_log = _git_text(root, after, log_path)
        current_testing = _git_text(root, after, testing_path)
        if current_status_text is None or current_log is None or current_testing is None:
            raise ValidationError(f"{project_root}: all three project control files are required")
        if status_path not in changed or log_path not in changed:
            raise ValidationError(f"{project_root}: every project task must change WORKBENCH_STATUS.json and append CODEX_LOG.md")
        current_status = load_json_text(current_status_text, f"{after}:{status_path}")
        validate_status(current_status)
        previous_status_text = _git_text(root, before, status_path)
        previous_log = _git_text(root, before, log_path)
        if previous_status_text is None:
            _validate_initial_revision(current_status, project_root, adoption_uuids)
            if not current_log.strip():
                raise ValidationError(f"{log_path}: new project requires an initial log entry")
            validate_codex_log(current_log, current_status)
        else:
            previous_status = load_json_text(previous_status_text, f"{before}:{status_path}")
            validate_status_transition(previous_status, current_status)
            validate_log_append(previous_log or "", current_log, current_status)


def make_plan(root: Path, before: str, after: str, repository: str, ref: str, config: dict[str, Any]) -> dict[str, Any]:
    if len(after) != 40 or any(character not in "0123456789abcdef" for character in after):
        raise ValidationError("after must be an exact lowercase 40-hex commit")
    if before != ZERO_COMMIT and (len(before) != 40 or any(character not in "0123456789abcdef" for character in before)):
        raise ValidationError("before must be an exact lowercase 40-hex commit or all-zero initial value")
    if repository != config["repository"] or ref != config["authoritative_ref"]:
        raise ValidationError("only the configured main ref may prepare an authoritative publication plan")
    validate_repository(root)
    validate_project_push_contract(root, before, after)
    adoption_uuids = _migration_adoption_uuids_at(root, before, after)
    previous = _manifests_at(root, before)
    current = _manifests_at(root, after)
    events = build_events(
        previous,
        current,
        repository=repository,
        ref=ref,
        publication_commit=after,
        config=config,
        migration_adoption_uuids=adoption_uuids,
    )
    return {
        "contract_version": config["contract_version"],
        "source": {"repository": repository, "ref": ref, "before": before, "after": after},
        "events": events,
    }


def _validate_plan(plan: dict[str, Any], config: dict[str, Any]) -> None:
    if set(plan) != {"contract_version", "source", "events"}:
        raise ValidationError("publication plan has unknown or missing keys")
    if plan["contract_version"] != config["contract_version"]:
        raise ValidationError("publication plan contract version mismatch")
    source = plan["source"]
    if not isinstance(source, dict) or set(source) != {"repository", "ref", "before", "after"}:
        raise ValidationError("publication plan source contract is invalid")
    if source["repository"] != config["repository"] or source["ref"] != config["authoritative_ref"]:
        raise ValidationError("publication plan is not from authoritative main")
    if not isinstance(plan["events"], list):
        raise ValidationError("publication plan events must be an array")


def signed_wrapper(event: dict[str, Any], secret: str) -> bytes:
    if len(secret.encode("utf-8")) < 32:
        raise ValidationError("Sheet HMAC secret must be at least 32 UTF-8 bytes")
    event_bytes = json.dumps(event, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
    payload = base64.urlsafe_b64encode(event_bytes).decode("ascii").rstrip("=")
    signature = hmac.new(secret.encode("utf-8"), payload.encode("ascii"), hashlib.sha256).hexdigest()
    return json.dumps(
        {"payload": payload, "signature": f"sha256={signature}"},
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")


def publish_plan(plan: dict[str, Any], config: dict[str, Any], environment: dict[str, str] | None = None) -> int:
    """Publish individual signed envelopes. All gates fail before transport."""

    _validate_plan(plan, config)
    environment = os.environ if environment is None else environment
    if config["enabled"] is not True:
        raise ValidationError("Sheet publication is tracked-disabled; cutover has not occurred")
    if environment.get(config["cutover_environment_variable"]) != config["cutover_required_value"]:
        raise ValidationError("Sheet publication cutover variable is not authorized")
    receiver_url = environment.get(config["receiver_url_environment_variable"])
    hmac_secret = environment.get(config["hmac_environment_variable"])
    if not receiver_url or not hmac_secret:
        raise ValidationError("Sheet receiver URL and HMAC secret are required after cutover")
    published = 0
    for event in plan["events"]:
        wrapper = signed_wrapper(event, hmac_secret)
        request = urllib.request.Request(receiver_url, data=wrapper, method="POST", headers={"Content-Type": "application/json"})
        retry_delays = [2, 4, 8, 16]
        for attempt in range(len(retry_delays) + 1):
            with urllib.request.urlopen(request, timeout=30) as response:  # noqa: S310 - gated configured endpoint.
                if response.status < 200 or response.status >= 300:
                    raise ValidationError(f"Sheet receiver returned HTTP {response.status}")
                response_text = response.read().decode("utf-8")
            try:
                result = json.loads(response_text)
            except json.JSONDecodeError as exc:
                raise ValidationError("Sheet receiver returned invalid JSON") from exc
            if isinstance(result, dict) and result.get("ok") is True and result.get("event_id") == event["event_id"]:
                break
            retryable = isinstance(result, dict) and result.get("code") in {"revision_gap", "busy"}
            if retryable and attempt < len(retry_delays):
                time.sleep(retry_delays[attempt])
                continue
            detail = result.get("error") if isinstance(result, dict) else None
            raise ValidationError(f"Sheet receiver rejected event {event['event_id']}: {detail or 'invalid acknowledgement'}")
        published += 1
    return published


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)
    plan = subparsers.add_parser("plan", help="prepare a secretless authoritative main publication plan")
    plan.add_argument("--root", type=Path, default=Path("."))
    plan.add_argument("--before", required=True)
    plan.add_argument("--after", required=True)
    plan.add_argument("--repository", required=True)
    plan.add_argument("--ref", required=True)
    plan.add_argument("--output", type=Path, required=True)
    publish = subparsers.add_parser("publish", help="publish a prepared plan after every cutover gate passes")
    publish.add_argument("--root", type=Path, default=Path("."))
    publish.add_argument("--plan", type=Path, required=True)
    change = subparsers.add_parser("validate-change", help="validate project revision/log protocol between two commits")
    change.add_argument("--root", type=Path, default=Path("."))
    change.add_argument("--before", required=True)
    change.add_argument("--after", required=True)
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    try:
        root = args.root.resolve()
        config = load_json(root / "tools" / "sheet_sync" / "publication.json")
        if args.command == "plan":
            plan = make_plan(root, args.before, args.after, args.repository, args.ref, config)
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(json.dumps(plan, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
            print(f"Prepared {len(plan['events'])} publication event(s); live publication remains separately gated.")
        elif args.command == "publish":
            plan = load_json(args.plan)
            count = publish_plan(plan, config)
            print(f"Published {count} project status event(s).")
        elif args.command == "validate-change":
            validate_project_push_contract(root, args.before, args.after)
            print("Project revision/log change protocol passed.")
    except (ValidationError, OSError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
