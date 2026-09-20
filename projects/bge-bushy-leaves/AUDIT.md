# BGE × Bushy Leaves — Canary 1 runtime no-op audit

## Finding

Canary 1 is a confirmed runtime no-op for its intended visual result. The owner tested exactly
bge-bushy-leaves-0.1.0-canary1.jar, SHA-256
e85262f792638e99b5c1fdac9c48043e5bb461da55ce9a50fbc07cc1a24f7fbf. BGE × Bushy Leaves loaded,
BGE leaf families resolved, and Minecraft reached normal gameplay with no BGE × Bushy Leaves crash
or exception. No visible bushy foliage was added to BGE leaf geometries. This record says nothing
about unreported geometry rows.

The active Oak/Spruce/Cherry bushiness is not supplied by Foundation v2 at runtime. Foundation's
higher-priority leaf blockstates are malformed for Minecraft Java 26.2, so the game skips them. The
valid fallback blockstate selects Matcha Flavoured's lower-priority leaf model, whose
minecraft:block/cross_leaves parent supplies eight slanted, non-grid decorative quads. C1's
rectangle-only filter rejects all eight; its six ordinary core-cube quads are correctly rejected as
non-exterior. The demonstrated result is 0 admitted decorative quads out of 8 and 0 admitted quads
out of the 14-quad final Oak model.

C2 is planned only. This audit implements no code, creates no new artifact, and makes no C2 build
or runtime claim.

## Audit inputs and boundary

All pack inspection was read-only against originals/resourcepacks. No ZIP was extracted in place,
renamed, normalized, modified, copied into the repository, or redistributed. No Minecraft profile
was inspected, selected, launched, or changed.

The owner supplied this relevant resource-pack order, high to low:

1. Matcha-Overlays-v37.zip
2. Mizuno_Fresh_Animations_Condensed_26.2.zip
3. Foundation v2.zip
4. FreshAnimations_v1.10.5.zip
5. translucent-glass/translucent-glass
6. Serified Font v13.zip
7. Matcha_Flavoured_1_12.zip

The supplied folder has Serified Font v1.2.zip; its metadata says formats 15–1024. No supplied copy
exists at originals/resourcepacks/translucent-glass/translucent-glass. That pack was not recovered
from a protected profile and has no demonstrated leaf resource in this audit.

| Pack | Supplied metadata | Relevant result |
|---|---|---|
| Matcha Overlays v37 | formats 69–999 | No Oak/Spruce/Cherry/Mynx leaf blockstate, model, or texture candidate. |
| Mizuno + Fresh Animations Condensed 26.2 | formats 84–88 | No relevant leaf candidate. |
| Foundation v2 | formats 88.0–88 | Has attempted vanilla/Mynx bushy selectors, models, and textures, but the selectors are invalid in 26.2. |
| Fresh Animations 1.10.5 | formats 84–999 | Entity CEM/animation resources; no relevant leaf candidate. |
| translucent-glass | supplied copy absent | No assertion possible beyond that absence. |
| Serified Font | formats 15–1024 | No relevant leaf candidate. |
| Matcha Flavoured 1.12 for 26.2 | formats 88–107.1 | Supplies the valid final vanilla leaf models, cross_leaves parent, and active control textures. |

The inspected Foundation ZIP has SHA-256
d5a594b47501befacc3307e4ab8ac74dbc6a446966b66c31f830c21d064d7994; the inspected Matcha ZIP has
SHA-256 6209783021c358044abedabacee471faff5bd4080437d4e3b5e51963f1804248.

## Effective resource resolution

### Foundation's apparent override is rejected

Foundation contains these high-priority blockstates:

- assets/minecraft/blockstates/oak_leaves.json
- assets/minecraft/blockstates/spruce_leaves.json
- assets/minecraft/blockstates/cherry_leaves.json
- assets/mynx_trees/blockstates/silver_birch_leaves.json
- assets/mynx_trees/blockstates/wisteria_leaves.json

Each names 32 variants with x, y, and z values such as −18, −24, and 41 degrees. The vanilla files
contain 95 non-quadrant angle fields out of 96; the two Mynx files contain 96 out of 96. All 32
variants in every file are invalid.

The exact Minecraft 26.2 merged JAR used by this project defines com.mojang.math.Quadrant with only
R0, R90, R180, and R270. Its JSON codec accepts only 0, 90, 180, or 270 modulo 360; another value
produces a parse error. The current BlockStateModelLoader catches the per-pack resource error, logs
Failed to load blockstate definition from pack, skips that invalid stack member, and continues with
valid members. This explains how gameplay reaches normal operation while Foundation's intended
selectors never become a model.

Foundation's models/block/*_leaves_bushy.json models and bushy pixels are therefore not active for
the three vanilla controls. Its Polytone file is only
assets/minecraft/polytone/biome_modifiers/lush_caves.json, not a leaf model operation.

### Final controls

| Canonical control | Candidate path / disposition | Winning resource chain | What supplies the visual |
|---|---|---|---|
| minecraft:oak_leaves | Foundation blockstates/oak_leaves.json is highest but rejected; its oak_leaves_bushy model is unreachable. | Vanilla blockstates/oak_leaves.json → Matcha models/block/oak_leaves.json → Matcha models/block/cross_leaves.json → Matcha oak_leaves.png, oak_leaves_outer_top.png, and oak_leaves_outer_bottom.png. | Matcha core cube plus slanted outer cutout planes. |
| minecraft:spruce_leaves | Foundation blockstates/spruce_leaves.json is rejected for the same 32 invalid variants. | Vanilla blockstates/spruce_leaves.json → Matcha models/block/spruce_leaves.json → Matcha cross_leaves.json → Matcha spruce base/outer textures. | Same authored geometry with Spruce sprites. |
| minecraft:cherry_leaves | Foundation blockstates/cherry_leaves.json is rejected for the same 32 invalid variants. | Vanilla blockstates/cherry_leaves.json → Matcha models/block/cherry_leaves.json → Matcha cross_leaves.json → Matcha cherry base/outer textures. | Same authored geometry with Cherry sprites. |
| mynx_trees:silver_birch_leaves | Foundation blockstates/silver_birch_leaves.json is rejected; it names Foundation's bushy model. | The lower-priority Mynx Trees provider blockstate/model is not supplied, so its post-failure winner cannot be asserted. | No active visual claim. |
| mynx_trees:wisteria_leaves | Foundation blockstates/wisteria_leaves.json is rejected for all 32 variants. | Same unavailable-provider limitation. | No active visual claim. |

BGE's current external-material catalog does admit the Silver Birch and Wisteria canonical leaf
families. That confirms BGE eligibility only; it cannot establish an absent provider resource chain.
This limitation is intentionally not resolved by inspecting any Minecraft profile.

## Active bushiness mechanism

The active vanilla mechanism is A — authored baked-model geometry. It is not texture-only, CTM,
shader, or a resource-driven runtime model transformation.

Each Matcha models/block/<family>_leaves.json uses block/cross_leaves. That parent has five elements
and emits 14 quads:

| Element | Bounds before element rotation | Rotation about [8,8,8] | Faces / quads | Function |
|---:|---|---|---:|---|
| 1 | [-8,-8,8] → [24,24,8] | Y +36° | north/south, 2 | outer-bottom decorative plane |
| 2 | [-8,-8,8] → [24,24,8] | Y −12° | north/south, 2 | outer-top decorative plane |
| 3 | [0,0,0] → [16,16,16] | none | six faces, 6 | ordinary cull-faced core cube |
| 4 | [8,-8,-8] → [8,24,24] | Y −12° | east/west, 2 | outer-top decorative plane |
| 5 | [8,-8,-8] → [8,24,24] | Y +36° | east/west, 2 | outer-bottom decorative plane |

The eight decorative quads are two-sided, vertical non-cull planes. Before element rotation they
extend 8/16 above and below the cube and span 32×32. The ±12°/36° rotations make their X/Z
coordinates non-grid and their normal-axis coordinate vary across each quad. Approximate side
overhangs are 7.650/16 for the −12° primary direction and 4.944/16 or 1.405/16 for the +36° pair.
The quads are neither face-parallel nor associated with one stable canonical cube face: their
exterior portions reach multiple side/top/bottom regions. A baked lightFace retains local lighting
information but cannot be used as whole-primitive ownership.

The core cube has the normal six 0..16 grid-aligned faces with cullface. The decorative planes do
not have cullface, so this active system does not itself suppress those eight planes merely because
two leaves are adjacent.

Foundation's unreachable template_leaves is different: it has three unrotated 32×32 planes from
−8 to 24 along X/Y/Z, six decorative faces, dark-cutout mipmap metadata, and bushy sprites. That
geometry would be a valid source if selected, but the malformed Foundation selectors prevent it from
becoming the active 26.2 model.

## Model/render-stage audit

C1 registers Fabric's public ModelLoadingPlugin.Context.modifyBlockModelAfterBake event. At that
stage Minecraft has parsed the resource stack, resolved model inheritance, and baked the selected
BlockStateModel. For each valid vanilla control, the model already includes the 14 Matcha quads.

C1 captures the canonical model, then BushyLeafBlockStateModel asks that captured model to emitQuads
into a Fabric MutableMesh. The source representation is therefore baked QuadView data: vertex
positions, UVs, tint, sprite/material/render-layer state, light, normals, lightFace, and cull
metadata. No supplied late model provider, CTM system, or shader introduces the active geometry
after this model stage. The current no-op is neither a too-early nor a too-late capture for
Oak/Spruce/Cherry; the detector is the cause.

There is a general ordering limitation. C1 captures in Fabric DEFAULT_PHASE, even though it wraps
BGE in a custom phase after WRAP_LAST_PHASE. An optional model modifier ordered after DEFAULT_PHASE
could be omitted from C1's stored canonical source. C2 should capture in a dedicated phase ordered
after ModelModifier.WRAP_LAST_PHASE, then install the BGE wrapper in a following dedicated phase.
This is a stable public Fabric seam for final model-modifier output. It cannot observe shader or
renderer-side transformations after BlockStateModel.emitQuads; a future provider there needs a
narrow optional bridge or C2 must fail closed.

No provider-specific bridge is warranted for this audited stack. A late final-mesh consumer remains
provider-neutral.

## C1 rejection proof

CanonicalFoliageProjection.ExteriorQuad.inspect first requires lightFace, then converts every
position to an exact sixteenth coordinate. Only after that does it require a constant plane,
strictly exterior depth, and a rectangular corner set.

| Oak source group | Quads | C1 result | First rejection | Additional reason if the grid rule were relaxed |
|---|---:|---|---|---|
| Matcha +36° outer-bottom planes (elements 1 and 5) | 4 | 0 admitted | Non-grid X/Z vertices | Slanted: no constant normal-axis plane. |
| Matcha −12° outer-top planes (elements 2 and 4) | 4 | 0 admitted | Non-grid X/Z vertices | Slanted: no constant normal-axis plane. |
| Matcha core cube (element 3) | 6 | 0 admitted | Strict-exterior test: plane is exactly 0 or 16 | Base/cull geometry, not exterior foliage. |
| **Total final Oak model** | **14** | **0 admitted** | **8 decorative non-grid rejections and 6 boundary-core rejections** | — |

The eight decorative quads do have usable baked lightFace values. The observed C1 no-op is
therefore demonstrated by filter rejection: first non-grid, then independently non-face-parallel
if that initial condition were loosened. It is not a startup issue, missing resource, or missing
later model provider.

## Recommended C2 architecture

Implement one provider-neutral late-baked exterior-fragment projector. This is face-local
projection, but not C1's rectangle-only scheme.

1. Register canonical_foliage_capture after Fabric WRAP_LAST_PHASE, then bge_foliage_projection
   after capture. Capture only BGE canonical-root leaf controls in the first phase and install BGE
   wrappers only in the second.
2. At world emission, ask the captured canonical BlockStateModel to emit into a MutableMesh.
   Consume only the live baked result—never model JSON, asset paths, sprite names, or copied pack
   files.
3. Select a non-cull source quad only if a validated planar convex polygon has a portion outside
   canonical [0,16]^3. Retain BGE's existing model for core geometry.
4. Carry source vertices as positions plus UV, color, light, normals, sprite/material/render-layer,
   tint, winding, and face metadata. Partition each source exterior portion into exactly one owner
   among the six canonical faces from signed outward distance. Use deterministic tie ownership at
   edges/corners so a source fragment is never duplicated.
5. Clip each owner fragment's face-local support footprint to [0,16] × [0,16], yielding
   (u, v, outward-distance) vertices. This splits the current cross planes into side, top, and
   bottom exterior pieces instead of assigning a whole plane to lightFace.
6. Map an owner fragment only to BGE SurfacePatch records with matching canonicalFace. Map u and v
   affinely into the exact patch rectangle and map outward distance along the patch's physical
   normal. BGE patches remain the sole topology authority; never reconstruct Stair, Wall, Layer,
   Corner, or Quarter Column geometry or use a bounding cube.
7. Emit with a proven polygon-to-quad tessellation, interpolate source attributes at clip edges,
   retain material/tint/UV/light/normal/winding/two-sided semantics, and clear cull metadata for
   projected decoration. Do not assume untested degenerate quads are a safe triangle encoding.
8. Do not add broad same-canonical-leaf decorative suppression for current Matcha: its eight
   primitives have no cullface. If a future primitive has explicit cull semantics, mask only its
   exact mapped support overlap at a real opposite-patch boundary with the same canonical material.
9. Fail closed per primitive for missing models, unsupported BGE surface data, invalid/non-planar
   source geometry, non-finite attributes, unassignable fragments, or unsupported tessellation.
   A texture-only model has no eligible primitive and receives no invented shell.

This stays dynamic across resource reloads and resource-pack overrides because its source is the
final canonical mesh. BGE binding and canonical leaf semantics let admitted modded leaf families
inherit the behavior once their actual source model exists; no registry-name list or hard provider
dependency is needed.

### Alternatives rejected

| Alternative | Why it is inferior |
|---|---|
| Loosen only C1's grid rule | The current planes still fail constant-plane/rectangle assumptions and cannot be mapped faithfully. |
| Treat lightFace as whole-quad ownership | The current planes reach several canonical faces, including top/bottom despite horizontal light faces. |
| Hard-code Foundation/Matcha model IDs or sprites | Breaks live overrides and modded families, and turns the JAR into a pack fork. |
| Parse/package Foundation bushy assets | Those selectors are not active here; packaging upstream material is unnecessary and crosses the asset boundary. |
| Project a full BGE bounding cube or branch by shape | Violates BGE SurfacePatch authority and decorates absent geometry. |
| Hook post-shader rendering | No audited provider creates the geometry there; a later provider needs an optional targeted seam, not a speculative dependency. |

## Licensing and distribution

Foundation's CREDITS.txt describes a private merged testing/layering composite, retains original
attribution and private-use restrictions for the Mynx foliage extension, and does not authorize a
new redistribution. Matcha's CREDITS.txt identifies Klei as author and includes informal reuse
language, but this audit established no standalone redistribution license. No broader licensing
inference is required.

C2 must ship neither Foundation nor Matcha artwork, model JSON, metadata, normal/specular maps, or
classes. The recommended final-mesh approach copies no upstream file and preserves the asset boundary.

## Concrete implementation and verification plan

### Headless work

- Test six-face ownership and polygon clipping using newly authored synthetic rotated −12°/+36°
  planes, a core cube, edge/corner ties, and invalid/non-planar input. Do not copy pack files or
  pixels into fixtures.
- Test exact source-to-patch mapping using BGE's own SurfacePatch output for full blocks, Slabs,
  Stairs, Walls, Vertical Slabs, Steps, Layers, Corners, and all Quarter Column occupancies.
- Test that Matcha-like non-cull decoration receives no invented contact masking; test exact
  opposite-patch masking separately with a synthetic explicitly cull-marked primitive.
- Prove renderer emission for every clipped polygon form, attribute interpolation, material
  preservation, and absence of duplicate output before choosing a triangle/quad representation.
- Static-check late phase ordering, reload invalidation, source capture, Fabric/BGE hash gates, and
  absence of provider assets/classes from the release JAR.

### Client evidence

- With the supplied order, compare canonical and BGE Oak/Spruce/Cherry through the full geometry and
  contact matrix in TESTING.md, shaders off first.
- Reload resources and change the selected leaf model to prove the result follows the active baked
  source or fails closed; a texture-only source must add nothing.
- Test Silver Birch/Wisteria only after its lower-priority provider model is available and audited;
  preserve provider tint and do not infer that geometry from Foundation's rejected override.
- Repeat representative geometry/contact rows with the normal Complementary stack. Record exact
  C2/BGE hashes, pack order, family, state, shader setting, and only observations actually made.
