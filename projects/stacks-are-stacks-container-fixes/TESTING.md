# Testing

## Current gate

**READY TO TEST VERIFIED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Test only unchanged `artifacts/stacks-are-stacks-container-fixes-0.1.0-canary1.jar`, 3,677 bytes, SHA-256 `AB3634C31C2F3B231908CF392ED5ADA074E624E3DFBA45B5707D8B38958E1EED`, embedded version `0.1.0-canary1`, source `625143806c44d6e5dc6725282edbfea0fafd8f53`.

Test Instance Manager revision 82, accepted Stack v17, state digest `19C42A70EF995A671B9A6D063AD61A927F0BD8A22617722FEF103F862F971577`, Slot B deployment `f58e3578-1149-4373-a32f-c6b3e1c9ceea`, and artifact `4eff5256-9775-4e5e-b459-ec40188849a5` bind the exact unchanged candidate. Its deployment is `READY_TO_TEST_VERIFIED`; its independent result is `UNTESTED`.

The verified hard dependency is the unmanaged external enabled provider `mods/StacksAreStacks-2.1.2-1.26.2.jar`, Fabric ID `stacksarestacks`, version `2.1.2-1.26.2`, 358,573 bytes, SHA-256 `8E318394EA52A6DB343A00987DD1B122C69BF48E42DEB7813CC5EF293E655917`. It is not a manager accepted-baseline member and has no invented deployment or artifact UUID.

Slot A is the separate CSR Canary 5 marker test. Accepted QSN C8 remains active from the baseline and occupies no Test Slot. Minecraft was not launched during deployment.

## Preflight

From the repository root, run the Test Instance Manager's read-only `verify` command and require `PHYSICAL_STATE_VERIFIED` for both exact slot identities, the upstream dependency receipt, Stack v17, revision 82, and the state digest above. Stop on drift. Do not rebuild or substitute the patch, change Stacks Are Stacks configuration, or touch the protected 26.1.2 gameplay profile. Use a disposable world and inspect the complete log through normal shutdown.

## Question 2 — Stacks Are Stacks patch

1. Create a stack of three saddles or another eligible normally non-stackable item.
2. Confirm the actual and displayed count remains three in the player inventory and an ordinary container.
3. Move, split, merge, shift-click, close, and reopen it; confirm actual and displayed counts remain correct.
4. Put it in a shulker and confirm Easy Shulker Boxes displays the item and count.
5. Use an ordinary vanilla-stackable item as a control and confirm its behavior is unchanged.
6. Reconnect once and confirm the client holder maximum remains aligned.

Judge this question independently from the Slot A CSR C5 marker check in that project's current `TESTING.md`.

## CSR/Stacks Are Stacks diagnostic-only probe

1. Attempt to create a CSR reservation using an eligible Stacks Are Stacks item.
2. Remove the physical stack and inspect the empty reservation.
3. Attempt matching manual and `QUICK_MOVE` reinsertion.
4. Optionally observe hopper and accepted QSN C8 behavior.
5. Record exactly what happens.

This probe is observation-only. A failure or inconclusive result is input for a future compatibility task; it is not authority to modify either implementation here, and no CSR/Stacks Are Stacks compatibility fix is claimed.

## Stopping and recording

Stop and report `RUNTIME_FAIL` or `INCONCLUSIVE` for the patch if identity/readiness or the exact dependency drifts; startup fails; a retained count is visually clamped or lost; a shulker-preview entry remains blank; movement, vanilla controls, configuration, eligibility, or reconnect behavior regresses; or a patch-attributable error appears. Record only observations actually made and do not promote Canary 1 without a complete applicable runtime pass.
