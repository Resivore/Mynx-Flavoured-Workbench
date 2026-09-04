# Testing

## Current gate

**READY TO TEST VERIFIED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Test only `artifacts/container-slot-reservations-0.1.0-canary5.jar`, 112,541 bytes, SHA-256 `D228EF4ECE4893A6538A845B34C1684A72FE8659790DC63AA12AB608AA886B8E`, embedded version `0.1.0-canary5`, from source checkpoint `793a29fa0cac2f505e6fcf373c56b76e16eca568`.

Test Instance Manager revision 82, accepted Stack v17, state digest `19C42A70EF995A671B9A6D063AD61A927F0BD8A22617722FEF103F862F971577`, Slot A deployment `f12e434a-51c1-4e84-a487-3ebcadbe3d64`, and artifact `c9551ad6-7a35-4a0e-ac25-af3efeccd86a` bind the exact candidate. Its deployment is `READY_TO_TEST_VERIFIED`; its independent result is `UNTESTED`.

The only intended production change from accepted Canary 4 is the occupied-reservation marker. The source and built JAR contain the same 103-byte, 3×3, 8-bit RGBA resource at `assets/container_slot_reservations/textures/gui/sprites/occupied_reservation_marker.png`, SHA-256 `5A9FE986E6AED154D8E1302C1A8F066D76A2AFF10AA4F6FC9FD973D0E5BCAACE`. It is drawn one-to-one at `itemX + 13, itemY`; the two bottom corners are transparent. The former opaque `0xFF24C7B8` fill is absent.

Slot B is the separate Stacks Are Stacks Container Fixes Canary 1 test. Accepted QSN C8 remains active from the baseline and occupies no Test Slot. Minecraft was not launched during build, promotion, or deployment.

## Preflight

From the repository root, run the Test Instance Manager's read-only `verify` command and require `PHYSICAL_STATE_VERIFIED` for every exact identity above, both slots, and Stack v17. Stop on any drift. Use only the dedicated Minecraft 26.2 Workbench, never the protected 26.1.2 gameplay profile. Use a disposable world, keep the managed Stack unchanged, inspect the complete log through normal shutdown, and record only behavior actually observed.

## Question 1 — CSR Canary 5 marker

1. Create an occupied reservation in representative storage and machine menus.
2. Confirm the old teal square is gone.
3. Confirm the exact new 3×3 marker appears at the same top-right position.
4. Confirm its bottom-left and bottom-right pixels are transparent and reveal the underlying item sprite.
5. Confirm the marker does not affect the item sprite, stack count, empty-reservation ghost, literal zero, tooltip, or later GUI rendering. Confirm empty reserved slots still show the ghost and zero, and unreserved, player-inventory, fake, and foreign slots remain untouched.
6. Confirm several existing Canary 4 container types still open and retain reservations normally, including representative ordinary storage, machine, copper-chest, Double Barrels, Ender Chest, manual insertion, `QUICK_MOVE`, hopper, carried-shulker, and accepted QSN C8 paths as applicable.

Judge this question independently from the Slot B Stacks Are Stacks patch check in that project's current `TESTING.md`.

## CSR/Stacks Are Stacks diagnostic-only probe

1. Attempt to create a CSR reservation using an eligible Stacks Are Stacks item.
2. Remove the physical stack and inspect the empty reservation.
3. Attempt matching manual and `QUICK_MOVE` reinsertion.
4. Optionally observe hopper and accepted QSN C8 behavior.
5. Record exactly what happens.

This probe is observation-only. It does not claim compatibility and must not trigger implementation changes in this task. Retain a failure or inconclusive observation as input for a future compatibility task.

## Stopping and recording

Stop and report `RUNTIME_FAIL` or `INCONCLUSIVE` for the CSR candidate if identity/readiness drifts, the marker differs in position, shape, color, scale, sampling, or transparency, the former fill remains, the marker appears outside `OCCUPIED_RESERVED`, any preserved Canary 4 behavior regresses, or a CSR-attributable error appears. Do not infer unobserved rows or promote Canary 5 from build, static, GameTest, or partial runtime evidence.
