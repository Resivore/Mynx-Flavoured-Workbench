# Testing Dramatic Doors C12

C12 targets Minecraft Java 26.2 Fabric and awaits owner gameplay verification. Match the JAR hash to the current entry in WORKBENCH_STATUS.json. C8 is the accepted rollback.

## AMC tall Macaw doors

The [88-row audit](fabric/src/main/resources/dramaticdoors/amc_macaw_doors.tsv) names each exact AMC ingredient, DD output, tall reference or source geometry, and block class. Each material has 22 designs. Enderscape supplies Veiled, Celestial and Murublight. Ribbits supplies Mossy Oak. Macaw’s Doors and Architectural Material Closure must both be loaded for these derivatives. AMC C30 defines the membership. C31 adds an independent trapdoor family.

Stable and Stable Head use the DD stable class. Japanese and Japanese2 use the DD Macaw sliding class. The other 72 use the ordinary tall class. Bark Glass uses the actual Veiled or Mossy Oak log and vanilla glass. Stem Glass uses the Celestial or Murublight stem. Classic uses the Spruce tall Macaw template. For Whispering, the two Macaw halves match all 512 vanilla Pale Oak texel positions under a one-to-one palette change. C12 applies that mapping to DD’s tall Pale Oak layout. Western retains AMC’s stepped upper leaf, lower leaf, pins and handed open models. Its new middle joins the original two-texel-thick edge and continues the slats.

### Controlled verification

Use Java 25, Pillow, the Gradle wrapper and local reference artifacts. Supply `-PdoorArtPython=<Python with Pillow>` for Gradle tasks. Run `:fabric:test :fabric:build` and `tools/Get-RegisteredItemIds.ps1 -ValidateDefinitions`. Regenerate the four local comparison sheets from the candidate with the following positional arguments:

```text
python -B tools/verify_amc_macaw.py fabric/src/main/resources/dramaticdoors/amc_macaw_doors.tsv <DD-C11> <AMC-C30> <Macaw-Doors-1.1.5> <Minecraft-26.2-client> <Enderscape-3.0.2> <Ribbits-C27> <DD-C12> build/c12/previews
```

Each sheet shows the tall structural reference, actual AMC two-block model, C12 tall face and C12 inventory sprite. Western’s first column shows the original Macaw two-block geometry. The verifier checks 88 names in each of English, Russian and Simplified Chinese, resolves the model and texture dependencies for 4,224 placed states and 88 inventory items, checks the wood ramps, protected pixels and Western seam geometry, and compares every historical C11 archive payload. The artwork and preview sheets are local build outputs.

Use `:fabric:runGameTest` with the existing `doorTestMatrix`, `doorTestJars` and `doorTestData` properties described below. Add AMC, Macaw’s Doors and AMC’s required Macaw’s Windows, Paths and Pavings, and Building But Better inputs per row. The server tests compare class, source properties, special collision shapes, Macaw creative-tab membership, exact recipe input and yield, recipe reward, two data reloads, actual wet placement and hand interaction, redstone, serialized state, and a single survival drop from each third. For the combined client, add `-PdoorTestJei=<JEI JAR>` and run `:fabric:runClientGameTest`.

| AMC | Macaw’s Doors | Wood providers | Expected new tall doors |
| --- | --- | --- | ---: |
| Absent | Present | Both | 0 |
| C30 | Absent | Both | 0 |
| C30 | Present | Neither | 0 |
| C30 | Present | Enderscape | 66 |
| C30 | Present | Ribbits | 22 |
| C30 or C31 | Present | Both | 88 |

The controlled server returned the counts above before and after both reloads. C31’s two door registration classes and 1,130 door-pack files are byte-identical to C30. The combined C30 client found all 88 tall items and all ten native door items in Creative Search and JEI 30.18.0.144.

### Owner visual and gameplay checks

1. Compare the locally generated sheets for all four woods against the installed doors under your usual lighting and resource packs. Inspect front and back, both hinges and all facings, closed and open, and view the upper/middle and middle/lower joints at eye level.
2. Check glass reflections, Shoji paper and grid spacing, stable ironwork and pulls. Compare Mossy Oak wood and the placement of its moss with the AMC source. Inspect Western’s stepped crown, hinge pins and uninterrupted slatted center, and the straps on Whispering.
3. Search each material and design in Creative and JEI. Compare hotbar and held-item silhouettes with existing tall Macaw doors. Craft three corresponding AMC doors vertically for two tall doors, then verify the unlock after reload and reconnect.
4. Operate Stable, Stable Head and both Japanese designs from both sides. Walk through open leaves, test collision and apply power beside each third. Repeat wet and dry, break each third in survival, then check drops and water restoration. Save, unload the chunks and restart to check existing and new placements.

## Expected native doors

Both output paths in each row belong to `dramaticdoors`. All five materials have a one-block short door and a three-block tall door.

| Source block and recipe ingredient | Short path | Tall path | English material name |
| --- | --- | --- | --- |
| `minecraft:pale_oak_door` | `short_pale_oak_door` | `tall_pale_oak_door` | Pale Oak |
| `enderscape:veiled_door` | `short_veiled_door` | `tall_veiled_door` | Veiled |
| `enderscape:celestial_door` | `short_celestial_door` | `tall_celestial_door` | Celestial |
| `enderscape:murublight_door` | `short_murushroom_door` | `tall_murushroom_door` | Murublight |
| `ribbits:mossy_oak_door` | `short_mossy_oak_door` | `tall_mossy_oak_door` | Mossy Oak |

## Controlled checks

The C11 native pair workflow composes eight block tiles and four flat item sprites from the exact local Enderscape 3.0.2 and Ribbits C27 inputs. The build checks the SHA-256 of all six source textures. To supply the same inputs from another path, set `-PveiledArtJar` and `-PmossyOakArtJar`. Keep the generated pixels and previews local.

For the C11-to-C10 historical asset comparison, run `python tools/verify_native_assets.py <C11.jar> <minecraft-merged-26.2.jar> <enderscape.jar> <ribbits.jar> <C10.jar>`. It resolves all five native pairs, compares both affected blockstate graphs with Celestial for every facing, hinge, open state and third, and checks the exact C10-to-C11 archive differences. Run `tools/Get-RegisteredItemIds.ps1 -ValidateDefinitions` after building.

With Pillow available, run `python tools/preview_native_doors.py <candidate> <C10.jar> <enderscape.jar> <ribbits.jar> build/c11-art/door-comparison.png`. The local sheet shows the source faces, C10 closed-face UV projections, C11 block and inventory art, and existing Celestial variants. The script checks source landmarks at native resolution, the joins of the assembled tall faces, window transparency, and the short and tall inventory silhouettes.

Run `:fabric:runGameTest` four times with `-PdoorTestMatrix=vanilla`, `enderscape`, `ribbits` and `combined`. For provider runs, pass the corresponding local JARs and their dependencies in `-PdoorTestJars`, separated by the platform path separator. Enderscape uses Lithostitched, Trim Patcher and YACL. Ribbits uses YACL, YUNG’s API compat.2, Trinkets Updated, GeckoLib 5.5.1, Cloth Config, Custom Portals and Matcha Heart. For Ribbits, set `-PdoorTestData` to the Matcha Flavoured 1.12 resource root for its trade contracts. The test mod loads only the necessary data categories. The four configurations use separate directories under the Fabric module’s build tree.

Each run checks exact item/block presence, search-tab routing, source material, recipe matches and outputs, advancement rewards and loot before and after two server data reloads. The placement test exercises real item placement, hand interaction, redstone, waterlogging, state serialization, and survival breaking of each tall third. The JUnit cutting-route check exercises Woodworks, Aurora’s Decorations, their combination and Universal Sawmill alone. Only supported cutting serializers receive generated unlocks.

For the controlled client search test, use the combined inputs and `-PdoorTestJei=<JEI 26.2 Fabric JAR>` with `:fabric:runClientGameTest`. It creates a disposable GameTest world and checks that each exact item appears in the Creative Search and JEI results for its English name, and that every registered state and item has a baked model.

The C11 server runs used Fabric Loader 0.19.3 and Fabric API 0.157.0+26.2. Both tests passed in every row. The full sets of recipe and advancement IDs matched before and after both reloads, with the following counts.

| Providers loaded | Recipes | Advancements |
| --- | ---: | ---: |
| Neither | 1,635 | 1,750 |
| Enderscape 3.0.2 | 2,008 | 2,138 |
| Ribbits C27 and its trade inputs | 2,305 | 1,761 |
| Both providers and their dependencies | 2,678 | 2,149 |

The C11 combined client GameTest passed with JEI 30.18.0.144. It found all ten items by their English names in both search indexes and verified their baked item and block models.

## Owner gameplay checks

1. Test the exact candidate with the four provider configurations above. Search both heights by English name in Creative Search and JEI. In each provider-absent run, check for absent items, recipes and missing-provider errors. With both providers installed, confirm ten distinct items. With either legacy Pale Garden mod available, confirm only one native Pale Oak pair.
2. Obtain the exact source door. Confirm one input yields two short doors in the stonecutter and three inputs in one vertical column yield two tall doors. Check each recipe-book unlock, reload data twice, then reconnect. With a supported sawmill installed, repeat in its cutting station.
3. Place each pair with both hinges and in all four directions. Compare opening, closing and footstep sounds with the source. Inspect closed and open silhouettes, collision and pathfinding. Use the local comparison sheet alongside original Veiled, Mossy Oak and the Celestial variants. Inspect both faces and both sides of each tall join under game lighting. The short Veiled grille and the tall Veiled grille should match the native diamond proportions. Check the two recessed waist panels and carved lower panel on the tall door. On both Mossy Oak doors, look for a round four-light window, a single brass latch and moss on the framing. The tall plank grooves should cross the middle-to-lower join cleanly. Inspect the flat icons in inventory, hotbar, Creative Search, JEI and both first- and third-person hands.
4. Open by hand and apply and remove redstone beside each tall third. Check synchronized facing, hinge, power and open state. Repeat in water. Break each section in survival and confirm one item, clean removal of every segment and appropriate fluid restoration. Check creative-mode cleanup separately. Compare Mossy Oak fire behavior with its source.
5. Save with a mix of wet, dry, open and closed doors. Unload and reload the chunks, restart and check the same positions. Include an existing world with Celestial and Murushroom registry IDs.
6. Sample existing vanilla, copper and Macaw doors. Check weathering, waxing, axe interaction, representative stable/sliding doors, fence gates and mob interaction. Check logs for missing assets, recipe or advancement decode errors and mixin failures. Record the exact JAR, provider set and observations for each result.
