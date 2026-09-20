# BGE C88 BBB beam model-resolution manual verification

Current candidate: `BGE C88.jar`

- Embedded version: `4.2.32-bge.canary88.bbb-beam-model-resolution+26.2`
- SHA-256: `fb007b8e5ebd3062e8f9af04ad893be77c87640d49d09cc23482f82883327bf3`
- Source checkpoint: `af37629a9df09209b444afae3f1a0030d1325200`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Immediate predecessor: exact C87 `BGE C87.jar`, SHA-256 `0f639cd769c0dc4f2efa1247391096c16b286fb399d96b54c3095fd464a5255d`.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for the
exact C88 bytes above. It does not authorize inspecting, creating, selecting, or
modifying any protected or retired Minecraft profile.

## BBB beam-stair first-bake regression

1. Use Building But Better with its standard beam forms available. On the first client load, before manually reloading resources, open or place `acacia beam stairs` and its BGE-owned generated forms. There must be no `Missing canonical model for resolved CNM parent: bbb:acacia_beam_stairs` exception, missing model, or missing texture.
2. Repeat the first-load check for every BBB beam material: Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Crimson, Warped, Mangrove, Bamboo, Cherry, and Pale Oak. For each valid admitted beam family, verify BGE Layer, Corner, and Quarter Column generation remains available and uses the material's beam side and top/end appearance.
3. Trigger one ordinary client resource reload, then repeat representative Acacia and non-Acacia beam checks. The same visual resources must remain deterministic; do not substitute or elect a different CNM/ShapeMap canonical parent merely because BBB uses a blockstate-selected model.

## Retained C87 CNM repeated-ShapeMap regression

1. Use stock Clutter No More 2.0.7+26.2 and Enderscape 3.0.2+mc26.2. In one client launch, allow an initial CNM ShapeMap build, then trigger one legitimate later client reload or other ordinary ShapeMap reconstruction. Do not add or remove BGE/CNM blocks between passes.
2. After both passes, inspect the selector/menu and placed forms for `mossy stone`, `polished end stone`, `shadoline`, `mirestone`, `kurodite`, `etched alluring magnia`, `dusk purpur block`, `celestial bricks`, and `murublight bricks`. Each eligible family must still contain exactly one BGE Corner, Quarter Column, and Layer.
3. After both passes, inspect `veiled planks`, `celestial planks`, and `murublight planks`. Each must still contain exactly one normal Wall plus exactly one BGE Corner, Quarter Column, and Layer; no role may duplicate or select a different canonical parent.
4. If log diagnostics are available, confirm each current pass describes that pass's resolved graph rather than reporting a stale role retained from an earlier mapping list. Record both trigger sequence and observed graph/role identities.

## Retained C87 first-load resources and Enderscape visuals

1. On the first client load, before manually reloading resources, open selector/inventory previews and place representative automatic CNM forms. There must be no missing model/texture warning or placeholder for a valid admitted BGE role. Repeat one ordinary client reload and confirm the same resources remain deterministic.
2. Verify all six C87 full families—`stripped veiled log`, `stripped veiled wood`, `stripped celestial stem`, `stripped celestial hyphae`, `stripped murublight stem`, and `stripped murublight hyphae`. Check every normal BGE role, axis-aware placement, side/end textures, and UV orientation. Do not infer or test an invented stripping transition from a derived geometry.
3. Compare `veiled end stone`, `celestial overgrowth`, and `corrupt overgrowth` against the Crimson Nylium BGE structural face/UV behavior while retaining their Enderscape material textures. Compare `celestial path` and `corrupt path` against Dirt Path/Grass Path BGE structural face/UV behavior. This does not imply source-only growth, path, survival, or flattening behavior on derived geometry.
4. Check Void Shale's BGE roles use the exact side appearance on horizontal faces and the exact end appearance on top/bottom faces, with no axis state and no missing `enderscape:block/void_shale` reference.
5. Exercise every exposed Blinklamp luminance state and selector/inventory preview. Every BGE geometry must use the provider's actual state-specific visual resources, retain C84 luminance behavior, and have no missing model/texture.
6. Place and render a Veiled Leaves slab in-world and inspect its inventory/menu preview. It must not crash chunk rendering and must retain the canonical provider appearance when the provider uses a model-owned tint route rather than a registered block tint source. Recheck a native foliage material to ensure its tint behavior is unchanged.

## Retained boundaries and evidence discipline

1. Recheck C87's CNM/Enderscape behavior, C86's completed Enderscape Layer facing behavior for the five DirectionalBlock-derived terrain/path families, and C84's Magnia/Blinklamp material-state behavior, only as regressions. Preserve the existing placement, economy, drop, and canonicalization behavior unless an exact C88 observation differs.
2. The retained C87/C86/C85/C84 and C80 owner observations are predecessor-only evidence and do not establish a C88 result. C88 has no recorded gameplay-runtime observation or acceptance.
3. For every observation, record the exact candidate SHA-256, source/mod versions, datapack configuration, repeatable sequence, expected result, and observed result. Controlled builds, static checks, and GameTests do not establish gameplay acceptance.

## Retention and rollback

Retain `BGE C88.jar` by its exact SHA-256 before runtime work. Keep C87 as
immediate predecessor provenance, C72 as rollback provenance, and C70 as the
accepted release; do not overwrite a retained artifact.
