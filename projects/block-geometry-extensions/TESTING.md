# BGE geometry verification

## Canary 112 Mossy Oak

Rotate raw and stripped log and wood slabs, stairs and walls across all axes. Compare inventory and placed faces with the source pillar. Inspect the separate Ribbits beam side and end on joined walls and slabs. Repeat the timber comparison with Foundation v12 above Matcha and with Foundation disabled.

## Canary 111 timber and beam geometry

Match BGE C111 and Mynx Trees C9 to their manifests. For Silver Birch, Wisteria and Mossy Oak, cycle the 18 Log/Wood roles and the separate 18 Stripped Log/Stripped Wood roles. Place every shape on multiple axes, then axe-strip the raw counterpart. Verify its axis, geometry, neighbor connections and drops. In particular, a Silver Birch Log cut uses the owner's rounded end texture and Silver Birch Wood exposes bark on every face.

For Silver Birch and Wisteria Planks, the second nine-role batch is the corresponding Beam. Compare its post, arm width, joins and collision against the BBB beam wall at a straight run, corner and junction. Compare the Planks Wall separately. Check CTM seams, inventory UVs, slabs and stairs, then check the existing 18-position Ribbits plank/beam selector. Repeat with the tree provider omitted to check existing providers.

## Canary 110 Mossy Oak

Match `BGE C110.jar` to its manifest and enable Ribbits C27. Obtain Mossy Oak Planks. Cycle a named stack through all 18 entries. Each group of nine follows Full, Slab, Stairs, Wall, Vertical Slab, Step, Corner, Quarter Column and Layer. The Planks group precedes Beam. Repeat from the final entry and check the count, custom components and inventory icons.

Place Beam blocks and shapes along X, Y and Z. Check both slab halves, stair turns and halves, wall arms next to solids and other walls, connected straight runs, isolated posts, end grain, side bands, culling and waterlogging. Compare against the original BBB Oak Beam in the same lighting. The thin wooden Beam Wall has a visible center and narrow arms. Check its collision in a straight run, corner and junction, then compare the ordinary Planks Wall. In survival place and break each shape. With CNM loot inheritance enabled, each returns the canonical Ribbits plank. Without Ribbits check that its 18-member selector is absent.

Run Java 25 `gradlew build`, `tools/Test-MaterialProfileArchitecture.ps1` and `tools/Verify-UnifiedArtifact.ps1` against the retained source jars. The server regression compares BBB's actual wall models and exercises all 324 selector transfers, single-block placement, inherited loot and each geometry's direct loot table. Record resource packs and renderer for owner visual observations.

### Registered selector order

| Role | Positions 1–9, Planks | Positions 10–18, Beam |
| --- | --- | --- |
| Full block | 1. `ribbits:mossy_oak_planks` | 10. `cnm_terrain_slabs_compat:ribbits/mossy_oak_beam` |
| Horizontal slab | 2. `ribbits:mossy_oak_planks_slab` | 11. `cnm_terrain_slabs_compat:ribbits/mossy_oak_beam_slab` |
| Stairs | 3. `ribbits:mossy_oak_planks_stairs` | 12. `cnm_terrain_slabs_compat:ribbits/mossy_oak_beam_stairs` |
| Wall | 4. `cnm_terrain_slabs_compat:ribbits/mossy_oak_planks_wall` | 13. `cnm_terrain_slabs_compat:ribbits/mossy_oak_beam_wall` |
| Vertical slab | 5. `clutternomore:ribbits/vertical_mossy_oak_planks_slab` | 14. `clutternomore:cnm_terrain_slabs_compat/vertical_ribbits/mossy_oak_beam_slab` |
| Step | 6. `clutternomore:ribbits/mossy_oak_planks_step` | 15. `clutternomore:cnm_terrain_slabs_compat/ribbits/mossy_oak_beam_step` |
| Corner | 7. `cnm_terrain_slabs_compat:ribbits/mossy_oak_planks_corner` | 16. `cnm_terrain_slabs_compat:cnm_terrain_slabs_compat/ribbits/mossy_oak_beam_corner` |
| Quarter column | 8. `cnm_terrain_slabs_compat:ribbits/mossy_oak_planks_quarter_column` | 17. `cnm_terrain_slabs_compat:cnm_terrain_slabs_compat/ribbits/mossy_oak_beam_quarter_column` |
| Layer | 9. `cnm_terrain_slabs_compat:ribbits/mossy_oak_planks_layer` | 18. `cnm_terrain_slabs_compat:cnm_terrain_slabs_compat/ribbits/mossy_oak_beam_layer` |

## Previous C109 patterned geometry reference

Use the private local `BGE C109.jar` and the SHA-256 and source checkpoint in `WORKBENCH_STATUS.json`. The controlled reference provider is `AMC C28.jar`, SHA-256 `c90194fb12306d1c47b22023eab23e62869f8310a56ab2c322c8f635bbce7fa9`. Use Enderscape 3.0.2 for the twenty Enderscape-backed patterns. Keep the JARs local.

## AMC patterns

1. In the owner-selected environment, record the release hashes, mod versions, active resource packs and renderer. Check each combination of `prismarine_bricks`, `quartz`, `nether_bricks`, `end_stone_bricks`, `tuff`, `cinnabar`, `sulfur`, `smooth_basalt`, `veradite`, `mirestone`, `kurodite`, `purpur`, `resin`, `dusk_purpur`, `calcite`, and `shadoline_block` with `running_bond`, `flagstone`, `windmill_weave`, and `crystal_floor`. Each parent is `architectural_material_closure:<material>_<pattern>`.
2. Stonecut one parent, then use the selector to cycle through Full Block, Horizontal Slab, Stairs, Wall, Vertical Slab, Step, Corner, Quarter Column and Layer. Repeat with a named stack. Check the name, count, order, item previews, and that switching stays within the selected pattern.
3. Place the shapes against their unengraved parent. Follow the same grout line across top, bottom and side faces. Inspect both Slab halves, straight/inner/outer Stairs in both halves, Wall posts and low/tall arms, both sides of the Vertical Slab, the inner Step riser, Corner recesses, diagonal Quarter Columns and Layers on all six faces. Check scale and alignment from multiple directions.
4. Toggle the full block with a pickaxe, then toggle it back. In survival, check collisions and outlines, waterlogging, tool drops, compatible Slab and fourth-Layer completion, and stack counts before and after obstructed or incompatible placement.
5. Stonecut raw Resin Blocks into each Resin pattern. Compare the artwork with its engraved AMC parent and check its Resin Bricks palette on every role. Check Dusk Purpur against its own AMC parent in the active Enderscape pack.
6. Check that ordinary vanilla Purpur and Resin Bricks keep their separate selectors. Check that ordinary Enderscape Veradite, Mirestone, Kurodite, Dusk Purpur, and Block of Shadoline each have a separate nine-role selector with their native Slab, Stairs and Wall. Each of the four AMC patterns per material must have its own selector, artwork and preview. Check Macaw's patterned families still use their original Slabs and Stairs.
7. Stonecut Calcite and Block of Shadoline into each of their four patterns. Compare placed shapes and inventory previews with the corresponding full block. With AMC C28 alone, check all four Calcite selectors are available and all four Shadoline pattern selectors are absent.

## Controlled reproduction

Run `gradlew build` for the full regression suite. Run `gradlew -PamcGameTest verifyAmcModels` on Java 25 for each row below. The task hashes the AMC input, runs an isolated Fabric server and checks the emitted blockstates, models and previews against the provider packs. The Enderscape runs use its retained JAR and the test libraries Lithostitched, Trim Patcher and YACL. Use `-PenderscapeTestDeps=<directory>` to select their local directory.

| Additional Gradle flags | Expected AMC families | Ordinary Enderscape families |
| --- | ---: | --- |
| Default (AMC C28 and Enderscape) | 64 | Present |
| `-PwithoutTestEnderscape` | 44 | Absent |
| `-PamcTestVersion=27` | 56 | Present |
| `-PamcTestVersion=26` | 56 | Present |
| `-PamcTestVersion=25` | 44 | Present |
| `-PamcTestVersion=24` | 44 | Present |
| `-PamcTestVersion=23` | 32 | Present |
| `-PamcTestVersion=none` | 0 | Present |
| `-PamcTestVersion=none -PwithoutTestEnderscape` | 0 | Absent |

In each configuration verify that unavailable patterned parents and all eight derived registrations are absent. Run `tools/Test-MaterialProfileArchitecture.ps1` and `tools/Verify-UnifiedArtifact.ps1` for the architecture and private archive checks.

## Existing family regressions

1. Record the exact current artifact SHA-256, Minecraft/mod versions, resource-pack order, shader/renderer settings, and world context in an owner-approved isolated Minecraft 26.2 environment.
2. Check the native and optional-provider family selectors. Each explicit family must retain its canonical nine-role order and reuse its declared provider Slab/Stairs/Wall. Sample Macaw patterns and plain Paths, Mynx logs/leaves, all Ribbits toadstools, Mossy Stone, and Enderscape stateful materials. Unknown CNM materials remain CNM-owned and receive no inferred BGE family.
3. Search Creative inventory and JEI for `veiled`, `celestial`, and `murublight`. The six BBB Beam roots/Walls for those materials must not appear as independent entries. Each matching Enderscape `[Planks][Beam]` selector must still expose its complete existing Planks and Beam geometries in the same order. Check existing placed/saved references; do not infer a registration or persistence change from presentation.
4. Place and switch representative native/provider Slabs, Stairs, Walls, Vertical Slabs, Steps, Corners, Quarter Columns, and Layers. Check geometry, orientation, waterlogging, collision, drops, and survival item consumption/refunds; matching complementary Slabs and the fourth matching Layer must complete to the canonical source while failed or incompatible placement leaves state and inventory intact.
5. Check independent log/wood axes, pillar grain, glazed orientation, all Stair shapes/halves, ordinary Walls without a material AXIS, lowered Path culling and Step UVs, and item/selector previews without changing placed rendering. Inspect authored glass Corners and generic glass Quarter Columns separately.
6. Check leaf distance/persistence/decay and tint, stripping, fuel, fire, spreadable Grass/Podzol/Mycelium/Pale Moss behavior, Copper oxidation/wax/scrape, Pointed Dripstone, Honey/Slime, Magma, Soul Sand, and Mangrove Roots on representative derived forms.
7. Check Stone's existing native Slab and Farmland Slab hoe conversion, hydration including water one block below, drying, crop support, trampling, and reversion. Preserve the state-only Farmland form and existing geometry/economy boundaries.
8. Check Bookshelf wooden end faces with the existing resource-pack seam present and absent, retained Bookshelf Stair UV-lock behavior, C94 spruce tint behavior, C96's tinted-Stair frames, Ribbits single-source toadstool surfaces, and Enderscape power/polarity/luminance/state projection.

Record only observations made against the exact current C109 SHA-256. This checklist is lifecycle-neutral and does not authorize Codex to inspect or modify any Minecraft testing profile. Accepted C70 and rollback C72 identities remain unchanged.
