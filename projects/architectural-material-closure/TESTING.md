# Architectural Material Closure verification

## Owner verification for Canary 23

Match the SHA-256 of `AMC C23.jar` to the current release in `WORKBENCH_STATUS.json`. Use Minecraft Java 26.2 Fabric with Fabric API, Macaw's Windows, Macaw's Paths and Pavings, and Building But Better. The controlled reference versions were Windows 2.4.2, Paths 1.1.1 and BBB C9 (2.0pre4+26.2-enderscape-dev.7). All 297 AMC IDs use the namespace `architectural_material_closure`.

### Tuff, Cinnabar, Sulfur and Smooth Basalt

For each family use the exact vanilla base block for side-by-side comparison under matching light. Look up its 30 items under the AMC namespace. The Button and Pressure Plate belong in Redstone Blocks and the other 28 in Building Blocks. Verify the family in the intended pack with Extended Block Shapes absent.

1. Try Button and Pressure Plate placement, timing, living-entity sensitivity and support removal alongside vanilla Stone controls. Compare the seven window/detail forms with Macaw's Andesite or Stone Brick references. Check the glass-only center, barred and keyed states, Gothic and Arrow Slit stacks, and the parapet's flower-pot support.
2. Run the five BBB masonry checks below. Compare connected column grain, bevels, moulding corners, urn soil, fence infill and frame diagonals. Each base material uses the same vanilla tile on every face. Inspect the held models, transparent regions, waterlogging and matching drops.
3. Craft the Smooth Basalt Arrow Slit, Frame and Louvered Shutter with vanilla Smooth Basalt blocks in their slab slots. Three Smooth Basalt blocks diagonally craft three shutters. Three vertically craft the BBB-style Column. For Tuff, Cinnabar and Sulfur, use the matching vanilla slabs.
4. Stonecut the base block into all fifteen Paths forms. Inspect weighted Strewn Rocky arrangements, Dumble orientation, collision and edge joins. Tile each of Running Bond, Flagstone, Windmill Weave and Crystal Floor independently. Engrave and undo each with a pickaxe, then save and rejoin with different states in place.
5. Compare the Louvered Shutter with AMC's Stone shutter, side by side and in hand. Check five slats, recesses, raised lips, transparent gaps, both hinges and vertical stack toggling. Inspect the dark and light shades at normal viewing distance.

### Mossy Stone Bricks

1. Under matching light, place the new Button and Pressure Plate beside the vanilla Stone controls, the four standard windows beside Macaw's Stone versions, and the Parapet beside AMC's Stone Parapet. Look for exposed Stone grain between the moss patches. Compare Gothic Window and Arrow Slit masonry with Macaw's Stone Brick versions. Toggle stacked windows and inspect glass, bars, joins, and held models.
2. Place all five masonry forms beside BBB's corresponding Stone models. Follow the masonry interaction table below. Inspect the column seams, urn lip and soil, moulding recesses, fence pillar and frame diagonals. Moss should follow the brightness of each bevel and groove. Look for repeated bright patches crossing a recessed edge, lost highlights, or altered soil and transparent gaps.
3. Compare the new shutter with AMC's Stone shutter in all four poses and in hand. Follow a moss patch across a raised lip and into a slat recess. Check all five slats at normal viewing distance, the transparent gaps and the vertical stack toggle. Compare button timing and plate sensitivity to Stone, verify each recipe and self-drop, and record observations against the C23 hash.

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
5. Craft each form, check self-drops, and rejoin with a stacked window and an open shutter in place. Record the exact C23 hash, orientation, state and screenshots for any visual differences.

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

Record the exact artifact hash, observations and any failing state, orientation or interaction. Include a screenshot for rendering issues. Owner verification and acceptance of C23 are pending.

## Controlled build checks

From the AMC directory, use Java 25, Gradle 9.5.1 and Python with Pillow. The default controlled run excludes Extended Block Shapes. Use `-PextshapeReference=<jar>` only to reproduce the historical C21 ownership audit with its reference archive. The generator checks the read-only Macaw provider JARs in `originals/mods`, the retained BBB C9 JAR in the sibling BBB project's `artifacts/` directory, and the official 26.2 client in Loom's cache. The Prismarine Bricks, Dark Prismarine, Deepslate, Nether Bricks, End Stone Bricks, Polished Blackstone and four C23 shutters use the Stone PNG extracted from the hash-verified `artifacts/AMC C9.jar`. Use `-PstoneShutterJar=<path>` for another copy of that artifact. Set `-PvanillaClient=<path>` for another copy of that exact client and `-PbbbJar=<path>` for another copy of the verified BBB reference.

```text
gradle build runGameTest --rerun-tasks -Ppython=<interpreter>
```

`verifyAmcJar` checks the production file list, translations, recipes, loot, tags, inventory models, exact reference state choices and model parents, sprite bindings, and the C9 shutter PNG. For each material’s five BBB forms, it compares every state branch and follows each placed and item model through its parents. It checks all model fields, resolves face slots, verifies each sheet pixel against its ranked palette and compares recipes, drops and direct tag membership to the provider. Controlled server GameTests exercise each material independently and compare creative tabs, registered entries, loaded data, block states, collision, redstone behavior, placement and masonry interactions. For C15 the archive check follows every Paths blockstate and item model, including weighted choices and engraved models, and compares every texel across eleven source sheets. For C16 the archive check compares every material face and reference element for both sandstone families, including cap sprite selection, UVs, culling, model parents and item transforms. C19 repeats the complete Paths graph and pixel checks independently for Quartz and Nether Bricks. The C20 shutter check compares each opaque pixel with the proportional rank of its C9 source gray in the full vanilla palette, checks that every target color appears and compares all transparent pixels in RGBA. For C22 the pixel check independently derives vanilla's five moss greens and checks all thirteen derived sprites, including unmodified pixels and the brightness of moss at every affected coordinate. The C23 package check compares against the hash-verified retained C22 JAR, including every previous production class and resource. Set `-Pc22Jar=<path>` to select another copy of that exact artifact. All generated resources, the isolated GameTest world and the retained JAR are local-only.
