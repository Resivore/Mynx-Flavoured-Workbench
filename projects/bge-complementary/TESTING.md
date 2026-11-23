# Testing — BGE × Complementary Canary 6

## Current gate

**ACTIVE — STATIC_PASS — RUNTIME_UNTESTED**

Canary 6 is `bge-complementary-0.1.0+26.2-canary6.jar`, SHA-256
`0fb76735d1dea4d6261ae14d985c721799af42e101e573c1dbafce12c7de4ace`.
Build and synthetic tests do not establish Minecraft shader behavior. Do not
alter `originals/`, a protected Minecraft profile, Complementary, Iris, or the
saved shader configuration while using this matrix.

Canary 3 reached Mixin preparation but crashed before client startup with
`MixinTargetAlreadyLoadedException` for BGE × CTM's
`net.minecraft.world.level.block.Block` target. Its BGE API probe resolved that
target too early; Sodium's preLaunch entrypoint only exposed the failure. No
Canary 3 shader/material result was observed. Canary 6 retains Canary 4's late BGE API check at Iris's completed-map RETURN hooks.

## Exact stack and evidence to record

Use only the normal Minecraft 26.2 Fabric stack with:

- exact BGE C97 for this observed stack (SHA-256 `3e13d6f4dfe46edea753926da4e1168515e8432dce85d63a29b360a6aefc9408`) with the authoritative canonical-binding API
  (`BgeMaterialBindings.all()` plus `Binding#physicalBlock()`,
  `#canonicalMaterial()`, and `#canonicalState(BlockState)`);
- Iris `1.11.2+mc26.2` and its required Sodium baseline;
- Canary 6 bridge JAR above;
- Complementary Unbound r5.8.1, SHA-256
  `bb89b1fc54687d4147a837fb2e3c3f7261a13bee51819761e9b6a91cb7915965`;
- the existing shader configuration, including the recorded `FANCY_GLASS=true`
  control when testing Fancy Glass.

Record any compatibility warning at launch or reload. An unavailable or incompatible BGE canonical-binding API produces one warning per session. The bridge activates only for the exact supported Iris hook. Use the visual rows below to check the active pack's mappings.

Run every visual row first with shaders disabled. This is the control for
ordinary BGE geometry, texture, native render-layer, face-culling, and BGE ×
CTM behavior. Then run the same arrangement under Complementary. Record the
exact release hash, pack/settings, time/weather, coordinates, useful
screenshots, and each row as PASS, FAIL, or INCONCLUSIVE only.

## Manual matrix

| Row | Arrangement | Required observation |
| --- | --- | --- |
| Startup coexistence | Start the normal stack with BGE × CTM and Canary 6 installed; reach the client title screen before entering a world | No `MixinTargetAlreadyLoadedException` for BGE × CTM's `Block` target. This confirms Canary 6 did not resolve `Block` during Mixin bootstrap. |
| Clear glass | Canonical `minecraft:glass` beside BGE glass Layer, slab, stair, wall, Vertical Slab, Step, Corner, and Quarter Column where available | Each visible BGE form follows canonical glass's shader material. With Fancy Glass, distinguish a material mismatch from a missing/extra physical face or CTM seam. |
| Cyan stained glass | Canonical cyan stained glass beside matching BGE cyan Layer, slab, stair, and another available geometry | The BGE form follows cyan—not clear glass or another stain. Compare an adjacent different stained color as a color control. |
| Magma Block | Place canonical `minecraft:magma_block` beside several BGE magma geometries available in the stack, then repeat with shaders disabled | Under Complementary, each BGE magma form receives canonical magma shader-material treatment. With shaders off, geometry, texture, and ordinary BGE behavior remain unchanged. |
| Another canonical material | Choose a BGE-bound canonical material other than the glass and magma rows; place it beside at least two BGE geometry roles | If Complementary maps its canonical parent, the BGE forms receive that exact parent treatment. If the parent is unmapped, no material inheritance is expected. |
| State-specific parent | Use a BGE material whose authoritative canonical projection carries material state, and compare two visibly distinct canonical states beside matching BGE forms | Each BGE physical state follows its projected canonical state, not a generic default parent state or another state in the same block. |
| Explicit physical precedence | If a shader-pack test setup explicitly maps a BGE physical state or block, compare it to its canonical parent | The physical assignment remains visible; Canary 6 must not replace it. Record the temporary test arrangement without editing upstream/archive files. |
| Shader disable/re-enable | Repeat one passing glass, magma, and another-canonical-material row | Disabled shaders use normal BGE rendering; re-enabling rebuilds only the active pack's map and shows current canonical-parent classification. |
| Shader reload / pack switch | Reload Complementary, then switch to one different shader pack or shaders-off | No Complementary classification survives in the other pack or shaders-off control. |
| Leave/rejoin and restart | Repeat the magma comparison and one state-specific row after world leave/rejoin, then full client restart | Stable mapping after each lifecycle; no crash, stale material, or Mixin failure. |

## Ownership boundary

This bridge owns only completed-Iris-map canonical-parent fallback. BGE owns
physical-to-canonical identity, state projection, geometry, and native
render-layer setup; BGE × CTM owns Continuity appearance and connections; the
active shader pack owns every canonical and explicit physical mapping. Runtime
evidence does not change `ACTIVE` to `ACCEPTED` without an explicit owner
decision.
