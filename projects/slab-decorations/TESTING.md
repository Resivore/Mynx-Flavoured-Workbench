# Testing

C5 (`0.1.0-canary5`) is the exact current candidate. It is independently
`RUNTIME_UNTESTED`, unaccepted, and not deployed. Its retained artifact is
`slab-decorations-0.1.0-canary5.jar`, 50,300 bytes, SHA-256
`8DE706EB59BDAFD5C594F5375BE6485B9EDB7B30B2B2017B2C8EAA44129FE480`, with
embedded version `0.1.0-canary5` from source checkpoint
`b589e1e52825e94a41fec2932c14c5a4ba07d791`.

Retain exact C4 unchanged as the historical predecessor:
`slab-decorations-0.1.0-canary4.jar`, 45,148 bytes, SHA-256
`F571F211F4B94909A6DED64EC8B97B87EA05B08F982A83BACF2EDE6BB12960FE`, source
`c15b21e47a736ccf090825df8e3de0afa29db717`. C4 was never gameplay-runtime
validated; C5 does not manufacture a C4 result or PASS.

## C5 design and controlled evidence

C5 retains C4's exact BGE-owned native-horizontal-slab gate and read-only
canonical-parent `LevelReader` projection. It adds an attachment orientation:

- Upward families resolve the physical root and project only the support below it. A bottom slab
  has offset `-0.5 Y`; top and double slabs have zero offset.
- Ceiling families resolve the physical top anchor and project only the support above it. A top
  slab has offset `+0.5 Y`; bottom and double slabs have zero offset.

The projected position contains the material profile's canonical parent. Every other block,
plant segment, fluid, light, biome, dimension, border, state, and environmental read remains real.
The existing re-entry guard still fails closed around unsafe projection, no projected state is
placed or saved, and waterlogged native slabs remain unusable.

Minecraft 26.2 provides no common hanging-foliage superclass for Hanging Roots and Spore Blossom,
so C5 uses their concrete block classes as narrow behavior contracts. Pale hanging moss uses its
own structural column contract. Generic `GrowingPlantBlock` head/body pairs whose protected growth
direction is `DOWN` cover Cave Vines, Weeping Vines, and compatible modded columns. From any
head/body segment, C5 walks only through the exact compatible pair to the real top anchor; the
anchor's ordinary projected `canSurvive` decides the column, and every connected segment derives
the same one-time offset. Upward-growing and fluid-dependent growing plants fail closed.

The current Mynx × Regions Unexplored downward dropleaf source uses the same Minecraft
`GrowingPlantHeadBlock`/body contract with `Direction.DOWN`; no RU registry-ID exception is needed.
Its current private ignored artifact was not added as a test or production dependency. Actual
Mynx × RU interoperability therefore remains a focused runtime/manual check.

The final Java 25 / Gradle 9.5.1 / Loom 1.17.19 command
`clean test runGameTest build --offline --no-daemon` passed 5/5 focused JUnit tests and all 29
required headless server Fabric GameTests. Controlled inputs were exact unified BGE C58
`cnm-nibaru-integration-4.2.2-bge.canary58.glass-corner-uv+26.2.jar`, SHA-256
`1A4E4D1CD9C8709720EC84975E70CAFFB5552AC676537B9BBAE42DCA96567E87`; CNM
2.0.7, SHA-256 `41A925E70D5E6E8C098BEA7DC88C44486AED46724E35CB2FA4B1622B2A4DBCCE`;
Terrain Slabs 3.3.2, SHA-256
`C67C334C8EA0A6A47EFFA96B740568BF505F41E9484A82E06736819F26F78EDA`; and Fabric API
0.157.0+26.2. The tests exercised canonical-valid and canonical-invalid ceiling parents,
top/bottom/double offset matrices, real item-use placement, support removal, multi-segment Cave and
Weeping Vine growth, head/body conversion, bonemeal, maximum age, berry light/harvesting and drop,
pale hanging moss, waterlogged and foreign-geometry isolation, positive-offset raycasting, and all
retained C4 upward and substrate-bonemeal regressions.

Artifact inspection confirmed byte identity between build output and the retained 50,300-byte
JAR, the embedded C5 version, Java class major 69, the generic growing-plant accessor plus all
required common/client mixins, capability-based production dependencies, no nested dependency
JARs, and no RU implementation classes or dependency. These are controlled build/static results,
not gameplay runtime evidence.

The server GameTests did not execute the FRAPI model wrapper, `RenderSectionRegionAccessor`,
`EntityPickMixin`, breaking-overlay client Mixin, real client particles, or a rendered client.
Those seams were source-, architecture-, and artifact-inspected; the shared server shapes and
raycast helper were executed. Existing reload automation is a block-state codec round-trip plus
in-memory remove/reinsert, not a real chunk unload, world save, or world reload.

C3's bounded substrate-bonemeal transaction remains unchanged. It delegates only dry bottom native
Grass Block, Moss Block, and Pale Moss Block slabs to vanilla, consumes one item per valid
activation, restores the exact native slab, reconciles supported moss outputs, lowers generated
foliage, and retains waterlogged/unsupported and lowered-azalea safety boundaries.

## Runtime preconditions

1. Use the exact retained C5 artifact above and stop if filename, size, SHA-256, embedded version,
   or source checkpoint differs. Runtime validation is user-directed and does not require or imply
   a Test Slot, profile reservation, or deployment-manager transition.
2. Confirm a compatible unified BGE provider is the sole enabled owner of
   `more_slabs_stairs_and_walls`. C58, CNM 2.0.7, and the other exact identities above are
   controlled baselines, not permanent runtime pins. Do not enable a conflicting standalone
   provider.
3. Use a disposable Minecraft 26.2 Fabric world. Do not touch the protected Matcha Flavoured
   26.1.2 gameplay instance or the retired 26.2 Workbench profile, and do not infer runtime evidence
   from the controlled GameTests.

## Focused C5 runtime matrix

1. Place Hanging Roots and Spore Blossom naturally against eligible top, bottom, and double native
   slab undersides. Compare each with its canonical full block and a canonical-invalid material.
   Top-slab foliage must attach at exactly `+0.5 Y`; bottom/double cases must remain unshifted.
   Remove each support and confirm ordinary cleanup, drops, and replacement behavior.
2. Place Glow Berries/Cave Vines against an eligible top native slab, allow a multi-segment column
   to grow downward across head/body conversion, bonemeal it, harvest both berry and non-berry
   states, and verify light, item drop, random growth, neighbor updates, replacement, and cleanup.
   Every segment must share one anchor and exactly one `+0.5 Y` adjustment.
3. Place Weeping Vines against an eligible top native slab. Verify random and bonemeal growth,
   head/body lifecycle, maximum-age behavior, drops, replacement, and complete support-removal
   cleanup with no accumulated or duplicated offset.
4. With the compatible current Mynx × Regions Unexplored build present, grow its downward dropleaf
   from an eligible top native slab. Confirm it inherits the generic downward-growing contract,
   preserves its own head/body lifecycle, shares one anchor/offset, and needs no direct Slab
   Decorations-to-RU dependency. Also check pale hanging moss as the naturally equivalent vanilla
   same-block ceiling column.
5. Re-run C4 upward regressions for flowers, mushrooms, short/tall grass, ferns, double plants,
   big/small dripleaf, moss and pale-moss carpets, petals, azaleas, wither rose, nether wart, and
   sweet berries. Bottom slabs must remain exactly `-0.5 Y`; top/double slabs must remain unshifted.
6. For representative single plants and several segments of each column, inspect rendered model,
   outline, targeting/raycast, cached and context collision, visual and interaction shape,
   entity-inside/effect volume, breaking overlay, and particles. Confirm exact `+0.5 Y` beneath top
   slabs, exact `-0.5 Y` above bottom slabs, coherent crack/particle placement, and no double shift.
7. Verify waterlogged native slabs, canonical-invalid parents, vanilla/foreign slabs, vertical
   slabs, Steps, layers, stairs, walls, columns, side vines, glow lichen, lanterns, chains, pointed
   dripstone, redstone, rails, arbitrary ceiling blocks, upward-growing vines, kelp, seagrass, and
   other aquatic families gain no survival, placement, geometry, or growth behavior.
8. Recheck dry bottom native Grass Block, Moss Block, and Pale Moss Block bonemeal: exact one-item
   consumption, native-slab restoration, moss-output reconciliation, lowered vegetation, and
   lowered-azalea tree safety must remain unchanged.
9. Save, unload and reload the relevant chunks, restart the world, and repeat placement,
   harvesting, targeting, breaking, growth, and support removal. Inspect logs for Mixin, recursion,
   dependency, rendering, update, cleanup, or stale-state errors and confirm unrelated BGE/CNM
   geometry remains unchanged.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` for any artifact mismatch, canonical-parity error,
wrong or duplicated translation, disconnected child support, changed persisted identity/state,
broken vanilla growth/harvest/drop/cleanup, unsupported-geometry spread, reload drift, crash, or
relevant log error. Record only behavior actually observed; do not infer row-level results or
promote C4 or C5 from controlled validation.
