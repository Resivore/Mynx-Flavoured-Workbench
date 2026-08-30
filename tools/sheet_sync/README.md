# Sheet synchronization

The repository is authoritative and Mynx uses its own separate Google Sheet as a UUID-keyed human mirror. Tracked publication is enabled, but `sheet_sync.py` still fails closed unless every source, cutover, environment, secret, signature, and receiver gate passes.

## Pending infrastructure repair

The Workbench V2 migration is complete and `ACTIVE`; its physical and runtime
authority is unaffected by this mirror-only blocker. From authoritative main
checkpoint `d0479458c9ac3c49db825be7a25746f554976922`, `Publish Project Status`
[run 33285532021](https://github.com/Resivore/Mynx-Flavoured-Workbench/actions/runs/33285532021)
successfully prepared the two ordered incremental R2 events, but its publish job
failed at `2026-08-30T01:23:45Z` with:

```text
Sheet receiver rejected event 594da0f70b90c1d7d7d95a92587710b76f33020ee0066e00e431f6a025398330: stale or skipped Sheet revision
```

That unacknowledged event is Matcha Heart / Death Compatibility UUID
`937d7ccc-44c9-55cb-8d33-0dc0bff5fe45`, revision 2. Because publication is
ordered and stopped at that rejection, Mossy Stone UUID
`9f1c5aa4-09c1-4de3-9921-4b045e8abcd2`, revision 2, event
`96f7689c6251cda8f14dc4cd7e7567e8783ee08f938d045332b70df440b4d23e`
was not attempted or acknowledged.

This remains a separate Sheet publisher/receiver infrastructure repair. Do not
work around it by changing either project revision, fabricating an
acknowledgement, skipping revisions, editing Sheet rows manually, overwriting
`Notes`, promoting either candidate, or creating C8/C3. A bounded follow-up must
repair the publication path and obtain real acknowledgements for the exact
pending main-authored events while preserving `Notes`.

## Incremental publication

`sheet_sync.py plan` prepares deterministic, secretless incremental events only for `Resivore/Mynx-Flavoured-Workbench` on `refs/heads/main`. It validates the normal push transition between exact `--before` and `--after` commits. Every changed participating manifest revision produces an event even when its visible status is unchanged. A manifest that stops participating receives its final ordinary update; a project task cannot bypass the revision and log protocol.

An intentional removal is valid only when the complete previously controlled project or resource-pack directory disappears. Incremental planning emits no event for that removed manifest because the production receiver remains upsert-only; any existing Sheet row must be removed manually or by rebuilding the Sheet. A later current-state bootstrap cannot recreate the row because it enumerates only manifests that still exist.

The normal main-push workflow uses this mode. It remains strict and is not a current-state scan.

## Current-state bootstrap

`sheet_sync.py current-state-plan` is the explicit recovery path for a fresh or recreated Sheet. It:

1. accepts only the configured repository and `refs/heads/main`;
2. requires the supplied `--commit` to be a lowercase full commit that exactly matches both checked-out `HEAD` and the configured main ref;
3. validates the repository before preparing publication;
4. reads every canonical `projects/*/WORKBENCH_STATUS.json` and `resourcepacks/*/WORKBENCH_STATUS.json` directly from that commit's Git tree;
5. includes only manifests whose canonical Sheet participation flag is true;
6. preserves each manifest's exact current revision and state; and
7. emits the same deterministic `project_status_upsert` receiver envelopes as normal publication without writing any manifest or log.

Example plan preparation on an exact main checkout:

```powershell
python tools/sheet_sync.py current-state-plan `
  --root . `
  --commit $env:GITHUB_SHA `
  --repository Resivore/Mynx-Flavoured-Workbench `
  --ref refs/heads/main `
  --output status-publication-plan.json
```

Plans carry `plan_kind`: `incremental` for normal pushes and `current_state_bootstrap` for a current-state scan. That distinction exists only in the local plan wrapper; bootstrap events do not change the receiver envelope contract. Publishing either kind uses the same `publish` command, signing, retry behavior, and gates.

A current-state bootstrap is intentionally not a reset mechanism. Use it only for a fresh/recreated Sheet, or replay the same plan from the same exact commit for an idempotency check. On an existing Sheet, the receiver rejects stale revisions, gaps, and same-revision event conflicts. It accepts an exact event replay as a no-op, so a safe repeat cannot create duplicate rows.

## Gates, ownership, and acknowledgements

Live sending requires tracked `publication.json` to be enabled, `MYNX_SHEET_CUTOVER=authorized`, the `sheet-production` GitHub environment to permit the main branch, and receiver URL/HMAC secrets to exist. The Apps Script receiver adds its own default-off `MYNX_STATUS_ACCEPT_WRITES` Script Property and exact source checks. Feature branches cannot prepare an authoritative plan.

Events contain only repository-owned fields. `Notes` is the sole human-owned Sheet field, the receiver never writes it, and `Priority` is absent from the contract. The receiver looks up rows by UUID and writes only its automation-column allowlist. Live publication remains gated until the Mynx Sheet and secrets are configured and the user authorizes cutover. The legacy repository may continue writing to its different legacy Sheet.

No legacy-row reconciliation or manual revision seeding is required. An event for an unknown UUID creates the full row at its current positive authoritative repository `Revision`, whether that is R1, R5, R12, or higher. For an existing UUID, the same revision and Event ID is an idempotent no-op, a different Event ID at the same revision is rejected, only the exact next revision is accepted, and stale or skipped revisions are rejected.

The receiver serializes writes, flushes canonical fields first, writes `Event ID`, and commits `Revision` last. Publishers retry temporary lock contention and revision gaps with the same signed event; a failed run remains safely replayable with the same deterministic event IDs. CLI publication logs emit one structured receiver acknowledgement per accepted event, including acknowledged `event_id`, project UUID, revision, and the receiver's boolean `changed` state. This makes the initial delivery and an exact-commit no-op replay auditable without exposing secrets; a success response that omits a boolean `changed` value is rejected.
