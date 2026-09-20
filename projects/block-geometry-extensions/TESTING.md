# BGE C89 CNM family-root classification manual verification

Current candidate: `BGE C89.jar`

- Embedded version: `4.2.33-bge.canary89.cnm-family-root-classification+26.2`
- SHA-256: `cc1368f48fffcde03997c4d3f1d0e8907236c584d7ae107f35eb4e06e98667f2`
- Source checkpoint: `f529b96f708520d5d01f1af21313f409d9ae2e32`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Immediate predecessor: exact C88 `BGE C88.jar`, SHA-256 `fb007b8e5ebd3062e8f9af04ad893be77c87640d49d09cc23482f82883327bf3`.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for the exact
C89 bytes above. It does not authorize inspecting, creating, selecting, or modifying any
protected or retired Minecraft profile.

## C89 recursive-family regression

1. In an ordinary permitted Minecraft environment with stock Clutter No More 2.0.7+26.2 and Enderscape 3.0.2+mc26.2, complete one normal initial load and then one ordinary resource reload or other legitimate CNM ShapeMap reconstruction. Do not add or remove BGE/CNM blocks between passes.
2. For every available canonical material family, verify the intended direct geometry set can include exactly one `material_slab`, `material_stairs`, `material_wall`, BGE Corner, Quarter Column, and Layer as applicable. The direct slab/stair input remains eligible only for the initial family-completion pass; this is not a request to remove ordinary CNM autopopulation.
3. In particular, inspect available Enderscape representatives `veradite`, `mirestone`, `mossy stone`, `celestial bricks`, `dusk purpur`, and `etched alluring magnia`. After each pass, there must be no `material_slab_wall`, `material_wall_slab`, or equivalent geometry-from-geometry combination in the selector, registry diagnostics, or generated-resource output.
4. Confirm world loading completes without a canonical-binding validation failure. If diagnostics are available, record the exact failing/absent IDs and the two-pass sequence; never mask a failure by adding a synthetic binding.
5. Repeat step 2 with at least one non-Enderscape ordinary CNM family. The expected absence of recursive combinations is role-based, not namespace- or material-name-specific.

## Retained C88 BBB first-bake regression

1. With Building But Better standard beam forms, before manually reloading resources, open or place `acacia beam stairs` and BGE-generated forms. There must be no `Missing canonical model for resolved CNM parent: bbb:acacia_beam_stairs` exception, missing model, or missing texture.
2. Repeat representative first-load and one-reload checks across BBB beam materials. Valid admitted beam families retain their Layer, Corner, and Quarter Column resources with the material's beam side and top/end appearance, without changing CNM/ShapeMap canonical parent selection.

## Retained C87/C86/C84 regressions

1. Across the initial and rebuilt ShapeMap passes, inspect representative automatic CNM families including `mossy stone`, `polished end stone`, `shadoline`, `mirestone`, `kurodite`, `etched alluring magnia`, `dusk purpur block`, `celestial bricks`, and `murublight bricks`. Each eligible family retains exactly one BGE Corner, Quarter Column, and Layer; wall-bearing plank families retain their one normal Wall without duplicate roles or a changed canonical parent.
2. Before and after an ordinary resource reload, check valid generated models for the six stripped Enderscape axis families, Void Shale, terrain/path materials, Blinklamp state-specific resources, and Veiled Leaves model-owned tint fallback. Preserve the existing C86 Layer-facing and C84 Magnia/Blinklamp material-state behavior.

## Evidence and retention

1. For every observation, record exact candidate SHA-256, source/mod versions, datapack configuration, repeatable sequence, expected result, and observed result. Controlled builds, static checks, and GameTests do not establish gameplay acceptance.
2. Retain `BGE C89.jar` by its exact SHA-256 before runtime work. Keep C88 as immediate predecessor provenance, C72 as rollback provenance, and C70 as the accepted release; do not overwrite retained artifacts.
