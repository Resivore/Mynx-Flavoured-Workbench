# Architectural Material Closure verification

## Owner verification for Canary 6

Use the exact retained `AMC C6.jar` and compare its SHA-256 with the current release in `WORKBENCH_STATUS.json`. The reference pair is Macaw's Windows 2.4.2 Andesite Parapet and Andesite Louvered Shutter on Minecraft Java 26.2 Fabric.

1. Find Stone Parapet and Stone Louvered Shutter in Building Blocks. Confirm their inventory and held models render. Both item IDs use the namespace `architectural_material_closure`.
2. Craft five parapets using three sticks across the first row and stick, Stone, stick across the second. Craft three shutters from three Stone Slabs in one column. Mine each with a pickaxe and check for one matching block item.
3. Place a Stone parapet beside an andesite parapet in each horizontal orientation. Compare the outline, ledge and face alignment. Add and remove a flower pot above each and compare the support. The AMC ledge and support should both show vanilla Stone.
4. Place shutters on both hinge sides in each orientation. Compare them with the andesite equivalents while open and closed. Look for distinct shaded slats and clear openings from either side, including in the inventory model. Check for missing textures on the panel and thin edges.
5. Open and close a vertical pair of Stone shutters together. Try using one while holding its own item, then with an empty hand. Compare with a Macaw pair and confirm an adjacent andesite shutter remains independent. Save and rejoin to check placement and state persistence.

Record the exact artifact hash, observed results, and any failing orientation, hinge, or pot state. Include a screenshot for a rendering issue. Owner runtime evidence is pending.

## Controlled build checks

From the AMC directory, use Java 25, Gradle 9.5.1 and a Python interpreter with Pillow. Keep `mcw-windows-2.4.2-mc26.2fabric.jar` in the repository's read-only `originals/mods`. The generator checks that JAR's SHA-256 and the official 26.2 client SHA-1 before deriving assets. Gradle uses Loom's cached client by default. Set `-PvanillaClient=<path>` if the official client JAR is elsewhere.

```text
gradle build runGameTest -Ppython=<interpreter>
```

`verifyStoneJar` checks the production archive, all state and item model bindings, inherited face UVs, sprite closure, recipes, drops, tags, pixel mapping and slat contrast. Three isolated Fabric GameTests check registrations, loaded data, state shapes, placement, flower-pot updates and shutter interaction. Generated resources, GameTest worlds and the production JAR belong under ignored local output. Retain the verified JAR locally and keep it out of distributions.
