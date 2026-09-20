# BGE × Bushy Leaves — Canary 2 correction and implementation record

## Revision-2 conclusion superseded by owner runtime evidence

The previous audit used static inspection of Foundation v2's arbitrary leaf blockstate rotations to
conclude that Minecraft Java 26.2 rejected Foundation's definitions and that Matcha Flavoured was
therefore the active full-block leaf winner. That conclusion was wrong as a statement about the
real client stack.

The owner has directly observed Foundation v2 visibly supplying the active full-block bushy leaves.
That runtime observation is authoritative. It establishes that the prior static analysis did not
account for some component of the installed/model-loading stack that successfully realizes those
resources. This project does not assert what that component is, and does not need to: Canary 2
does not reuse Foundation replacement geometry.

This supersedes the old active-provider conclusion only. The historical C1 runtime result remains
unchanged: exact `bge-bushy-leaves-0.1.0-canary1.jar`, SHA-256
`e85262f792638e99b5c1fdac9c48043e5bb461da55ce9a50fbc07cc1a24f7fbf`, loaded, reached gameplay,
raised no BGE × Bushy Leaves exception, and visibly added no foliage to BGE geometries. It remains
a `RUNTIME_FAIL`/no-op and is not rewritten as a launch failure.

Foundation is an active-pack appearance input. Matcha remains only the conceptual reference for
additive foliage: preserve the physical partial block and add foliage around real exposed surfaces.
Neither provider's JSON, textures, assets, metadata, or classes are in the C2 JAR.

## Canary 2 architecture

The untouched canonical leaf remains entirely owned by the active resource-pack/model stack. C2
changes only BGE derivatives, and only during world block-model emission:

1. A dedicated canonical appearance phase follows Fabric `ModelModifier.WRAP_LAST_PHASE` and
   captures final canonical-root leaf `BlockStateModel` instances. A second following phase wraps
   only BGE-derived leaf models. The cache is cleared on every resource reload.
2. The wrapper emits BGE's existing model unchanged. It asks the captured canonical model to emit
   with an isolated deterministic random source, so it cannot advance BGE's original random stream.
3. It samples all final canonical quads as appearance/material candidates. Non-cull decorative
   quads are preferred; if none exist, ordinary canonical leaf quads supply the fallback. Copying
   the chosen quad retains its atlas/sprite, tint index, render material/layer, emissive behavior,
   UVs, light maps, ambient/shading policy, animation, and other Fabric quad attributes.
4. BGE's binding resolves the exact canonical leaf and supplies the exact supported
   `BgeSurfaceGeometry.SurfacePatch` collection for the physical BGE state. Equivalent coplanar
   tiles are merged before planning; C2 never reconstructs Slab, Stair, Wall, Layer, Corner, or
   Quarter Column topology.
5. Each merged patch is treated as its own normal/U/V frame. C2 emits a small authored crossed,
   slanted two-sided card motif whose support rectangle is precisely that patch. Cards protrude
   outward for volume, may use only a bounded visual tip overhang, and never obtain a cube-sized
   attachment region over empty space. Centralized constants control depth, overhang, and the
   generic small-patch scale/omit rule.
6. Position, canonical material/state, and patch identity yield deterministic orientation and
   appearance selection. It is stable across frames and does not consume another model's random
   stream. C2 uses only an unambiguous renderer cull predicate for a complete 16×16 boundary face;
   it does not invent broad same-leaf decorative culling or a partial-contact mask system.

The same binding/surface path covers horizontal Slabs, resolved STAIR shapes/facings/halves, WALL
post/LOW/TALL/compound states, Vertical Slabs, Steps, 1–3 Layer depths, Corner rotations, and all
Quarter Column occupancies. Unsupported BGE surface data or a missing canonical model fails closed
for decoration while preserving the clean BGE base model.

## Controlled validation

`clean check stageCanaryArtifact` passed with Java 25, Fabric Loom 1.17.19, Fabric API
0.156.0+26.2, and the current BGE C84 controlled artifact SHA-256
`9c3a7af8eb86ffabcf5ddc3c7ba96134fb4322c6b92c3530dc8788edac015759`.

Nineteen focused tests cover semantic leaf recognition and non-leaf rejection; synthetic
non-cull appearance selection and ordinary fallback; path-neutral appearance metadata; reload
cache invalidation; deterministic orientation; full, Slab, resolved Stair, Wall, Vertical Slab,
Step, Layer, Corner, and every Quarter Column fixture; small-patch scale/omit; no empty-space
support; and coplanar patch merging. The release verifier confirms metadata, required classes,
controlled API hashes, and no Foundation/Matcha/provider asset or class content.

These are controlled code/geometry/API checks, not Minecraft visual runtime evidence. C2 remains
`RUNTIME_UNTESTED` pending owner testing of its exact artifact.
