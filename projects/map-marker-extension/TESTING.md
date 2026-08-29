# Testing

## Current gate

**NOT DEPLOYED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Exact current C7 is the two-artifact candidate
`map-marker-extension-0.4.0-canary7.jar` plus
`map-marker-extension-icons-0.4.0-canary7.zip`. It passed controlled Java 25
build, test, and archive checks, but none of those checks are Minecraft runtime
evidence. This migration did not deploy either artifact, launch Minecraft,
change a Test Slot or accepted baseline, mutate the dedicated Workbench, or
access the protected gameplay instance.

Treasure X C4 remains the accepted binary baseline and rollback. Do not load
C4 and C7 together. C7 persists custom map-decoration holder keys, so C4 is not
a data-safe rollback for a world after C7 has normalized maps; use a disposable
or restorable world and restore its pre-C7 backup before removing C7.

## Exact retained evidence

- Current C7 JAR: 55,499 bytes, SHA-256
  `09A3BEC8A74B333B65AB34EE6C9AACE85B27EEEA46C82DBF800A452429F5A228`,
  source/build checkpoint `883f0c1a165c2b783d27e5da67bd90e038204af8`.
- Current C7 icon pack: 34,938 bytes, SHA-256
  `E88B18402275139A28A5FC9C1CE860937138D0E665CBD842EDEC5DE6EDA2521C`,
  retained with C7 at `5e89daada6d6e9e9826bdb945761b86b34090b37`.
- Accepted/rollback Treasure X C4: 31,465 bytes, SHA-256
  `9D5E5C22873417346247EAD34D87795E3F4AA84EAF0674345D4CA477EF3AE6E7`,
  source `3aac5ff7259a8bbb4bcac2a6147749589ee94bff`, accepted at
  `67a43eb692c6ffb93d3db96db26199a30a0b1510`.
- C7's historical controlled validation used Java 25.0.4+7 and Gradle 9.5.1:
  47 tests in 11 suites passed with zero failures, errors, or skips. Migration
  verification reproduced the same 47/47 result. C7 remains runtime untested.
- Exact C4 was not separately runtime-tested. The genuine historical focused
  observation belongs to C3: World Map icons were visible, correctly
  positioned, and upright. C4 was accepted as a narrowly proven positive 2x
  presentation-only successor; that evidence does not transfer to C7.

## Next controlled runtime matrix

Run this only under separately authorized deployment ownership in a disposable
or restorable world after verifying the exact C7 JAR and ZIP hashes above and
ensuring every Treasure X C1-C6 artifact is disabled.

1. Launch a client and, where applicable, a server with Minecraft 26.2,
   Fabric Loader 0.19.3 or newer, Fabric API 0.157.0+26.2, Xaero Minimap
   26.4.2, Xaero World Map 1.44.2, and exactly one MME candidate. Confirm clean
   registration, login, resource reload, and shutdown without relevant Mixin,
   registry, atlas, model, or compatibility errors.
2. Obtain representative maps for all formerly shared legacy groups and
   buried treasure. Confirm all 17 custom identities remain distinct while
   vanilla buried treasure remains `minecraft:red_x`.
3. Open the maps in vanilla and confirm each custom holder resolves its native
   `map_marker_extension:poi_icons/<id>` marker; disable and re-enable the icon
   pack to verify the bundled fallback and pack override without any global
   `assets/minecraft` replacement.
4. Inspect filled maps in inventory and hand. Confirm the 18 separate
   `map_sprites` item models select the correct art, remain separate from
   opened-map marker art, and fall back to the wrapped base model when needed.
5. Carry maps in main inventory and offhand, then add, remove, and swap them.
   Confirm Xaero targets are possession-gated, ephemeral, deduplicated, and
   removed promptly when no matching map is carried; no waypoint or persistent
   Xaero object may be created.
6. On Xaero Minimap, verify exact target coordinates, Overworld-only dimension
   filtering, in-bounds admission, and rejection of out-of-bounds markers.
7. On Xaero World Map, verify exact coordinates, upright `V1` to `V0` sprite
   orientation, positive centered 2x scale, and no mirrored, inverted, offset,
   or dimension-leaking marker.
8. If Compass Ribbon is present, verify only that its existing generic native
   holder path displays the live marker asset. MME must create no direct
   Compass dependency, link, or alternate marker ownership path.
9. Confirm normalization changes only `MAP_DECORATIONS["+"]` when its holder
   matches the identity's expected legacy type, preserves exact X/Z/rotation,
   leaves unrelated decorations and every other ItemStack component unchanged,
   and is idempotent on repeated scans.
10. Save, exit, and reload the disposable world. Confirm C7 marker identities
    persist and render correctly with C7 still installed, then restore the
    pre-C7 world backup before testing the accepted C4 binary rollback.

Stop and record `FAIL` or `INCONCLUSIVE` for any crash, registration or atlas
error, collapsed marker identities, unexpected buried-treasure replacement,
changed map coordinate/component, non-idempotent rewrite, wrong item art,
missing fallback, global vanilla override, Xaero waypoint persistence,
dimension leak, stale target, inverted/mirrored/offset World Map sprite,
incorrect scale, save/reload loss, or inability to restore the disposable
world safely. Do not infer unobserved rows from a launch-only pass.
