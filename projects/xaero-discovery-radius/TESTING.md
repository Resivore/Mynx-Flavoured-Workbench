# Testing

Canary 3 is the current successor and is `STATIC_PASS`, `NOT_DEPLOYED`, and `RUNTIME_UNTESTED`. Canary 2 remains the accepted `READY_TO_TEST_VERIFIED` / user-reported aggregate `RUNTIME_PASS` baseline; no Canary 3 runtime observation is supplied or inferred. Java 25 / Gradle 9.5.1 / Fabric Loom 1.17.19 `clean test build --no-daemon` passed 34 focused tests in seven suites with no failures, errors, or skips.

The current artifact is `artifacts/xaero-discovery-radius-0.1.2-canary3.jar`, 34,474 bytes, SHA-256 `5524dfa84e35ea5c31263df4705daca3718df7a095f5c544bb731190dcc0cf95`. The accepted baseline remains `artifacts/xaero-discovery-radius-0.1.1-canary2.jar`, 34,577 bytes, SHA-256 `a7d0125dbe13bbcd5418aac39e0e51a32ea79d508d9dc47f2ebd86526a794c99`.

## Future regression procedure

Use disposable world copies with known existing surface and cave map data. Never use the protected gameplay instance. Reverify the current artifact and the exact Xaero Minimap 26.4.2, Xaero World Map 1.44.2, and embedded XaeroLib 1.7.1 validation-baseline hashes before launch.

1. With radius 2 and a larger Minecraft render distance, exercise the complete inclusive 5 by 5 / 25-chunk square around positive and negative camera chunks in both Minimap and World Map. Verify all four corners at offsets (+2,+2), (+2,-2), (-2,+2), and (-2,-2) discover; (+3,0), (-3,0), (0,+3), (0,-3), and (+3,+3) remain undiscovered. A tighter native Xaero distance must remain tighter.
2. Confirm prior surface/cave maps remain visible, newly mapped terrain persists, and Minecraft render distance is unchanged.
3. Exercise the same horizontal chunk independently at surface, adjacent ordinary cave layers, and full/single-depth mode; one layer must not authorize another, including direct-save reconstruction before and after its first successful live fetch.
4. Upgrade a disposable C1 world and confirm v1 history remains byte-identical as an all-layer wildcard while new entries use only the dimension-local v2 layer journal; verify frozen cache imports authorize only their exact layer.
5. Across restart and dimension travel, confirm exact-layer persistence and dimension isolation. In disposable corrupt/interrupted copies, confirm durable fail-open behavior without deletion, rewriting, or concealment of existing map data.
6. Regress multiplayer horizontal behavior, camera/freecam centering, waypoints, radar, navigation, zoom, overlays, normal rendering, shutdown, and existing map visibility.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if any exact dependency or artifact identity differs, a required Mixin fails, render distance changes, an excluded chunk maps, an included chunk fails to map, one new layer authorizes another, direct-save reconstruction bypasses the gate, retry timing strands eligible terrain, v1 is rewritten, prior map data disappears, history fails across restart, a crash occurs, or unrelated Xaero behavior regresses.
