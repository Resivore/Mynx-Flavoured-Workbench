# Testing

Canary 6 is `ACTIVE / PARTIAL_RUNTIME_PASS`: exact C5 owner evidence established a positive
Standard Overlay semantic/contact result for one ordinary bottom-slab receiver, but its emitted
quad floated at y=1 instead of the captured y=0.5 surface. C6 has controlled geometry coverage;
it has not yet received Minecraft-client visual validation. Use Minecraft Java 26.2, Fabric Loader
0.19.3+, Continuity `3.0.1+26.2` exactly, and a BGE build satisfying the declared range. Disable
shaders. Do not create a large test area.

At startup retain `[BGE-CTM DIAG] STARTUP`. C6 adds the independent managed-only
`overlayEmitCap=100` (set with `-Dbge_ctm.diagnostics.overlayEmitCap=N`, clamped 1–300) alongside
the existing APPEARANCE, REGULAR, RULE_SELECTION, OVERLAY, and OVERLAY_BASELINE budgets. It does
not consume their caps.

## A. Exact displaced-overlay reproduction

Use the same known-valid canonical inducing/source relationship from the supplied C5 case with a
bottom ordinary receiver slab. Inspect its `UP` surface.

Expected C6 result: the overlay lies on the slab top at y=0.5, not at y=1. Capture the positive
`OVERLAY` line and the matching line shaped like:

`[BGE-CTM DIAG] event=OVERLAY_EMIT receiver=... face=UP captured=QuadSurface[normal=UP, plane16=8, ...] emitted=face=UP,plane16=8,u=0..16,v=0..16,... path=PROJECTED reason=EMIT_MATCH_RECEIVER`

## B. Slab side faces

Use a terrain relationship that produces a Standard Overlay on a slab side. Check one bottom and
one top slab. The bottom overlay must occupy only Y 0..8; the top overlay must occupy only Y 8..16.
Their texture must be the corresponding canonical half, not a vertically compressed full sprite.
Retain `OVERLAY_EMIT` lines showing the captured/emitted bounds and UV values.

## C. Layer and Vertical Slab

Check one supported Layer boundary face and one recessed Layer face, then one supported single
Vertical Slab face. Each overlay must occupy only the actual captured surface region and plane.
For a capture missing on simple cuboids, C6 may report `STATE_DERIVED_PROJECTED`; complex/rim
topology must instead veto with `SURFACE_CAPTURE_MISSING` or `SURFACE_UNSUPPORTED` rather than
draw a full-block overlay.

Send the visual result, exact block states/positions/face, shader and pack state, and relevant
`[BGE-CTM DIAG]` lines. Regular CTM across ordinary slabs remains a separate unproven runtime
question; do not infer its result from these Standard Overlay checks.
