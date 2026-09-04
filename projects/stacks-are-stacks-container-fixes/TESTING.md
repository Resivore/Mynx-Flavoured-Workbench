# Testing

## Accepted baseline

**ACCEPTED — AGGREGATE RUNTIME PASS — RETAIN FOR REGRESSION**

The accepted release is `artifacts/stacks-are-stacks-container-fixes-0.1.0-canary2.jar`, embedded version `0.1.0-canary2`, 16,921 bytes, SHA-256 `7C294CE614CCC6F889DDE7DB5B5BD474B74DC8B6A6C8E62FAEFD44E5811896F2`, source `382cb276455ffe74efc100ce65e853b499204b56`.

Its exact external provider is `StacksAreStacks-2.1.2-1.26.2.jar`, Fabric ID/version `stacksarestacks` / `2.1.2-1.26.2`, 358,573 bytes, SHA-256 `8E318394EA52A6DB343A00987DD1B122C69BF48E42DEB7813CC5EF293E655917`. Do not substitute another provider version or alter its configuration while reproducing the accepted result.

At `2026-09-04T20:15:03Z`, manager revision 85 recorded the user's aggregate statement that CSR × SAS is passing only for Canary 2 deployment `66cd7696-3d01-4d92-805e-9221c1b93e07` / artifact `01661d77-c5d5-4838-8485-1cb4309265ce` with that exact upstream provider. No individual checklist row and no unrelated CSR behavior is claimed. Manager revision 87 at `2026-09-04T20:16:38Z` promoted the unchanged patch, cleared Slot B, and produced accepted Stack v18 with state digest `954271FD5113BA0E330203A042B94DA4FA2EFB7F6D9AEB3063F861F5C10DD3AA`.

After the independent CSR successor deployment, final manager revision 88 is `PHYSICAL_STATE_VERIFIED`, accepted Stack v18, with empty Slot B, state digest `DEE397C4FFEBACABF41028A0FAFB22D674EE695A937BA22F9A6122771577C9A4`, and physical inventory digest `C6FD9345DD5DA38D5FD07ED46628D09BAF0C3B6B2014C3BE24A1848647A73F2`. Canary 2 is supplied by the accepted baseline, not an experimental slot.

Canary 1 remains a failed historical predecessor, not a regression candidate. Its `CLIENT_STARTED` hook invoked the upstream alignment before all built-in item components were bound and produced the supplied uncaught render-thread `NullPointerException: Components not bound yet` before the title screen.

## Regression preflight

Use only the dedicated Minecraft 26.2 Workbench. Never access the protected 26.1.2 gameplay profile. Before a regression run, use the Test Instance Manager's read-only verification and require the current canonical revision and digest to match the physical profile, accepted Canary 2 to remain the sole `stacksarestacks_container_fixes` provider, and the exact upstream provider above to resolve its hard dependency. Stop on identity, dependency, or physical-state drift.

When testing a new CSR release against accepted SAS Canary 2, record the new observation for that CSR release unless the evidence establishes a patch-specific SAS regression. The historical aggregate PASS must not be transferred to another SAS or CSR version.

## Current regression procedure

1. Launch to the title screen and confirm there is no `Components not bound yet`, mixin, linkage, or holder-alignment failure. Confirm registry synchronization completes, every item holder passes readiness, and Canary 2 aligns exactly once for the binding epoch.
2. Use an eligible stack of three saddles unless the unchanged Stacks Are Stacks configuration excludes saddles; if so, record that fact and use another eligible normally non-stackable item.
3. Confirm the actual and displayed count remains three in player inventory, an ordinary container, and an Easy Shulker Boxes preview. Split, merge, drag, manually move, and vanilla `QUICK_MOVE` the stack, checking for clamp, loss, duplication, or a hidden count.
4. With the current CSR candidate present, create a reservation from the stack, remove it, and check the ghost, literal zero, tooltip, and occupied marker. Reinsert an exact match through representative manual, `QUICK_MOVE`, hopper, QSN, and supported carried-shulker paths; confirm a different item and a component-distinct stack are rejected, count differences still match, unreserved fallback remains available, and effective Stacks Are Stacks capacity is preserved.
5. Close and reopen the container, then disconnect and reconnect once. Confirm the physical count and reservation survive, a fresh configuration epoch aligns exactly once, and no prior epoch is reused.
6. Check vanilla-stackable controls and inspect the complete log through normal shutdown for duplicate alignment, overlap, partial mutation, count loss, serialization errors, or CSR/QSN regressions.

## Stopping and recording

Stop and record only the behavior actually observed if startup or readiness fails; alignment is absent, duplicated in one epoch, or off-thread; any count clamps, disappears, duplicates, or becomes hidden; reservation matching, admission, capacity, or persistence regresses; reconnect fails; exact identities drift; or a patch-attributable error appears.

Do not replace Canary 2's accepted aggregate result with inferred row-level results. Controlled tests, GameTests, accepted-baseline membership, and physical verification are not additional Minecraft runtime evidence.
