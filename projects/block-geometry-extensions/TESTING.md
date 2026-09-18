# BGE C71 Farmland Slab manual verification

Current candidate: `cnm-nibaru-integration-4.2.15-bge.canary71.farmland-slab+26.2.jar`

- Embedded version: `4.2.15-bge.canary71.farmland-slab+26.2`
- SHA-256: `caefdf6c73c9ffeaf1418de9861c4f34e9801fab32450776a1e4c49aadfeff95`
- Source checkpoint: `f036fa52e6034d2b3362ff0027543ae0fb87cc4b`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Accepted rollback/predecessor: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`

This checklist is lifecycle-neutral. Record only behavior actually observed for the exact C71 bytes above. It does not authorize inspecting, creating, selecting, or modifying any protected or retired Minecraft profile.

## Hoe parity matrix

Exercise bottom, top, and double forms for every row. Start with an unwaterlogged slab and air above unless the row says otherwise.

| Source slab | First hoe use | Second hoe use |
| --- | --- | --- |
| Grass Block | matching Farmland Slab | unchanged Farmland Slab |
| Dirt Path | matching Farmland Slab | unchanged Farmland Slab |
| Dirt | matching Farmland Slab | unchanged Farmland Slab |
| Coarse Dirt | matching Dirt Slab | matching Farmland Slab |
| Rooted Dirt | matching Dirt Slab plus one Hanging Roots drop from the clicked face | matching Farmland Slab |

For every successful use, confirm the vanilla hoe sound, one durability cost, the same bottom/top/double type, and no full-block replacement. Confirm the canonical BGE Dirt and Grass compatibility slabs follow the same direct conversion.

Then verify the rejection rules:

1. Grass Block, Dirt Path, Dirt, and Coarse Dirt reject a DOWN-face click or any non-air block above, without sound, durability loss, or conversion.
2. Rooted Dirt keeps vanilla's unconditional first step, including its Hanging Roots drop, before Dirt's normal air/face rule applies.
3. Every waterlogged eligible source remains unchanged and retains its water.
4. Podzol, Mycelium, and other non-tillable slabs remain unchanged.

## Farmland lifecycle and economy

1. Confirm the state has only `type` and `moisture=0..7`; there is no waterlogged state, BlockItem, recipe, creative entry, or derived stair/wall/other BGE geometry.
2. Confirm nearby water hydrates to moisture 7 through vanilla's inclusive four-block horizontal and current-to-one-above vertical range. Confirm rain hydrates where vanilla rain reaches the block.
3. Remove water and rain. Confirm moisture drops one level per applicable random tick, then zero-moisture Farmland eventually returns to the matching Dirt Slab.
4. Confirm a block in vanilla's `maintains_farmland` contract prevents zero-moisture reversion. Crop placement/growth on the partial-height surface is intentionally not supplied by C71 and remains a Slab Decorations task.
5. Place a solid block above and confirm the scheduled survival check returns bottom, top, and double Farmland to the matching Dirt Slab.
6. Confirm player trampling follows vanilla probability/size behavior; non-player trampling respects `mobGriefing`, and undersized living entities do not trample.
7. Confirm the occupied top is 7/16 for bottom Farmland and 15/16 for top/double Farmland, with no accidental full-block collision.
8. Break each type: a single slab yields one canonical Dirt Slab and a double yields two.

## Regression boundary

1. Recheck Dirt Slab to Dirt Path shovel conversion and Dirt Path survival/reversion.
2. Recheck accepted C70's Stone profile: `minecraft:stone_slab` remains its exact effective horizontal source and no duplicate Stone slab appears.
3. Spot-check another material profile and each existing BGE geometry catalog role for unchanged placement, identity, texture behavior, and economy.
4. Do not treat missing crop placement, crop projection, crop targeting, particles, or partial-height crop rendering as a C71 failure; those behaviors are explicitly deferred to Slab Decorations.

Record `PASS`, `FAIL`, or `INCONCLUSIVE` only for rows actually exercised, together with the exact artifact SHA-256. Runtime testing does not by itself accept or otherwise change the project lifecycle.
