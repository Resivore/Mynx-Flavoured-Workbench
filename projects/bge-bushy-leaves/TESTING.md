# BGE × Bushy Leaves — Canary 1 runtime verification

Use the exact BGE × Bushy Leaves Canary and BGE C82 artifact recorded in `WORKBENCH_STATUS.json`.
Canary 1 is world-rendering only: inventory, creative-menu, JEI, held-item, and dropped-item
models must remain clean BGE models. Do not infer any PASS from a build, test, or clean launch.

## Preconditions and common checks

Enable the active bushy-leaf treatment normally used by the client, then place an ordinary canonical
full leaf control beside its BGE forms. Test Oak Leaves, a visibly different vanilla family such as
Spruce or Cherry Leaves, and Mynx Trees Silver Birch or Wisteria when currently admitted to BGE.
For every row, verify canonical texture/resource-pack override, tint, cutout/mips, animation,
lighting, two-sided appearance, and shader material remain coherent. There must be no missing
texture, black/opaque leaf plane, z-fighting, cube-space halo, foliage over an empty block portion,
or foliage through a covered/internal BGE surface.

## Geometry matrix — shaders off

| Geometry | Required observations |
|---|---|
| Bottom and top Slab | Bushiness follows the occupied half only. |
| Stair | Test several facings, TOP/BOTTOM, STRAIGHT, INNER, and OUTER: foliage follows real tread, riser, side, and underside without a removed-member halo. |
| Wall | Test minimal/post, LOW and TALL arms, plus compound arms: no foliage occupies a missing arm or duplicates internal tiles. |
| Vertical Slab / Step | Test multiple facings, single and double Steps: each real surface is decorated; empty half-cells stay empty. |
| Layer | Test 1/4, 2/4, and 3/4 occupancy in every useful orientation; foliage stays inside the occupied bounds. |
| Corner / Quarter Column | Test every rotation and all six Quarter Column occupancy arrangements; no full-cube projection appears. |

## Adjacency matrix

Test BGE leaf ↔ canonical full leaf, identical BGE geometry, two different BGE geometries of the
same canonical material, non-leaf, partial-to-full, and partial-to-partial contacts. Hidden contact
regions must suppress foliage only where actual BGE surface patches meet; exposed residual regions
must remain. Different canonical leaf materials must not be treated as connected.

## Complementary

Repeat representative Slab, Stair, Wall, Layer, Corner, Quarter Column, and partial-contact rows
with the current supported Complementary stack. Record only actual observations. Any runtime report
must name the precise Canary/BGE hashes, leaf provider/resource-pack stack, shader state, leaf family,
geometry state, and observed result. Acceptance remains an explicit owner decision.
