# Testing

Canary 1 is the current candidate and remains `NOT_DEPLOYED` and `RUNTIME_UNTESTED`. The frozen legacy build recorded 23 passing focused tests and no failures, errors, or skips. This migration did not rebuild, launch Minecraft, access either runtime profile, or change a Test Slot. It instead verified that all 23 maintainable source, test, and build blobs match legacy source checkpoint `b025ffc38bb2810c7a472ba73970dd8a7277bac2` and that the retained 31,753-byte artifact remains byte-identical at SHA-256 `fe4c9164a744c2684067346c8a3daaa2b935f5802b2bdb1f357d5b9e854137ce`.

## Runtime prerequisites

Use a disposable singleplayer world with clear mapped and unmapped boundaries, plus a representative multiplayer world when that phase is authorized. An explicit runtime owner must deploy only the retained `artifacts/xaero-discovery-radius-0.1.0-canary1.jar` with the hash above as an experimental overlay alongside the cumulative accepted stack and the exact external Xaero pair:

- Xaero Minimap 26.4.2, SHA-256 `69284892d2eb853c9aefa85a4c9b74232c322da00207994c67ab8aeed8a64048`;
- Xaero World Map 1.44.2, SHA-256 `d55ef45c559ae0adcf66d894c022f61d9d921629b0c885d04aa00424546a2389`.

Run the normal focused readiness verifier before opening Minecraft. Do not rebuild or substitute a same-version JAR during the handoff. Preserve the client profile's `config/xaero-discovery-radius/legacy-xaero-cache-manifest.json` and the test world's dimension-local `data/xaero-discovery-radius.history` files throughout persistence checks.

## Runtime matrix

1. Before first C1 launch, identify existing Xaero terrain that must remain visible and a fresh unmapped boundary suitable for controlled travel.
2. Set Minecraft render distance to 16 chunks and leave `discoveryRadiusChunks` at 4. Confirm the Minecraft option and F3 value remain 16 after C1 loads.
3. In fresh terrain, stand still and travel while chunks 5 through 16 away are visibly rendered. Confirm both Xaero Minimap and World Map leave terrain outside the inclusive 4-chunk square undiscovered.
4. Approach the same terrain until its chunk is within 4 chunks on both axes of the player's chunk. Confirm it maps normally and Minimap and World Map remain consistent.
5. Leave the area and confirm newly mapped terrain remains visible beyond the current discovery radius. Confirm all pre-install map terrain and exact frozen `.xwmc` cache regions remain visible and no existing map data is deleted or rewritten by the companion.
6. In singleplayer, generate or save terrain outside the allowed radius without approaching it, force a save and World Map region load, and confirm Xaero's direct world-save reader does not reveal it. Then approach within radius and confirm the chunk becomes permanently eligible.
7. Save and quit, reload the world, restart the full client, and revisit inside and outside boundaries. Confirm discovery history persists separately for each tested dimension and that an ordinary save copy retains its dimension-local history.
8. In multiplayer, confirm normal nearby mapping, existing-map loading, relog persistence, and disconnect/reconnect behavior while visible terrain beyond the configured radius remains undiscovered.
9. Change `discoveryRadiusChunks` to another valid value, restart, and confirm only the discovery boundary changes. Separately confirm malformed, fractional, and out-of-range values fall back to 4 without changing render distance.
10. Regress waypoints, waypoint visibility, map navigation and zoom, Minimap and World Map rendering, cave and light overlays, entity radar, unrelated Xaero UI, dimension travel, and normal client shutdown.
11. If freecam or another camera mod is present, classify it separately because World Map normally centers on Xaero's current camera entity while the Minimap fallback and persistent singleplayer eligibility use the local player's chunk.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if the exact dependency or artifact identities differ, readiness verification fails, Minecraft render distance changes, outside-radius terrain is newly mapped, inside-radius terrain fails to map, any previously discovered terrain disappears, history fails across reload, singleplayer direct-save reading bypasses the gate, a required Mixin fails, or an unrelated Xaero behavior regresses. Do not accept C1 without a controlled pass of the applicable matrix.
