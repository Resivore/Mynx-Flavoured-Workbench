# Testing — BGE × Complementary Canary 1

## Current gate

**ACTIVE — STATIC_PASS — RUNTIME_UNTESTED**

Canary 1 is `bge-complementary-0.1.0+26.2-canary1.jar`, SHA-256
`e508f19d4e24d8eb0d3f9c72f24e1898dbe30bc717777c4b548cbf387a113a7b`.
Build and synthetic tests do not establish Minecraft shader behavior. Do not
alter `originals/`, a protected Minecraft profile, Complementary, or Iris while
using this matrix.

## Exact stack and evidence to record

Use only the normal Minecraft 26.2 Fabric stack with:

- BGE C79 `4.2.23-bge.canary79.cnm-family-bridge+26.2`;
- Iris `1.11.2+mc26.2` and its required Sodium baseline;
- Canary 1 bridge JAR above;
- Complementary Unbound r5.8.1, SHA-256
  `bb89b1fc54687d4147a837fb2e3c3f7261a13bee51819761e9b6a91cb7915965`;
- the existing shader configuration, including the recorded `FANCY_GLASS=true`
  control when testing Fancy Glass.

At every shader-enabled launch or reload, preserve the aggregate bridge log
line. It must report the loaded Iris/BGE versions and nonzero inherited state
count for a parent the active pack maps. An absent/mismatched Iris or BGE
version intentionally produces no bridge hook.

First run each visual row with shaders disabled. This separates ordinary BGE
geometry, translucent rendering, face culling, and BGE × CTM behavior from a
shader-material result. Then run it under Complementary. Record exact release
hash, pack/settings, time/weather, coordinates, screenshots from useful sides,
and each row as PASS, FAIL, or INCONCLUSIVE only.

## Manual matrix

| Row | Arrangement | Required observation |
| --- | --- | --- |
| Clear glass | Canonical `minecraft:glass` beside BGE glass Layer, slab, stair, wall, Vertical Slab, Step, Corner, and Quarter Column where available | Each visible BGE form follows canonical glass's shader material. With Fancy Glass, distinguish a material mismatch from a missing/extra physical face or CTM seam. |
| Cyan stained glass | Canonical cyan stained glass beside matching BGE cyan Layer, slab, stair, and another available geometry | The BGE form follows cyan—not clear glass or another stain. Compare an adjacent different stained color as a negative color-control. |
| Iron | Canonical iron block beside BGE iron slab/stair/wall | Equivalent reflective/smooth material treatment without changing correct BGE geometry or texture orientation. |
| Gold or diamond | Canonical gold or diamond block beside at least two BGE geometry roles | The exact chosen parent classification is inherited, with no shared hard-coded metal/gem ID. |
| Glowstone or sea lantern | Canonical parent beside at least two BGE roles | Equivalent parent material/emission classification; do not infer fake block-light behavior. |
| Explicit physical precedence | If a shader-pack test setup explicitly maps a BGE physical state or block, compare it to its canonical parent | The physical assignment remains visible; Canary 1 must not replace it. Record the temporary test arrangement and remove no upstream/archive files. |
| Shader disable/re-enable | Repeat one passing glass and one solid row | Disabled shaders use normal BGE rendering; re-enabling rebuilds only the active pack's map and shows current classification. |
| Shader reload / pack switch | Reload Complementary, then switch to one different shader pack or shaders-off | The log emits a new aggregate construction line. No Complementary classification survives in the other pack or shaders-off control. |
| Leave/rejoin and restart | Repeat one glass and one solid after world leave/rejoin, then full client restart | Stable mapping after each lifecycle; no crash, stale material, or Mixin failure. |

## Withheld classes

Do not treat leaves, foliage, vines, crops, waving plants, fluids, lily-pad or
water-like classes, portals, beacons, or block-entity paths as Canary 1 parity
successes. A future expansion needs separate exact visual evidence and a new
canonical-parent policy decision.

## Ownership boundary

This bridge owns only Iris's inherited material/layer map fallback. BGE owns
geometry and native render-layer setup; BGE × CTM owns Continuity appearance,
connections, and physical contact; shader packs own their explicit mappings.
Runtime evidence does not change `ACTIVE` to `ACCEPTED` without an explicit
owner decision.
