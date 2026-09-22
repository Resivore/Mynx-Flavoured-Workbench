# BGE C96 effective spruce-tint and Stair-frame manual verification

Current candidate: `BGE C96.jar` — **PRIVATE_LOCAL_ONLY; no redistribution permission.**

- Embedded version: `4.2.40-bge.canary96.spruce-tint-stair-frame+26.2`
- Size: `6,423,305` bytes
- SHA-256: `4f8ea46d2766bab183a958790ea6c696621cb9dbdec4b11bdf9ae878cfecedf7`
- Finalized: `2026-09-22T05:50:51.8360976Z`
- Source checkpoint: `4dbb425d30170faf68f45b51b017a27a43d86a59`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Private provenance: this exact artifact contains six local-only palette derivatives generated from locally supplied BBB dev.6 Beam artwork (SHA-256 `0d54034725c3e354515c78bcee32ab2cb5ce764a33e0602419e26c78aaef8c5a`) and Enderscape 3.0.2+mc26.2 palettes (SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`). Do not copy, attach, upload, publish, release, redistribute, or move this JAR or its Beam textures into a tracked/public path.
- Retained predecessor contract: C96 preserves C94 item presentation; C93 provider completion, explicit-family ownership, Wall topology, lowered-Path culling, Step UVs, Mossy Stone, and Corrupt Overgrowth; and all unrelated registration, lifecycle, placement, drops, family, selector, and CTM behavior.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior observed for these exact C96 bytes. The owner's C95 observation is predecessor failure evidence, not a C96 result. This checklist does not authorize inspecting, creating, selecting, populating, or modifying any protected or retired Minecraft profile.

## Effective spruce tint under the actual pack stack

1. With the owner's actual resource-pack order active, place native spruce leaves and all eight derived spruce roles together in two biomes with visibly different foliage color: standard/provider Slab, Stair, and Wall where present; BGE Vertical Slab, Step, Corner, Quarter Column, and Layer.
2. Confirm every derived role matches native spruce leaves in both biomes and none changes with biome foliage.
3. Check asymmetric orientations and waterlogged states; tint must remain fixed without changing placement, geometry, textures, lifecycle, or CTM behavior.

## Generated tinted Stair frames

1. With the relevant providers present, inspect `enderscape:veiled_leaves` and `mynx_trees:silver_birch_leaves` Stairs. These are the only current users of the corrected generated tinted-stair branch.
2. Exercise straight, inner-left, inner-right, outer-left, and outer-right shapes in top and bottom halves across all four horizontal facings.
3. Confirm each rendered step matches its collision/selection orientation with no rotated, missing, or extra quadrant; textures, established tint behavior, placement, and inventory presentation must remain intact.

## Retained foliage and evidence

1. Confirm birch remains fixed-color, an ordinary oak family remains biome-tinted, and cherry, pale oak, azalea, flowering azalea, and other special/source-defined leaves retain their prior behavior.
2. Record exact artifact SHA-256, mod/provider versions, datapacks, exact resource-pack order, repeatable sequence, expected result, and observed result. Controlled builds, static suites, GameTests, and archive verification do not establish gameplay acceptance.
3. Retain `BGE C96.jar` only in ignored local artifact storage by the exact SHA-256. Do not copy, attach, upload, publish, release, or redistribute it or its private Beam textures.
