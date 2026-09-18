# BGE C74 Surface Semantics manual verification

Current candidate: `cnm-nibaru-integration-4.2.18-bge.canary74.surface-semantics+26.2.jar`

- Embedded version: `4.2.18-bge.canary74.surface-semantics+26.2`
- SHA-256: `8845817c1418d039a424ee6218883d53ad8a3e5d680d4d717317ee29f0276d24`
- Source checkpoint: `33dec16d9eedfd7d131a318f60f2a8f48a7ab0bb`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Immediate predecessor: exact C73 `cnm-nibaru-integration-4.2.17-bge.canary73.canonical-bindings+26.2.jar`, SHA-256 `4e8e7bbac17b828177059d21ed6a6d216028a17252e64b23ff4142b6d1b8200c`.
- Rollback release: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`. The owner reported only an aggregate external runtime `PASS` for these C72 bytes; that evidence does not transfer to C74.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for the exact C74 bytes. It does not authorize inspecting, creating, selecting, or modifying any protected or retired Minecraft profile. The clean build, archive audit, and 127/127 headless GameTests prove controlled state and geometry contracts; they are not Minecraft visual validation.

## Canonical completion regression

1. Complete ordinary Stone horizontal slabs in both orders and complementary Stone Vertical Slabs. Each newly completed blockspace must become `minecraft:stone`; a singleton remains derived.
2. Repeat with Oak Log, Oak Leaves, Grass Block under snow, one glazed terracotta, and one available optional-provider family. Preserve canonical material state (`axis`, leaf `distance`/`persistent`, `snowy`, and glazed pattern orientation) without leaking geometry state.
3. Try an incompatible second material, occupied face, and blocked placement. The world state and item count must remain unchanged.
4. Historical horizontal `type=double` and Vertical Slab `double=true` states remain legal and are not migrated merely by loading.

## Surface-contract matrix

Use exact BGE × CTM C8 when visually exercising Continuity. Continuity remains the semantic rule authority; this matrix checks that BGE's physical surface description is truthful.

1. On all six face orientations, compare full/full, matching partial/partial, full beside a bottom slab's top plane (negative), full beside a bottom slab's coplanar side (positive when Continuity's rule is positive), full beside a top slab top (positive), and top beside bottom slab top (negative).
2. Repeat representative positive and negative planes with one Vertical Slab and one Layer orientation. Confirm the connected texture or overlay remains on the receiver's actual rendered plane.
3. Exercise Step, Corner, and Quarter Column states with more than one exposed patch. Faces must be evaluated independently; no geometry may be flattened to one whole-block cuboid.
4. Native Stairs and Walls are intentionally fail-closed in C74 because their rendered post/arm or inner/outer topology is neighbor-contextual. Lack of BGE × CTM behavior there is a documented limitation, not evidence that speculative support should be added.

## Typed terrain inset matrix

1. Establish an already-positive Continuity semantic relationship for full Grass Block and full Farmland, then check the corresponding 16/16-to-15/16 surface relationship.
2. Check grass bottom slab 8/16 beside Farmland Slab 7/16, grass top 16/16 beside Farmland Slab top 15/16, and full grass beside double Farmland Slab 15/16. Check top and corresponding side faces separately.
3. Check one Dirt Path-derived Step or other supported path geometry against its ordinary corresponding plane.
4. Include a non-terrain pair whose planes differ by 1/16. It must not receive the terrain tolerance.

## Farmland and C72 regression boundary

1. Farmland Slab remains one state-only special horizontal form with no item, recipe, creative entry, or generated stair, wall, Vertical Slab, Step, Layer, Corner, or Quarter Column. Moisture `0..7` projects to canonical Farmland, while bottom/top/double placement never normalizes to a full Farmland block.
2. Bottom, top, and double Farmland Slabs hydrate from water directly below and exactly four blocks horizontally away at Y-1; five blocks away and Y-2 do not. Existing Y/Y+1 and rain hydration remain valid.
3. Recheck drying/reversion, `maintains_farmland`, obstruction, player/mob trampling, one/two-slab drops, Dirt Path conversion, and accepted C70's exact `minecraft:stone_slab` ownership.
4. Crop placement/growth, partial-height crop projection, targeting, particles, and rendering remain deferred to Slab Decorations and are not C74 failures.

Record `PASS`, `FAIL`, or `INCONCLUSIVE` only for rows actually exercised, with the exact artifact SHA-256 and companion/resource-pack identities where applicable. Runtime testing alone does not accept or otherwise change the project lifecycle.
