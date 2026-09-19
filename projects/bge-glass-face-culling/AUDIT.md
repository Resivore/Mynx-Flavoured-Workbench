# BGE Glass Face Culling Canary 3 audit

## Controlled provider

Canary 3 compiles and validates against exact BGE C78:

- embedded version: `4.2.22-bge.canary78.stair-wall-surface-authority+26.2`
- artifact: `BGE C78.jar`
- SHA-256: `ed2f5592b699532174bc63672f69c3574f01eb752175126bbc702e9cc86a07f0`
- implementation checkpoint: `85acefa42fa46e133b0917d056817d6eaf0768b3`
- finalized records commit: `fc072586c0808d9610e5290705ff9b1bd505a24c`

The consumer calls only `BgeMaterialBindings.Binding.surfaceModel(state)` and the returned
`SurfacePatch` records. It contains no geometry registry-name switch, block-class topology table,
Stair reconstruction, Wall reconstruction, collision-shape approximation, or terrain-plane
equivalence. Physical planes must match exactly after the one-cell block-position translation.

## Material policy

Material eligibility and geometry are separate checks. A state participates only when its BGE
material profile has both `VisualProfile.GLASS_EDGE` and
`BehaviorCapability.TRANSLUCENT_ADJACENCY`. Two participating states mutually cull only when their
typed BGE `canonicalMaterial` objects are identical. This preserves distinct clear and stained
families and excludes ice, honey, slime, and arbitrary translucent render layers.

## Minecraft 26.2 rendering seam

The controlled compile baseline is Minecraft Java 26.2, Fabric API `0.156.0+26.2`, Fabric Renderer
API `14.1.3+2b0d8a229e`, and Fabric Model Loading API `8.0.16+c80601bb9e`.

Canary 1 ordered its custom after-bake phase only after Fabric's default phase. That did not order it
after Slab Decorations' `ModelModifier.WRAP_LAST_PHASE`: C1 could therefore be the inner wrapper,
receive unshifted source quads, and clip before `SurfaceOffsetModel` copied and translated them. The
claimed final-transformed-quad guarantee was not true for that supported stack.

Canary 2 orders its custom after-bake phase after `ModelModifier.WRAP_LAST_PHASE`. Fabric applies
after-bake modifiers sequentially, so C2 becomes the outer wrapper around Slab Decorations. C2 asks
that complete wrapped chain to emit into `Renderer.quadEmitter(Consumer<MutableQuadView>)`; Slab
Decorations first gathers its wrapped model into a mesh and emits its translated copies to that
consumer, and C2 then clips those emitted quads. Continuity/default-phase transformations remain
inside that chain. C2 therefore clips the transformed geometry it actually receives, without
manufacturing CTM rules or sprites.

Canary 2 correctly delegated a null direction, but its directional predicate evaluated as
`upstreamWholeFaceCull || canEvaluateBoundary`. `canEvaluateBoundary` therefore made a compatible
full-glass ↔ partial-BGE boundary return true even when only part of the face could be removed. The
renderer omitted that complete source face before `GlassQuadClipper` could subtract its exact
overlap. This is the confirmed root cause of the owner-observed C2 full ↔ partial failure.

For a null direction, C3 still delegates directly to the upstream predicate and neither reads a
neighbor nor evaluates BGE geometry. For a real direction that is canonically compatible and
geometrically evaluable, C3 instead returns false before consulting the upstream whole-face cull
predicate. The complete source face consequently reaches `GlassQuadClipper`, which remains the sole
owner of exact interface subtraction: complete overlap emits nothing, partial overlap emits cropped
fragments, and zero overlap preserves the original quad. Noneligible directional boundaries retain
the ordinary upstream predicate behavior. C3 changes neither quad `cullFace` metadata, BGE
model-generation cullface policy, material compatibility, CTM behavior, nor the generic BGE C78
surface resolver.

## Quad preservation

The clipper accepts only exact rectangular axis-aligned final quads that match BGE's sixteenth-grid
contract. It subtracts the union by a deterministic edge partition, never by a coarse bounding
rectangle. Each fragment begins with `QuadEmitter.copyFrom`, preserving all global quad metadata;
positions, UVs, colors, packed light coordinates, and present vertex normals are then interpolated
at the physical crop boundary. Original vertex ordering is retained. Unsupported/non-grid quads
pass through unchanged rather than receiving guessed geometry.

## Controlled evidence and limit

The Java 25 suite proves null-direction delegation for both upstream outcomes without a level,
position, or state to touch; preservation of noneligible directional culling; eligible-boundary
whole-face-cull bypass; and the predicate-to-clipper handoff for an exact partial crop. BGE-backed
GameTests prove full/full complete removal, partial/partial exact intersection, noncontacting
partial preservation, every representative full ↔ partial BGE family (bottom/top Slab, Vertical
Slab, Step, Layer, Quarter Column, Corner, Stair, and Wall), all 40 Stair states, and all 161
nonempty Wall states. The build hash-gates BGE C78 and the exact Fabric rendering/model-loading
modules and audits the release JAR boundary.

These tests do not render a Minecraft client framebuffer and are not gameplay-runtime or shader
evidence. The owner observed that exact C2 opened the previously affected world without the C1
null-Direction crash and that partial ↔ partial geometry appeared correct. The owner also observed
that C2 incorrectly removed the entire full-glass face for a partial full ↔ partial contact. C3 has
no runtime result yet; its first gate and matrix in `TESTING.md` remain required before any C3
runtime result is recorded.
