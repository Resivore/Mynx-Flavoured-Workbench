# Testing

## Current gate

**C20 NOT DEPLOYED — STATIC PASS — RUNTIME UNTESTED — CLAM-ONLY EXTERNAL RUNTIME TEST REQUIRED — NOT READY FOR PROMOTION**

C19 is reconciled as USER-REPORTED / EXTERNAL RUNTIME **FAIL** for the Clam completeness gate. Exact identity: `naturalist-xaero-entity-icons-compat-0.1.0-canary19.jar`, embedded `0.1.0-canary19`, 38,021 bytes, SHA-256 `721e4854971301dd0987c21958a42834c08cf6456386ee88355b6cfc5befb0c1`, source `0866b0199860dccc6a368d945f6e1bba38201a79`. Brown Bear remained good. Clam was no longer label-only: C19 rendered a real, partial icon similar to the earlier partial-success canary. It did not show the complete top surface, so it is not a Clam pass. This proves the retained `0.30F` top-down presentation can produce a real icon; the remaining defect is captured-surface framing/completeness.

Exact current C20 identity: `naturalist-xaero-entity-icons-compat-0.1.0-canary20.jar`, embedded `0.1.0-canary20`, 38,117 bytes, SHA-256 `e4cc28bfc6c9cd6addbf72b30aa632e90c2fc57c0c0c94bfa1bcd4291fb7a62d`, source `31d3b24a03bb0279be154a227a93515880727b70`. Its C20 Clam contract copies and traces only `ClamModel`'s authored `top` child at `0.30F`, X rotation `1.5708F`, zero Y/Z rotation, and zero frame offset. The lower shell and hinge are excluded. Brown Bear remains the unchanged native `naturalist:bear` → `RadarIconSpriteForm` path at `0.65F`.

Both authoritative test slots are occupied by unrelated ready cohorts (BGE C60 in A and IBF C4 in B). C20 is not manager-deployed; no Minecraft profile was changed.

## Focused C20 external runtime procedure

Use the exact C20 artifact with Naturalist C8, Xaero Minimap 26.4.2, EMF 3.2.6, and Xaero × EMF C9. Reload resources once, then request Clam through a normal cache MISS followed by a cache HIT.

- Brown Bear is a regression control only: it must remain correct and unchanged.
- Clam must be a real Xaero icon, viewed top-down, centered, and show the entire authored top shell without top-edge clipping or a return to label-only/tiny/empty output.
- Compare explicitly with C19's partial top-surface icon. If C20 still shows only part of the Clam, record only the visible portion and stop; do not create C21 automatically.
- Retain the one-time `NaturalistXaero ClamCapture` sequence: model class; source and trace paths; selected children; scale; frame Y offset; native empty result; fallback recorded-part result; and final manager result. A non-null creator/cache result or empty render destination is not a visual pass.

Do not alter Starfish, either Scorpion, Giant Isopod, Zebra, Great White Shark, Piranha, Bass, Ray, Hedgehog, any other Naturalist entity, Naturalist, Xaero/XaeroLib, Xaero × EMF, Ribbits × Xaero, or either Minecraft profile. The protected Matcha Flavoured 26.1.2 profile is off-limits.
