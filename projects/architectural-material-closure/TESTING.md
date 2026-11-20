# Architectural Material Closure verification

## Owner verification for Canary 13

Match the SHA-256 of `AMC C13.jar` to the current release in `WORKBENCH_STATUS.json`. Use Minecraft Java 26.2 Fabric with Fabric API, Macaw's Windows and Building But Better. The controlled reference versions were Macaw 2.4.2 and BBB C9 (2.0pre4+26.2-enderscape-dev.7). All 29 AMC IDs use the namespace `architectural_material_closure`.

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

Record the exact artifact hash, observations and any failing state, orientation or interaction. Include a screenshot for rendering issues. Owner verification and acceptance of C13 are pending.

## Controlled build checks

From the AMC directory, use Java 25, Gradle 9.5.1 and Python with Pillow. The generator checks the read-only Macaw provider JAR in `originals/mods`, the retained BBB C9 JAR in the sibling BBB project's `artifacts/` directory, and the official 26.2 client in Loom's cache. Set `-PvanillaClient=<path>` for another copy of that exact client and `-PbbbJar=<path>` for another copy of the verified BBB reference.

```text
gradle build runGameTest --rerun-tasks -Ppython=<interpreter>
```

`verifyAmcJar` checks the production file list, translations, recipes, loot, tags, inventory models, exact reference state choices and model parents, sprite bindings, and the C9 shutter PNG. For each material’s five BBB forms, it compares every state branch and follows each placed and item model through its parents. It checks all model fields, resolves face slots, verifies each sheet pixel against its ranked palette and compares recipes, drops and direct tag membership to the provider. Controlled server GameTests exercise each material independently and compare creative tabs, registered entries, loaded data, block states, collision, redstone behavior, placement and masonry interactions. The generated resources, isolated GameTest world and retained JAR are local-only.
