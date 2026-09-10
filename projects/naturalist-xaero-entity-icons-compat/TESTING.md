# Testing

## Current gate

**C23 NOT DEPLOYED — STATIC PASS — USER-REPORTED / EXTERNAL FOCUSED RUNTIME PASS — NOT READY FOR PROMOTION**

Exact C20 Clam is USER-REPORTED / EXTERNAL RUNTIME **PASS**: `naturalist-xaero-entity-icons-compat-0.1.0-canary20.jar`, embedded `0.1.0-canary20`, 38,117 bytes, SHA-256 `e4cc28bfc6c9cd6addbf72b30aa632e90c2fc57c0c0c94bfa1bcd4291fb7a62d`, source `31d3b24a03bb0279be154a227a93515880727b70`. The reported result is a real Xaero model icon showing the full intended top surface with correct top-down presentation. C21 preserves that Clam implementation exactly. Brown Bear remains the unchanged runtime-proven native `naturalist:bear` → `RadarIconSpriteForm` path at `0.65F`.

Exact C21 is USER-REPORTED / EXTERNAL RUNTIME **FAIL**: `naturalist-xaero-entity-icons-compat-0.1.0-canary21.jar`, embedded `0.1.0-canary21`, 38,363 bytes, SHA-256 `3c52dcdd36af4f27e450ce590d150acd38900c0a0d12e8b52eaa4d8ebc4256d0`, source `9d534c4eb864c849238ab65273284ed6d00660ed`. The observed Starfish was label-only with no usable model icon. C21 reached the contract, trace bridge, and `RadarIconModelPartPrerenderer#renderPart`, but its fallback destination remained empty (`fallbackRenderedParts=0`) with no exception; non-null creator/cache results are not model success.

Exact C22 is USER-REPORTED / EXTERNAL RUNTIME **PASS**: `naturalist-xaero-entity-icons-compat-0.1.0-canary22.jar`, embedded `0.1.0-canary22`, 39,279 bytes, SHA-256 `d9b8b7d564dc6ebd4b95db6e1b773eb010cb55c0769be6eb815ed71dbd938060`, source `0a75a2a8bff2f7a508cc1843381f5e4e70262b9d`. Starfish is a real full five-arm silhouette rather than label-only and the live-body render-center fix is confirmed. The only follow-up is minor size tuning; it is not a Starfish failure.

Exact current C23 identity: `naturalist-xaero-entity-icons-compat-0.1.0-canary23.jar`, embedded `0.1.0-canary23`, 45,991 bytes, SHA-256 `d1deb67227eac2e9de37a5e2f2c00a27b34abaa1cb19a1a38269bd7041a625c1`, source `34e3ec7f4141762f3418e60307c2cc13a09db038`. The user reports successful focused runtime results: Starfish PASS after the slight size reduction in C23, Desert Scorpion PASS, and Jungle Scorpion PASS. Starfish remains unchanged except scale `0.58F` -> `0.52F`. Exact C8 audit confirms both Scorpion constructors pass `root.getChild("root")` to `EntityModel`; therefore `model.root()` is already the authored root. Desert `body` owns claws/tail and Jungle `body` owns arms/claws/tail; `legs` is a sibling in each. C23 uses source `<model root>`, trace/live render center `body`, normalized copied selected `body` + `legs`, X `1.5708F`, zero Y/Z/frame offset, and retains Desert `0.34F` / `0.28F` Jungle scales.

Both authoritative test slots are occupied by unrelated ready cohorts (BGE C60 in A and IBF C4 in B). C23 is not manager-deployed; no Minecraft profile was changed.

## C23 focused runtime result

The exact C23 artifact was externally/user runtime tested with the focused result supplied by the user: `naturalist:starfish` PASS after its slight size reduction, `naturalist:desert_scorpion` PASS, and `naturalist:jungle_scorpion` PASS. These results confirm the focused C23 objectives only; C23 remains `NOT_DEPLOYED` because no Test Instance Manager transition was made, and it is not accepted or promoted. Brown Bear and Clam remain unchanged regression controls.
