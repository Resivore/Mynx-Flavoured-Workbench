# BGE C93 provider-role completion manual verification

Current candidate: `BGE C93.jar` — **PRIVATE_LOCAL_ONLY; no redistribution permission.**

- Embedded version: `4.2.37-bge.canary93.provider-role-completion+26.2`
- Size: `6,416,477` bytes
- SHA-256: `16087d67acb3554f8a6aeee3652f4d75ff00b8a042a395332c72c7fb9f7016e3`
- Finalized: `2026-09-21T06:08:09.831Z`
- Source checkpoint: `374fe75c6ce3f428611b6d84f340101d197db099`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Private provenance: this exact artifact contains six local-only palette derivatives generated from the locally supplied BBB dev.6 Beam artwork (SHA-256 `0d54034725c3e354515c78bcee32ab2cb5ce764a33e0602419e26c78aaef8c5a`) and Enderscape 3.0.2+mc26.2 palettes (SHA-256 `9fcc4f59ca88e91f90e7c7d18289f2f859f20c810eebcca924764aa15236c40b`). Do not copy, attach, upload, publish, release, redistribute, or move this JAR or its Beam textures into a tracked/public path.
- Immediate predecessor evidence: exact C92 `BGE C92.jar`, SHA-256 `81c5327b0f87ce156985ff5b5a0505a7c213bbcba24abd6418e24c69aace060e`, was reported to have incomplete Mossy Stone and the 17 named Enderscape families; provider-role ownership requiring the exact End Stone, Plank, Purpur, and Purpur-Wall corrections; incorrect geometry on the six named Veiled/Celestial/Murublight Plank and Beam Walls while their textures were correct; reversed menu previews for apparently all Stairs, Vertical Slabs, Steps, and Walls while placed blocks oriented correctly; Celestial/Corrupt Path Stairs and Vertical Slabs culling the neighboring top 1/16; and incorrect UVs on Celestial/Corrupt Path Steps, not Stairs. Those observations bind only to those exact C92 bytes. Corrupt Overgrowth's no-AXIS contract is a source-backed invariant, not a claimed C92 runtime failure. No aggregate or unreported row result, or acceptance, is inferred.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for these exact C93 bytes. It does not authorize inspecting, creating, selecting, populating, or modifying any protected or retired Minecraft profile.

## Catalog and provider ownership

1. Open Mossy Stone. Its one visible variation must be exactly `Root, Slab, Stairs, Wall, Vertical Slab, Step, Corner, Quarter Column, Layer`, with every role present once and no similarly named Macaw material substituted.
2. Open End Stone. Confirm vanilla End Stone is the root, Enderscape owns the exact Slab, Stairs, and Wall, BGE owns only the five BGE-specific tail roles, and the visible list has exactly nine entries once each.
3. Open Veiled Planks, Celestial Planks, and Murublight Planks. Confirm the provider's actual `veiled_slab`/`veiled_stairs`, `celestial_slab`/`celestial_stairs`, and `murublight_slab`/`murublight_stairs` are canonical; each BGE Plank Wall and five-role tail occurs once; no guessed `*_planks_slab` or `*_planks_stairs` duplicate is visible.
4. Open Purpur Block. Confirm vanilla owns Root, Slab, and Stairs; Enderscape Purpur Wall is the one canonical Wall; and BGE supplies only Vertical Slab, Step, Corner, Quarter Column, and Layer.
5. Open Purpur Tiles. Confirm Enderscape owns Root, Slab, and Stairs; BGE supplies its genuinely missing Wall and five BGE-specific roles; all nine occur once.
6. Check each listed C92-incomplete family independently: Block of Shadoline; Cut Shadoline; Overgrown End Stone Bricks; Veradite; Veradite Bricks; Mirestone; Polished Mirestone; Overgrown Mirestone Bricks; Kurodite; Polished Kurodite; Kurodite Bricks; Etched Alluring Magnia; Etched Repulsive Magnia; Dusk Purpur Block; Purpur Tiles; Celestial Bricks; Murublight Bricks. For each, check only that one root plus the eight canonical roles is visible exactly once and that an existing provider Slab, Stairs, or Wall is the selected standard role.
7. Across those selectors, confirm there is no duplicate BGE/provider standard role, provisional identity, `*_slab_wall`, `*_wall_slab`, or unrelated family inferred from CNM.

## Plank and Beam Walls

1. For Veiled, Celestial, and Murublight Planks Walls, compare item, isolated, straight, corner, T, and end states with Pale Oak Planks Wall. Post/arm proportions, state topology, face layout, and connectivity must match the ordinary Plank reference while each Enderscape Wall retains its own material textures.
2. For Veiled, Celestial, and Murublight Beam Walls, compare the same states with Pale Oak Beam Wall. Item topology and thin Beam post/arm geometry must match the Beam reference while retaining each Enderscape palette; do not compare them against the ordinary Plank Wall shape.
3. Confirm straight and cross Beam runs omit the center post where the Pale Oak Beam reference does, while T, corner, isolated, and end states follow that reference exactly.

## Item and selector previews versus placed blocks

1. Inspect representative ordinary, axis-textured, state-bridged, terrain, Path, Plank, and Beam families for each category: Stairs, Vertical Slab, Step, and Wall. Every item/CNM-selector preview must use the same forward-facing convention category-wide; a Stair must rise away from the viewer like a vanilla Minecraft stair item.
2. For the same representatives, place all relevant facings before and after inspecting the item preview. Compare their blockstate properties and world model orientation: the preview correction must cause zero placed-world orientation change.
3. Include at least one Log or Stem Wall preview and confirm exposed faces still show the correct end-grain/rings wherever its topology exposes them; Wood/Hyphae remains all-side texture.

## Lowered Paths and Corrupt Overgrowth

1. Place a full block beside each Celestial Path Stair, Corrupt Path Stair, Celestial Path Vertical Slab, and Corrupt Path Vertical Slab. The neighbor's exposed top 1/16 side strip must remain visible with no gap, while unrelated full-height geometries retain normal complete-face culling.
2. Inspect Celestial Path Step and Corrupt Path Step in every form/facing. Each must use Dirt Path Step's lowered topology and vertically cropped/offset side-UV layout, but Celestial Path must keep Celestial textures and Corrupt Path must keep Corrupt textures.
3. Inspect Corrupt Overgrowth Root, Slab, Stairs, Wall, Vertical Slab, Step, Corner, Quarter Column, and Layer. No generated role may expose an invented material `AXIS`; the source's real facing/directional state and existing Layer-facing compatibility must remain functional independently of geometry state.

## Evidence and local retention

1. For every observation, record this candidate SHA-256, all mod/provider versions, datapack configuration, repeatable sequence, expected result, and observed result. Controlled builds, static suites, GameTests, and archive verification do not establish gameplay acceptance.
2. Retain `BGE C93.jar` only in ignored local artifact storage by the exact SHA-256. Its six private Beam derivative textures must remain only in ignored build output and this retained private JAR. Preserve exact C92 failure evidence, C72 rollback provenance, and C70 accepted provenance without overwriting prior artifacts.
