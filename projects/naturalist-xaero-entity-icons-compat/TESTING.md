# Testing

## Current gate

**C22 NOT DEPLOYED — STATIC PASS — RUNTIME UNTESTED — STARFISH-ONLY EXTERNAL RUNTIME TEST REQUIRED — NOT READY FOR PROMOTION**

Exact C20 Clam is USER-REPORTED / EXTERNAL RUNTIME **PASS**: `naturalist-xaero-entity-icons-compat-0.1.0-canary20.jar`, embedded `0.1.0-canary20`, 38,117 bytes, SHA-256 `e4cc28bfc6c9cd6addbf72b30aa632e90c2fc57c0c0c94bfa1bcd4291fb7a62d`, source `31d3b24a03bb0279be154a227a93515880727b70`. The reported result is a real Xaero model icon showing the full intended top surface with correct top-down presentation. C21 preserves that Clam implementation exactly. Brown Bear remains the unchanged runtime-proven native `naturalist:bear` → `RadarIconSpriteForm` path at `0.65F`.

Exact C21 is USER-REPORTED / EXTERNAL RUNTIME **FAIL**: `naturalist-xaero-entity-icons-compat-0.1.0-canary21.jar`, embedded `0.1.0-canary21`, 38,363 bytes, SHA-256 `3c52dcdd36af4f27e450ce590d150acd38900c0a0d12e8b52eaa4d8ebc4256d0`, source `9d534c4eb864c849238ab65273284ed6d00660ed`. The observed Starfish was label-only with no usable model icon. C21 reached the contract, trace bridge, and `RadarIconModelPartPrerenderer#renderPart`, but its fallback destination remained empty (`fallbackRenderedParts=0`) with no exception; non-null creator/cache results are not model success.

Exact current C22 identity: `naturalist-xaero-entity-icons-compat-0.1.0-canary22.jar`, embedded `0.1.0-canary22`, 39,279 bytes, SHA-256 `d9b8b7d564dc6ebd4b95db6e1b773eb010cb55c0769be6eb815ed71dbd938060`, source `0a75a2a8bff2f7a508cc1843381f5e4e70262b9d`. It preserves source `<model root>`, live trace `body`, normalized copied `body` + `legs` assembly, `0.58F`, X rotation `1.5708F`, zero Y/Z rotations, zero frame offset, and all five arms. C22 changes only Xaero's render center to the real live `body`: Xaero 26.4.2 uses center for direct-cuboid centering, while its trace lookup remains for the rendered adapter. `body` contains the disk and fifth arm; `legs` contains the other four.

Both authoritative test slots are occupied by unrelated ready cohorts (BGE C60 in A and IBF C4 in B). C22 is not manager-deployed; no Minecraft profile was changed.

## Focused C22 external runtime procedure

Use the exact C22 artifact with Naturalist C8, Xaero Minimap 26.4.2, EMF 3.2.6, ETF 7.1.1, and Xaero × EMF C9. Reload Xaero radar icon resources normally, then request only `naturalist:starfish` through a normal cache MISS followed by a cache HIT.

- Retain the one-time `NaturalistXaero StarfishCapture` sequence. It must identify `StarfishModel`, source `<model root>`, trace `body`, synthetic selected `body,legs`, `0.58F` top-down presentation, live `body` render center, `renderCenterIsTrace=true`, direct body MRT entry, adapter-trace binding, native/fallback destination counts, caught exception if any, creator/cache outcomes, and final manager result.
- PASS requires a real Xaero model icon rather than a label, a centered top-down recognizable Starfish with center and all five arms, no major clipping, normal usable size, and Naturalist's runtime texture/model identity.
- Brown Bear and Clam are regression controls only and must remain unchanged. Do not test either Scorpion or alter any other entity.
- If Starfish remains label-only and the live body center still leaves the destination `0 -> 0`, record that exact result, reject the centering hypothesis, and stop. If a real icon is incomplete, clipped, or wrongly sized, record the exact visible parts and stop. Do not begin Scorpion work automatically.
