# BGE C98 Sodium/Iris spruce terrain diagnostic manual verification

Current candidate: `BGE C98.jar` — **PRIVATE_LOCAL_ONLY; no redistribution permission.**

- Embedded version: `4.2.42-bge.canary98.sodium-iris-spruce-trace+26.2`
- Size: `6,433,502` bytes
- SHA-256: `19654c57715fcf4a96311252813059ae001d1eb2677a75792103ba0312ce885c`
- Finalized: `2026-09-23T02:04:20.5961959Z`
- Source checkpoint: `f247dda15222e83afe57354ac1b5b1bb4fba4646`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Private provenance: this exact artifact contains six local-only palette derivatives generated from locally supplied BBB dev.6 Beam artwork (SHA-256 `0d54034725c3e354515c78bcee32ab2cb5ce764a33e0602419e26c78aaef8c5a`) and Enderscape 3.0.2+mc26.2 palettes (SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`). Do not copy, attach, upload, publish, release, redistribute, or move this JAR or its Beam textures into a tracked/public path.
- Retained contract: C98 observes only. It preserves C96's fixed-tint interception, tinted-Stair frames, and every geometry, UV, placement, family, CTM, and foliage contract.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. It does not authorize inspecting, creating, selecting, populating, or modifying any protected or retired Minecraft profile. Record only behavior observed for these exact C98 bytes; C97's zero vanilla rows are C97 diagnostic evidence only.

## Capture the production terrain seam under the actual stack

1. Record the exact C98 SHA-256, Minecraft/mod/provider versions, datapacks, resource-pack order, shader pack/settings, and active Sodium/Iris versions.
2. In two biomes with visibly different foliage color, render native `minecraft:spruce_leaves` adjacent to the derived spruce family. Include at least one standard/provider role if present and BGE Vertical Slab, Step, Corner, Quarter Column, and Layer; include a second role with a different geometry/orientation when practical.
3. Collect the `BGE_SODIUM_SPRUCE_TINT_TRACE` pair for each target. The provider row records canonical binding/state, provider identity, and resolved ARGB; the vertex row records written ABGR after Sodium's `bufferQuad`.
4. Collect BGE × Complementary Canary 5's `BGE_SPRUCE_IRIS_MATERIAL_TRACE` and `BGE_SPRUCE_IRIS_LAYER_TRACE` rows. They record effective values and explicit-versus-inherited origin for native plus each derived binding.
5. Interpret only observed rows: ARGB divergence is the first Java difference; equal ARGB but distinct ABGR is vertex emission; equal colors with a map difference is material/layer classification. If all are equal while pixels differ, Iris's per-vertex block ID/mid-block data and Complementary's native-leaves shader branch are the first remaining inputs.

## Retained behavior

1. Confirm native spruce leaves remain unchanged. Confirm birch stays fixed-color, an ordinary oak family stays biome-tinted, and special/source-defined foliage retains its prior behavior.
2. Check C96's Veiled Leaves and Silver Birch Leaves Stair frames through straight, inner, and outer shapes in both halves/facings. No C98 change is expected there.
3. Retain `BGE C98.jar` only in ignored local artifact storage by this exact SHA-256. Do not copy, attach, upload, publish, release, or redistribute it or its private Beam textures.
