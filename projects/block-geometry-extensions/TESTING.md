# BGE C78 Stair and Wall surface-authority manual verification

Current candidate: `BGE C78.jar`

- Embedded version: `4.2.22-bge.canary78.stair-wall-surface-authority+26.2`
- SHA-256: `ed2f5592b699532174bc63672f69c3574f01eb752175126bbc702e9cc86a07f0`
- Source checkpoint: `85acefa42fa46e133b0917d056817d6eaf0768b3`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Immediate predecessor: exact C77 `BGE C77.jar`, SHA-256 `7a7cce7949415ed7752595f3af39251c86f0ae053834fd7c0529366a632f9371`.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`. The owner-reported aggregate external runtime `PASS` applies only to those exact C72 bytes and does not transfer to C78.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for the exact C78 bytes above. It does not authorize inspecting, creating, selecting, or modifying any protected or retired Minecraft profile.

## Stair surface authority

Use a diagnostic consumer of `BgeMaterialBindings.Binding.surfaceModel(BlockState)` and inspect resolved states rather than registry-name assumptions.

1. Exercise all four facings, both halves, and all five shapes (`STRAIGHT`, `INNER_LEFT`, `INNER_RIGHT`, `OUTER_LEFT`, `OUTER_RIGHT`). Every one of the 40 states must return a supported, nonempty, deterministic patch set.
2. Confirm the tread, riser, underside, outer sides, inner-corner surfaces, and outer-corner surfaces follow the resolved physical shape. No face may remain at a cuboid interface hidden inside the union.
3. Rotate each state through the four horizontal facings and compare the entire patch set. Mirror left/right shapes and top/bottom halves; planes, footprints, physical normals, and canonical material faces must transform consistently.
4. Inspect ordinary, axis-material, grass-like, glazed-patterned, transparent/authored, and optional-provider representatives. Geometry must come from the topology/state contract while canonical face meaning remains in the material's world frame.
5. Inspect Path Stairs. Their lowered 7/16 and 15/16 tread planes must be reported with the typed `TERRAIN_HEIGHT_INSET` relation; ordinary one-sixteenth offsets must remain `EXACT`.

## Wall surface authority

1. Exercise resolved `up` post state plus every NORTH/EAST/SOUTH/WEST arm value (`NONE`, `LOW`, `TALL`), including isolated post, single arms, corners, tees, crosses, straight runs, and mixed LOW/TALL junctions. Every nonempty ordinary state must return a supported, nonempty, deterministic patch set.
2. Confirm posts occupy their central footprint and arms use their resolved 14/16 LOW or 16/16 TALL height. Surfaces where post and arms or crossing arms overlap must be absent from the exposed patch set.
3. Rotate representative asymmetric states through all four horizontal orientations and compare the entire patch set, including planes, footprints, normals, and canonical faces.
4. Inspect standard native walls, Path Walls, leaf/translucent walls, axis materials, glazed patterns, and Ribbits huge-toadstool walls. Path posts retain their 15/16 typed terrain height; huge-toadstool arm reach follows its provider-owned compound silhouette without registry-ID branching.
5. Verify a consumer can process Stair and Wall patches through the same generic surface API used by every other BGE topology, with no Stair/Wall special case.

## Existing surface and canonical regressions

1. Inspect full blocks, horizontal slabs, every Layer height, all Vertical Slab orientations, Steps, Corners, Quarter Columns, Paths, Roots, and Farmland Slab. Every valid binding remains supported and each topology retains exact sixteenth planes and independent compound patches.
2. Confirm Grass keeps TOP/SIDE/BOTTOM material-face meaning, logs retain their canonical axis frame, and glazed patterns do not rotate merely because the geometry rotates.
3. Confirm a successful fourth matching Layer placement becomes the exact canonical material block with one item consumed; partial and failed placements, legacy four-Layer states, and Ribbits uniform fallback models retain C77 behavior.
4. Confirm the three Ribbits toadstool families still use their assigned source texture on every generated role and never route a generated surface through `ribbits:block/toadstool_inside`.
5. Confirm ordinary horizontal and Vertical Slab completion preserves applicable axis, leaf, snowy, and glazed state, while Farmland Slab remains a state-only special form that never normalizes to full Farmland.

## C72 Farmland regression boundary

1. Exercise bottom, top, and double Farmland Slabs. Water directly below and exactly four blocks horizontally away at Y-1 hydrates; five blocks away and Y-2 do not. Existing Y/Y+1 and rain hydration remain valid.
2. Remove water and rain. Moisture follows the vanilla-style lifecycle and zero-moisture Farmland returns to the matching Dirt Slab unless `maintains_farmland` prevents it.
3. Recheck solid-block survival, trampling, Dirt-slab reversion, one/two-slab drops, Dirt Path conversion, and accepted C70's exact `minecraft:stone_slab` ownership.
4. Crop placement/growth, partial-height crop projection, targeting, particles, and rendering remain deferred to Slab Decorations and are not C78 failures.

The clean build, 13 static suites, archive/JAR audits, and 135/135 controlled GameTests are not Minecraft gameplay-runtime evidence. C78 changes no CTM rule or rendering policy, and `projects/bge-ctm` remains untouched. Record `PASS`, `FAIL`, or `INCONCLUSIVE` only for exercised rows with the exact artifact SHA-256. Runtime testing alone does not accept or otherwise change the project lifecycle.
