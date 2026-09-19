# BGE C80 CNM two-phase binding manual verification

Current candidate: `BGE C80.jar`

- Embedded version: `4.2.24-bge.canary80.cnm-two-phase+26.2`
- SHA-256: `5bc7c23a3724a1e20bc2142459f33d2a45a19295c27d40197c5fc48d6e43b0fc`
- Source checkpoint: `3c3be0ea28d3de809f1414dd7e8b6eab73930ee2`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Immediate predecessor: exact C79 `BGE C79.jar`, SHA-256 `7e3265c8746547465ede1a48b31c3db06370de84331b8ff9e35a2ce450650a91`.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`. Its owner-reported aggregate runtime `PASS` applies only to C72 and does not transfer to C80.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for the exact C80 bytes above. It does not authorize inspecting, creating, selecting, or modifying a protected or retired Minecraft profile.

## Native C80 resource-closure regressions

1. In a permitted, user-selected runtime environment, obtain `minecraft:chiseled_resin_bricks` and `minecraft:chiseled_cinnabar` plus their BGE horizontal Slab, Stair, Wall, Vertical Slab, Step, Layer, Corner, and Quarter Column forms. Confirm source texture, placement, collision, drops, and normal family switching behavior match the established ordinary-material contract.
2. Obtain `minecraft:purpur_pillar` and its BGE forms. Confirm the source, horizontal Slab, Stair, Vertical Slab, Step, Layer, Corner, and Quarter Column retain their valid axis-aware placement and rendering behavior.
3. Place Purpur Pillar Walls in several post/arm arrangements. They must be ordinary WallBlocks with no `AXIS` property. Their placed post, side, and tall-side faces must preserve the intended side/end column presentation; their inventory item must use an ordinary wall silhouette.

## C80 CNM two-phase behavior

1. With stock Clutter No More 2.0.7+26.2 present, exercise ordinary CNM Horizontal Slab/Stair sources that BGE already supports. Confirm their existing Vertical Slab, Step, Layer, Corner, and Quarter Column roles still switch as one family with no duplicate role or catalog entry.
2. If a datapack removes or blacklists a CNM mapping for an otherwise CNM-admitted source, verify BGE does not present an additional BGE-owned candidate role through CNM family switching. Restore the datapack before checking normal existing provider families.
3. Verify Macaw's Paths, Mynx Trees, Ribbits, and Building But Better retain their established BGE roles and ownership. C80 must not infer a family from a registry name, model JSON, texture path, pillar class, or provider identity.
4. Verify BGE-generated Layer, Corner, and Quarter Column blocks do not cause an additional CNM family admission or duplicate switching role.

## Regression and evidence discipline

1. Recheck representative C78 Stair and Wall forms: ordinary, axis-material, grass-like, glazed-patterned, transparent/authored, and optional-provider forms. Rotation, collision, drops, canonical material identity, and established BGE surface behavior must remain intact.
2. Recheck C71/C72 Farmland Slab tilling, moisture, and lower-water behavior separately if that work is in scope; C80 must not be credited with historic runtime evidence.
3. If an issue is observed, record the exact candidate SHA-256, source/mod versions, relevant data-pack configuration, reproducible placement or switching sequence, observed result, and expected result. Do not infer a lifecycle transition, acceptance, or runtime pass from controlled checks.

## Retention and rollback

Retain `BGE C80.jar` by its exact SHA-256 before runtime work. C79 is the immediate predecessor for comparison; C72 remains the designated rollback provenance and C70 remains the accepted release. Do not overwrite any retained artifact.
