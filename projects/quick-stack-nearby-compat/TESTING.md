# QSN C11 runtime procedure — not currently deployed

**CONTROLLED_VALIDATION_PASS — NOT_DEPLOYED — RUNTIME_UNTESTED**

Exact candidate: `quick-stack-nearby-0.4.0-workbench-canary11.jar`, 208,035 bytes, SHA-256 `47f0931026f28c179bc0f4b7ae461ab4f81413a9eea37fdaf701871bea1d0d9b`, source `0699c64e153acab1331627714d04f9ba6c0e8fd7`. Assign C11 through a verified Test Instance Manager transition before recording gameplay evidence. C8 remains accepted. C10 is historical `RUNTIME_FAIL`; retained C9 remains not deployed and inconclusive with its exact historical observation unchanged.

1. Retest the exact C9-proven case: an otherwise-empty count-one nested shulker with only a matching CSR reservation. A matching player item must route into that nested slot; mismatched components must remain protected. Repeat with a physical matching nested stack.
2. Verify parent-first ordering when both outer container and nested shulker match; when only the child matches, confirm the physical parent receives nothing. Verify physical → matching reserved → ordinary empty nested placement, custom host metadata and unrelated nested contents, and no loss/duplication. Exercise stale, replaced, recursive, and multicount hosts; each must fail closed.
3. Verify normal QSN routing, Inventory Extended rows/rules/locks/keep counts, hotbar/equipment exclusion, CNM ShapeMap affinity, shelf exclusion, capacity/remainders, connected Double Barrels, and host deduplication.
4. Open the actual Workbench player inventory with Inventory Extended enabled. Confirm the existing Quick Stack button and its right-click rules behavior remain available. Confirm a distinct C11 Nearby Search button is visible, clickable, collision-free with installed controls and recipe-book layout, and has no Inventory Search/Core dependency.
5. Open the C11 Nearby Search modal. Check live-only search results, counts, locations, nested labels, filtering, targeting/camera behavior, stale-target rejection, and that normal search/modal networking remains server-authoritative.
6. Record only directly observed C11 rows and any startup or UI issue. Controlled JUnit, GameTests, and the isolated development-client startup do not constitute runtime PASS; do not promote without independent Minecraft runtime PASS.
