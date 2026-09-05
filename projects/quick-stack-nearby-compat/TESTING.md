# QSN C9 runtime procedure

**CONTROLLED VALIDATION PASS - RUNTIME UNTESTED**

Exact candidate: `quick-stack-nearby-compat-0.1.0-canary9.jar`, 46607 bytes, SHA-256 `14a6844b04d9236233c7007ed111a452db659cc5662dca07aa11f33b2cb4ec76`, source `a333a4b5fa417cd9d35f6fc2608727828c23f6ea`. Verified Slot B; deployment `03f7540f-94f0-47c5-b6bd-94f2af232ff4`, artifact `8b8abdf9-26a4-407c-bbd6-1623f0a5eeb5`. Verify canonical manager readiness before the user launches the dedicated 26.2 Workbench.

1. Put a count-one shulker with a matching physical stack in a nearby chest. QSN must merge internally, then use legal empties; reconcile source, destination and remainder counts.
2. Test a physically empty shulker with only a matching CSR reservation. Its nested slot qualifies but the outer chest must not receive a nested-only key. An unrelated or component-mismatched reservation establishes no affinity.
3. Fill a matching physical slot partially; reserve a later empty slot and leave an earlier ordinary empty. Verify physical merge, reserved empties in index order, then ordinary empties. A full physical match still establishes affinity. Conflicting occupied reservations cannot grow.
4. Test parent physical affinity with several nested hosts and a farther parent. The native parent goes first, followed by its children in physical slot order, then the next parent.
5. Check vanilla/copper double chests and accepted Double Barrels. Each physical host is touched once; reopen both halves and confirm persistent writeback and viewer synchronization.
6. Empty unreserved, multi-count, component-mismatched and unsupported hosts must not attract items. Deny shulker nesting, output-only hosts, locked/inaccessible/unloaded/out-of-range or stale/replaced parents without loss or duplication.
7. Preserve host CSR reservations, custom name, lore, CCAR lock and unrelated components. Removing physical contents reveals the same reservation. Check effective Stacks Are Stacks maximums and exact remainder on full targets.
8. Check source locks/keep counts, Inventory Extended rows, top-level CSR priorities, CNM ShapeMap, shelf exclusion, Inventory Search and unchanged QSN button/packet/feedback behavior.
9. Inspect the full log; stop on startup errors, ordering changes, duplicate host writes, stale edits, unauthorized destinations, lost components, sync failures or any count discrepancy. Record only observed rows and exact C9 identity; CCAR C12 and CSR C8 results stay independent.

Controlled evidence: 60 JUnit tests; 26 GameTests in final clean build with exact CSR C8 and accepted Double Barrels. Earlier provider matrix passed 25 GameTests with accepted CSR C4 and 14 with CSR absent; connected-barrel case is inactive when provider absent. Exact API-floor audit, existing top-level contracts and optional-class isolation passed. No user runtime evidence.

Deployment verification: Manager revision 93, accepted Stack 19, timestamp 2026-09-05T04:27:20Z; state SHA-256 6a9e9cd01e01b210b239d50416483cd4eb426c3b57a57f25a3f9f914f39eb2e6; physical inventory SHA-256 d50a36599630947d42b95bf9e2a185acad04709f4705a32359deed8e8b274639. One atomic DEPLOY_PROFILE replaced C7 in A and placed C9 in B. Both exact current releases are READY_TO_TEST_VERIFIED with independent UNTESTED results. Accepted CCAR C12, SAS C2, Stacks Are Stacks, Offhand Shift-Click QoL, Double Barrels and all unrelated accepted units remain unchanged. Exact upstream QSN 0.4.0 remains accepted passthrough; accepted C8 is disabled as the reversible predecessor. No dedicated client was launched.
