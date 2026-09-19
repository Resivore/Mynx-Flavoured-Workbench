# BGE Glass Face Culling Canary 1 audit

## Controlled provider

Canary 1 compiles and validates against exact BGE C78:

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

Canary 1 registers an after-bake model modifier in an event phase ordered after the default phase.
The resulting outer wrapper asks the complete wrapped model chain to emit into
`Renderer.quadEmitter(Consumer<MutableQuadView>)`. Fabric invokes that consumer from the emitter's
final direct-emission seam after its transform stack. Consequently Continuity or another normal
default-phase model wrapper may first choose its final sprite, UVs, layer, and quad attributes;
Canary 1 then clips those final quads without manufacturing CTM rules or sprites.

The wrapper also reconciles existing boolean face culling. It re-allows a direction only when both
states are canonically compatible, both surface models are supported, and the source has a real
BGE boundary patch on that direction. The final quad clipper then produces the coherent result:
complete overlap emits nothing, partial overlap emits cropped fragments, and zero overlap preserves
the original quad. It does not recreate provider-removed interior surfaces.

## Quad preservation

The clipper accepts only exact rectangular axis-aligned final quads that match BGE's sixteenth-grid
contract. It subtracts the union by a deterministic edge partition, never by a coarse bounding
rectangle. Each fragment begins with `QuadEmitter.copyFrom`, preserving all global quad metadata;
positions, UVs, colors, packed light coordinates, and present vertex normals are then interpolated
at the physical crop boundary. Original vertex ordering is retained. Unsupported/non-grid quads
pass through unchanged rather than receiving guessed geometry.

## Controlled evidence and limit

The Java 25 suite proves deterministic rectangle union/subtraction, complete and partial emission,
UV/vertex-attribute preservation, material eligibility, full/simple/compound/partial contacts, all
40 Stair topology states, and all 161 nonempty Wall states. The build also hash-gates BGE C78 and
the exact Fabric rendering/model-loading modules and audits the release JAR boundary.

These tests do not render a Minecraft client framebuffer and are not gameplay-runtime or shader
evidence. The owner matrix in `TESTING.md` remains required before any runtime result is recorded.
