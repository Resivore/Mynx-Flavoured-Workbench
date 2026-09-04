# Testing

## Current gate

**NOT DEPLOYED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Exact current C8 is the two-artifact candidate
`map-marker-extension-0.4.0-canary8.jar` plus
`map-marker-extension-icons-0.4.0-canary8.zip`. It passed controlled Java 25
build, test, archive, and deterministic-build checks. Those checks do not
establish Minecraft runtime behavior. This task did not deploy either artifact,
launch Minecraft, change a Test Slot or accepted baseline, mutate the dedicated
Workbench, or access the protected gameplay instance.

Treasure X C4 remains the accepted binary baseline and rollback. Do not load C4
and C8 together. C4 is not a data-safe rollback for a world after C7 or C8 has
persisted custom map-decoration holder keys; use a disposable or restorable
world and restore its pre-C7 backup before removing C8.

## Exact retained evidence

- Current C8 JAR: 58,720 bytes, SHA-256
  `C3DA533B207050855C691584DB6BC3C85B383A9EC8E338737F7986D426668CA2`,
  implementation checkpoint `a7ddb6afa6ed9cd620f49a28522c7866fdd35f04`.
- Current C8 icon pack: 34,938 bytes, SHA-256
  `E88B18402275139A28A5FC9C1CE860937138D0E665CBD842EDEC5DE6EDA2521C`.
  Its bytes are identical to C7 because Ribbits continues to own its native
  marker and item artwork and MME's existing 17 marker and 18 item assets did
  not change.
- Accepted/rollback Treasure X C4: 31,465 bytes, SHA-256
  `9D5E5C22873417346247EAD34D87795E3F4AA84EAF0674345D4CA477EF3AE6E7`,
  source `3aac5ff7259a8bbb4bcac2a6147749589ee94bff`, accepted at
  `67a43eb692c6ffb93d3db96db26199a30a0b1510`.
- Temurin 25.0.4.1+1 and Gradle 9.5.1 offline `clean test build` passed
  56 tests in 13 suites with zero failures, errors, or skips. Repeated clean
  builds produced byte-identical C8 JAR and icon-pack output.
- Static inspection of official Compass Ribbon 2.9.0 for 26.2, CurseForge file
  8261473, covered the exact 469,546-byte JAR with SHA-256
  `0F3A03C4ECC8B78420ECDCB4CC126810EA10581694AB808141D7F81B16D59905`.
  It consumes every standard `MAP_DECORATIONS` entry through the native
  `MapDecorationType` holder and live map-decoration asset without a
  vanilla-only type whitelist. This is architectural evidence only; Compass
  Ribbon runtime compatibility remains untested.

## Next controlled runtime matrix

Run this only under separately authorized deployment ownership in a disposable
or restorable world after verifying the exact C8 JAR and ZIP hashes above and
ensuring C7 and every Treasure X C1-C6 artifact are disabled.

1. Launch a client and, where applicable, a server with Minecraft 26.2, Fabric
   Loader 0.19.3 or newer, Fabric API 0.157.0+26.2, Xaero Minimap 26.4.2,
   Xaero World Map 1.44.2, and exactly one MME candidate. Confirm clean
   registration, login, resource reload, and shutdown without relevant Mixin,
   registry, atlas, model, or compatibility errors.
2. Obtain representative maps for all 18 existing MME identities and buried
   treasure. Confirm all 17 custom identities remain distinct while vanilla
   buried treasure remains `minecraft:red_x` and existing inventory artwork,
   normalization, coordinates, Xaero filtering, orientation, and scale remain
   unchanged from C7.
3. With Ribbits installed, obtain a successful, exact
   `minecraft:filled_map` carrying a real `minecraft:map_id`,
   `ribbits:ribbit_village_explorer_map=true`, and the native
   `ribbits:ribbit_village` decoration. Confirm MME reads the target from the
   ordinary decoration entry and does not replace its holder, coordinates, or
   live Ribbits-owned asset.
4. Carry that map in each configured inventory/offhand position. Confirm one
   possession-gated ephemeral target appears at the exact location in Xaero
   Minimap and Xaero World Map, then disappears promptly when the map is no
   longer carried. No waypoint or persistent Xaero object may be created.
5. Confirm the successful Ribbits map retains Ribbits' mushroom-map inventory
   presentation, its opened map retains the Ribbits marker, and resource reload
   does not cause either the Ribbits or MME filled-map model wrapper to mask the
   other, regardless of initializer order.
6. Remove MME and confirm the native Ribbits map, opened-map marker, item art,
   persistence, copying, synchronization, and item-frame behavior still work.
   Reinstall MME without Ribbits and confirm there is no classlink, registry,
   asset, or startup failure and no Ribbits behavior is contributed.
7. Confirm a map with only the native Ribbits holder but no exact success marker
   is not projected by MME. Repeat with a false marker, a missing `map_id`, an
   unrelated marker/type, and the failed Uncharted Ribbit Map; none may become
   a Ribbits target.
8. With exact Compass Ribbon 2.9.0 present, verify its existing generic native
   holder path displays the live `ribbits:ribbit_village` marker. MME must add
   no direct Compass dependency, alternate marker, or copied Ribbits asset.
9. Save, exit, and reload the disposable world. Confirm both C8's existing
   normalized identities and the externally owned Ribbits identity remain
   correct, then restore the pre-C7 world backup before testing the accepted C4
   binary rollback.

Stop and record `FAIL` or `INCONCLUSIVE` for any crash, registration or atlas
error, C7 identity regression, unexpected holder rewrite, duplicate marker,
wrong coordinates or art, model-wrapper masking, generic/failed map projection,
Ribbits-absent classlink, persistent Xaero waypoint, stale target, dimension
leak, save/reload loss, Compass Ribbon failure, or inability to restore the
disposable world safely. Do not infer unobserved rows from a launch-only pass.
