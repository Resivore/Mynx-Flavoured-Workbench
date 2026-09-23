# BGE C99 spruce rollback and Bookshelf manual verification

Current candidate: `BGE C99.jar` — **PRIVATE_LOCAL_ONLY; no redistribution permission.**

- Embedded version: `4.2.43-bge.canary99.spruce-rollback-bookshelf+26.2`
- Size: `6,420,402` bytes
- SHA-256: `b1fa590b2924450d71d258a5e7c7b7bc81be202db73adb32d01688eb66fc152a`
- Finalized: `2026-09-23T15:08:31.396566Z`
- Source checkpoint: `53a55bd3619d7e25f20f93ce7fcca020a559c97d`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Private provenance: this exact artifact includes six local-only BBB/Enderscape Beam palette derivatives. Do not copy, attach, upload, release, or redistribute the JAR or those texture bytes.

The owner's `originals/assets/bookshelves.png` (SHA-256 `9e9e5f7f66a9c68301baec69471a9bcf754c5cc6a678242fb5b360f8eb53f208`) and report are **C98 predecessor evidence only**. They describe Corner, Quarter Column, and Layer Bookshelf wooden ends falling back to ordinary Oak Planks, plus Bookshelf Stair wooden ends rotating with facing. They do not show C99 behavior.

Controlled C99 evidence: Java 25 clean build; 163/163 Minecraft 26.2 server GameTests; isolated client lookup `SPRUCE_C94_LOOKUP|derived=8|contexts=2|nativeShadows=0|result=PASS`; exact 40-state Bookshelf Stair UV/model audit; read-only Matcha reference audit; architecture checks; and archive verification. The retained C96 Veiled Leaves and Silver Birch Leaves Stair-frame GameTest covers all 80 physical states. None of these is a gameplay-profile result.

## Owner-directed runtime checks

1. Record the exact `BGE C99.jar` SHA-256, Minecraft/mod versions, resource-pack order, shader/renderer settings, and world context. Use the normal Matcha Flavoured and Matcha Overlays packs; note whether external Foundation v3 is active. Do not copy Foundation or Matcha assets into BGE.
2. Check native `minecraft:spruce_leaves` under the selected pack stack. Its blockstate, model route, artwork, and visible tint must remain resource-pack controlled. Compare the derived spruce roles across two foliage-color biomes and confirm the C94 client behavior is restored. Birch's fixed profile, ordinary biome foliage, and provider-defined leaves should retain their established behavior. No C95-C98 trace output should appear.
3. With Matcha active, compare the top and bottom wood on canonical Bookshelf, Slab, Stair, Wall, Vertical Slab, Step, Corner, Quarter Column, and Layer. Exercise every Layer thickness and orientation, all Corner directions, and Quarter Column occupancies. The wood must match the established Matcha treatment; visible side faces must retain Bookshelf artwork. Then remove the Matcha packs in an owner-controlled test and confirm valid oak-planks fallback without a missing texture.
4. Rotate Bookshelf Stair through all four facings, top and bottom halves, and straight, inner-left, inner-right, outer-left, and outer-right shapes. Its top/bottom wood frame must stay non-directional while physical orientation, side art, collision, selection, placement, and item presentation remain correct.
5. Check Veiled Leaves and Silver Birch Leaves generated Stairs through their straight, inner, and outer forms in both halves and four facings. Their physical model frames must retain the C96 correction.
6. Smoke-check retained C93/C94 behavior: provider-role ownership, Mossy Stone, Enderscape families, Plank/Beam Wall topology, lowered Path culling/UVs, Beam item UVs, placement, economy, selector order, and CTM.

Record only observations made against this exact C99 SHA-256. This checklist is lifecycle-neutral and does not authorize Codex to inspect or modify any protected Minecraft testing profile. Accepted C70 and rollback C72 remain unchanged.
