# Testing

C8 (legacy Canary 7), embedded version `1.20.1-3.3.3+26.2-workbench-canary7`, is the current unaccepted successor. It fixes the C7 latent defect reproduced twice during the IBF C5 diagnostic: `AdvancementManagerMixin.interceptApply(...)` mutated the immutable map passed to Minecraft 26.2 `ServerAdvancementManager.apply(...)`, producing `UnsupportedOperationException`. Exact C7 / legacy Canary 6 remains accepted historical provenance; this successor has not been promoted or deployed through the Test Instance Manager.

Controlled validation used the exact C8 JAR with Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.157.0+26.2, Java 25, and a production-mode isolated standalone Fabric server. Startup loaded 1,633 recipes and 1,748 advancements. Two consecutive authenticated `reload` commands each completed with those same counts, with no `UnsupportedOperationException`, mixin error, or count growth. This is `PARTIAL_RUNTIME_PASS` for the server-data reload path only; it is not a full gameplay or IBF dependency-chain pass.

1. In the IBF C5 full dependency-chain environment (including its approximately 3,802-recipe data-pack diagnostic), replace C7 with the exact C8 artifact and run startup plus at least two data reloads. Confirm the server continues past `ServerAdvancementManager.apply(...)` without `UnsupportedOperationException` and that generated Dramatic Doors recipe advancements, vanilla/provider advancements, and advancement totals remain present and stable.

2. Confirm representative vanilla and Macaw tall-door recipes craft two tall doors; confirm representative short stonecutting recipes craft two short doors and copper waxing recipes craft one correctly waxed door.

3. Place and use representative wooden, metal, copper, Macaw stable, sliding, and Japanese doors; confirm facing, hinge, hand and redstone interaction, collision, and property-optional waterlogging remain correct.

4. Exercise lower, middle, and upper tall-door placement, synchronization, breaking, cleanup, and drops; confirm each true three-block door behaves as one logical door.

5. Exercise representative short and tall waterlogging plus copper weathering, waxing, and axe interaction, then save, reload, unload/reload the chunk, and restart.

6. Confirm representative vanilla, glass, metal, weathering, tall, short, and available optional-family items remain visible and render correctly in inventory, hand, and world.

7. Exercise representative villager, piglin, witch, generic door-interaction, fence-gate, loot, and recipe paths and inspect `latest.log` for injection, missing-model, missing-texture, resource, or compatibility errors.

Stop and record a failure or inconclusive result if the game crashes, world data reload fails, a required mixin does not apply, generated or pre-existing advancements disappear or accumulate, registration or block-state construction fails, a door segment duplicates or loses drops, a recipe changes identity/count, or the test cannot distinguish Dramatic Doors behavior from an unrelated stack failure. Do not infer unobserved matrix rows from a successful launch or aggregate result.
