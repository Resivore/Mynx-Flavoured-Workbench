# BGE × Bushy Leaves — recorded Canary 1 result and future C2 verification

## Recorded owner evidence: exact Canary 1 is a runtime no-op

The owner tested exactly `bge-bushy-leaves-0.1.0-canary1.jar`, SHA-256
`e85262f792638e99b5c1fdac9c48043e5bb461da55ce9a50fbc07cc1a24f7fbf`.

- BGE × Bushy Leaves loaded normally and BGE leaf families resolved.
- Minecraft reached normal gameplay.
- No BGE × Bushy Leaves exception or crash was observed.
- No visible bushy foliage was added to BGE leaf geometries.

This is a `RUNTIME_FAIL` for Canary 1's visible result, not a startup failure. No unreported
geometry row, shader result, pack-change result, or visual detail is implied.

The audit in `AUDIT.md` identifies the active source as Matcha Flavoured's baked
`minecraft:block/cross_leaves` model. Foundation v2 contains attempted replacement leaf
blockstates, but Minecraft Java 26.2 rejects their non-right-angle blockstate rotations before
they can select Foundation's `_bushy` models. Canary 1's rectangle-only detector then rejects
every active Matcha decorative quad as non-grid.

## C2 status

C2 does not yet exist, has not been built, and has no artifact or runtime result. The following is
the verification plan for a future implementation only. It must be revised if its implementation
or an owner-selected resource stack changes the audited source path.

## First C2 gate — source and mesh capture

With the exact active priority order from `AUDIT.md`, reload resources and compare ordinary Oak,
Spruce, and Cherry Leaves with their BGE forms. Confirm that the canonical controls still use their
normal Matcha cross-leaf presentation before evaluating BGE geometry. A C2 candidate must:

1. add foliage only where a BGE `SurfacePatch` exists;
2. preserve the active outer-top/outer-bottom sprites, tint behavior, cutout material, UVs, and
   two-sided presentation from the canonical baked mesh;
3. leave BGE inventory, hand, dropped-item, Creative-menu, and JEI models clean; and
4. introduce no missing texture, opaque plane, z-fighting, full-cube halo, or foliage attached to
   a BGE member that is not physically present.

Do not infer a pass from a clean launch, a resource reload, a build, or a headless fixture.

## Geometry matrix — shaders off

For each row, compare the BGE form with the nearest corresponding exposed region of a canonical
full leaf. Inspect from several angles because the current source is slanted cross-plane geometry,
not a six-face shell.

| Geometry | Required C2 observation |
|---|---|
| Bottom and top Slab | Foliage is supported by the occupied half only; the empty half receives no invented full-cube shell. |
| Stair | Test several facings, TOP/BOTTOM, STRAIGHT, INNER, and OUTER. Real tread, riser, side, and underside surfaces may carry the mapped exterior fragments; removed members may not. |
| Wall | Test post, LOW and TALL arms, and compound arms. No fragment may occupy a missing arm or duplicate an internal tile. |
| Vertical Slab / Step | Test several facings and single/double Steps. Each actual surface is eligible; empty half-cells remain undecorated. |
| Layer | Test 1/4, 2/4, and 3/4 occupancy in useful orientations. Decorative fragments remain anchored to the occupied boundary. |
| Corner / Quarter Column | Test every rotation and all six Quarter Column occupancies. No whole-block fallback may appear. |

Repeat Oak, Spruce, and Cherry. Test Silver Birch or Wisteria only after the active provider's own
lower-priority model resources are available for audit; the supplied pack copy proves Foundation's
Mynx overrides are malformed but does not contain that provider fallback.

## Contact and override checks

The active Matcha decorative planes have no `cullface`; their canonical appearance does not itself
suppress the eight decorative quads at a leaf contact. A C2 candidate must not invent broad
same-leaf culling merely because two block cells touch. Test canonical full leaf ↔ BGE leaf,
identical BGE geometry, different BGE geometry of the same canonical material, incompatible leaf
material, non-leaf, partial-to-full, and partial-to-partial contacts. Record only actual effects.

Temporarily removing or replacing the model-owning Matcha resource must cause C2 to follow the new
final canonical mesh or fail closed; it may not retain a cached Matcha sprite or a hard-coded cross
shape. A texture-only canonical treatment must add no invented geometry.

## Complementary and reporting

Repeat representative Slab, Stair, Wall, Layer, Corner, Quarter Column, and contact rows with the
normally supported Complementary stack. Record the exact C2 filename/SHA-256, BGE artifact/SHA-256,
resource-pack priority order, leaf family, geometry state, shader state, and only the observations
actually made. Runtime testing does not itself change lifecycle or accept a release.
