# Testing — BGE × Complementary Canary 4

## Current gate

**ACTIVE — STATIC_PASS — RUNTIME_UNTESTED**

Canary 4 is `bge-complementary-0.1.0+26.2-canary4.jar`, SHA-256
`9d48cd2a11f038f74685a9c1265206df452e764c763960482d3343b97b9c9e04`.
Build and synthetic tests do not establish Minecraft shader behavior. Do not
alter `originals/`, a protected Minecraft profile, Complementary, Iris, or the
saved shader configuration while using this matrix.

Canary 3 reached Mixin preparation but crashed before client startup with
`MixinTargetAlreadyLoadedException` for BGE × CTM's
`net.minecraft.world.level.block.Block` target. Its BGE API probe resolved that
target too early; Sodium's preLaunch entrypoint only exposed the failure. No
Canary 3 shader/material result was observed. Canary 4 moves that probe to
Iris's completed-map RETURN hooks and must be tested as a new, runtime-untested
release.

## Exact stack and evidence to record

Use only the normal Minecraft 26.2 Fabric stack with:

- BGE that is present and exposes the authoritative canonical-binding API
  (`BgeMaterialBindings.all()` plus `Binding#physicalBlock()`,
  `#canonicalMaterial()`, and `#canonicalState(BlockState)`); current static
  validation baseline: C80 `4.2.24-bge.canary80.cnm-two-phase+26.2`;
- Iris `1.11.2+mc26.2` and its required Sodium baseline;
- Canary 4 bridge JAR above;
- Complementary Unbound r5.8.1, SHA-256
  `bb89b1fc54687d4147a837fb2e3c3f7261a13bee51819761e9b6a91cb7915965`;
- the existing shader configuration, including the recorded `FANCY_GLASS=true`
  control when testing Fancy Glass.

At every shader-enabled launch or reload, preserve the aggregate bridge log
line. It must report the loaded Iris/BGE versions and a nonzero inherited
state count whenever the active pack maps a canonical parent represented by a
placed BGE form. Iris other than the exact supported hook version produces no
bridge hook. A BGE release without the required API produces one controlled
warning and leaves Iris maps untouched; a different BGE version string alone
must not suppress the bridge when that API is present.

Run every visual row first with shaders disabled. This is the control for
ordinary BGE geometry, texture, native render-layer, face-culling, and BGE ×
CTM behavior. Then run the same arrangement under Complementary. Record the
exact release hash, pack/settings, time/weather, coordinates, useful
screenshots, and each row as PASS, FAIL, or INCONCLUSIVE only.

## Manual matrix

| Row | Arrangement | Required observation |
| --- | --- | --- |
| Startup coexistence | Start the normal stack with BGE × CTM and Canary 4 installed; reach the client title screen before entering a world | No `MixinTargetAlreadyLoadedException` for BGE × CTM's `Block` target. This confirms Canary 4 did not resolve `Block` during Mixin bootstrap. |
| Clear glass | Canonical `minecraft:glass` beside BGE glass Layer, slab, stair, wall, Vertical Slab, Step, Corner, and Quarter Column where available | Each visible BGE form follows canonical glass's shader material. With Fancy Glass, distinguish a material mismatch from a missing/extra physical face or CTM seam. |
| Cyan stained glass | Canonical cyan stained glass beside matching BGE cyan Layer, slab, stair, and another available geometry | The BGE form follows cyan—not clear glass or another stain. Compare an adjacent different stained color as a color control. |
| Magma Block | Place canonical `minecraft:magma_block` beside several BGE magma geometries available in the stack, then repeat with shaders disabled | Under Complementary, each BGE magma form receives canonical magma shader-material treatment. With shaders off, geometry, texture, and ordinary BGE behavior remain unchanged. |
| Another canonical material | Choose a BGE-bound canonical material other than the glass and magma rows; place it beside at least two BGE geometry roles | If Complementary maps its canonical parent, the BGE forms receive that exact parent treatment. If the parent is unmapped, no material inheritance is expected. |
| State-specific parent | Use a BGE material whose authoritative canonical projection carries material state, and compare two visibly distinct canonical states beside matching BGE forms | Each BGE physical state follows its projected canonical state, not a generic default parent state or another state in the same block. |
| Explicit physical precedence | If a shader-pack test setup explicitly maps a BGE physical state or block, compare it to its canonical parent | The physical assignment remains visible; Canary 4 must not replace it. Record the temporary test arrangement without editing upstream/archive files. |
| Shader disable/re-enable | Repeat one passing glass, magma, and another-canonical-material row | Disabled shaders use normal BGE rendering; re-enabling rebuilds only the active pack's map and shows current canonical-parent classification. |
| Shader reload / pack switch | Reload Complementary, then switch to one different shader pack or shaders-off | The log emits a new aggregate construction line. No Complementary classification survives in the other pack or shaders-off control. |
| Leave/rejoin and restart | Repeat the magma comparison and one state-specific row after world leave/rejoin, then full client restart | Stable mapping after each lifecycle; no crash, stale material, or Mixin failure. |

## Ownership boundary

This bridge owns only completed-Iris-map canonical-parent fallback. BGE owns
physical-to-canonical identity, state projection, geometry, and native
render-layer setup; BGE × CTM owns Continuity appearance and connections; the
active shader pack owns every canonical and explicit physical mapping. Runtime
evidence does not change `ACTIVE` to `ACCEPTED` without an explicit owner
decision.
