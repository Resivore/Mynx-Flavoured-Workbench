# QSN C10 future runtime procedure — not currently deployed

**CONTROLLED_VALIDATION_PASS — NOT_DEPLOYED — RUNTIME_UNTESTED**

Exact candidate: `quick-stack-nearby-0.4.0-workbench-canary10.jar`, 201,677 bytes, SHA-256 `7882491e0d5f0a3bd8f8ab5135ebb900f16f4caa520a693f682c61d09f092dfe`, source `6c121ec5bdbb66f4c8092289d2f665a306fdb290`. Do not run this procedure or record runtime evidence unless C10 is first assigned through a verified Test Instance Manager slot transition. C8 remains accepted; retained C9 remains not deployed and inconclusive.

1. Verify normal QSN quick stack, Inventory Extended rows and matching rule range, source locks/keep counts, hotbar/equipment exclusion, CSR reservation-only targets and physical → reserved → ordinary empty ordering. Confirm no loss or duplication.
2. Verify CNM ShapeMap affinity, vanilla shelf exclusion, Stacks Are Stacks capacity/remainders, single/double chests, and connected Double Barrels without duplicate writes.
3. Put physical and CSR-only matching contents into a count-one shulker in a nearby container. Verify parent-first ordering, exact component identity, transactional host writeback, custom host metadata preservation, and rejection of stale/replaced/recursive hosts.
4. Open an Inventory screen. Confirm the QSN quick-stack/rules controls remain available and the separate Nearby Search button neither overlaps them nor uses Inventory Search.
5. In Nearby Search, verify the intentional Inventory Search-inspired modal proportions, search field, icon, count column, expandable locations, scroll controls, close control, display-name/ID/category/component filtering, and only live nearby storage contents. Check actual counts, multiple locations, nearest ordering, nested shulker labels and outer world positions.
6. Select a main row and an explicit expanded location. The modal must close only after server validation; the player must not move and the camera must aim at the correct physical outer container. Stale, inaccessible, out-of-range, unloaded, empty-reservation, and removed-result selections must fail safely without camera rotation. No historical catalogue, last-seen data, or remembered out-of-range result may appear.
7. Stop and record only directly observed rows, exact release identity, and any count discrepancy or startup/error evidence. Do not infer C10 runtime PASS from controlled JUnit/GameTests.
