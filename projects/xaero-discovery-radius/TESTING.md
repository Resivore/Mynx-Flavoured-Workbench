# Testing

Canary 2 is the current unaccepted candidate and remains `STATIC_PASS`, `NOT_DEPLOYED`, and `RUNTIME_UNTESTED`. A Java 25 / Gradle 9.5.1 / Fabric Loom 1.17.19 `clean test build --no-daemon` passed 32 focused tests in seven suites with no failures, errors, or skips. The exact upstream bytecode-contract and client-only packaging checks also passed.

The retained artifact is `artifacts/xaero-discovery-radius-0.1.1-canary2.jar`, 34,577 bytes, SHA-256 `a7d0125dbe13bbcd5418aac39e0e51a32ea79d508d9dc47f2ebd86526a794c99`.

## Runtime prerequisites

Perform this runtime handoff only in a separate user-managed disposable profile outside the Workbench. Do not use either Workbench runtime profile, either Test Slot, the Test Instance Manager, or the protected gameplay instance. Use disposable singleplayer world copies with known existing surface and cave map data, plus a representative multiplayer server if available.

Use the retained Canary 2 artifact exactly as hashed above with:

- Xaero Minimap 26.4.2, SHA-256 `69284892d2eb853c9aefa85a4c9b74232c322da00207994c67ab8aeed8a64048`;
- Xaero World Map 1.44.2, SHA-256 `d55ef45c559ae0adcf66d894c022f61d9d921629b0c885d04aa00424546a2389`;
- embedded XaeroLib 1.7.1, SHA-256 `7f4a78dd7e046fea0500fef83b1481d85317c8348d47e947035d7a07efe51065`.

Before testing, preserve or hash the Xaero map folders, `config/xaero-discovery-radius/legacy-xaero-cache-manifest.json`, every dimension-local `data/xaero-discovery-radius.history` v1 file, and any `data/xaero-discovery-radius.layers-v2.history` file. Explicitly set `discoveryRadiusChunks` to `2`, or remove the companion config in the disposable profile. A valid existing C1 value of `4` remains an explicit user setting and is not silently overwritten by C2.

## Exact horizontal boundary

At radius 2, the inclusive predicate `dx² + dz² <= 4` contains exactly 13 chunk offsets:

- `(0, 0)`;
- `(0, ±1)` and `(±1, 0)`;
- `(±1, ±1)`;
- `(0, ±2)` and `(±2, 0)`.

Offsets such as `(±2, ±1)`, `(±1, ±2)`, and `(±2, ±2)` are excluded. Exercise the same cases around both positive and negative world chunk coordinates.

## Xaero vertical identity

C2 uses Xaero World Map's signed `caveLayer` identity, not an invented Y slice:

- surface is `Integer.MAX_VALUE`;
- full or single-depth mode is `Integer.MIN_VALUE`;
- an ordinary cave layer is arithmetic `caveStart >> 4`;
- `caveDepth` controls sampling thickness but is not part of the persistent layer key.

The singleplayer direct-save gate reads the destination tile region's exact `caveLayer`. Successful live World Map terrain fetching records the 13 horizontally eligible chunks for the current `writingLayer`. Frozen legacy cache import records its owning region's exact layer.

## Runtime matrix

1. Set Minecraft render distance to a visibly larger value such as 16 chunks. Confirm C2 does not change it.
2. At a fresh boundary, confirm both Minimap and World Map discover every included radius-two offset and do not discover excluded rendered chunks. Repeat across negative chunk coordinates.
3. Configure a native Xaero distance tighter than 2 and confirm it remains the tighter boundary.
4. Leave the area and confirm newly mapped terrain remains visible. Confirm every pre-C2 surface and cave map remains visible and unchanged.
5. In a fresh world without v1 history, test the same horizontal chunk independently at the surface, one ordinary cave layer, an adjacent cave layer, and full or single-depth mode. Eligibility in one layer must not authorize another.
6. For each representative layer, save or generate an outside-circle chunk without approaching it and force World Map direct-save reconstruction. It must remain undiscovered until a successful live World Map fetch establishes that exact layer's eligibility.
7. Test retry timing explicitly: deny a direct-save read before the first successful live fetch, then enter the circle and confirm a later live fetch and retry map the layer normally.
8. Upgrade a disposable C1 world. Confirm the v1 history bytes remain identical, every valid v1 chunk stays visible across all layers, and new discovery is appended only to `xaero-discovery-radius.layers-v2.history`.
9. Confirm pre-existing Xaero cache and map files are not deleted, rewritten, invalidated, or hidden. Verify an exact frozen `.xwmc` or `.xwmc.outdated` import authorizes its actual layer without authorizing another layer.
10. Restart the client, reload the world, and travel between dimensions. Confirm exact-layer records persist and each dimension remains independent.
11. In disposable failure copies, exercise malformed or interrupted v1 and v2 state. Confirm the companion remains fail-open across restart, never rewrites v1, and never hides existing maps.
12. Test multiplayer separately. Confirm live horizontal discovery remains circular and persistent, while the singleplayer Anvil direct-save history gate is not applied.
13. With freecam or another camera mod, confirm Minimap and World Map follow Xaero's current camera-centered scan and remain aligned with persistent eligibility.
14. Regress waypoints, radar, navigation, zoom, cave and light overlays, normal Minimap and World Map rendering, dimension travel, client shutdown, and existing map visibility.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if any exact dependency or artifact identity differs, a required Mixin fails, render distance changes, an excluded chunk maps, an included chunk fails to map, one new layer authorizes another, direct-save reconstruction bypasses the gate, retry timing strands eligible terrain, v1 is rewritten, prior map data disappears, history fails across restart, a crash occurs, or unrelated Xaero behavior regresses. Do not accept Canary 2 from static evidence alone.
