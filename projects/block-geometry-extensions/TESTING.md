# BGE C92 explicit-family reconstruction manual verification

Current candidate: `BGE C92.jar` — **PRIVATE_LOCAL_ONLY; no redistribution permission.**

- Embedded version: `4.2.36-bge.canary92.explicit-family-reconstruction+26.2`
- SHA-256: `81c5327b0f87ce156985ff5b5a0505a7c213bbcba24abd6418e24c69aace060e`
- Finalized: `2026-09-21T04:18:35.7661287Z`
- Source checkpoint: `4d23509c7165951c71d236310a80ca92d877afbe`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Private provenance: this exact artifact contains six local-only palette derivatives generated from the locally supplied BBB dev.6 Beam artwork (SHA-256 `0d54034725c3e354515c78bcee32ab2cb5ce764a33e0602419e26c78aaef8c5a`) and Enderscape 3.0.2+mc26.2 palettes (SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`). Do not copy, attach, upload, publish, release, redistribute, or move this JAR or its Beam textures into a tracked/public path.
- Immediate predecessor evidence: exact C91 `BGE C91.jar`, SHA-256 `c59294526858627cc20a496fd14842b4920b12c05ff66d520a024168153d2478`, was reported to show Hyphae before Stem; duplicate or misordered Plank/Beam entries; missing end-grain rings in Log/Stem Wall item previews despite correct placed Walls; visible culling gaps; rotated/misaligned new Corners; and visible deferred Wall entries. Those observations bind only to those exact C91 bytes. No other row result or aggregate acceptance is inferred.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for these exact C92 bytes. It does not authorize inspecting, creating, selecting, populating, or modifying any protected or retired Minecraft profile.

## Catalog and exact selectors

1. Open one ordinary single-variation family such as Block of Raw Shadoline. Its complete selector must be exactly `Root, Slab, Stairs, Wall, Vertical Slab, Step, Corner, Quarter Column, Layer`, with no extra, duplicate, provisional, `*_slab_wall`, or `*_wall_slab` entry.
2. Open Polished End Stone and confirm the same exact nine-role order and normal human-facing names.
3. Open Veiled Planks, Celestial Planks, and Murublight Planks. Each selector must be exactly `[complete Plank variation][complete matching Beam variation]`; every variation uses the nine-role order above, and no standard or BGE role is duplicated.
4. Open Celestial Stem/Hyphae and Murublight Stem/Hyphae, including stripped pairs. The complete Stem variation must precede the complete Hyphae variation.
5. Open one representative Log/Wood pair, such as Oak Log/Wood or Veiled Log/Wood. The complete Log variation must precede the complete Wood variation.

## Walls, Corners, and geometry

1. Place representative Log and Stem Walls in isolated, straight, corner, T, and end states. Placed Walls remain ordinary non-axis Wall geometry; exposed horizontal/end-grain surfaces use the source ring texture. Their inventory/selector previews must show the same side/end role contract. Wood/Hyphae Walls remain all-side texture.
2. Inspect Polished Veradite and Mirestone Bricks Corners in all four facing states. Each must use canonical C78 Corner topology and orientation without rotation or material-frame drift.
3. For representative Enderscape terrain families, confirm each Step is the genuine half-depth BGE Step topology rather than a renamed Stair, and every generated face uses the actual source texture with no Crimson Nylium or reference-family leakage.
4. Inspect representative Slab, Vertical Slab, Step, Corner, Quarter Column, Layer, Wall, terrain, and path boundaries. Partial visible faces must remain visible without black/empty gaps, while genuinely complete 16×16 boundary planes retain culling.
5. Inspect both Enderscape path families across Layer depths and facings. Side UVs must be vertically cropped/offset so the intended texture top meets the lowered Layer top, without stretching or mirrored seams.
6. Confirm Block of Raw Shadoline remains a complete explicit family with correct names, textures, topology, and selector membership.

## Evidence and local retention

1. For every observation, record this candidate SHA-256, all mod/provider versions, datapack configuration, repeatable sequence, expected result, and observed result. Controlled builds, static suites, GameTests, and archive verification do not establish gameplay acceptance.
2. Retain `BGE C92.jar` only in ignored local artifact storage by the exact SHA-256. Its six private Beam derivative textures must remain only in ignored build output and this retained private JAR. Preserve exact C91 failure evidence, C72 rollback provenance, and C70 accepted provenance without overwriting prior artifacts.
