# Testing Dramatic Doors C11

C11 targets Minecraft Java 26.2 on Fabric. The candidate is ACTIVE and awaits owner gameplay testing. The manifest identifies the exact candidate and accepted C8 rollback.

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

Use Java 25 and the project Gradle wrapper. Run `:fabric:test :fabric:build`. The build composes eight block tiles and four flat item sprites from the exact local Enderscape 3.0.2 and Ribbits C27 inputs. The build checks the SHA-256 of all six source textures. To supply the same inputs from another path, set `-PveiledArtJar` and `-PmossyOakArtJar`. Keep the generated pixels and previews local.

Run `python tools/verify_native_assets.py <candidate> <minecraft-merged-26.2.jar> <enderscape.jar> <ribbits.jar> <C10.jar>`. It resolves all five native pairs, compares both affected blockstate graphs with Celestial for every facing, hinge, open state and third, and checks the exact C10-to-C11 archive differences. Run `tools/Get-RegisteredItemIds.ps1 -ValidateDefinitions` after building.

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

The combined client GameTest passed with JEI 30.18.0.144. It found all ten items by their English names in both search indexes and verified their baked item and block models.

## Owner gameplay checks

1. Test the exact candidate with the four provider configurations above. Search both heights by English name in Creative Search and JEI. In each provider-absent run, check for absent items, recipes and missing-provider errors. With both providers installed, confirm ten distinct items. With either legacy Pale Garden mod available, confirm only one native Pale Oak pair.
2. Obtain the exact source door. Confirm one input yields two short doors in the stonecutter and three inputs in one vertical column yield two tall doors. Check each recipe-book unlock, reload data twice, then reconnect. With a supported sawmill installed, repeat in its cutting station.
3. Place each pair with both hinges and in all four directions. Compare opening, closing and footstep sounds with the source. Inspect closed and open silhouettes, collision and pathfinding. Use the local comparison sheet alongside original Veiled, Mossy Oak and the Celestial variants. Inspect both faces and both sides of each tall join under game lighting. The short Veiled grille and the tall Veiled grille should match the native diamond proportions. Check the two recessed waist panels and carved lower panel on the tall door. On both Mossy Oak doors, look for a round four-light window, a single brass latch and moss on the framing. The tall plank grooves should cross the middle-to-lower join cleanly. Inspect the flat icons in inventory, hotbar, Creative Search, JEI and both first- and third-person hands.
4. Open by hand and apply and remove redstone beside each tall third. Check synchronized facing, hinge, power and open state. Repeat in water. Break each section in survival and confirm one item, clean removal of every segment and appropriate fluid restoration. Check creative-mode cleanup separately. Compare Mossy Oak fire behavior with its source.
5. Save with a mix of wet, dry, open and closed doors. Unload and reload the chunks, restart and check the same positions. Include an existing world with Celestial and Murushroom registry IDs.
6. Sample existing vanilla, copper and Macaw doors. Check weathering, waxing, axe interaction, representative stable/sliding doors, fence gates and mob interaction. Check logs for missing assets, recipe or advancement decode errors and mixin failures. Record the exact JAR, provider set and observations for each result.
