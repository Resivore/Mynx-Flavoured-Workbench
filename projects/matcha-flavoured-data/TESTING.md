# Testing

## Matcha 1.12.5, Canary 3

`matcha-flavoured-data-0.2.1-canary3.jar`

SHA-256 `a659433747497cb7ddb44267d489edd22949aadae92a9bea701d99785d83fa65`

Source `1ce48a4c2b288c0a18d71e625531e56b618b7ae7`

### Static checks

Build the project for Minecraft 26.2 with Java 25. The project suite covers 2 tests. After building the eleven coordinated mods, run the following from the repository root with paths to the verified publisher downloads and retained Foundation ZIP.

```text
python -B tests/verify_matcha_migration.py --official-mod "matcha-flavoured-1.12.5+dp.jar" --client-pack "MF_RP_1_12_3_26_2.zip" --foundation "Foundation v13.zip"
```

### Runtime checks

Record observations against the exact filename and SHA-256 above in a user-directed disposable world. Verify startup, a repeated reload and a reconnect before reporting runtime results.

1. Start a new disposable world, then open a copy of a 1.12 world with the old provider removed. Inspect /datapack list enabled and the reload log for exactly one Matcha generation. Repeat /reload twice and check startup, tick and advancement functions. In a separate copy deliberately enable a renamed old world datapack and require an explicit duplicate-generation failure.
2. Use client pack 1.12.3. With Village Restoration enabled, inspect the five village types and placement using spacing 80, separation 50 and salt 10387312. Record the selected resource-pack order and check Foundation and Continuity against representative models.

The [1.12 testing procedure](https://github.com/Resivore/Mynx-Flavoured-Workbench/blob/c5172fee26245bfb71b768f8dc5a89c1c7c0a1fd/projects/matcha-flavoured-data/TESTING.md) retains the full previous release checklist and historical evidence. Reuse its unaffected controls alongside the cases above.

## Coordinated runtime checks in priority order

Use the exact eleven current JARs and Foundation v13, Minecraft Java 26.2 Fabric, the publisher's client pack 1.12.3, and the normal runtime dependencies. Record filenames, SHA-256, pack order, seed, logs and observations for a disposable new world and a copy of an existing world. Check the following before considering acceptance.

1. **Startup and persistence.** Inspect the enabled datapacks, world preparation and two reloads. Require exactly one 1.12.5 generation. Check function, recipe, loot, registry, advancement, tag and item-component decoding. Reconnect and restart with the same world. Exercise renamed duplicate-pack detection in a separate copy.
2. **Health and scarcity.** Start at 10 hearts. Die successively to 5, die at the floor, and repeat with keepInventory both ways. Use normal Hearts at 9, 10, 29 and 30 and Reinforced Hearts at 5, 9 and 10. Check deliberate main-hand use, the three exact shaped recipes, structure loot, the Wither and all progression advancements. Confirm death fires once and the correct items drop. Check Ancient City loot and Luck variation, sculk with and without Silk Touch, fishing and the disc and blessing sinks. Use the Heart project's checklist for each boundary.
3. **Equipment.** Exercise all six slots on fresh smithing outputs and saved items. Test intrinsic levels below and above the minimum, an unrelated enchantment, alternating hands, two players and an empty item after an enchanted item. Check smithing, equip, reload and reconnect.
4. **Villagers.** Compare new offers and newly unlocked tiers against the 307-row TSV. Inspect both Abbey payments, Shipwreck, Archaeologist's Brush, Ofuda costs and all reclamation families including the Claymore. Check Fisherman and Leatherworker quantities, the three Butcher sales, the four modified trades and empty reviewed tiers. On loaded old villagers, verify removal of dynamic favorite-food buybacks and the forbidden Heart exchange while levels, XP and unaffected offer uses persist. Visit previously unloaded chunks, exhaust stock, wait through restock, reload and restart. Confirm later restocks and fresh trades never reintroduce a buyback. Pre-existing ordinary vanilla offers retain their saved state. Also exercise a Florist, Farmer, normal wandering trader and job-site work routines.
5. **Ribbits.** Check the two permanent Chef offers and one daily special per unlocked tier. Observe day change with a menu open, stock exhaustion, two restocks, chunk reload and restart. Confirm the permanent specialty persists. Check one exact purchased stack per tier against its 1.12.5 recipe and verify the three excluded foods remain absent. Open saved Sorcerers from all four former ranks, including partially used offers and both blessing choices. Check the Benzene gate, Gatecaller and Oracle at 30 XP, three displayed titles, no Flask, the map service and unchanged pricing/stock across restock and restart.
6. **Client and world integration.** Open JEI for hearts, semantic foods, asylum applications and merchant trades. Verify five village placements in fresh terrain and saved terrain continuity, Abbey and Shipwreck maps, Enderscape drops and Kindling, Frost equipment, coal consolidation and Inventory Particles. With Foundation above the official client pack, check the five armor families in inventory and on entities, Sturdy Leather overlays, established block overrides and Continuity. Recheck the Noxious Redstone effect and private overlays on the client.

Project-specific procedures give the exact offers, boundaries and negative controls. Build results, registry checks and fixture results are static evidence. Record runtime observations separately against the tested release hashes.
