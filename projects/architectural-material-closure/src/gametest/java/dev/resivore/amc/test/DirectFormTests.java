package dev.resivore.amc.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.kikoz.mcwwindows.init.BlockInit;
import net.kikoz.mcwwindows.init.ItemInit;
import net.kikoz.mcwwindows.objects.ConnectedWindow;
import net.kikoz.mcwwindows.objects.WindowBarred;
import net.kikoz.mcwwindows.objects.GothicWindow;
import net.kikoz.mcwwindows.objects.WindowBase;
import net.kikoz.mcwwindows.util.WindowPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public final class DirectFormTests {
    private static final BlockPos LEFT = new BlockPos(0, 1, 1);
    private static final BlockPos RIGHT = new BlockPos(2, 1, 1);
    private static final Map<String, Block> REFERENCES = Map.of(
            "button", Blocks.STONE_BUTTON, "pressure_plate", Blocks.STONE_PRESSURE_PLATE,
            "gothic_window", BlockInit.STONE_BRICK_GOTHIC, "arrow_slit", BlockInit.STONE_BRICK_ARROW_SLIT);

    private static final Map<String, Block> MUD_REFERENCES = Map.of(
            "button", Blocks.STONE_BUTTON, "pressure_plate", Blocks.STONE_PRESSURE_PLATE,
            "window", BlockInit.ANDESITE_WINDOW, "window2", BlockInit.ANDESITE_WINDOW2,
            "four_window", BlockInit.ANDESITE_FOUR_WINDOW, "pane_window", BlockInit.ANDESITE_PANE_WINDOW,
            "parapet", BlockInit.ANDESITE_PARAPET, "louvered_shutter", BlockInit.ANDESITE_LOUVERED_SHUTTER);

    static final List<String> PATH_FORMS = List.of("running_bond_path", "strewn_rocky_path", "windmill_weave_path",
            "flagstone_path", "crystal_floor_path", "diamond_paving", "basket_weave_paving", "square_paving",
            "honeycomb_paving", "clover_paving", "dumble_paving", "running_bond", "flagstone", "windmill_weave",
            "crystal_floor");

    static Map<String, Block> references(String material) {
        var result = new LinkedHashMap<String, Block>();
        switch (material) {
            case "sandstone", "red_sandstone", "quartz", "deepslate" -> {
                result.putAll(REFERENCES);
                result.put("parapet", BlockInit.ANDESITE_PARAPET);
                result.put("louvered_shutter", BlockInit.ANDESITE_LOUVERED_SHUTTER);
            }
            case "polished_blackstone" -> result.put("louvered_shutter", BlockInit.ANDESITE_LOUVERED_SHUTTER);
            case "end_stone_bricks" -> {
                result.putAll(MUD_REFERENCES);
                result.remove("button");
                result.remove("pressure_plate");
            }
            case "mud_bricks", "nether_bricks" -> result.putAll(MUD_REFERENCES);
            case "prismarine_bricks", "dark_prismarine" -> {
                result.put("button", Blocks.STONE_BUTTON);
                result.put("pressure_plate", Blocks.STONE_PRESSURE_PLATE);
                result.put("louvered_shutter", BlockInit.ANDESITE_LOUVERED_SHUTTER);
            }
            default -> result.putAll(REFERENCES);
        }
        if (List.of("prismarine_bricks", "quartz", "nether_bricks", "end_stone_bricks").contains(material)) {
            for (var form : PATH_FORMS) {
                result.put(form, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("mcwpaths", "dark_prismarine_" + form)));
            }
        }
        return result;
    }

    private static Identifier id(String material, String form) {
        return Identifier.fromNamespaceAndPath("architectural_material_closure", material + "_" + form);
    }

    private static Block block(String material, String form) {
        return BuiltInRegistries.BLOCK.getValue(id(material, form));
    }

    @GameTest
    public void andesiteReferenceStatesAndData(GameTestHelper helper) {
        referenceStatesAndData(helper, "andesite");
    }

    @GameTest
    public void dioriteReferenceStatesAndData(GameTestHelper helper) {
        referenceStatesAndData(helper, "diorite");
    }

    @GameTest
    public void graniteReferenceStatesAndData(GameTestHelper helper) {
        referenceStatesAndData(helper, "granite");
    }

    @GameTest
    public void sandstoneReferenceStatesAndData(GameTestHelper helper) {
        referenceStatesAndData(helper, "sandstone");
    }

    @GameTest
    public void deepslateReferenceStatesAndData(GameTestHelper helper) {
        referenceStatesAndData(helper, "deepslate");
    }

    @GameTest
    public void quartzReferenceStatesAndData(GameTestHelper helper) {
        referenceStatesAndData(helper, "quartz");
    }

    @GameTest
    public void netherBricksReferenceStatesAndData(GameTestHelper helper) {
        referenceStatesAndData(helper, "nether_bricks");
    }

    @GameTest
    public void polishedBlackstoneReferenceStatesAndData(GameTestHelper helper) {
        referenceStatesAndData(helper, "polished_blackstone");
    }

    @GameTest
    public void endStoneBricksReferenceStatesAndData(GameTestHelper helper) {
        referenceStatesAndData(helper, "end_stone_bricks");
    }

    @GameTest
    public void redSandstoneReferenceStatesAndData(GameTestHelper helper) {
        referenceStatesAndData(helper, "red_sandstone");
    }

    @GameTest
    public void mudBricksReferenceStatesAndData(GameTestHelper helper) {
        referenceStatesAndData(helper, "mud_bricks");
    }

    @GameTest
    public void prismarineBricksReferenceStatesAndData(GameTestHelper helper) {
        referenceStatesAndData(helper, "prismarine_bricks");
    }

    @GameTest
    public void darkPrismarineReferenceStatesAndData(GameTestHelper helper) {
        referenceStatesAndData(helper, "dark_prismarine");
    }

    private static void referenceStatesAndData(GameTestHelper helper, String material) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(LEFT);
        references(material).forEach((form, ref) -> {
            var own = block(material, form);
            var item = BuiltInRegistries.ITEM.getValue(id(material, form));
            helper.assertTrue(own != ref && own.getClass() == ref.getClass(), form + " implementation");
            helper.assertTrue(item instanceof BlockItem bi && bi.getBlock() == own && own.asItem() == item
                    && item.getClass() == ref.asItem().getClass(),
                    form + " item binding");
            helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, id(material, form))).isPresent(),
                    form + " recipe missing");
            var drops = Block.getDrops(own.defaultBlockState(), level, pos, null);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(item) && drops.getFirst().getCount() == 1,
                    form + " self-drop");
            helper.assertTrue(own.builtInRegistryHolder().tags().collect(Collectors.toSet())
                    .equals(ref.builtInRegistryHolder().tags().collect(Collectors.toSet())), form + " block tags");
            helper.assertTrue(item.builtInRegistryHolder().tags().collect(Collectors.toSet())
                    .equals(ref.asItem().builtInRegistryHolder().tags().collect(Collectors.toSet())), form + " item tags");
            helper.assertTrue(own.defaultBlockState().getValues().toList().equals(ref.defaultBlockState().getValues().toList()),
                    form + " default state");
            helper.assertTrue(own.getStateDefinition().getPossibleStates().size() == ref.getStateDefinition().getPossibleStates().size(),
                    form + " state count");
            for (var expected : ref.getStateDefinition().getPossibleStates()) {
                var actual = own.withPropertiesOf(expected);
                helper.assertTrue(actual.getValues().toList().equals(expected.getValues().toList()), form + " state");
                helper.assertTrue(actual.getShape(level, pos).toAabbs().equals(expected.getShape(level, pos).toAabbs())
                        && actual.getCollisionShape(level, pos).toAabbs().equals(expected.getCollisionShape(level, pos).toAabbs()),
                        form + " outline/collision " + expected);
                for (var rotation : net.minecraft.world.level.block.Rotation.values()) {
                    helper.assertTrue(actual.rotate(rotation).getValues().toList().equals(
                            expected.rotate(rotation).getValues().toList()), form + " rotation");
                }
                for (var dir : Direction.values()) {
                    helper.assertTrue(actual.getSignal(level, pos, dir) == expected.getSignal(level, pos, dir)
                            && actual.getDirectSignal(level, pos, dir) == expected.getDirectSignal(level, pos, dir),
                            form + " redstone output " + dir);
                }
            }
        });
        helper.succeed();
    }

    @GameTest
    public void andesitePlacement(GameTestHelper helper) {
        placement(helper, "andesite");
    }

    @GameTest
    public void dioritePlacement(GameTestHelper helper) {
        placement(helper, "diorite");
    }

    @GameTest
    public void granitePlacement(GameTestHelper helper) {
        placement(helper, "granite");
    }

    @GameTest
    public void sandstonePlacement(GameTestHelper helper) {
        placement(helper, "sandstone");
    }

    @GameTest
    public void deepslatePlacement(GameTestHelper helper) {
        placement(helper, "deepslate");
    }

    @GameTest
    public void quartzPlacement(GameTestHelper helper) {
        placement(helper, "quartz");
    }

    @GameTest
    public void netherBricksPlacement(GameTestHelper helper) {
        placement(helper, "nether_bricks");
    }

    @GameTest
    public void polishedBlackstonePlacement(GameTestHelper helper) {
        placement(helper, "polished_blackstone");
    }

    @GameTest
    public void endStoneBricksPlacement(GameTestHelper helper) {
        placement(helper, "end_stone_bricks");
    }

    @GameTest
    public void redSandstonePlacement(GameTestHelper helper) {
        placement(helper, "red_sandstone");
    }

    @GameTest
    public void mudBricksPlacement(GameTestHelper helper) {
        placement(helper, "mud_bricks");
    }

    @GameTest
    public void prismarineBricksPlacement(GameTestHelper helper) {
        placement(helper, "prismarine_bricks");
    }

    @GameTest
    public void darkPrismarinePlacement(GameTestHelper helper) {
        placement(helper, "dark_prismarine");
    }

    private static void placement(GameTestHelper helper, String material) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var center = new BlockPos(1, 1, 1);
        for (var face : Direction.values()) {
            var support = center.relative(face.getOpposite());
            helper.setBlock(support, Blocks.STONE);
            var pos = helper.absolutePos(support);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos).relative(face, 0.5), face, pos, false);
            for (int yaw : new int[]{0, 90, 180, 270}) {
                player.setYRot(yaw);
                references(material).forEach((form, ref) -> {
                    var own = block(material, form);
                    var expected = ref.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                            ref.asItem().getDefaultInstance(), hit));
                    var actual = own.getStateForPlacement(new BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                            own.asItem().getDefaultInstance(), hit));
                    helper.assertTrue(actual != null && expected != null
                            && actual.getValues().toList().equals(expected.getValues().toList()), form + " placement " + face);
                });
            }
            helper.setBlock(support, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest
    public void controlSupports(GameTestHelper helper) {
        for (String material : List.of("sandstone", "red_sandstone", "deepslate", "quartz", "nether_bricks")) {
            for (String form : List.of("button", "pressure_plate")) {
                for (Block control : List.of(block(material, form), REFERENCES.get(form))) {
                    helper.setBlock(LEFT.below(), Blocks.STONE);
                    var state = control.defaultBlockState();
                    if (form.equals("button")) state = state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
                    helper.setBlock(LEFT, state);
                    helper.assertTrue(helper.getBlockState(LEFT).is(control), "Control on support");
                    helper.setBlock(LEFT.below(), Blocks.AIR);
                    helper.assertTrue(helper.getBlockState(LEFT).isAir(), "Control after support removal");
                }
            }
        }
        helper.succeed();
    }

    @GameTest
    public void andesiteButtonPulse(GameTestHelper helper) {
        buttonPulse(helper, "andesite");
    }

    @GameTest
    public void dioriteButtonPulse(GameTestHelper helper) {
        buttonPulse(helper, "diorite");
    }

    @GameTest
    public void graniteButtonPulse(GameTestHelper helper) {
        buttonPulse(helper, "granite");
    }

    @GameTest
    public void sandstoneButtonPulse(GameTestHelper helper) {
        buttonPulse(helper, "sandstone");
    }

    @GameTest
    public void deepslateButtonPulse(GameTestHelper helper) {
        buttonPulse(helper, "deepslate");
    }

    @GameTest
    public void quartzButtonPulse(GameTestHelper helper) {
        buttonPulse(helper, "quartz");
    }

    @GameTest
    public void netherBricksButtonPulse(GameTestHelper helper) {
        buttonPulse(helper, "nether_bricks");
    }

    @GameTest
    public void redSandstoneButtonPulse(GameTestHelper helper) {
        buttonPulse(helper, "red_sandstone");
    }

    @GameTest
    public void mudBricksButtonPulse(GameTestHelper helper) {
        buttonPulse(helper, "mud_bricks");
    }

    @GameTest
    public void prismarineBricksButtonPulse(GameTestHelper helper) {
        buttonPulse(helper, "prismarine_bricks");
    }

    @GameTest
    public void darkPrismarineButtonPulse(GameTestHelper helper) {
        buttonPulse(helper, "dark_prismarine");
    }

    private static void buttonPulse(GameTestHelper helper, String material) {
        for (var pos : List.of(LEFT, RIGHT)) {
            var button = pos.equals(LEFT) ? block(material, "button") : Blocks.STONE_BUTTON;
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos, button.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR));
            helper.useBlock(pos);
            helper.assertBlockProperty(pos, BlockStateProperties.POWERED, true);
        }
        helper.runAfterDelay(19, () -> {
            for (var pos : List.of(LEFT, RIGHT)) helper.assertBlockProperty(pos, BlockStateProperties.POWERED, true);
        });
        helper.runAfterDelay(20, () -> {
            for (var pos : List.of(LEFT, RIGHT)) helper.assertBlockProperty(pos, BlockStateProperties.POWERED, false);
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void andesitePressurePlateSensitivity(GameTestHelper helper) {
        pressurePlateSensitivity(helper, "andesite");
    }

    @GameTest(maxTicks = 40)
    public void dioritePressurePlateSensitivity(GameTestHelper helper) {
        pressurePlateSensitivity(helper, "diorite");
    }

    @GameTest(maxTicks = 40)
    public void granitePressurePlateSensitivity(GameTestHelper helper) {
        pressurePlateSensitivity(helper, "granite");
    }

    @GameTest(maxTicks = 40)
    public void sandstonePressurePlateSensitivity(GameTestHelper helper) {
        pressurePlateSensitivity(helper, "sandstone");
    }

    @GameTest(maxTicks = 40)
    public void deepslatePressurePlateSensitivity(GameTestHelper helper) {
        pressurePlateSensitivity(helper, "deepslate");
    }

    @GameTest(maxTicks = 40)
    public void quartzPressurePlateSensitivity(GameTestHelper helper) {
        pressurePlateSensitivity(helper, "quartz");
    }

    @GameTest(maxTicks = 40)
    public void netherBricksPressurePlateSensitivity(GameTestHelper helper) {
        pressurePlateSensitivity(helper, "nether_bricks");
    }

    @GameTest(maxTicks = 40)
    public void redSandstonePressurePlateSensitivity(GameTestHelper helper) {
        pressurePlateSensitivity(helper, "red_sandstone");
    }

    @GameTest(maxTicks = 40)
    public void mudBricksPressurePlateSensitivity(GameTestHelper helper) {
        pressurePlateSensitivity(helper, "mud_bricks");
    }

    @GameTest(maxTicks = 40)
    public void prismarineBricksPressurePlateSensitivity(GameTestHelper helper) {
        pressurePlateSensitivity(helper, "prismarine_bricks");
    }

    @GameTest(maxTicks = 40)
    public void darkPrismarinePressurePlateSensitivity(GameTestHelper helper) {
        pressurePlateSensitivity(helper, "dark_prismarine");
    }

    private static void pressurePlateSensitivity(GameTestHelper helper, String material) {
        for (var pos : List.of(LEFT, RIGHT)) {
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos, pos.equals(LEFT) ? block(material, "pressure_plate") : Blocks.STONE_PRESSURE_PLATE);
            helper.spawnItem(Items.ANDESITE, Vec3.atBottomCenterOf(pos));
        }
        helper.runAfterDelay(4, () -> {
            for (var pos : List.of(LEFT, RIGHT)) helper.assertBlockProperty(pos, BlockStateProperties.POWERED, false);
            var leftMob = helper.spawnWithNoFreeWill(EntityTypes.CHICKEN, LEFT);
            var rightMob = helper.spawnWithNoFreeWill(EntityTypes.CHICKEN, RIGHT);
            helper.runAfterDelay(4, () -> {
                for (var pos : List.of(LEFT, RIGHT)) helper.assertBlockProperty(pos, BlockStateProperties.POWERED, true);
                leftMob.discard();
                rightMob.discard();
            });
        });
        helper.runAfterDelay(32, () -> {
            for (var pos : List.of(LEFT, RIGHT)) helper.assertBlockProperty(pos, BlockStateProperties.POWERED, false);
            helper.succeed();
        });
    }

    @GameTest
    public void prismarineEngraving(GameTestHelper helper) {
        engraving(helper, "prismarine_bricks");
    }

    @GameTest
    public void quartzEngraving(GameTestHelper helper) {
        engraving(helper, "quartz");
    }

    @GameTest
    public void netherBricksEngraving(GameTestHelper helper) {
        engraving(helper, "nether_bricks");
    }

    @GameTest
    public void endStoneBricksEngraving(GameTestHelper helper) {
        engraving(helper, "end_stone_bricks");
    }

    private static void engraving(GameTestHelper helper, String material) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(LEFT);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        for (String form : List.of("running_bond", "flagstone", "windmill_weave", "crystal_floor")) {
            for (Block block : List.of(block(material, form), references(material).get(form))) {
                helper.setBlock(LEFT, block);
                var tool = Items.IRON_PICKAXE.getDefaultInstance();
                player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                for (boolean engraved : List.of(true, false)) {
                    helper.getBlockState(LEFT).useItemOn(tool, level, player, InteractionHand.MAIN_HAND, hit);
                    helper.assertBlockProperty(LEFT, com.mcwpaths.kikoz.objects.EngravedBlock.ENGRAVED, engraved);
                }
            }
        }
        helper.succeed();
    }

    @GameTest
    public void mudBricksWindowInteractions(GameTestHelper helper) {
        windowInteractions(helper, "mud_bricks");
    }

    @GameTest
    public void netherBricksWindowInteractions(GameTestHelper helper) {
        windowInteractions(helper, "nether_bricks");
    }

    @GameTest
    public void endStoneBricksWindowInteractions(GameTestHelper helper) {
        windowInteractions(helper, "end_stone_bricks");
    }

    private static void windowInteractions(GameTestHelper helper, String material) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var absolute = helper.absolutePos(LEFT);
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false);
        for (String form : List.of("window", "window2", "four_window", "pane_window")) {
            for (Block window : List.of(MUD_REFERENCES.get(form), block(material, form))) {
                helper.setBlock(LEFT, window);
                if (form.equals("window")) {
                    helper.setBlock(LEFT.above(), window);
                    helper.assertBlockProperty(LEFT, ConnectedWindow.PART, ConnectedWindow.ConnectionStatus.BOTTOM);
                    helper.setBlock(LEFT.above(), Blocks.AIR);
                    helper.assertBlockProperty(LEFT, ConnectedWindow.PART, ConnectedWindow.ConnectionStatus.BASE);
                } else {
                    helper.useBlock(LEFT);
                    var open = helper.getBlockState(LEFT).getValue(WindowBarred.WINDOWSTATE);
                    helper.assertTrue(open == WindowBarred.WindowState.OPEN_LEFT || open == WindowBarred.WindowState.OPEN_RIGHT,
                            form + " failed to open");
                    helper.useBlock(LEFT);
                    helper.assertBlockProperty(LEFT, WindowBarred.WINDOWSTATE, WindowBarred.WindowState.CLOSED);
                    helper.getBlockState(LEFT).useItemOn(ItemInit.KEY.getDefaultInstance(), helper.getLevel(), player,
                            InteractionHand.MAIN_HAND, hit);
                    helper.assertBlockProperty(LEFT, WindowBarred.WINDOWSTATE, WindowBarred.WindowState.LOCKED);
                    helper.useBlock(LEFT);
                    helper.assertBlockProperty(LEFT, WindowBarred.WINDOWSTATE, WindowBarred.WindowState.LOCKED);
                }
                helper.setBlock(LEFT, Blocks.AIR);
            }
        }
        helper.succeed();
    }

    @GameTest
    public void andesiteStackedMasonry(GameTestHelper helper) {
        stackedMasonry(helper, "andesite");
    }

    @GameTest
    public void dioriteStackedMasonry(GameTestHelper helper) {
        stackedMasonry(helper, "diorite");
    }

    @GameTest
    public void graniteStackedMasonry(GameTestHelper helper) {
        stackedMasonry(helper, "granite");
    }

    @GameTest
    public void sandstoneStackedMasonry(GameTestHelper helper) {
        stackedMasonry(helper, "sandstone");
    }

    @GameTest
    public void deepslateStackedMasonry(GameTestHelper helper) {
        stackedMasonry(helper, "deepslate");
    }

    @GameTest
    public void quartzStackedMasonry(GameTestHelper helper) {
        stackedMasonry(helper, "quartz");
    }

    @GameTest
    public void redSandstoneStackedMasonry(GameTestHelper helper) {
        stackedMasonry(helper, "red_sandstone");
    }

    private static void stackedMasonry(GameTestHelper helper, String material) {
        for (String form : List.of("gothic_window", "arrow_slit")) {
            for (var base : List.of(LEFT, RIGHT)) {
                var block = base.equals(LEFT) ? block(material, form) : REFERENCES.get(form);
                helper.setBlock(base, block);
                helper.assertBlockProperty(base, WindowBase.PART, WindowPart.BASE);
                helper.setBlock(base.above(), block);
                helper.setBlock(base.above(2), block);
                helper.assertBlockProperty(base, WindowBase.PART, WindowPart.BOTTOM);
                helper.assertBlockProperty(base.above(), WindowBase.PART, WindowPart.MIDDLE);
                helper.assertBlockProperty(base.above(2), WindowBase.PART, WindowPart.TOP);
                if (form.equals("gothic_window")) {
                    var initial = helper.getBlockState(base).getValue(GothicWindow.OPEN);
                    helper.useBlock(base);
                    helper.assertBlockProperty(base.above(2), GothicWindow.OPEN, !initial);
                    helper.useBlock(base);
                    helper.assertBlockProperty(base.above(), GothicWindow.OPEN, initial);
                }
                helper.setBlock(base.above(), Blocks.AIR);
                helper.assertBlockProperty(base, WindowBase.PART, WindowPart.BASE);
                helper.assertBlockProperty(base.above(2), WindowBase.PART, WindowPart.BASE);
                helper.setBlock(base.above(2), Blocks.AIR);
                helper.setBlock(base, Blocks.AIR);
            }
        }
        helper.succeed();
    }
}
