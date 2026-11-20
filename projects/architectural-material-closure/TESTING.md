# Architectural Material Closure verification

## Owner verification for Canary 10

Match the SHA-256 of `AMC C10.jar` to the current release in `WORKBENCH_STATUS.json`. Use Minecraft Java 26.2 Fabric with Fabric API and Macaw's Windows 2.4.2. All six AMC IDs use the namespace `architectural_material_closure`.

1. Find Andesite Button and Andesite Pressure Plate in Redstone Blocks, and Andesite Gothic Window and Andesite Arrow Slit in Building Blocks. Check each in inventory, in hand and after placement. Compare the masonry grain to adjacent vanilla Andesite under the same lighting.
2. Craft one button from one Andesite and one plate from two Andesite side by side. Compare the button to a Stone Button on a floor, wall and ceiling. Use each to power a redstone lamp, compare the 20-tick pulse, try an arrow, and remove its support. Compare the plate to a Stone Pressure Plate with a player, a mob and a dropped item. Check activation and release after the living entity steps off.
3. Craft four Gothic Windows from a glass pane with one Andesite on each cardinal side. Craft three Arrow Slits in two columns with two Andesite on each of the top and bottom rows and two Andesite Slabs in the middle. Mine all four new blocks with a pickaxe and check each drops one matching item.
4. Place each masonry form beside its Macaw Stone Brick reference in all four directions. Compare single blocks and columns of two and three. Inspect the frame, opening, face alignment and pick outline from both sides. Remove the middle block and check the upper and lower blocks return to the single model. On Gothic Windows, toggle a connected stack and cycle the part with the Macaw hammer. Compare the glass and conditional iron bars in the corresponding reference states. Save and rejoin to check state persistence.
5. Compare Stone Parapet and Stone Louvered Shutter against C9 at the same angle and light. Check the parapet's flower-pot support, the shutter's two hinges, open/closed geometry, vertical toggling and the selected 75% shadow depth. Craft five parapets from three sticks across the top and stick, Stone, stick below. Craft three shutters from three Stone Slabs in one column.

Record the exact artifact hash, observations and any failing state, orientation or interaction. Include a screenshot for rendering issues. Owner verification and acceptance of C10 are pending.

## Controlled build checks

From the AMC directory, use Java 25, Gradle 9.5.1 and Python with Pillow. The generator checks the read-only Macaw provider JAR in `originals/mods` and the official 26.2 client in Loom's cache. Set `-PvanillaClient=<path>` for another copy of that exact client.

```text
gradle build runGameTest --rerun-tasks -Ppython=<interpreter>
```

`verifyAmcJar` checks the production file list, translations, recipes, loot, tags, inventory models, exact reference state choices and model parents, sprite bindings, and the C9 shutter PNG. Controlled server GameTests compare registered entries, loaded data, block states, collision, redstone behavior, placement and masonry interactions. The generated resources, isolated GameTest world and retained JAR are local-only.
