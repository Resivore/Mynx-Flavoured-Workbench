# Testing

No new Minecraft run is required for this migration. Use this procedure only for a future focused confirmation or regression test of the exact original accepted C5 private artifact; never substitute the clean base or a rebuilt equivalent.

## Preconditions

1. Verify `shulker-trowel-0.1.0-canary4-private.jar` is exactly 40,343 bytes with SHA-256 `73C012C08CA5567E795711CF11C8EAAC625528E7350309114467129394F0AFBA` and embedded version `0.1.0-canary4`.
2. Use the intended Minecraft 26.2 Fabric stack with Clutter No More 2.0.7, accepted Nibaru C45, and accepted Block Geometry Extensions C52. Keep Jake's Build Tools disabled; its separately authorized sprite is already inside the exact private artifact.
3. Record only checks actually observed. The preserved aggregate C5 pass does not imply that every numbered row was separately reported.

## Focused C5 procedure

1. Put exactly two Oak Planks in the offhand shulker, select Step mode, and place two fitting steps into one block space. Confirm the double geometry forms and exactly one plank remains.
2. Repeat with Horizontal Slab, then Vertical Slab while clicking the fitting half. Each double must use one source block total and leave one plank.
3. Place each of those geometries into separate empty spaces. Confirm each successful ordinary placement consumes one source item, while failed or incompatible placement consumes none.
4. Exercise Full Block, Slab, Stair, Wall, Vertical Slab, and Step modes through CNM's existing configured shape key. Move the trowel between slots and save/reload; confirm the selected server-authoritative mode persists and the familiar overlay does not replace or mutate the trowel.
5. Mix Oak Log, Oak Wood, Stripped Oak Log, and distinct oxidation/wax-state Copper sources. Confirm exact variants remain separate and only candidates eligible for the selected shaped mode participate in quantity weighting.
6. Use a shaped-mode palette with no eligible source. Confirm there is no placement, sound, or consumption.
7. Place stairs against multiple faces and into water. Confirm normal orientation, state, waterlogging, and one canonical placement sound for the actor and nearby players.
8. Smoke-check Full mode, the `S  ` / ` II` / `   ` recipe, the authorized trowel icon, and ordinary CNM switching with a non-trowel item.
9. Inspect `latest.log` for payload rejection, mixin, CNM overlay, registry, desynchronization, placement, or resource errors.

Stop and record `FAIL` or `INCONCLUSIVE` if a fitting double consumes the second full source block, exact variants collapse, an ineligible source participates, mode authority or persistence diverges, ordinary CNM behavior changes, the game crashes, or relevant errors appear in the log.
