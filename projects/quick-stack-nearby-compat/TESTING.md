# QSN C9 runtime procedure

**CONTROLLED VALIDATION PASS — READY_TO_TEST_VERIFIED — RUNTIME INCONCLUSIVE**

User reported only: "Nested shulker routing using an internal CSR reservation worked." No other C9 matrix behavior was reported. This partial success does not justify promotion; remaining cases below still require observation.

Exact candidate: `quick-stack-nearby-compat-0.1.0-canary9.jar`, 46607 bytes, SHA-256 `14a6844b04d9236233c7007ed111a452db659cc5662dca07aa11f33b2cb4ec76`, source `a333a4b5fa417cd9d35f6fc2608727828c23f6ea`. Verified Slot B; deployment `03f7540f-94f0-47c5-b6bd-94f2af232ff4`, artifact `8b8abdf9-26a4-407c-bbd6-1623f0a5eeb5`. Verify canonical manager readiness before the user launches the dedicated 26.2 Workbench.

1. Put a count-one shulker with a matching physical stack in a nearby chest. QSN must merge internally, then use legal empties; reconcile source, destination and remainder counts.
2. Test a physically empty shulker with only a matching CSR reservation. Its nested slot qualifies but the outer chest must not receive a nested-only key. An unrelated or component-mismatched reservation establishes no affinity.
3. Fill a matching physical slot partially; reserve a later empty slot and leave an earlier ordinary empty. Verify physical merge, reserved empties in index order, then ordinary empties. A full physical match still establishes affinity. Conflicting occupied reservations cannot grow.
4. Test parent physical affinity with several nested hosts and a farther parent. The native parent goes first, followed by its children in physical slot order, then the next parent.
5. Check vanilla/copper double chests and accepted Double Barrels. Each physical host is touched once; reopen both halves and confirm persistent writeback and viewer synchronization.
6. Empty unreserved, multi-count, component-mismatched and unsupported hosts must not attract items. Deny shulker nesting, output-only hosts, locked/inaccessible/unloaded/out-of-range or stale/replaced parents without loss or duplication.
7. Preserve host CSR reservations, custom name, lore, CCAR lock and unrelated components. Removing physical contents reveals the same reservation. Check effective Stacks Are Stacks maximums and exact remainder on full targets.
8. Check source locks/keep counts, Inventory Extended rows, top-level CSR priorities, CNM ShapeMap, shelf exclusion, Inventory Search and unchanged QSN button/packet/feedback behavior.
9. Inspect the full log; stop on startup errors, ordering changes, duplicate host writes, stale edits, unauthorized destinations, lost components, sync failures or any count discrepancy. Record only observed rows and exact C9 identity; CCAR C12 and CSR C9 results stay independent.

Controlled evidence: Retained exact QSN C9 passed 60 JUnit tests and 26 required GameTests against new CSR C9 and accepted Double Barrels, with QSN production compilation/resources/JAR tasks disabled. No QSN production or artifact changed. Earlier provider matrix and API-floor evidence remains recorded in CODEX_LOG.md; controlled tests do not extend the supplied runtime observation.

Manager revision 96 / accepted Stack v19; deployed 2026-09-05T06:07:58Z; state SHA-256 2473241803c3f355c8138968da9845d921e46c4643a2f3e0ba64f0ce9ce20e25; physical inventory SHA-256 493c01f74dea121be5832dbad05368d96bfa4e53626e3daeb17a2c2b85256cc6. All 38 managed files physically verified. One atomic DEPLOY_PROFILE added CSR C9 and removed CSR C8 only; Slot B including its result and timestamps, accepted baseline, companions, rollback bytes and all unrelated deployments are unchanged. QSN C9 remains independently INCONCLUSIVE; current CSR C9 in Slot A remains UNTESTED. No Minecraft client was launched.
