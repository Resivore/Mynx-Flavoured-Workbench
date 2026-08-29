# Sheet synchronization foundation

The repository is authoritative and Mynx uses its own separate, initially empty Google Sheet as a UUID-keyed human mirror. `sheet_sync.py plan` prepares deterministic, secretless events only for `Resivore/Mynx-Flavoured-Workbench` on `refs/heads/main`. Every changed manifest revision produces an event even when its visible status is unchanged.

Live sending fails closed behind all of these gates: tracked `publication.json` is enabled, `MYNX_SHEET_CUTOVER=authorized`, the `sheet-production` GitHub environment permits the main branch, and receiver URL/HMAC secrets exist. The Apps Script receiver adds its own default-off `MYNX_STATUS_ACCEPT_WRITES` Script Property and exact source checks.

Events contain only repository-owned fields. `Notes` is the sole human-owned Sheet field, the receiver never writes it, and `Priority` is absent from the contract. The receiver looks up rows by UUID and writes only its automation-column allowlist. Live publication remains gated until the Mynx Sheet and secrets are configured and the user authorizes cutover. The legacy repository may continue writing to its different legacy Sheet.

No legacy-row reconciliation or manual revision seeding is required. An event for an unknown UUID creates the full row at its current positive authoritative repository `Revision`, whether that is R1, R5, R12, or higher. For an existing UUID, the same revision and Event ID is an idempotent no-op, a different Event ID at the same revision is rejected, only the exact next revision is accepted, and stale or skipped revisions are rejected.

The receiver serializes writes, flushes canonical fields first, writes `Event ID`, and commits `Revision` last. Publishers retry temporary lock contention and revision gaps; a failed run remains safely replayable with the same deterministic event IDs.
