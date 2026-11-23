# C32 verification checklist

Use the exact current JAR, SHA-256, and source checkpoint in [WORKBENCH_STATUS.json](WORKBENCH_STATUS.json).

For the AMC-present controlled run, add `"-Pamc_reference_jar=../architectural-material-closure/artifacts/AMC C32.jar"` to the Gradle invocation. Repeat the full invocation with that property omitted. Add `-Pwithout_enderscape` to each invocation to run the focused catalog and selector GameTest with Enderscape omitted. The full suite runs with Enderscape present. The controlled recipe audit uses the base, Bamboo trapdoor, Enderscape and Ribbits reference packs and DD C12’s packaged tall-door audit when the AMC JAR is supplied. Use the standard providers and full Matcha fixture in all four runs. Extended Block Shapes is reference-only.

The normal controlled tasks are `test runGameTest build stageCanaryArtifact`. Use the controlled dependencies listed here, including exact accepted Nibaru C45 and BGE C52, BBB Enderscape dev.7, Dramatic Doors C12, Ribbits C17, Quick Stack Nearby 0.4.0 with compatibility C6, and the audited external providers wired in `build.gradle`. Ribbits support is supplied through `ribbits_runtime_reference_jars`: YUNG's API compat.2, Trinkets Updated compatibility C5, GeckoLib 5.5.1, Cloth Config 26.2.155, Custom Portals C2, and Matcha Heart compatibility C11. Supply `matcha_food_resource_root` from the tracked Matcha Flavoured 1.12 data resource root. Enderscape uses the existing Lithostitched 1.7.13 and Trim Patcher 2.1 controlled inputs. These external inputs remain outside the production IBF archive. The exact BBB dependency predicate remains necessary because dev.7 contains the audited Enderscape package and its version is not ordered Fabric semantic versioning; the contract test rejects incompatible dev.6 before registry startup.

Run the controlled graphical check with the same full-provider arguments and `-Pdoor_viewport_test runClientGameTest`. This exercises the nine Bamboo accessories, 18 Bamboo trapdoors, four 23-member tall wood door selectors, 17 Mossy Oak trapdoors, all 23 positions of each two-high door selector and all 18 positions of each Enderscape trapdoor selector after a server data reload through the held-item overlay and inventory tooltip in a disposable Gradle test world.

### Bamboo and DD tall Macaw doors

1. Craft a Bamboo Button. Cycle a named stack of seven through Button, Pressure Plate, Bamboo Planks Path, Log Parapet, Plank Parapet, Blinds, Curtain Rod, Bamboo Shutter and Louvered Shutter. Check all nine overlay and inventory tooltip positions, the result, count and name. Craft a Bamboo Trapdoor and check Ranch between Paper and Swamp across its 18-position selector.
2. Craft each native DD tall door from its matching two-high source. Veiled, Celestial, Murublight and Mossy Oak use the DD parents `tall_veiled_door`, `tall_celestial_door`, `tall_murushroom_door` and `tall_mossy_oak_door`. Switch forward and backward through the 22 designs in the two-high wood door checklist below. Check the first and last icons at your usual and narrow GUI scales. Place, open, power and break representatives at each height. Check the dropped design and count.
3. Reload and check all six parent recipes, the native short-door conversions and the two-high selectors. All six new Bamboo recipes and all 88 DD tall alternate crafting recipes should be suppressed. Without AMC, check the original four Bamboo accessories and 17 Bamboo trapdoors and craft each available native tall door independently. Without Enderscape, check Mossy Oak and Bamboo. For the three missing-provider checks, run the controlled server with Fabric’s JVM option `-Dfabric.debug.disableModIds=<id>`, using `mcwdoors`, `mcwwindows` and `mcwtrpdoors` one at a time. Verify the loader names IBF and the missing dependency before world startup.

### Macaw wood trapdoors

1. With AMC C32 and Enderscape, craft each native Veiled, Celestial and Murublight trapdoor. From a named stack of seven, switch through Bamboo, Bark, Barn, Barred, Barrel, Beach, Blossom, Classic, Cottage, Four Panel, Glass, Mystic, Paper, Ranch, Swamp, Tropical and Whispering. Return through every position in reverse. Check count, components and the highlighted icon at your usual GUI scale.
2. After reload, verify each native crafting recipe and the Celestial-to-Murublight conversion. The 51 AMC alternatives should be accessible by switching. Check placement, opening and redstone on representatives.
3. With AMC and Ribbits, craft the AMC Classic Mossy Oak Trapdoor. Cycle a named stack of seven through the same designs in step 1, starting with Classic and skipping it between Blossom and Cottage. Check all 17 positions forward and backward, including Whispering in both the overlay and tooltip. Reload and verify Classic remains craftable and the 16 alternate crafting recipes are suppressed. Without AMC, each installed native trapdoor should keep its recipe and have no selector. Without Enderscape, repeat the Mossy Oak checks. Ribbits is required by IBF's Fabric metadata.

### Two-high wood doors

1. With AMC C32, Enderscape and Ribbits, craft Veiled, Celestial, Murublight and Mossy Oak doors from their native recipes. Open each selector on a named stack of seven. Cycle forward and backward across all 23 icons, including Whispering at the far end. Check both the held-item overlay and inventory tooltip at your usual GUI scale and at a narrow scaled width. Confirm the highlighted icon, resulting item, count and components at each step.
2. Verify each native door remains first. The alternate order is Bamboo, Bark Glass (Stem Glass for Celestial and Murublight), Barn, Barn Glass, Beach, Classic, Cottage, Four Panel, Glass, Japanese, Japanese2, Modern, Mystic, Nether, Paper, Stable, Stable Head, Swamp, Tropical, Waffle, Western and Whispering. Confirm that the 22 alternate crafting recipes per wood are suppressed after reload. Place and open representatives and check drops from both halves.
3. Without AMC, craft each available native door and verify it has no selector. With AMC and Ribbits alone, repeat the full Mossy Oak check. Enderscape doors should be absent. Record the exact IBF hash, provider set and GUI scale with observations.

### Mossy Oak switches

1. With Ribbits, AMC C32 and CNM loaded, craft the Ribbits Mossy Oak Planks Fence. Switch a named stack of seven through Fence Gate, AMC Frame and AMC Lattice in that order, then back. Check the name and count at each step.
2. Craft AMC Mossy Oak Trim. Repeat through Balustrade, Support and Pallet, then back. Inspect the recipe list for the two canonical recipes and verify that CNM suppresses the other six family members' crafting recipes.
3. With AMC absent, repeat the Ribbits Fence to Fence Gate round trip. Check that the Mossy Oak detail selector is absent. Record each provider set with the exact IBF release hash.

### Calcite and Shadoline Block

| AMC C32 | Enderscape 3.0.2 | Active families | Unique items | New selectors available |
|---|---|---:|---:|---|
| Present | Present | 267 | 2,146 | Four Calcite and four Shadoline Block |
| Absent | Present | 181 | 1,397 | None |
| Present | Absent | 224 | 1,779 | Four Calcite |
| Absent | Absent | 164 | 1,348 | None |

Repeat each applicable row for `calcite` and `shadoline_block`. Item IDs use `architectural_material_closure:<material>_<form>`. Open each selector from its canonical parent. Cycle a named stack of seven through each member and back, checking the item, count and components after every switch.

| Family key | Canonical form | Other forms in order |
|---|---|---|
| `cnm/building_accessory/<material>_button` | `button` | `pressure_plate`, `parapet`, `louvered_shutter` |
| `cnm/bbb_detail/stone/<material>` | `column` | `urn`, `moulding`, `fence`, `frame`, `arrow_slit` |
| `cnm/window/<material>` | `window` | `window2`, `four_window`, `pane_window`, `gothic_window` |
| `cnm/building_accessory/<material>` | `running_bond_path` | `strewn_rocky_path`, `windmill_weave_path`, `flagstone_path`, `crystal_floor_path`, `diamond_paving`, `basket_weave_paving`, `square_paving`, `honeycomb_paving`, `clover_paving`, `dumble_paving` |

After reload, check the parent recipes, including Column stonecutting, and verify alternate crafting and stonecutting results are suppressed. Find each material's full `running_bond`, `flagstone`, `windmill_weave` and `crystal_floor` in Building Blocks. Obtain each from its own recipe and check its placed ID. With only AMC installed among the two optional providers, repeat the Calcite checks and verify that the AMC End Stone Bricks Parapet and Louvered Shutter retain their standalone recipes.

### Veradite, Mirestone and Kurodite

The earlier family checks below assume Enderscape 3.0.2 is present. With exact AMC C32, repeat these checks for `veradite`, `mirestone` and `kurodite`. In the table, AMC forms use `architectural_material_closure:<material>_<form>`. The two polished controls use `enderscape:polished_<material>_<form>`.

| Family key | Canonical parent | Subsequent members in order |
|---|---|---|
| `cnm/building_accessory/enderscape_polished_<material>` | Enderscape `button` | Enderscape `pressure_plate`, AMC `parapet`, `louvered_shutter` |
| `cnm/bbb_detail/stone/<material>` | AMC `column` | `urn`, `moulding`, `fence`, `frame`, `arrow_slit` |
| `cnm/window/<material>` | AMC `window` | `window2`, `four_window`, `pane_window`, `gothic_window` |
| `cnm/building_accessory/<material>` | AMC `running_bond_path` | `strewn_rocky_path`, `windmill_weave_path`, `flagstone_path`, `crystal_floor_path`, `diamond_paving`, `basket_weave_paving`, `square_paving`, `honeycomb_paving`, `clover_paving`, `dumble_paving` |

Cycle a named stack of seven from each parent through every member and back. Verify the selected item, stack count and component patch at each step. After reload, check each parent recipe, Column stonecutting and cleanup of alternate results. Obtain and place each material's `running_bond`, `flagstone`, `windmill_weave` and `crystal_floor` full blocks separately. Check their Building Blocks tab entries, recipes and placed IDs.

With Enderscape present and AMC omitted, cycle each polished Button through its original Pressure Plate and back. Confirm the nine AMC-parented selectors disappear and recheck Enderscape recipes, including the polished Kurodite Button conversion.

### Purpur, Dusk Purpur, Resin, Tuff, Cinnabar, Sulfur and Smooth Basalt

Repeat each row for `purpur`, `dusk_purpur`, `resin`, `tuff`, `cinnabar`, `sulfur` and `smooth_basalt`. All IDs use `architectural_material_closure:<material>_<form>`. Open each family from its first item. Cycle a named stack of seven through every member in both directions and verify its name, components and count at every step.

| Family key | Canonical form | Alternate forms in order |
|---|---|---|
| `cnm/building_accessory/<material>_button` | `button` | `pressure_plate`, `parapet`, `louvered_shutter` |
| `cnm/bbb_detail/stone/<material>` | `column` | `urn`, `moulding`, `fence`, `frame`, `arrow_slit` |
| `cnm/window/<material>` | `window` | `window2`, `four_window`, `pane_window`, `gothic_window` |
| `cnm/building_accessory/<material>` | `running_bond_path` | `strewn_rocky_path`, `windmill_weave_path`, `flagstone_path`, `crystal_floor_path`, `diamond_paving`, `basket_weave_paving`, `square_paving`, `honeycomb_paving`, `clover_paving`, `dumble_paving` |

After reload, check each parent recipe and Column stonecutting. Verify alternate crafting and stonecutting results are suppressed. Find each materialÃ¢â‚¬â„¢s `running_bond`, `flagstone`, `windmill_weave` and `crystal_floor` full blocks in Building Blocks, obtain them from their own recipes and place each independently. Check its placed ID and confirm it stays outside the four selectors.

Check the vanilla Purpur and Resin Brick slabs and stairs and the Enderscape Dusk Purpur slab and stairs retain their own identities outside these selectors. Craft Resin Clumps from a single Resin Block and confirm the vanilla recipe survives reload.

Repeat without AMC. The twelve C26 selectors and sixteen C24 selectors should disappear. Recheck the Macaw and Enderscape families below, their canonical recipes and Quick Stack Nearby behavior.

### Mossy Stone Bricks selector checks

With AMC C32 loaded, open each Mossy Stone Bricks selector from its canonical parent. Switch a named stack of seven through every alternate in order and back in reverse. Check the item identity, count and components at each step.

| AMC canonical parent | AMC alternates in order |
|---|---|
| Mossy Stone Bricks Button | Pressure Plate, Parapet, Louvered Shutter |
| Mossy Stone Bricks Column | Urn, Moulding, Fence, Frame, Arrow Slit |
| Mossy Stone Bricks Window | Window 2, Four Window, Pane Window, Gothic Window |

After reload, check the three parent crafting recipes and the Column stonecutting recipe. Verify cleanup of alternate crafting recipes and of the Urn, Moulding and Fence stonecutting recipes.

With AMC installed and again without it, open the separate Macaw selector from `mcwpaths:mossy_stone_running_bond_path`. Cycle through its five Mossy Stone paths and six Mossy Cobblestone pavings and back. Check that its parent recipe remains available. Without AMC, the three Mossy Stone Bricks selectors should disappear.

### End Stone Bricks and Polished Blackstone selector checks

With AMC C32 loaded, open each selector below. Switch a named stack of seven through the listed alternates in order, then back to its canonical parent. Check the item, count and component patch after each switch.

| Canonical parent | Alternates in order |
|---|---|
| Enderscape Polished End Stone Button | Enderscape Polished End Stone Pressure Plate, AMC End Stone Bricks Parapet, AMC End Stone Bricks Louvered Shutter |
| AMC End Stone Bricks Column | AMC Urn, Moulding, Fence, Frame, then Macaw Arrow Slit (`mcwwindows:ender_brick_arrow_slit`) |
| AMC End Stone Bricks Window | AMC Window 2, Four Window, Pane Window, then Macaw Gothic Window (`mcwwindows:end_brick_gothic`) |
| AMC End Stone Bricks Running Bond Path | AMC Strewn Rocky, Windmill Weave, Flagstone, Crystal Floor paths, then Diamond, Basket Weave, Square, Honeycomb, Clover, Dumble pavings |
| Vanilla Polished Blackstone Button | Vanilla Polished Blackstone Pressure Plate, Macaw Blackstone Parapet, AMC Polished Blackstone Louvered Shutter |

After reload, check the five parent recipes, Column stonecutting and alternate cleanup. Stonecut and place the End Stone Bricks Running Bond, Flagstone, Windmill Weave and Crystal Floor full blocks separately. Verify their independent identities and recipes. Open the original Polished Blackstone Window and Blackstone Path selectors separately.

Repeat without AMC. Check that both Macaw End Stone Bricks items retain their standalone recipes. The three AMC-parented selectors should disappear. Cycle the Enderscape Polished End Stone Button through its original Pressure Plate and the vanilla Polished Blackstone Button through its original Pressure Plate and Macaw Parapet. Recheck both parent recipes after reload.

### Deepslate, Quartz and Nether Bricks selector checks

With the exact AMC C32 JAR loaded, cycle a named stack of seven through each applicable row in order. Return to the canonical parent and check item identity, count and component patch after every switch.

| Material | Canonical parent | Alternates in order |
|---|---|---|
| Deepslate, Quartz, Nether Bricks | AMC Button | AMC Pressure Plate, Parapet, Louvered Shutter |
| Deepslate, Quartz | Macaw Window | Macaw Window 2, Four Window, Pane Window, then AMC Gothic Window |
| Deepslate, Quartz | BBB Column | BBB Urn, Moulding, Fence, Frame, then AMC Arrow Slit |
| Nether Bricks | AMC Window | AMC Window 2, Four Window, Pane Window, then Macaw Nether Brick Gothic Window |
| Quartz, Nether Bricks | AMC Running Bond Path | Strewn Rocky, Windmill Weave, Flagstone, Crystal Floor paths, then Diamond, Basket Weave, Square, Honeycomb, Clover, Dumble pavings |

Check that parent recipes survive a reload and alternate recipes are cleaned up. For Quartz and Nether Bricks, stonecut and place each patterned full block independently (Running Bond, Flagstone, Windmill Weave, Crystal Floor). Each should keep its own block identity and recipe and offer no IBF selector. Open the original Deepslate and Cobbled Deepslate Path selectors separately. Check that the Macaw Nether Brick Arrow Slit still cycles to the BBB Nether Brick Column.

Repeat without AMC. The three Button selectors, two new Path selectors and Nether Bricks Window selector should disappear. Check that the standalone Macaw Nether Brick Gothic Window retains its recipe. Cycle both four-member Macaw Window selectors and both five-member BBB Column selectors, then check the original parent recipes and Nether Brick BBB selector again.

### Sandstone and Red Sandstone selector checks

With AMC C32 loaded, repeat each row for Sandstone and Red Sandstone. Open the selector from its canonical parent, switch a named stack of seven through the listed alternates in order, then return to the parent. Check the item, count, and component patch after every switch.

| Canonical parent | Alternates in order |
|---|---|
| AMC Button | AMC Pressure Plate, AMC Parapet, AMC Louvered Shutter |
| Macaw Window | Macaw Window 2, Macaw Four Window, Macaw Pane Window, AMC Gothic Window |
| BBB Column | BBB Urn, BBB Moulding, BBB Fence, BBB Frame, AMC Arrow Slit |

After reload, verify the three parent recipes and BBB Column stonecutting recipe for each material. Check that alternate crafting recipes are cleaned up. Open each Macaw Running Bond Path selector separately and check its original eleven paths and pavings.

Repeat without AMC. Each Macaw Window should cycle through its original four variants and each BBB Column through its original five details. Check that their parent recipes and both Running Bond Path recipes remain available and that neither AMC Button selector appears.

### Prismarine selector checks

With AMC C32 loaded, cycle a named stack of seven through each of these selectors and back to its parent. Check item identity, stack count, and component patch after each switch.

| Material | Parent | Alternates in order |
|---|---|---|
| Prismarine Bricks and Dark Prismarine, each separately | AMC Button | AMC Pressure Plate, matching Macaw Parapet, AMC Louvered Shutter |
| Prismarine Bricks and Dark Prismarine, each separately | AMC Column | AMC Urn, Moulding, Fence, Frame, matching Macaw Arrow Slit |
| Prismarine Bricks | AMC Running Bond Path | Strewn Rocky, Windmill Weave, Flagstone, Crystal Floor paths, then Diamond, Basket Weave, Square, Honeycomb, Clover, Dumble pavings (all AMC) |

Check that both Button recipes, both Column crafting and stonecutting recipes, and the Running Bond Path stonecutting recipe survive a reload. Check that alternate recipes are removed. Open the two existing Macaw window selectors and the Dark Prismarine Macaw Path selector from their original parents.

Stonecut and place the AMC Prismarine Bricks Running Bond, Flagstone, Windmill Weave, and Crystal Floor full blocks individually. Each should keep its own identity and sit outside all five selectors.

With AMC absent, check that the five selectors disappear and that both Macaw parapets and both Macaw arrow slits keep their own recipes. Repeat the window and Dark Prismarine Path checks.

### Mud Bricks selector checks

With AMC C32 installed, start with a named stack of seven of each canonical item. Cycle through its selector in the order below and return to the parent. Check the name, count and components at every step.

| Canonical item | Alternates in order |
|---|---|
| AMC Mud Bricks Button | AMC Pressure Plate, Parapet, Louvered Shutter |
| AMC Mud Bricks Column | AMC Urn, Moulding, Fence, Frame, then Macaw Mud Brick Arrow Slit |
| AMC Mud Bricks Window | AMC Window 2, Four Window, Pane Window, then Macaw Mud Brick Gothic Window |

Check that the three parent crafting recipes and the Column stonecutting recipe remain available. Check cleanup of the alternate crafting recipes and the Urn, Moulding and Fence stonecutting recipes. Open the separate Mud Brick Path selector from `mcwpaths:mud_brick_running_bond_path` and check its original eleven Paths and Pavings.

Repeat without AMC. Neither Macaw item should expose a Mud Bricks selector, and both should remain independently craftable. Repeat the selector and recipe checks after a reload.

### Existing family checks

1. With AMC C32 loaded, repeat the following for Andesite, Diorite, and Granite. Use a named stack of seven and switch through each selector, checking count and components after every change and on return to the first item.

   | Selector | Expected order |
   |---|---|
   | AMC Button | AMC Button, AMC Pressure Plate, Macaw Parapet, Macaw Louvered Shutter |
   | AMC Column | AMC Column, Urn, Moulding, Fence, Frame, Arrow Slit (all AMC) |
   | Macaw Window | Macaw Window, Window 2, Four Window, Pane Window, AMC Gothic Window |
   | Macaw Running Bond Path | Running Bond, Strewn Rocky, Windmill Weave, Flagstone, Crystal Floor paths, followed by Diamond, Basket Weave, Square, Honeycomb, Clover, Dumble pavings |

   Open each selector from an alternate as well. Check that the AMC Button recipe and both AMC Column recipes (crafting and stonecutting) remain available. Check that the alternate crafting and stonecutting recipes are removed.

2. In both configurations, cycle the BBB Stone Column through BBB Stone Urn, Moulding, Fence, Frame, then Macaw Stone Brick Arrow Slit. Check that Cobblestone Arrow Slit keeps its independent recipe. With AMC absent, open each of the three Macaw window selectors and verify the original four windows. The material parapets and louvered shutters should keep their own recipes with no accessory selector. Verify that the C16 Stone Button family still has Button, Pressure Plate, AMC Parapet, AMC Louvered Shutter with AMC present, and the vanilla pair when absent.
3. Recheck the existing Macaw ordinary and Dramatic Doors tall-door families, trapdoors, windows/shutters, the 77 audited Macaw thin Paths and 78 Pavings, Aurora lanterns, Ribbits, BBB, vanilla fences, bars/chains, and accessories. Confirm wood shutters/louvered shutters occur only with their Button-parent wood family; Stone Button is separate from Stone thin Paths; and the Nether Brick arrow slit still belongs to its original BBB detail family. Providers retain native placement, collision, redstone, recipe costs, drops, and copper behavior. CNM retains switching and recipe/equivalence ownership. Macaw path blocks and patterned full-block/slab/stair geometry remain outside IBF.
4. Exercise all 15 literal display-fixture triples: Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Pale Oak, Bamboo, Crimson, Warped, Veiled, Celestial, and Murublight. Each uses Sign as canonical, then Hanging Sign and Shelf. Wall Sign and Wall Hanging Sign placement blocks and ordinary geometry remain outside these triples.
5. Verify Veiled, Celestial, and Murublight Fence/Fence Gate/BBB Frame/Lattice families; their three separate BBB Trim/Balustrade/Support/Pallet detail families; the seven Enderscape Button families (Polished End Stone also includes the AMC parapet and shutter when available); and Shadoline Bars/Chain. Confirm the original C7/C8 families and canonical parents remain intact without cross-material merging.
6. After reload, check canonical acquisition and recipe cleanup. The controlled recipe corpus removes 1,837 alternate recipes with AMC and 1,114 without it. Both runs retain every canonical recipe. Stone Running Bond Path remains obtainable as the Stone Path parent, while Stone Strewn Rocky Path is removed. Five Enderscape `void_lachryma` conversions remain provider-owned at CNM's custom-recipe boundary. Recheck a pre-existing Macaw and BBB canonical recipe and the separate accepted Nibaru/BGE component.
7. Transfer component-bearing stacks through a vanilla, Enderscape, and BBB family; preserve exact count and component patch. Mismatched components and incompatible targets must fail closed. Check Quick Stack Nearby affinity and the selector viewport/transfer behavior for a 22-member family.
8. Restart or reload once, repeat representative C7, C8, and C9 families, and report only observed missing members, duplicate membership, canonical-parent changes, cross-family matches, recipe drift, crashes, or reload errors against the exact release identity.

This checklist is lifecycle-neutral and does not authorize access to a Minecraft testing profile. Runtime evidence and acceptance require separate owner direction.
