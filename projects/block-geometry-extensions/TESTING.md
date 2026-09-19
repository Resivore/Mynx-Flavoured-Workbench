# BGE C79 CNM family-bridge manual verification

Current candidate: `BGE C79.jar`

- Embedded version: `4.2.23-bge.canary79.cnm-family-bridge+26.2`
- SHA-256: `7e3265c8746547465ede1a48b31c3db06370de84331b8ff9e35a2ce450650a91`
- Source checkpoint: `704a19b4022af8661a3b2c33a81abf41ea26d8fb`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Immediate predecessor: exact C78 `BGE C78.jar`, SHA-256 `ed2f5592b699532174bc63672f69c3574f01eb752175126bbc702e9cc86a07f0`.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`. Its owner-reported aggregate runtime `PASS` applies only to those exact C72 bytes and does not transfer to C79.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for the exact C79 bytes above. It does not authorize inspecting, creating, selecting, or modifying any protected or retired Minecraft profile.

## Native C79 families

1. In a permitted, user-selected runtime environment, obtain `minecraft:chiseled_resin_bricks` and `minecraft:chiseled_cinnabar` plus their BGE horizontal Slab, Stair, Wall, Vertical Slab, Step, Layer, Corner, and Quarter Column forms. Confirm source texture, placement, collision, drops, and normal family switching behavior match the established ordinary-material contract.
2. Obtain `minecraft:purpur_pillar` and its BGE forms. Confirm the source, horizontal Slab, Stair, Vertical Slab, Step, Layer, Corner, and Quarter Column retain their valid axis-aware placement and rendering behavior.
3. Place Purpur Pillar Walls in several post/arm arrangements. They must be ordinary WallBlocks with no `AXIS` property. Their placed post, side, and tall-side faces must preserve the pillar's intended side/end column presentation; their inventory item must use an ordinary wall silhouette rather than an axis selector.

## Generic external axis-pillar bridge

1. With Enderscape present, verify the bridge exposes exactly one normal nine-role BGE family each for `enderscape:veiled_log` and `enderscape:veiled_wood`; source, Slab, Stair, Wall, Vertical Slab, Step, Layer, Corner, and Quarter Column must switch coherently without duplicate registrations or duplicate ShapeMap roles.
2. For each Veiled source, check all source axes and representative derived orientations. The verified resource side/end faces must remain correct on axis-capable forms. Walls must remain ordinary no-AXIS WallBlocks; compare their inventory silhouette with placed post/arm side/end faces.
3. Verify established optional-provider families (Macaw's Paths, Mynx Trees, Ribbits, and BBB) retain their existing roles and ownership. The C79 bridge must not add a second role for a provider-owned source.
4. A source missing a packaged direct `minecraft:block/cube_column` model, using ambiguous, overridden, or non-direct textures, lacking a BlockItem, or requiring an unsupported stripping transition must remain unadopted. Do not alter provider resources to force this outcome; record only observed behavior.

## Regression and evidence discipline

1. Recheck a C78 Stair and Wall representative: ordinary, axis-material, grass-like, glazed-patterned, transparent/authored, and optional-provider forms. Rotation, collision, drops, canonical material identity, and established BGE surface behavior must remain intact.
2. Recheck C71/C72 Farmland Slab tilling, moisture, and lower-water behavior separately if that work is in scope; C79 must not be credited with historic runtime evidence.
3. If any issue is observed, record the exact candidate SHA-256, source/mod versions, reproducible placement or switching sequence, observed result, and expected result. Do not infer a lifecycle transition, acceptance, or runtime pass from controlled checks.

## Retention and rollback

Retain `BGE C79.jar` by its exact SHA-256 before runtime work. C78 is the immediate predecessor for comparison; C72 remains the designated rollback provenance and C70 remains the accepted release. Do not overwrite any retained artifact.
