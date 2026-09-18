# BGE C74 Ribbits Huge-Toadstool manual verification

Current candidate: `cnm-nibaru-integration-4.2.18-bge.canary74.ribbits-toadstools+26.2.jar` (pending finalized build hash)

- Embedded version: `4.2.18-bge.canary74.ribbits-toadstools+26.2`
- SHA-256: recorded in `WORKBENCH_STATUS.json` after the exact finalized artifact is retained
- Source checkpoint: `c0f17c650454e60b9518454b2f42bb1f837ccfd9`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Rollback/predecessor: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`. The owner reported an aggregate external runtime `PASS` for these exact C72 bytes without checklist-row observations; C72 was not accepted, and its evidence does not transfer to C73.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for the exact C74 bytes above. It does not authorize inspecting, creating, selecting, or modifying any protected or retired Minecraft profile.

## Ribbits huge-toadstools

For `ribbits:red_toadstool`, `ribbits:brown_toadstool`, and `ribbits:toadstool_stem`:

1. Verify the source full block. Join two matching source blocks, break one, and confirm the newly exposed face is the pale/spongy `toadstool_inside` surface rather than a fresh exterior.
2. Inspect one horizontal slab, its completion to the actual Ribbits source block, one Vertical Slab and its completion, a stair, wall, Layer, Step, Corner, and Quarter Column.
3. Join and break representative same-role members. Confirm no missing textures, stretched UVs, cap/stem substitutions, or state/model warnings. A focused red pass plus brown/stem spot checks is sufficient only after automated parity has passed.

## Canonical completion matrix

Use survival mode and verify the held stack decreases by exactly one only when placement succeeds. Use the debug screen or another exact registry-state inspection method to confirm the final world block is the canonical full block, not a derived slab with a full-looking state.

1. For ordinary horizontal Stone Slabs, place bottom then top and top then bottom. Each completed blockspace must become `minecraft:stone`; a single half must remain `minecraft:stone_slab`.
2. Repeat horizontal completion for representative catalog sources: Oak Planks, an axis material such as Oak Log, Oak Leaves, Grass Block under snow, a glazed terracotta orientation, and one optional-provider family available in the runtime set. The result must be the source's canonical full block.
3. Repeat with CNM/BGE Vertical Slabs, placing the complementary face for Stone and the same representative axis, leaves, glazed, and provider materials. A completed blockspace must become the canonical full block; a singleton must remain a Vertical Slab.
4. Confirm material state survives where the canonical block supports it: log `axis`, leaf `distance` and `persistent`, Grass Block `snowy`, and glazed horizontal pattern orientation. Geometry-only slab type, Vertical Slab facing/double state, and waterlogging that is not canonical material state must not leak into the full block.
5. Try a different-material second slab, the already occupied Vertical Slab face, blocked placement, and another invalid placement. The existing block must remain unchanged and the item must not be consumed.
6. Load or set historical horizontal `type=double` and Vertical Slab `double=true` states without placing a second item. They must remain valid derived states and must not be migrated in place.

## Canonical identity and special Farmland boundary

1. Spot-check all nine roles for representative vanilla, absorbed-Nibaru, BBB, and optional-provider families: canonical block, horizontal slab, stair, wall, Vertical Slab, Step, Layer, Corner, and Quarter Column must all represent the same canonical material.
2. Confirm the retained direct Dirt and Grass horizontal slab identities still resolve to Dirt and Grass Block, the retained direct Dirt Vertical Slab resolves to Dirt, and the existing Grass Vertical Slab remains the primary Grass role.
3. Confirm Farmland Slab remains one state-only special horizontal form: no BlockItem, recipe, creative entry, stair, wall, Vertical Slab, Step, Layer, Corner, or Quarter Column is added.
4. Farmland Slab must preserve `moisture=0..7` when mapped to canonical Farmland identity, but bottom/top/double placement must never normalize it to a full Farmland block. Its occupied top remains 7/16 for bottom and 15/16 for top/double.

## C72 Farmland regression boundary

1. Exercise bottom, top, and double Farmland Slabs. Water directly below and exactly four blocks horizontally away at Y-1 must hydrate; five blocks away and Y-2 must not. Existing Y and Y+1 hydration and rain hydration remain valid.
2. Remove water and rain. Moisture must fall through the vanilla-style lifecycle and zero-moisture Farmland must return to the matching Dirt Slab unless a block in the `maintains_farmland` contract prevents it.
3. Recheck survival under a solid block, player and mob trampling rules, Dirt-slab reversion, one/two-slab drops, Dirt Path conversion, and accepted C70's exact `minecraft:stone_slab` ownership.
4. Crop placement/growth, partial-height crop projection, targeting, particles, and rendering remain deferred to Slab Decorations and are not C73 failures.

The clean build, static suites, archive audit, and 123/123 controlled GameTests are not Minecraft gameplay-runtime evidence. Record `PASS`, `FAIL`, or `INCONCLUSIVE` only for rows actually exercised, together with the exact artifact SHA-256. Runtime testing alone does not accept or otherwise change the project lifecycle.
