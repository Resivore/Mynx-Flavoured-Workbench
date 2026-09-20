# BGE × Bushy Leaves — Canary 5 owner runtime procedure

Test exactly `bge-bushy-leaves-0.2.3-canary5.jar`, SHA-256
`13f273c3b4d9555ea09377a003f3977086c9424ed00b0d1dedc9dabab210952e`.

Canary 5 is world-rendering-only. It keeps clean BGE inventory, held-item, Creative-menu, JEI,
and dropped-item models. It does not alter collision, selection, placement, economy, waterlogging,
leaf state/lifecycle, ticks, rain, pathfinding, redstone, or BGE × CTM behavior.

## First gate — C5 chunk-meshing smoke test

This Canary first needs to become testable. Before any visual matrix:

1. Launch with the exact C5 JAR above.
2. Enter a world containing or near BGE leaf geometry.
3. Allow surrounding chunks to finish meshing.
4. Move and turn enough to force additional chunk rebuilds.
5. Record whether the client remains alive and whether any Bushy Leaves, Sodium, or FRAPI exception occurs.

Record the exact BGE artifact/hash, resource-pack order, renderer/shader state, world/state, and
only direct observations. A successful launch or controlled build does not itself pass this gate.

## Recorded predecessor evidence

Predecessor evidence does not apply to C5. The owner tested exact C1
`bge-bushy-leaves-0.1.0-canary1.jar`, SHA-256
`e85262f792638e99b5c1fdac9c48043e5bb461da55ce9a50fbc07cc1a24f7fbf`: it loaded, reached
gameplay, raised no project exception, and added no visible foliage. C1 remains `RUNTIME_FAIL`/no-op.

C2 `bge-bushy-leaves-0.2.0-canary2.jar`, SHA-256
`728bb10952642edb37f1c22e9a5514bf1499e4b2c7f97024c09ee88a9c1596eb`, failed while chunk meshing
in Sodium's FRAPI `copyFrom` path. C3 `bge-bushy-leaves-0.2.1-canary3.jar`, SHA-256
`e68097eb0199778ef2cb99adac1c83df85aa012c835667bc97fd77582fb8214a`, failed after an unsafe
wrapped-quad `atlas()` access. C4 `bge-bushy-leaves-0.2.2-canary4.jar`, SHA-256
`06147c8eb94c43f348da0a2876f1bb71edb9d356d08a28825b77c52bd17a3ac2`, launched and entered a
world, then failed as chunk meshing began: `PatchLocalFoliage.copyAppearance` read `chunkLayer()`
from a retained transient source `QuadView`; Sodium's wrapper/backing data was no longer valid and
raised a `NullPointerException`. C2, C3, and C4 are `RUNTIME_FAIL`.

Do not mark C5 passed from a build, a launch, resource reload, or a headless test.

## Visual matrix — only after the smoke test passes

With the usual active stack and shaders off, compare an untouched canonical full leaf to BGE
derivatives. The canonical full leaf remains provider-owned. The BGE form should remain physically
recognizable and receive additive foliage consistent with the final canonical texture/color/tint,
with no cube-space halo or attachment over empty space.

Use Oak, Spruce or another tinted vanilla leaf, and Cherry. Test Silver Birch and Wisteria if the
provider model is present and BGE admits the family. Look for wrong sprite, biome tint, black or
opaque cards, missing textures, z-fighting, flicker, or layer/shader errors.

| Geometry | Required C5 check |
|---|---|
| Bottom Slab; Top Slab | Foliage follows only the occupied half and exposed surfaces. |
| Stairs | Test several facings, TOP/BOTTOM, and STRAIGHT/INNER/OUTER states. |
| Walls | Test post, LOW arm, TALL arm, and compound arms; tiny arms must not grow 16×16 tufts. |
| Vertical Slabs | Test both orientations; empty halves remain unclaimed. |
| Steps | Test single and double states in several facings. |
| Layers | Test 1/4, 2/4, and 3/4 thickness/orientation with no support over unused volume. |
| Corners | Test every rotation; foliage follows only the occupied L-shaped form. |
| Quarter Columns | Test every occupancy, including both diagonal forms; no whole-block fallback. |

## Adjacency, reload, and shaders

Test a canonical full leaf beside a BGE leaf, equal and different BGE forms of the same family,
leaf beside non-leaf full block, and partial beside partial. C5 must not broadly remove foliage
because a neighbor shares canonical leaf material; only a renderer-supplied complete 16×16 cull
face may suppress a boundary card.

Reload resources with Foundation active and then with another bushy pack, a plain texture-only leaf
pack, or a modded admitted leaf family. C5 must follow the final canonical appearance without
Foundation/Matcha filenames, `_bushy` paths, or Oak conventions. After shader-off behavior is
coherent, repeat representative Slab, Stair, Wall, Layer, Corner, and Quarter Column cases with
the current supported Complementary stack. Record exact stack details and direct observations only.
