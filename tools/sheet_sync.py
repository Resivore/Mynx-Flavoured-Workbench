#!/usr/bin/env python3
"""Reconcile the Sheet mirror with the current authoritative main state."""

from __future__ import annotations

import argparse
import base64
import hashlib
import hmac
import http.client
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path, PurePosixPath
from typing import Any, Callable

try:
    from .workbench import ValidationError, load_json, load_json_text, validate_codex_log, validate_log_append, validate_repository, validate_status, validate_status_transition
except ImportError:  # Direct execution from tools/.
    from workbench import ValidationError, load_json, load_json_text, validate_codex_log, validate_log_append, validate_repository, validate_status, validate_status_transition  # type: ignore


ZERO_COMMIT = "0" * 40
PLAN_KIND_INCREMENTAL = "incremental"
PLAN_KIND_CURRENT_STATE_BOOTSTRAP = "current_state_bootstrap"
PLAN_KIND_CURRENT_STATE_RECONCILIATION = "current_state_reconciliation"
HUMAN_FIELDS = ["Notes"]
RECEIVER_RESPONSE_TIMEOUT_SECONDS = 60
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
    if manifest["identity"]["uuid"] in adoption_uuids:
        return
    if revision != 1:
        raise ValidationError(f"{path}: a new project must begin at revision 1 unless its UUID was frozen for migration adoption")
    current = manifest["state"]["releases"]["current"]
    if (
        current is not None
        and current["artifact"] is not None
        and "runtime_dependency_policy" not in current
    ):
        raise ValidationError(
            f"{path}: a new revision-1 current artifact requires state.releases.current.runtime_dependency_policy"
        )


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


def _validate_authoritative_source(repository: str, ref: str, config: dict[str, Any]) -> None:
    if repository != config["repository"]:
        raise ValidationError(f"authoritative publication repository must be {config['repository']}")
    if ref != config["authoritative_ref"]:
        raise ValidationError(f"authoritative publication ref must be {config['authoritative_ref']}")


def _validate_exact_commit(commit: str, label: str) -> None:
    if (
        not isinstance(commit, str)
        or len(commit) != 40
        or any(character not in "0123456789abcdef" for character in commit)
    ):
        raise ValidationError(f"{label} must be an exact lowercase 40-hex commit")


def _event_for_manifest(
    path: str,
    manifest: dict[str, Any],
    *,
    repository: str,
    ref: str,
    publication_commit: str,
    config: dict[str, Any],
) -> dict[str, Any]:
    """Build the ordinary receiver envelope used by every publication mode."""

    project_uuid = manifest["identity"]["uuid"]
    revision = manifest["synchronization"]["revision"]
    identity = f"{repository}\n{ref}\n{publication_commit}\n{project_uuid}\n{revision}".encode("utf-8")
    event_id = hashlib.sha256(identity).hexdigest()
    return {
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
    _validate_authoritative_source(repository, ref, config)
    events: list[dict[str, Any]] = []
    previous_uuid_paths: dict[str, str] = {}
    for path in sorted(previous):
        manifest = previous[path]
        validate_status(manifest)
        project_uuid = manifest["identity"]["uuid"]
        if project_uuid in previous_uuid_paths:
            raise ValidationError(
                f"duplicate project UUID in previous publication set: {previous_uuid_paths[project_uuid]} and {path}"
            )
        previous_uuid_paths[project_uuid] = path
    current_uuids: dict[str, str] = {}
    for path in sorted(current):
        manifest = current[path]
        validate_status(manifest)
        project_uuid = manifest["identity"]["uuid"]
        if project_uuid in current_uuids:
            raise ValidationError(f"duplicate project UUID in publication set: {current_uuids[project_uuid]} and {path}")
        current_uuids[project_uuid] = path
        previous_path = previous_uuid_paths.get(project_uuid)
        if previous_path is not None and previous_path != path:
            raise ValidationError(f"project UUID cannot move from {previous_path} to {path}")
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
        events.append(
            _event_for_manifest(
                path,
                manifest,
                repository=repository,
                ref=ref,
                publication_commit=publication_commit,
                config=config,
            )
        )
    return events


def build_current_state_events(
    current: dict[str, dict[str, Any]],
    *,
    repository: str,
    ref: str,
    publication_commit: str,
    config: dict[str, Any],
) -> list[dict[str, Any]]:
    """Build ordinary upserts for every participating manifest at one commit."""

    _validate_authoritative_source(repository, ref, config)
    events: list[dict[str, Any]] = []
    current_uuids: dict[str, str] = {}
    for path in sorted(current):
        manifest = current[path]
        validate_status(manifest)
        project_uuid = manifest["identity"]["uuid"]
        if project_uuid in current_uuids:
            raise ValidationError(f"duplicate project UUID in publication set: {current_uuids[project_uuid]} and {path}")
        current_uuids[project_uuid] = path
        if not manifest["synchronization"]["google_sheet"]["participates"]:
            continue
        events.append(
            _event_for_manifest(
                path,
                manifest,
                repository=repository,
                ref=ref,
                publication_commit=publication_commit,
                config=config,
            )
        )
    return events


def _current_state_records(
    root: Path,
    commit: str,
) -> tuple[dict[str, dict[str, Any]], list[dict[str, str]]]:
    """Read independently-valid current manifests without a repository-wide gate.

    The Sheet is a materialized view, so one malformed project must not keep
    every other valid project stale.  Duplicate UUIDs are isolated as well:
    neither ambiguous row is published, while unrelated UUIDs remain eligible.
    """

    records: dict[str, dict[str, Any]] = {}
    failures: list[dict[str, str]] = []
    uuid_paths: dict[str, list[str]] = {}
    for path in _manifest_paths_at(root, commit):
        try:
            text = _git_text(root, commit, path)
            if text is None:
                raise ValidationError(f"cannot read {path} at {commit}")
            manifest = load_json_text(text, f"{commit}:{path}")
            validate_status(manifest)
        except (ValidationError, UnicodeDecodeError, json.JSONDecodeError) as exc:
            failures.append({"manifest_path": path, "error": str(exc)})
            continue
        records[path] = manifest
        uuid_paths.setdefault(manifest["identity"]["uuid"], []).append(path)

    for project_uuid, paths in uuid_paths.items():
        if len(paths) < 2:
            continue
        detail = f"duplicate project UUID {project_uuid} in current authoritative state: {', '.join(sorted(paths))}"
        for path in paths:
            records.pop(path, None)
            failures.append({"manifest_path": path, "error": detail})
    return records, sorted(failures, key=lambda failure: failure["manifest_path"])


def _changed_paths(root: Path, before: str, after: str) -> list[str]:
    if before == ZERO_COMMIT:
        return _git(root, "ls-tree", "-r", "--name-only", after).splitlines()
    return _git(root, "diff", "--name-only", before, after).splitlines()


def _tracked_paths_under(root: Path, commit: str, path: str) -> list[str]:
    if commit == ZERO_COMMIT:
        return []
    return _git(root, "ls-tree", "-r", "--name-only", commit, "--", path).splitlines()


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
        previous_status_text = _git_text(root, before, status_path)
        previous_log = _git_text(root, before, log_path)
        previous_testing = _git_text(root, before, testing_path)
        current_controls = (current_status_text, current_log, current_testing)
        previous_controls = (previous_status_text, previous_log, previous_testing)
        if all(control is None for control in current_controls):
            if not all(control is not None for control in previous_controls):
                raise ValidationError(f"{project_root}: complete deletion requires all three prior project control files")
            if _tracked_paths_under(root, after, project_root):
                raise ValidationError(f"{project_root}: project deletion must remove the entire project directory")
            continue
        if current_status_text is None or current_log is None or current_testing is None:
            raise ValidationError(f"{project_root}: all three project control files are required")
        if status_path not in changed or log_path not in changed:
            raise ValidationError(f"{project_root}: every project task must change WORKBENCH_STATUS.json and append CODEX_LOG.md")
        current_status = load_json_text(current_status_text, f"{after}:{status_path}")
        validate_status(current_status)
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
    _validate_exact_commit(after, "after")
    if before != ZERO_COMMIT:
        _validate_exact_commit(before, "before")
    _validate_authoritative_source(repository, ref, config)
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
        "plan_kind": PLAN_KIND_INCREMENTAL,
        "source": {"repository": repository, "ref": ref, "before": before, "after": after},
        "events": events,
    }


def make_current_state_plan(
    root: Path,
    commit: str,
    repository: str,
    ref: str,
    config: dict[str, Any],
) -> dict[str, Any]:
    """Prepare a legacy explicit current-state plan from checked-out main."""

    _validate_exact_commit(commit, "commit")
    _validate_authoritative_source(repository, ref, config)
    head_commit = _git(root, "rev-parse", "--verify", "HEAD^{commit}").strip()
    if head_commit != commit:
        raise ValidationError("current-state bootstrap commit must exactly match checked-out HEAD")
    ref_commit = _git(root, "rev-parse", "--verify", f"{ref}^{{commit}}").strip()
    if ref_commit != commit:
        raise ValidationError("current-state bootstrap commit must exactly match the configured authoritative ref")
    current, record_failures = _current_state_records(root, commit)
    events = build_current_state_events(
        current,
        repository=repository,
        ref=ref,
        publication_commit=commit,
        config=config,
    )
    return {
        "contract_version": config["contract_version"],
        "plan_kind": PLAN_KIND_CURRENT_STATE_BOOTSTRAP,
        "source": {"repository": repository, "ref": ref, "commit": commit},
        "events": events,
        "record_failures": record_failures,
    }


def make_current_state_reconciliation_plan(
    root: Path,
    repository: str,
    ref: str,
    config: dict[str, Any],
) -> dict[str, Any]:
    """Build a current-state plan from the actual fetched ``origin/main``.

    Callers must fetch and detach at ``origin/main`` immediately beforehand.
    Verifying the detached checkout against that remote-tracking ref prevents a
    delayed workflow from mirroring its event SHA after authoritative main has
    advanced.
    """

    _validate_authoritative_source(repository, ref, config)
    head_commit = _git(root, "rev-parse", "--verify", "HEAD^{commit}").strip()
    authoritative_commit = _git(root, "rev-parse", "--verify", "origin/main^{commit}").strip()
    _validate_exact_commit(head_commit, "checked-out reconciliation commit")
    if head_commit != authoritative_commit:
        raise ValidationError("checked-out reconciliation commit must exactly match fetched origin/main")
    current, record_failures = _current_state_records(root, head_commit)
    events = build_current_state_events(
        current,
        repository=repository,
        ref=ref,
        publication_commit=head_commit,
        config=config,
    )
    return {
        "contract_version": config["contract_version"],
        "plan_kind": PLAN_KIND_CURRENT_STATE_RECONCILIATION,
        "source": {"repository": repository, "ref": ref, "commit": head_commit},
        "events": events,
        "record_failures": record_failures,
    }


def _validate_plan(plan: dict[str, Any], config: dict[str, Any]) -> None:
    plan_kind = plan.get("plan_kind") if isinstance(plan, dict) else None
    expected_plan_keys = {"contract_version", "plan_kind", "source", "events"}
    if plan_kind in {PLAN_KIND_CURRENT_STATE_BOOTSTRAP, PLAN_KIND_CURRENT_STATE_RECONCILIATION}:
        expected_plan_keys.add("record_failures")
    if not isinstance(plan, dict) or set(plan) != expected_plan_keys:
        raise ValidationError("publication plan has unknown or missing keys")
    if plan["contract_version"] != config["contract_version"]:
        raise ValidationError("publication plan contract version mismatch")
    if not isinstance(plan_kind, str) or plan_kind not in {
        PLAN_KIND_INCREMENTAL,
        PLAN_KIND_CURRENT_STATE_BOOTSTRAP,
        PLAN_KIND_CURRENT_STATE_RECONCILIATION,
    }:
        raise ValidationError("publication plan kind is invalid")
    source = plan["source"]
    expected_source_keys = (
        {"repository", "ref", "before", "after"}
        if plan_kind == PLAN_KIND_INCREMENTAL
        else {"repository", "ref", "commit"}
    )
    if not isinstance(source, dict) or set(source) != expected_source_keys:
        raise ValidationError("publication plan source contract is invalid")
    if source["repository"] != config["repository"] or source["ref"] != config["authoritative_ref"]:
        raise ValidationError("publication plan is not from authoritative main")
    if plan_kind == PLAN_KIND_INCREMENTAL:
        if source["before"] != ZERO_COMMIT:
            _validate_exact_commit(source["before"], "publication plan before")
        _validate_exact_commit(source["after"], "publication plan after")
    else:
        _validate_exact_commit(source["commit"], "publication plan commit")
    if not isinstance(plan["events"], list):
        raise ValidationError("publication plan events must be an array")
    if plan_kind in {PLAN_KIND_CURRENT_STATE_BOOTSTRAP, PLAN_KIND_CURRENT_STATE_RECONCILIATION}:
        failures = plan["record_failures"]
        if not isinstance(failures, list):
            raise ValidationError("current-state reconciliation record failures must be an array")
        for failure in failures:
            if not isinstance(failure, dict) or set(failure) != {"manifest_path", "error"}:
                raise ValidationError("current-state reconciliation record failure is invalid")
            if not all(isinstance(failure[key], str) and failure[key] for key in failure):
                raise ValidationError("current-state reconciliation record failure is invalid")


def signed_wrapper(event: dict[str, Any], secret: str) -> bytes:
    _validate_hmac_secret(secret)
    event_bytes = json.dumps(event, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
    payload = base64.urlsafe_b64encode(event_bytes).decode("ascii").rstrip("=")
    signature = hmac.new(secret.encode("utf-8"), payload.encode("ascii"), hashlib.sha256).hexdigest()
    return json.dumps(
        {"payload": payload, "signature": f"sha256={signature}"},
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")


def _validate_hmac_secret(secret: str) -> None:
    if len(secret.encode("utf-8")) < 32:
        raise ValidationError("Sheet HMAC secret must be at least 32 UTF-8 bytes")


def _publication_identity(event: dict[str, Any]) -> dict[str, Any]:
    record = event.get("record")
    return {
        "event_id": event.get("event_id"),
        "project_uuid": record.get("project_uuid") if isinstance(record, dict) else None,
        "revision": record.get("revision") if isinstance(record, dict) else None,
    }


def _publication_failure(event: dict[str, Any], code: str, error: str) -> dict[str, Any]:
    return {**_publication_identity(event), "ok": False, "code": code, "error": error}


def _publish_event(event: dict[str, Any], receiver_url: str, hmac_secret: str) -> dict[str, Any]:
    identity = _publication_identity(event)
    wrapper = signed_wrapper(event, hmac_secret)
    try:
        request = urllib.request.Request(
            receiver_url,
            data=wrapper,
            method="POST",
            headers={"Content-Type": "application/json"},
        )
    except ValueError:
        return _publication_failure(event, "transport_error", "Sheet receiver transport configuration is invalid")

    retry_delays = [2, 4, 8, 16]
    for attempt in range(len(retry_delays) + 1):
        try:
            with urllib.request.urlopen(
                request,
                timeout=RECEIVER_RESPONSE_TIMEOUT_SECONDS,
            ) as response:  # noqa: S310 - gated configured endpoint.
                if response.status < 200 or response.status >= 300:
                    return _publication_failure(event, "http_error", f"Sheet receiver returned HTTP {response.status}")
                response_bytes = response.read()
        except urllib.error.HTTPError as exc:
            return _publication_failure(event, "http_error", f"Sheet receiver returned HTTP {exc.code}")
        except urllib.error.URLError:
            return _publication_failure(event, "transport_error", "Sheet receiver transport failed (URLError)")
        except http.client.HTTPException as exc:
            return _publication_failure(
                event,
                "transport_error",
                f"Sheet receiver transport failed ({type(exc).__name__})",
            )
        except ValueError:
            return _publication_failure(event, "transport_error", "Sheet receiver transport configuration is invalid")
        except OSError as exc:
            return _publication_failure(
                event,
                "transport_error",
                f"Sheet receiver transport failed ({type(exc).__name__})",
            )

        try:
            result = json.loads(response_bytes.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError):
            return _publication_failure(event, "invalid_response", "Sheet receiver returned invalid JSON")

        acknowledged = (
            isinstance(result, dict)
            and result.get("ok") is True
            and result.get("event_id") == event["event_id"]
            and isinstance(result.get("changed"), bool)
        )
        if acknowledged:
            return {**identity, "ok": True, "changed": result["changed"]}

        rejected = isinstance(result, dict) and result.get("ok") is False
        code = result.get("code") if rejected and isinstance(result.get("code"), str) else None
        detail = result.get("error") if rejected and isinstance(result.get("error"), str) else None
        if code == "busy" and attempt < len(retry_delays):
            time.sleep(retry_delays[attempt])
            continue
        if rejected:
            return _publication_failure(event, code or "rejected", detail or "Sheet receiver rejected event")
        return _publication_failure(event, "invalid_acknowledgement", "Sheet receiver returned invalid acknowledgement")

    raise AssertionError("publication retry loop completed without a result")  # pragma: no cover


def publish_plan(
    plan: dict[str, Any],
    config: dict[str, Any],
    environment: dict[str, str] | None = None,
    acknowledgement_logger: Callable[[dict[str, Any]], None] | None = None,
    failure_logger: Callable[[dict[str, Any]], None] | None = None,
) -> int:
    """Publish individual signed envelopes. All gates fail before transport."""

    _validate_plan(plan, config)
    environment = os.environ if environment is None else environment
    if config["enabled"] is not True:
        raise ValidationError("Sheet publication is tracked-disabled; cutover has not occurred")
    if environment.get(config["cutover_environment_variable"]) != config["cutover_required_value"]:
        raise ValidationError(
            "Sheet publication cutover variable is not authorized in the authorized publication environment; "
            "local development hosts do not publish"
        )
    receiver_url = environment.get(config["receiver_url_environment_variable"])
    hmac_secret = environment.get(config["hmac_environment_variable"])
    if not receiver_url or not hmac_secret:
        raise ValidationError(
            "Sheet receiver URL and HMAC secret are required in the authorized GitHub Actions sheet-production "
            "environment; local development hosts do not publish"
        )
    _validate_hmac_secret(hmac_secret)

    outcomes: list[dict[str, Any]] = []
    for event in plan["events"]:
        outcomes.append(_publish_event(event, receiver_url, hmac_secret))

    for outcome in outcomes:
        if outcome["ok"] is True:
            if acknowledgement_logger is not None:
                acknowledgement_logger({key: value for key, value in outcome.items() if key != "ok"})
        elif failure_logger is not None:
            failure_logger({key: value for key, value in outcome.items() if key != "ok"})

    failures = [outcome for outcome in outcomes if outcome["ok"] is False]
    record_failures = plan.get("record_failures", [])
    if failures:
        details = "; ".join(
            f"{outcome['event_id']} ({outcome['project_uuid']} R{outcome['revision']}) "
            f"[{outcome['code']}]: {outcome['error']}"
            for outcome in failures
        )
        raise ValidationError(
            f"Sheet publication failed for {len(failures)} of {len(outcomes)} event(s): {details}"
        )
    if record_failures:
        details = "; ".join(
            f"{failure['manifest_path']}: {failure['error']}" for failure in record_failures
        )
        raise ValidationError(
            f"Sheet reconciliation could not safely prepare {len(record_failures)} record(s): {details}"
        )
    return len(outcomes)


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
    current_state_plan = subparsers.add_parser(
        "current-state-plan",
        help="prepare an explicit fresh-Sheet bootstrap plan from exact checked-out main",
    )
    current_state_plan.add_argument("--root", type=Path, default=Path("."))
    current_state_plan.add_argument("--commit", required=True)
    current_state_plan.add_argument("--repository", required=True)
    current_state_plan.add_argument("--ref", required=True)
    current_state_plan.add_argument("--output", type=Path, required=True)
    reconciliation_plan = subparsers.add_parser(
        "reconcile-current-state-plan",
        help="prepare a current-state reconciliation from fetched origin/main",
    )
    reconciliation_plan.add_argument("--root", type=Path, default=Path("."))
    reconciliation_plan.add_argument("--repository", required=True)
    reconciliation_plan.add_argument("--ref", required=True)
    reconciliation_plan.add_argument("--output", type=Path, required=True)
    publish = subparsers.add_parser(
        "publish",
        help="publish a prepared plan in the authorized GitHub Actions sheet-production environment",
    )
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
            print(f"Prepared {len(plan['events'])} incremental publication event(s); live publication remains separately gated.")
        elif args.command == "current-state-plan":
            plan = make_current_state_plan(root, args.commit, args.repository, args.ref, config)
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(json.dumps(plan, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
            print(
                f"Prepared {len(plan['events'])} current-state bootstrap event(s) from exact commit {args.commit}; "
                "live publication remains separately gated."
            )
        elif args.command == "reconcile-current-state-plan":
            plan = make_current_state_reconciliation_plan(root, args.repository, args.ref, config)
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(json.dumps(plan, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
            print(
                f"Prepared {len(plan['events'])} current-state reconciliation event(s) from exact commit "
                f"{plan['source']['commit']}; {len(plan['record_failures'])} record(s) could not be prepared."
            )
        elif args.command == "publish":
            plan = load_json(args.plan)
            count = publish_plan(
                plan,
                config,
                acknowledgement_logger=lambda acknowledgement: print(
                    "Receiver acknowledgement: "
                    + json.dumps(acknowledgement, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
                ),
                failure_logger=lambda failure: print(
                    "Receiver failure: "
                    + json.dumps(failure, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
                ),
            )
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
