# Matcha trade overlay comparison

Source: accepted Matcha Flavoured 1.12 embedded data tree. Counts and item IDs below are effective trade stacks; model and contents notes identify custom stacks.

| Trade ID | Old effective trade | New effective trade |
| --- | --- | --- |
| `minecraft:armorer/1/comparator` | 1 minecraft:emerald → 64 minecraft:comparator; max uses 999 | 1 minecraft:emerald → 16 minecraft:comparator; max uses 999 |
| `minecraft:armorer/1/composter` | 1 minecraft:emerald → 64 minecraft:composter; max uses 999 | REMOVED |
| `minecraft:armorer/1/noteblock` | 1 minecraft:emerald → 64 minecraft:note_block; max uses 999 | 1 minecraft:emerald → 32 simple_copper_pipes:waxed_copper_pipe; max uses 999 |
| `minecraft:armorer/1/redstone_block` | 1 minecraft:emerald → 64 minecraft:redstone_block; max uses 999 | 1 minecraft:emerald → 16 minecraft:redstone_block; max uses 999 |
| `minecraft:armorer/1/repeater` | 1 minecraft:emerald → 64 minecraft:repeater; max uses 999 | 1 minecraft:emerald → 32 minecraft:repeater; max uses 999 |
| `minecraft:armorer/2/redstone_lamp` | 1 minecraft:emerald → 64 minecraft:redstone_lamp; max uses 999 | 1 minecraft:emerald → 16 minecraft:redstone_lamp; max uses 999 |
| `minecraft:armorer/2/target` | 1 minecraft:emerald → 64 minecraft:target; max uses 999 | 1 minecraft:emerald → 8 simple_copper_pipes:waxed_copper_fitting; max uses 999 |
| `minecraft:armorer/2/waxed_copper_bulb` | 1 minecraft:emerald → 64 minecraft:waxed_copper_bulb; max uses 999 | 1 minecraft:emerald → 16 minecraft:waxed_copper_bulb; max uses 999 |
| `minecraft:armorer/3/dispenser` | 1 minecraft:emerald → 32 minecraft:dispenser; max uses 999 | 1 minecraft:emerald → 8 minecraft:dispenser; max uses 999 |
| `minecraft:armorer/3/dropper` | 1 minecraft:emerald → 64 minecraft:dropper; max uses 999 | 1 minecraft:emerald → 16 minecraft:dropper; max uses 999 |
| `minecraft:armorer/3/hopper` | 1 minecraft:emerald → 32 minecraft:hopper; max uses 999 | 1 minecraft:emerald → 8 minecraft:hopper; max uses 999 |
| `minecraft:armorer/3/observer` | 1 minecraft:emerald → 64 minecraft:observer; max uses 999 | 1 minecraft:emerald → 16 minecraft:observer; max uses 999 |
| `minecraft:armorer/4/crafter` | 1 minecraft:emerald → 32 minecraft:crafter; max uses 999 | 1 minecraft:emerald → 8 minecraft:crafter; max uses 999 |
| `minecraft:armorer/4/piston` | 1 minecraft:emerald → 32 minecraft:piston; max uses 999 | 1 minecraft:emerald → 8 minecraft:piston; max uses 999 |
| `minecraft:armorer/4/sticky_piston` | 1 minecraft:emerald → 32 minecraft:sticky_piston; max uses 999 | 1 minecraft:emerald → 8 minecraft:sticky_piston; max uses 999 |
| `minecraft:armorer/5/lava_kit` | 1 minecraft:emerald → 1 minecraft:red_bundle [bundle contents]; max uses 999 | 1 minecraft:emerald → 8 copperhopper:copper_hopper; max uses 999 |
| `minecraft:butcher/1/sweet_berry_toast` | 1 minecraft:emerald → 8 minecraft:poisonous_potato [model: minecraft:sweet_berry_toast]; max uses 999 | 1 minecraft:emerald → 2 minecraft:poisonous_potato [model: minecraft:sweet_berry_toast]; max uses 999 |
| `minecraft:butcher/1/sweet_berry_toast_recipe` | 1 minecraft:emerald → 1 minecraft:paper [model: minecraft:sweet_berry_toast_recipe]; max uses 999 | REMOVED |
| `minecraft:butcher/2/warped_stroganoff` | 1 minecraft:emerald → 4 minecraft:poisonous_potato [model: minecraft:warped_stroganoff]; max uses 999 | 1 minecraft:emerald → 1 minecraft:poisonous_potato [model: minecraft:warped_stroganoff]; max uses 999 |
| `minecraft:butcher/2/warped_stroganoff_recipe` | 1 minecraft:emerald → 1 minecraft:paper [model: minecraft:warped_stroganoff_recipe]; max uses 999 | REMOVED |
| `minecraft:butcher/3/chorus_mochi` | 1 minecraft:emerald → 16 minecraft:poisonous_potato [model: minecraft:chorus_mochi]; max uses 999 | 1 minecraft:emerald → 2 minecraft:poisonous_potato [model: minecraft:chorus_mochi]; max uses 999 |
| `minecraft:butcher/3/chorus_mochi_recipe` | 1 minecraft:emerald → 1 minecraft:paper [model: minecraft:chorus_mochi_recipe]; max uses 999 | REMOVED |
| `minecraft:butcher/4/butcher_knife` | 1 minecraft:emerald → 1 minecraft:stone_sword [model: minecraft:butcher_knife]; max uses 999 | 8 minecraft:emerald → 1 minecraft:stone_sword [model: minecraft:butcher_knife]; max uses 999 |
| `minecraft:cartographer/5/cheerful_clay_statue` | 1 minecraft:goat_horn [model: minecraft:cheerful_clay_statue] → 1 minecraft:poisonous_potato [model: minecraft:heart_container]; max uses 999 | 1 minecraft:goat_horn [model: minecraft:cheerful_clay_statue] → 16 minecraft:emerald; max uses 999 |
| `minecraft:cartographer/5/mournful_clay_statue` | 1 minecraft:goat_horn [model: minecraft:mournful_clay_statue] → 1 minecraft:poisonous_potato [model: minecraft:heart_container]; max uses 999 | 1 minecraft:goat_horn [model: minecraft:mournful_clay_statue] → 16 minecraft:emerald; max uses 999 |
| `minecraft:farmer/1/bonemeal` | 1 minecraft:emerald → 64 minecraft:bone_meal; max uses 999 | 1 minecraft:emerald → 8 minecraft:bone_meal; max uses 999 |
| `minecraft:farmer/1/exotic_seed_bundle` | 1 minecraft:emerald + 1 minecraft:jungle_sapling → 1 minecraft:bundle [model: exotic_seed_bundle] [bundle contents]; max uses 999 | 1 minecraft:emerald → 8 minecraft:wheat_seeds; max uses 999 |
| `minecraft:farmer/1/floral_bundle` | 1 minecraft:emerald → 1 minecraft:bundle [model: floral_bundle] [bundle contents]; max uses 999 | 1 minecraft:emerald → 8 minecraft:pumpkin_seeds; max uses 999 |
| `minecraft:farmer/1/mushy_bundle` | 1 minecraft:emerald → 1 minecraft:bundle [model: mushy_bundle] [bundle contents]; max uses 999 | 1 minecraft:emerald → 8 minecraft:melon_seeds; max uses 999 |
| `minecraft:farmer/1/seed_bundle` | 1 minecraft:emerald → 1 minecraft:bundle [model: seed_bundle] [bundle contents]; max uses 999 | 1 minecraft:emerald → 8 minecraft:beetroot_seeds; max uses 999 |
| `minecraft:farmer/2/blue_egg` | 1 minecraft:emerald → 1 minecraft:chicken_spawn_egg [model: minecraft:blue_egg]; max uses 999 | 8 minecraft:emerald → 1 minecraft:chicken_spawn_egg [model: minecraft:blue_egg]; max uses 1 |
| `minecraft:farmer/2/brown_egg` | 1 minecraft:emerald → 1 minecraft:chicken_spawn_egg [model: minecraft:brown_egg]; max uses 999 | 8 minecraft:emerald → 1 minecraft:chicken_spawn_egg [model: minecraft:brown_egg]; max uses 1 |
| `minecraft:farmer/2/white_egg` | 1 minecraft:emerald → 1 minecraft:chicken_spawn_egg [model: minecraft:egg]; max uses 999 | 8 minecraft:emerald → 1 minecraft:chicken_spawn_egg [model: minecraft:egg]; max uses 1 |
| `minecraft:farmer/3/baby_cold_cow` | 1 minecraft:emerald → 1 minecraft:cow_spawn_egg [model: crate_cold_cow]; max uses 999 | 16 minecraft:emerald → 1 minecraft:cow_spawn_egg [model: crate_cold_cow]; max uses 1 |
| `minecraft:farmer/3/baby_cold_pig` | 1 minecraft:emerald → 1 minecraft:pig_spawn_egg [model: crate_cold_pig]; max uses 999 | 16 minecraft:emerald → 1 minecraft:pig_spawn_egg [model: crate_cold_pig]; max uses 1 |
| `minecraft:farmer/3/baby_temperate_cow` | 1 minecraft:emerald → 1 minecraft:cow_spawn_egg [model: crate_temperate_cow]; max uses 999 | 16 minecraft:emerald → 1 minecraft:cow_spawn_egg [model: crate_temperate_cow]; max uses 1 |
| `minecraft:farmer/3/baby_temperate_pig` | 1 minecraft:emerald → 1 minecraft:pig_spawn_egg [model: crate_temperate_pig]; max uses 999 | 16 minecraft:emerald → 1 minecraft:pig_spawn_egg [model: crate_temperate_pig]; max uses 1 |
| `minecraft:farmer/3/baby_warm_cow` | 1 minecraft:emerald → 1 minecraft:cow_spawn_egg [model: crate_warm_cow]; max uses 999 | 16 minecraft:emerald → 1 minecraft:cow_spawn_egg [model: crate_warm_cow]; max uses 1 |
| `minecraft:farmer/3/baby_warm_pig` | 1 minecraft:emerald → 1 minecraft:pig_spawn_egg [model: crate_warm_pig]; max uses 999 | 16 minecraft:emerald → 1 minecraft:pig_spawn_egg [model: crate_warm_pig]; max uses 1 |
| `minecraft:fisherman/1/alaska_blackfish` | 12 minecraft:cod [model: alaska_blackfish] → 1 minecraft:emerald; max uses 999 | 6 minecraft:cod [model: alaska_blackfish] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/1/anchovy` | 12 minecraft:cod [model: anchovy] → 1 minecraft:emerald; max uses 999 | 6 minecraft:cod [model: anchovy] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/1/bluegill` | 12 minecraft:cod [model: bluegill] → 1 minecraft:emerald; max uses 999 | 6 minecraft:cod [model: bluegill] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/1/bujurqui` | 12 minecraft:cod [model: bujurqui] → 1 minecraft:emerald; max uses 999 | 6 minecraft:cod [model: bujurqui] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/1/cod` | 12 minecraft:cod → 1 minecraft:emerald; max uses 999 | 6 minecraft:cod → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/1/crappie` | 12 minecraft:cod [model: crappie] → 1 minecraft:emerald; max uses 999 | 6 minecraft:cod [model: crappie] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/1/freshwater_pufferfish` | 12 minecraft:pufferfish [model: freshwater_pufferfish] → 1 minecraft:emerald; max uses 999 | 6 minecraft:pufferfish [model: freshwater_pufferfish] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/1/guppy` | 12 minecraft:cod [model: guppy] → 1 minecraft:emerald; max uses 999 | 6 minecraft:cod [model: guppy] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/1/humpback_whitefish` | 12 minecraft:cod [model: humpback_whitefish] → 1 minecraft:emerald; max uses 999 | 6 minecraft:cod [model: humpback_whitefish] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/1/mediterranean_killifish` | 12 minecraft:cod [model: mediterranean_killifish] → 1 minecraft:emerald; max uses 999 | 6 minecraft:cod [model: mediterranean_killifish] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/1/pufferfish` | 12 minecraft:pufferfish → 1 minecraft:emerald; max uses 999 | 6 minecraft:pufferfish → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/1/rainbow_wrasse` | 12 minecraft:cod [model: rainbow_wrasse] → 1 minecraft:emerald; max uses 999 | 6 minecraft:cod [model: rainbow_wrasse] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/1/salmon` | 12 minecraft:salmon → 1 minecraft:emerald; max uses 999 | 6 minecraft:salmon → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/1/shad` | 12 minecraft:cod [model: shad] → 1 minecraft:emerald; max uses 999 | 6 minecraft:cod [model: shad] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/1/striped_perch` | 12 minecraft:cod [model: striped_perch] → 1 minecraft:emerald; max uses 999 | 6 minecraft:cod [model: striped_perch] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/1/tropical_fish` | 12 minecraft:tropical_fish → 1 minecraft:emerald; max uses 999 | 6 minecraft:tropical_fish → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/2/black_seabass` | 8 minecraft:salmon [model: black_seabass] → 1 minecraft:emerald; max uses 999 | 4 minecraft:salmon [model: black_seabass] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/2/carp` | 8 minecraft:salmon [model: carp] → 1 minecraft:emerald; max uses 999 | 4 minecraft:salmon [model: carp] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/2/flying_fish` | 8 minecraft:cod [model: flying_fish] → 1 minecraft:emerald; max uses 999 | 4 minecraft:cod [model: flying_fish] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/2/gurnard` | 8 minecraft:cod [model: gurnard] → 1 minecraft:emerald; max uses 999 | 4 minecraft:cod [model: gurnard] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/2/herring` | 8 minecraft:cod [model: herring] → 1 minecraft:emerald; max uses 999 | 4 minecraft:cod [model: herring] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/2/lamprey` | 8 minecraft:cod [model: lamprey] → 1 minecraft:emerald; max uses 999 | 4 minecraft:cod [model: lamprey] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/2/mahi_mahi` | 8 minecraft:salmon [model: mahi_mahi] → 1 minecraft:emerald; max uses 999 | 4 minecraft:salmon [model: mahi_mahi] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/2/piranha` | 8 minecraft:cod [model: piranha] → 1 minecraft:emerald; max uses 999 | 4 minecraft:cod [model: piranha] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/2/spoonhead_sculpin` | 8 minecraft:cod [model: spoonhead_sculpin] → 1 minecraft:emerald; max uses 999 | 4 minecraft:cod [model: spoonhead_sculpin] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/2/walleye` | 8 minecraft:salmon [model: walleye] → 1 minecraft:emerald; max uses 999 | 4 minecraft:salmon [model: walleye] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/3/armoured_catfish` | 4 minecraft:salmon [model: armoured_catfish] → 1 minecraft:emerald; max uses 999 | 2 minecraft:salmon [model: armoured_catfish] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/3/catfish` | 4 minecraft:salmon [model: catfish] → 1 minecraft:emerald; max uses 999 | 2 minecraft:salmon [model: catfish] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/3/flounder` | 4 minecraft:salmon [model: flounder] → 1 minecraft:emerald; max uses 999 | 2 minecraft:salmon [model: flounder] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/3/gar` | 4 minecraft:salmon [model: gar] → 1 minecraft:emerald; max uses 999 | 2 minecraft:salmon [model: gar] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/3/monkfish` | 4 minecraft:salmon [model: monkfish] → 1 minecraft:emerald; max uses 999 | 2 minecraft:salmon [model: monkfish] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/3/northern_pike` | 4 minecraft:salmon [model: northern_pike] → 1 minecraft:emerald; max uses 999 | 2 minecraft:salmon [model: northern_pike] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/3/painted_moray` | 4 minecraft:salmon [model: painted_moray] → 1 minecraft:emerald; max uses 999 | 2 minecraft:salmon [model: painted_moray] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/3/sturgeon` | 4 minecraft:salmon [model: sturgeon] → 1 minecraft:emerald; max uses 999 | 2 minecraft:salmon [model: sturgeon] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/3/tunisian_barb` | 4 minecraft:salmon [model: tunisian_barb] → 1 minecraft:emerald; max uses 999 | 2 minecraft:salmon [model: tunisian_barb] → 1 minecraft:emerald; max uses 999 |
| `minecraft:fisherman/3/wolffish` | 4 minecraft:salmon [model: wolffish] → 1 minecraft:emerald; max uses 999 | 2 minecraft:salmon [model: wolffish] → 1 minecraft:emerald; max uses 999 |
| `minecraft:leatherworker/1/birch_log` | 1 minecraft:emerald → 64 minecraft:birch_log; max uses 999 | 1 minecraft:emerald → 16 minecraft:birch_log; max uses 999 |
| `minecraft:leatherworker/1/oak_log` | 1 minecraft:emerald → 64 minecraft:oak_log; max uses 999 | 1 minecraft:emerald → 16 minecraft:oak_log; max uses 999 |
| `minecraft:leatherworker/1/spruce_log` | 1 minecraft:emerald → 64 minecraft:spruce_log; max uses 999 | 1 minecraft:emerald → 16 minecraft:spruce_log; max uses 999 |
| `minecraft:leatherworker/2/acacia_log` | 1 minecraft:emerald + 1 minecraft:acacia_sapling → 64 minecraft:acacia_log; max uses 999 | 1 minecraft:emerald + 1 minecraft:acacia_sapling → 16 minecraft:acacia_log; max uses 999 |
| `minecraft:leatherworker/2/cherry_log` | 1 minecraft:emerald + 1 minecraft:cherry_sapling → 64 minecraft:cherry_log; max uses 999 | 1 minecraft:emerald + 1 minecraft:cherry_sapling → 16 minecraft:cherry_log; max uses 999 |
| `minecraft:leatherworker/2/dark_oak_log` | 1 minecraft:emerald + 1 minecraft:dark_oak_sapling → 64 minecraft:dark_oak_log; max uses 999 | 1 minecraft:emerald + 1 minecraft:dark_oak_sapling → 16 minecraft:dark_oak_log; max uses 999 |
| `minecraft:leatherworker/2/jungle_log` | 1 minecraft:emerald + 1 minecraft:jungle_sapling → 64 minecraft:jungle_log; max uses 999 | 1 minecraft:emerald + 1 minecraft:jungle_sapling → 16 minecraft:jungle_log; max uses 999 |
| `minecraft:leatherworker/2/mangrove_log` | 1 minecraft:emerald + 1 minecraft:mangrove_propagule → 64 minecraft:mangrove_log; max uses 999 | 1 minecraft:emerald + 1 minecraft:mangrove_propagule → 16 minecraft:mangrove_log; max uses 999 |
| `minecraft:leatherworker/3/crimson_log` | 1 minecraft:emerald + 1 minecraft:crimson_fungus → 64 minecraft:crimson_stem; max uses 999 | 1 minecraft:emerald + 1 minecraft:crimson_fungus → 16 minecraft:crimson_stem; max uses 999 |
| `minecraft:leatherworker/3/mushroom_stem` | 1 minecraft:emerald → 64 minecraft:mushroom_stem; max uses 999 | 1 minecraft:emerald → 16 minecraft:mushroom_stem; max uses 999 |
| `minecraft:leatherworker/3/pale_oak_log` | 1 minecraft:emerald + 1 minecraft:pale_oak_sapling → 64 minecraft:pale_oak_log; max uses 999 | 1 minecraft:emerald + 1 minecraft:pale_oak_sapling → 16 minecraft:pale_oak_log; max uses 999 |
| `minecraft:leatherworker/3/warped_log` | 1 minecraft:emerald + 1 minecraft:warped_fungus → 64 minecraft:warped_stem; max uses 999 | 1 minecraft:emerald + 1 minecraft:warped_fungus → 16 minecraft:warped_stem; max uses 999 |
| `minecraft:leatherworker/5/filler` | 18 minecraft:black_wool → 1 minecraft:emerald; max uses 16 | 1 minecraft:emerald → 16 minecraft:brown_mushroom_block; max uses 16 |
| `minecraft:leatherworker/5/red_mushroom_block` | NEW | 1 minecraft:emerald → 16 minecraft:red_mushroom_block; max uses 16 |
| `minecraft:librarian/1/divine_comedy` | 1 minecraft:book [model: minecraft:divine_comedy] → 1 minecraft:poisonous_potato [model: minecraft:heart_container]; max uses 999 | 1 minecraft:book [model: minecraft:divine_comedy] → 6 minecraft:emerald; max uses 999 |
| `minecraft:librarian/1/ender_pearl` | 1 minecraft:emerald → 16 minecraft:ender_pearl; max uses 999 | 1 minecraft:emerald → 1 minecraft:ender_pearl; max uses 999 |
| `minecraft:librarian/1/paradise_lost` | 1 minecraft:book [model: minecraft:paradise_lost] → 1 minecraft:poisonous_potato [model: minecraft:heart_container]; max uses 999 | 1 minecraft:book [model: minecraft:paradise_lost] → 6 minecraft:emerald; max uses 999 |
| `minecraft:mason/1/andesite` | 1 minecraft:emerald → 4 minecraft:chicken_spawn_egg [model: minecraft:andesite] [contains: 64 minecraft:andesite]; max uses 999 | 1 minecraft:emerald → 64 minecraft:andesite; max uses 999 |
| `minecraft:mason/1/diorite` | 1 minecraft:emerald → 4 minecraft:chicken_spawn_egg [model: minecraft:diorite] [contains: 64 minecraft:diorite]; max uses 999 | 1 minecraft:emerald → 64 minecraft:diorite; max uses 999 |
| `minecraft:mason/1/granite` | 1 minecraft:emerald → 4 minecraft:chicken_spawn_egg [model: minecraft:granite] [contains: 64 minecraft:granite]; max uses 999 | 1 minecraft:emerald → 64 minecraft:granite; max uses 999 |
| `minecraft:mason/1/stone` | 1 minecraft:emerald → 4 minecraft:chicken_spawn_egg [model: minecraft:stone] [contains: 64 minecraft:stone]; max uses 999 | 1 minecraft:emerald → 64 minecraft:stone; max uses 999 |
| `minecraft:mason/2/deepslate` | 1 minecraft:emerald → 2 minecraft:chicken_spawn_egg [model: minecraft:deepslate] [contains: 64 minecraft:deepslate]; max uses 999 | 1 minecraft:emerald → 32 minecraft:deepslate; max uses 999 |
| `minecraft:mason/2/glass` | 1 minecraft:emerald → 3 minecraft:chicken_spawn_egg [model: minecraft:glass] [contains: 64 minecraft:glass]; max uses 999 | 1 minecraft:emerald → 32 minecraft:glass; max uses 999 |
| `minecraft:mason/2/smooth_red_sandstone` | 1 minecraft:emerald → 2 minecraft:chicken_spawn_egg [model: minecraft:smooth_red_sandstone] [contains: 64 minecraft:smooth_red_sandstone]; max uses 999 | 1 minecraft:emerald → 32 minecraft:smooth_red_sandstone; max uses 999 |
| `minecraft:mason/2/smooth_sandstone` | 1 minecraft:emerald → 2 minecraft:chicken_spawn_egg [model: minecraft:smooth_sandstone] [contains: 64 minecraft:smooth_sandstone]; max uses 999 | 1 minecraft:emerald → 32 minecraft:smooth_sandstone; max uses 999 |
| `minecraft:mason/2/terracotta` | 1 minecraft:emerald → 64 minecraft:terracotta; max uses 999 | 1 minecraft:emerald → 16 minecraft:terracotta; max uses 999 |
| `minecraft:mason/2/tuff` | 1 minecraft:emerald → 2 minecraft:chicken_spawn_egg [model: minecraft:tuff] [contains: 64 minecraft:tuff]; max uses 999 | 1 minecraft:emerald → 32 minecraft:tuff; max uses 999 |
| `minecraft:mason/3/calcite` | 1 minecraft:emerald → 2 minecraft:chicken_spawn_egg [model: minecraft:calcite] [contains: 64 minecraft:calcite]; max uses 999 | 1 minecraft:emerald → 32 minecraft:calcite; max uses 999 |
| `minecraft:mason/3/flowstone` | 1 minecraft:emerald → 2 minecraft:chicken_spawn_egg [model: minecraft:dripstone_block] [contains: 64 minecraft:dripstone_block]; max uses 999 | 1 minecraft:emerald → 32 minecraft:dripstone_block; max uses 999 |
| `minecraft:mason/3/malachite` | 1 minecraft:emerald → 64 minecraft:prismarine; max uses 999 | 1 minecraft:emerald → 16 minecraft:prismarine; max uses 999 |
| `minecraft:mason/4/blackstone` | 1 minecraft:emerald → 64 minecraft:blackstone; max uses 999 | 1 minecraft:emerald → 16 minecraft:blackstone; max uses 999 |
| `minecraft:mason/4/smooth_basalt` | 1 minecraft:emerald → 64 minecraft:smooth_basalt; max uses 999 | 1 minecraft:emerald → 16 minecraft:smooth_basalt; max uses 999 |
| `minecraft:mason/4/smooth_quartz` | 1 minecraft:emerald → 64 minecraft:smooth_quartz; max uses 999 | 1 minecraft:emerald → 16 minecraft:smooth_quartz; max uses 999 |
| `minecraft:shepherd/1/black_wool` | 1 minecraft:emerald → 64 minecraft:black_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:black_wool; max uses 999 |
| `minecraft:shepherd/1/blue_wool` | 1 minecraft:emerald → 64 minecraft:blue_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:blue_wool; max uses 999 |
| `minecraft:shepherd/1/brown_wool` | 1 minecraft:emerald → 64 minecraft:brown_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:brown_wool; max uses 999 |
| `minecraft:shepherd/1/cyan_wool` | 1 minecraft:emerald → 64 minecraft:cyan_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:cyan_wool; max uses 999 |
| `minecraft:shepherd/1/gray_wool` | 1 minecraft:emerald → 64 minecraft:gray_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:gray_wool; max uses 999 |
| `minecraft:shepherd/1/green_wool` | 1 minecraft:emerald → 64 minecraft:green_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:green_wool; max uses 999 |
| `minecraft:shepherd/1/light_blue_wool` | 1 minecraft:emerald → 64 minecraft:light_blue_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:light_blue_wool; max uses 999 |
| `minecraft:shepherd/1/light_gray_wool` | 1 minecraft:emerald → 64 minecraft:light_gray_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:light_gray_wool; max uses 999 |
| `minecraft:shepherd/1/lime_wool` | 1 minecraft:emerald → 64 minecraft:lime_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:lime_wool; max uses 999 |
| `minecraft:shepherd/1/magenta_wool` | 1 minecraft:emerald → 64 minecraft:magenta_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:magenta_wool; max uses 999 |
| `minecraft:shepherd/1/orange_wool` | 1 minecraft:emerald → 64 minecraft:orange_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:orange_wool; max uses 999 |
| `minecraft:shepherd/1/pink_wool` | 1 minecraft:emerald → 64 minecraft:pink_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:pink_wool; max uses 999 |
| `minecraft:shepherd/1/purple_wool` | 1 minecraft:emerald → 64 minecraft:purple_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:purple_wool; max uses 999 |
| `minecraft:shepherd/1/red_wool` | 1 minecraft:emerald → 64 minecraft:red_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:red_wool; max uses 999 |
| `minecraft:shepherd/1/white_wool` | 1 minecraft:emerald → 64 minecraft:white_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:white_wool; max uses 999 |
| `minecraft:shepherd/1/yellow_wool` | 1 minecraft:emerald → 64 minecraft:yellow_wool; max uses 999 | 1 minecraft:emerald → 16 minecraft:yellow_wool; max uses 999 |
| `minecraft:shepherd/2/baby_cold_sheep` | 1 minecraft:emerald → 1 minecraft:sheep_spawn_egg [model: crate_sheep_black]; max uses 999 | 16 minecraft:emerald → 1 minecraft:sheep_spawn_egg [model: crate_sheep_black]; max uses 1 |
| `minecraft:shepherd/2/baby_temperate_sheep` | 1 minecraft:emerald → 1 minecraft:sheep_spawn_egg [model: crate_sheep_white]; max uses 999 | 16 minecraft:emerald → 1 minecraft:sheep_spawn_egg [model: crate_sheep_white]; max uses 1 |
| `minecraft:shepherd/2/baby_warm_sheep` | 1 minecraft:emerald → 1 minecraft:sheep_spawn_egg [model: crate_sheep_brown]; max uses 999 | 16 minecraft:emerald → 1 minecraft:sheep_spawn_egg [model: crate_sheep_brown]; max uses 1 |
| `minecraft:shepherd/2/shepherds_shears` | 1 minecraft:emerald → 1 minecraft:shears [model: minecraft:bronze_shears]; max uses 999 | 8 minecraft:emerald → 1 minecraft:shears [model: minecraft:bronze_shears]; max uses 999 |
| `minecraft:shepherd/3/crook` | 1 minecraft:emerald → 1 minecraft:wooden_sword [model: minecraft:crook]; max uses 999 | 8 minecraft:emerald → 1 minecraft:wooden_sword [model: minecraft:crook]; max uses 999 |

124 changed, 4 removed, 1 added; 129 actionable sheet rows.
