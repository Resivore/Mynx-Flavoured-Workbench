# BGE C97 spruce renderer-seam diagnostic manual verification

Current candidate: `BGE C97.jar` — **PRIVATE_LOCAL_ONLY; no redistribution permission.**

- Embedded version: `4.2.41-bge.canary97.spruce-render-trace+26.2`
- Size: `6,426,611` bytes
- SHA-256: `3e13d6f4dfe46edea753926da4e1168515e8432dce85d63a29b360a6aefc9408`
- Finalized: `2026-09-22T15:09:08.6623340Z`
- Source checkpoint: `0b047026a4e3e7539cbf2ef69a176d9292147aee`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Private provenance: this exact artifact contains six local-only palette derivatives generated from locally supplied BBB dev.6 Beam artwork (SHA-256 `0d54034725c3e354515c78bcee32ab2cb5ce764a33e0602419e26c78aaef8c5a`) and Enderscape 3.0.2+mc26.2 palettes (SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`). Do not copy, attach, upload, publish, release, redistribute, or move this JAR or its Beam textures into a tracked/public path.
- Retained contract: C97 does not change tint behavior. It preserves C96's fixed-tint interception and tinted-Stair frames, as well as C94 item presentation, C93 provider completion, explicit-family ownership, geometry, UVs, placement, drops, economy, ShapeMap order, CTM, and all other behavior.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. It does not authorize inspecting, creating, selecting, populating, or modifying any protected or retired Minecraft profile. Record only behavior observed for these exact C97 bytes.

## Capture the Java renderer seam under the actual stack

1. Record the exact C97 SHA-256, Minecraft/mod/provider versions, datapacks, resource-pack order, shader pack and settings, and the renderer/shader loader in use. Pack order was unavailable to the C97 audit and must be observed here rather than inferred.
2. In two biomes with visibly different foliage color, render native `minecraft:spruce_leaves` adjacent to the derived spruce family. Include at least one standard/provider role if present and BGE Vertical Slab, Step, Corner, Quarter Column, and Layer; include a second role with a different geometry/orientation when practical.
3. Collect client-log lines beginning `BGE_SPRUCE_TINT_TRACE`. Each first-rendered target reports the block/state, position, quad direction/sprite, tint index, selected `BlockTintSource`, and ARGB immediately before Minecraft 26.2 `ModelBlockRenderer.putQuadWithTint` multiplies the quad color. Keep the raw lines with the visual observation.
4. Interpret only the observed rows:
   - If native and derived rows both show `0xFF619961`, the last Java renderer seam is equivalent and any visible divergence is after it (renderer backend or shader classification). Do not alter BGE tint from that result.
   - If native is fixed but a derived row is biome-dependent at this seam, the first observed divergence is at or before the Java `BlockColors`/tint-source result. Keep the source-class and state evidence before considering a correction.
   - If a target renders without a corresponding row, document the actual renderer path/component. It did not reach this vanilla seam, so no conclusion about the tint source is justified.

## Retained behavior

1. Confirm native spruce leaves remain unchanged. Confirm birch stays fixed-color, an ordinary oak family stays biome-tinted, and special/source-defined foliage retains its prior behavior.
2. Check C96's Veiled Leaves and Silver Birch Leaves Stair frames through straight, inner, and outer shapes in both halves/facings. No C97 change is expected there.
3. Retain `BGE C97.jar` only in ignored local artifact storage by this exact SHA-256. Do not copy, attach, upload, publish, release, or redistribute it or its private Beam textures.
