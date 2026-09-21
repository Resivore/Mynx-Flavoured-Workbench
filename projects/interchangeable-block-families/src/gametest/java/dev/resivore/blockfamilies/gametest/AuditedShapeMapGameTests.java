package dev.resivore.blockfamilies.gametest;

import dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamilies;
import dev.resivore.blockfamilies.cnm.catalog.AuditedShapeFamily;
import dev.resivore.blockfamilies.cnm.runtime.AuditedShapeRuntime;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
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

    private static final List<List<String>> C8_ENDERSCAPE_FENCE_GATE_FAMILIES = List.of(
            List.of("enderscape:veiled_fence", "enderscape:veiled_fence_gate"),
            List.of("enderscape:celestial_fence", "enderscape:celestial_fence_gate"),
            List.of("enderscape:murublight_fence", "enderscape:murublight_fence_gate")
    );

    private static final List<List<String>> C8_ENDERSCAPE_BUILDING_ACCESSORY_FAMILIES = List.of(
            List.of("enderscape:veiled_button", "enderscape:veiled_pressure_plate"),
            List.of("enderscape:celestial_button", "enderscape:celestial_pressure_plate"),
            List.of("enderscape:murublight_button", "enderscape:murublight_pressure_plate"),
            List.of("enderscape:polished_end_stone_button",
                    "enderscape:polished_end_stone_pressure_plate"),
            List.of("enderscape:polished_mirestone_button",
                    "enderscape:polished_mirestone_pressure_plate"),
            List.of("enderscape:polished_veradite_button",
                    "enderscape:polished_veradite_pressure_plate"),
            List.of("enderscape:polished_kurodite_button",
                    "enderscape:polished_kurodite_pressure_plate")
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

    private static final List<String> C8_ENDERSCAPE_DOOR_TRAPDOOR_EXCLUSIONS = List.of(
            "enderscape:veiled_door", "enderscape:veiled_trapdoor",
            "enderscape:celestial_door", "enderscape:celestial_trapdoor",
            "enderscape:murublight_door", "enderscape:murublight_trapdoor"
    );

    private static final List<String> C8_CANONICAL_RESULT_RECIPE_IDS = List.of(
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
            "enderscape:shadoline_bars"
    );

    private static final List<String> C8_REMOVED_ALTERNATE_RECIPE_IDS = List.of(
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
            "enderscape:shadoline_chain"
    );

    private static final List<String> C8_RETAINED_ALTERNATE_PROVIDER_RECIPE_IDS = List.of(
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
            "mcwwindows:andesite_louvered_shutter",
            "mcwwindows:diorite_louvered_shutter",
            "mcwwindows:granite_louvered_shutter",
            "mcwwindows:stone_brick_gothic",
            "mcwwindows:end_brick_gothic",
            "mcwwindows:nether_brick_gothic",
            "mcwwindows:mud_brick_gothic",
            "mcwwindows:blackstone_brick_gothic",
            "mcwwindows:stone_brick_arrow_slit",
            "mcwwindows:cobblestone_arrow_slit",
            "mcwwindows:nether_brick_arrow_slit",
            "mcwwindows:ender_brick_arrow_slit",
            "mcwwindows:mud_brick_arrow_slit",
            "mcwwindows:blackstone_brick_arrow_slit",
            "mcwwindows:prismarine_brick_arrow_slit",
            "mcwwindows:dark_prismarine_brick_arrow_slit",
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
            "mcwwindows:prismarine_parapet",
            "mcwwindows:red_curtain"
    );

    private static final List<String> UNREGISTERED_EXCLUSIONS = List.of(
            "mcwdoors:cherry_waffle_door",
            "dramaticdoors:tall_netherite_door",
            "dramaticdoors:tall_pale_oak_door",
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
        List<AuditedShapeFamily> families = AuditedShapeFamilies.families();
        helper.assertTrue(families.size() == AuditedShapeFamilies.EXPECTED_FAMILY_COUNT,
                "Expected " + AuditedShapeFamilies.EXPECTED_FAMILY_COUNT
                        + " audited families, found " + families.size());

        Set<Identifier> seen = new LinkedHashSet<>();
        int largest = 0;
        for (AuditedShapeFamily family : families) {
            helper.assertTrue(family.canonicalParent().equals(family.members().getFirst()),
                    "Canonical parent is not first for " + family.key());

            Item parent = requiredItem(family.canonicalParent());
            List<Identifier> actual = ids(ShapeMap.getShapes(parent));
            helper.assertTrue(actual.equals(family.members()),
                    "Resolved order differs for " + family.key() + ": expected="
                            + family.members() + ", actual=" + actual);

            for (Identifier id : family.members()) {
                helper.assertTrue(seen.add(id), "Audited ID materialized more than once: " + id);
                helper.assertTrue(ShapeMap.getParent(requiredItem(id)) == parent,
                        "Resolved parent differs for " + id + " in " + family.key());
            }
            largest = Math.max(largest, family.members().size());
        }

        helper.assertTrue(seen.size() == AuditedShapeFamilies.EXPECTED_UNIQUE_MEMBER_COUNT,
                "Expected " + AuditedShapeFamilies.EXPECTED_UNIQUE_MEMBER_COUNT
                        + " unique runtime IDs, found " + seen.size());
        helper.assertTrue(largest == AuditedShapeFamilies.EXPECTED_LARGEST_FAMILY_SIZE,
                "Expected largest runtime family size 22, found " + largest);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void c8DisplayFixtureFamiliesResolveExactlyWithSignsCanonical(GameTestHelper helper) {
        List<AuditedShapeFamily> actual = AuditedShapeFamilies.families(
                AuditedShapeFamily.Category.DISPLAY_FIXTURE);
        assertExactFamilies(helper, "C8 display fixtures", actual, C8_DISPLAY_FIXTURE_FAMILIES);

        for (AuditedShapeFamily family : actual) {
            String path = family.canonicalParent().getPath();
            helper.assertTrue(path.endsWith("_sign") && !path.endsWith("_hanging_sign"),
                    "Display-fixture canonical parent is not its standing Sign: "
                            + family.key() + " -> " + family.canonicalParent());
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void c8EnderscapeFenceAccessoryAndShadolineFamiliesResolveExactly(GameTestHelper helper) {
        assertExactFamilies(helper, "C8 Enderscape fence/gate families",
                providerFamilies(AuditedShapeFamily.Category.FENCE_GATE, "enderscape"),
                C8_ENDERSCAPE_FENCE_GATE_FAMILIES);
        assertExactFamilies(helper, "C8 Enderscape button/plate families",
                providerFamilies(AuditedShapeFamily.Category.BUILDING_ACCESSORY, "enderscape"),
                C8_ENDERSCAPE_BUILDING_ACCESSORY_FAMILIES);
        assertExactFamilies(helper, "C8 Enderscape Shadoline bars/chain family",
                providerFamilies(AuditedShapeFamily.Category.BAR_CHAIN, "enderscape"),
                C8_ENDERSCAPE_BAR_CHAIN_FAMILIES);
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
    public void c8EnderscapeDoorsAndTrapdoorsRemainOutsideDisplayCatalog(GameTestHelper helper) {
        Set<Identifier> approved = approvedIds();
        for (String value : C8_ENDERSCAPE_DOOR_TRAPDOOR_EXCLUSIONS) {
            Identifier itemId = id(value);
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(itemId)
                            && BuiltInRegistries.ITEM.containsKey(itemId),
                    "Pinned Enderscape no longer registers audited door/trapdoor " + itemId);
            helper.assertTrue(!approved.contains(itemId),
                    "Enderscape door/trapdoor entered the display catalog " + itemId);

            Set<Identifier> intersection = new HashSet<>(ids(
                    ShapeMap.getShapes(requiredItem(itemId))));
            intersection.retainAll(approved);
            helper.assertTrue(intersection.isEmpty(),
                    "Enderscape door/trapdoor joined an approved ShapeMap component: "
                            + itemId + " -> " + intersection);
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
        Item originalTarget = resolved.set(1, celestialShelf);
        try {
            ItemStack rejected = ShapeMap.transferStack(source, 1);
            helper.assertTrue(rejected.getItem() == source.getItem()
                            && rejected.getCount() == source.getCount()
                            && rejected.getComponentsPatch().equals(source.getComponentsPatch()),
                    "Transfer guard did not fail closed on a cross-family display target");
        } finally {
            resolved.set(1, originalTarget);
        }
        helper.assertTrue(originalTarget == veiledHangingSign
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
                "mcwwindows:oak_curtain_rod"));
        assertExactShapeSet(helper, "minecraft:stone_button", List.of(
                "minecraft:stone_button",
                "minecraft:stone_pressure_plate",
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
                "mcwwindows:andesite_parapet",
                "mcwpaths:andesite_diamond_paving",
                "mcwpaths:andesite_basket_weave_paving",
                "mcwpaths:andesite_square_paving",
                "mcwpaths:andesite_honeycomb_paving",
                "mcwpaths:andesite_clover_paving",
                "mcwpaths:andesite_dumble_paving"));
        assertExactShapeSet(helper, "ribbits:mossy_oak_planks_fence", List.of(
                "ribbits:mossy_oak_planks_fence", "ribbits:mossy_oak_planks_fence_gate"));
        assertExactShapeSet(helper, "minecraft:light_weighted_pressure_plate", List.of(
                "minecraft:light_weighted_pressure_plate",
                "mcwwindows:golden_curtain_rod"));

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
        assertExactShapeSet(helper, "ribbits:mossy_oak_planks_fence", List.of(
                "ribbits:mossy_oak_planks_fence", "ribbits:mossy_oak_planks_fence_gate"));
        for (String stone : List.of(
                "stone", "blackstone", "deepslate", "nether_brick", "sandstone", "red_sandstone", "quartz")) {
            assertExactShapeSet(helper, "bbb:" + stone + "_column", List.of(
                    "bbb:" + stone + "_column", "bbb:" + stone + "_urn",
                    "bbb:" + stone + "_moulding", "bbb:" + stone + "_fence", "bbb:" + stone + "_frame"));
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
                "mcwpaths:stone_running_bond_path",
                "mcwwindows:oak_log_parapet",
                "mcwwindows:metal_curtain_rod",
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
                "minecraft:light_weighted_pressure_plate",
                "mcwpaths:andesite_running_bond_path").stream()
                .filter(retained -> !hasRecipe(helper, retained))
                .toList();
        helper.assertTrue(missingCanonicalRecipes.isEmpty(),
                "CNM removed intended canonical acquisition recipes: " + missingCanonicalRecipes);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void c8RecipeCleanupPreserves30CanonicalAnd5ProviderConversionsAndRemoves41CraftingAlternates(
            GameTestHelper helper) {
        helper.assertTrue(C8_CANONICAL_RESULT_RECIPE_IDS.size() == 30
                        && new HashSet<>(C8_CANONICAL_RESULT_RECIPE_IDS).size() == 30,
                "C8 canonical-result recipe fixture must contain exactly 30 distinct IDs");
        helper.assertTrue(C8_REMOVED_ALTERNATE_RECIPE_IDS.size() == 41
                        && new HashSet<>(C8_REMOVED_ALTERNATE_RECIPE_IDS).size() == 41,
                "C8 removed-alternate recipe fixture must contain exactly 41 distinct IDs");
        helper.assertTrue(C8_RETAINED_ALTERNATE_PROVIDER_RECIPE_IDS.size() == 5
                        && new HashSet<>(C8_RETAINED_ALTERNATE_PROVIDER_RECIPE_IDS).size() == 5,
                "C8 retained provider-conversion fixture must contain exactly 5 distinct IDs");
        Set<String> overlap = new HashSet<>(C8_CANONICAL_RESULT_RECIPE_IDS);
        overlap.retainAll(C8_REMOVED_ALTERNATE_RECIPE_IDS);
        helper.assertTrue(overlap.isEmpty(),
                "C8 canonical and alternate recipe fixtures overlap: " + overlap);

        List<String> missingCanonicalRecipes = C8_CANONICAL_RESULT_RECIPE_IDS.stream()
                .filter(recipeId -> !hasRecipe(helper, recipeId))
                .toList();
        helper.assertTrue(missingCanonicalRecipes.isEmpty(),
                "CNM removed C8 canonical-result recipes: " + missingCanonicalRecipes);

        List<String> retainedAlternateRecipes = C8_REMOVED_ALTERNATE_RECIPE_IDS.stream()
                .filter(recipeId -> hasRecipe(helper, recipeId))
                .toList();
        helper.assertTrue(retainedAlternateRecipes.isEmpty(),
                "CNM retained C8 alternate-result crafting recipes: " + retainedAlternateRecipes);

        List<String> missingProviderConversions = C8_RETAINED_ALTERNATE_PROVIDER_RECIPE_IDS.stream()
                .filter(recipeId -> !hasRecipe(helper, recipeId))
                .toList();
        helper.assertTrue(missingProviderConversions.isEmpty(),
                "CNM removed Enderscape custom conversion recipes it cannot re-encode: "
                        + missingProviderConversions);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void categoriesMaterialsAndAuditedExclusionsRemainIsolated(GameTestHelper helper) {
        List<AuditedShapeFamily> families = AuditedShapeFamilies.families();
        Set<Identifier> approved = approvedIds();
        Set<Item> resolvedParents = new HashSet<>();

        for (AuditedShapeFamily family : families) {
            Item representative = requiredItem(family.members().get(1));
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
        AuditedShapeFamily family = AuditedShapeFamilies.families().stream()
                .filter(candidate -> candidate.members().size()
                        == AuditedShapeFamilies.EXPECTED_LARGEST_FAMILY_SIZE)
                .findFirst()
                .orElseThrow();
        List<Item> actual = ShapeMap.getShapes(requiredItem(family.canonicalParent()));
        int middleIndex = actual.size() / 2;
        int lastIndex = actual.size() - 1;

        ItemStack first = new ItemStack(actual.getFirst(), 37);
        first.set(DataComponents.CUSTOM_NAME, Component.literal("IBF transfer sentinel"));
        ItemStack middle = ShapeMap.transferStack(first, middleIndex);
        ItemStack last = ShapeMap.transferStack(middle, lastIndex);

        helper.assertTrue(actual.size() == 22, "Large-family transfer fixture is no longer 22 members");
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
        List<Identifier> expected = List.of(
                id("minecraft:sea_lantern"),
                id("more_slabs_stairs_and_walls:sea_lantern_slab"),
                id("more_slabs_stairs_and_walls:sea_lantern_stairs"),
                id("more_slabs_stairs_and_walls:sea_lantern_wall"),
                id("clutternomore:more_slabs_stairs_and_walls/vertical_sea_lantern_slab"),
                id("clutternomore:more_slabs_stairs_and_walls/sea_lantern_step")
        );
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
            assertExactShapeSet(helper, expectedValues.getFirst(), expectedValues);
        }
    }

    private static void assertSeparate(GameTestHelper helper, String first, String second, String label) {
        Item firstItem = requiredItem(first);
        Item secondItem = requiredItem(second);
        helper.assertTrue(ShapeMap.getParent(firstItem) != ShapeMap.getParent(secondItem)
                        && !ShapeMap.inSameShapeSet(firstItem, secondItem),
                label + ": " + first + " / " + second);
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
