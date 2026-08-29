# Testing

C7 (legacy Canary 6) is the exact accepted runtime baseline. The frozen Workbench run reached and rendered the test world, and the user reported aggregate `PASS`; individual gameplay checks below were not separately reported. Run this concise matrix only for a future cumulative regression or an explicitly authorized successor handoff.

1. Launch through world data reload and confirm the Dramatic Doors data pack loads, the integrated server starts, the player joins, and the world renders without Dramatic Doors mixin, registry, recipe, or server-data errors.
2. Confirm representative vanilla and Macaw tall-door recipes craft two tall doors; confirm representative short stonecutting recipes craft two short doors and copper waxing recipes craft one correctly waxed door.
3. Place and use representative wooden, metal, copper, Macaw stable, sliding, and Japanese doors; confirm facing, hinge, hand and redstone interaction, collision, and property-optional waterlogging remain correct.
4. Exercise lower, middle, and upper tall-door placement, synchronization, breaking, cleanup, and drops; confirm each true three-block door behaves as one logical door.
5. Exercise representative short and tall waterlogging plus copper weathering, waxing, and axe interaction, then save, reload, unload/reload the chunk, and restart.
6. Confirm representative vanilla, glass, metal, weathering, tall, short, and available optional-family items remain visible and render correctly in inventory, hand, and world.
7. Exercise representative villager, piglin, witch, generic door-interaction, fence-gate, loot, and recipe paths and inspect `latest.log` for injection, missing-model, missing-texture, resource, or compatibility errors.

Stop and record a failure or inconclusive result if the game crashes, world data reload fails, a required mixin does not apply, registration or block-state construction fails, a door segment duplicates or loses drops, a recipe changes identity/count, or the test cannot distinguish Dramatic Doors behavior from an unrelated stack failure. Do not infer unobserved matrix rows from a successful launch or aggregate result.
