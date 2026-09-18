# Testing

Canary 7 is `ACTIVE / RUNTIME_UNTESTED`. Use Minecraft Java 26.2, Fabric Loader 0.19.3+,
Continuity `3.0.1+26.2` exactly, and BGE `>=4.2.17-bge.canary73.canonical-bindings+26.2`.
Diagnostics are **off by default**. Enable bounded diagnostics only for a failed check with
`-Dbge_ctm.diagnostics=true`; the legacy `-Dbge_ctm.diagnostics.disable=true` remains an
explicit override.

## A. Placement responsiveness

With diagnostics left off, compare ordinary block placement with C7 active and, if convenient,
with BGE × CTM absent. Record only the observed responsiveness; C7's controlled checks prove
that disabled diagnostics do no attempt allocation, diagnostic ThreadLocal work, rule-selection
classification, registry formatting, signature construction, or budget admission. They do not
measure client FPS/TPS or prove the supplied C6 placement lag resolved.

## B. Grass × podzol regression

Confirm the owner-observed good grass × podzol slab overlay still renders correctly. Verify the
overlay is attached to the receiver's actual partial surface, not the full-block plane.

## C. Grass × farmland

First establish the corresponding full grass ↔ full farmland Continuity relationship and note the
actual receiver/direction. Then test that same direction for horizontally adjacent grass/farmland
bottom slabs, top slabs where meaningful, and one full ↔ Farmland Slab case. C7 must retain only
an already-positive Continuity semantic result; it may not invent a grass/farmland rule. A valid
partial relationship need not have equal top heights (grass bottom 8/16 versus farmland bottom
7/16), but both occupied volumes must reach the shared block boundary and overlap there.

## D. Receiver-surface emission

For every positive overlay, check the quad remains on the receiver: grass uses the grass surface;
Farmland Slab as receiver uses its 7/16 or 15/16 surface. It must not float at y=1 or move to the
inducing source plane. Enable diagnostics only after a failure and retain the relevant
`OVERLAY`/`OVERLAY_EMIT` lines, receiver/source states, positions, face, pack and shader state.
