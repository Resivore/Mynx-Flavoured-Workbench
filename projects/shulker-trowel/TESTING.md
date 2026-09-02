# Shulker Trowel C10 runtime procedure

Verified Slot A cohort:

- BGE C58: `cnm-nibaru-integration-4.2.2-bge.canary58.glass-corner-uv+26.2.jar`, SHA-256 `1a4e4d1cd9c8709720ec84975e70caffb5552ac676537b9bbae42dca96567e87`.
- Shulker Trowel C10 / Private Canary 9: `shulker-trowel-0.1.0-canary9-private.jar`, 44,738 bytes, SHA-256 `b78679eaf6eaf6f7ff75a32ffae024e45515de3af38bf2ed92ac8727a5138df8`.

Manager revision 62 verifies this exact two-member Slot A cohort as `READY_TO_TEST_VERIFIED / UNTESTED` and Slot B as empty. Before testing, run manager verification and require `PHYSICAL_STATE_VERIFIED`, the same exact filenames and hashes, and the visible title `Slot A: Block Geometry Extensions (BGE) - Canary 58 + Shulker Trowel - Canary 9`. Use only the dedicated Minecraft 26.2 Workbench. Keep CNM external and enable no standalone Nibaru JAR.

## Focused C58 compatibility checks

1. With a representative supported material in the offhand shulker, open the selector and confirm the established order remains `Full Block`, `Slab`, `Stair`, `Wall`, `Vertical Slab`, `Step`, `Corner`, `Quarter Column`, `Layer`.
2. Select Corner and confirm the overlay shows the exact Corner item resolved by the active BGE C58 catalog, including the corrected glass-Corner icon where applicable.
3. Place that Corner through the Trowel and confirm the result is the exact BGE C58 Corner block/item for the selected material, with normal delegated `BlockItem` orientation and state.
4. Exercise one representative non-Corner mode and confirm normal delegated placement, one-item Survival consumption from the actual offhand shulker, placement sound, and selector continuity remain unchanged.
5. Reopen and scroll the selector, then reconnect once; confirm saved mode identity and client/server synchronization remain continuous without duplicate, substituted, or unavailable modes.
6. Confirm the loader and logs report no dependency-predicate rejection, missing provider, duplicate BGE/Nibaru ownership, rejected payload, or synchronization error. The active provider for both `cnm_terrain_slabs_compat` and `more_slabs_stairs_and_walls` must be exact BGE C58.

## Boundaries and stopping conditions

The runnable candidate contains the separately authorized 346-byte private sprite; it remains ignored, private, and non-redistributable. Do not extract, publish, replace, or commit it.

Record only actual runtime observations. Build, unit tests, GameTest compilation, cohort deployment, and physical verification are not runtime evidence. Stop for a wrong Corner icon or placed result, selector identity/order drift, delegated-placement or consumption changes, dependency errors, duplicate ownership, or synchronization failures. C58 and Trowel C10 remain runtime untested until this focused procedure is completed; do not claim the C58 visual issue is fixed or promote either member from this procedure alone.
