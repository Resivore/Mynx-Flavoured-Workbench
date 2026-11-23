# Architectural Material Closure verification

## Canary 35 wood windows and Mossy Oak fixtures

Match `AMC C35.jar` to the current manifest. Use Macaw's Windows 2.4.2, BBB dev.7 and Minecraft 26.2. Repeat the window checks with Enderscape and Ribbits together, separately and both absent. Bamboo has 12 windows in every configuration. Enderscape adds 36 and Ribbits adds 12 windows and four fixtures.

1. For Bamboo, Veiled, Celestial, Murublight and Mossy Oak, check timber, stripped timber and planks side by side. Within each finish place Window, Window 2, Four Window and Pane Window against their Oak Macaw references. Check facing, joined neighbors, open/closed interaction, water, outlines, drops, inventory icons and glass transparency. Compare Bamboo stalk direction on adjoining rails, end grain on cuts, and moss on the Mossy Oak frames.
2. Craft a Mossy Oak Sign. Place a standing and a wall sign, edit both sides, wax it, unload the chunk and return. Repeat with a hanging sign underneath a support and on a wall. Inspect the editing screen, glyph contrast, supports, rotations and dropped items. Place a Shelf, load its three slots, check the displayed stacks and powered interaction, then reload and break it. Compare each step with the native Oak fixture.
3. Place the Mossy Oak Lantern on and below supports. Compare it with the BBB Oak Lantern under the same lighting. Use the BBB Hammer, remove the support and collect the drop. Inspect metal and light pixels and compare the separate Ribbits Swamp Lantern.
4. In Java 25 run the existing Gradle `build` with the local artwork sources and Python/Pillow. Run `runGameTest` with `-PwithEnderscape` and `-PwithRibbits` separately and together, supplying their existing test dependencies. The build audits the archive against C34 and traverses the 60 new Macaw graphs. Record owner observations against the manifest hash.

## Previous C34 shutter and accessory checks

Match the SHA-256 of `AMC C34.jar` to the current release in `WORKBENCH_STATUS.json`. Use Minecraft Java 26.2 Fabric with Fabric API, Macaw's Windows, Macaw's Paths and Pavings, and Building But Better. The controlled reference versions were Windows 2.4.2, Paths 1.1.1 and BBB C9 (2.0pre4+26.2-enderscape-dev.7). AMC supplies 392 base pairs, 144 additional pairs with Enderscape and six additional pairs with Ribbits. With Macaw’s Doors 1.1.5 also present, add 66 Enderscape doors and 22 Ribbits doors. All AMC IDs use the namespace `architectural_material_closure`.

### C33 wood accessories

With Enderscape installed, check `veiled`, `celestial` and `murublight`. With Ribbits installed, check `mossy_oak`. In `architectural_material_closure`, each wood has the suffixes `planks_path`, `log_parapet` (Veiled and Mossy Oak) or `stem_parapet` (Celestial and Murublight), `plank_parapet`, `blinds`, `curtain_rod`, `shutter` and `louvered_shutter`. Mossy Oak also has `button` and `pressure_plate`. Enderscape supplies its original controls.

Compare each installed regular shutter with its owner front in `build/c34/previews/regular_shutters.png` and the placed and inventory views in the four wood sheets. Veiled and Mossy Oak use complete 16×16 owner textures. Celestial and Murublight replace the broad face at (0, 1), preserving all 91 surrounding C33 texels. Check both shutter hinges and stacked operation, all five louver highlights and their recesses, the plank-path facing, the parapet support with and without a pot, joined blind cycles and the stripped shaft of each curtain rod. Test the Mossy Oak Button with a hand and arrow and the Pressure Plate with a dropped item.

Use the original Oak crafting grids. Substitute native slabs, planks, logs/stems and trapdoors. Mossy Oak uses Ribbits slabs in the Log Parapet and Curtain Rod log slots. Three AMC Classic Trapdoors arranged vertically make three Mossy Oak regular shutters. Without Macaw's Trapdoors that recipe and its advancement disappear while the shutter block remains. The unchanged C32 regression procedures below describe their original registry totals; add 21 pairs with Enderscape and nine with Ribbits. Run Java 25 `build runGameTest` with both, each and neither wood provider, then with both woods and `-PwithoutTrapdoors`. Compare with the exact retained C32. Record observations against the C34 hash.

### Bamboo additions

All six registry paths below use the `architectural_material_closure` namespace. Compare them with the matching `oak_` designs in Macaw's Windows 2.4.2 or, for Ranch, Macaw's Trapdoors 1.1.5.

| Bamboo registry path | Reference implementation | Crafting grid ingredients | Yield |
|---|---|---|---:|
| `bamboo_ranch_trapdoor` | Macaw Ranch's vanilla TrapDoorBlock and block-set type | Four Bamboo Blocks in the corners, five sticks in the cross | 2 |
| `bamboo_log_parapet` | Parapet | Five sticks around a Bamboo Block in the lower middle of a 3×2 grid | 5 |
| `bamboo_plank_parapet` | Parapet | Same pattern with Bamboo Planks in the lower middle | 5 |
| `bamboo_blinds` | Blinds | Three Bamboo Slabs above two rows of three sticks | 3 |
| `bamboo_curtain_rod` | CurtainRod | Stick, Bamboo Block, stick in one row | 5 |
| `bamboo_louvered_shutter` | Shutter | Three Bamboo Slabs in a vertical column | 3 |

1. Compare the six designs with the local `build/c32/previews/bamboo.png` sheet, in inventory and at normal viewing distance. Inspect Bamboo Block end grain on crosscuts and lengthwise side grain along the Ranch frame, diagonal rails, log shelf and tilted pot support. The Ranch uses the regular Bamboo Block on every wooden member. Plank Parapet, Blinds and Curtain Rod bind Bamboo Planks directly. The Louvered Shutter samples native Bamboo Planks at the Oak shutter's atlas coordinates, preserving its transparency and the light and shadow on all five slats.
2. Place upper/lower Ranch trapdoors facing all four ways, underwater and dry. Toggle by hand and redstone, then check the drop. Place a flower pot above each parapet and remove it. Cycle joined Blinds through open, closed and raised states, place rods in every facing and toggle a stack of left- and right-hinged shutters. Craft every grid above. Compare with Macaw's ordinary Bamboo Shutter, vanilla Bamboo Button and Pressure Plate and the Macaw Bamboo Planks Path.
3. With both wood providers and both Macaw door providers installed, check 699 AMC pairs. Remove Macaw's Trapdoors and check 630 pairs including all five Bamboo window forms. With neither wood provider, check 393 with Macaw's Trapdoors or 392 without it. Record owner observations against the C32 hash.

The Java 25 `build` check revalidates the retained C31 and earlier packages, resolves the Bamboo model hierarchies and compares authored face UVs and the generated pixels. Use `-Pc31Jar=<path>` to select the retained C31 baseline. Run `runGameTest` both with and without `-PwithoutTrapdoors`, including `-PwithRibbits -PwithEnderscape` for full-provider coverage.

### Macaw wood trapdoors

Macaw's Trapdoors 1.1.5 for Fabric 26.2 provides 195 registered trapdoors. Its 192 wood entries span 17 designs, including the standalone bamboo cane recipe. Three fixed Metal designs have no wood series. None of the 68 combinations below was registered by Macaw, Enderscape, Ribbits or C30. Enderscape's three native trapdoors remain separate. The 51 new Enderscape pairs require Enderscape and Macaw's Trapdoors. The 17 Mossy Oak pairs require Ribbits and Macaw's Trapdoors. Macaw's Doors is independently optional.

Every destination below is an exact registry path in `architectural_material_closure`. In the two rows with two sources, Oak supplies Veiled and Mossy Oak. Warped supplies the Celestial and Murublight stem models. Each other row has one source for all four destinations.

| Macaw reference | Veiled | Celestial | Murublight | Mossy Oak |
|---|---|---|---|---|
| `mcwtrpdoors:oak_bamboo_trapdoor` | `veiled_bamboo_trapdoor` | `celestial_bamboo_trapdoor` | `murublight_bamboo_trapdoor` | `mossy_oak_bamboo_trapdoor` |
| `mcwtrpdoors:oak_bark_trapdoor` / `mcwtrpdoors:warped_bark_trapdoor` | `veiled_bark_trapdoor` | `celestial_bark_trapdoor` | `murublight_bark_trapdoor` | `mossy_oak_bark_trapdoor` |
| `mcwtrpdoors:oak_barn_trapdoor` | `veiled_barn_trapdoor` | `celestial_barn_trapdoor` | `murublight_barn_trapdoor` | `mossy_oak_barn_trapdoor` |
| `mcwtrpdoors:oak_barred_trapdoor` | `veiled_barred_trapdoor` | `celestial_barred_trapdoor` | `murublight_barred_trapdoor` | `mossy_oak_barred_trapdoor` |
| `mcwtrpdoors:oak_barrel_trapdoor` | `veiled_barrel_trapdoor` | `celestial_barrel_trapdoor` | `murublight_barrel_trapdoor` | `mossy_oak_barrel_trapdoor` |
| `mcwtrpdoors:oak_beach_trapdoor` | `veiled_beach_trapdoor` | `celestial_beach_trapdoor` | `murublight_beach_trapdoor` | `mossy_oak_beach_trapdoor` |
| `mcwtrpdoors:oak_blossom_trapdoor` | `veiled_blossom_trapdoor` | `celestial_blossom_trapdoor` | `murublight_blossom_trapdoor` | `mossy_oak_blossom_trapdoor` |
| `mcwtrpdoors:spruce_classic_trapdoor` | `veiled_classic_trapdoor` | `celestial_classic_trapdoor` | `murublight_classic_trapdoor` | `mossy_oak_classic_trapdoor` |
| `mcwtrpdoors:oak_cottage_trapdoor` | `veiled_cottage_trapdoor` | `celestial_cottage_trapdoor` | `murublight_cottage_trapdoor` | `mossy_oak_cottage_trapdoor` |
| `mcwtrpdoors:oak_four_panel_trapdoor` | `veiled_four_panel_trapdoor` | `celestial_four_panel_trapdoor` | `murublight_four_panel_trapdoor` | `mossy_oak_four_panel_trapdoor` |
| `mcwtrpdoors:oak_glass_trapdoor` | `veiled_glass_trapdoor` | `celestial_glass_trapdoor` | `murublight_glass_trapdoor` | `mossy_oak_glass_trapdoor` |
| `mcwtrpdoors:oak_mystic_trapdoor` | `veiled_mystic_trapdoor` | `celestial_mystic_trapdoor` | `murublight_mystic_trapdoor` | `mossy_oak_mystic_trapdoor` |
| `mcwtrpdoors:oak_paper_trapdoor` | `veiled_paper_trapdoor` | `celestial_paper_trapdoor` | `murublight_paper_trapdoor` | `mossy_oak_paper_trapdoor` |
| `mcwtrpdoors:oak_ranch_trapdoor` / `mcwtrpdoors:warped_ranch_trapdoor` | `veiled_ranch_trapdoor` | `celestial_ranch_trapdoor` | `murublight_ranch_trapdoor` | `mossy_oak_ranch_trapdoor` |
| `mcwtrpdoors:oak_swamp_trapdoor` | `veiled_swamp_trapdoor` | `celestial_swamp_trapdoor` | `murublight_swamp_trapdoor` | `mossy_oak_swamp_trapdoor` |
| `mcwtrpdoors:oak_tropical_trapdoor` | `veiled_tropical_trapdoor` | `celestial_tropical_trapdoor` | `murublight_tropical_trapdoor` | `mossy_oak_tropical_trapdoor` |
| `mcwtrpdoors:oak_whispering_trapdoor` | `veiled_whispering_trapdoor` | `celestial_whispering_trapdoor` | `murublight_whispering_trapdoor` | `mossy_oak_whispering_trapdoor` |

#### Owner visual and interaction checks

1. Compare each row in the local `build/c31/previews/{veiled,celestial,murublight,mossy_oak}.png` sheets. Each shows the Macaw reference, the closed lower placement, an open placement and the packaged item's GUI model. Compare actual inventory and placed views in each wood. Inspect the clear Glass and Bark panes, Paper fittings, Barred openings, Barrel bands, Ranch diagonals and slat edges.
2. Place each trapdoor in upper and lower positions in all four directions, including underwater. Compare the reference outline, collision, sounds, hand toggling and powered/unpowered transitions. Check the item drop and crafting output. Mossy Oak Bark uses four Ribbits Mossy Oak Slabs and two glass panes for two trapdoors. Glass uses planks. Ranch uses four Mossy Oak Planks and five sticks for two.
3. Check all four wood-provider combinations with Macaw's Trapdoors installed and absent. With Macaw's Doors installed, the expected AMC totals are 699 with both woods, 654 with Enderscape, 438 with Ribbits, 393 with neither and 630 with both woods but without Macaw's Trapdoors. With Macaw's Trapdoors absent, the Enderscape-only, Ribbits-only and neither-wood totals are 602, 420 and 392. With both woods and only Macaw's Trapdoors installed among the two Macaw door providers, expect 611. Record observations against the exact C32 hash.

The Java 25 `build` task runs the historical resource checks and the C31 artifact comparison. The texture checker uses Pillow and NumPy. Use `-PtrapdoorsJar=<path>` and `-Pc30Jar=<path>` to select retained inputs. Run `runGameTest` in each C30 wood configuration with and without `-PwithoutTrapdoors`, then run both woods with `-PwithoutDoors`. The 15 authored trapdoor tiles per wood use design-specific cross-wood masks. The upward face reverses the vertical texture coordinate for the selective moss footprint. Bark and Ranch reuse the actual model face slots and UVs, including the log/stem sides, stripped Ranch rails and glass. The effective vanilla wooden trapdoor and axe tags are carried through. The provider's legacy plural tag directories do not supply loaded 26.2 tags.

### Macaw wood doors

Each occupied cell below gives an exact AMC registry path in the `architectural_material_closure` namespace. Macaw uses Bark Glass for log woods and Stem Glass for stem woods. The native `enderscape:veiled_door`, `enderscape:celestial_door`, `enderscape:murublight_door` and `ribbits:mossy_oak_door` remain provider-owned. Macaw's fixed-material metal, store, sliding glass, garage and portcullis designs have no per-wood series. The 1.1.5 JAR contains a Cherry Waffle blockstate and item definition without a corresponding BlockInit registration. Oak Waffle is registered and supplies the Waffle reference here.

| Macaw reference | Veiled | Celestial | Murublight | Mossy Oak |
|---|---|---|---|---|
| `mcwdoors:oak_bamboo_door` | `veiled_bamboo_door` | `celestial_bamboo_door` | `murublight_bamboo_door` | `mossy_oak_bamboo_door` |
| `mcwdoors:oak_bark_glass_door` | `veiled_bark_glass_door` | — | — | `mossy_oak_bark_glass_door` |
| `mcwdoors:oak_barn_door` | `veiled_barn_door` | `celestial_barn_door` | `murublight_barn_door` | `mossy_oak_barn_door` |
| `mcwdoors:oak_barn_glass_door` | `veiled_barn_glass_door` | `celestial_barn_glass_door` | `murublight_barn_glass_door` | `mossy_oak_barn_glass_door` |
| `mcwdoors:oak_beach_door` | `veiled_beach_door` | `celestial_beach_door` | `murublight_beach_door` | `mossy_oak_beach_door` |
| `mcwdoors:spruce_classic_door` | `veiled_classic_door` | `celestial_classic_door` | `murublight_classic_door` | `mossy_oak_classic_door` |
| `mcwdoors:oak_cottage_door` | `veiled_cottage_door` | `celestial_cottage_door` | `murublight_cottage_door` | `mossy_oak_cottage_door` |
| `mcwdoors:oak_four_panel_door` | `veiled_four_panel_door` | `celestial_four_panel_door` | `murublight_four_panel_door` | `mossy_oak_four_panel_door` |
| `mcwdoors:oak_glass_door` | `veiled_glass_door` | `celestial_glass_door` | `murublight_glass_door` | `mossy_oak_glass_door` |
| `mcwdoors:oak_japanese_door` | `veiled_japanese_door` | `celestial_japanese_door` | `murublight_japanese_door` | `mossy_oak_japanese_door` |
| `mcwdoors:oak_japanese2_door` | `veiled_japanese2_door` | `celestial_japanese2_door` | `murublight_japanese2_door` | `mossy_oak_japanese2_door` |
| `mcwdoors:oak_modern_door` | `veiled_modern_door` | `celestial_modern_door` | `murublight_modern_door` | `mossy_oak_modern_door` |
| `mcwdoors:oak_mystic_door` | `veiled_mystic_door` | `celestial_mystic_door` | `murublight_mystic_door` | `mossy_oak_mystic_door` |
| `mcwdoors:oak_nether_door` | `veiled_nether_door` | `celestial_nether_door` | `murublight_nether_door` | `mossy_oak_nether_door` |
| `mcwdoors:oak_paper_door` | `veiled_paper_door` | `celestial_paper_door` | `murublight_paper_door` | `mossy_oak_paper_door` |
| `mcwdoors:oak_stable_door` | `veiled_stable_door` | `celestial_stable_door` | `murublight_stable_door` | `mossy_oak_stable_door` |
| `mcwdoors:oak_stable_head_door` | `veiled_stable_head_door` | `celestial_stable_head_door` | `murublight_stable_head_door` | `mossy_oak_stable_head_door` |
| `mcwdoors:warped_stem_glass_door` | — | `celestial_stem_glass_door` | `murublight_stem_glass_door` | — |
| `mcwdoors:oak_swamp_door` | `veiled_swamp_door` | `celestial_swamp_door` | `murublight_swamp_door` | `mossy_oak_swamp_door` |
| `mcwdoors:oak_tropical_door` | `veiled_tropical_door` | `celestial_tropical_door` | `murublight_tropical_door` | `mossy_oak_tropical_door` |
| `mcwdoors:oak_waffle_door` | `veiled_waffle_door` | `celestial_waffle_door` | `murublight_waffle_door` | `mossy_oak_waffle_door` |
| `mcwdoors:oak_western_door` | `veiled_western_door` | `celestial_western_door` | `murublight_western_door` | `mossy_oak_western_door` |
| `mcwdoors:oak_whispering_door` | `veiled_whispering_door` | `celestial_whispering_door` | `murublight_whispering_door` | `mossy_oak_whispering_door` |

1. Open the four local sheets in `build/c30/previews/`. Each design shows its Macaw reference, the packaged AMC closed front and the AMC inventory sprite. Compare those against placed doors and inventory under the same light. Inspect the paper panels, glass reflections, Stable bars, hinges and handles. Mossy Oak should show separate moss patches while the grain and exposed fittings remain legible.
2. Place left- and right-hinged doors facing each direction. Use each half, power and unpower from beside the lower half, break either half and rejoin. Compare the Shoji sliding motion and Stable behavior with the exact reference in the table.
3. Craft all designs using their Macaw recipe grids. The Mossy Oak Bark Glass Door takes two Ribbits Mossy Oak Planks and four glass blocks and yields three. Confirm both Stable designs, including the Stable-to-Stable Horse conversion. Keep the four native door recipes available.
4. With Macaw's Doors installed and Macaw's Trapdoors absent, check 392 AMC pairs with neither wood provider, 602 with Enderscape only, 420 with Ribbits only, and 630 with both. Check the absent providers' door items and recipes are absent. With both woods present and both Macaw door providers absent, check 542 AMC pairs.
5. Record the exact C30 SHA-256, provider set, design, placement state and any visual or interaction differences. Owner gameplay and visual verification remain pending.

### Ribbits Mossy Oak

1. With Ribbits present, locate the six Mossy Oak items in Building Blocks. Compare each placed form and item preview with its BBB Oak counterpart and Ribbits Mossy Oak Planks under the same light. Check frame sticks, slat edges, wood shadows, lattice plant tints and transparent openings.
2. Join and separate neighboring forms. Place them on different faces and in water. Cycle Frame, Balustrade, Support and Pallet options with the BBB hammer. Plant and shear a Lattice, and crouch-use a Pallet. Break each form and compare the result with its Oak reference.
3. With AMC alone as the recipe provider, craft the Frame from Ribbits slabs and sticks. Use Ribbits planks in the Lattice, Trim, Support and Pallet grids. For six Balustrades, place three stripped Oak logs vertically with a moss block beside the middle log. With IBF C28 and CNM active, check the two canonical recipes and switch to the alternatives through their selectors.
4. In an environment without Ribbits, check that all six items and their recipes are absent and earlier AMC forms remain available. Record observations against the exact C32 hash.

### Calcite and Shadoline Block

1. Locate 30 Calcite items and, with Enderscape 3.0.2 enabled, 30 Shadoline Block items. Compare each family to its source block under the same light. Check the Button and Pressure Plate in Redstone Blocks and the other 28 items in Building Blocks. In a separate world without Enderscape, verify the Calcite items and recipes are available and Shadoline Block forms are absent.
2. Craft the Calcite Frame, Arrow Slit and Shutter using full Calcite in every slab slot. Compare three Calcite placed diagonally (three Shutters) with three vertically (three Columns). For Shadoline Block use native `enderscape:shadoline_block_slab` in those slots. Two diagonal Blocks of Shadoline craft one Button, and one block unpacks into nine Enderscape ingots.
3. Repeat the BBB and Macaw interaction checks below. Compare column joins, moulding corners, fence infill, frame diagonals and planted urns. Lock, open and stack the windows. Place, engrave and restore each of the four full patterns separately. Check the weighted Strewn Rocky path and rotate the Dumble paving.
4. Place each shutter beside the AMC Stone shutter. Compare the five slats, highlights, recesses and transparent openings in hand and on both hinges. Toggle a vertical stack and check it again after rejoining.

### Purpur and Dusk Purpur Buttons

1. Place an upright AMC Column beside the matching Button under the same light. Compare the Button face with the center of the Column cap. The vanilla Button samples a 6-by-4 region of the 16-by-16 tile. Compare the narrow edge bands and the inventory preview as well.
2. Place the Button on a floor, wall and ceiling, then press it in each position. Check the artwork in both states, its orientation, the usual Stone Button pulse and the matching Pressure Plate beside it.
3. Repeat for Dusk Purpur with Enderscape enabled. In the separate provider-absent world, verify the regular Purpur Button still resolves and the Dusk Purpur Button is absent.

### Purpur, Dusk Purpur and Resin

1. Locate 30 AMC items for each available family. Compare Purpur to vanilla Purpur Block, Dusk Purpur to Enderscape's Dusk Purpur Block and Resin to vanilla Resin Bricks under matching light. Compare the shutter to the five slats and transparent gaps of the AMC Stone shutter. Open, stack and reverse both hinges.
2. Craft and stonecut with Purpur Block, Dusk Purpur Block or Block of Resin. Use the corresponding Purpur slabs in slab slots and full raw Resin Blocks in Resin recipes. Verify two diagonal Resin Blocks craft one Button, one block unpacks into nine vanilla clumps, three diagonal blocks craft three Shutters and three vertically craft three Columns.
3. Repeat the control, window, masonry and paths procedures below for each family. Engrave and restore all four full patterns independently. Inspect the thin path heights, weighted Strewn Rocky placements, directional Dumble and all rotations of the window glass and bars.
4. In a separate world with Enderscape absent, check that the 60 new Purpur and Resin entries remain available and all 30 Dusk Purpur entries and recipes are absent. Record the exact artifact hash with owner observations.

### Veradite, Mirestone and Kurodite

1. With Enderscape 3.0.2 and its required providers enabled, locate 28 AMC Building Blocks items per material. Verify the polished Button and Pressure Plate remain under Enderscape. Compare all material surfaces against their unpolished Enderscape base under the same light.
2. Exercise all five BBB forms using the masonry procedure below, including connected column grain, planting, moulding corners, hammer-operated fence pillars and infill, and frame diagonals. Check Mirestone hardness and each family's placement sounds. Compare joining, glass, bars, locking, parapet supports and shutter hinges against Macaw's Windows.
3. Craft the Frame with eight native slabs around an empty center (12), the Arrow Slit with four base blocks and two slabs (3), and the Shutter with three vertical slabs (3). Stonecut each unpolished base into its eleven thin forms (4 each) and four independent full patterns (1 each). Engrave and restore each full pattern and check weighted Strewn Rocky arrangements and Dumble orientation.
4. Place a pair of each shutter beside the AMC Stone Shutter. Inspect all five slats, raised lips, recesses, open edges and transparent gaps in-world and in inventory. Toggle both hinges and stacked shutters, then save and rejoin.
5. In a separate disposable world with Enderscape and Ribbits absent, check the 392 standalone AMC pairs with Macaw's Trapdoors absent, creative inventory and recipe loading. Use separate worlds for the two provider configurations. Record owner observations against the exact C32 hash.

### Tuff, Cinnabar, Sulfur and Smooth Basalt

Compare Cinnabar forms with vanilla Cinnabar Bricks under matching light. For the other three families, use their vanilla base block. Check the Cinnabar controls and windows for the actual brick grain and the carved forms for their established patterns in the brick colors. Craft and stonecut using raw Cinnabar and the original slab ingredients. With BGE present, compare each of its four Cinnabar patterned families to its matching AMC full block in hand and in the world. Look up its 30 items under the AMC namespace. The Button and Pressure Plate belong in Redstone Blocks and the other 28 in Building Blocks. Verify the family in the intended pack with Extended Block Shapes absent.

1. Try Button and Pressure Plate placement, timing, living-entity sensitivity and support removal alongside vanilla Stone controls. Compare the seven window/detail forms with Macaw's Andesite or Stone Brick references. Check the glass-only center, barred and keyed states, Gothic and Arrow Slit stacks, and the parapet's flower-pot support.
2. Run the five BBB masonry checks below. Compare connected column grain, bevels, moulding corners, urn soil, fence infill and frame diagonals. Each base material uses the same vanilla tile on every face. Inspect the held models, transparent regions, waterlogging and matching drops.
3. Craft the Smooth Basalt Arrow Slit, Frame and Louvered Shutter with vanilla Smooth Basalt blocks in their slab slots. Three Smooth Basalt blocks diagonally craft three shutters. Three vertically craft the BBB-style Column. For Tuff, Cinnabar and Sulfur, use the matching vanilla slabs.
4. Stonecut the base block into all fifteen Paths forms. Inspect weighted Strewn Rocky arrangements, Dumble orientation, collision and edge joins. Tile each of Running Bond, Flagstone, Windmill Weave and Crystal Floor independently. Engrave and undo each with a pickaxe, then save and rejoin with different states in place.
5. Compare the Louvered Shutter with AMC's Stone shutter, side by side and in hand. Check five slats, recesses, raised lips, transparent gaps, both hinges and vertical stack toggling. Inspect the dark and light shades at normal viewing distance.

### Mossy Stone Bricks

1. Under matching light, place the new Button and Pressure Plate beside the vanilla Stone controls, the four standard windows beside Macaw's Stone versions, and the Parapet beside AMC's Stone Parapet. Look for exposed Stone grain between the moss patches. Compare Gothic Window and Arrow Slit masonry with Macaw's Stone Brick versions. Toggle stacked windows and inspect glass, bars, joins, and held models.
2. Place all five masonry forms beside BBB's corresponding Stone models. Follow the masonry interaction table below. Inspect the column seams, urn lip and soil, moulding recesses, fence pillar and frame diagonals. Moss should follow the brightness of each bevel and groove. Look for repeated bright patches crossing a recessed edge, lost highlights, or altered soil and transparent gaps.
3. Compare the new shutter with AMC's Stone shutter in all four poses and in hand. Follow a moss patch across a raised lip and into a slat recess. Check all five slats at normal viewing distance, the transparent gaps and the vertical stack toggle. Compare button timing and plate sensitivity to Stone, verify each recipe and self-drop, and record observations against the current release hash.

### End Stone Bricks and Polished Blackstone

1. Find all 26 End Stone Bricks items and the Polished Blackstone Louvered Shutter in Building Blocks. Compare held and inventory views with the placed blocks. With Extended Block Shapes installed, check the existing button and pressure plate under the `extshape` namespace. Macaw's Windows supplies `mcwwindows:end_brick_gothic` and `mcwwindows:ender_brick_arrow_slit`.
2. Compare each End Stone Bricks masonry form with its BBB Stone counterpart. Use the five checks in the masonry table below, including waterlogging, column joins, planting and soil, moulding corners, fence fills and frame diagonals. Inspect the carved surfaces and joins against vanilla End Stone Bricks.
3. Stonecut End Stone Bricks into all fifteen Paths forms. Check the eleven thin paths and pavings against their Macaw Dark Prismarine references, including weighted Strewn Rocky variants and Dumble orientation. Tile Running Bond, Flagstone, Windmill Weave and Crystal Floor separately. Engrave each with a pickaxe, save and rejoin, then restore its original pattern.
4. Place the four standard windows and the parapet beside Macaw's Andesite references and vanilla End Stone Bricks. Check window joins, the glass-only center, opening and locking where applicable, and the parapet's flower-pot support. Craft each shutter from three matching slabs in a column. Inspect both hinges and toggle vertical pairs. Under matching light, compare each shutter's five slats, raised lips, recesses and gaps with the Stone shutter and its colors with its respective vanilla block.

### Deepslate, Quartz and Nether Bricks

1. Check the six Deepslate, 21 Quartz and 23 Nether Bricks additions in inventory, in hand and after placement. Find the controls in Redstone Blocks and the other items in Building Blocks. Compare button timing, plate sensitivity, support removal, recipes and self-drops with the vanilla Stone controls. Use Block of Quartz for Quartz recipes. Deepslate shutter and Arrow Slit recipes use Deepslate Brick Slabs, following BBB's Deepslate Frame.
2. Compare Deepslate and Quartz controls, parapets, Gothic Windows and Arrow Slits to their vanilla or Macaw reference in each orientation. Check both ends and the side grain against vanilla Deepslate and Block of Quartz. Toggle stacked Gothic Windows and inspect the glass and iron bars. Place and remove a pot on each parapet. For Nether Bricks, check the four standard windows against Macaw's Andesite versions using the joining, opening and key checks below.
3. Place all three shutters beside the Stone shutter. Try each hinge, open and close two shutters stacked vertically, inspect both sides and compare the five slats, raised lips, recessed frame and gaps under the same lighting. Compare the Deepslate and Nether Bricks shutter artwork with the Stone shutter at the same orientation and light. Inspect the five-color Deepslate and seven-color Nether Bricks shading in hand and at normal viewing distance. Compare the Deepslate end caps with vanilla Deepslate's top. On Quartz compare the broad faces with the vanilla side and the narrow ends with the vanilla top.
4. Stonecut both Block of Quartz and Nether Bricks into the fifteen Paths forms listed under Prismarine below. Check each thin form against its Macaw Dark Prismarine counterpart, including Strewn Rocky variants, Dumble orientation and collision. Tile each full block independently, engrave it, save and rejoin, then undo the engraving. Compare pattern joins, relief, gaps, item models and colors under the same light.

### C17 prismarine shutter correction

Place the Stone, Prismarine Bricks and Dark Prismarine Louvered Shutters side by side, facing the same way under the same light. Compare the five slats, recesses, raised edges, grain and transparent gaps with the Stone shutter. Check both hinges, open and closed positions, a vertical pair and the held and inventory views. Note whether the eight-color Prismarine Bricks and six-color Dark Prismarine versions keep the slats distinct at ordinary viewing distance. Record the C23 hash and any affected face or pose with the owner's observations.

### Sandstone and Red Sandstone

Run these checks for both materials. Place the corresponding vanilla block alongside the test pieces under the same light so its banded sides, top and bottom are visible.

1. Find the Button and Pressure Plate in Redstone Blocks and the Parapet, Gothic Window, Arrow Slit and Louvered Shutter in Building Blocks. Inspect the held and inventory models. Place the button on the floor, wall and ceiling. Its outward cap rotates with the button. Inspect the plate from above and along its edge. Compare activation, release and support removal with the vanilla Stone controls.
2. Face each Parapet in four directions and place and remove a flower pot above it. Inspect the top, underside and edge of both the ledge and support. Compare their shape and orientation with Macaw's Andesite Parapet.
3. Compare both windows with the Macaw Stone Brick Gothic Window and Arrow Slit. Inspect the masonry cap and underside, the narrow faces of the opening, and the visible side bands. Form stacks, remove the middle block, then toggle the Gothic Window and check its glass and conditional iron bars in the reference states.
4. Face the shutter in each direction, use both hinges and toggle a vertical pair. Compare its five shaded slats with the C9 Stone shutter. Compare the broad faces, narrow edges and underside with the top face of the corresponding vanilla block. Check that the smoother grain follows the original UVs, with the five slats, raised lips and dark recesses visible in open, closed and held views.
5. Craft each form, check self-drops, and rejoin with a stacked window and an open shutter in place. Record the exact C32 hash, orientation, state and screenshots for any visual differences.

### Prismarine Bricks and Dark Prismarine

1. For each material, find the Button and Pressure Plate in Redstone Blocks and the other additions in Building Blocks. Compare the controls with vanilla Stone controls on the same supports and redstone circuit. Inspect their faces alongside vanilla Prismarine Bricks or Dark Prismarine.
2. Build the five BBB forms for each material and perform the Column, Urn, Moulding, Fence and Frame checks below. Inspect connected bands, carved edges, soil and inventory views. Compare with the matching BBB Stone forms under the same light.
3. Place left and right shutter hinges, open and close a vertical stack, and inspect the back and slat recesses. Compare the grain and structural shadows with the Stone shutter, and the colors with the corresponding vanilla block.
4. In a stonecutter, use Prismarine Bricks to obtain four of each thin path or paving, or one of each patterned full block. Compare all fifteen side by side with their Dark Prismarine counterparts from Macaw's Paths. Check inventory, held, placed and dropped appearances. Inspect these patterns:
   - Thin paths: Running Bond, Strewn Rocky, Windmill Weave, Flagstone and Crystal Floor. Place several Strewn Rocky paths to see the weighted arrangements.
   - Pavings: Diamond, Basket Weave, Square, Honeycomb, Clover and Dumble. Inspect gaps, edge thickness and side faces. Place Dumble from all four directions.
   - Full blocks: Running Bond, Flagstone, Windmill Weave and Crystal Floor. Tile a floor and a wall, inspect the joins, then use a pickaxe twice on each. Check the engraved face, outline, collision and restored state beside the provider block. Save and rejoin with one block engraved.
5. Check recipes, matching slab ingredients and self-drops for both new families. Confirm the existing Macaw windows, parapets and Dark Prismarine paths still carry the provider namespace. Repeat the C14 and earlier checks below as needed.

### Mud Bricks family

1. Find the Button and Pressure Plate in Redstone Blocks and the other eleven items in Building Blocks. Compare each item in inventory, in hand and after placement. Place vanilla Packed Mud beside the controls, four windows and parapet under the same lighting to compare the grain and color.
2. Try the Button on a floor, wall and ceiling. Compare its 20-tick pulse and the Pressure Plate's response to living entities and dropped items with vanilla Stone controls.
3. Build a three-by-three Window panel alongside Macaw's Andesite Window. Check the glass-only center, edges and corners. For Window 2, Four Window and Pane Window, compare both opening directions and locking with Macaw's key. Stack Pane Windows and remove the middle one. Place and remove a flower pot on the Parapet to check its support.
4. Place both shutter hinges and open and close a pair stacked vertically. Inspect the slats and recesses beside the Stone Louvered Shutter. Check that each slat retains the Packed Mud grain at its original position. Compare the outline and held model with Macaw's Andesite shutter.
5. Run the five BBB checks in the table below for Mud Bricks. Compare the carved column bands, urn rim, dentils, filled fence and frame diagonals against BBB Stone at the same angle. Check the dirt surface in a soiled urn. Craft with Packed Mud for full blocks, use Mud Brick Slabs for the shutter and frame, and check the four BBB stonecutting outputs from Packed Mud.

### Existing families

Compare the Granite Column, Urn, Moulding, Fence and Frame with C12 captures at the same distance and lighting. Check whether the darkest and lightest speckles are reduced while bevels, recesses and connected column bands remain distinct. Inspect the soiled Urn for its dirt surface. Use a Granite Gothic Window and adjacent vanilla Granite as controls for the original full-palette texture.

Run the following checks once with Diorite and once with Granite. For each BBB form, place its Andesite and `bbb:stone_<form>` counterparts beside it under the same lighting. Check the inventory icon, both hands and a dropped item. After placement, inspect the faces from several angles and compare the highlights and shadows to the reference. Compare the material colors to the corresponding vanilla Diorite or Granite block. Check the joins between column layers and the dirt in the soiled urn.

| Form | Checks |
| --- | --- |
| Column | Place on all three axes. Join and split columns, compare end caps and middle sections, then strike each quarter with the BBB hammer. Waterlog a carved column and restore the last layer. |
| Urn | Add dirt, plant a flower, then remove the flower and shovel out the dirt. Compare the rim, inside and separate soil surface. Mine an empty and a soiled urn to check the extra dirt drop. |
| Moulding | Place upper and lower halves in all four directions. Make left and right inner and outer corners. Toggle the dentil with the hammer, checking the teeth and undersides in each shape. |
| Fence | Connect to a BBB stone fence, a wall and a gate. Form a corner and a straight run. Hammer the side fill, sneak-hammer the pillar on a straight run, then remove neighbors. |
| Frame | Join frames beside and above each other. Check edges and corners as neighbors change. Cycle both diagonals and both straight centers with the hammer, then attack to reset. Compare the held-frame pick outline. |

Find all five in Building Blocks. Compare their shaped recipes to BBB using the chosen stone, with matching slabs for the frame. Check crafting yields of 3 columns, 2 urns, 8 mouldings, 5 fences and 12 frames. The stonecutter should offer one column, urn, moulding or fence per stone block. Waterlog each applicable form, save and rejoin, and verify its connections and fluid state.

### Controls and Macaw forms

1. For each material, find its Button and Pressure Plate in Redstone Blocks and its Gothic Window and Arrow Slit in Building Blocks. Check each in inventory, in hand and after placement. Compare the masonry grain to the matching vanilla stone under the same lighting.
2. Craft one button from one stone block and one plate from two matching blocks side by side. Compare the button to a Stone Button on a floor, wall and ceiling. Use each to power a redstone lamp, compare the 20-tick pulse, try an arrow, and remove its support. Compare the plate to a Stone Pressure Plate with a player, a mob and a dropped item. Check activation and release after the living entity steps off.
3. Craft four Gothic Windows from a glass pane with one matching stone block on each cardinal side. Craft three Arrow Slits in two columns with two matching stone blocks on each of the top and bottom rows and two matching slabs in the middle. Mine all four blocks with a pickaxe and check each drops one matching item.
4. Place each masonry form beside its Macaw Stone Brick reference in all four directions. Compare single blocks and columns of two and three. Inspect the frame, opening, face alignment and pick outline from both sides. Remove the middle block and check the upper and lower blocks return to the single model. On Gothic Windows, toggle a connected stack and cycle the part with the Macaw hammer. Compare the glass and conditional iron bars in the corresponding reference states. Save and rejoin to check state persistence.

### C11 regression checks

Repeat the nine forms above with Andesite. Confirm the Diorite and Granite parapets and louvered shutters still use their Macaw IDs. Compare Stone Parapet and Stone Louvered Shutter against C9 at the same angle and light. Check the parapet's flower-pot support, the shutter's two hinges, open/closed geometry, vertical toggling and the selected 75% shadow depth. Craft five parapets from three sticks across the top and stick, Stone, stick below. Craft three shutters from three Stone Slabs in one column.

Record the exact artifact hash, observations and any failing state, orientation or interaction. Include a screenshot for rendering issues. Owner verification and acceptance of C30 are pending.

## Controlled build checks

From the AMC directory, use Java 25, Gradle 9.5.1 and Python with Pillow. The default controlled run excludes Extended Block Shapes. Use `-PextshapeReference=<jar>` only to reproduce the historical C21 ownership audit with its reference archive. The generator checks the read-only Macaw provider JARs in `originals/mods`, the retained BBB C9 JAR in the sibling BBB project's `artifacts/` directory, and the official 26.2 client in Loom's cache. The Prismarine Bricks, Dark Prismarine, Deepslate, Nether Bricks, End Stone Bricks, Polished Blackstone and four C23 shutters use the Stone PNG extracted from the hash-verified `artifacts/AMC C9.jar`. Use `-PstoneShutterJar=<path>` for another copy of that artifact. Set `-PvanillaClient=<path>` for another copy of that exact client and `-PbbbJar=<path>` for another copy of the verified BBB reference.

```text
gradle build runGameTest --rerun-tasks -Ppython=<interpreter>
```

`verifyAmcJar` checks the production file list, translations, recipes, loot, tags, inventory models, exact reference state choices and model parents, sprite bindings, and the C9 shutter PNG. For each material’s five BBB forms, it compares every state branch and follows each placed and item model through its parents. It checks all model fields, resolves face slots, verifies each sheet pixel against its ranked palette and compares recipes, drops and direct tag membership to the provider. Controlled server GameTests exercise each material independently and compare creative tabs, registered entries, loaded data, block states, collision, redstone behavior, placement and masonry interactions. For C15 the archive check follows every Paths blockstate and item model, including weighted choices and engraved models, and compares every texel across eleven source sheets. For C16 the archive check compares every material face and reference element for both sandstone families, including cap sprite selection, UVs, culling, model parents and item transforms. C19 repeats the complete Paths graph and pixel checks independently for Quartz and Nether Bricks. The C20 shutter check compares each opaque pixel with the proportional rank of its C9 source gray in the full vanilla palette, checks that every target color appears and compares all transparent pixels in RGBA. For C22 the pixel check independently derives vanilla's five moss greens and checks all thirteen derived sprites, including unmodified pixels and the brightness of moss at every affected coordinate. The C23 package check compares against the hash-verified retained C22 JAR, including every previous production class and resource. Set `-Pc22Jar=<path>` to select another copy of that exact artifact. All generated resources, the isolated GameTest world and the retained JAR are local-only.


Run the Java 25 suite a second time with `-PwithEnderscape`. Supply Enderscape 3.0.2 under `originals/mods` and its supporting JARs under `build/c28/test-dependencies`, or set `-PenderscapeTestDeps=<directory>`. The controlled run uses Lithostitched 1.8.0, Trim Patcher 2.1 and YACL 3.9.7.

The C26 checker verifies the exact retained C25 artifact and its historical reference chain. It compares all new resource graphs to the finalized Tuff family and checks the derived textures against each provider's artwork pixel by pixel. It verifies all prior entries, additive tag and translation memberships, version metadata and the two registration classes. Set `-Pc25Jar=<path>` for another copy of the exact C25 artifact. Controlled GameTests exercise both registry sets, all 45 new crafting grids, the vanilla Resin Clump recipe, stonecutting, drops, engraving and provider interactions.

The C27 checker compares all entries to the hash-verified C26 artifact (`-Pc26Jar=<path>` selects another copy). It traces each Column's exposed up face through the upright multipart selections and checks the sprite, orientation and UVs. It resolves all 24 placed states and the inventory model per Button against the vanilla parents, and checks every sampled region against its C26 Column cap.

The C28 package check compares all entries to the verified retained C27 JAR. Use `-Pc27Jar=<path>` for an alternate copy of those bytes. It verifies the two families against the C27 Tuff resource graphs, the actual Calcite and Shadoline Block sprites, the provider sheets and the C9 Stone shutter. Run the controlled suites both with and without Enderscape to check registry coverage, creative entries, recipe resolution and interactions.

The C29 package check compares every retained C28 entry, the six original BBB Oak graphs and all 15 generated wood sprites. The real Ribbits C27 reference is required for generation even when omitted at runtime. For the provider-present suite use `build runGameTest -PwithRibbits -PwithEnderscape`, setting `ribbitsJar`, semicolon-separated `ribbitsTestDeps`, `enderscapeTestDeps` and the Matcha 1.12 fixture `matchaFoodResources` where necessary. Repeat without the provider switches to exercise the standalone registry.

The C30 checker revalidates C29, compares the retained payloads, follows each door graph into Macaw's actual model parents, verifies wood and protected pixels, and generates all four local preview sheets. Set `-PdoorsJar=<path>` and `-Pc29Jar=<path>` to select the verified inputs. Run Java 25 `build runGameTest` in the four Enderscape/Ribbits combinations, using the C29 provider switches and fixtures described above. Add `-PwithoutDoors` with both wood providers for the optional Macaw-absent regression.
