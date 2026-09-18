# BGE C77 Layer canonical-completion manual verification

Current candidate: `BGE C77.jar`

- Embedded version: `4.2.21-bge.canary77.layer-canonical-completion+26.2`
- SHA-256: `7a7cce7949415ed7752595f3af39251c86f0ae053834fd7c0529366a632f9371`
- Source checkpoint: `4a5a48358ee1ffe9f34405bf750cd9576066899f`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Immediate predecessor: exact C76 `BGE C76.jar`, SHA-256 `18186f436399cecbd74b9b57dc662f1a7516ec3a860d6b964e2b999b0eedd9de`.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`. The owner reported an aggregate external runtime `PASS` only for these exact C72 bytes without checklist-row observations; that evidence does not transfer to C77.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for the exact C77 bytes above. It does not authorize inspecting, creating, selecting, or modifying any protected or retired Minecraft profile.

## Layer canonical completion

Use survival mode and exact registry-state inspection. Start from an empty blockspace with a matching Layer item and verify the held stack after every action.

1. Place the first, second, and third matching Layers. Each result must remain the same BGE Layer block with `layers=1`, `layers=2`, and `layers=3`; collision, targeting, and visible height must remain unchanged.
2. Place the fourth matching Layer onto `layers=3`. The result must be that material family's exact canonical full block, not a `layers=4` Layer or a merely full-looking substitute, and the held stack must decrease by exactly one.
3. Repeat with representative directional, axis, and oriented materials, including an Oak Log and glazed terracotta. Confirm the canonical block state retains every applicable state already handled by BGE projection rather than falling back to the canonical block's default state.
4. Try blocked, incompatible-material, wrong-face, and otherwise invalid fourth placements. The Layer state and held item count must remain exactly unchanged.
5. Manually create a legacy `layers=4` state through commands or fixture-equivalent means. Confirm it remains valid, renders as a full cube, is not migrated automatically, and rejects further placement without altering the block or consuming an item.

## Legacy Ribbits four-Layer fallback

For legacy `layers=4` states of `ribbits:red_toadstool`, `ribbits:brown_toadstool`, and `ribbits:toadstool_stem`:

1. Confirm each uses BGE's generated uniform full-cube Layer model on every face, with that family's assigned source texture and no `ribbits:block/toadstool_inside`, provider HugeMushroom model, or canonical-model route.
2. Confirm the legacy result remains correct when the provider's canonical block model is available; missing provider assets must not be the reason canonical reuse is avoided.
3. Complete a new Layer placement normally and confirm the resulting actual canonical Ribbits source block retains provider-owned rendering. C77 does not change the canonical source block's provider model.

## Ribbits all-surface texture rule

For `ribbits:red_toadstool`, `ribbits:brown_toadstool`, and `ribbits:toadstool_stem`:

1. Inspect the generated horizontal slab, stair, wall, Vertical Slab, Step, Layer, Corner, and Quarter Column. Confirm every externally visible face uses that family's assigned source-block texture.
2. Inspect outer faces, inset/cut faces, undersides, tops, sides, rotated states, and compound geometry. No visible face may use `ribbits:block/toadstool_inside`, and material orientation must not select a different texture.
3. Join and break representative matching generated roles. Newly visible generated faces must still use the assigned source texture; no pale/spongy interior face, missing texture, stretched UV, cap/stem substitution, or state/model warning is permitted.
4. Repeat with inventory previews for all eight generated roles. Their visible surfaces must follow the same uniform source-texture rule.
5. Complete matching horizontal and Vertical Slabs and confirm the result is the actual canonical Ribbits source block. Provider-owned source-block rendering after canonical completion is outside C76's generated-geometry texture rule and must retain provider behavior.

## C75 surface-semantics regression

1. Inspect full blocks, bottom/top/double horizontal slabs, each Layer height, all four Vertical Slab orientations, Steps, Corners, and Quarter Columns. For each exposed face, confirm the visible plane and footprint match the physical geometry; disjoint patches must remain independent.
2. Repeat representative checks on UP, DOWN, NORTH, SOUTH, EAST, and WEST. Confirm canonical TOP/SIDE/BOTTOM material-face meaning remains available through the C75 API without changing the C76 toadstool texture result.
3. Exercise Grass and Dirt Path against Farmland and Farmland Slab. Confirm their visible 16/15 and 8/7 top-height relationships remain exactly one sixteenth apart, while an ordinary non-terrain one-sixteenth mismatch is not treated as terrain.
4. Confirm Roots and Path families remain binding-driven and retain their existing appearance/state behavior. Registry names or Java carrier classes must not create an otherwise absent surface identity.
5. Treat native Stairs and Walls as limitation controls: their contextual inner/outer and post/arm surfaces must fail closed rather than reporting fabricated patches.

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
4. Crop placement/growth, partial-height crop projection, targeting, particles, and rendering remain deferred to Slab Decorations and are not C77 failures.

The clean build, 13 static suites, archive/JAR audits, and 130/130 controlled GameTests are not Minecraft gameplay-runtime evidence. Record `PASS`, `FAIL`, or `INCONCLUSIVE` only for exercised rows with the exact artifact SHA-256. Runtime testing alone does not accept or otherwise change the project lifecycle.
