# Testing

## Current gate

**READY TO TEST VERIFIED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Test only `artifacts/stacks-are-stacks-container-fixes-0.1.0-canary2.jar`, 16,921 bytes, SHA-256 `7C294CE614CCC6F889DDE7DB5B5BD474B74DC8B6A6C8E62FAEFD44E5811896F2`, embedded version `0.1.0-canary2`, source `382cb276455ffe74efc100ce65e853b499204b56`.

Test Instance Manager revision 84, accepted Stack v17, state digest `589256B4CDECA64B22933887B1C14DA37B9BBF4BBDB972FA9F8D91B6EC075C02`, Slot B deployment `66cd7696-3d01-4d92-805e-9221c1b93e07`, and artifact `01661d77-c5d5-4838-8485-1cb4309265ce` bind exact Canary 2 as `READY_TO_TEST_VERIFIED / UNTESTED`. Slot A remains exact CSR Canary 5 deployment `f12e434a-51c1-4e84-a487-3ebcadbe3d64` / artifact `c9551ad6-7a35-4a0e-ac25-af3efeccd86a`, independently `UNTESTED`. Accepted QSN C8 remains active from the baseline.

The verified external provider is `mods/StacksAreStacks-2.1.2-1.26.2.jar`, Fabric ID/version `stacksarestacks` / `2.1.2-1.26.2`, 358,573 bytes, SHA-256 `8E318394EA52A6DB343A00987DD1B122C69BF48E42DEB7813CC5EF293E655917`. Do not substitute it or alter its configuration.

Canary 1 is not the candidate. Its exact former deployment is canonically `FAIL`: `CLIENT_STARTED` invoked upstream `setStackSizes(null)` before every built-in item holder had bound components, producing the supplied uncaught render-thread `NullPointerException: Components not bound yet`. The launcher exit code does not change that result.

## Preflight and startup gate

Use only the dedicated Minecraft 26.2 Workbench. Never access the protected 26.1.2 gameplay profile. Before launching, run the manager's read-only `verify` command and require `PHYSICAL_STATE_VERIFIED`, revision 84, the exact state digest, both slot identities/results, Stack v17, and the exact external provider above. Stop on drift.

Startup is the first and mandatory runtime gate:

1. Launch to the title screen and confirm there is no `Components not bound yet`, mixin, linkage, or holder-alignment failure.
2. Confirm the log says registry synchronization completed, every item holder passed readiness, and the alignment completed exactly once for that binding epoch.
3. Enter a disposable world only after the startup gate passes.

## Focused SAS and CSR matrix

Use an eligible stack of three saddles unless the exact unchanged Stacks Are Stacks configuration excludes saddles; if it does, record that fact and use another eligible normally non-stackable item.

1. Confirm the actual and displayed count remains three in player inventory, an ordinary supported container, and an Easy Shulker Boxes preview.
2. Split, merge, drag, manually move, and vanilla `QUICK_MOVE` the stack; confirm no clamp, loss, duplication, or hidden count.
3. Create a CSR reservation from the stack, remove the physical stack, and confirm the translucent count-one identity ghost, literal zero, tooltip, and empty-reservation behavior remain correct.
4. Reinsert a matching stack by manual placement, vanilla `QUICK_MOVE`, hopper insertion where native sided rules permit it, and accepted QSN C8; confirm existing CSR affinity, admission, order, capacity, remainder, packet, and feedback behavior.
5. Confirm a matching occupied reservation accepts more items only up to the effective Stacks Are Stacks maximum.
6. Confirm a different item and a component-distinct saddle are rejected while count differences alone still match.
7. Confirm unsupported, player-inventory, hotbar, fake, inactive, result, and foreign slots remain unaffected.
8. Confirm vanilla-stackable controls, the CSR occupied marker, carried-shulker persistence, component preservation, Easy Shulker Boxes / Item Interactions overlays, and representative accepted Canary 4 storage/machine behavior remain unchanged.
9. Close and reopen the container; confirm the physical count, reservation, ghost/marker, and tooltip remain correct.
10. Disconnect and reconnect once. Confirm a fresh configuration epoch aligns once, the prior epoch is not reused, and the same stack/reservation checks remain correct.
11. Inspect the complete log through normal shutdown for duplicate alignment, overlap/rejection, partial mutation, count loss, serialization errors, or CSR/QSN regressions.

## Stopping and recording

Stop and record Canary 2 as `RUNTIME_FAIL` or `INCONCLUSIVE`, as supported by the actual observations, if startup fails; readiness does not pass; alignment is absent, duplicated within one epoch, or runs off the client thread; any count clamps, disappears, duplicates, or becomes visually hidden; matching/component-distinct reservation behavior is wrong; manual, `QUICK_MOVE`, hopper, or QSN insertion regresses; reconnect fails to realign; exact identities drift; or a patch-attributable error appears.

Judge CSR Canary 5's marker question independently even though the combined path is exercised. Record only behavior actually observed. Controlled tests, GameTests, deployment, and physical verification are not Minecraft runtime evidence; do not promote or mark either candidate passed without a complete applicable runtime pass.
