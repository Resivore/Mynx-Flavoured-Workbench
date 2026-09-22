# BGE C95 spruce-leaf tint manual verification

Current candidate: `BGE C95.jar` — **PRIVATE_LOCAL_ONLY; no redistribution permission.**

- Embedded version: `4.2.39-bge.canary95.spruce-leaf-tint+26.2`
- Size: `6,420,964` bytes
- SHA-256: `cb4d1c04042588b848e8348e33f3688b28d14e8517c020162277cf596e7948b7`
- Finalized: `2026-09-22T01:37:18.3405374Z`
- Source checkpoint: `400a74c1f3ded757682dcd9bd5e0d8782607377b`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Private provenance: this exact artifact contains six local-only palette derivatives generated from locally supplied BBB dev.6 Beam artwork (SHA-256 `0d54034725c3e354515c78bcee32ab2cb5ce764a33e0602419e26c78aaef8c5a`) and Enderscape 3.0.2+mc26.2 palettes (SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`). Do not copy, attach, upload, publish, release, redistribute, or move this JAR or its Beam textures into a tracked/public path.
- Retained predecessor contract: C95 changes only the shared spruce profile-to-client tint seam. It preserves C94 item presentation; C93 provider completion, explicit-family ownership, Wall topology, lowered-Path culling, Step UVs, Mossy Stone, and Corrupt Overgrowth; all geometry, models, textures, UVs, lifecycle, placement, drops, family membership, selector order, and CTM behavior.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for these exact C95 bytes. It does not authorize inspecting, creating, selecting, populating, or modifying any protected or retired Minecraft profile.

## Spruce fixed-color contract

1. In two biomes with visibly different biome foliage colors, place a `minecraft:spruce_leaves` control and each available spruce family role together under the same lighting: standard/provider Slab, Stairs, and Wall where present; BGE Vertical Slab, Step, Corner, Quarter Column, and Layer.
2. In each biome, every spruce derived role must match the native spruce-leaf control's fixed color. Move between the two biomes and confirm none of those spruce roles changes color or falls back to generic biome foliage tint.
3. Check each relevant state/orientation of the asymmetric roles. Orientation, waterlogging, and placement must not alter the fixed spruce color or any pre-existing geometry behavior.

## Retained foliage behavior

1. Compare birch leaves and the available birch family roles in the same two biomes. Birch must retain its existing fixed-color appearance; C95 must not turn it into biome foliage.
2. Compare an ordinary biome-tinted leaf family such as oak in the same two biomes. Its foliage tint must remain biome-dependent.
3. Smoke-test cherry, pale oak, azalea, and flowering azalea where available. Their existing special/source-defined color behavior must remain unchanged.

## Evidence and local retention

1. For every observation, record this candidate SHA-256, all mod/provider versions, datapack configuration, repeatable sequence, expected result, and observed result. Controlled builds, static suites, GameTests, and archive verification do not establish gameplay acceptance.
2. Retain `BGE C95.jar` only in ignored local artifact storage by the exact SHA-256. Its six private Beam derivative textures must remain only in ignored build output and this retained private JAR. Preserve historical evidence and provenance without overwriting prior artifacts.
