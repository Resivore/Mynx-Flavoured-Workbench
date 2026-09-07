# BGE C60 runtime procedure

Current Slot A candidate: `cnm-nibaru-integration-4.2.4-bge.canary60.external-families+26.2.jar`, 6,089,707 bytes, SHA-256 `163e9c094bf598d8e3d591917d244f5870adfb5d4007774e0cb8bab02d8dbd9f`, embedded version `4.2.4-bge.canary60.external-families+26.2`, source checkpoint `91fd9f753f0b33f42f1ead6a65f57219822f2fb1`, deployment `9d34963c-2156-4f19-9f99-98041a0aebe8`, artifact `408f68a4-7596-44db-ad02-c90920b003dc`. Manager revision 117 reports `READY_TO_TEST_VERIFIED / UNTESTED`; exact C58 remains the accepted disabled rollback.

Use only the dedicated Matcha Flavoured 26.2 Workbench. Do not use or modify the protected Matcha Flavoured 26.1.2 profile.

## Exact source inventory

- Ribbits (1): `ribbits:mossy_oak_planks`.
- Mynx Trees (6): `mynx_trees:wisteria_log`, `mynx_trees:wisteria_wood`, `mynx_trees:wisteria_leaves`, `mynx_trees:silver_birch_log`, `mynx_trees:silver_birch_wood`, and `mynx_trees:silver_birch_leaves`.
- Macaw's Paths patterned paths (52): every Cartesian pair of materials `andesite`, `diorite`, `granite`, `sandstone`, `red_sandstone`, `brick`, `stone`, `mossy_stone`, `cobbled_deepslate`, `deepslate`, `mud_brick`, `blackstone`, and `dark_prismarine` with patterns `running_bond`, `windmill_weave`, `flagstone`, and `crystal_floor`, using exact ID `mcwpaths:<material>_<pattern>_path`.
- Macaw's Paths soil paths (5): `mcwpaths:podzol_path_block`, `mcwpaths:dirt_path_block`, `mcwpaths:gravel_path_block`, `mcwpaths:sand_path_block`, and `mcwpaths:red_sand_path_block`.

## Manual checks

1. Confirm each of the 64 sources has one contiguous nine-role family: source Block, Slab, Stairs, Wall, Vertical Slab, Step, Corner, Quarter Column, and Layer. Confirm no other Macaw's Paths source receives BGE geometry.
2. Place representative members of every role, switch through the family with Clutter No More, break them with the expected tool, and verify collision, waterlogging, drops, orientation, and one-source material economy remain correct.
3. For all four Macaw's patterns across representative stone, sandstone, deepslate, mud-brick, blackstone, and prismarine materials, verify the live source texture is retained without rotation, substitution, or missing-model texture. Verify the five soil paths retain their distinct source textures and shovel-mining semantics.
4. Verify Wisteria Log and Silver Birch Log retain side/end distinction on axis-bearing Slab, Stairs, Vertical Slab, Step, Corner, Quarter Column, and Layer forms; rotate each applicable form across X/Y/Z axes and strip it to the correct accepted native stripped family.
5. Verify Wisteria Wood and Silver Birch Wood use bark on every face under the same orientation and stripping checks.
6. Verify Wisteria Leaves are untinted and Silver Birch Leaves use birch foliage tint. For both leaf families, check cutout rendering, distance/persistent state, player placement persistence, natural decay/random ticks, water behavior, fire behavior, tags, and drops.
7. Verify Ribbits Mossy Oak Planks retains its source appearance, axe-mining, fire/fuel behavior, sound, and physical properties through all eight generated forms.
8. Recheck accepted C58 registry/resource identities and representative native families, including the authored Glass Corner UV/geometry route, for regression.

Do not mark any row passed without direct observation. GameTests, static validation, archive inspection, reproducible builds, or deployment/readiness verification do not constitute Minecraft runtime PASS. Stop and record `FAIL` or `INCONCLUSIVE` against the exact deployment above if any requested form is missing, any provider adds extra forms, resource/model loading fails, or a material behavior differs from its live source.
