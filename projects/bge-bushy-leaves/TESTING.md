# BGE × Bushy Leaves — Canary 4 owner visual matrix

Test exactly `bge-bushy-leaves-0.2.2-canary4.jar`, SHA-256
`06147c8eb94c43f348da0a2876f1bb71edb9d356d08a28825b77c52bd17a3ac2`.

Canary 4 is world-rendering-only. It should retain the clean BGE inventory, held-item,
Creative-menu, JEI, and dropped-item models. It does not alter collision, selection, placement,
economy, waterlogging, leaf state/lifecycle, ticks, rain, pathfinding, redstone, BGE family
membership, or BGE × CTM behavior.

## Recorded predecessor evidence

Predecessor evidence is not evidence for C4. The owner tested exact C1
`bge-bushy-leaves-0.1.0-canary1.jar`, SHA-256
`e85262f792638e99b5c1fdac9c48043e5bb461da55ce9a50fbc07cc1a24f7fbf`: it loaded normally,
reached gameplay without a BGE × Bushy Leaves exception, and added no visible foliage. C1 remains
`RUNTIME_FAIL`/no-op.

The owner also supplied exact C2 runtime failure evidence for
`bge-bushy-leaves-0.2.0-canary2.jar`, SHA-256
`728bb10952642edb37f1c22e9a5514bf1499e4b2c7f97024c09ee88a9c1596eb`: chunk mesh building
raised a NullPointerException in Sodium's FRAPI `copyFrom` path, reached from
`PatchLocalFoliage.emitCard`. C2 is `RUNTIME_FAIL`; no visual geometry result is inferred.

The owner supplied exact C3 runtime failure evidence for
`bge-bushy-leaves-0.2.1-canary3.jar`, SHA-256
`e68097eb0199778ef2cb99adac1c83df85aa012c835667bc97fd77582fb8214a`: when Sodium meshed
visible chunks containing bushy-leaf cards, `QuadViewImpl.getQuadAtlas` raised a
NullPointerException because its backing `data` was null, via `QuadViewWrapper.atlas`,
`PatchLocalFoliage.copyAppearance`, and `PatchLocalFoliage.emitCard`. C3 is `RUNTIME_FAIL`; no
visual geometry result is inferred.

C4 has no Minecraft runtime observation yet. Do not mark a row passed from a build, launch,
resource reload, or headless test.

## Foundation active: first visual pass, shaders off

With the usual active stack, compare an untouched canonical Foundation full leaf to BGE derivatives.
Foundation's full-block replacement need not be reproduced. The expected relationship is:

- The canonical full leaf remains completely untouched and continues to display its active
  Foundation result.
- BGE geometry remains physically recognizable.
- The BGE form receives additive protruding foliage which feels consistent in family
  texture/color/tint with the canonical control.
- No foliage forms a cube-space/whole-block halo or attaches across obviously empty space.

Test Oak, Spruce or another tinted vanilla leaf, and Cherry. Test Silver Birch and Wisteria if the
provider model is present and BGE admits the family. For each family, look for wrong sprite,
incorrect biome tint, black/opaque cards, missing textures, z-fighting, flicker, or layer/shader
errors.

## Geometry matrix

For each available family, inspect from several angles and record exact state plus observations:

| Geometry | Required C4 check |
|---|---|
| Bottom Slab; Top Slab | Foliage follows only the occupied half and its exposed surfaces. |
| Stairs | Test multiple facings, TOP/BOTTOM, and STRAIGHT/INNER/OUTER. Treads, risers, sides, and undersides may be bushy; removed members may not. |
| Walls | Test post, LOW arm, TALL arm, and compound arms. Tiny arms must not receive a 16×16 tuft or internal duplicate cards. |
| Vertical Slabs | Test both orientations; empty halves remain unclaimed. |
| Steps | Test single and double states in several facings. |
| Layers | Test 1/4, 2/4, and 3/4 thickness/orientation. No card may be supported by the unused 12/16 or 8/16 volume. |
| Corners | Test every rotation; foliage follows only the occupied L-shaped form. |
| Quarter Columns | Test all occupancies, including the two diagonal forms. No whole-block fallback may appear. |

## Adjacency

Test canonical full leaf beside BGE leaf, equal BGE forms, different BGE forms of the same family,
leaf beside non-leaf full block, and partial beside partial. C4 deliberately does not broadly remove
foliage merely because a neighbor has the same canonical leaf material. It may suppress a complete
16×16 boundary only when the renderer itself supplies a reliable full-face cull result.

Watch especially for giant cube halos, cards over empty regions, dense tiny Wall/Column surfaces,
internal duplicates, or decorative foliage disappearing too aggressively at partial contacts.

## Resource reload and complementary

Reload resources with Foundation active and confirm BGE foliage uses the currently active canonical
appearance while retaining C4's BGE-aware geometry. With a different bushy pack, a plain
texture-only leaf pack, or a modded admitted leaf family, C4 should follow the final canonical
appearance when emitted: it must not require Foundation/Matcha filenames, `_bushy` paths, or an
Oak convention. A valid ordinary leaf face is an intentional fallback appearance.

After shader-off behavior is coherent, repeat representative Slab, Stair, Wall, Layer, Corner, and
Quarter Column cases with the current supported Complementary stack. Record exact resource-pack
order, BGE artifact/hash, C4 hash, family/state, shader state, and only direct observations. Owner
runtime evidence remains the only basis for changing C4 from `RUNTIME_UNTESTED`.
