# Sheet synchronization foundation

The repository is authoritative and the Sheet is a UUID-keyed human mirror. `sheet_sync.py plan` prepares deterministic, secretless events only for `Resivore/Mynx-Flavoured-Workbench` on `refs/heads/main`. Every changed manifest revision produces an event even when its visible status is unchanged.

Live sending fails closed behind all of these gates: tracked `publication.json` is enabled, `MYNX_SHEET_CUTOVER=authorized`, the `sheet-production` GitHub environment permits the main branch, and receiver URL/HMAC secrets exist. The Apps Script receiver adds its own default-off `MYNX_STATUS_ACCEPT_WRITES` Script Property and exact source checks.

Events contain only repository-owned fields. `Notes` is the sole human-owned Sheet field. The receiver looks up rows by UUID, writes only its automation-column allowlist, and never writes `Notes`. Cutover must not enable these gates until UUID reconciliation is complete, secrets are installed, the user authorizes it, and the legacy writer is stopped.

Cutover reconciliation must create or match every participating UUID row and seed its exact current repository `Revision` before enabling writes; human fields remain untouched and `Event ID` may remain blank. That makes the next `revision + 1` event a normal recoverable update rather than an implicit bulk import.

The receiver serializes writes and commits `Revision` last. Publishers retry temporary lock contention and revision gaps; a failed run remains safely replayable with the same deterministic event IDs.
