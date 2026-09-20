# BGE C85 Enderscape item-tint fallback manual verification

Current candidate: `BGE C85.jar`

- Embedded version: `4.2.29-bge.canary85.ender-item-tint-fallback+26.2`
- SHA-256: `05c3df94913ccd343e9aab7afd9c7869e5bd6d41b50d0d1d21b8afff31db0510`
- Source checkpoint: `71f9402f453cdb29d40ad64acd9687d62bc1b8e2`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Immediate predecessor: exact C84 `BGE C84.jar`, SHA-256 `9c3a7af8eb86ffabcf5ddc3c7ba96134fb4322c6b92c3530dc8788edac015759`.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for the exact C85 bytes above. It does not authorize inspecting, creating, selecting, or modifying any protected or retired Minecraft profile.

## C84 predecessor evidence — does not transfer to C85

The observed C84 startup fault is specific to `cnm_terrain_slabs_compat 4.2.28-bge.canary84.ender-material-state-bridge+26.2`: while generating the Veiled Leaves Layer item, C84 rejects the exact valid Enderscape model-only `enderscape:items/veiled_leaves.json` because it has no item `tints` array. C85 accepts that canonical form without replacing C84's block/source-provider tint route. The report is not a C85 runtime result, and C85 has no gameplay-runtime observation.

The reported C83 Blistered Magnia `POLARITY` callback fault remains C83-only evidence; C84's explicit material-state bridge is retained unchanged by C85.

The retained C80 owner observations (Chiseled Resin Bricks and Chiseled Cinnabar PASS; Purpur side texture and missing existing-CNM forms FAIL) remain exact-C80 evidence only. Do not transfer them to C85.

## C85 startup and state-bearing Enderscape geometry

1. With Enderscape 3.0.2+mc26.2 and stock Clutter No More 2.0.7+26.2, load all nine roles (source, Slab, Stair, Wall, Vertical Slab, Step, Layer, Corner, and Quarter Column) for `nebulite_block`, Alluring Magnia, Repulsive Magnia, Blistered Magnia, and Blinklamp. Confirm each role registers once and does not create a second CNM family.
2. For Nebulite and both Magnia forms, confirm the derived roles preserve the expected power/signal behavior. For Blistered Magnia, placement adjacent to canonical/BGE Magnia selects `none`, `alluring`, or `repulsive` polarity as applicable; re-evaluate scheduled changes, signal behavior, sound, dynamic map color, and light level (`0` unpolarized, `14` polarized) across every role.
3. Start with the exact pinned Enderscape 3.0.2+mc26.2 artifact and confirm resource generation handles `enderscape:veiled_leaves` without a startup crash. Inspect BGE-derived Veiled Leaves in inventory and world so the source-provider block tint remains present; record any crash report with the exact JAR SHA-256 and loaded-mod versions.
4. Confirm Blistered Magnia remains free of C83's absent-`POLARITY` callback crash during startup or placement. Record any crash report with the exact JAR SHA-256 and loaded-mod versions.
5. For Blinklamp, test each luminance state (`0` through `7`) across every derived role. Confirm state transitions, copied light behavior, and the model progression `0`, `1/2`, `3/4`, `5/6`, `7` map to the audited five provider luminance model textures.
6. Recheck ordinary geometry state independently: slab type, stair shape/facing, wall arms, vertical orientation, step, layer, corner, and quarter-column state must still work alongside material state; one must not replace the other.

## Exact Enderscape audit boundaries

1. Recheck the C83 runtime-resource closure: CNM-resolved families retain Corner, Quarter Column, and Layer after a normal client resource reload, and generated resources resolve under their own namespace.
2. For all 33 audited Enderscape sources, verify source identity and generated geometry/resources. The audit records projected material/geometry contracts; it deliberately does not claim that unprojected provider-specific source interactions are inherited by BGE geometry.
3. In particular, source-only behavior for Void Shale and overgrowth/path sources (natural/iteration/stress interactions, attachment/survival/random-tick transforms, flattening, bonemeal, or provider-specific collision) remains canonical-provider behavior unless an exact C85 generated-role observation is recorded. Do not infer it from appearance or controlled tests.

## Regression and evidence discipline

1. Recheck representative Macaw's Paths, Mynx Trees, Ribbits, Building But Better, and native CNM families for retained registration, placement, collision, drops, canonical ownership, and switching behavior.
2. Recheck C78 Stair/Wall surface behavior and C71/C72 Farmland Slab behavior separately if those scopes are relevant.
3. For every observation, record the exact candidate SHA-256, source/mod versions, datapack configuration, reproducible sequence, expected result, and observed result. Controlled build, static checks, and GameTests do not establish gameplay acceptance.

## Retention and rollback

Retain `BGE C85.jar` by its exact SHA-256 before runtime work. Keep C84 as immediate predecessor provenance, C72 as rollback provenance, and C70 as the accepted release; do not overwrite a retained artifact.
