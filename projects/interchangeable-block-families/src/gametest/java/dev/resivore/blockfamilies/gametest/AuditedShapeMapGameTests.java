package dev.resivore.blockfamilies.gametest;

import dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamilies;
import dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily;
import dev.resivore.blockfamilies.cnm.runtime.AuditedShapeRuntime;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runtime proofs against the real provider registries and CNM's resolved ShapeMap.
 * Catalog-only contracts live in the ordinary unit-test source set; these tests
 * deliberately exercise the transformed Minecraft/CNM classes after every
 * pinned provider has registered its items.
 */
public final class AuditedShapeMapGameTests implements CustomTestMethodInvoker {
    private static final List<List<String>> C8_DISPLAY_FIXTURE_FAMILIES = List.of(
            List.of("minecraft:oak_sign", "minecraft:oak_hanging_sign", "minecraft:oak_shelf"),
            List.of("minecraft:spruce_sign", "minecraft:spruce_hanging_sign", "minecraft:spruce_shelf"),
            List.of("minecraft:birch_sign", "minecraft:birch_hanging_sign", "minecraft:birch_shelf"),
            List.of("minecraft:jungle_sign", "minecraft:jungle_hanging_sign", "minecraft:jungle_shelf"),
            List.of("minecraft:acacia_sign", "minecraft:acacia_hanging_sign", "minecraft:acacia_shelf"),
            List.of("minecraft:dark_oak_sign", "minecraft:dark_oak_hanging_sign", "minecraft:dark_oak_shelf"),
            List.of("minecraft:mangrove_sign", "minecraft:mangrove_hanging_sign", "minecraft:mangrove_shelf"),
            List.of("minecraft:cherry_sign", "minecraft:cherry_hanging_sign", "minecraft:cherry_shelf"),
            List.of("minecraft:pale_oak_sign", "minecraft:pale_oak_hanging_sign", "minecraft:pale_oak_shelf"),
            List.of("minecraft:bamboo_sign", "minecraft:bamboo_hanging_sign", "minecraft:bamboo_shelf"),
            List.of("minecraft:crimson_sign", "minecraft:crimson_hanging_sign", "minecraft:crimson_shelf"),
            List.of("minecraft:warped_sign", "minecraft:warped_hanging_sign", "minecraft:warped_shelf"),
            List.of("enderscape:veiled_sign", "enderscape:veiled_hanging_sign", "enderscape:veiled_shelf"),
            List.of("enderscape:celestial_sign", "enderscape:celestial_hanging_sign", "enderscape:celestial_shelf"),
            List.of("enderscape:murublight_sign", "enderscape:murublight_hanging_sign",
                    "enderscape:murublight_shelf")
    );

    private static final List<List<String>> C9_ENDERSCAPE_FENCE_GATE_FAMILIES = List.of(
            List.of("enderscape:veiled_fence", "enderscape:veiled_fence_gate",
                    "bbb:veiled_frame", "bbb:veiled_lattice"),
            List.of("enderscape:celestial_fence", "enderscape:celestial_fence_gate",
                    "bbb:celestial_frame", "bbb:celestial_lattice"),
            List.of("enderscape:murublight_fence", "enderscape:murublight_fence_gate",
                    "bbb:murublight_frame", "bbb:murublight_lattice")
    );

    private static final List<List<String>> C9_BBB_ENDERSCAPE_DETAIL_FAMILIES = List.of(
            List.of("bbb:veiled_trim", "bbb:veiled_balustrade", "bbb:veiled_support", "bbb:veiled_pallet"),
            List.of("bbb:celestial_trim", "bbb:celestial_balustrade", "bbb:celestial_support", "bbb:celestial_pallet"),
            List.of("bbb:murublight_trim", "bbb:murublight_balustrade", "bbb:murublight_support", "bbb:murublight_pallet")
    );

    private static final List<List<String>> C8_ENDERSCAPE_BUILDING_ACCESSORY_FAMILIES = List.of(
            List.of("enderscape:veiled_button", "enderscape:veiled_pressure_plate",
                    "architectural_material_closure:veiled_planks_path",
                    "architectural_material_closure:veiled_log_parapet",
                    "architectural_material_closure:veiled_plank_parapet",
                    "architectural_material_closure:veiled_blinds",
                    "architectural_material_closure:veiled_curtain_rod",
                    "architectural_material_closure:veiled_shutter",
                    "architectural_material_closure:veiled_louvered_shutter",
                    "dramaticdoors:short_veiled_door"),
            List.of("enderscape:celestial_button", "enderscape:celestial_pressure_plate",
                    "architectural_material_closure:celestial_planks_path",
                    "architectural_material_closure:celestial_stem_parapet",
                    "architectural_material_closure:celestial_plank_parapet",
                    "architectural_material_closure:celestial_blinds",
                    "architectural_material_closure:celestial_curtain_rod",
                    "architectural_material_closure:celestial_shutter",
                    "architectural_material_closure:celestial_louvered_shutter",
                    "dramaticdoors:short_celestial_door"),
            List.of("enderscape:murublight_button", "enderscape:murublight_pressure_plate",
                    "architectural_material_closure:murublight_planks_path",
                    "architectural_material_closure:murublight_stem_parapet",
                    "architectural_material_closure:murublight_plank_parapet",
                    "architectural_material_closure:murublight_blinds",
                    "architectural_material_closure:murublight_curtain_rod",
                    "architectural_material_closure:murublight_shutter",
                    "architectural_material_closure:murublight_louvered_shutter",
                    "dramaticdoors:short_murushroom_door"),
            List.of("enderscape:polished_end_stone_button",
                    "enderscape:polished_end_stone_pressure_plate",
                    "architectural_material_closure:end_stone_bricks_parapet",
                    "architectural_material_closure:end_stone_bricks_louvered_shutter"),
            List.of("enderscape:polished_mirestone_button",
                    "enderscape:polished_mirestone_pressure_plate",
                    "architectural_material_closure:mirestone_parapet",
                    "architectural_material_closure:mirestone_louvered_shutter"),
            List.of("enderscape:polished_veradite_button",
                    "enderscape:polished_veradite_pressure_plate",
                    "architectural_material_closure:veradite_parapet",
                    "architectural_material_closure:veradite_louvered_shutter"),
            List.of("enderscape:polished_kurodite_button",
                    "enderscape:polished_kurodite_pressure_plate",
                    "architectural_material_closure:kurodite_parapet",
                    "architectural_material_closure:kurodite_louvered_shutter")
    );

    private static final List<List<String>> C8_ENDERSCAPE_BAR_CHAIN_FAMILIES = List.of(
            List.of("enderscape:shadoline_bars", "enderscape:shadoline_chain")
    );

    private static final List<String> C8_WALL_ONLY_SIGN_BLOCKS = List.of(
            "minecraft:oak_wall_sign", "minecraft:oak_wall_hanging_sign",
            "minecraft:spruce_wall_sign", "minecraft:spruce_wall_hanging_sign",
            "minecraft:birch_wall_sign", "minecraft:birch_wall_hanging_sign",
            "minecraft:jungle_wall_sign", "minecraft:jungle_wall_hanging_sign",
            "minecraft:acacia_wall_sign", "minecraft:acacia_wall_hanging_sign",
            "minecraft:dark_oak_wall_sign", "minecraft:dark_oak_wall_hanging_sign",
            "minecraft:mangrove_wall_sign", "minecraft:mangrove_wall_hanging_sign",
            "minecraft:cherry_wall_sign", "minecraft:cherry_wall_hanging_sign",
            "minecraft:pale_oak_wall_sign", "minecraft:pale_oak_wall_hanging_sign",
            "minecraft:bamboo_wall_sign", "minecraft:bamboo_wall_hanging_sign",
            "minecraft:crimson_wall_sign", "minecraft:crimson_wall_hanging_sign",
            "minecraft:warped_wall_sign", "minecraft:warped_wall_hanging_sign",
            "enderscape:veiled_wall_sign", "enderscape:veiled_wall_hanging_sign",
            "enderscape:celestial_wall_sign", "enderscape:celestial_wall_hanging_sign",
            "enderscape:murublight_wall_sign", "enderscape:murublight_wall_hanging_sign"
    );


    private static final List<String> C9_CANONICAL_RESULT_RECIPE_IDS = List.of(
            "minecraft:oak_sign",
            "minecraft:spruce_sign",
            "minecraft:birch_sign",
            "minecraft:jungle_sign",
            "minecraft:acacia_sign",
            "minecraft:dark_oak_sign",
            "minecraft:mangrove_sign",
            "minecraft:cherry_sign",
            "minecraft:pale_oak_sign",
            "minecraft:bamboo_sign",
            "minecraft:crimson_sign",
            "minecraft:warped_sign",
            "enderscape:veiled_sign",
            "enderscape:celestial_sign",
            "enderscape:murublight_sign",
            "enderscape:murublight_sign_from_celestial_sign",
            "enderscape:veiled_fence",
            "enderscape:celestial_fence",
            "enderscape:murublight_fence",
            "enderscape:murublight_fence_from_celestial_fence",
            "enderscape:veiled_button",
            "enderscape:celestial_button",
            "enderscape:murublight_button",
            "enderscape:murublight_button_from_celestial_button",
            "enderscape:polished_end_stone_button",
            "enderscape:polished_mirestone_button",
            "enderscape:polished_veradite_button",
            "enderscape:polished_kurodite_button",
            "enderscape:polished_kurodite_button_from_polished_veradite_button",
            "enderscape:shadoline_bars",
            "bbb:veiled_trim", "bbb:celestial_trim", "bbb:murublight_trim"
    );

    private static final List<String> C9_REMOVED_ALTERNATE_RECIPE_IDS = List.of(
            "minecraft:oak_hanging_sign", "minecraft:oak_shelf",
            "minecraft:spruce_hanging_sign", "minecraft:spruce_shelf",
            "minecraft:birch_hanging_sign", "minecraft:birch_shelf",
            "minecraft:jungle_hanging_sign", "minecraft:jungle_shelf",
            "minecraft:acacia_hanging_sign", "minecraft:acacia_shelf",
            "minecraft:dark_oak_hanging_sign", "minecraft:dark_oak_shelf",
            "minecraft:mangrove_hanging_sign", "minecraft:mangrove_shelf",
            "minecraft:cherry_hanging_sign", "minecraft:cherry_shelf",
            "minecraft:pale_oak_hanging_sign", "minecraft:pale_oak_shelf",
            "minecraft:bamboo_hanging_sign", "minecraft:bamboo_shelf",
            "minecraft:crimson_hanging_sign", "minecraft:crimson_shelf",
            "minecraft:warped_hanging_sign", "minecraft:warped_shelf",
            "enderscape:veiled_hanging_sign",
            "enderscape:veiled_shelf",
            "enderscape:celestial_hanging_sign",
            "enderscape:celestial_shelf",
            "enderscape:murublight_hanging_sign",
            "enderscape:murublight_shelf",
            "enderscape:veiled_fence_gate",
            "enderscape:celestial_fence_gate",
            "enderscape:murublight_fence_gate",
            "enderscape:veiled_pressure_plate",
            "enderscape:celestial_pressure_plate",
            "enderscape:murublight_pressure_plate",
            "enderscape:polished_end_stone_pressure_plate",
            "enderscape:polished_mirestone_pressure_plate",
            "enderscape:polished_veradite_pressure_plate",
            "enderscape:polished_kurodite_pressure_plate",
            "enderscape:shadoline_chain",
            "bbb:veiled_frame", "bbb:veiled_lattice", "bbb:veiled_balustrade",
            "bbb:veiled_support", "bbb:veiled_pallet",
            "bbb:celestial_frame", "bbb:celestial_lattice", "bbb:celestial_balustrade",
            "bbb:celestial_support", "bbb:celestial_pallet",
            "bbb:murublight_frame", "bbb:murublight_lattice", "bbb:murublight_balustrade",
            "bbb:murublight_support", "bbb:murublight_pallet"
    );

    private static final List<String> C9_RETAINED_ALTERNATE_PROVIDER_RECIPE_IDS = List.of(
            "enderscape:murublight_hanging_sign_from_celestial_hanging_sign",
            "enderscape:murublight_shelf_from_celestial_shelf",
            "enderscape:murublight_fence_gate_from_celestial_fence_gate",
            "enderscape:murublight_pressure_plate_from_celestial_pressure_plate",
            "enderscape:polished_kurodite_pressure_plate_from_polished_veradite_pressure_plate"
    );

    private static final List<String> REGISTERED_EXCLUSIONS = List.of(
            "mcwdoors:garage_white_door",
            "mcwdoors:garage_silver_door",
            "mcwdoors:garage_gray_door",
            "mcwdoors:garage_black_door",
            "mcwdoors:wooden_portcullis",
            "mcwdoors:iron_portcullis",
            "dramaticdoors:tall_copper_door",
            "dramaticdoors:tall_exposed_copper_door",
            "dramaticdoors:tall_weathered_copper_door",
            "dramaticdoors:tall_oxidized_copper_door",
            "dramaticdoors:tall_waxed_copper_door",
            "dramaticdoors:tall_waxed_exposed_copper_door",
            "dramaticdoors:tall_waxed_weathered_copper_door",
            "dramaticdoors:tall_waxed_oxidized_copper_door",
            "mcwwindows:cobblestone_arrow_slit",
            "mcwwindows:blackstone_brick_arrow_slit",
            "minecraft:nether_brick_fence",
            "minecraft:copper_door",
            "minecraft:exposed_copper_door",
            "minecraft:weathered_copper_door",
            "minecraft:oxidized_copper_door",
            "minecraft:waxed_copper_door",
            "minecraft:waxed_exposed_copper_door",
            "minecraft:waxed_weathered_copper_door",
            "minecraft:waxed_oxidized_copper_door",
            "minecraft:copper_trapdoor",
            "minecraft:exposed_copper_trapdoor",
            "minecraft:weathered_copper_trapdoor",
            "minecraft:oxidized_copper_trapdoor",
            "minecraft:waxed_copper_trapdoor",
            "minecraft:waxed_exposed_copper_trapdoor",
            "minecraft:waxed_weathered_copper_trapdoor",
            "minecraft:waxed_oxidized_copper_trapdoor",
            "mcwpaths:andesite_running_bond",
            "mcwpaths:andesite_running_bond_slab",
            "mcwpaths:andesite_running_bond_stairs",
            "mcwpaths:andesite_running_bond",
            "mcwpaths:dirt_path_block",
            "mcwwindows:iron_shutter",
            "mcwwindows:golden_curtain_rod",
            "mcwwindows:metal_curtain_rod",
            "dramaticdoors:tall_pale_oak_door",
            "mcwwindows:red_curtain"
    );

    private static final List<String> UNREGISTERED_EXCLUSIONS = List.of(
            "mcwdoors:cherry_waffle_door",
            "dramaticdoors:tall_netherite_door",
            "dramaticdoors:tall_macaw_oak_classic_door",
            "dramaticdoors:tall_macaw_spruce_cottage_door",
            "dramaticdoors:tall_macaw_birch_paper_door",
            "dramaticdoors:tall_macaw_jungle_beach_door",
            "dramaticdoors:tall_macaw_acacia_tropical_door",
            "dramaticdoors:tall_macaw_dark_oak_four_panel_door",
            "dramaticdoors:tall_macaw_mangrove_swamp_door",
            "dramaticdoors:tall_macaw_cherry_waffle_door",
            "dramaticdoors:tall_macaw_crimson_nether_door",
            "dramaticdoors:tall_macaw_warped_mystic_door"
    );

    @GameTest(maxTicks = 40)
    public void allAuditedFamiliesMaterializeWithExactParentsAndOrder(GameTestHelper helper) {
        List<AuditedShapeFamily> families = expectedActiveFamilies();
        int expectedFamilies = Boolean.getBoolean("ibf.test.amcExpected") ? 284 : 181;
        expectedFamilies += treeFamilyCount();
        helper.assertTrue(families.size() == expectedFamilies, "Expected " + expectedFamilies + " families");

        Set<Identifier> seen = new LinkedHashSet<>();
        int largest = 0;
        for (AuditedShapeFamily family : families) {
            helper.assertTrue(family.canonicalParent().equals(family.members().getFirst()),
                    "Canonical parent is not first for " + family.key());

            Item parent = requiredItem(family.canonicalParent());
            List<Identifier> actual = ids(ShapeMap.getShapes(parent));
            List<Identifier> expected = family.members().stream()
                    .filter(id -> Boolean.getBoolean("ibf.test.amcExpected")
                            || !id.getNamespace().equals("architectural_material_closure"))
                    .toList();
            helper.assertTrue(actual.equals(expected),
                    "Resolved order differs for " + family.key() + ": expected="
                            + expected + ", actual=" + actual);

            for (Identifier id : expected) {
                helper.assertTrue(seen.add(id), "Audited ID materialized more than once: " + id);
                helper.assertTrue(ShapeMap.getParent(requiredItem(id)) == parent,
                        "Resolved parent differs for " + id + " in " + family.key());
            }
            largest = Math.max(largest, actual.size());
        }

        int expectedCount = Boolean.getBoolean("ibf.test.amcExpected") ? 2_255 : 1_412;
        expectedCount += treeItemCount();
        helper.assertTrue(seen.size() == expectedCount,
                "Expected " + expectedCount
                        + " unique runtime IDs, found " + seen.size());
        helper.assertTrue(largest == (Boolean.getBoolean("ibf.test.amcExpected") ? 23 : 22),
                "Unexpected largest runtime family size " + largest);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void c8DisplayFixtureFamiliesResolveExactlyWithSignsCanonical(GameTestHelper helper) {
        List<AuditedShapeFamily> actual = AuditedShapeFamilies.families(
                AuditedShapeFamily.Category.DISPLAY_FIXTURE);
        assertExactFamilies(helper, "C8 display fixtures", actual.stream()
                .filter(f -> Set.of("minecraft","enderscape").contains(f.canonicalParent().getNamespace())).toList(), C8_DISPLAY_FIXTURE_FAMILIES);

        for (AuditedShapeFamily family : actual) {
            String path = family.canonicalParent().getPath();
            helper.assertTrue(path.endsWith("_sign") && !path.endsWith("_hanging_sign"),
                    "Display-fixture canonical parent is not its standing Sign: "
                            + family.key() + " -> " + family.canonicalParent());
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void c9EnderscapeFenceAccessoryAndShadolineFamiliesResolveExactly(GameTestHelper helper) {
        assertExactFamilies(helper, "C9 Enderscape fence/gate families",
                providerFamilies(AuditedShapeFamily.Category.FENCE_GATE, "enderscape"),
                C9_ENDERSCAPE_FENCE_GATE_FAMILIES);
        assertExactFamilies(helper, "C8 Enderscape button/plate families",
                providerFamilies(AuditedShapeFamily.Category.BUILDING_ACCESSORY, "enderscape"),
                C8_ENDERSCAPE_BUILDING_ACCESSORY_FAMILIES);
        assertExactFamilies(helper, "C8 Enderscape Shadoline bars/chain family",
                providerFamilies(AuditedShapeFamily.Category.BAR_CHAIN, "enderscape"),
                C8_ENDERSCAPE_BAR_CHAIN_FAMILIES);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void c9BbbEnderscapeItemsExistResolveExactlyAndExcludeOrdinaryBuildingForms(
            GameTestHelper helper) {
        Set<Identifier> approved = approvedIds();
        for (String material : List.of("veiled", "celestial", "murublight")) {
            for (String form : List.of("frame", "lattice", "trim", "balustrade", "support", "pallet")) {
                Identifier itemId = id("bbb:" + material + "_" + form);
                helper.assertTrue(BuiltInRegistries.ITEM.containsKey(itemId),
                        "BBB dev.7 is missing audited Enderscape item " + itemId);
            }

            assertExactShapeSet(helper, "enderscape:" + material + "_fence", List.of(
                    "enderscape:" + material + "_fence", "enderscape:" + material + "_fence_gate",
                    "bbb:" + material + "_frame", "bbb:" + material + "_lattice"));
            assertExactShapeSet(helper, "bbb:" + material + "_trim", List.of(
                    "bbb:" + material + "_trim", "bbb:" + material + "_balustrade",
                    "bbb:" + material + "_support", "bbb:" + material + "_pallet"));

            for (String excluded : List.of("wall", "beam", "beam_stairs", "beam_slab", "lantern")) {
                Identifier itemId = id("bbb:" + material + "_" + excluded);
                helper.assertTrue(BuiltInRegistries.ITEM.containsKey(itemId),
                        "BBB dev.7 lost ordinary building form " + itemId);
                helper.assertTrue(!approved.contains(itemId),
                        "Ordinary BBB building form entered IBF " + itemId);
                Set<Identifier> overlap = new HashSet<>(ids(ShapeMap.getShapes(requiredItem(itemId))));
                overlap.retainAll(approved);
                helper.assertTrue(overlap.isEmpty(),
                        "Ordinary BBB building form joined an IBF ShapeMap family " + itemId + " -> " + overlap);
            }
        }

        assertExactFamilies(helper, "C9 BBB Enderscape detail families",
                AuditedShapeFamilies.families(AuditedShapeFamily.Category.BBB_DETAIL).stream()
                        .filter(family -> family.key().getPath().startsWith("cnm/bbb_detail/wood/enderscape_"))
                        .toList(),
                C9_BBB_ENDERSCAPE_DETAIL_FAMILIES);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void c8WallSignPlacementBlocksRemainBlockOnlyAndUncataloged(GameTestHelper helper) {
        helper.assertTrue(C8_WALL_ONLY_SIGN_BLOCKS.size() == 30
                        && new HashSet<>(C8_WALL_ONLY_SIGN_BLOCKS).size() == 30,
                "C8 wall-only sign fixture must contain exactly 30 distinct IDs");
        Set<Identifier> approved = approvedIds();
        for (String value : C8_WALL_ONLY_SIGN_BLOCKS) {
            Identifier blockId = id(value);
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(blockId),
                    "Pinned provider is missing wall-only placement block " + blockId);
            helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(blockId),
                    "Wall-only placement block unexpectedly has an inventory item " + blockId);
            helper.assertTrue(!approved.contains(blockId),
                    "Wall-only placement block entered the literal catalog " + blockId);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void enderscapeTrapdoorsStaySeparateFromDoorsAndDisplayFixtures(GameTestHelper helper) {
        for (String wood : List.of("veiled", "celestial", "murublight")) {
            String trapdoor = "enderscape:" + wood + "_trapdoor";
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id(trapdoor)), "Missing block " + trapdoor);
            assertSeparate(helper, trapdoor, "enderscape:" + wood + "_door", "Trapdoor joined full-height doors");
            assertSeparate(helper, trapdoor, "enderscape:" + wood + "_sign", "Trapdoor joined display fixtures");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void c8DisplayComponentsAndTransfersPreservePatchesAndFailClosed(GameTestHelper helper) {
        Item veiledSign = requiredItem("enderscape:veiled_sign");
        Item veiledHangingSign = requiredItem("enderscape:veiled_hanging_sign");
        Item veiledShelf = requiredItem("enderscape:veiled_shelf");
        Item celestialShelf = requiredItem("enderscape:celestial_shelf");

        ItemStack equalSign = new ItemStack(veiledSign, 8);
        equalSign.set(DataComponents.CUSTOM_NAME, Component.literal("Display"));
        ItemStack equalShelf = new ItemStack(veiledShelf, 5);
        equalShelf.set(DataComponents.CUSTOM_NAME, Component.literal("Display"));
        helper.assertTrue(ItemStack.isSameItemSameComponents(equalSign, equalShelf),
                "Equal display-fixture component patches were rejected");

        ItemStack incompatibleHangingSign = new ItemStack(veiledHangingSign, 8);
        incompatibleHangingSign.set(DataComponents.CUSTOM_NAME, Component.literal("Left"));
        ItemStack incompatibleShelf = new ItemStack(veiledShelf, 5);
        incompatibleShelf.set(DataComponents.CUSTOM_NAME, Component.literal("Right"));
        helper.assertTrue(!ItemStack.isSameItemSameComponents(
                        incompatibleHangingSign, incompatibleShelf),
                "Incompatible display-fixture component patches merged");

        ItemStack crossMaterialSign = new ItemStack(veiledSign, 8);
        crossMaterialSign.set(DataComponents.CUSTOM_NAME, Component.literal("Shared"));
        ItemStack crossMaterialShelf = new ItemStack(celestialShelf, 5);
        crossMaterialShelf.set(DataComponents.CUSTOM_NAME, Component.literal("Shared"));
        helper.assertTrue(!ItemStack.isSameItemSameComponents(
                        crossMaterialSign, crossMaterialShelf),
                "Equal patches merged across two display-fixture materials");

        ItemStack source = new ItemStack(veiledShelf, 19);
        source.set(DataComponents.CUSTOM_NAME, Component.literal("Gallery Display"));
        ItemStack first = ShapeMap.transferStack(source, 0);
        ItemStack middle = ShapeMap.transferStack(source, 1);
        ItemStack last = ShapeMap.transferStack(middle, 2);
        assertTransferred(helper, source, first, veiledSign,
                "Veiled display transfer to first failed");
        assertTransferred(helper, source, middle, veiledHangingSign,
                "Veiled display transfer to middle failed");
        assertTransferred(helper, middle, last, veiledShelf,
                "Veiled display transfer to last failed");

        List<Item> resolved = ShapeMap.getShapes(veiledSign);
        helper.assertTrue(resolved.equals(List.of(veiledSign, veiledHangingSign, veiledShelf)),
                "Veiled display fixture changed before fail-closed transfer proof");
        Map<Item, List<Item>> mutableShapes = mutableShapeMapForGuardTest();
        List<Item> originalMembers = mutableShapes.put(veiledSign,
                List.of(veiledSign, celestialShelf, veiledShelf));
        try {
            ItemStack rejected = ShapeMap.transferStack(source, 1);
            helper.assertTrue(rejected.getItem() == source.getItem()
                            && rejected.getCount() == source.getCount()
                            && rejected.getComponentsPatch().equals(source.getComponentsPatch()),
                    "Transfer guard did not fail closed on a cross-family display target");
        } finally {
            if (originalMembers == null) {
                mutableShapes.remove(veiledSign);
            } else {
                mutableShapes.put(veiledSign, originalMembers);
            }
        }
        helper.assertTrue(originalMembers != null
                        && ShapeMap.getShapes(veiledSign).get(1) == veiledHangingSign,
                "Veiled display fixture was not restored after fail-closed transfer proof");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void barsChainsCopperFinishesAndAccessoryMaterialsResolveLiterally(GameTestHelper helper) {
        assertExactShapeSet(helper, "minecraft:iron_bars", List.of(
                "minecraft:iron_bars", "bbb:iron_fence", "minecraft:iron_chain", "auroraslanterns:chandelier/iron"));
        for (String prefix : List.of(
                "copper", "exposed_copper", "weathered_copper", "oxidized_copper",
                "waxed_copper", "waxed_exposed_copper", "waxed_weathered_copper",
                "waxed_oxidized_copper")) {
            assertExactShapeSet(helper, "minecraft:" + prefix + "_bars", List.of(
                    "minecraft:" + prefix + "_bars",
                    "minecraft:" + prefix + "_chain",
                    "auroraslanterns:chandelier/" + prefix));
        }

        assertExactShapeSet(helper, "minecraft:oak_button", List.of(
                "minecraft:oak_button",
                "minecraft:oak_pressure_plate",
                "mcwpaths:oak_planks_path",
                "mcwwindows:oak_log_parapet",
                "mcwwindows:oak_plank_parapet",
                "mcwwindows:oak_blinds",
                "mcwwindows:oak_curtain_rod",
                "mcwwindows:oak_shutter",
                "mcwwindows:oak_louvered_shutter", "dramaticdoors:short_oak_door"));
        assertExactShapeSet(helper, "mcwpaths:stone_running_bond_path", List.of(
                "mcwpaths:stone_running_bond_path",
                "mcwpaths:stone_strewn_rocky_path",
                "mcwpaths:stone_windmill_weave_path",
                "mcwpaths:stone_flagstone_path",
                "mcwpaths:stone_crystal_floor_path",
                "mcwpaths:cobblestone_diamond_paving",
                "mcwpaths:cobblestone_basket_weave_paving",
                "mcwpaths:cobblestone_square_paving",
                "mcwpaths:cobblestone_honeycomb_paving",
                "mcwpaths:cobblestone_clover_paving",
                "mcwpaths:cobblestone_dumble_paving"));
        assertExactShapeSet(helper, "mcwpaths:andesite_running_bond_path", List.of(
                "mcwpaths:andesite_running_bond_path",
                "mcwpaths:andesite_strewn_rocky_path",
                "mcwpaths:andesite_windmill_weave_path",
                "mcwpaths:andesite_flagstone_path",
                "mcwpaths:andesite_crystal_floor_path",
                "mcwpaths:andesite_diamond_paving",
                "mcwpaths:andesite_basket_weave_paving",
                "mcwpaths:andesite_square_paving",
                "mcwpaths:andesite_honeycomb_paving",
                "mcwpaths:andesite_clover_paving",
                "mcwpaths:andesite_dumble_paving"));
        checkMossyOak(helper);
        helper.assertTrue(AuditedShapeFamilies.families().stream().noneMatch(family -> family.members()
                        .contains(requiredItem("mcwwindows:golden_curtain_rod"))),
                "Gold curtain rod acquired a family without a Button parent");

        assertSeparate(helper, "minecraft:copper_bars", "minecraft:exposed_copper_bars",
                "copper oxidation stages joined");
        assertSeparate(helper, "minecraft:copper_bars", "minecraft:waxed_copper_bars",
                "waxed and unwaxed copper joined");
        assertSeparate(helper, "minecraft:iron_bars", "minecraft:heavy_weighted_pressure_plate",
                "bar/chain and small-accessory scopes joined");
        assertSeparate(helper, "minecraft:oak_button", "minecraft:spruce_button",
                "wood accessory materials joined");

        for (String value : List.of(
                "minecraft:iron_chain",
                "minecraft:waxed_oxidized_copper_chain",
                "auroraslanterns:chandelier/waxed_oxidized_copper",
                "ribbits:mossy_oak_planks_fence_gate",
                "mcwpaths:oak_planks_path",
                "mcwwindows:oak_curtain_rod",
                "mcwpaths:andesite_crystal_floor_path",
                "mcwpaths:andesite_dumble_paving")) {
            helper.assertTrue(!AuditedShapeRuntime.isAuditedAlternateDoor(requiredItem(value)),
                    "New non-door member inherited door-only loot handling: " + value);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void bbbWoodAndStoneFamiliesResolveAsOneLiteralFamilyEach(GameTestHelper helper) {
        for (String wood : List.of(
                "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                "crimson", "warped", "mangrove", "bamboo", "cherry", "pale_oak")) {
            assertExactShapeSet(helper, "bbb:" + wood + "_trim", List.of(
                    "bbb:" + wood + "_trim", "bbb:" + wood + "_balustrade",
                    "bbb:" + wood + "_support", "bbb:" + wood + "_pallet"));
            assertExactShapeSet(helper, "minecraft:" + wood + "_fence", List.of(
                    "minecraft:" + wood + "_fence", "minecraft:" + wood + "_fence_gate",
                    "bbb:" + wood + "_frame", "bbb:" + wood + "_lattice"));
        }
        checkMossyOak(helper);
        for (String stone : List.of(
                "stone", "blackstone", "deepslate", "nether_brick", "sandstone", "red_sandstone", "quartz")) {
            List<String> expected = new java.util.ArrayList<>(List.of(
                    "bbb:" + stone + "_column", "bbb:" + stone + "_urn",
                    "bbb:" + stone + "_moulding", "bbb:" + stone + "_fence", "bbb:" + stone + "_frame"));
            if (stone.equals("stone")) expected.add("mcwwindows:stone_brick_arrow_slit");
            if (stone.equals("nether_brick")) expected.add("mcwwindows:nether_brick_arrow_slit");
            if (Boolean.getBoolean("ibf.test.amcExpected")
                    && Set.of("sandstone", "red_sandstone", "deepslate", "quartz").contains(stone)) {
                expected.add("architectural_material_closure:" + stone + "_arrow_slit");
            }
            assertExactShapeSet(helper, "bbb:" + stone + "_column", expected);
        }
        helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(id("bbb:mossy_oak_frame"))
                        && !BuiltInRegistries.ITEM.containsKey(id("bbb:mossy_oak_lattice")),
                "BBB unexpectedly registered excluded Mossy Oak frame/lattice variants");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void recipeCleanupRemovesNewNonParentsAndRetainsCanonicalAcquisition(GameTestHelper helper) {
        for (String removed : List.of(
                "minecraft:iron_chain",
                "minecraft:copper_chain",
                "minecraft:waxed_copper_chain_from_honeycomb",
                "minecraft:waxed_exposed_copper_chain_from_honeycomb",
                "minecraft:waxed_weathered_copper_chain_from_honeycomb",
                "minecraft:waxed_oxidized_copper_chain_from_honeycomb",
                "minecraft:oak_pressure_plate",
                "mcwpaths:oak_planks_path",
                "mcwpaths:stone_strewn_rocky_path",
                "mcwwindows:oak_log_parapet",
                "mcwwindows:oak_louvered_shutter",
                "mcwwindows:stone_brick_gothic",
                // A non-IBF CNM alternate remains governed by CNM cleanup;
                // the canonical-parent guard must not broaden beyond IBF.
                "minecraft:stone_slab")) {
            helper.assertTrue(!hasRecipe(helper, removed),
                    "CNM retained a new-family non-parent recipe: " + removed);
        }
        List<String> missingCanonicalRecipes = List.of(
                "minecraft:iron_bars",
                "minecraft:copper_bars",
                "minecraft:waxed_copper_bars_from_honeycomb",
                "minecraft:oak_button",
                "minecraft:stone_button",
                "mcwpaths:andesite_running_bond_path").stream()
                .filter(retained -> !hasRecipe(helper, retained))
                .toList();
        helper.assertTrue(missingCanonicalRecipes.isEmpty(),
                "CNM removed intended canonical acquisition recipes: " + missingCanonicalRecipes);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void c9RecipeCleanupPreserves33CanonicalAnd5ProviderConversionsAndRemoves56CraftingAlternates(
            GameTestHelper helper) {
        helper.assertTrue(C9_CANONICAL_RESULT_RECIPE_IDS.size() == 33
                        && new HashSet<>(C9_CANONICAL_RESULT_RECIPE_IDS).size() == 33,
                "C9 canonical-result recipe fixture must contain exactly 33 distinct IDs");
        helper.assertTrue(C9_REMOVED_ALTERNATE_RECIPE_IDS.size() == 56
                        && new HashSet<>(C9_REMOVED_ALTERNATE_RECIPE_IDS).size() == 56,
                "C9 removed-alternate recipe fixture must contain exactly 56 distinct IDs");
        helper.assertTrue(C9_RETAINED_ALTERNATE_PROVIDER_RECIPE_IDS.size() == 5
                        && new HashSet<>(C9_RETAINED_ALTERNATE_PROVIDER_RECIPE_IDS).size() == 5,
                "C9 retained provider-conversion fixture must contain exactly 5 distinct IDs");
        Set<String> overlap = new HashSet<>(C9_CANONICAL_RESULT_RECIPE_IDS);
        overlap.retainAll(C9_REMOVED_ALTERNATE_RECIPE_IDS);
        helper.assertTrue(overlap.isEmpty(),
                "C8 canonical and alternate recipe fixtures overlap: " + overlap);

        List<String> missingCanonicalRecipes = C9_CANONICAL_RESULT_RECIPE_IDS.stream()
                .filter(recipeId -> !hasRecipe(helper, recipeId))
                .toList();
        helper.assertTrue(missingCanonicalRecipes.isEmpty(),
                "CNM removed C9 canonical-result recipes: " + missingCanonicalRecipes);

        List<String> retainedAlternateRecipes = C9_REMOVED_ALTERNATE_RECIPE_IDS.stream()
                .filter(recipeId -> hasRecipe(helper, recipeId))
                .toList();
        helper.assertTrue(retainedAlternateRecipes.isEmpty(),
                "CNM retained C9 alternate-result crafting recipes: " + retainedAlternateRecipes);

        List<String> missingProviderConversions = C9_RETAINED_ALTERNATE_PROVIDER_RECIPE_IDS.stream()
                .filter(recipeId -> !hasRecipe(helper, recipeId))
                .toList();
        helper.assertTrue(missingProviderConversions.isEmpty(),
                "CNM removed Enderscape custom conversion recipes it cannot re-encode: "
                        + missingProviderConversions);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void categoriesMaterialsAndAuditedExclusionsRemainIsolated(GameTestHelper helper) {
        List<AuditedShapeFamily> families = expectedActiveFamilies();
        Set<Identifier> approved = approvedIds();
        Set<Item> resolvedParents = new HashSet<>();

        for (AuditedShapeFamily family : families) {
            Item representative = requiredItem(family.availableMembers(Boolean.getBoolean("ibf.test.amcExpected")).stream().filter(BuiltInRegistries.ITEM::containsKey).skip(1).findFirst().orElseThrow());
            Item resolvedParent = ShapeMap.getParent(representative);
            helper.assertTrue(resolvedParents.add(resolvedParent),
                    "Two approved families joined one ShapeMap component at " + family.key());
        }

        assertSeparate(helper, "minecraft:oak_door", "dramaticdoors:tall_oak_door",
                "2-high and 3-high oak doors joined");
        assertSeparate(helper, "minecraft:oak_door", "minecraft:spruce_door",
                "oak and spruce door materials joined");
        assertSeparate(helper, "minecraft:oak_door", "minecraft:oak_trapdoor",
                "door and trapdoor categories joined");
        assertSeparate(helper, "minecraft:oak_trapdoor", "mcwwindows:oak_window",
                "trapdoor/shutter and framed-window categories joined");
        assertSeparate(helper, "mcwwindows:oak_window", "mcwwindows:oak_plank_window",
                "oak-log and oak-plank window materials joined");
        assertSeparate(helper, "minecraft:oak_fence", "minecraft:spruce_fence_gate",
                "oak and spruce fence/gate materials joined");
        assertSeparate(helper, "minecraft:oak_fence", "ribbits:mossy_oak_planks_fence",
                "Ribbits Mossy Oak fence/gate joined vanilla oak");

        for (String value : REGISTERED_EXCLUSIONS) {
            Identifier id = id(value);
            helper.assertTrue(!approved.contains(id), "Audit exclusion entered the literal catalog: " + id);
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id),
                    "Pinned provider no longer registers expected audited exclusion: " + id);
            Set<Identifier> intersection = new HashSet<>(ids(ShapeMap.getShapes(requiredItem(id))));
            intersection.retainAll(approved);
            helper.assertTrue(intersection.isEmpty(),
                    "Audit exclusion joined an approved ShapeMap component: " + id + " -> " + intersection);
        }

        for (String value : UNREGISTERED_EXCLUSIONS) {
            Identifier id = id(value);
            helper.assertTrue(!approved.contains(id), "Unregistered audit exclusion entered the catalog: " + id);
            helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(id),
                    "Pinned provider unexpectedly registered excluded residue/constant: " + id);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void preResolutionCollisionGuardRejectsOutsideAndCrossFamilyEdges(GameTestHelper helper) {
        Item oakDoor = requiredItem("minecraft:oak_door");
        Item spruceDoor = requiredItem("minecraft:spruce_door");
        Item outside = requiredItem("minecraft:stone");
        Identifier syntheticSource = id("interchangeable_block_families_runtime_tests:synthetic_overlap");

        List<ShapeMap.Mapping> outsideOverlap = new ArrayList<>();
        outsideOverlap.add(new ShapeMap.Mapping(oakDoor, outside, 2_000, syntheticSource));
        helper.assertTrue(rejectedByPreResolutionGuard(outsideOverlap),
                "Pre-resolution guard accepted an audited-item to outside-item edge");
        helper.assertTrue(outsideOverlap.size() == 1,
                "IBF mappings were appended before the outside collision was rejected");

        List<ShapeMap.Mapping> crossFamilyOverlap = new ArrayList<>();
        crossFamilyOverlap.add(new ShapeMap.Mapping(oakDoor, spruceDoor, 2_000, syntheticSource));
        helper.assertTrue(rejectedByPreResolutionGuard(crossFamilyOverlap),
                "Pre-resolution guard accepted an edge between two approved families");
        helper.assertTrue(crossFamilyOverlap.size() == 1,
                "IBF mappings were appended before the cross-family collision was rejected");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void largestFamilyTransfersFirstToMiddleToLastWithoutLosingPatch(GameTestHelper helper) {
        int expectedSize = Boolean.getBoolean("ibf.test.amcExpected") ? 23 : 22;
        AuditedShapeFamily family = expectedActiveFamilies().stream()
                .filter(candidate -> candidate.members().size() == expectedSize)
                .findFirst()
                .orElseThrow();
        List<Item> actual = ShapeMap.getShapes(requiredItem(family.canonicalParent()));
        int middleIndex = actual.size() / 2;
        int lastIndex = actual.size() - 1;

        ItemStack first = new ItemStack(actual.getFirst(), 37);
        first.set(DataComponents.CUSTOM_NAME, Component.literal("IBF transfer sentinel"));
        ItemStack middle = ShapeMap.transferStack(first, middleIndex);
        ItemStack last = ShapeMap.transferStack(middle, lastIndex);

        helper.assertTrue(actual.size() == expectedSize, "Wrong active large-family size: " + actual.size());
        assertTransferred(helper, first, middle, actual.get(middleIndex),
                "first-to-middle transfer failed");
        assertTransferred(helper, middle, last, actual.get(lastIndex),
                "middle-to-last transfer failed");
        helper.assertTrue(ShapeMap.currentIndex(first) == 0
                        && ShapeMap.currentIndex(middle) == middleIndex
                        && ShapeMap.currentIndex(last) == lastIndex,
                "Large-family transfer indexes changed");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void ibfComponentGuardAllowsEqualPatchesAndRejectsUnequalPatches(GameTestHelper helper) {
        Item parent = requiredItem("minecraft:oak_door");
        Item firstShape = requiredItem("mcwdoors:oak_barn_door");
        Item secondShape = requiredItem("mcwdoors:oak_glass_door");

        ItemStack defaultParent = new ItemStack(parent, 8);
        ItemStack defaultShape = new ItemStack(firstShape, 5);
        helper.assertTrue(ItemStack.isSameItemSameComponents(defaultParent, defaultShape),
                "Default stacks from one audited family no longer merge");

        ItemStack equalParent = new ItemStack(parent, 8);
        equalParent.set(DataComponents.CUSTOM_NAME, Component.literal("shared patch"));
        ItemStack equalShape = new ItemStack(firstShape, 5);
        equalShape.set(DataComponents.CUSTOM_NAME, Component.literal("shared patch"));
        helper.assertTrue(equalParent.getComponentsPatch().equals(equalShape.getComponentsPatch())
                        && ItemStack.isSameItemSameComponents(equalParent, equalShape),
                "Equal custom patches from one audited family were rejected");

        ItemStack distinctParent = new ItemStack(parent, 8);
        distinctParent.set(DataComponents.CUSTOM_NAME, Component.literal("destination patch"));
        ItemStack distinctShape = new ItemStack(firstShape, 5);
        distinctShape.set(DataComponents.CUSTOM_NAME, Component.literal("source patch"));
        helper.assertTrue(!ItemStack.isSameItemSameComponents(distinctParent, distinctShape),
                "Unequal custom patches merged across audited members");

        ItemStack sameNonParentA = new ItemStack(secondShape, 8);
        sameNonParentA.set(DataComponents.CUSTOM_NAME, Component.literal("non-parent A"));
        ItemStack sameNonParentB = new ItemStack(secondShape, 5);
        sameNonParentB.set(DataComponents.CUSTOM_NAME, Component.literal("non-parent B"));
        helper.assertTrue(!ItemStack.isSameItemSameComponents(sameNonParentA, sameNonParentB),
                "Unequal patches merged for the same audited non-parent item");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void qsnResolverSeesOakEquivalenceButRejectsSpruce(GameTestHelper helper) {
        Item oakParent = requiredItem("minecraft:oak_door");
        Item oakShape = requiredItem("mcwdoors:oak_barn_door");
        Item spruceShape = requiredItem("mcwdoors:spruce_barn_door");

        helper.assertTrue(ShapeMap.inSameShapeSet(oakParent, oakShape),
                "CNM did not expose the representative oak IBF relationship");
        helper.assertTrue(qsnInSameShapeSet(oakParent, oakShape),
                "Accepted QSN CNM resolver did not admit a same-oak-family source");
        helper.assertTrue(!ShapeMap.inSameShapeSet(oakParent, spruceShape)
                        && !qsnInSameShapeSet(oakParent, spruceShape),
                "Accepted QSN CNM resolver admitted a different-material source");
        helper.assertTrue(ItemStack.isSameItemSameComponents(
                        new ItemStack(oakParent), new ItemStack(oakShape))
                        && !ItemStack.isSameItemSameComponents(
                        new ItemStack(oakParent), new ItemStack(spruceShape)),
                "The ItemStack equality used by QSN disagrees with representative ShapeMap affinity");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void acceptedNibaruComponentRemainsExactAndUnjoined(GameTestHelper helper) {
        List<Identifier> expected = new ArrayList<>(List.of(
                id("minecraft:sea_lantern"),
                id("more_slabs_stairs_and_walls:sea_lantern_slab"),
                id("more_slabs_stairs_and_walls:sea_lantern_stairs"),
                id("more_slabs_stairs_and_walls:sea_lantern_wall"),
                id("clutternomore:more_slabs_stairs_and_walls/vertical_sea_lantern_slab"),
                id("clutternomore:more_slabs_stairs_and_walls/sea_lantern_step")
        ));
        if (Boolean.getBoolean("ibf.test.unifiedBge")) {
            for (String role : List.of("corner", "quarter_column", "layer")) {
                expected.add(id("cnm_terrain_slabs_compat:minecraft/sea_lantern_" + role));
            }
        }
        Item parent = requiredItem(expected.getFirst());
        List<Identifier> actual = ids(ShapeMap.getShapes(parent));
        helper.assertTrue(actual.equals(expected),
                "Accepted sea-lantern CNM/Nibaru component changed: expected="
                        + expected + ", actual=" + actual);

        Set<Identifier> approved = approvedIds();
        Set<Identifier> intersection = new HashSet<>(actual);
        intersection.retainAll(approved);
        helper.assertTrue(intersection.isEmpty(),
                "Accepted CNM/Nibaru component joined IBF members: " + intersection);
        for (Identifier member : expected) {
            helper.assertTrue(ShapeMap.getParent(requiredItem(member)) == parent,
                    "Accepted CNM/Nibaru parent changed for " + member);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void alternateTwoHighDoorsDropOneMatchingDesignFromEverySegment(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        for (String doorId : List.of(
                "mcwdoors:oak_bamboo_door",
                "mcwdoors:oak_stable_door",
                "mcwdoors:oak_japanese_door"
        )) {
            for (int segment = 0; segment < 2; segment++) {
                assertPlacedDrop(helper, player, doorId, doorId, 2, segment);
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void alternateThreeHighDoorsDropOneMatchingDesignFromEverySegment(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        for (String doorId : List.of(
                "dramaticdoors:tall_macaw_oak_bamboo_door",
                "dramaticdoors:tall_macaw_oak_stable_door",
                "dramaticdoors:tall_macaw_oak_japanese_door"
        )) {
            for (int segment = 0; segment < 3; segment++) {
                assertPlacedDrop(helper, player, doorId, doorId, 3, segment);
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void alternateTwoHighDoorsDropOneMatchingDesignWhenSupportIsLost(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        for (String doorId : List.of(
                "mcwdoors:oak_bamboo_door",
                "mcwdoors:oak_stable_door",
                "mcwdoors:oak_japanese_door"
        )) {
            assertSupportLossDrop(helper, player, doorId, doorId, 2);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void alternateThreeHighDoorsDropOneMatchingDesignWhenSupportIsLost(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        for (String doorId : List.of(
                "dramaticdoors:tall_macaw_oak_bamboo_door",
                "dramaticdoors:tall_macaw_oak_stable_door",
                "dramaticdoors:tall_macaw_oak_japanese_door"
        )) {
            assertSupportLossDrop(helper, player, doorId, doorId, 3);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void canonicalDoorControlsStillDropOnce(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        for (int segment = 0; segment < 2; segment++) {
            assertPlacedDrop(helper, player,
                    "minecraft:oak_door", "minecraft:oak_door", 2, segment);
        }
        assertPlacedDrop(helper, player,
                "dramaticdoors:tall_oak_door", "dramaticdoors:tall_oak_door", 3, 0);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void canonicalDoorsDropOnceWhenSupportIsLost(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        assertSupportLossDrop(helper, player,
                "minecraft:oak_door", "minecraft:oak_door", 2);
        assertSupportLossDrop(helper, player,
                "dramaticdoors:tall_oak_door", "dramaticdoors:tall_oak_door", 3);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void nonDoorShapeDropsRemainUnchanged(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        assertPlacedDrop(helper, player,
                "mcwtrpdoors:oak_bamboo_trapdoor", "minecraft:oak_trapdoor", 1, 0);
        assertPlacedDrop(helper, player,
                "mcwwindows:oak_window2", "mcwwindows:oak_window", 1, 0);
        assertPlacedDrop(helper, player,
                "minecraft:oak_fence_gate", "minecraft:oak_fence", 1, 0);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void unrelatedSupportLossDropsRemainUnchanged(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        assertSupportLossDrop(helper, player,
                "minecraft:torch", "minecraft:torch", 1);
        assertSupportLossDrop(helper, player,
                "minecraft:sunflower", "minecraft:sunflower", 2);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void cnmDisabledModeClearsShapeMapWithoutProviderMutation(GameTestHelper helper) {
        Map<Item, List<Item>> shapesSnapshot = new LinkedHashMap<>();
        ShapeMap.shapesView().forEach((parent, members) ->
                shapesSnapshot.put(parent, List.copyOf(members)));
        Map<Item, Item> inverseSnapshot = new IdentityHashMap<>();
        inverseSnapshot.putAll(ShapeMap.inverseView());
        Item auditedItem = requiredItem("minecraft:oak_door");

        try {
            ShapeMap.setMappings(List.of(), false);
            helper.assertTrue(ShapeMap.shapesView().isEmpty()
                            && ShapeMap.inverseView().isEmpty(),
                    "CNM disabled mode did not leave an empty ShapeMap");
            helper.assertTrue(!AuditedShapeRuntime.isAudited(auditedItem),
                    "IBF runtime membership remained active while CNM ShapeMap was disabled");
        } finally {
            ShapeMap.setShapeMaps(shapesSnapshot, inverseSnapshot);
        }

        helper.assertTrue(AuditedShapeRuntime.isAudited(auditedItem)
                        && ShapeMap.getShapes(auditedItem).size() == 22,
                "Enabled ShapeMap snapshot did not restore after the disabled-mode proof");
        helper.succeed();
    }

    private static List<AuditedShapeFamily> providerFamilies(
            AuditedShapeFamily.Category category,
            String namespace
    ) {
        return AuditedShapeFamilies.families(category).stream()
                .filter(family -> family.members().stream()
                        .anyMatch(member -> member.getNamespace().equals(namespace)))
                .toList();
    }

    private static void assertExactFamilies(
            GameTestHelper helper,
            String label,
            List<AuditedShapeFamily> actualFamilies,
            List<List<String>> expectedFamilies
    ) {
        helper.assertTrue(actualFamilies.size() == expectedFamilies.size(),
                label + " count changed: expected=" + expectedFamilies.size()
                        + ", actual=" + actualFamilies.size());
        for (int index = 0; index < expectedFamilies.size(); index++) {
            AuditedShapeFamily actual = actualFamilies.get(index);
            List<String> expectedValues = expectedFamilies.get(index);
            List<Identifier> expectedIds = expectedValues.stream()
                    .map(AuditedShapeMapGameTests::id)
                    .toList();
            helper.assertTrue(actual.members().equals(expectedIds),
                    label + " family " + index + " changed: expected=" + expectedIds
                            + ", actual=" + actual.members());
            helper.assertTrue(actual.canonicalParent().equals(expectedIds.getFirst()),
                    label + " canonical parent changed for " + actual.key());
            List<String> active = expectedValues.stream()
                    .filter(value -> Boolean.getBoolean("ibf.test.amcExpected") || !value.startsWith("architectural_material_closure:"))
                    .toList();
            assertExactShapeSet(helper, active.getFirst(), active);
        }
    }

    private static void assertSeparate(GameTestHelper helper, String first, String second, String label) {
        Item firstItem = requiredItem(first);
        Item secondItem = requiredItem(second);
        helper.assertTrue(ShapeMap.getParent(firstItem) != ShapeMap.getParent(secondItem)
                        && !ShapeMap.inSameShapeSet(firstItem, secondItem),
                label + ": " + first + " / " + second);
    }

    private static int treeFamilyCount() {
        if (!BuiltInRegistries.ITEM.containsKey(id("mynx_trees:wisteria_sign"))) return 0;
        return Boolean.getBoolean("ibf.test.amcExpected") ? 20 : 6;
    }

    private static int treeItemCount() {
        if (treeFamilyCount() == 0) return 0;
        return Boolean.getBoolean("ibf.test.amcExpected") ? 194 : 16;
    }

    private static List<AuditedShapeFamily> expectedActiveFamilies() {
        return AuditedShapeFamilies.families().stream()
                .filter(family -> family.availableMembers(Boolean.getBoolean("ibf.test.amcExpected")).stream().filter(BuiltInRegistries.ITEM::containsKey).count() >= 2)
                .toList();
    }

    private static List<String> stoneAccessoryMembers() {
        return Boolean.getBoolean("ibf.test.amcExpected")
                ? List.of("minecraft:stone_button", "minecraft:stone_pressure_plate",
                        "architectural_material_closure:stone_parapet",
                        "architectural_material_closure:stone_louvered_shutter")
                : List.of("minecraft:stone_button", "minecraft:stone_pressure_plate");
    }

    @GameTest(maxTicks = 40)
    public void stoneAccessoriesSwitchInOrderAndReturnToButton(GameTestHelper helper) {
        helper.assertTrue(FabricLoader.getInstance().isModLoaded("architectural_material_closure")
                        == Boolean.getBoolean("ibf.test.amcExpected"),
                "AMC presence differs from the requested GameTest configuration");
        assertSelectorRoundTrip(helper, stoneAccessoryMembers());
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void masonryAccessoriesAndWindowsFollowTheirProviders(GameTestHelper helper) {
        assertSelectorRoundTrip(helper, List.of("bbb:stone_column", "bbb:stone_urn", "bbb:stone_moulding",
                "bbb:stone_fence", "bbb:stone_frame", "mcwwindows:stone_brick_arrow_slit"));
        helper.assertTrue(hasRecipe(helper, "mcwwindows:cobblestone_arrow_slit"), "Lost Cobblestone Arrow Slit recipe");

        for (String material : List.of("andesite", "diorite", "granite")) {
            String amc = "architectural_material_closure:" + material + "_";
            String macaw = "mcwwindows:" + material + "_";
            List<String> windows = new ArrayList<>(List.of(macaw + "window", macaw + "window2",
                    macaw + "four_window", macaw + "pane_window"));
            if (Boolean.getBoolean("ibf.test.amcExpected")) {
                assertSelectorRoundTrip(helper, List.of(amc + "button", amc + "pressure_plate",
                        macaw + "parapet", macaw + "louvered_shutter"));
                assertSelectorRoundTrip(helper, List.of(amc + "column", amc + "urn", amc + "moulding",
                        amc + "fence", amc + "frame", amc + "arrow_slit"));
                helper.assertTrue(hasRecipe(helper, amc + "column_from_stonecutting"), "Lost column stonecutting");
                for (String alternate : List.of("urn", "moulding", "fence")) {
                    helper.assertTrue(!hasRecipe(helper, amc + alternate + "_from_stonecutting"),
                            "Retained alternate stonecutting recipe for " + amc + alternate);
                }
                windows.add(amc + "gothic_window");
                assertSeparate(helper, amc + "button", "mcwpaths:" + material + "_running_bond_path",
                        "Accessory and path selectors merged");
            } else {
                for (String standalone : List.of(macaw + "parapet", macaw + "louvered_shutter")) {
                    Item item = requiredItem(standalone);
                    helper.assertTrue(!AuditedShapeRuntime.isAudited(item) && !ShapeMap.isShape(item)
                                    && hasRecipe(helper, standalone),
                            "Missing AMC parent changed standalone item " + standalone);
                }
                assertSeparate(helper, macaw + "parapet", macaw + "louvered_shutter",
                        "Missing AMC parent produced a partial selector");
            }
            assertSelectorRoundTrip(helper, windows);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void mudBricksSelectorsRequireTheirAmcParents(GameTestHelper helper) {
        String amc = "architectural_material_closure:mud_bricks_";
        String slit = "mcwwindows:mud_brick_arrow_slit";
        String gothic = "mcwwindows:mud_brick_gothic";
        if (Boolean.getBoolean("ibf.test.amcExpected")) {
            assertSelectorRoundTrip(helper, List.of(amc + "button", amc + "pressure_plate",
                    amc + "parapet", amc + "louvered_shutter"));
            assertSelectorRoundTrip(helper, List.of(amc + "column", amc + "urn", amc + "moulding",
                    amc + "fence", amc + "frame", slit));
            assertSelectorRoundTrip(helper, List.of(amc + "window", amc + "window2", amc + "four_window",
                    amc + "pane_window", gothic));
            helper.assertTrue(hasRecipe(helper, amc + "column_from_stonecutting"), "Lost Mud Bricks Column stonecutting");
            for (String alternate : List.of("urn", "moulding", "fence")) {
                helper.assertTrue(!hasRecipe(helper, amc + alternate + "_from_stonecutting"),
                        "Retained alternate stonecutting recipe for " + alternate);
            }
            assertSeparate(helper, amc + "button", "mcwpaths:mud_brick_running_bond_path",
                    "Mud Bricks accessories merged with Paths");
        } else {
            for (String standalone : List.of(slit, gothic)) {
                Item item = requiredItem(standalone);
                helper.assertTrue(!AuditedShapeRuntime.isAudited(item) && !ShapeMap.isShape(item)
                                && hasRecipe(helper, standalone),
                        "Absent AMC changed standalone Macaw item " + standalone);
            }
            assertSeparate(helper, slit, gothic, "Mud Brick Arrow Slit and Gothic Window joined without AMC");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void stoneAccessoriesExtendMacawWindowsAndBbbDetails(GameTestHelper helper) {
        for (String material : List.of("sandstone", "red_sandstone", "deepslate", "quartz")) {
            String amc = "architectural_material_closure:" + material + "_";
            String macaw = "mcwwindows:" + material + "_";
            String bbb = "bbb:" + material + "_";
            String path = material.equals("quartz") ? amc + "running_bond_path" : "mcwpaths:" + material + "_running_bond_path";
            List<String> windows = new ArrayList<>(List.of(macaw + "window", macaw + "window2",
                    macaw + "four_window", macaw + "pane_window"));
            List<String> details = new ArrayList<>(List.of(bbb + "column", bbb + "urn", bbb + "moulding",
                    bbb + "fence", bbb + "frame"));
            if (Boolean.getBoolean("ibf.test.amcExpected")) {
                assertSelectorRoundTrip(helper, List.of(amc + "button", amc + "pressure_plate",
                        amc + "parapet", amc + "louvered_shutter"));
                assertSeparate(helper, amc + "button", path, "Accessory and path selectors merged for " + material);
                windows.add(amc + "gothic_window");
                details.add(amc + "arrow_slit");
            } else {
                for (String form : List.of("button", "pressure_plate", "parapet", "louvered_shutter",
                        "gothic_window", "arrow_slit")) {
                    helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(id(amc + form)), "Unexpected item " + amc + form);
                }
            }
            assertSelectorRoundTrip(helper, windows);
            assertSelectorRoundTrip(helper, details);
            helper.assertTrue(hasRecipe(helper, path) == (!material.equals("quartz") || Boolean.getBoolean("ibf.test.amcExpected")),
                    "Unexpected path recipe availability for " + path);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void prismarineFamiliesFollowTheirProviders(GameTestHelper helper) {
        boolean amcPresent = Boolean.getBoolean("ibf.test.amcExpected");
        for (String material : List.of("prismarine_bricks", "dark_prismarine")) {
            String prefix = "architectural_material_closure:" + material + "_";
            String macaw = material.equals("prismarine_bricks") ? "mcwwindows:prismarine" : "mcwwindows:dark_prismarine";
            String parapet = macaw + "_parapet";
            String slit = macaw + "_brick_arrow_slit";
            if (amcPresent) {
                assertSelectorRoundTrip(helper, List.of(prefix + "button", prefix + "pressure_plate",
                        parapet, prefix + "louvered_shutter"));
                assertSelectorRoundTrip(helper, List.of(prefix + "column", prefix + "urn", prefix + "moulding",
                        prefix + "fence", prefix + "frame", slit));
                helper.assertTrue(hasRecipe(helper, prefix + "column_from_stonecutting"), "Lost column stonecutting");
                for (String alternate : List.of("urn", "moulding", "fence")) {
                    helper.assertTrue(!hasRecipe(helper, prefix + alternate + "_from_stonecutting"),
                            "Retained alternate stonecutting recipe for " + prefix + alternate);
                }
            } else {
                for (String standalone : List.of(parapet, slit)) {
                    Item item = requiredItem(standalone);
                    helper.assertTrue(!AuditedShapeRuntime.isAudited(item) && !ShapeMap.isShape(item)
                                    && hasRecipe(helper, standalone),
                            "Absent AMC changed standalone Macaw item " + standalone);
                }
                assertSeparate(helper, parapet, slit, "Unparented Prismarine selectors joined");
            }
        }

        if (amcPresent) {
            String prefix = "architectural_material_closure:prismarine_bricks_";
            assertPathsAndIndependentFullBlocks(helper, prefix, prefix + "button");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void netherBricksWindowsAndPatternedPathsFollowTheirProviders(GameTestHelper helper) {
        String nether = "architectural_material_closure:nether_bricks_";
        String gothic = "mcwwindows:nether_brick_gothic";
        if (Boolean.getBoolean("ibf.test.amcExpected")) {
            assertSelectorRoundTrip(helper, List.of(nether + "window", nether + "window2",
                    nether + "four_window", nether + "pane_window", gothic));
            assertSelectorRoundTrip(helper, List.of(nether + "button", nether + "pressure_plate",
                    nether + "parapet", nether + "louvered_shutter"));
            assertPathsAndIndependentFullBlocks(helper, "architectural_material_closure:quartz_", "architectural_material_closure:quartz_button");
            assertPathsAndIndependentFullBlocks(helper, nether, nether + "button");
        } else {
            Item item = requiredItem(gothic);
            helper.assertTrue(!AuditedShapeRuntime.isAudited(item) && !ShapeMap.isShape(item) && hasRecipe(helper, gothic),
                    "Nether Brick Gothic Window lost its independent recipe or joined a partial family");
            for (String parent : List.of(nether + "window", nether + "button", nether + "running_bond_path",
                    "architectural_material_closure:quartz_running_bond_path")) {
                helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(id(parent)), "Unexpected AMC parent " + parent);
            }
        }
        assertSeparate(helper, gothic, "mcwwindows:nether_brick_arrow_slit", "Nether Brick window and details joined");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void amcMaterialsKeepFourSelectorsAndIndependentPatternedBlocks(GameTestHelper helper) {
        boolean amcPresent = Boolean.getBoolean("ibf.test.amcExpected");
        for (String material : List.of("tuff", "cinnabar", "sulfur", "smooth_basalt", "purpur", "dusk_purpur", "resin")) {
            String prefix = "architectural_material_closure:" + material + "_";
            if (!amcPresent) {
                helper.assertTrue(BuiltInRegistries.ITEM.keySet().stream()
                        .noneMatch(key -> key.toString().startsWith(prefix)), "Unexpected AMC items for " + material);
                continue;
            }

            assertSelectorRoundTrip(helper, List.of(prefix + "button", prefix + "pressure_plate",
                    prefix + "parapet", prefix + "louvered_shutter"));
            assertMasonryWindowsPathsAndPatterns(helper, material, prefix, prefix + "button");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void optionalProviderMatrix(GameTestHelper helper) {
        var loader = FabricLoader.getInstance();
        boolean enderscape = loader.isModLoaded("enderscape");
        boolean amc = loader.isModLoaded("architectural_material_closure");
        helper.assertTrue(enderscape == Boolean.getBoolean("ibf.test.enderscapeExpected"), "Enderscape setup differs");
        helper.assertTrue(amc == Boolean.getBoolean("ibf.test.amcExpected"), "AMC setup differs");

        // Derive expected membership independently from the live provider registries.
        int families = 0;
        Set<Item> items = new LinkedHashSet<>();
        for (AuditedShapeFamily family : AuditedShapeFamilies.families()) {
            var registered = family.members().stream().filter(BuiltInRegistries.ITEM::containsKey).toList();
            if (!registered.contains(family.canonicalParent())) {
                for (Identifier id : registered) {
                    helper.assertTrue(!AuditedShapeRuntime.isAudited(requiredItem(id)), "Orphaned alternate " + id);
                }
                continue;
            }
            if (!amc && (family.requiresAmcDoorPack() || family.category() == AuditedShapeFamily.Category.TWO_HIGH_DOOR
                    || family.category() == AuditedShapeFamily.Category.TRAPDOOR)
                    && !family.canonicalParent().getNamespace().equals("minecraft")) {
                Item nativeDoor = requiredItem(family.canonicalParent());
                helper.assertTrue(registered.equals(List.of(family.canonicalParent()))
                        && !AuditedShapeRuntime.isAudited(nativeDoor) && !ShapeMap.isShape(nativeDoor),
                        "Unexpected standalone door selector " + family.key());
                continue;
            }
            helper.assertTrue(registered.size() >= 2, "Singleton selector " + family.key());
            assertExactShapeSet(helper, family.canonicalParent().toString(),
                    registered.stream().map(Identifier::toString).toList());
            for (Identifier id : registered) {
                Item item = requiredItem(id);
                helper.assertTrue(items.add(item) && AuditedShapeRuntime.isAudited(item), "Membership mismatch " + id);
            }
            if (family.key().getPath().contains("/mynx_trees_")) {
                assertSelectorRoundTrip(helper, registered.stream().map(Identifier::toString).toList());
            }
            families++;
        }
        checkMossyOak(helper);
        checkWoodDoors(helper, amc, enderscape);
        for (String key : List.of("cnm/building_accessory/bamboo", "cnm/trapdoor/bamboo")) {
            var family = AuditedShapeFamilies.families().stream().filter(f -> f.key().getPath().equals(key)).findFirst().orElseThrow();
            assertSelectorRoundTrip(helper, family.availableMembers(amc).stream().map(Identifier::toString).toList());
        }
        for (String wood : List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove",
                "cherry", "pale_oak", "bamboo", "crimson", "warped", "veiled", "celestial", "murublight", "mossy_oak")) {
            boolean mossyOak = wood.equals("mossy_oak");
            boolean endWood = List.of("veiled", "celestial", "murublight").contains(wood);
            String shortId = "dramaticdoors:short_" + (wood.equals("murublight") ? "murushroom" : wood) + "_door";
            if (endWood && !enderscape) {
                helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(id(shortId)), "Unexpected " + shortId);
                continue;
            }
            var item = requiredItem(shortId);
            var block = BuiltInRegistries.BLOCK.getOptional(id(shortId)).orElseThrow();
            helper.assertTrue(item instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() == block,
                    "Wrong DD BlockItem for " + shortId);
            var drops = net.minecraft.world.level.block.Block.getDrops(block.defaultBlockState(), helper.getLevel(),
                    helper.absolutePos(new BlockPos(2, 1, 2)), null);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(item) && drops.getFirst().getCount() == 1,
                    "Lost native DD drop for " + shortId + ": " + drops);
            if (mossyOak && !amc) {
                helper.assertTrue(!ShapeMap.isShape(item) && hasRecipe(helper, shortId), "Lost independent " + shortId);
                continue;
            }
            String path = "cnm/building_accessory/" + (mossyOak ? "ribbits_" : endWood ? "enderscape_" : "") + wood;
            var family = AuditedShapeFamilies.families().stream()
                    .filter(candidate -> candidate.key().getPath().equals(path)).findFirst().orElseThrow();
            var members = family.availableMembers(amc).stream().map(Identifier::toString).toList();
            int size = amc ? 10 : endWood ? 3 : wood.equals("bamboo") ? 5 : 10;
            helper.assertTrue(members.size() == size && members.getLast().equals(shortId), "Accessory order for " + path);
            assertSelectorRoundTrip(helper, members);
        }
        if (amc) {
            for (String wood : List.of("bamboo", "veiled", "celestial", "murublight", "mossy_oak")) {
                if (!enderscape && List.of("veiled", "celestial", "murublight").contains(wood)) continue;
                for (String finish : List.of(wood, "stripped_" + wood, wood + "_plank")) {
                    String base = "architectural_material_closure:" + finish + "_";
                    var windows = List.of(base + "window", base + "window2", base + "four_window", base + "pane_window");
                    assertSelectorRoundTrip(helper, windows);
                    assertWoodMatrixCraft(helper, windows.getFirst(), 4);
                }
            }
            String base = "architectural_material_closure:mossy_oak_";
            assertSelectorRoundTrip(helper, List.of(base + "sign", base + "hanging_sign", base + "shelf"));
            helper.assertTrue(hasRecipe(helper, base + "sign") && !hasRecipe(helper, base + "hanging_sign")
                    && !hasRecipe(helper, base + "shelf") && hasRecipe(helper, base + "lantern"), "Fixture recipes");
            assertWoodMatrixCraft(helper, base + "sign", 3);
            assertWoodMatrixCraft(helper, base + "lantern", 1);
        }
        if (Boolean.getBoolean("ibf.test.unifiedBge")) {
            Item planks = requiredItem("ribbits:mossy_oak_planks");
            var shapes = ShapeMap.getShapes(planks);
            helper.assertTrue(shapes.size() == 18 && shapes.getFirst() == planks
                    && shapes.get(9) == requiredItem("cnm_terrain_slabs_compat:ribbits/mossy_oak_beam"),
                    "Combined Mossy Oak Planks/Beam order " + shapes);
            for (Item member : shapes) {
                helper.assertTrue(!AuditedShapeRuntime.isAudited(member), "IBF claimed a BGE shape " + member);
                var state = Block.byItem(member).defaultBlockState();
                var drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)),
                        null, null, ItemStack.EMPTY);
                helper.assertTrue(drops.size() == 1 && drops.getFirst().is(planks) && drops.getFirst().getCount() == 1,
                        "Combined Ribbits/CNM loot " + member + ": " + drops);
            }
        }
        if (treeFamilyCount() > 0 && Boolean.getBoolean("ibf.test.unifiedBge")) {
            verifyTreeGeometry(helper);
        }
        checkWoodTrapdoors(helper, amc, enderscape);
        if (enderscape) {
            helper.assertTrue(hasRecipe(helper, "enderscape:murublight_door_from_celestial_door"),
                    "Lost Enderscape's native door conversion");
        }
        int expectedFamilies = enderscape ? (amc ? 284 : 181) : (amc ? 232 : 164);
        int expectedItems = enderscape ? (amc ? 2255 : 1412) : (amc ? 1828 : 1360);
        expectedFamilies += treeFamilyCount();
        expectedItems += treeItemCount();
        helper.assertTrue(families == expectedFamilies && items.size() == expectedItems,
                "Unexpected resolved catalog: " + families + " families, " + items.size() + " items");

        for (String material : List.of("calcite", "shadoline_block")) {
            String prefix = "architectural_material_closure:" + material + "_";
            if (amc && (enderscape || material.equals("calcite"))) {
                assertSelectorRoundTrip(helper, List.of(prefix + "button", prefix + "pressure_plate",
                        prefix + "parapet", prefix + "louvered_shutter"));
                assertMasonryWindowsPathsAndPatterns(helper, material, prefix, prefix + "button");
            } else {
                helper.assertTrue(BuiltInRegistries.ITEM.keySet().stream().noneMatch(id -> id.toString().startsWith(prefix)),
                        "Unexpected optional items for " + material);
            }
        }
        if (amc && !enderscape) {
            helper.assertTrue(hasRecipe(helper, "architectural_material_closure:end_stone_bricks_parapet")
                            && hasRecipe(helper, "architectural_material_closure:end_stone_bricks_louvered_shutter"),
                    "Standalone AMC recipes lost their Enderscape parent");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void purpurAndResinKeepProviderGeometryAndVanillaClumpRecipe(GameTestHelper helper) {
        for (String itemId : List.of("minecraft:purpur_slab", "minecraft:purpur_stairs",
                "enderscape:dusk_purpur_slab", "enderscape:dusk_purpur_stairs",
                "minecraft:resin_brick_slab", "minecraft:resin_brick_stairs")) {
            helper.assertTrue(!AuditedShapeRuntime.isAudited(requiredItem(itemId)),
                    "Provider geometry joined an IBF family: " + itemId);
        }
        helper.assertTrue(hasRecipe(helper, "minecraft:resin_clump"), "Lost vanilla Resin Clump recipe");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void enderscapeMaterialsKeepTheirPolishedControls(GameTestHelper helper) {
        boolean amcPresent = Boolean.getBoolean("ibf.test.amcExpected");
        for (String material : List.of("veradite", "mirestone", "kurodite")) {
            String prefix = "architectural_material_closure:" + material + "_";
            String polished = "enderscape:polished_" + material + "_";
            List<String> controls = new ArrayList<>(List.of(polished + "button", polished + "pressure_plate"));
            if (amcPresent) {
                controls.add(prefix + "parapet");
                controls.add(prefix + "louvered_shutter");
                assertMasonryWindowsPathsAndPatterns(helper, material, prefix, controls.getFirst());
            } else {
                helper.assertTrue(BuiltInRegistries.ITEM.keySet().stream()
                        .noneMatch(key -> key.toString().startsWith(prefix)), "Unexpected AMC items for " + material);
            }
            assertSelectorRoundTrip(helper, controls);
        }
        helper.succeed();
    }

    private static void assertMasonryWindowsPathsAndPatterns(GameTestHelper helper, String material, String prefix,
                                                             String accessoryParent) {
        var level = helper.getLevel();
        CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), false, level.registryAccess());
        assertSelectorRoundTrip(helper, List.of(prefix + "column", prefix + "urn", prefix + "moulding",
                prefix + "fence", prefix + "frame", prefix + "arrow_slit"));
        assertSelectorRoundTrip(helper, List.of(prefix + "window", prefix + "window2", prefix + "four_window",
                prefix + "pane_window", prefix + "gothic_window"));
        assertPathsAndIndependentFullBlocks(helper, prefix, accessoryParent);

        helper.assertTrue(hasRecipe(helper, prefix + "column_from_stonecutting"), "Lost column recipe for " + material);
        for (String form : List.of("urn", "moulding", "fence")) {
            helper.assertTrue(!hasRecipe(helper, prefix + form + "_from_stonecutting"),
                    "Retained alternate stonecutting recipe for " + prefix + form);
        }
        var buildingTab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.BUILDING_BLOCKS);
        for (String form : List.of("running_bond", "flagstone", "windmill_weave", "crystal_floor")) {
            Item item = requiredItem(prefix + form);
            helper.assertTrue(buildingTab.getDisplayItems().stream().anyMatch(stack -> stack.is(item)),
                    "Missing patterned full block in Building Blocks: " + prefix + form);
        }
    }

    @GameTest(maxTicks = 40)
    public void mossyStoneBricksSelectorsStaySeparateFromMacawPaths(GameTestHelper helper) {
        String path = "mcwpaths:mossy_stone_";
        String paving = "mcwpaths:mossy_cobblestone_";
        assertSelectorRoundTrip(helper, List.of(path + "running_bond_path", path + "strewn_rocky_path",
                path + "windmill_weave_path", path + "flagstone_path", path + "crystal_floor_path",
                paving + "diamond_paving", paving + "basket_weave_paving", paving + "square_paving",
                paving + "honeycomb_paving", paving + "clover_paving", paving + "dumble_paving"));

        String prefix = "architectural_material_closure:mossy_stone_bricks_";
        List<List<String>> selectors = List.of(
                List.of(prefix + "button", prefix + "pressure_plate", prefix + "parapet", prefix + "louvered_shutter"),
                List.of(prefix + "column", prefix + "urn", prefix + "moulding", prefix + "fence",
                        prefix + "frame", prefix + "arrow_slit"),
                List.of(prefix + "window", prefix + "window2", prefix + "four_window",
                        prefix + "pane_window", prefix + "gothic_window"));
        for (List<String> members : selectors) {
            if (Boolean.getBoolean("ibf.test.amcExpected")) {
                assertSelectorRoundTrip(helper, members);
                assertSeparate(helper, members.getFirst(), path + "running_bond_path", "Mossy Stone selectors merged");
            } else {
                for (String member : members) {
                    helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(id(member)), "Unexpected AMC item " + member);
                }
            }
        }
        if (Boolean.getBoolean("ibf.test.amcExpected")) {
            helper.assertTrue(hasRecipe(helper, prefix + "column_from_stonecutting"), "Missing Mossy Stone Bricks Column recipe");
            for (String alternate : List.of("urn", "moulding", "fence")) {
                helper.assertTrue(!hasRecipe(helper, prefix + alternate + "_from_stonecutting"),
                        "Retained alternate stonecutting recipe for " + alternate);
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void endStoneBricksAndPolishedBlackstoneFollowTheirProviders(GameTestHelper helper) {
        String prefix = "architectural_material_closure:end_stone_bricks_";
        String gothic = "mcwwindows:end_brick_gothic";
        String slit = "mcwwindows:ender_brick_arrow_slit";
        List<String> endStone = new ArrayList<>(List.of("enderscape:polished_end_stone_button",
                "enderscape:polished_end_stone_pressure_plate"));
        List<String> blackstone = new ArrayList<>(List.of("minecraft:polished_blackstone_button",
                "minecraft:polished_blackstone_pressure_plate", "mcwwindows:blackstone_parapet"));
        if (Boolean.getBoolean("ibf.test.amcExpected")) {
            endStone.add(prefix + "parapet");
            endStone.add(prefix + "louvered_shutter");
            blackstone.add("architectural_material_closure:polished_blackstone_louvered_shutter");
            assertSelectorRoundTrip(helper, List.of(prefix + "window", prefix + "window2", prefix + "four_window",
                    prefix + "pane_window", gothic));
            assertSelectorRoundTrip(helper, List.of(prefix + "column", prefix + "urn", prefix + "moulding",
                    prefix + "fence", prefix + "frame", slit));
            helper.assertTrue(hasRecipe(helper, prefix + "column_from_stonecutting"), "Missing Column stonecutting recipe");
            for (String form : List.of("urn", "moulding", "fence")) {
                helper.assertTrue(!hasRecipe(helper, prefix + form + "_from_stonecutting"),
                        "Retained alternate stonecutting recipe for " + form);
            }
            assertPathsAndIndependentFullBlocks(helper, prefix, endStone.getFirst());
        } else {
            for (String standalone : List.of(gothic, slit)) {
                Item item = requiredItem(standalone);
                helper.assertTrue(!AuditedShapeRuntime.isAudited(item) && !ShapeMap.isShape(item) && hasRecipe(helper, standalone),
                        "Expected independent provider item and recipe for " + standalone);
            }
        }
        assertSelectorRoundTrip(helper, endStone);
        assertSelectorRoundTrip(helper, blackstone);
        helper.succeed();
    }

    private static void assertPathsAndIndependentFullBlocks(GameTestHelper helper, String prefix, String accessoryParent) {
        assertSelectorRoundTrip(helper, List.of(prefix + "running_bond_path", prefix + "strewn_rocky_path",
                prefix + "windmill_weave_path", prefix + "flagstone_path", prefix + "crystal_floor_path",
                prefix + "diamond_paving", prefix + "basket_weave_paving", prefix + "square_paving",
                prefix + "honeycomb_paving", prefix + "clover_paving", prefix + "dumble_paving"));
        assertSeparate(helper, accessoryParent, prefix + "running_bond_path", "Accessory and path selectors joined");

        ServerPlayer player = survivalPlayer(helper);
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos.below(), Blocks.STONE);
        for (String form : List.of("running_bond", "flagstone", "windmill_weave", "crystal_floor")) {
            String fullId = prefix + form;
            Item item = requiredItem(fullId);
            helper.assertTrue(!AuditedShapeRuntime.isAudited(item) && !ShapeMap.isShape(item)
                            && hasRecipe(helper, fullId), "Patterned full block lost independent acquisition: " + fullId);
            helper.setBlock(pos, Blocks.AIR);
            ItemStack stack = new ItemStack(item);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            helper.placeAt(player, stack, pos.below(), Direction.UP);
            helper.assertTrue(BuiltInRegistries.BLOCK.getKey(helper.getBlockState(pos).getBlock()).equals(id(fullId)),
                    "Patterned full block failed independent placement: " + fullId);
        }
    }

    private static void checkWoodTrapdoors(GameTestHelper helper, boolean amc, boolean enderscape) {
        for (String wood : List.of("veiled", "celestial", "murublight", "mossy_oak")) {
            boolean mossyOak = wood.equals("mossy_oak");
            String parent = (mossyOak ? "ribbits:" : "enderscape:") + wood + "_trapdoor";
            boolean hasParent = enderscape && !mossyOak;
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id(parent)) == hasParent
                            && BuiltInRegistries.BLOCK.containsKey(id(parent)) == hasParent,
                    "Unexpected native trapdoor registration: " + parent);
            var members = new java.util.ArrayList<String>();
            if (!mossyOak) members.add(parent);
            for (String style : List.of("bamboo", "bark", "barn", "barred", "barrel", "beach", "blossom",
                    "classic", "cottage", "four_panel", "glass", "mystic", "paper", "ranch", "swamp",
                    "tropical", "whispering")) {
                String alternate = "architectural_material_closure:" + wood + "_" + style + "_trapdoor";
                boolean registered = amc && (enderscape || mossyOak);
                helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id(alternate)) == registered
                                && BuiltInRegistries.BLOCK.containsKey(id(alternate)) == registered,
                        "Unexpected AMC trapdoor registration: " + alternate);
                if (mossyOak && style.equals("classic")) members.addFirst(alternate);
                else members.add(alternate);
            }
            if (amc && (mossyOak || enderscape)) {
                assertSelectorRoundTrip(helper, members);
            } else if (hasParent) {
                helper.assertTrue(!AuditedShapeRuntime.isAudited(requiredItem(parent)) && hasRecipe(helper, parent),
                        "Lost standalone native trapdoor: " + parent);
            }
        }
        if (enderscape) helper.assertTrue(hasRecipe(helper, "enderscape:murublight_trapdoor_from_celestial_trapdoor"),
                "Lost Enderscape's native trapdoor conversion");
    }

    private static void verifyTreeGeometry(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var axe = net.minecraft.world.item.Items.IRON_AXE.getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
                Direction.NORTH, pos, false);
        for (String wood : List.of("silver_birch","wisteria","mossy_oak")) {
            var raw = ShapeMap.getShapes(requiredItem("mynx_trees:"+wood+"_log"));
            var stripped = ShapeMap.getShapes(requiredItem("mynx_trees:stripped_"+wood+"_log"));
            helper.assertTrue(raw.size()==18 && stripped.size()==18, "Log and wood geometry for "+wood);
            for (int i=0;i<raw.size();i++) {
                var state = Block.byItem(raw.get(i)).defaultBlockState();
                var axis = net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;
                if (state.hasProperty(axis)) state=state.setValue(axis,Direction.Axis.X);
                level.setBlock(pos,state,2);
                axe.useOn(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,hit));
                var result = level.getBlockState(pos);
                helper.assertTrue(result.is(Block.byItem(stripped.get(i))), "Wrong stripped geometry for "+raw.get(i));
                if (state.hasProperty(axis)) helper.assertTrue(result.getValue(axis)==Direction.Axis.X, "Lost timber axis");
            }
            if (wood.equals("mossy_oak")) continue;
            var planks = ShapeMap.getShapes(requiredItem("mynx_trees:"+wood+"_planks"));
            helper.assertTrue(planks.size()==18 && planks.get(9)==requiredItem("cnm_terrain_slabs_compat:mynx_trees/"+wood+"_beam"),
                    "Expected separate plank and thin beam geometry batches for "+wood);
        }
    }

    private static void checkWoodDoors(GameTestHelper helper, boolean amc, boolean enderscape) {
        for (String wood : List.of("veiled", "celestial", "murublight", "mossy_oak")) {
            boolean mossyOak = wood.equals("mossy_oak");
            String parent = (mossyOak ? "ribbits:" : "enderscape:") + wood + "_door";
            String nativeWood = wood.equals("murublight") ? "murushroom" : wood;
            String tallParent = "dramaticdoors:tall_" + nativeWood + "_door";
            var tallMembers = new ArrayList<>(List.of(tallParent));
            String glass = wood.equals("celestial") || wood.equals("murublight") ? "stem_glass" : "bark_glass";
            var members = new java.util.ArrayList<>(List.of(parent));
            for (String style : List.of("bamboo", glass, "barn", "barn_glass", "beach", "classic", "cottage",
                    "four_panel", "glass", "japanese", "japanese2", "modern", "mystic", "nether", "paper",
                    "stable", "stable_head", "swamp", "tropical", "waffle", "western", "whispering")) {
                String alternate = "architectural_material_closure:" + wood + "_" + style + "_door";
                helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id(alternate)) == (amc && (enderscape || mossyOak)),
                        "Unexpected registration for " + alternate);
                members.add(alternate);
                String tall = "dramaticdoors:tall_macaw_" + wood + "_" + style + "_door";
                helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id(tall)) == (amc && (enderscape || mossyOak)),
                        "Unexpected DD registration for " + tall);
                tallMembers.add(tall);
            }
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id(tallParent)) == (enderscape || mossyOak),
                    "Unexpected native tall door " + tallParent);
            if (enderscape || mossyOak) {
                helper.assertTrue(hasRecipe(helper, tallParent), "Missing native tall conversion for " + parent);
                if (amc) {
                    assertSelectorRoundTrip(helper, tallMembers);
                    assertSeparate(helper, parent, tallParent, "Door heights merged for " + wood);
                } else {
                    helper.assertTrue(!AuditedShapeRuntime.isAudited(requiredItem(tallParent))
                            && !ShapeMap.isShape(requiredItem(tallParent)), "Unexpected singleton " + tallParent);
                }
            }
            if (enderscape || mossyOak) {
                if (amc) assertSelectorRoundTrip(helper, members);
                else helper.assertTrue(!ShapeMap.isShape(requiredItem(parent)) && hasRecipe(helper, parent),
                        "Missing independent native door or recipe: " + parent);
            } else {
                helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(id(parent)), "Unexpected native door " + parent);
            }
        }
    }

    private static void checkMossyOak(GameTestHelper helper) {
        var fences = new java.util.ArrayList<>(List.of("ribbits:mossy_oak_planks_fence", "ribbits:mossy_oak_planks_fence_gate"));
        String prefix = "architectural_material_closure:mossy_oak_";
        if (Boolean.getBoolean("ibf.test.amcExpected")) {
            fences.add(prefix + "frame");
            fences.add(prefix + "lattice");
            assertSelectorRoundTrip(helper, List.of(prefix + "trim", prefix + "balustrade", prefix + "support", prefix + "pallet"));
        } else {
            helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(id(prefix + "trim")), "Unexpected optional detail parent");
        }
        assertSelectorRoundTrip(helper, fences);
    }

    @GameTest
    public void paleOakDoorAvailability(GameTestHelper helper) {
        var input = net.minecraft.world.item.Items.PALE_OAK_DOOR.getDefaultInstance();
        for (var height : List.of("short", "tall")) {
            var key = id("dramaticdoors:" + height + "_pale_oak_door");
            var block = BuiltInRegistries.BLOCK.getOptional(key).orElseThrow();
            var item = requiredItem(key);
            helper.assertTrue(item instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() == block,
                    "Door block/item " + key);
            var level = helper.getLevel();
            if (height.equals("short")) {
                helper.assertTrue(!hasRecipe(helper, key.toString()), "Retained Pale Oak short-door recipe");
                continue;
            }
            var recipe = level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, key)).orElseThrow().value();
            var grid = net.minecraft.world.item.crafting.CraftingInput.of(1, 3, List.of(input, input, input));
            var shaped = (net.minecraft.world.item.crafting.ShapedRecipe) recipe;
            helper.assertTrue(shaped.matches(grid, level), "Vertical Pale Oak conversion");
            ItemStack output = shaped.assemble(grid);
            helper.assertTrue(ShapeMap.getShapes(item).size() <= 1, "Unexpected tall Pale Oak alternatives");
            helper.assertTrue(output.is(item) && output.getCount() == 2, "Pale Oak output " + key);
        }
        helper.succeed();
    }

    private static void assertSelectorRoundTrip(GameTestHelper helper, List<String> members) {
        String parentId = members.getFirst();
        assertExactShapeSet(helper, parentId, members);
        Item parent = requiredItem(parentId);
        ItemStack stack = new ItemStack(parent, 7);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Selector round trip"));
        stack.set(DataComponents.MAX_STACK_SIZE, 37);
        for (int index = 1; index < members.size(); index++) {
            ItemStack next = ShapeMap.transferStack(stack, index);
            assertTransferred(helper, stack, next, requiredItem(members.get(index)), members.get(index));
            helper.assertTrue(!hasRecipe(helper, members.get(index)),
                    "CNM retained an alternate recipe for " + members.get(index));
            stack = next;
        }
        assertTransferred(helper, stack, ShapeMap.transferStack(stack, 0), parent, "Return to " + parentId);
        for (int index = members.size() - 2; index >= 0; index--) {
            ItemStack previous = ShapeMap.transferStack(stack, index);
            assertTransferred(helper, stack, previous, requiredItem(members.get(index)), "Reverse to " + members.get(index));
            stack = previous;
        }
        helper.assertTrue(hasRecipe(helper, parentId), "Canonical recipe missing for " + parentId);
    }

    private static void assertWoodMatrixCraft(GameTestHelper helper, String parent, int count) {
        var level = helper.getLevel();
        var resource = id(parent).withPath(path -> "recipe/" + path + ".json");
        try (var reader = level.getServer().getResourceManager().openAsReader(resource)) {
            var recipe = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            var pattern = recipe.getAsJsonArray("pattern");
            var keys = recipe.getAsJsonObject("key");
            var stacks = new ArrayList<ItemStack>();
            for (var row : pattern) {
                for (char slot : row.getAsString().toCharArray()) {
                    stacks.add(slot == ' ' ? ItemStack.EMPTY :
                            new ItemStack(requiredItem(keys.get(String.valueOf(slot)).getAsString())));
                }
            }
            var grid = net.minecraft.world.item.crafting.CraftingInput.of(pattern.get(0).getAsString().length(),
                    pattern.size(), stacks);
            var match = level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,
                    grid, level).orElseThrow();
            var result = match.value().assemble(grid);
            helper.assertTrue(result.is(requiredItem(parent)) && result.getCount() == count,
                    "Survival crafting grid for " + parent + " selected " + match.id() + " and produced " + result);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Missing wood matrix recipe " + parent, failure);
        }
    }

    private static void assertExactShapeSet(
            GameTestHelper helper,
            String parent,
            List<String> expectedValues
    ) {
        List<Identifier> expected = expectedValues.stream().map(AuditedShapeMapGameTests::id).toList();
        Item parentItem = requiredItem(parent);
        List<Identifier> actual = ids(ShapeMap.getShapes(parentItem));
        helper.assertTrue(actual.equals(expected),
                "Exact ShapeMap family changed for " + parent + ": expected=" + expected
                        + ", actual=" + actual);
        helper.assertTrue(!ShapeMap.isShape(parentItem),
                "Canonical ShapeMap parent was also registered as an alternate: " + parent);
    }

    private static boolean hasRecipe(GameTestHelper helper, String value) {
        return helper.getLevel().recipeAccess().byKey(
                ResourceKey.create(Registries.RECIPE, id(value))).isPresent();
    }

    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void assertPlacedDrop(
            GameTestHelper helper,
            ServerPlayer player,
            String placedId,
            String expectedDropId,
            int height,
            int brokenSegment
    ) {
        BlockPos base = new BlockPos(3, 1, 3);
        Item placedItem = requiredItem(placedId);
        Item expectedDrop = requiredItem(expectedDropId);
        Block placedBlock = Block.byItem(placedItem);

        helper.killAllEntitiesOfClass(ItemEntity.class);
        for (int offset = 0; offset < 4; offset++) {
            helper.setBlock(base.above(offset), Blocks.AIR);
        }
        helper.setBlock(base.below(), Blocks.STONE);

        ItemStack placementStack = new ItemStack(placedItem);
        player.setItemInHand(InteractionHand.MAIN_HAND, placementStack);
        helper.placeAt(player, placementStack, base.below(), Direction.UP);
        for (int offset = 0; offset < height; offset++) {
            helper.assertTrue(helper.getBlockState(base.above(offset)).is(placedBlock),
                    "Placement did not create segment " + offset + " for " + placedId);
        }

        boolean destroyed = player.gameMode.destroyBlock(
                helper.absolutePos(base.above(brokenSegment)));
        helper.assertTrue(destroyed,
                "Survival player did not destroy segment " + brokenSegment + " for " + placedId);

        List<ItemEntity> entities = helper.getEntities(EntityTypes.ITEM, base, 4.0);
        helper.assertTrue(entities.size() == 1,
                "Expected one item entity after breaking " + placedId + " segment "
                        + brokenSegment + ", found " + entities.size());
        ItemStack dropped = entities.getFirst().getItem();
        helper.assertTrue(dropped.getItem() == expectedDrop && dropped.getCount() == 1,
                "Expected exactly one " + expectedDropId + " after breaking " + placedId
                        + " segment " + brokenSegment + ", found "
                        + BuiltInRegistries.ITEM.getKey(dropped.getItem()) + " x" + dropped.getCount());
        for (int offset = 0; offset < height; offset++) {
            helper.assertTrue(helper.getBlockState(base.above(offset)).isAir(),
                    "Segment " + offset + " remained after breaking " + placedId
                            + " segment " + brokenSegment);
        }
    }

    private static void assertSupportLossDrop(
            GameTestHelper helper,
            ServerPlayer player,
            String placedId,
            String expectedDropId,
            int height
    ) {
        BlockPos base = new BlockPos(3, 1, 3);
        Item placedItem = requiredItem(placedId);
        Item expectedDrop = requiredItem(expectedDropId);
        Item supportDrop = Blocks.DIRT.asItem();
        Block placedBlock = Block.byItem(placedItem);

        helper.killAllEntitiesOfClass(ItemEntity.class);
        for (int offset = 0; offset < 4; offset++) {
            helper.setBlock(base.above(offset), Blocks.AIR);
        }
        helper.setBlock(base.below(), Blocks.DIRT);

        ItemStack placementStack = new ItemStack(placedItem);
        player.setItemInHand(InteractionHand.MAIN_HAND, placementStack);
        helper.placeAt(player, placementStack, base.below(), Direction.UP);
        for (int offset = 0; offset < height; offset++) {
            helper.assertTrue(helper.getBlockState(base.above(offset)).is(placedBlock),
                    "Placement did not create segment " + offset + " for " + placedId);
        }

        boolean supportDestroyed = player.gameMode.destroyBlock(
                helper.absolutePos(base.below()));
        helper.assertTrue(supportDestroyed,
                "Survival player did not destroy the support below " + placedId);

        List<ItemEntity> entities = helper.getEntities(EntityTypes.ITEM, base, 4.0);
        List<ItemEntity> nonSupportDrops = entities.stream()
                .filter(entity -> entity.getItem().getItem() != supportDrop)
                .toList();
        int matchingItemCount = nonSupportDrops.stream()
                .filter(entity -> entity.getItem().getItem() == expectedDrop)
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
        helper.assertTrue(nonSupportDrops.size() == 1,
                "Expected one non-support item entity after support loss for " + placedId
                        + ", found " + nonSupportDrops.size());
        ItemStack dropped = nonSupportDrops.getFirst().getItem();
        helper.assertTrue(dropped.getItem() == expectedDrop
                        && dropped.getCount() == 1
                        && matchingItemCount == 1,
                "Expected exactly one " + expectedDropId + " after support loss for "
                        + placedId + ", found "
                        + BuiltInRegistries.ITEM.getKey(dropped.getItem()) + " x"
                        + dropped.getCount() + " (matching total " + matchingItemCount + ")");
        helper.assertTrue(helper.getBlockState(base.below()).isAir(),
                "Support remained after destruction below " + placedId);
        for (int offset = 0; offset < height; offset++) {
            helper.assertTrue(helper.getBlockState(base.above(offset)).isAir(),
                    "Segment " + offset + " remained after support loss for " + placedId);
        }
    }

    private static void assertTransferred(
            GameTestHelper helper,
            ItemStack source,
            ItemStack result,
            Item expectedItem,
            String label
    ) {
        helper.assertTrue(result.getItem() == expectedItem
                        && result.getCount() == source.getCount()
                        && result.getComponentsPatch().equals(source.getComponentsPatch()),
                label + ": source=" + BuiltInRegistries.ITEM.getKey(source.getItem())
                        + ", result=" + BuiltInRegistries.ITEM.getKey(result.getItem()));
    }

    private static boolean qsnInSameShapeSet(Item first, Item second) {
        try {
            Class<?> resolver = Class.forName(
                    "dev.resivore.quickstacknearbycompat.core.CnmShapeMapResolver");
            Method method = resolver.getMethod("inSameShapeSet", Item.class, Item.class);
            return (boolean) method.invoke(null, first, second);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Accepted QSN CNM resolver is unavailable", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Item, List<Item>> mutableShapeMapForGuardTest() {
        try {
            java.lang.reflect.Field field = ShapeMap.class.getDeclaredField("SHAPES_BY_PARENT");
            field.setAccessible(true);
            return (Map<Item, List<Item>>) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("CNM ShapeMap storage is unavailable for guard testing", exception);
        }
    }

    private static boolean rejectedByPreResolutionGuard(List<ShapeMap.Mapping> mappings) {
        try {
            AuditedShapeRuntime.addMappings(mappings);
            return false;
        } catch (IllegalStateException expected) {
            return true;
        }
    }

    private static Set<Identifier> approvedIds() {
        Set<Identifier> result = new HashSet<>();
        for (AuditedShapeFamily family : AuditedShapeFamilies.families()) {
            result.addAll(family.members());
        }
        return Set.copyOf(result);
    }

    private static List<Identifier> ids(Iterable<Item> items) {
        List<Identifier> result = new ArrayList<>();
        for (Item item : items) {
            result.add(BuiltInRegistries.ITEM.getKey(item));
        }
        return List.copyOf(result);
    }

    private static Item requiredItem(String value) {
        return requiredItem(id(value));
    }

    private static Item requiredItem(Identifier id) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            throw new AssertionError("Pinned runtime is missing required item " + id);
        }
        return BuiltInRegistries.ITEM.getValue(id);
    }

    private static Identifier id(String value) {
        return Identifier.parse(value);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
