# BGE C94 item-preview and Beam UV manual verification

Current candidate: `BGE C94.jar` — **PRIVATE_LOCAL_ONLY; no redistribution permission.**

- Embedded version: `4.2.38-bge.canary94.item-preview-beam-uv+26.2`
- Size: `6,418,548` bytes
- SHA-256: `38f6f50341cea0dd3a31da657616651626b89b8e73558794edf95e1d2229a967`
- Finalized: `2026-09-21T16:03:27.0181044Z`
- Source checkpoint: `2a2372f03e8b1c528ae8af3dab4f2de8606c1d5b`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Private provenance: this exact artifact contains six local-only palette derivatives generated from the locally supplied BBB dev.6 Beam artwork (SHA-256 `0d54034725c3e354515c78bcee32ab2cb5ce764a33e0602419e26c78aaef8c5a`) and Enderscape 3.0.2+mc26.2 palettes (SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`). Do not copy, attach, upload, publish, release, redistribute, or move this JAR or its Beam textures into a tracked/public path.
- Immediate predecessor evidence: exact C93 `BGE C93.jar`, SHA-256 `16087d67acb3554f8a6aeee3652f4d75ff00b8a042a395332c72c7fb9f7016e3`, was owner-tested and reported to have unwanted item/selector rotations introduced after C92 for Stairs, Vertical Slabs, Steps, and Walls while placed-world geometry/orientation remained correct. Beam Vertical Slab and Step item previews cropped or omitted the source Beam decorative center line/band on their vertical faces. Those observations bind only to exact C93; they do not establish any C94 result or acceptance.
- Retained predecessor contract: C94 preserves C93's provider-role completion, explicit-family architecture, Enderscape ownership, Wall topology, lowered-Path culling and Step UV, Mossy Stone, and Corrupt Overgrowth behavior. Unified verification found all 7,992 packaged placed blockstate/block-model resources byte-identical to C93 with no added or deleted placed resources.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for these exact C94 bytes. It does not authorize inspecting, creating, selecting, populating, or modifying any protected or retired Minecraft profile.

## Exact C92 item presentation, unchanged C93 placement

1. Inspect representative Stairs, Vertical Slabs, Steps, and Walls in both inventory and the CNM selector. Compare with exact C92: each category must have the same apparent rise/direction and handed presentation as C92, with no C93-only catalog rotation remaining.
2. Include at least one ordinary stone-like family, one axis-textured Log/Stem family, one state-bridged Enderscape family, one Path family, one Plank family, and one Beam family. Confirm each category uses its C92 presentation contract while retaining the material-specific texture and tint.
3. Place every relevant facing for the same representatives. Confirm blockstate properties, facing interpretation, selected placed models, world orientation, culling, and UVs remain C93-identical; item inspection must not change placed behavior.
4. Confirm Log/Stem Wall previews retain end grain/rings wherever their topology exposes an end; Wood/Hyphae remains all-side texture.

## Beam Vertical Slab and Step item UV frames

1. Inspect the Vertical Slab and Step item/selector preview for every BBB Beam: Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Crimson, Warped, Mangrove, Bamboo, Cherry, and Pale Oak.
2. Inspect the same two item roles for Veiled Beam, Celestial Beam, and Murublight Beam. Each must retain its own palette; no family may display Pale Oak or another Beam's texture.
3. On every relevant vertical side face, compare against that Beam source material's corresponding cut face. The texel density and complete side/end frame must match, and the decorative center line/band must remain clearly visible rather than stretched, shifted, or cropped away.
4. Rotate the item in GUI/hand contexts and place the corresponding Vertical Slab and Step in every orientation. The item-only Beam wrapper must not alter axis/facing state, placed model selection, placed UV orientation, geometry, or collision.

## Retained C93 regressions

1. Spot-check Mossy Stone and the exact Enderscape/vanilla provider-owned families: every selector still has one root plus eight canonical roles, with existing provider Slab/Stairs/Wall ownership retained and no duplicate, provisional, `*_slab_wall`, `*_wall_slab`, or arbitrary/deferred family.
2. Compare Veiled, Celestial, and Murublight Plank Walls with Pale Oak Planks Wall and their Beam Walls with Pale Oak Beam Wall. C93's distinct topology/connectivity fixes and each family's textures must remain intact.
3. Place a full block beside Celestial/Corrupt Path Stairs and Vertical Slabs, then inspect Celestial/Corrupt Path Steps in every form/facing. C93's exposed top 1/16 strip and exact lowered-Path Step topology/UVs must remain intact.
4. Confirm Corrupt Overgrowth roles still expose no invented material `AXIS` and preserve the source's real facing behavior.

## Evidence and local retention

1. For every observation, record this candidate SHA-256, all mod/provider versions, datapack configuration, repeatable sequence, expected result, and observed result. Controlled builds, static suites, GameTests, and archive verification do not establish gameplay acceptance.
2. Retain `BGE C94.jar` only in ignored local artifact storage by the exact SHA-256. Its six private Beam derivative textures must remain only in ignored build output and this retained private JAR. Preserve exact C93 and C92 evidence, C72 rollback provenance, and C70 accepted provenance without overwriting prior artifacts.
