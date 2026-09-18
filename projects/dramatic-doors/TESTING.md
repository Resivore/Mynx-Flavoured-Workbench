# Testing

C9 (legacy Canary 8), embedded version `1.20.1-3.3.3+26.2-workbench-canary8`, is the current controlled-validated candidate. It changes only the Enderscape compatibility source item for Dramatic Doors’ retained `short_murushroom_door` and `tall_murushroom_door` IDs: all registration, generated recipe, and recipe-advancement paths now use `enderscape:murublight_door`; `enderscape:celestial_door` and existing Dramatic Doors resources remain unchanged. Exact C8 / legacy Canary 7 remains the owner-accepted rollback and reference release until C9 is explicitly accepted.

The exact C9 JAR passed the focused Gradle/JUnit and static/package validation suite, including a dedicated Enderscape guard that rejects `enderscape:murushroom_door` in the compatibility source and packaged class while confirming all four generated recipe/advancement paths use `enderscape:murublight_door`. No exact Enderscape 3.0.2 input was available through allowed repository inputs, so no C9 Minecraft startup or data-reload validation was performed. This candidate is `RUNTIME_UNTESTED`; do not infer the C8 standalone-server result for C9.

1. With the exact C9 artifact, Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.157.0+26.2, and Enderscape 3.0.2 in an isolated standalone environment, create/load a world and perform data reload. Confirm there is no `Unknown registry key ... enderscape:murushroom_door` failure from `RecipeManagerMixin.interceptPrepare(...)`; confirm the short and tall Dramatic Doors Murushroom-ID recipes and recipe advancements source `enderscape:murublight_door`, and confirm the Celestial family remains available.

2. In the IBF C5 full dependency-chain environment (including its approximately 3,802-recipe data-pack diagnostic), replace C8 with the exact C9 artifact and run startup plus at least two data reloads. Confirm the server continues past `ServerAdvancementManager.apply(...)` without `UnsupportedOperationException` and that generated Dramatic Doors recipe advancements, vanilla/provider advancements, and advancement totals remain present and stable.

3. Confirm representative vanilla and Macaw tall-door recipes craft two tall doors; confirm representative short stonecutting recipes craft two short doors and copper waxing recipes craft one correctly waxed door.

4. Place and use representative wooden, metal, copper, Macaw stable, sliding, and Japanese doors; confirm facing, hinge, hand and redstone interaction, collision, and property-optional waterlogging remain correct.

5. Exercise lower, middle, and upper tall-door placement, synchronization, breaking, cleanup, and drops; confirm each true three-block door behaves as one logical door.

6. Exercise representative short and tall waterlogging plus copper weathering, waxing, and axe interaction, then save, reload, unload/reload the chunk, and restart.

7. Confirm representative vanilla, glass, metal, weathering, tall, short, and available optional-family items remain visible and render correctly in inventory, hand, and world.

8. Exercise representative villager, piglin, witch, generic door-interaction, fence-gate, loot, and recipe paths and inspect `latest.log` for injection, missing-model, missing-texture, resource, or compatibility errors.

Stop and record a failure or inconclusive result if the game crashes, world data reload fails, a required mixin does not apply, generated or pre-existing advancements disappear or accumulate, registration or block-state construction fails, a door segment duplicates or loses drops, a recipe changes identity/count, or the test cannot distinguish Dramatic Doors behavior from an unrelated stack failure. Do not infer unobserved matrix rows from a successful launch or aggregate result.
