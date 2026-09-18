# Testing

Canary 4 is `ACTIVE / RUNTIME_UNTESTED`. Its controlled checks prove typed appearance,
Continuity seam compatibility, source-gate control flow, and geometry decisions; they do not
prove client visual output. Use Minecraft Java 26.2, Fabric Loader 0.19.3+, Continuity
`3.0.1+26.2` exactly, a BGE build satisfying the declared range, the normal Matcha/overlay pack
order, connected textures enabled, and shaders off. Record every relevant `[BGE-CTM DIAG]` line
from `latest.log`; do not substitute an unverified Continuity version.

At client startup expect one line beginning `[BGE-CTM DIAG] STARTUP` that reports BGE × CTM,
Minecraft, BGE, Continuity, required hook status, and the default cap of 200 unique records.
Records are INFO-level by default, deduplicated by decision signature, and emit one suppression
notice after the cap. `-Dbge_ctm.diagnostics.disable=true` opts out; `-Dbge_ctm.diagnostics.cap=N`
sets a cap clamped to 1–300.

## Short diagnostic-first pass

For each row, capture the exact blocks/states, positions, receiver/source direction, inspected
face, shader/pack state, visual result, and corresponding diagnostic lines. A `REGULAR` record
must show canonical appearances, upstream result, quad/state geometry and final result. An
`OVERLAY` record must show the real inducing state, `nativeFull`, `partialPromoted`, canonical
appearance, semantic outcome, geometry, and final result.

1. Establish the control: full clear glass ↔ full clear glass. Then test two `type=top` clear
   glass slabs side-by-side on `UP`, a top clear-glass slab ↔ full clear glass on `UP`, and top
   ↔ bottom as a negative. The first three should have `upstream=true`, a coplanar geometry
   decision, and `FINAL_CONNECT`; the negative must be `NON_COPLANAR`/`FINAL_VETO`.
2. If a regular candidate has no valid captured quad, inspect the reason. Simple single-cuboid
   profiles may show `STATE_FALLBACK_CONTACT_OK`; glass-edge/rim or other unsafe visual topology
   must remain `QUAD_CAPTURE_INVALID` rather than guessing rendered bounds.
3. Before every terrain partial test, establish the directional full-block baseline on the same
   face: full podzol ↔ full grass, full podzol ↔ full glass, and full grass ↔ full glass when
   relevant. Do not assume symmetry or infer a relationship that the active pack does not have.
4. Reproduce the reported C3 layouts: a full podzol beside two stacked grass slabs, then two
   stacked podzol slabs beside full glass. For every “stacked” arrangement, use diagnostics to
   record whether it is one `type=double` block or two `TOP`/`BOTTOM` states at distinct positions.
   Compare only to the matching full-block baseline.
5. Pick one canonical full-block overlay baseline that is positive and replace only its inducing
   source with a coplanar top slab, a non-coplanar bottom slab, and a double slab. A supported
   partial source may log `PARTIAL_SOURCE_PROMOTED`, but it must still pass native semantic checks
   and real contact before `FINAL_OVERLAY`. The bottom control must be vetoed; a double slab uses
   full-volume geometry and normally logs `nativeFull=true`.
6. Only after the above is understood, repeat one positive and one negative case for a top-boundary
   Layer and an aligned Vertical Slab. Reversed/recessed Layers and misaligned Vertical Slabs must
   remain rejected. Steps, Corners, Quarter Columns, roots, paths, custom models, and unrelated
   translucency/culling behavior remain excluded.

## Interpreting the diagnostics

- `APPEARANCE ... reason=PROJECTED` confirms typed BGE profile lookup and canonical state
  projection. `NO_PROFILE`, `UNSUPPORTED_GEOMETRY`, `UNSUPPORTED_VISUAL`, and
  `UNMAPPABLE_CANONICAL_STATE` are deliberate non-participation reasons.
- `REGULAR ... reason=UPSTREAM_REJECT` means Continuity/resource-pack semantics rejected before
  BGE × CTM could permit a connection. `NON_COPLANAR`, `NO_BOUNDARY_CONTACT`, and
  `QUAD_CAPTURE_INVALID` identify a geometry/capture veto; `FINAL_CONNECT` is an allowed final
  decision, not visual proof.
- `OVERLAY ... reason=PARTIAL_SOURCE_GATE_REJECT` means the real source was an unrelated partial
  block. `PARTIAL_SOURCE_PROMOTED` means only that a supported typed carrier reached native
  canonical semantic evaluation. `CONNECT_BLOCKS_REJECT`, `NATIVE_SEMANTIC_REJECT`,
  `NON_COPLANAR`, `NO_BOUNDARY_CONTACT`, and `FINAL_VETO` remain terminal. BGE × CTM never makes
  a canonically invalid material/rule pair true.

Continuity still independently requires a unit-square receiver quad for Standard Overlay and may
reject `connectTiles` or its connection predicate before C4's final contact test. C4 does not
alter those rules, source collision/physics globally, or the active resource pack. Report the
exact log lines with the visual result rather than an aggregate pass/fail statement.
