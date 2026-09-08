# Testing

## Current gate

**NOT DEPLOYED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Exact current C11 is the two-artifact candidate
`map-marker-extension-0.4.0-canary11.jar` plus
`map-marker-extension-icons-0.4.0-canary11.zip`. It passed controlled Java 25
build, test, archive, and deterministic-build checks. Those checks do not
establish Minecraft runtime behavior. This task did not deploy either artifact,
launch Minecraft, change a Test Slot or accepted baseline, mutate the dedicated
Workbench, or access the protected gameplay instance.

C10 has scoped external/user runtime evidence of `FAIL` only for Compass Ribbon
POI visual scale: all 17 custom MME icons appeared and their identities and
selections functioned, but the icons remained visibly tiny rather than matching
the apparent footprint of vanilla `minecraft:red_x`. No unrelated C10 runtime
row is inferred from that observation. C11 has no runtime evidence yet.

Treasure X C4 remains the accepted binary baseline and rollback. Do not load C4
and C11 together. C4 is not a data-safe rollback for a world after C7, C8, C9, C10, or C11 has
persisted custom map-decoration holder keys; use a disposable or restorable
world and restore its pre-C7 backup before removing C8.

## Exact retained evidence

- Current C11 JAR: 57,903 bytes, SHA-256
  `92DF3CCA27F3D4F6CFCB0ED52BA26874DC5F86EB756601B3A75771CFD7E9CBAF`,
  implementation checkpoint `dff852f9a44af75f8e881cf5d7050314c439374d`.
- Current C11 icon pack: 27,297 bytes, SHA-256
  `A277BAF8AA3A12740EAD9CA0D721CDE98D013FBD45FE38F647921E1964598866`.
  It contains exactly the 17 custom native `poi_icons`, derived from immutable
  `originals/assets/poi_icons` artwork by nearest-neighbor reframing into the
  standard 8-by-8 map-decoration canvas. The artwork occupies a 7-by-8,
  8-by-8, 8-by-7, or 8-by-6 alpha footprint according to its preserved aspect
  ratio; all 18 C9 item-side `map_sprites` remain byte-identical to C9.
- Retained failed C10 JAR: 59,016 bytes, SHA-256
  `94F657B7083669F32D8500794EB6DC7283CDC8A331F592B1AEB02ADC51B1396C`,
  and icon pack: 28,467 bytes, SHA-256
  `EE66E2A1022EE4FEFD16F7428C4E71682065752778F658139641354C233ADA29`,
  source `e8fd269901ae3c23b74691b7b09b80d56feaace6`.
- Retained C9 predecessor JAR: 58,720 bytes, SHA-256
  `5281481E599A14175C1D567A0ACF440D91B6EBCA0D6C171673F09D267772201B`;
  icon pack: 28,172 bytes, SHA-256
  `6AD89721C6AE628F2E3995132BCF0FA98F6E7FA43749B0A2695B7DB8A2B97A42`,
  source `211af5f505eddff29d8adb03ca0ca7db95e58e4b`.
- Retained C8 predecessor JAR: 58,720 bytes, SHA-256
  `C3DA533B207050855C691584DB6BC3C85B383A9EC8E338737F7986D426668CA2`;
  icon pack: 34,938 bytes, SHA-256
  `E88B18402275139A28A5FC9C1CE860937138D0E665CBD842EDEC5DE6EDA2521C`.
  External user runtime evidence is a `FAIL` only for its too-small filled-map
  inventory/item presentation. Marker identity and icon selection worked; no
  other C8 runtime rows are inferred.
- Accepted/rollback Treasure X C4: 31,465 bytes, SHA-256
  `9D5E5C22873417346247EAD34D87795E3F4AA84EAF0674345D4CA477EF3AE6E7`,
  source `3aac5ff7259a8bbb4bcac2a6147749589ee94bff`, accepted at
  `67a43eb692c6ffb93d3db96db26199a30a0b1510`.
- Temurin 25.0.4.1+1 and Gradle 9.5.1 offline `clean test build` passed
  59 tests in 13 suites with zero failures, errors, or skips. Two clean builds
  produced byte-identical C11 JAR and icon-pack output.
- Static inspection of official Compass Ribbon 2.9.0 for 26.2, CurseForge file
  8261473, covered the exact 469,546-byte JAR with SHA-256
  `0F3A03C4ECC8B78420ECDCB4CC126810EA10581694AB808141D7F81B16D59905`.
  It consumes every standard `MAP_DECORATIONS` entry through the native
  `MapDecorationType` holder and live map-decoration asset without a
  vanilla-only type whitelist. Its `renderMarker` resolves that live sprite,
  blits its complete UV bounds into a fixed -1..1 quad, then scales the quad by
  4. Minecraft 26.2 likewise renders a complete atlas sprite in a fixed quad.
  Thus a 32-by-32 whole-canvas version of the same image has the same normalized
  visible footprint as C10's 16-by-16 source. The exact native red X is an
  8-by-8 canvas with alpha bounds `(0,0)..(7,7)`. This is architectural/static
  evidence only; C11 Compass Ribbon runtime success remains untested.

## Next controlled runtime matrix

Run this only under separately authorized deployment ownership in a disposable
or restorable world after verifying the exact C11 JAR and ZIP hashes above and
ensuring C7 and every Treasure X C1-C6 artifact are disabled.

1. Launch a client and, where applicable, a server with Minecraft 26.2, Fabric
   Loader 0.19.3 or newer, Fabric API 0.157.0+26.2, Xaero Minimap 26.4.2,
   Xaero World Map 1.44.2, and exactly one MME candidate. Confirm clean
   registration, login, resource reload, and shutdown without relevant Mixin,
   registry, atlas, model, or compatibility errors.
2. Before the larger matrix, obtain representative (preferably all 18) MME
   exploration maps. Confirm each keeps its distinct selected inventory icon,
   is crisp, and has an apparent inventory scale comparable to Ribbits' known
   good explorer map rather than C8's tiny presentation. Confirm opened-map
   artwork still looks correct, vanilla buried treasure remains
   `minecraft:red_x`, and resource reload reports no missing model or texture.
3. Confirm all 17 custom identities remain distinct and their opened-map icons
   are visibly crisp at the intended 2x native artwork size, while normalization,
   coordinates, Xaero filtering, Minimap native scale, World Map accepted 2x
   scale, and upright UV correction remain unchanged from C8.
4. With Ribbits installed, obtain a successful, exact
   `minecraft:filled_map` carrying a real `minecraft:map_id`,
   `ribbits:ribbit_village_explorer_map=true`, and the native
   `ribbits:ribbit_village` decoration. Confirm MME reads the target from the
   ordinary decoration entry and does not replace its holder, coordinates, or
   live Ribbits-owned asset.
5. Carry that map in each configured inventory/offhand position. Confirm one
   possession-gated ephemeral target appears at the exact location in Xaero
   Minimap and Xaero World Map, then disappears promptly when the map is no
   longer carried. No waypoint or persistent Xaero object may be created.
6. Confirm the successful Ribbits map retains Ribbits' mushroom-map inventory
   presentation, its opened map retains the Ribbits marker, and resource reload
   does not cause either the Ribbits or MME filled-map model wrapper to mask the
   other, regardless of initializer order.
7. Remove MME and confirm the native Ribbits map, opened-map marker, item art,
   persistence, copying, synchronization, and item-frame behavior still work.
   Reinstall MME without Ribbits and confirm there is no classlink, registry,
   asset, or startup failure and no Ribbits behavior is contributed.
8. Confirm a map with only the native Ribbits holder but no exact success marker
   is not projected by MME. Repeat with a false marker, a missing `map_id`, an
   unrelated marker/type, and the failed Uncharted Ribbit Map; none may become
   a Ribbits target.
9. With exact Compass Ribbon 2.9.0 present, verify its existing generic native
   holder path displays the live `ribbits:ribbit_village` marker. MME must add
   no direct Compass dependency, alternate marker, or copied Ribbits asset.
10. Save, exit, and reload the disposable world. Confirm both C11's existing
   normalized identities and the externally owned Ribbits identity remain
   correct, then restore the pre-C7 world backup before testing the accepted C4
   binary rollback.

Stop and record `FAIL` or `INCONCLUSIVE` for any crash, registration or atlas
error, C7 identity regression, unexpected holder rewrite, duplicate marker,
wrong coordinates or art, model-wrapper masking, generic/failed map projection,
Ribbits-absent classlink, persistent Xaero waypoint, stale target, dimension
leak, save/reload loss, Compass Ribbon failure, or inability to restore the
disposable world safely. Do not infer unobserved rows from a launch-only pass.
