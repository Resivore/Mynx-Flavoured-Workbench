# Testing

This planned project has no implementation or artifact. The cases below are future runtime acceptance targets and have not been tested.

## Future runtime acceptance targets

1. Source block ↔ source block CTM behavior remains correct.
2. A source block ↔ its corresponding BGE geometry connects when the canonical material relationship makes connection semantically appropriate.
3. Compatible BGE geometry ↔ BGE geometry adjacency connects correctly.
4. Orientation- or state-specific geometries do not create false connections.
5. Representative transparent and opaque CTM material families behave correctly.
6. BGE geometries that should not participate in a source CTM group remain excluded.
7. Continuity behavior outside BGE remains unchanged.
8. Complementary shaders do not expose obvious new seams attributable to the compatibility layer.

When implementation reaches a controlled runtime handoff, record each applicable result precisely and stop as failed or inconclusive if the compatibility layer causes crashes, false connections, missing expected connections, or regressions outside BGE. Do not infer any runtime result from asset generation, static checks, or builds.
