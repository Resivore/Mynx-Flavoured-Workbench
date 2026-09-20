# BGE C84 Enderscape material-state bridge manual verification

Current candidate: `BGE C84.jar`

- Embedded version: `4.2.28-bge.canary84.ender-material-state-bridge+26.2`
- SHA-256: `9c3a7af8eb86ffabcf5ddc3c7ba96134fb4322c6b92c3530dc8788edac015759`
- Source checkpoint: `7df5c8ebad8a7721afc80afe354d59d13be78877`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Immediate predecessor: exact C83 `BGE C83.jar`, SHA-256 `c0865a2677fbcdc82b53b751e145a3330757694fd73a064671e8fd6d9cd4b4c9`.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for the exact C84 bytes above. It does not authorize inspecting, creating, selecting, or modifying any protected or retired Minecraft profile.

## C83 predecessor evidence — does not transfer to C84

The reported C83 startup fault is specific to C83: `BlisteredMagniaBlock`'s copied light callback reads `POLARITY`, but its generic BGE-derived block state did not contain that property. C84 introduces the explicit material-state bridge to correct that fault. The report is not a C84 runtime result, and C84 has no gameplay-runtime observation.

The retained C80 owner observations (Chiseled Resin Bricks and Chiseled Cinnabar PASS; Purpur side texture and missing existing-CNM forms FAIL) remain exact-C80 evidence only. Do not transfer them to C84.

## C84 state-bearing Enderscape geometry

1. With Enderscape 3.0.2+mc26.2 and stock Clutter No More 2.0.7+26.2, load all nine roles (source, Slab, Stair, Wall, Vertical Slab, Step, Layer, Corner, and Quarter Column) for `nebulite_block`, Alluring Magnia, Repulsive Magnia, Blistered Magnia, and Blinklamp. Confirm each role registers once and does not create a second CNM family.
2. For Nebulite and both Magnia forms, confirm the derived roles preserve the expected power/signal behavior. For Blistered Magnia, placement adjacent to canonical/BGE Magnia selects `none`, `alluring`, or `repulsive` polarity as applicable; re-evaluate scheduled changes, signal behavior, sound, dynamic map color, and light level (`0` unpolarized, `14` polarized) across every role.
3. Confirm Blistered Magnia no longer crashes during startup or placement because a copied callback reads an absent `POLARITY` property. Record any crash report with the exact JAR SHA-256 and loaded-mod versions.
4. For Blinklamp, test each luminance state (`0` through `7`) across every derived role. Confirm state transitions, copied light behavior, and the model progression `0`, `1/2`, `3/4`, `5/6`, `7` map to the audited five provider luminance model textures.
5. Recheck ordinary geometry state independently: slab type, stair shape/facing, wall arms, vertical orientation, step, layer, corner, and quarter-column state must still work alongside material state; one must not replace the other.

## Exact Enderscape audit boundaries

1. Recheck the C83 runtime-resource closure: CNM-resolved families retain Corner, Quarter Column, and Layer after a normal client resource reload, and generated resources resolve under their own namespace.
2. For all 33 audited Enderscape sources, verify source identity and generated geometry/resources. The audit records projected material/geometry contracts; it deliberately does not claim that unprojected provider-specific source interactions are inherited by BGE geometry.
3. In particular, source-only behavior for Void Shale and overgrowth/path sources (natural/iteration/stress interactions, attachment/survival/random-tick transforms, flattening, bonemeal, or provider-specific collision) remains canonical-provider behavior unless an exact C84 generated-role observation is recorded. Do not infer it from appearance or controlled tests.

## Regression and evidence discipline

1. Recheck representative Macaw's Paths, Mynx Trees, Ribbits, Building But Better, and native CNM families for retained registration, placement, collision, drops, canonical ownership, and switching behavior.
2. Recheck C78 Stair/Wall surface behavior and C71/C72 Farmland Slab behavior separately if those scopes are relevant.
3. For every observation, record the exact candidate SHA-256, source/mod versions, datapack configuration, reproducible sequence, expected result, and observed result. Controlled build, static checks, and GameTests do not establish gameplay acceptance.

## Retention and rollback

Retain `BGE C84.jar` by its exact SHA-256 before runtime work. Keep C83 as immediate predecessor provenance, C72 as rollback provenance, and C70 as the accepted release; do not overwrite a retained artifact.
