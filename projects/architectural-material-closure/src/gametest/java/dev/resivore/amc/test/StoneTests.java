package dev.resivore.amc.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.kikoz.mcwwindows.init.BlockInit;
import net.kikoz.mcwwindows.objects.Parapet;
import net.kikoz.mcwwindows.objects.Shutter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.stream.Collectors;

public final class StoneTests {
    private static final String NAMESPACE = "architectural_material_closure";
    private static final BlockPos POSITION = new BlockPos(1, 1, 1);

    private static Identifier id(String form) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, "stone_" + form);
    }

    private static Block block(String form) {
        return BuiltInRegistries.BLOCK.getValue(id(form));
    }

    @GameTest
    public void familyCreativeTabs(GameTestHelper helper) {
        var level = helper.getLevel();
        CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), false, level.registryAccess());
        var tabs = BuiltInRegistries.CREATIVE_MODE_TAB;
        var building = tabs.getValueOrThrow(CreativeModeTabs.BUILDING_BLOCKS);
        var redstone = tabs.getValueOrThrow(CreativeModeTabs.REDSTONE_BLOCKS);
        for (var key : BuiltInRegistries.ITEM.keySet()) {
            if (!key.getNamespace().equals(NAMESPACE)) continue;
            var item = BuiltInRegistries.ITEM.getValue(key);
            boolean control = key.getPath().endsWith("_button") || key.getPath().endsWith("_pressure_plate");
            var target = control ? redstone : building;
            var other = control ? building : redstone;
            helper.assertTrue(target.getDisplayItems().stream().filter(stack -> stack.is(item)).count() == 1
                    && other.getDisplayItems().stream().noneMatch(stack -> stack.is(item)), key + " creative tab");
        }
        helper.succeed();
    }

    @GameTest
    public void endStoneBricksProviderOwnership(GameTestHelper helper) {
        for (String name : java.util.List.of("extshape:end_stone_brick_button", "extshape:end_stone_brick_pressure_plate",
                "mcwwindows:end_brick_gothic", "mcwwindows:ender_brick_arrow_slit")) {
            var key = Identifier.parse(name);
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(key) && BuiltInRegistries.ITEM.containsKey(key),
                    "Missing provider registration " + key);
            var block = BuiltInRegistries.BLOCK.getValue(key);
            helper.assertTrue(BuiltInRegistries.ITEM.getValue(key) instanceof BlockItem item && item.getBlock() == block,
                    "Provider block/item mismatch " + key);
        }
        helper.succeed();
    }

    @GameTest
    public void registryAndResources(GameTestHelper helper) {
        var ids = new java.util.HashSet<>(Set.of(id("parapet"), id("louvered_shutter")));
        for (String material : java.util.List.of("andesite", "diorite", "granite", "mud_bricks", "prismarine_bricks", "dark_prismarine",
                "sandstone", "red_sandstone", "deepslate", "quartz", "nether_bricks", "end_stone_bricks", "polished_blackstone", "mossy_stone_bricks")) {
            var forms = new java.util.ArrayList<>(DirectFormTests.references(material).keySet());
            if (Set.of("andesite", "diorite", "granite", "mud_bricks", "prismarine_bricks", "dark_prismarine", "end_stone_bricks", "mossy_stone_bricks").contains(material)) {
                forms.addAll(java.util.List.of("column", "urn", "moulding", "fence", "frame"));
            }
            for (String form : forms) {
                var name = material + "_" + form;
                ids.add(Identifier.fromNamespaceAndPath(NAMESPACE, name));
                for (String ns : java.util.List.of("minecraft", "mcwwindows", "bbb", "mcwpaths")) {
                    var other = Identifier.fromNamespaceAndPath(ns, name);
                    helper.assertTrue(!BuiltInRegistries.BLOCK.containsKey(other)
                            && !BuiltInRegistries.ITEM.containsKey(other), "Provider already owns " + other);
                }
            }
        }
        helper.assertTrue(ids.size() == 177, "Expected 177 block/item pairs");
        for (var registry : java.util.List.of(BuiltInRegistries.BLOCK, BuiltInRegistries.ITEM)) {
            var registered = registry.keySet().stream().filter(key -> NAMESPACE.equals(key.getNamespace()))
                    .collect(Collectors.toSet());
            helper.assertTrue(registered.equals(ids), "Unexpected AMC registry entries: " + registered);
        }
        for (String form : java.util.List.of("parapet", "louvered_shutter")) {
            var key = id(form);
            var stone = block(form);
            var item = BuiltInRegistries.ITEM.getValue(key);
            var providerKey = Identifier.fromNamespaceAndPath("mcwwindows", "andesite_" + form);
            var reference = BuiltInRegistries.BLOCK.getValue(providerKey);
            helper.assertTrue(reference == (form.equals("parapet") ? BlockInit.ANDESITE_PARAPET
                    : BlockInit.ANDESITE_LOUVERED_SHUTTER), "Provider identity changed");
            helper.assertTrue(stone != reference && stone.getClass() == reference.getClass(), "Wrong implementation");
            helper.assertTrue(!BuiltInRegistries.BLOCK.containsKey(Identifier.fromNamespaceAndPath(
                    "mcwwindows", key.getPath())), "Macaw already owns the Stone form");
            helper.assertTrue(item instanceof BlockItem bi && bi.getBlock() == stone && stone.asItem() == item,
                    "Block item binding failed");
            var tag = Identifier.fromNamespaceAndPath("mcwwindows", form.equals("parapet") ? "parapets" : "shutters");
            helper.assertTrue(stone.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
                    && stone.defaultBlockState().is(TagKey.create(Registries.BLOCK, tag)), "Missing block tag");
            helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, key))
                    .isPresent(), "Recipe failed to load");
            var drops = Block.getDrops(stone.defaultBlockState(), helper.getLevel(), helper.absolutePos(POSITION), null);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(item) && drops.getFirst().getCount() == 1,
                    "Expected one matching self-drop");
            var pos = helper.absolutePos(POSITION);
            for (var state : reference.getStateDefinition().getPossibleStates()) {
                var actual = stone.withPropertiesOf(state);
                helper.assertTrue(actual.getValues().toList().equals(state.getValues().toList()), "Missing state properties");
                helper.assertTrue(actual.getShape(helper.getLevel(), pos).toAabbs()
                        .equals(state.getShape(helper.getLevel(), pos).toAabbs()), "Wrong outline");
                helper.assertTrue(actual.getCollisionShape(helper.getLevel(), pos).toAabbs()
                        .equals(state.getCollisionShape(helper.getLevel(), pos).toAabbs()), "Wrong collision");
            }
        }
        helper.succeed();
    }

    @GameTest
    public void masonryPlacement(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(POSITION);
        for (int yaw : new int[]{0, 90, 180, 270}) {
            player.setYRot(yaw);
            for (double offset : new double[]{0.2, 0.8}) {
                var hit = new BlockHitResult(new Vec3(pos.getX() + offset, pos.getY(), pos.getZ() + offset),
                        Direction.UP, pos, false);
                for (var reference : java.util.List.of(BlockInit.ANDESITE_PARAPET, BlockInit.ANDESITE_LOUVERED_SHUTTER)) {
                    var stone = block(reference instanceof Parapet ? "parapet" : "louvered_shutter");
                    var expected = reference.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                            reference.asItem().getDefaultInstance(), hit));
                    var actual = stone.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                            stone.asItem().getDefaultInstance(), hit));
                    helper.assertTrue(actual.getValues().toList().equals(expected.getValues().toList()), "Placement differs at yaw " + yaw);
                }
            }
        }
        helper.succeed();
    }

    @GameTest
    public void supportsAndShutterInteraction(GameTestHelper helper) {
        supportsAndShutterInteraction(helper, block("parapet"), block("louvered_shutter"));
    }

    @GameTest
    public void mudBricksSupportsAndShutterInteraction(GameTestHelper helper) {
        var blocks = BuiltInRegistries.BLOCK;
        supportsAndShutterInteraction(helper, blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "mud_bricks_parapet")),
                blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "mud_bricks_louvered_shutter")));
    }

    @GameTest
    public void sandstoneSupportsAndShutterInteraction(GameTestHelper helper) {
        var blocks = BuiltInRegistries.BLOCK;
        supportsAndShutterInteraction(helper, blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "sandstone_parapet")),
                blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "sandstone_louvered_shutter")));
    }

    @GameTest
    public void deepslateSupportsAndShutterInteraction(GameTestHelper helper) {
        var blocks = BuiltInRegistries.BLOCK;
        supportsAndShutterInteraction(helper, blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "deepslate_parapet")),
                blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "deepslate_louvered_shutter")));
    }

    @GameTest
    public void quartzSupportsAndShutterInteraction(GameTestHelper helper) {
        var blocks = BuiltInRegistries.BLOCK;
        supportsAndShutterInteraction(helper, blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "quartz_parapet")),
                blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "quartz_louvered_shutter")));
    }

    @GameTest
    public void netherBricksSupportsAndShutterInteraction(GameTestHelper helper) {
        var blocks = BuiltInRegistries.BLOCK;
        supportsAndShutterInteraction(helper, blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "nether_bricks_parapet")),
                blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "nether_bricks_louvered_shutter")));
    }

    @GameTest
    public void endStoneBricksSupportsAndShutterInteraction(GameTestHelper helper) {
        var blocks = BuiltInRegistries.BLOCK;
        supportsAndShutterInteraction(helper, blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "end_stone_bricks_parapet")),
                blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "end_stone_bricks_louvered_shutter")));
    }

    @GameTest
    public void mossyStoneBricksSupportsAndShutterInteraction(GameTestHelper helper) {
        var blocks = BuiltInRegistries.BLOCK;
        supportsAndShutterInteraction(helper, blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "mossy_stone_bricks_parapet")),
                blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "mossy_stone_bricks_louvered_shutter")));
    }

    @GameTest
    public void redSandstoneSupportsAndShutterInteraction(GameTestHelper helper) {
        var blocks = BuiltInRegistries.BLOCK;
        supportsAndShutterInteraction(helper, blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "red_sandstone_parapet")),
                blocks.getValue(Identifier.fromNamespaceAndPath(NAMESPACE, "red_sandstone_louvered_shutter")));
    }

    private static void supportsAndShutterInteraction(GameTestHelper helper, Block parapet, Block shutterBlock) {
        helper.setBlock(POSITION, parapet);
        helper.setBlock(POSITION.above(), Blocks.FLOWER_POT);
        helper.assertTrue(helper.getBlockState(POSITION).getValue(Parapet.FLOWER) == Parapet.Flower.FLOWER,
                "Flower pot did not enable the support");
        helper.setBlock(POSITION.above(), Blocks.AIR);
        helper.assertTrue(helper.getBlockState(POSITION).getValue(Parapet.FLOWER) == Parapet.Flower.EMPTY,
                "Removing pot did not clear the support");
        shutterInteraction(helper, shutterBlock);
    }

    @GameTest
    public void prismarineShutterInteraction(GameTestHelper helper) {
        shutterInteraction(helper, BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath(NAMESPACE, "prismarine_bricks_louvered_shutter")));
    }

    @GameTest
    public void darkPrismarineShutterInteraction(GameTestHelper helper) {
        shutterInteraction(helper, BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath(NAMESPACE, "dark_prismarine_louvered_shutter")));
    }

    @GameTest
    public void polishedBlackstoneShutterInteraction(GameTestHelper helper) {
        shutterInteraction(helper, BuiltInRegistries.BLOCK.getValue(
                Identifier.fromNamespaceAndPath(NAMESPACE, "polished_blackstone_louvered_shutter")));
    }

    private static void shutterInteraction(GameTestHelper helper, Block shutterBlock) {
        var shutter = shutterBlock.defaultBlockState().setValue(Shutter.OPEN, false);
        helper.setBlock(POSITION, shutter);
        helper.setBlock(POSITION.above(), shutter);
        helper.setBlock(POSITION.east(), BlockInit.ANDESITE_LOUVERED_SHUTTER.defaultBlockState().setValue(Shutter.OPEN, false));
        for (boolean open : new boolean[]{true, false}) {
            helper.useBlock(POSITION);
            helper.assertTrue(helper.getBlockState(POSITION).getValue(Shutter.OPEN) == open
                    && helper.getBlockState(POSITION.above()).getValue(Shutter.OPEN) == open, "Stack toggle failed");
            helper.assertTrue(!helper.getBlockState(POSITION.east()).getValue(Shutter.OPEN), "Provider shutter was toggled");
        }
        helper.succeed();
    }
}
