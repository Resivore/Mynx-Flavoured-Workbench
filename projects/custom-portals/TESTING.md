# Testing

The exact accepted Canary 2 remains `RUNTIME_PASS` for the aggregate intended
scope reported by the user. This migration performed no Minecraft deployment
or runtime testing. Canary 1 failed during client initialization before the
title screen because its `GuiMixin` targeted `Gui`; Canary 2 contains the
narrow `Hud` owner correction. The numbered cases below remain the focused
regression procedure, not separately reported historical observations.

Use exact retained
`artifacts/custom-portals-26.2-4.0.0+26.2-port-canary2.jar` (SHA-256
`13fd0e76748fcc3ef963bcc2f4a820d5fc2d473a90ad5d5988cbe0129c2be148`)
with Minecraft Java 26.2, Fabric Loader 0.19.3 or newer, Fabric API for 26.2,
and a compatible YACL v3. Mod Menu is optional. Before launch, verify
`config/customportals.json` has SHA-256
`eb3b7d63a1db7090a79ec6175d33709f5930e0b0467749c5f0e04801b075222c`
and these exact semantics:

- `unlimitedRange: true` and `alwaysInterdim: true`;
- `alwaysHaste: "CREATIVE"`, `privatePortals: false`, and `redstone: "OFF"`;
- both sound mute values `false`;
- ranges `100`, `1000`, and `10000`; and
- unrestricted frame selection through `isWhitelist: false` and
  `filteredBlocks: []`.

## Runtime regression procedure

1. Launch to the title screen and confirm there is no dependency, mixin, or
   `extractPortalOverlay` initialization failure.
2. In a disposable world, craft a representative upstream catalyst and confirm
   its recipe and cost are unchanged.
3. Build and activate an ordinary portal from one consistent valid frame
   material with the matching colored catalyst; confirm appearance and sound.
4. Build a compatible same-color, same-frame pair within 100 blocks in one
   dimension; confirm bidirectional linking and traversal.
5. Build a compatible pair more than 100 blocks apart, preferably also more
   than 10,000 blocks apart, without enhancer or infinity runes; confirm
   bidirectional linking and traversal.
6. Build compatible Overworld and Nether portals without a gate rune; confirm
   cross-dimensional traversal and return traversal.
7. During player traversal, confirm arrival position, orientation, delay,
   effects, and cooldown without suffocation, falls, loops, or bounce-back.
8. Send a safe non-player entity through a linked pair and confirm transfer and
   cooldown without duplication or loss.
9. Save and reload, then fully restart and reload; confirm portal state,
   linking, and traversal persist.
10. Break one portal, confirm clean unregistration, recreate it, and confirm no
    ghost or stale destination remains.
11. Confirm ordinary frame selection remains unrestricted and the preserved
    redstone, sound, and creative-mode haste defaults did not change.
12. Inspect `logs/latest.log` for Custom Portals, mixin, YACL, payload,
    networking, registry, component/codec, save, teleport, rendering, or
    missing-resource errors.

Enhancer, infinity, and gate rune restriction checks are outside the accepted
configuration because those restrictions are disabled; do not remove or alter
their items, blocks, recipes, IDs, or serialized fields. Dedicated-server
validation remains unperformed. Stop and record a failure or inconclusive
result on any crash, link/traversal/persistence failure, recipe drift, missing
content, identifier/serialization error, or unexplained runtime log error.
