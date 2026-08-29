# Testing

## Current gate

**PLANNED — NO RUNTIME CANDIDATE**

Implementation has not started. No source, build, artifact, Canary, deployment,
or Minecraft runtime evidence exists for this project. Do not infer readiness
or validation from this administrative migration.

## Future runtime acceptance procedure

1. Under explicit runtime-slot ownership, record the exact Translucent Glass,
   BGE, Continuity, Sodium, and Complementary shader identities, the complete
   enabled stack, the world, and the matching log.
2. Establish clear-glass and representative stained-glass full-block baselines,
   then exercise slabs, stairs, walls, vertical slabs, and steps only where BGE
   actually exposes those family geometries.
3. Check placed and inventory/item rendering, same-family adjacency, and mixed
   full-block/derived-geometry seams for correct translucency, textures,
   internal faces, and depth ordering.
4. With Continuity enabled, verify intended connected textures on applicable
   derived geometry and confirm that different materials or colors do not
   connect incorrectly.
5. Repeat representative cases with Complementary shaders enabled, then perform
   a resource reload, save/reload the world, and complete a full client restart.

Pass only when every applicable representative case retains the intended
Translucent Glass treatment without opaque fallback, missing assets, incorrect
connections, sorting defects, internal-face artifacts, shader halos, or item
render regressions. Stop and preserve the exact stack, world, screenshots, and
log on any such defect, crash, or relevant render/resource error.
