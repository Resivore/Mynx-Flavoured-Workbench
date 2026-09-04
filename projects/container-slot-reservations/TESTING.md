# Testing

## Current gate

**READY TO TEST VERIFIED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Test only unchanged `artifacts/container-slot-reservations-0.1.0-canary5.jar`, 112,541 bytes, SHA-256 `D228EF4ECE4893A6538A845B34C1684A72FE8659790DC63AA12AB608AA886B8E`, embedded version `0.1.0-canary5`, source `793a29fa0cac2f505e6fcf373c56b76e16eca568`.

Test Instance Manager revision 84, accepted Stack v17, state digest `589256B4CDECA64B22933887B1C14DA37B9BBF4BBDB972FA9F8D91B6EC075C02`, Slot A deployment `f12e434a-51c1-4e84-a487-3ebcadbe3d64`, and artifact `c9551ad6-7a35-4a0e-ac25-af3efeccd86a` preserve the exact original `READY_TO_TEST_VERIFIED / UNTESTED` C5 candidate and its original readiness timestamps. Slot B is SAS patch Canary 2 deployment `66cd7696-3d01-4d92-805e-9221c1b93e07` / artifact `01661d77-c5d5-4838-8485-1cb4309265ce`, independently `UNTESTED`. Accepted QSN C8 remains baseline-active.

C5 still differs from accepted C4 only by the exact 103-byte, 3×3, 8-bit RGBA occupied-reservation marker at `itemX + 13, itemY`, SHA-256 `5A9FE986E6AED154D8E1302C1A8F066D76A2AFF10AA4F6FC9FD973D0E5BCAACE`. No CSR production byte changed for Stacks Are Stacks compatibility. Canary 2 supplies the corrected client holder default before play materialization; CSR retains format 1, count-one `ItemStackTemplate` identity, complete component matching, effective-maximum classification, and its existing public API.

## Preflight and startup gate

Use only the dedicated Minecraft 26.2 Workbench. Never access the protected 26.1.2 gameplay profile. Before launching, run the manager's read-only `verify` command and require `PHYSICAL_STATE_VERIFIED`, revision 84, the exact state digest, both slot identities and independent `UNTESTED` results, Stack v17, accepted QSN C8, and exact external `stacksarestacks` 2.1.2-1.26.2 at SHA-256 `8E318394EA52A6DB343A00987DD1B122C69BF48E42DEB7813CC5EF293E655917`. Stop on drift.

Startup is the first and mandatory gate:

1. Launch to the title screen without `Components not bound yet`, mixin/linkage errors, or another startup failure.
2. Confirm the log reports a complete all-item holder readiness pass and one completed SAS client alignment for the configuration epoch.
3. Enter a disposable world only after startup passes. A Slot B startup failure is not a CSR C5 failure; record each candidate independently.

## Focused C5 and shared SAS matrix

1. Create occupied reservations in representative ordinary storage and machine menus. Confirm the old teal square is absent and the exact new 3×3 marker appears at the unchanged top-right position with transparent bottom corners, without affecting the item sprite, count, empty ghost, literal zero, tooltip, or later GUI rendering.
2. Confirm representative accepted C4 behavior across ordinary storage, furnaces/machines, copper chests, Double Barrels, Ender Chest, carried shulkers, component preservation, Easy Shulker Boxes / Item Interactions overlays, manual insertion, vanilla `QUICK_MOVE`, hopper admission, and accepted QSN C8.
3. Use an eligible count-three saddle unless the exact Stacks Are Stacks configuration excludes it. Confirm count three remains actual and visible in player inventory, a supported container, and an Easy Shulker Boxes preview through split, merge, move, and `QUICK_MOVE`.
4. Create a reservation from that saddle, remove the physical stack, and confirm the translucent ghost, literal zero, tooltip, count-one template identity, and component-exact match remain correct.
5. Reinsert a matching stack manually, by vanilla `QUICK_MOVE`, by hopper where native sided rules permit, and through accepted QSN C8. Confirm existing reservation affinity/admission, native ordering, capacity, remainders, packets, and feedback.
6. Confirm a matching occupied reservation accepts additional saddles only up to the effective aligned Stacks Are Stacks maximum.
7. Confirm a different item and a component-distinct saddle are rejected while a different count of the same exact item/components still matches.
8. Confirm unsupported, player-inventory, hotbar, fake, inactive, result, foreign, and reservation-free slots remain unaffected, and vanilla-stackable controls behave exactly as before.
9. Close and reopen the container, then disconnect and reconnect once. Confirm physical counts, reservation state, marker/ghost/tooltip, and insertion behavior persist; confirm the fresh connection aligns one new binding epoch without a duplicate for either epoch.
10. Inspect the complete log through normal shutdown for holder, template, snapshot, menu, rendering, insertion, QSN, count-loss, duplicate-alignment, or linkage errors.

## Stopping and recording

Judge C5 independently from Canary 2. Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE`, as supported by the actual observations, if exact identity/readiness drifts; the C5 marker differs in image, position, shape, scale, sampling, or transparency; accepted C4 behavior regresses; count-one/component-exact reservation semantics or effective maximums are wrong; counts clamp, disappear, duplicate, or become visually hidden; matching or mismatching admission fails; reopen/reconnect regresses; or a CSR-attributable error appears.

Record only behavior actually observed. The repeated JUnit/GameTest suites, QSN controlled suite, deployment, and physical verification establish readiness only; they do not constitute a CSR or SAS Minecraft runtime pass and do not authorize promotion.
