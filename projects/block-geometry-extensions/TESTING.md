# BGE C87 CNM rebuild-closure manual verification

Current candidate: `BGE C87.jar`

- Embedded version: `4.2.31-bge.canary87.cnm-rebuild-closure+26.2`
- SHA-256: `0f639cd769c0dc4f2efa1247391096c16b286fb399d96b54c3095fd464a5255d`
- Source checkpoint: `45207323fa68d80665a7f5dfe7ec337c36c0dbf3`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Immediate predecessor: exact C86 `BGE C86.jar`, SHA-256 `0767ac014444264a418dec21e23009190552d10e6c2508043de46ee918b1a715`.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for the
exact C87 bytes above. It does not authorize inspecting, creating, selecting, or
modifying any protected or retired Minecraft profile.

## CNM repeated-ShapeMap regression

1. Use stock Clutter No More 2.0.7+26.2 and Enderscape 3.0.2+mc26.2. In one client launch, allow an initial CNM ShapeMap build, then trigger one legitimate later client reload or other ordinary ShapeMap reconstruction. Do not add or remove BGE/CNM blocks between passes.
2. After both passes, inspect the selector/menu and placed forms for `mossy stone`, `polished end stone`, `shadoline`, `mirestone`, `kurodite`, `etched alluring magnia`, `dusk purpur block`, `celestial bricks`, and `murublight bricks`. Each eligible family must still contain exactly one BGE Corner, Quarter Column, and Layer.
3. After both passes, inspect `veiled planks`, `celestial planks`, and `murublight planks`. Each must still contain exactly one normal Wall plus exactly one BGE Corner, Quarter Column, and Layer; no role may duplicate or select a different canonical parent.
4. If log diagnostics are available, confirm each current pass describes that pass's resolved graph rather than reporting a stale role retained from an earlier mapping list. Record both trigger sequence and observed graph/role identities.

## First-load resources and Enderscape visuals

1. On the first client load, before manually reloading resources, open selector/inventory previews and place representative automatic CNM forms. There must be no missing model/texture warning or placeholder for a valid admitted BGE role. Repeat one ordinary client reload and confirm the same resources remain deterministic.
2. Verify all six new full families—`stripped veiled log`, `stripped veiled wood`, `stripped celestial stem`, `stripped celestial hyphae`, `stripped murublight stem`, and `stripped murublight hyphae`. Check every normal BGE role, axis-aware placement, side/end textures, and UV orientation. Do not infer or test an invented stripping transition from a derived geometry.
3. Compare `veiled end stone`, `celestial overgrowth`, and `corrupt overgrowth` against the Crimson Nylium BGE structural face/UV behavior while retaining their Enderscape material textures. Compare `celestial path` and `corrupt path` against Dirt Path/Grass Path BGE structural face/UV behavior. This does not imply source-only growth, path, survival, or flattening behavior on derived geometry.
4. Check Void Shale's BGE roles use the exact side appearance on horizontal faces and the exact end appearance on top/bottom faces, with no axis state and no missing `enderscape:block/void_shale` reference.
5. Exercise every exposed Blinklamp luminance state and selector/inventory preview. Every BGE geometry must use the provider's actual state-specific visual resources, retain C84 luminance behavior, and have no missing model/texture.
6. Place and render a Veiled Leaves slab in-world and inspect its inventory/menu preview. It must not crash chunk rendering and must retain the canonical provider appearance when the provider uses a model-owned tint route rather than a registered block tint source. Recheck a native foliage material to ensure its tint behavior is unchanged.

## Retained boundaries and evidence discipline

1. Recheck C86's completed Enderscape Layer facing behavior for the five DirectionalBlock-derived terrain/path families, and C84's Magnia/Blinklamp material-state behavior, only as regressions. Preserve the existing placement, economy, drop, and canonicalization behavior unless an exact C87 observation differs.
2. The retained C86/C85/C84 and C80 owner observations are predecessor-only evidence and do not establish a C87 result. C87 has no recorded gameplay-runtime observation or acceptance.
3. For every observation, record the exact candidate SHA-256, source/mod versions, datapack configuration, repeatable sequence, expected result, and observed result. Controlled builds, static checks, and GameTests do not establish gameplay acceptance.

## Retention and rollback

Retain `BGE C87.jar` by its exact SHA-256 before runtime work. Keep C86 as
immediate predecessor provenance, C72 as rollback provenance, and C70 as the
accepted release; do not overwrite a retained artifact.
