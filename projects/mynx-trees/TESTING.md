# Mynx Trees verification

## Canary 11 Mossy Oak

Check raw and stripped logs on X, Y and Z and compare both cut ends to the final previews. Check all six faces of each wood block and the inventory icons. Repeat with Foundation v12 above Matcha and with Foundation disabled.

Match the candidate to Canary 11 in WORKBENCH_STATUS.json and record its SHA-256 with observations. Canary 8's owner acceptance applies to that earlier release. Use an owner-selected Minecraft Java 26.2 Fabric world for these checks.

## Timber, crafting and fixtures

1. Compare all three woods in daylight with Foundation above Matcha Flavoured, then without Foundation. Place raw and stripped logs on X, Y and Z. Inspect both cuts. The rounded Silver Birch cuts should match the owner's barked and stripped sprites respectively. Rotate raw and stripped wood blocks and inspect every face for side bark.
2. For Silver Birch, alternate grass block, dirt, coarse dirt, rooted dirt, podzol and mycelium beneath an upright log. Swap to stone, sand, air and another log. Check the four sides and both end faces after each swap, on a horizontal log, after a chunk reload and with adjacent identical logs. The base-bark change belongs only to the upright log directly on a tagged soil. Its inventory model uses ordinary bark.
3. Craft four same-species planks from each raw and stripped timber. Craft the four-log square into three wood blocks. Axe-strip every axis and both wood forms. With Ribbits present, Mossy Oak timber yields Ribbits planks. Without Ribbits, the logs remain obtainable and the four Ribbits plank recipes are absent.
4. Place slabs, stairs, fences, gates, buttons, pressure plates, doors and trapdoors beside the corresponding vanilla forms. Check placement, orientation, support, water and redstone as applicable. Edit both sign faces, wax the signs and break all four standing, wall and hanging placements. Store three stacks on a shelf and check power, persistence and drops.
5. Craft each boat and chest boat. Launch by hand and dispenser, steer, transport a passenger, store and retrieve cargo, reload the world and break the vessel. Inspect the hull, oars, chest, inventory icon and dropped items. AMC supplies the Mossy Oak vessels when Ribbits is present.

## Generation and foliage

1. In fresh chunks compare a fixed seed against an equivalent baseline. Survey Swamp Oak, Large/Fancy Oak and Fallen Oak separately. Count Oak and Mossy Oak log positions by feature. Expected long-run shares are one third, one tenth and one third. Inspect angled limbs, horizontal fallen logs and upright stumps, decorators, vines, leaves and neighboring tree density. Small ordinary oaks retain native timber. Existing terrain has no conversion pass.
2. Inspect old-growth birch forest for Silver Birch standing and fallen forms. Grow a planted Silver Birch. Compare golden canopy variation at several X/Z positions, the inventory leaf cube, sapling, grass blending and shears or Silk Touch drops against accepted C8. Check decay with raw and stripped logs nearby.
3. Observe ordinary cherry and Wisteria in fresh cherry groves and from planted saplings. Check the Wisteria selection, Sweet Violets, particles, saplings and leaves. Repeat the relevant checks with Nemo present and absent. With Iris and a compatible shader, compare leaf waving and lighting to matching Birch and Cherry states, including after resource reload.

## Oak sapling growth

1. In a well-lit clearing, let Oak Saplings grow by random ticks and grow a second group with bonemeal. Repeat with and without nearby flowers. Without a swamp-tree provider, ordinary Oak and its bee variant should contain only Oak Logs in plains and swamps. Across many Fancy Oaks, about one in ten log positions should be mossy, including eligible horizontal branches. Record the world seed and use different planting positions.
2. If FarmTweaks has swamp saplings enabled, repeat in a swamp. If using Replant Swamp Trees, also repeat in a mangrove swamp. When the grower selects Swamp Oak, count about one mossy position in three. Compare vines, canopy, branch axes and any nests with the same provider and seed. A single short tree may contain zero Mossy Oak Logs. Check that a low obstruction still prevents growth and leaves the sapling in place.

## Build reproduction

Use the Java 25 Gradle wrapper and the pinned Minecraft 26.2 client. Run `gradlew.bat build`. The private assembler reads the primary checkout's immutable references and `private/assets/silver-birch/{birch_log_top,stripped_birch_log_top}.png`. The input manifest records both owner files, their SHA-256 values and their namespaced package destinations. Package checks cover source bytes, native models and all log axes. The world-generation fixture samples 32,768 positions per probability and compares adjacent positions and independent world seeds.

The isolated Fabric fixture runs with `gradlew.bat runGameTest`. For the FarmTweaks case, pass `-PfarmTweaksJar=<jar>` and `-PmidnightLibJar=<jar>`. For Replant Swamp Trees, pass `-PreplantJar=<jar>`. Pass all three for the combined case. The test classpath uses local reference JARs. The release has no dependency on either grower.

The 26.2 random-tick and successful bonemeal paths both call `SaplingBlock.advanceTree`. The vanilla Oak grower selects ordinary or Fancy Oak, using their `_bees_005` variants near flowers. The fixture records the configured feature actually placed. It compares two attempts at the same position and seed with only the timber roll disabled for the control. Checks cover every occupied position and block state in the plot, the next growth RNG value, vines, nests, leaf litter, blocked growth, and direct feature placement outside sapling growth. Natural-feature checks exercise registered features in a FEATURES-step WorldGenRegion backed by the fixture's writable chunks.

The local 26.2 pack exports (`Matcha Flavoured 26.2 1.0.0.mrpack` and `1.17.mrpack`) list neither candidate. On the isolated Fabric 0.19.3 test stack, FarmTweaks 2.3 with MidnightLib 1.10.0 selects a grower for `minecraft:swamp_oak` in swamps. Replant Swamp Trees 0.0.3+26.1 changes the selected key to `minecraft:swamp_oak` in swamps and mangrove swamps. Its published file targets 26.1 and its manifest permits 26.2. Live pack configuration and owner gameplay evidence remain unverified.
