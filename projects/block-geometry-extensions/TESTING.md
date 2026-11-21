# BGE C106 patterned geometry verification

Use the private local `BGE C106.jar` and the SHA-256 and source checkpoint in `WORKBENCH_STATUS.json`. The controlled reference provider is `AMC C23.jar`, SHA-256 `8e850efa8d6e8f17e8bfa0b2da7058a7b1b0cd91e4c42e133fabedf3062589f5`. Keep both JARs local.

## AMC patterns

1. In the owner-selected environment, record the release hashes, mod versions, active resource packs and renderer. Check each combination of `prismarine_bricks`, `quartz`, `nether_bricks`, `end_stone_bricks`, `tuff`, `cinnabar`, `sulfur`, and `smooth_basalt` with `running_bond`, `flagstone`, `windmill_weave`, and `crystal_floor`. Each parent is `architectural_material_closure:<material>_<pattern>`.
2. Stonecut one parent, then use the selector to cycle through Full Block, Horizontal Slab, Stairs, Wall, Vertical Slab, Step, Corner, Quarter Column and Layer. Repeat with a named stack. Check the name, count, order, item previews, and that switching stays within the selected pattern.
3. Place the shapes against their unengraved parent. Follow the same grout line across top, bottom and side faces. Inspect both Slab halves, straight/inner/outer Stairs in both halves, Wall posts and low/tall arms, both sides of the Vertical Slab, the inner Step riser, Corner recesses, diagonal Quarter Columns and Layers on all six faces. Check scale and alignment from multiple directions.
4. Toggle the full block with a pickaxe, then toggle it back. In survival, check collisions and outlines, waterlogging, tool drops, compatible Slab and fourth-Layer completion, and stack counts before and after obstructed or incompatible placement.
5. With AMC omitted, verify normal startup and check that the patterned parents and all eight derived roles disappear. With Macaw's Paths installed, verify its patterned families still use the original Macaw Slabs and Stairs.

## Controlled reproduction

Run `gradlew build` for the AMC-absent regression suite, then `gradlew -PamcGameTest verifyAmcModels` for the isolated server with the real C23 artifact and the model audit. The latter reads test-generated JSON alongside the local Minecraft, BGE, CNM, AMC, BBB and Macaw resources. Run `tools/Test-MaterialProfileArchitecture.ps1` and `tools/Verify-UnifiedArtifact.ps1` for architecture and private archive checks.

## Existing family regressions

1. Record the exact current artifact SHA-256, Minecraft/mod versions, resource-pack order, shader/renderer settings, and world context in an owner-approved isolated Minecraft 26.2 environment.
2. Check the native and optional-provider family selectors. Each explicit family must retain its canonical nine-role order and reuse its declared provider Slab/Stairs/Wall. Sample Macaw patterns and plain Paths, Mynx logs/leaves, all Ribbits toadstools, Mossy Stone, and Enderscape stateful materials. Unknown CNM materials remain CNM-owned and receive no inferred BGE family.
3. Search Creative inventory and JEI for `veiled`, `celestial`, and `murublight`. The six BBB Beam roots/Walls for those materials must not appear as independent entries. Each matching Enderscape `[Planks][Beam]` selector must still expose its complete existing Planks and Beam geometries in the same order. Check existing placed/saved references; do not infer a registration or persistence change from presentation.
4. Place and switch representative native/provider Slabs, Stairs, Walls, Vertical Slabs, Steps, Corners, Quarter Columns, and Layers. Check geometry, orientation, waterlogging, collision, drops, and survival item consumption/refunds; matching complementary Slabs and the fourth matching Layer must complete to the canonical source while failed or incompatible placement leaves state and inventory intact.
5. Check independent log/wood axes, pillar grain, glazed orientation, all Stair shapes/halves, ordinary Walls without a material AXIS, lowered Path culling and Step UVs, and item/selector previews without changing placed rendering. Inspect authored glass Corners and generic glass Quarter Columns separately.
6. Check leaf distance/persistence/decay and tint, stripping, fuel, fire, spreadable Grass/Podzol/Mycelium/Pale Moss behavior, Copper oxidation/wax/scrape, Pointed Dripstone, Honey/Slime, Magma, Soul Sand, and Mangrove Roots on representative derived forms.
7. Check Stone's existing native Slab and Farmland Slab hoe conversion, hydration including water one block below, drying, crop support, trampling, and reversion. Preserve the state-only Farmland form and existing geometry/economy boundaries.
8. Check Bookshelf wooden end faces with the existing resource-pack seam present and absent, retained Bookshelf Stair UV-lock behavior, C94 spruce tint behavior, C96's tinted-Stair frames, Ribbits single-source toadstool surfaces, and Enderscape power/polarity/luminance/state projection.

Record only observations made against the exact current C106 SHA-256. This checklist is lifecycle-neutral and does not authorize Codex to inspect or modify any Minecraft testing profile. Accepted C70 and rollback C72 identities remain unchanged.
