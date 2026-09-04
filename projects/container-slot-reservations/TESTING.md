# Testing

## Current gate

**READY TO TEST VERIFIED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Test only `artifacts/container-slot-reservations-0.1.0-canary4.jar`, 111,335 bytes, SHA-256 `006f2c01e3502bd19d67e26a53979ccc65d53dde08aa07313094017ca170df65`, embedded version `0.1.0-canary4`, from source checkpoint `d09c3f7153d4e53a36f41fb1ea80d85b4cb2e28f`. Test Instance Manager revision 77, accepted Stack v15, state digest `02020f5ec695bc7cfa0d15478ee1df31a7d1506188e5ea1ecdb9c60c2264d854`, deployment `8211900d-913c-40f2-9829-68509e0d71fe`, and artifact `cc469fa7-e2a5-417e-8e5c-978355f052f3` identify the exact Slot A candidate. Its deployment is `READY_TO_TEST_VERIFIED`; its independent result is `RUNTIME_UNTESTED`.

The deployment transition did not launch Minecraft, and no row below has been observed for Canary 4. Builds, static checks, bytecode audits, and GameTests are controlled evidence, not Minecraft runtime evidence. The user's report that Canary 3 ghost visuals look good is preserved only as a narrow positive predecessor observation; use it as the visual regression target, not as a Canary 4 result or a complete Canary 3 pass.

Stacks Are Stacks compatibility remains deferred. Do not enable, deploy, modify, test, or claim a fix for Stacks Are Stacks in this pass.

## Preflight

From the repository root, run the Test Instance Manager's read-only `verify` command. Require `PHYSICAL_STATE_VERIFIED` for the exact revision, Stack, digest, deployment, artifact, filename, hash, and source above. Also require unchanged QSN C8 in Slot B and accepted Double Barrels `1.0.1+26.2-canary2` (`doublebarrels-fabric-26.2-1.0.1+26.2-canary2.jar`) in the baseline. Stop on any drift. Do not redeploy or alter the managed Stack while testing, and never access the protected Minecraft 26.1.2 gameplay profile.

Use a disposable world and inspect the complete `latest.log` from registration through final shutdown, plus any project-attributable warnings, errors, stack traces, Mixin failures, codec failures, or crash reports. Record only rows actually observed.

## Runtime matrix

1. **Load and basic reservation contract.** Launch the verified Minecraft 26.2 Workbench, join the test world, and open each supported family below. For representative slots, set from an occupied stack and from the cursor, replace, clear, close, reopen, relog, and restart. Require count-one component-exact templates, server-authoritative convergence, unchanged extraction, and no fake zero-count stacks or unrelated component/content mutation.

2. **Chest and copper matrix.** Exercise single and double ordinary Chests, Trapped Chests, and every state: Copper Chest, Exposed Copper Chest, Weathered Copper Chest, Oxidized Copper Chest, Waxed Copper Chest, Waxed Exposed Copper Chest, Waxed Weathered Copper Chest, and Waxed Oxidized Copper Chest. In every permitted double form, reserve identifiable slots on both physical halves and around the 26/27 boundary; open from both halves and change orientation. Oxidize, scrape, wax, and unwax one half at a time. Require each reservation to remain with its original physical block entity and local index, with no deletion, duplication, migration, or swapping.

3. **General storage.** Test Barrels, the undyed Shulker Box, and all 16 dyed Shulker Boxes. Exercise manual placement, vanilla `QUICK_MOVE`, hopper insertion, component mismatches, extraction, placed-shulker save/reload, and carried-shulker break/place round trips. Require exact matching admission, native merge/order/remainder behavior, retained reservations after extraction, shulker nesting rejection, and intact `DataComponents.CONTAINER` and unrelated components. Chests, barrels, machines, and Double Barrels must not gain reservation-bearing item drops.

4. **Dispenser, Dropper, and Hopper.** Cover all nine Dispenser and Dropper slots and all five Hopper slots using manual placement, `QUICK_MOVE`, ordinary hopper input, and every applicable sided insertion path. Verify matching and mismatching component-rich reservations, transfer cooldowns, refill, extraction, forwarding, and Dispenser/Dropper activation. Reservations must gate only external insertion; they must not block, redirect, duplicate, or consume internal operations.

5. **Furnace family.** For Furnace, Blast Furnace, and Smoker, cover recipe input, fuel, and result slots through manual placement, `QUICK_MOVE`, hopper input, and applicable sided automation. A matching reservation may allow only a natively writable input or fuel slot. The output remains non-writable and must not gain reservation or QSN affinity merely because its template matches. Complete recipes and verify progress, fuel consumption, output production, XP, result-slot callbacks, extraction, and remainder behavior remain vanilla-owned.

6. **Brewing Stand.** Cover all three bottle/potion slots, ingredient, and blaze-powder fuel through manual placement, `QUICK_MOVE`, and all applicable sided insertion/extraction. Verify native item eligibility is still required before an exact reservation match. Complete brewing and require transformations, fuel use, extraction, slot limits, callbacks, and menu notifications to remain unchanged.

7. **Crafter.** Cover all nine persistent grid slots with matching and mismatching reservations. Test manual placement, `QUICK_MOVE`, hopper and sided insertion, slot toggling, and disabled slots. A disabled slot may retain and render a reservation but remains non-writable and supplies no QSN affinity; re-enabling may restore matching writability. Trigger crafting/ejection and require vanilla recipe, activation, output, remainder, notification, and disabled-slot behavior.

8. **Native versus reservation policy and QSN.** Across the families above, compare ordinary writable empties, matching reserved empties, mismatching reserved empties, compatible occupied stacks, native-invalid inputs, output-only slots, and disabled Crafter slots. Test manual, `QUICK_MOVE`, hopper/sided, and unchanged QSN C8 paths. Require native permission **and** an absent or exact component match. QSN may prefer a natively writable matching reserved empty only after its existing-stack merges; it must not gain affinity from a native-non-writable, full, mismatching, foreign, fake, inactive, player-inventory, or Ender Chest slot. Preserve QSN ordering, capacity, packets, remainders, and feedback.

9. **Ender Chest ownership and persistence.** With player A, reserve multiple physical indices, open different placed Ender Chests, change dimensions, relog, restart, die, and respawn with keep-inventory both enabled and disabled where practical. Require one stable reservation set to follow player A's Ender Chest inventory independently of placed blocks and ordinary inventory rules. Load an old player record without `container_slot_reservations:ender_chest_reservations` and require an empty default. With player B, prove independent contents and reservations; neither player may observe or mutate the other's set. Breaking a placed Ender Chest must not affect either player, and Ender Chests must remain inaccessible to hoppers and QSN world scanning.

10. **Double Barrels.** Using the exact accepted mod, connect two physical barrels that have distinguishable reservations. Open from each half and verify combined slots 0 and 26 map to the main physical barrel while 27 and 53 map to the partner with local indices 0 and 26. Repeat manual placement, `QUICK_MOVE`, hopper insertion/extraction, and QSN routing on both halves. Save/reload, disconnect, reconnect, reverse main/partner orientation, and break one half. Require reservations to stay on their original physical barrel without wrapper-owned persistence, loss, duplication, migration, or swapping; preserve accepted comparator, drops, opening/closing, automation, and menu behavior.

11. **Canary 3 visual regression.** On the Matcha `#43362A` slot background, inspect light and dark atlas-rendered and oversized/picture-in-picture ghost items at practical GUI scales. Require original hue at approximately 35% opacity, clean transparent edges, no black/brown tint or matte, and no opacity leakage. Confirm the shadowed literal `0`, compact occupied marker, empty-reservation tooltip, highlights, counts, optional Easy Shulker Boxes / Item Interactions overlays, and reservation-free slots remain unchanged. This row checks the user-approved Canary 3 appearance only; it does not inherit a result.

12. **Synchronization and shutdown.** With two viewers on the same supported block container, set, replace, and clear reservations from both sides and require exact convergence without stale snapshots or unauthorized slots. Reopen every menu family, then save and shut down normally. Inspect the complete log through shutdown and record the exact families, paths, indices, persistence transitions, and observations actually exercised.

## Stopping and recording

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if identity or physical verification drifts; any requested owner/menu slot is absent, duplicated, foreign, fake, or mapped to the wrong physical owner; native-invalid insertion becomes writable; a matching writable reservation is ignored; extraction, routing order, remainders, internal processing, persistence, synchronization, visuals, optional-mod absence safety, or logs regress; or any project-attributable error occurs.

Do not promote from partial observations or controlled evidence. Do not infer unobserved rows, turn the Canary 3 visual report into a Canary 4 result, or claim the deferred Stacks Are Stacks issue was exercised.
