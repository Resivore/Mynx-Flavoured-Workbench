# Testing

Canary 2 is the accepted current release and is `STATIC_PASS`, `READY_TO_TEST_VERIFIED`, and `RUNTIME_PASS`. The user reported an aggregate pass for the exact candidate, and no individual runtime cases were supplied or inferred. A Java 25 / Gradle 9.5.1 / Fabric Loom 1.17.19 `clean test build --no-daemon` previously passed 32 focused tests in seven suites with no failures, errors, or skips.

The accepted artifact is `artifacts/xaero-discovery-radius-0.1.1-canary2.jar`, 34,577 bytes, SHA-256 `a7d0125dbe13bbcd5418aac39e0e51a32ea79d508d9dc47f2ebd86526a794c99`.

## Future regression procedure

Use disposable world copies with known existing surface and cave map data. Never use the protected gameplay instance. Reverify the accepted artifact and the exact Xaero Minimap 26.4.2, Xaero World Map 1.44.2, and embedded XaeroLib 1.7.1 hashes before launch.

1. With radius 2 and a larger Minecraft render distance, exercise the inclusive 13-offset circle around positive and negative chunk coordinates in both Minimap and World Map; excluded rendered chunks must remain undiscovered and a tighter native Xaero distance must remain tighter.
2. Confirm prior surface/cave maps remain visible, newly mapped terrain persists, and Minecraft render distance is unchanged.
3. Exercise the same horizontal chunk independently at surface, adjacent ordinary cave layers, and full/single-depth mode; one layer must not authorize another, including direct-save reconstruction before and after its first successful live fetch.
4. Upgrade a disposable C1 world and confirm v1 history remains byte-identical as an all-layer wildcard while new entries use only the dimension-local v2 layer journal; verify frozen cache imports authorize only their exact layer.
5. Across restart and dimension travel, confirm exact-layer persistence and dimension isolation. In disposable corrupt/interrupted copies, confirm durable fail-open behavior without deletion, rewriting, or concealment of existing map data.
6. Regress multiplayer horizontal behavior, camera/freecam centering, waypoints, radar, navigation, zoom, overlays, normal rendering, shutdown, and existing map visibility.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if any exact dependency or artifact identity differs, a required Mixin fails, render distance changes, an excluded chunk maps, an included chunk fails to map, one new layer authorizes another, direct-save reconstruction bypasses the gate, retry timing strands eligible terrain, v1 is rewritten, prior map data disappears, history fails across restart, a crash occurs, or unrelated Xaero behavior regresses.
