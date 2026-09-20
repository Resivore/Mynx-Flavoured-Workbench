# BGE C86 Enderscape Layer-facing binding manual verification

Current candidate: `BGE C86.jar`

- Embedded version: `4.2.30-bge.canary86.ender-layer-facing-binding+26.2`
- SHA-256: `0767ac014444264a418dec21e23009190552d10e6c2508043de46ee918b1a715`
- Source checkpoint: `cf9c26fffcb3e4527fac4e30a608173626991129`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Immediate predecessor: exact C85 `BGE C85.jar`, SHA-256 `05c3df94913ccd343e9aab7afd9c7869e5bd6d41b50d0d1d21b8afff31db0510`.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for the exact C86 bytes above. It does not authorize inspecting, creating, selecting, or modifying any protected or retired Minecraft profile.

## C85 predecessor evidence — does not transfer to C86

The supplied C85 world/datapack-load failure is specific to `cnm_terrain_slabs_compat 4.2.29-bge.canary85.ender-item-tint-fallback+26.2`: `BgeMaterialBindings.validateAndFreeze()` reports an unclassified shared canonical `facing` for the five Enderscape Layer families. C86 explicitly classifies that exact shared DirectionalBlock property and does not otherwise weaken binding validation. The report is not a C86 runtime result, and C86 has no gameplay-runtime observation.

The reported C84 Veiled Leaves startup fault and C83 Blistered Magnia `POLARITY` callback fault remain predecessor-only evidence; their C85/C84 fixes are retained unchanged by C86.

The retained C80 owner observations (Chiseled Resin Bricks and Chiseled Cinnabar PASS; Purpur side texture and missing existing-CNM forms FAIL) remain exact-C80 evidence only. Do not transfer them to C86.

## C86 canonical-facing world-load regression

1. With the exact pinned Enderscape 3.0.2+mc26.2 and stock Clutter No More 2.0.7+26.2, create and load both a new and an existing world. Confirm datapack/world loading completes with no `BgeMaterialBindings.validateAndFreeze()` fatal validation error.
2. For `veiled_end_stone`, `celestial_overgrowth`, `corrupt_overgrowth`, `celestial_path`, and `corrupt_path`, create/use the BGE Layer forms through normal supported play. Confirm each family registers once, and completing a Layer produces the canonical source without a binding-validation failure.
3. If commands or state inspection are available, repeat the full-Layer completion for each valid cardinal and vertical exposed face. Record the source blockstate's resulting `facing`; it must match the completed Layer's face rather than silently resetting to a default.
4. Recheck an existing axis-bearing family such as `veiled_log`: a completed Layer's canonical `axis` must remain intact, confirming C86 did not replace established canonical-property projection.

## Exact Enderscape audit boundaries

1. Recheck the C83 runtime-resource closure: CNM-resolved families retain Corner, Quarter Column, and Layer after a normal client resource reload, and generated resources resolve under their own namespace.
2. For all 33 audited Enderscape sources, verify source identity and generated geometry/resources. The audit records projected material/geometry contracts; it deliberately does not claim that unprojected provider-specific source interactions are inherited by BGE geometry.
3. In particular, source-only behavior for Void Shale and overgrowth/path sources (natural/iteration/stress interactions, attachment/survival/random-tick transforms, flattening, bonemeal, or provider-specific collision) remains canonical-provider behavior unless an exact C86 generated-role observation is recorded. Do not infer it from appearance or controlled tests.

## Regression and evidence discipline

1. Recheck representative Macaw's Paths, Mynx Trees, Ribbits, Building But Better, and native CNM families for retained registration, placement, collision, drops, canonical ownership, and switching behavior.
2. Recheck C78 Stair/Wall surface behavior and C71/C72 Farmland Slab behavior separately if those scopes are relevant.
3. For every observation, record the exact candidate SHA-256, source/mod versions, datapack configuration, reproducible sequence, expected result, and observed result. Controlled build, static checks, and GameTests do not establish gameplay acceptance.

## Retention and rollback

Retain `BGE C86.jar` by its exact SHA-256 before runtime work. Keep C85 as immediate predecessor provenance, C72 as rollback provenance, and C70 as the accepted release; do not overwrite a retained artifact.
