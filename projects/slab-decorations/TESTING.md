# Testing

C3 (`0.1.0-canary3`) is the exact current candidate, remains independently
`RUNTIME_UNTESTED`, and is not deployed. Retain only
`slab-decorations-0.1.0-canary3.jar`, 36,081 bytes, SHA-256
`5DFE24CF686A781C16DCA46A11EF4112D7473E1F8D63FAC659CB750C6709BF2B`,
from source checkpoint `2358342ebbd51cdd1b110e74e815205a68acc882`.
Manager revision 68 removed its former revision-67 Slot B deployment
`d6350d3f-bf00-4955-8b27-3f186dace93f` / artifact
`a80957e9-041d-4758-96fa-fb6868051257` without promotion or a runtime result.

C3 is a metadata-only compatibility successor to C2. Comparison of the two JARs found 31 entries: only the root `fabric.mod.json` changed, and the other 30 code/resource entries are byte-identical. The root descriptor now requires the stable `more_slabs_stairs_and_walls` provider ID with a flexible predicate and keeps `clutternomore` and `cnm_terrain_slabs_compat` as flexible optional suggestions. The final enabled graph uses exact unified BGE C58, `cnm-nibaru-integration-4.2.2-bge.canary58.glass-corner-uv+26.2.jar`, SHA-256 `1A4E4D1CD9C8709720EC84975E70CAFFB5552AC676537B9BBAE42DCA96567E87`, as the sole provider of both BGE and legacy Nibaru IDs; no standalone Nibaru artifact is enabled.

Controlled validation used exact BGE C58, CNM 2.0.7, Terrain Slabs 3.3.2, Java 25, Gradle 9.5.1, and Loom 1.17.19 only as build/test baselines. The focused artifact-contract suite passed 1/1 JUnit test, and the Fabric runner passed all 16 required GameTests. Those results demonstrate unchanged packaging and automated behavior; they are not Minecraft gameplay/runtime evidence. Minecraft was not launched for this transition.

## Runtime preconditions

1. Before any future runtime test, use a new serialized manager assignment for exact C3 and require `CURRENT_RELEASE_DEPLOYED / READY_TO_TEST_VERIFIED` with a fresh `UNTESTED` slot result. Revision 67 is historical deployment evidence only; stop if C3 is not currently assigned or if its filename, SHA-256, embedded version, or source checkpoint differs.
2. Confirm exact unified BGE C58 remains the sole enabled provider of `cnm_terrain_slabs_compat` and `more_slabs_stairs_and_walls`, and that no standalone Nibaru artifact is enabled. The exact C58/CNM/test-tool identities above are controlled-test baselines, not permanent exact runtime pins for future compatible providers.
3. Use a disposable Minecraft 26.2 Fabric test world in the dedicated Workbench only. Do not touch the protected gameplay profile, rebuild or substitute either artifact, infer a result from automated tests, or mutate the accepted baseline or the independent Slot A candidate.

## Focused C3 runtime matrix

1. In Survival, bonemeal exact dry bottom native Grass Block, Moss Block, and Pale Moss Block slabs. Each valid activation must consume exactly one bonemeal and leave the clicked exact native bottom slab intact.
2. For Grass, compare with a nearby full Grass Block in the same biome. Confirm representative vegetation and biome flower/petal generation, with generated decorations represented on the real `+0.5` slab surface.
3. For Moss, compare with a full Moss Block. Confirm representative spread and vegetation, and require every eligible canonical replacement to reconcile to the exact corresponding bottom native moss slab.
4. For Pale Moss, compare with a full Pale Moss Block. Confirm representative spread, growth, and carpet behavior, with eligible canonical replacements reconciled to exact bottom native pale-moss slabs.
5. Check generated short/tall grass, flowers, pink petals, wildflowers, azalea/flowering azalea, moss carpet, and pale-moss carpet. Verify model position, outline/collision where applicable, targeting, Survival break/drop, support-removal cleanup, and save/reload at the half-height surface. Bonemealing a lowered generated azalea must not consume the item or grow a floating tree.
6. Confirm unsupported materials and waterlogged bottom slabs do not activate or consume bonemeal. Confirm top/double slabs, vertical slabs, CNM Steps, stairs, walls, and unrelated geometry gain no substrate spread, reconciliation, or offset behavior.
7. Recheck the preserved C1 behavior carried through C2 and C3: short-grass and fern bonemeal must still produce coherently lowered tall grass/large fern and clean up after support removal; representative flowers, dead bush, and nether wart must preserve placement, survival, growth/harvest, targeting, and non-bonemeal behavior.
8. Save and reload representative C3 results, then inspect logs for Mixin application, registry, projection-restoration, reconciliation, rendering, targeting, collision, or dependency errors. Confirm unrelated BGE/CNM geometry, Slot A behavior, and accepted-stack behavior remain unchanged.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` for any identity/readiness mismatch, wrong item consumption, lost or changed slab identity, canonical full-block residue, spread into unsupported geometry/material, waterlogged activation, floating tree, invalid generated decoration, stale representation after reload, preserved-behavior regression, crash, or relevant error. Record only behavior actually observed; do not infer individual matrix rows or promote C3 from controlled validation.
