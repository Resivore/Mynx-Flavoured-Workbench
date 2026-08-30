# Sheet synchronization

The repository is authoritative and Mynx uses a separate Google Sheet as a UUID-keyed human mirror. A missed, delayed, or failed Sheet delivery may leave that mirror stale, but it does not invalidate current `main` and must not be repaired by changing project revisions. Tracked publication remains fail-closed unless every repository, ref, cutover, environment, secret, signature, receiver, and write gate passes.

## Receiver convergence contract

Projects are looked up only by immutable project UUID. The receiver applies these rules to an existing UUID row:

- an incoming revision lower than the stored revision is rejected as stale;
- an incoming revision equal to the stored revision acknowledges an exact replay or the same authoritative project content, but rejects conflicting machine-owned content;
- an incoming revision higher than the stored revision is accepted directly, without requiring the Sheet to have received every intermediate revision.

An unknown UUID is inserted at any positive authoritative revision. A blank revision left by an interrupted first write is treated as having no committed revision, so a later current-state reconciliation can complete it. Event ID and Publication Commit are delivery metadata: a current-state event from a later main commit may differ in those fields while representing unchanged same-revision project content. That case is idempotent, not a conflict. Any other same-revision machine-content difference remains a conflict.

`Notes` is the sole human-owned field. Events cannot contain it, same-revision comparisons ignore it, and receiver writes target only the explicit machine-owned allowlist. `Priority` is not part of the contract. The receiver validates source authority, envelope shape, HMAC signature, and its write gate before acquiring one script lock for the read/compare/write operation.

## Original failure evidence

Heart R2 event `594da0f70b90c1d7d7d95a92587710b76f33020ee0066e00e431f6a025398330` was rejected in run `33285532021` as `stale or skipped Sheet revision`; Mossy R2 was never attempted. Heart R3 event `aaf1de5449df66010b7fa9216ac90337fe577a1e1187bddab6a081ccaae57505` was rejected the same way in run `33290538641`; deterministic plan ordering and fail-fast publication again prevented Mossy R3 event `dba90156bafe67477bdce4929d2aa96ba76ddcc4d5555254937d065801bd2913` from being attempted.

The old receiver converted the existing row's Revision cell to a number and accepted unequal revisions only when `incoming == stored + 1`. Heart R2 would have succeeded from stored R1, while Heart R3 would have succeeded from stored R2. The rejections therefore prove only that the receiver did not see the required predecessor at either attempt; its response did not disclose the raw stored value and cannot by itself distinguish a behind, blank, invalid, or unexpectedly ahead cell. A blank interrupted or pre-existing row, interpreted as revision zero, is consistent with the history but is not proven. The infrastructure root cause is the receiver's strict adjacency requirement, not a project-state defect, and no manual revision change is needed under the convergence contract.

## Incremental publication

`sheet_sync.py plan` prepares deterministic, secretless incremental events only for `Resivore/Mynx-Flavoured-Workbench` on `refs/heads/main`. It validates the normal push transition between exact `--before` and `--after` commits. The repository contract still requires each project task to advance its manifest revision exactly once; the relaxed rule applies only to what the Sheet must already have observed.

Every changed participating manifest revision produces an event even when its visible status is unchanged. A manifest that stops participating receives its final ordinary update; a project task cannot bypass the revision and log protocol.

An intentional removal is valid only when the complete previously controlled project or resource-pack directory disappears. Incremental planning emits no event for that removed manifest because the production receiver remains upsert-only; any existing Sheet row must be removed manually or by rebuilding the Sheet. A later current-state bootstrap cannot recreate the row because it enumerates only manifests that still exist.

The normal main-push workflow uses this mode. Publication attempts events sequentially in deterministic plan order but treats their outcomes independently: a permanent failure for one UUID does not prevent later events from being attempted. Busy lock responses alone are retried with the same signed request. The CLI reports a structured result for every event and fails the overall command after all attempts if any result failed.

## Current-state reconciliation

`sheet_sync.py current-state-plan` is the explicit recovery path for a fresh, recreated, or stale Sheet. It:

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

Plans carry `plan_kind`: `incremental` for normal pushes and `current_state_bootstrap` for a current-state scan. The GitHub workflow dispatch operation remains named `bootstrap-current-state` for compatibility, but it is also the normal reconciliation operation for an existing stale mirror. The distinction exists only in the local plan wrapper; current-state events use the same receiver envelope and publication path as incremental events.

A current-state reconciliation is not a reset mechanism. Lower incoming revisions remain stale, same-revision conflicts still fail, and higher current-main revisions converge directly. Exact and semantically identical replays are idempotent, so a safe repeat cannot create duplicate rows. The plan does not mutate project manifests or logs.

## Gates, ownership, and acknowledgements

Live sending requires tracked `publication.json` to be enabled, `MYNX_SHEET_CUTOVER=authorized`, the `sheet-production` GitHub environment to permit the main branch, and receiver URL/HMAC secrets to exist. The Apps Script receiver adds its own default-off `MYNX_STATUS_ACCEPT_WRITES` Script Property and exact source checks. Feature branches cannot prepare an authoritative plan.

Events contain only repository-owned fields. `Notes` is the sole human-owned Sheet field, the receiver never writes it, and `Priority` is absent from the contract. The receiver looks up rows by UUID and writes only its automation-column allowlist. Live publication remains gated until the Mynx Sheet and secrets are configured and the user authorizes cutover. The legacy repository may continue writing to its different legacy Sheet.

No legacy-row reconciliation or manual revision seeding is required. An event for an unknown UUID creates the full row at its current positive authoritative repository `Revision`, whether that is R1, R5, R12, or higher. An existing UUID rejects a lower revision, accepts any higher revision, and distinguishes idempotent same-revision content from a true conflict.

The receiver serializes writes, flushes canonical fields first, writes `Event ID`, and commits `Revision` last. Publishers retry temporary lock contention with the same signed event; stale and conflicting writes are permanent failures. CLI publication logs identify every event by Event ID, project UUID, and revision, including the receiver's boolean `changed` value on success, without exposing secrets.

## Production receiver deployment and recovery

The repository contains the receiver source and manifest but no Apps Script project ID, `.clasp.json`, deployment credential, or deployment workflow. A receiver-code change therefore requires an authorized external update of the existing Apps Script project and a new version of its existing web-app deployment. Preserve its Script Properties, URL, HMAC secret, spreadsheet ID, sheet name, and write gate.

After deploying the repository-backed receiver, dispatch `Publish Project Status` with `bootstrap-current-state` from current authoritative `main`. Before the dispatch, record the exact Heart and Mossy Notes cells. Completion requires successful structured acknowledgements for both UUIDs at R3, Sheet Revision values matching their canonical `WORKBENCH_STATUS.json` files, and byte-for-byte unchanged Notes cells. Do not seed R1/R2 events or edit Sheet/project revisions to make reconciliation pass.
