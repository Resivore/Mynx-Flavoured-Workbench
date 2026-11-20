package dev.resivore.amc.test;

import com.starfish_studios.bbb.block.ColumnBlock;
import com.starfish_studios.bbb.block.FrameBlock;
import com.starfish_studios.bbb.block.MouldingBlock;
import com.starfish_studios.bbb.block.StoneFenceBlock;
import com.starfish_studios.bbb.block.UrnBlock;
import com.starfish_studios.bbb.block.properties.ColumnType;
import com.starfish_studios.bbb.block.properties.FrameStickDirection;
import com.starfish_studios.bbb.registry.BBBContent;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.stream.Collectors;

public final class MasonryTests {
    private static final List<String> FORMS = List.of("column", "urn", "moulding", "fence", "frame");
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;

    private static Identifier id(String form, boolean reference) {
        return Identifier.fromNamespaceAndPath(reference ? "bbb" : "architectural_material_closure",
                (reference ? "stone_" : "andesite_") + form);
    }

    private static Block block(String form, boolean reference) {
        return BuiltInRegistries.BLOCK.getValue(id(form, reference));
    }

    private static void sameState(GameTestHelper helper, BlockState own, BlockState ref) {
        helper.assertTrue(own != null && ref != null && own.getValues().toList().equals(ref.getValues().toList()),
                "State mismatch: " + own + " vs " + ref);
    }

    @GameTest
    public void completeStateAndShapeParity(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(CENTER);
        for (String form : FORMS) {
            var own = block(form, false);
            var ref = block(form, true);
            helper.assertTrue(own != ref && own.getClass() == ref.getClass(), form + " implementation");
            var item = BuiltInRegistries.ITEM.getValue(id(form, false));
            helper.assertTrue(item instanceof BlockItem bi && bi.getBlock() == own
                    && item == own.asItem() && item.getClass() == ref.asItem().getClass(), form + " item class/binding");
            var other = Identifier.fromNamespaceAndPath("bbb", "andesite_" + form);
            helper.assertTrue(!BuiltInRegistries.BLOCK.containsKey(other) && !BuiltInRegistries.ITEM.containsKey(other),
                    form + " already registered by BBB");
            helper.assertTrue(own.builtInRegistryHolder().tags().collect(Collectors.toSet()).equals(
                    ref.builtInRegistryHolder().tags().collect(Collectors.toSet())), form + " loaded block tags");
            helper.assertTrue(item.builtInRegistryHolder().tags().collect(Collectors.toSet()).equals(
                    ref.asItem().builtInRegistryHolder().tags().collect(Collectors.toSet())), form + " loaded item tags");
            for (String suffix : form.equals("frame") ? List.of("") : List.of("", "_from_stonecutting")) {
                helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE,
                        id(form + suffix, false))).isPresent(), form + suffix + " recipe");
            }
            var drops = Block.getDrops(own.defaultBlockState(), level, pos, null);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(item) && drops.getFirst().getCount() == 1,
                    form + " self-drop");
            sameState(helper, own.defaultBlockState(), ref.defaultBlockState());
            var states = ref.getStateDefinition().getPossibleStates();
            helper.assertTrue(states.size() == own.getStateDefinition().getPossibleStates().size(), form + " state count");
            for (var state : states) {
                var tested = own.withPropertiesOf(state);
                sameState(helper, tested, state);
                helper.assertTrue(tested.getShape(level, pos).toAabbs().equals(state.getShape(level, pos).toAabbs())
                        && tested.getCollisionShape(level, pos).toAabbs().equals(state.getCollisionShape(level, pos).toAabbs())
                        && tested.getFluidState() == state.getFluidState()
                        && tested.getPistonPushReaction() == state.getPistonPushReaction(), "Geometry/fluid/piston " + tested);
                for (var rotation : Rotation.values()) sameState(helper, tested.rotate(rotation), state.rotate(rotation));
                for (var mirror : Mirror.values()) sameState(helper, tested.mirror(mirror), state.mirror(mirror));
            }
        }
        helper.succeed();
    }

    @GameTest
    public void wetAndDryPlacement(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (boolean wet : List.of(false, true)) {
            helper.setBlock(CENTER, wet ? Blocks.WATER : Blocks.AIR);
            for (var face : Direction.values()) {
                var support = CENTER.relative(face.getOpposite());
                helper.setBlock(support, Blocks.STONE);
                var pos = helper.absolutePos(support);
                for (int yaw : List.of(0, 90, 180, 270)) {
                    player.setYRot(yaw);
                    for (double height : List.of(0.25, 0.75)) {
                        var location = Vec3.atCenterOf(pos).relative(face, 0.5)
                                .add(0, face.getAxis().isHorizontal() ? height - 0.5 : 0, 0);
                        var hit = new BlockHitResult(location, face, pos, false);
                        for (String form : FORMS) {
                            var own = block(form, false);
                            var ref = block(form, true);
                            var actual = own.getStateForPlacement(new BlockPlaceContext(player, HAND,
                                    own.asItem().getDefaultInstance(), hit));
                            var expected = ref.getStateForPlacement(new BlockPlaceContext(player, HAND,
                                    ref.asItem().getDefaultInstance(), hit));
                            sameState(helper, actual, expected);
                            helper.assertTrue(actual.getValue(BlockStateProperties.WATERLOGGED) == (wet && !form.equals("column")),
                                    form + " wet placement " + face);
                        }
                    }
                }
                helper.setBlock(support, Blocks.AIR);
            }
        }
        helper.succeed();
    }

    private static BlockHitResult hit(GameTestHelper helper) {
        var absolute = helper.absolutePos(CENTER);
        return new BlockHitResult(Vec3.atCenterOf(absolute).add(0, -0.4, 0), Direction.NORTH, absolute, false);
    }

    private static void hammer(GameTestHelper helper, Player player) {
        player.setItemInHand(HAND, BBBContent.HAMMER.getDefaultInstance());
        helper.assertTrue(UseBlockCallback.EVENT.invoker().interact(player, helper.getLevel(), HAND, hit(helper))
                .consumesAction(), "BBB hammer callback");
    }

    @GameTest
    public void columnAndMouldingChanges(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (boolean reference : List.of(true, false)) {
            var column = (ColumnBlock) block("column", reference);
            helper.setBlock(CENTER, column);
            helper.setBlock(CENTER.above(), column);
            helper.assertBlockProperty(CENTER, ColumnBlock.TYPE, ColumnType.BOTTOM);
            helper.setBlock(CENTER.above(), Blocks.AIR);
            helper.assertBlockProperty(CENTER, ColumnBlock.TYPE, ColumnType.NONE);
            hammer(helper, player);
            helper.assertBlockProperty(CENTER, ColumnBlock.LAYER_1_AABB, false);
            helper.assertTrue(column.placeLiquid(helper.getLevel(), helper.absolutePos(CENTER), helper.getBlockState(CENTER),
                    Fluids.WATER.getSource(false)), "Carved column should waterlog");
            hammer(helper, player);
            helper.assertBlockProperty(CENTER, ColumnBlock.WATERLOGGED, false);

            var moulding = block("moulding", reference).defaultBlockState();
            helper.setBlock(CENTER, moulding.setValue(StairBlock.FACING, Direction.NORTH));
            helper.setBlock(CENTER.north(), moulding.setValue(StairBlock.FACING, Direction.EAST));
            helper.assertBlockProperty(CENTER, StairBlock.SHAPE, StairsShape.OUTER_RIGHT);
            boolean dentil = helper.getBlockState(CENTER).getValue(MouldingBlock.DENTIL);
            hammer(helper, player);
            helper.assertBlockProperty(CENTER, MouldingBlock.DENTIL, !dentil);
            helper.setBlock(CENTER.north(), Blocks.AIR);
            helper.assertBlockProperty(CENTER, StairBlock.SHAPE, StairsShape.STRAIGHT);
        }
        helper.succeed();
    }

    @GameTest
    public void fenceAndFrameChanges(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (boolean reference : List.of(true, false)) {
            helper.setBlock(CENTER, block("fence", reference));
            helper.setBlock(CENTER.east(), block("fence", true));
            helper.setBlock(CENTER.west(), Blocks.COBBLESTONE_WALL);
            helper.assertBlockProperty(CENTER, StoneFenceBlock.EAST, true);
            helper.assertBlockProperty(CENTER, StoneFenceBlock.WEST, true);
            hammer(helper, player);
            helper.assertBlockProperty(CENTER, StoneFenceBlock.SIDE_FILL, true);
            player.setShiftKeyDown(true);
            hammer(helper, player);
            player.setShiftKeyDown(false);
            helper.assertBlockProperty(CENTER, StoneFenceBlock.PILLAR, false);
            helper.setBlock(CENTER.east(), Blocks.AIR);
            helper.setBlock(CENTER.west(), Blocks.AIR);
            helper.assertBlockProperty(CENTER, StoneFenceBlock.PILLAR, true);
            helper.assertBlockProperty(CENTER, StoneFenceBlock.SIDE_FILL, false);

            helper.setBlock(CENTER, block("frame", reference));
            helper.setBlock(CENTER.above(), block("frame", true));
            helper.assertBlockProperty(CENTER, FrameBlock.TOP, false);
            helper.setBlock(CENTER.above(), Blocks.AIR);
            helper.assertBlockProperty(CENTER, FrameBlock.TOP, true);
            for (var dir : List.of(FrameStickDirection.LEFT, FrameStickDirection.VERTICAL,
                    FrameStickDirection.RIGHT, FrameStickDirection.HORIZONTAL)) {
                hammer(helper, player);
                helper.assertBlockProperty(CENTER, FrameBlock.FRAME_CENTER, dir);
            }
            helper.getBlockState(CENTER).attack(helper.getLevel(), helper.absolutePos(CENTER), player);
            helper.assertBlockProperty(CENTER, FrameBlock.FRAME_CENTER, FrameStickDirection.NONE);
        }
        helper.succeed();
    }

    @GameTest
    public void urnSoilAndPlant(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(CENTER);
        for (boolean reference : List.of(true, false)) {
            var urn = block("urn", reference);
            helper.setBlock(CENTER, urn);
            var dirt = Items.DIRT.getDefaultInstance();
            helper.getBlockState(CENTER).useItemOn(dirt, helper.getLevel(), player, HAND, hit(helper));
            helper.assertTrue(dirt.isEmpty(), "Urn consumes dirt");
            helper.assertBlockProperty(CENTER, UrnBlock.SOILED, true);
            helper.assertTrue(Blocks.DANDELION.defaultBlockState().canSurvive(helper.getLevel(), pos.above()), "Soiled urn supports flower");
            var drops = Block.getDrops(helper.getBlockState(CENTER), helper.getLevel(), pos, null);
            helper.assertTrue(drops.size() == 2 && drops.stream().anyMatch(stack -> stack.is(Items.DIRT))
                    && drops.stream().anyMatch(stack -> stack.is(urn.asItem())), "Soiled urn drops dirt and urn");
            helper.getBlockState(CENTER).useItemOn(Items.IRON_SHOVEL.getDefaultInstance(), helper.getLevel(), player, HAND, hit(helper));
            helper.assertBlockProperty(CENTER, UrnBlock.SOILED, false);
        }
        helper.succeed();
    }
}
