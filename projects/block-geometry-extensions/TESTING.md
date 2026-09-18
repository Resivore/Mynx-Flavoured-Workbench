# BGE C75 surface-semantics manual verification

Current candidate: `cnm-nibaru-integration-4.2.19-bge.canary75.surface-semantics+26.2.jar`

- Embedded version: `4.2.19-bge.canary75.surface-semantics+26.2`
- SHA-256: `ea963665679ca116a1d0dc49ea6f730e16ec8222df5174d56c79aa3cd6845d8e`
- Source checkpoint: `b84315647592d7460d9e015a06187ce14f2b4e63`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Immediate predecessor: exact C74 `cnm-nibaru-integration-4.2.18-bge.canary74.ribbits-toadstools+26.2.jar`, SHA-256 `e261c8afdbf48b1078d2464dee814e4ca61538a669661aee98731e6acd4e8c15`.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`. The owner reported an aggregate external runtime `PASS` only for these exact C72 bytes without checklist-row observations; that evidence does not transfer to C75.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for the exact C75 bytes above. It does not authorize inspecting, creating, selecting, or modifying any protected or retired Minecraft profile.

## Surface and geometry matrix

1. Inspect full blocks, bottom/top/double horizontal slabs, each Layer height, all four Vertical Slab orientations, Steps, Corners, and Quarter Columns. For each exposed face, confirm the visible plane and footprint match the physical geometry; disjoint patches must remain independent.
2. Repeat representative checks on UP, DOWN, NORTH, SOUTH, EAST, and WEST. Confirm canonical TOP/SIDE/BOTTOM material-face meaning follows the oriented face without rotating or flattening the geometry.
3. Exercise Grass and Dirt Path against Farmland and Farmland Slab. Confirm their visible 16/15 and 8/7 top-height relationships remain exactly one sixteenth apart, while an ordinary non-terrain one-sixteenth mismatch is not treated as terrain.
4. Confirm Roots and Path families remain binding-driven and retain their existing appearance/state behavior. Registry names or Java carrier classes must not create an otherwise absent surface identity.
5. Treat native Stairs and Walls as limitation controls: their contextual inner/outer and post/arm surfaces must fail closed rather than reporting fabricated patches.

## Ribbits huge-toadstool regression

For `ribbits:red_toadstool`, `ribbits:brown_toadstool`, and `ribbits:toadstool_stem`:

1. Verify the source full block. Join two matching source blocks, break one, and confirm the newly exposed face is the pale/spongy `toadstool_inside` surface rather than a fresh exterior.
2. Inspect one horizontal slab, its completion to the actual Ribbits source block, one Vertical Slab and its completion, a stair, wall, Layer, Step, Corner, and Quarter Column.
3. Join and break representative same-role members. Confirm no missing textures, stretched UVs, cap/stem substitutions, or state/model warnings.

## Canonical completion and identity regression

Use survival mode and verify the held stack decreases by exactly one only when placement succeeds. Use exact registry-state inspection to confirm the final block is canonical, not merely full-looking.

1. Complete ordinary Stone Slabs in both orders, then representative Oak Planks, Oak Log, Oak Leaves, snowy Grass Block, glazed terracotta, and an available optional-provider family. Singletons remain derived; completions become their canonical full blocks.
2. Repeat with complementary CNM/BGE Vertical Slabs. Confirm supported material state survives (`axis`, leaf `distance`/`persistent`, `snowy`, and glazed orientation) while geometry-only state and noncanonical waterlogging do not leak.
3. Confirm failed, blocked, incompatible, and already-occupied placements preserve the block and item count. Historical double states remain valid and are not migrated in place.
4. Spot-check all nine roles for representative vanilla, absorbed-Nibaru, BBB, Ribbits, and other optional-provider families. Each must resolve to one canonical material.
5. Confirm Farmland Slab remains one state-only special horizontal form with no item or expanded geometry family. It preserves `moisture=0..7`, never normalizes to full Farmland, and retains occupied top planes of 7/16 for bottom and 15/16 for top/double.

## C72 Farmland regression boundary

1. Exercise bottom, top, and double Farmland Slabs. Water directly below and exactly four blocks horizontally away at Y-1 hydrates; five blocks away and Y-2 do not. Existing Y/Y+1 and rain hydration remain valid.
2. Remove water and rain. Moisture follows the vanilla-style lifecycle and zero-moisture Farmland returns to the matching Dirt Slab unless `maintains_farmland` prevents it.
3. Recheck solid-block survival, player/mob trampling, Dirt-slab reversion, one/two-slab drops, Dirt Path conversion, and accepted C70's exact `minecraft:stone_slab` ownership.
4. Crop placement/growth, partial-height crop projection, targeting, particles, and rendering remain deferred to Slab Decorations and are not C75 failures.

The clean build, static suites, archive audit, and 128/128 controlled GameTests are not Minecraft gameplay-runtime evidence. Record `PASS`, `FAIL`, or `INCONCLUSIVE` only for exercised rows with the exact artifact SHA-256. Runtime testing alone does not accept or otherwise change the project lifecycle.
