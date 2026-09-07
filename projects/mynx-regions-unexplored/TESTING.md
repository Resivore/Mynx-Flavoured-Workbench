# Testing

Exact current release `mynx-regions-unexplored-private-0.1.0+26.2-canary3.jar` (version `0.1.0+26.2-canary3`, SHA-256 `20460eac76b1712be8d0c6d39b5bcb755f2a7664e51eb70e73ad4ef071fd99a5`, source `b12564572871441efdd6478aecdb434322ada923`) is accepted in Workbench Stack v27 after the user's 2026-09-06 user-reported external aggregate PASS. No Test Slot deployment or slot history was created; Slot A remained empty and Slot B was preserved. No row-level observations beyond the reported PASS are inferred.

## Future regression procedure

The retained procedure below is for future regressions of this exact accepted identity or an explicitly identified successor. Any former pre-promotion candidate wording is historical and superseded by this accepted result.

# Mynx Regions Unexplored — Canary 3 runtime procedure

Current candidate: `0.1.0+26.2-canary3`, `mynx-regions-unexplored-private-0.1.0+26.2-canary3.jar`. The canonical manifest records its exact SHA-256 and implementation checkpoint. It is ACTIVE, NOT_DEPLOYED, and RUNTIME_UNTESTED. Build and package checks are not Minecraft observations. Canary 3 adds sparse, gently drifting vanilla end-rod particles around Mycotoxic Daisy flowers through the client ambient-display tick. The tall plant emits only from its upper half, and the existing potted form participates. Canary 2's Glowleaf display names and planted/potted Daisy block light 14 remain unchanged.

## Build and static reproduction

Use Java 25 and run `gradlew.bat clean check` from this project. The build verifies the exact private reference (`RegionsUnexploredFabric-0.5.6+1.20.1.jar`, SHA-256 `d24dc3a6a08432d51d042750fe50a6c5051f9d0c725cfc434cf9ce6eb8b7e332`) and Minecraft 26.2 baseline, generates only the requested asset closure, compiles the mod, runs the shader-alias unit test, and checks registry, loot, recipe, tag, resource and final-JAR boundaries. Restricted artwork, generated resources and the assembled JAR remain ignored and private.

The exact archive contains potted forms for Hyssop, five Lupines and Mycotoxic Daisy. It contains no potted Meadow Sage registration or asset, despite a contradictory row in the prior Ribbits audit; this candidate follows the archive. Stone Bud is the historical `regions_unexplored:stone_bud`, not Grass Sprouts or another merged lookalike.

## Runtime authorization and setup

This implementation does not allocate a testing slot or deploy anything. A later explicitly authorized task must assign this exact candidate through Test Instance Manager to the dedicated `Matcha Flavoured 26.2 Workbench`, preserving the other slot. Never touch the protected 26.1.2 profile.

Use a disposable creative world with cheats after managed deployment. Record the candidate SHA-256, deployment UUID, Minecraft/Fabric/Iris/shader versions, seed, positions and each observation. Repeat the server-safe cases on a dedicated Fabric server. Stop on a crash, registry mismatch, missing model/texture, broken half cleanup, fluid loss, item duplication, optional-client dependency failure, or unrelated RU/vanilla behavior change; preserve logs before altering the setup.

## Registration, placement and harvesting

1. Confirm exactly these 16 items appear in Natural Blocks and work with `/give`: Glowleaf (registry ID `mynx_regions_unexplored:dropleaf`), Barley, Windswept Grass, Clover, Stone Bud, Mycotoxic Daisy, Hyssop, Blue/Pink/Purple/Red/Yellow Lupine, Cattail, Duckweed, Tassel and Meadow Sage. `dropleaf_plant` and all seven potted blocks must have no separate item. No `glowleaf` registry ID, extra item/block, other RU plant, world generation, trade or initial-acquisition path should exist.
2. Load a world and inventory that already contain the Canary 1 `dropleaf` stack and placed `dropleaf`/`dropleaf_plant` blocks. Confirm they remain intact and now display Glowleaf and Glowleaf Plant where their names are shown; commands, loot and shader mappings must continue using the existing IDs.
3. Place, outline, break and support-test every plant in Survival. Verify correct names, sounds, collision, offsets, replaceability, drops and support loss. Hyssop and each Lupine use their compact reference shape. Both halves of Mycotoxic Daisy are static, emit block light level 14, grow on dirt-like blocks, nylium and soul soil, and apply no toxicity.
4. Stone Bud places only on the dedicated stone-support tag, not ordinary dirt/farmland; it has grass tint in world and item form, no bonemeal growth, one self drop with shears, and otherwise the vanilla short-grass wheat-seed chance including Fortune. It is flammable; Mycotoxic Daisy is deliberately not. Do not substitute or expose `grass_sprouts`.
5. Barley and Windswept Grass are static two-block plants. Breaking either half must clean the pair without duplicate drops. Barley drops one Barley. Windswept Grass drops two plants with shears and otherwise uses the tall-grass seed chance/Fortune behavior.
6. Clover must face the placer, stack from amount 1 through 4, drop its exact amount, fill through repeat placement/bonemeal, and eject one extra item when bonemealed at 4. It is not treated as a flower for bee or flower tags.

## Growth, water and renewal

1. Place Glowleaf beneath valid support with room below. Confirm the luminous head is age-based, grows downward randomly, creates non-random-ticking body segments, remains climbable, emits light level 14 only at the head, and uses 1–2 new blocks per bonemeal. Body-segment lighting must remain unchanged. Break head/body/support at several lengths and verify only one Glowleaf item is obtainable—never a body item or duplicate loot.
2. Place Cattail on its dirt/sand/clay/gravel support tag at dry edges and in source water. Confirm both halves, waterlogged state, fluid scheduling, source-water restoration when either half is removed, and correct cleanup if support or the other half disappears. Try water in the upper half with a bucket and record the exact accepted/rejected behavior rather than inferring it.
3. Place Duckweed on exposed source water and on each ice family supported by the target `IceBlock` check. Reject flowing water and invalid dry blocks. Confirm it is replaceable by another placed item, yields one Duckweed, does not invoke Lily Pad boat-destruction behavior, and never generates naturally.
4. Bonemeal Tassel at the lower and upper half, standing and crouching, by hand and dispenser. The plant must remain, produce one Tassel item per successful application, consume bonemeal through normal rules, and avoid tall-plant duplication. Record dispenser output/consumption separately.

## Food, recipes, compost, pots and bees

1. Smelt one Barley for 200 ticks and smoke one for 100 ticks; each yields one bread and 0.35 XP. Confirm all declared first-copy recipe-book unlocks. No recipe may create the first specimen of any plant.
2. Craft the nine flower stew recipes and verify their corresponding vanilla suspicious-stew effects: Hyssop uses Luck for 10 seconds; each Lupine uses Saturation for the target instant/short duration; Mycotoxic Daisy, Tassel and Meadow Sage use the declared vanilla mappings. Inspect recipe viewers for collisions or unrelated recipes.
3. Eat Meadow Sage when eligible: nutrition 2, saturation modifier 0.15, with an independent 50% Instant Health I chance and no lingering status. Then place the same item as the two-block plant and verify normal block placement takes precedence when targeting a valid surface.
4. Test every item directly and by hopper against a composter over repeated trials. Expected chances: Glowleaf/Duckweed 15%; Clover 20%; Stone Bud/Mycotoxic Daisy 30%; six small flowers 40%; Windswept Grass 50%; Barley/Cattail/Tassel/Meadow Sage 60%.
5. Pot and retrieve exactly Hyssop, five Lupines and Mycotoxic Daisy. Each potted block must return its flower plus the pot through normal interaction, while block loot itself is only the flower pot. Potted Mycotoxic Daisy emits block light level 14. Meadow Sage must not enter a pot.
6. Use bees with the nine requested flowers: Hyssop, five Lupines, Mycotoxic Daisy, Tassel and Meadow Sage. Confirm attraction, feeding/breeding and pollination navigation. Only Hyssop and the Lupines belong to `small_flowers`; Stone Bud and Clover do not.

## Rendering and optional shader behavior

1. Test without Iris, with Iris but shaders disabled, and with Iris 1.11.2 plus Complementary Unbound r5.8.1. The mod must load and render normally without Iris; there is no required Iris or shader-pack dependency.
2. With Complementary foliage waving enabled, compare Stone Bud, Clover and the six small flowers beside short grass; Barley, Windswept Grass, Mycotoxic Daisy, Cattail, Tassel and Meadow Sage beside tall grass; Duckweed beside a lily pad; and Glowleaf head/body beside weeping vines. Lower/upper tall halves must inherit their matching active-pack classifications. Pots must never inherit plant wind.
3. Explicit custom shader mappings must win. Reload shaders/resources and rejoin; inherited mappings must rebuild without hardcoded numeric material IDs, unrelated mapping changes, missing geometry, doubled faces or optional-mod class-loading errors. A shader pack that does not classify the vanilla counterpart receives no invented category.
4. Inspect all block and item textures, two-block seams, transparency, grass tints, light emission and inventory models under vanilla rendering and the normal resource/shader stack. No shader/profile/resource-pack file is changed by this project.

## Focused light-update regression

1. With shaders disabled, place a Mycotoxic Daisy in a dark enclosed area and confirm both its lower and upper states report and visibly emit block light level 14 into surrounding blocks. Repeat with the potted variant and confirm level 14.
2. Break the lower half, break the upper half, remove support, pot and unpot the flower, and reload the world after each representative setup. Confirm normal lighting updates immediately or after the normal engine recalculation, with no lingering light and no duplicate drops.
3. Repeat placement and removal beside opaque blocks and at a chunk boundary. Confirm the existing artwork, transparency, geometry and normal shader movement are unchanged; no shader bloom or dynamic-light feature is needed for surrounding blocks to illuminate.

## Focused Daisy particle regression

1. With the particle setting on All, observe one planted Mycotoxic Daisy for several minutes. Confirm sparse individual vanilla end-rod particles appear around its flowers with small horizontal motion and gentle upward drift; the lower half must not independently emit or double the effect.
2. Repeat with the existing potted Mycotoxic Daisy. Confirm the same sparse particle character is positioned around the potted flower, with no change to potting, retrieval, drops, model, texture or block light 14.
3. Compare equal observation periods on All, Decreased and Minimal. Decreased must visibly reduce the already sparse effect over a sufficient sample, and Minimal must suppress it through Minecraft's normal particle limiter. No always-visible or limiter-bypassing particle path is expected.
4. Repeat with shaders disabled and with the normal shader setup. Confirm particles require no shader or dynamic-light feature, and that Daisy lighting, transparency, geometry and wind motion remain unchanged.
5. Start a dedicated Fabric server and connect a client. Confirm server startup and plant gameplay require no client class, particle packet, server tick or shader dependency; particle presentation remains local to each client's settings.

## Compatibility and stopping conditions

Run the focused matrix once with only required Fabric dependencies, then with the normal Workbench stack. Confirm no runtime dependency on Regions Unexplored and no Create serializer or recipe when Create is unavailable for 26.2. Verify the enabled dependency graph and server startup, client join, resource reload and shutdown. Do not claim Create compatibility, shader motion, bee behavior, fluid restoration, dispenser behavior or dedicated-server safety until directly observed.

No Minecraft client, server, GameTest world, managed deployment, Test Instance Manager state or testing slot was launched or changed during implementation. Every runtime case above, including particle appearance and setting behavior, remains NOT_RUN.
