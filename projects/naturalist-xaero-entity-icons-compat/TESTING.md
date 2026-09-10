# Testing

## Current gate

**C21 NOT DEPLOYED — STATIC PASS — RUNTIME UNTESTED — STARFISH-ONLY EXTERNAL RUNTIME TEST REQUIRED — NOT READY FOR PROMOTION**

Exact C20 Clam is USER-REPORTED / EXTERNAL RUNTIME **PASS**: `naturalist-xaero-entity-icons-compat-0.1.0-canary20.jar`, embedded `0.1.0-canary20`, 38,117 bytes, SHA-256 `e4cc28bfc6c9cd6addbf72b30aa632e90c2fc57c0c0c94bfa1bcd4291fb7a62d`, source `31d3b24a03bb0279be154a227a93515880727b70`. The reported result is a real Xaero model icon showing the full intended top surface with correct top-down presentation. C21 preserves that Clam implementation exactly. Brown Bear remains the unchanged runtime-proven native `naturalist:bear` → `RadarIconSpriteForm` path at `0.65F`.

Exact current C21 identity: `naturalist-xaero-entity-icons-compat-0.1.0-canary21.jar`, embedded `0.1.0-canary21`, 38,363 bytes, SHA-256 `3c52dcdd36af4f27e450ce590d150acd38900c0a0d12e8b52eaa4d8ebc4256d0`, source `9d534c4eb864c849238ab65273284ed6d00660ed`. C21 corrects Starfish only: source `<model root>`, trace `body`, copied normalized `body` + `legs` assembly, `0.58F` scale, X rotation `1.5708F`, zero Y/Z rotations, and zero frame offset. `body` contains the center and fifth arm; `legs` contains the remaining four arms.

Both authoritative test slots are occupied by unrelated ready cohorts (BGE C60 in A and IBF C4 in B). C21 is not manager-deployed; no Minecraft profile was changed.

## Focused C21 external runtime procedure

Use the exact C21 artifact with Naturalist C8, Xaero Minimap 26.4.2, EMF 3.2.6, ETF 7.1.1, and Xaero × EMF C9. Reload Xaero radar icon resources normally, then request only `naturalist:starfish` through a normal cache MISS followed by a cache HIT.

- Retain the one-time `NaturalistXaero StarfishCapture` sequence. It must identify `StarfishModel`, source `<model root>`, trace `body`, selected `body,legs`, scale/rotations/frame offset, native and fallback rendered-destination counts, adapter/trace binding, caught exception if any, creator/cache outcomes, and final manager result.
- PASS requires a real Xaero model icon rather than a label, a centered top-down recognizable Starfish with center and all five arms, no major clipping, normal usable size, and Naturalist's runtime texture/model identity.
- Brown Bear and Clam are regression controls only and must remain unchanged. Do not test either Scorpion or alter any other entity.
- If Starfish remains label-only, preserve the exact diagnostic sequence and stop. If a real icon is incomplete, clipped, or wrongly sized, record that exact visual result and stop. Do not create C22 automatically.
