# BGE C62 runtime procedure

Current candidate: `cnm-nibaru-integration-4.2.6-bge.canary62.family-dedup+26.2.jar`, SHA-256 `c8d42032166ef2ba6796f3cd61e89698ccca1a4331d90ee1d1f1ba6720cc01ce`, embedded version `4.2.6-bge.canary62.family-dedup+26.2`, source checkpoint `cdc0acdcba4d7b8761b035b048578a3a3635079b`. Exact C58 remains accepted. Exact C61 is preserved as externally runtime-failed provenance. C62 is not deployed and has no Minecraft runtime result; controlled build, archive validation, and GameTests are not runtime evidence. Canonical Slot A still names older C60, but manager verification currently fails closed before physical inspection because the unrelated Building But Better status is revision 2 while its latest log entry is revision 1.

Use only the dedicated Matcha Flavoured 26.2 Workbench. Never use or modify the protected Matcha Flavoured 26.1.2 profile. Before launch, require the Test Instance Manager to report exact C62 as `READY_TO_TEST_VERIFIED` in the BGE slot while preserving the other slot byte-for-byte.

## Scope

- 64 canonical source variants and nine roles per variant: source Block, Slab, Stairs, Wall, Vertical Slab, Step, Corner, Quarter Column, and Layer.
- Mynx Trees: Wisteria Log/Wood/Leaves and Silver Birch Log/Wood/Leaves. Log and Wood intentionally share their species ShapeMap family but remain distinct canonical variants.
- Ribbits: Mossy Oak Planks, reusing the provider's exact Slab and Stairs and generating only the six missing roles.
- Macaw's Paths: 52 patterned full-block parents, each reusing its provider Slab and Stairs and generating only the six missing roles; plus the five independent provider Path sources `mcwpaths:podzol_path_block`, `dirt_path_block`, `gravel_path_block`, `sand_path_block`, and `red_sand_path_block`, each with all eight derived roles. The five vanilla soil families remain separate and unchanged.

## Manual checks

1. Wisteria Log Wall and Silver Birch Log Wall must be ordinary connected walls with no `AXIS` state or crossed geometry. Check post, low side, tall side, and inventory: vertical faces use bark and exposed horizontal faces use the correct Wisteria/Silver Birch end grain.
2. Wisteria Wood Wall and Silver Birch Wood Wall must use the same ordinary wall route, remain distinct from their Log counterparts inside the intentionally shared species family, and show bark on every face. Confirm the other Log/Wood roles retain their legitimate material-axis, stripping, fire/fuel, sound, tags, drops, and orientation behavior.
3. Silver Birch Leaves Wall must use normal post/low/tall/inventory leaf-wall geometry with the provider's live world and inventory tint across the complete model. Check multiple positions/biomes. Wisteria Leaves Wall must follow the same ordinary geometry and remain untinted.
4. Inspect Ribbits Mossy Oak Planks and representative entries from every Macaw material/pattern. Each exact canonical variant must show one source and exactly one of each role in the CNM selector; the provider Slab/Stairs must be the selected roles, with no equivalent BGE copies.
5. Inspect all five plain Macaw soil Path sources independently. Each must expose all eight derived roles in its own selector family, never alias to `minecraft:podzol`, `dirt`, `gravel`, `sand`, or `red_sand`; those vanilla families must remain unchanged.
6. Recheck representative accepted C58 native families, intentional multi-variant families, Glass Corner UV/geometry, and the one-source Layer/Corner/Quarter Column economy.

Record only directly observed runtime results for exact C62. Stop and record `FAIL` or `INCONCLUSIVE` if any canonical variant has a duplicate/missing role, any normal wall presents as crossed/multi-axis, any log/wood face or leaf tint differs, any plain Macaw soil Path aliases vanilla, or any accepted C58 behavior regresses. Do not infer `PASS` from readiness, build, archive inspection, static tests, or GameTests.
