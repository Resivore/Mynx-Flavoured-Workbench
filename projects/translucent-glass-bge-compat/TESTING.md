# Testing

## Current gate

**PLANNED — NO RUNTIME CANDIDATE**

Implementation has not started. No source, build, artifact, Canary, deployment,
or Minecraft runtime evidence exists for this project. Do not infer readiness
or validation from this administrative migration.

## Future runtime acceptance procedure

After an implementation produces a candidate and this UUID has explicit
runtime-slot ownership, record the exact Translucent Glass, BGE, Nibaru, CNM,
Continuity, Sodium, Iris, Complementary, Fabric API, resource-pack order,
world, candidate hashes, and matching log. Use one clear family for the main
matrix and one stained color only as a negative-family control.

1. With shaders off, compare canonical full glass with one isolated BGE Layer
   or Vertical. Check exterior alpha, sorting, edge texture, placed rendering,
   and the corresponding inventory item.
2. Place two equal pieces of one BGE geometry at a full-contact boundary. Check
   that the internal face is absent without hiding an exterior partial face.
3. Place the same material across two geometries, preferably Layer ↔ Step.
   Check the shared face and seam from both sides.
4. Place Layer or Step against canonical full glass in one full-contact and one
   partial-contact orientation. Check both render directions for missing or
   extra faces.
5. With Continuity enabled, run the one corresponding BGE × CTM connection
   expectation and put a different stained color beside it as a negative
   connection case. Classify a general connectivity defect under BGE × CTM
   unless evidence ties it to this compatibility layer.
6. Only after an authorized/supported shader-classification path exists, repeat
   the isolated and canonical/BGE adjacency comparison with Complementary Fancy
   Glass enabled. Compare opacity, highlights/reflections, halos, and depth
   ordering.
7. Resource-reload once, save/reload the world, and restart the client once.

Pass only when every applicable representative case retains the intended
Translucent Glass treatment without opaque fallback, missing assets, incorrect
connections, sorting defects, internal-face artifacts, shader mismatch/halos,
or item regressions. Stop and preserve the exact stack, world, screenshots, and
log on any such defect, crash, or relevant render/resource error. Do not infer
runtime validation from compilation, generated assets, fixtures, GameTests, or
screenshots produced outside Minecraft.
