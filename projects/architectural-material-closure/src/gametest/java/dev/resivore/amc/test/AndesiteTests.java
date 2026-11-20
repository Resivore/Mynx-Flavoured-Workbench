package dev.resivore.amc.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.kikoz.mcwwindows.init.BlockInit;
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
import java.util.stream.Collectors;

public final class AndesiteTests {
    private static final BlockPos LEFT = new BlockPos(0, 1, 1);
    private static final BlockPos RIGHT = new BlockPos(2, 1, 1);
    private static final Map<String, Block> REFERENCES = Map.of(
            "button", Blocks.STONE_BUTTON, "pressure_plate", Blocks.STONE_PRESSURE_PLATE,
            "gothic_window", BlockInit.STONE_BRICK_GOTHIC, "arrow_slit", BlockInit.STONE_BRICK_ARROW_SLIT);

    private static Identifier id(String form) {
        return Identifier.fromNamespaceAndPath("architectural_material_closure", "andesite_" + form);
    }

    private static Block block(String form) {
        return BuiltInRegistries.BLOCK.getValue(id(form));
    }

    @GameTest
    public void referenceStatesAndData(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(LEFT);
        REFERENCES.forEach((form, ref) -> {
            var own = block(form);
            var item = BuiltInRegistries.ITEM.getValue(id(form));
            helper.assertTrue(own != ref && own.getClass() == ref.getClass(), form + " implementation");
            helper.assertTrue(item instanceof BlockItem bi && bi.getBlock() == own && own.asItem() == item,
                    form + " item binding");
            helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, id(form))).isPresent(),
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
    public void placement(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var center = new BlockPos(1, 1, 1);
        for (var face : Direction.values()) {
            var support = center.relative(face.getOpposite());
            helper.setBlock(support, Blocks.STONE);
            var pos = helper.absolutePos(support);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos).relative(face, 0.5), face, pos, false);
            for (int yaw : new int[]{0, 90, 180, 270}) {
                player.setYRot(yaw);
                REFERENCES.forEach((form, ref) -> {
                    var own = block(form);
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
    public void buttonPulse(GameTestHelper helper) {
        for (var pos : List.of(LEFT, RIGHT)) {
            var button = pos.equals(LEFT) ? block("button") : Blocks.STONE_BUTTON;
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
    public void pressurePlateSensitivity(GameTestHelper helper) {
        for (var pos : List.of(LEFT, RIGHT)) {
            helper.setBlock(pos.below(), Blocks.STONE);
            helper.setBlock(pos, pos.equals(LEFT) ? block("pressure_plate") : Blocks.STONE_PRESSURE_PLATE);
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
    public void stackedMasonry(GameTestHelper helper) {
        for (String form : List.of("gothic_window", "arrow_slit")) {
            for (var base : List.of(LEFT, RIGHT)) {
                var material = base.equals(LEFT) ? block(form) : REFERENCES.get(form);
                helper.setBlock(base, material);
                helper.assertBlockProperty(base, WindowBase.PART, WindowPart.BASE);
                helper.setBlock(base.above(), material);
                helper.setBlock(base.above(2), material);
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
