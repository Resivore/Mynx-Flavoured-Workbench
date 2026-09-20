# BGE × Bushy Leaves — Canary 6 owner runtime procedure

Test exactly `bge-bushy-leaves-0.2.4-canary6.jar`, SHA-256
`af52f88d6637b360038b87cbbca97244f64e3af6cb4d7907c480763ab9b5b376`.

Canary 6 is world-rendering-only. It leaves canonical full-leaf rendering provider-owned and does
not alter inventory, held-item, Creative-menu, JEI, dropped-item, collision, selection, placement,
drops, recipes, waterlogging, leaf lifecycle, ticks, rain, pathfinding, redstone, or BGE × CTM.

## First gate — crash-free world rendering

1. Launch with the exact C6 JAR above and BGE compile-evidence artifact `BGE C89.jar`, SHA-256
   `cc1368f48fffcde03997c4d3f1d0e8907236c584d7ae107f35eb4e06e98667f2`.
2. Enter a world containing BGE leaf geometry and allow surrounding chunks to mesh.
3. Move and turn enough to trigger additional chunk rebuilds.
4. Record whether the client remains alive and whether any Bushy Leaves, Sodium, or FRAPI exception occurs.

This is only a smoke check. Record the exact stack, resource-pack order, renderer/shader state,
world/state, and direct observations; a build or launch alone is not a visual or tint pass.

## Second gate — coherent foliage composition

With shaders off, compare an untouched canonical full leaf with BGE derivatives. C6 should read as
one partial/derived bushy leaf volume: fixed world-axis crossed/slanted planes are clipped to the
BGE occupied volume, rather than a separate shrub attached to every exposed face. Confirm that the
physical shape remains recognizable and that empty regions are not replaced by a full-block halo.

| Geometry | Required C6 check |
|---|---|
| Slab | Bottom and top forms read as half-height bushy leaves. |
| Stair | Test several facings, TOP/BOTTOM, and STRAIGHT/INNER/OUTER states. |
| Wall | Test post, LOW arm, TALL arm, and compound arms as coherent leafy volume. |
| Vertical slab | Test both orientations; the empty half remains unclaimed. |
| Step | Test single and double states in several facings. |
| Layer | Test 1/4, 2/4, and 3/4 thickness/orientation. |
| Corner | Test every rotation as an L-shaped leafy form. |
| Quarter column | Test every occupancy, including both diagonal forms, with no whole-block fallback. |

Look for repeated face-local tufts, foliage growing perpendicular to every vertical face, a wrong
sprite, black/opaque cards, missing textures, z-fighting, flicker, or layer/shader errors.

## Third gate — tint behavior

Verify that generated decorative foliage follows the sampled canonical tint metadata:

1. One foliage-biome family: move between contrasting biomes and confirm the decorative planes
   change with the canonical leaf.
2. Spruce: confirm the fixed spruce tint stays correct.
3. Birch: confirm the fixed birch tint stays correct.
4. One untinted or admitted modded leaf family, if applicable: confirm the decorative planes remain
   untinted rather than inheriting a foliage color.

## Adjacency, reload, and shaders

Test canonical full leaf beside BGE leaf, equal/different BGE forms of the same family, leaf beside
non-leaf full block, and partial beside partial. C6 must not broadly remove foliage because a
neighbor shares canonical material. Reload resources with Foundation active and then with another
bushy, plain, or admitted modded leaf source; C6 must follow the final canonical appearance without
Foundation/Matcha filenames, `_bushy` paths, or Oak conventions. After shader-off behavior is
coherent, repeat representative Slab, Stair, Wall, Layer, Corner, and Quarter Column cases with the
current supported Complementary stack. Record exact details and direct observations only.

## Recorded predecessor evidence

Exact C5 `bge-bushy-leaves-0.2.3-canary5.jar`, SHA-256
`13f273c3b4d9555ea09377a003f3977086c9424ed00b0d1dedc9dabab210952e`, reached in-world rendering
without immediately reproducing the retained-`QuadView` crash path. Its visual output was a design
failure: many small face-local tufts instead of a coherent bushy volume, and biome tint was not
preserved correctly. This is C5 `RUNTIME_FAIL` relative to design intent, not a broad runtime PASS.
C1–C4 remain `RUNTIME_FAIL` for their separately recorded no-op or renderer-lifetime crashes.
