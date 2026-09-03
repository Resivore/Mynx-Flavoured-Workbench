# Testing

C4 (`0.1.0-canary4`) is the exact current candidate, remains independently
`RUNTIME_UNTESTED`, and is not deployed. Its retained artifact is
`slab-decorations-0.1.0-canary4.jar`, 45,148 bytes, SHA-256
`F571F211F4B94909A6DED64EC8B97B87EA05B08F982A83BACF2EDE6BB12960FE`,
with embedded version `0.1.0-canary4` from source checkpoint
`c15b21e47a736ccf090825df8e3de0afa29db717`.

Retain exact C3 as historical predecessor:
`slab-decorations-0.1.0-canary3.jar`, 36,081 bytes, SHA-256
`5DFE24CF686A781C16DCA46A11EF4112D7473E1F8D63FAC659CB750C6709BF2B`,
source `2358342ebbd51cdd1b110e74e815205a68acc882`. The earlier user observation
that many C3 foliage variants rendered correctly while dripleaf accepted by full moss did not
inherit onto moss slab motivated C4 only; it is not recorded as a C3 runtime result or PASS.

## C4 design and controlled evidence

C4 resolves only exact native horizontal slabs owned by a BGE material profile. For a
structurally eligible plant it resolves the physical root, derives the profile's canonical parent
state with shared properties, and evaluates ordinary Minecraft `canSurvive` against a read-only
`LevelReader` that substitutes that one support position. All other direct block, fluid, light,
biome, dimension, border, feature, and environmental reads remain real. A tightly scoped
`ThreadLocal` guard prevents recursive projection and is removed in `finally`; no projected state
is placed, saved, or sent to clients. Raw `ChunkAccess` inspection remains delegated and is outside
the compatibility claim for standard foliage placement contracts.

Eligibility is class-contract based rather than a registry-ID permission table: ordinary upward
`VegetationBlock` states, double-height plants, small and big dripleaf segments, mossy-carpet
base/topper segments, and non-wool surface carpets use family/root adapters. Crop/farmland classes
(including the distinct double-height pitcher crop class), stems, saplings, nether fungi, lily
pads, and fluid-dependent/aquatic plants fail closed. Mushroom and azalea structure-generating
bonemeal is refused only on lowered bottom-slab decoration paths. C4 does not override ordinary
mushroom spread or harvesting methods; sweet-berry age and bonemeal growth were exercised by the
server suite, while gameplay spread, general harvesting, and reload behavior remain in the runtime
matrix.

Accepted bottom-slab states use the existing `-0.5` Y model offset. Outline, targeting,
cached/context collision, visual, interaction, entity-inside, breaking-overlay, and shape-derived
particle geometry follow the same surface exactly once for every rooted segment. The render model
pairs each `RenderSectionRegion` snapshot with its own `ClientLevel`. Top/double slabs and ordinary
full blocks retain zero offset.

The final Java 25 / Gradle 9.5.1 / Loom 1.17.19 clean build passed 4/4 focused JUnit tests and all
21 implemented headless server Fabric GameTests against exact unified BGE C58
`cnm-nibaru-integration-4.2.2-bge.canary58.glass-corner-uv+26.2.jar`, SHA-256
`1A4E4D1CD9C8709720EC84975E70CAFFB5552AC676537B9BBA42DCA96567E87`,
CNM 2.0.7, Terrain Slabs 3.3.2, and Fabric API 0.157.0+26.2. Artifact inspection confirmed Java
25 bytecode, embedded C4 metadata, all required common/client mixins, no stale C3 mixins, no nested
dependencies, and a wildcard `more_slabs_stairs_and_walls` capability requirement. Dependency
policy validation checked seven predicates with no exceptions. These are controlled build/static
results, not gameplay runtime evidence.

The server GameTests did not execute `SurfaceOffsetModel`, `RenderSectionRegionAccessor`,
`EntityPickMixin`, `LevelRendererDestroyOverlayMixin`, real client rendering, or client particles;
those client seams were source- and artifact-inspected. The automated reload case performed a
`BlockState` codec round-trip plus in-memory remove/reinsert and was not an actual chunk unload,
world save, or world reload. The runtime matrix below retains those checks.

C3 substrate bonemeal remains intentionally separate from the read-only survival projection. Its
bounded server transaction still delegates only dry bottom native Grass Block, Moss Block, and
Pale Moss Block slabs to vanilla, consumes exactly one item per valid activation, restores original
native slabs, reconciles supported moss outputs, positions generated foliage at the half-height
surface, and retains waterlogged/unsupported negatives plus lowered-azalea tree safety.

## Runtime preconditions

1. Before any future runtime test, use a new serialized Test Instance Manager assignment for exact
   C4 and require `CURRENT_RELEASE_DEPLOYED / READY_TO_TEST_VERIFIED` with a fresh `UNTESTED` result.
   Stop if its filename, size, SHA-256, embedded version, or source checkpoint differs.
2. Confirm a compatible unified BGE provider is the sole enabled owner of
   `more_slabs_stairs_and_walls`; exact C58/CNM/test-tool identities above are controlled baselines,
   not permanent runtime pins. Do not enable a standalone conflicting Nibaru provider.
3. Use a disposable Minecraft 26.2 Fabric world in the dedicated Workbench only. Do not touch the
   protected gameplay profile, substitute the artifact, infer runtime evidence from GameTests, or
   modify the accepted baseline or other Test Slot.

## Focused C4 runtime matrix

1. Compare red and brown mushrooms on full podzol and its exact dry bottom native slab; both small
   mushrooms must place, survive, spread normally, and remain lowered. Confirm a canonical-rejecting
   glass pair remains invalid and bottom-slab giant-mushroom bonemeal fails without consumption.
2. Compare lower/upper small dripleaf, big-dripleaf stem/head chains, and valid leaf-on-leaf heads
   on full moss and its native bottom slab. Every segment must survive and align; invalid canonical
   support must reject the column, and ordinary dripleaf growth/updates must remain coherent.
3. Recheck flowers, short/tall grass, fern/large fern, dead bush, nether wart, petals, azaleas,
   moss/pale-moss carpets, wither rose, and sweet-berry age/growth/harvest. Verify identity, drops,
   replacement, neighbor updates, support-removal cleanup, and save/chunk reload.
4. For representative single and multi-segment states, verify model position, outline, targeting,
   both collision paths, visual/interaction/effect volume, breaking overlay, and particles at the
   real `+0.5` bottom-slab surface. Top/double slabs and full blocks must remain unshifted.
5. Bonemeal exact dry bottom native Grass Block, Moss Block, and Pale Moss Block slabs. Confirm one
   item is consumed per valid activation, the exact clicked slab remains, generated/reconciled
   outputs use native bottom slabs, vegetation is lowered, and azalea cannot create a floating tree.
6. Confirm waterlogged slabs, invalid canonical pairs, unsupported materials, vertical slabs, CNM
   Steps, layers, stairs, walls, columns, vanilla/foreign slabs, and unrelated blocks gain no C4
   survival, offset, or substrate-bonemeal behavior.
7. Save, unload/reload chunks, and reload the world, then inspect for identity, state-property,
   alignment, Mixin, recursion, dependency, rendering, targeting, or cleanup errors. Confirm C3,
   unrelated BGE/CNM geometry, the accepted stack, and the other Test Slot remain unchanged.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` for any identity/readiness mismatch, canonical
parity mismatch, double/missing geometry shift, recursive or leaked evaluation state, real-world
projection mutation, lost state/companion, stale reload representation, incorrect bonemeal
consumption/reconciliation, unsupported-geometry spread, crash, or relevant log error. Record only
behavior actually observed; do not infer individual matrix rows or promote C4 from controlled
validation.
