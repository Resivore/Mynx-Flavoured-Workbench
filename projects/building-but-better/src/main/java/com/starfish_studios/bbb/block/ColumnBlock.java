package com.starfish_studios.bbb.block;

import com.starfish_studios.bbb.block.properties.BBBBlockStateProperties;
import com.starfish_studios.bbb.block.properties.ColumnType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class ColumnBlock extends Block implements SimpleWaterloggedBlock, HammerableBlock {
    public static final BooleanProperty LAYER_1_AABB = BBBBlockStateProperties.LAYER_1;
    public static final BooleanProperty LAYER_2_AABB = BBBBlockStateProperties.LAYER_2;
    public static final BooleanProperty LAYER_3_AABB = BBBBlockStateProperties.LAYER_3;
    public static final BooleanProperty LAYER_4_AABB = BBBBlockStateProperties.LAYER_4;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final EnumProperty<ColumnType> TYPE = BBBBlockStateProperties.COLUMN_TYPE;

    private static final BooleanProperty[] LAYER_PROPERTIES =
            {LAYER_1_AABB, LAYER_2_AABB, LAYER_3_AABB, LAYER_4_AABB};
    private static final Map<Direction.Axis, VoxelShape> CENTER_AABB = new EnumMap<>(Direction.Axis.class);
    private static final Map<Direction.Axis, VoxelShape[]> LAYER_AABB = new EnumMap<>(Direction.Axis.class);

    static {
        CENTER_AABB.put(Direction.Axis.Y, Block.box(2, 0, 2, 14, 16, 14));
        CENTER_AABB.put(Direction.Axis.X, Block.box(0, 2, 2, 16, 14, 14));
        CENTER_AABB.put(Direction.Axis.Z, Block.box(2, 2, 0, 14, 14, 16));
        LAYER_AABB.put(Direction.Axis.Y, new VoxelShape[]{
                Block.box(0, 0, 0, 16, 4, 16), Block.box(0, 4, 0, 16, 8, 16),
                Block.box(0, 8, 0, 16, 12, 16), Block.box(0, 12, 0, 16, 16, 16)});
        LAYER_AABB.put(Direction.Axis.X, new VoxelShape[]{
                Block.box(0, 0, 0, 4, 16, 16), Block.box(4, 0, 0, 8, 16, 16),
                Block.box(8, 0, 0, 12, 16, 16), Block.box(12, 0, 0, 16, 16, 16)});
        LAYER_AABB.put(Direction.Axis.Z, new VoxelShape[]{
                Block.box(0, 0, 12, 16, 16, 16), Block.box(0, 0, 8, 16, 16, 12),
                Block.box(0, 0, 4, 16, 16, 8), Block.box(0, 0, 0, 16, 16, 4)});
    }

    public ColumnBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(TYPE, ColumnType.NONE)
                .setValue(LAYER_1_AABB, true)
                .setValue(LAYER_2_AABB, true)
                .setValue(LAYER_3_AABB, true)
                .setValue(LAYER_4_AABB, true)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState()
                .setValue(AXIS, context.getClickedFace().getAxis())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
        return removeWaterIfFull(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER_AABB.get(state.getValue(AXIS));
        VoxelShape[] layers = LAYER_AABB.get(state.getValue(AXIS));
        for (int i = 0; i < LAYER_PROPERTIES.length; i++) {
            if (state.getValue(LAYER_PROPERTIES[i])) {
                shape = Shapes.or(shape, layers[i]);
            }
        }
        return shape;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        if (direction.getAxis() == state.getValue(AXIS)) {
            Direction.Axis axis = state.getValue(AXIS);
            BlockState positive = level.getBlockState(pos.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE)));
            BlockState negative = level.getBlockState(pos.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE)));
            state = state.setValue(TYPE, determineColumnType(state, positive, negative));
        }
        state = removeWaterIfFull(state);
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state;
    }

    @Override
    public InteractionResult onHammerUse(BlockState state, Level level, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        Direction.Axis axis = state.getValue(AXIS);
        double hitFraction = hit.getLocation().get(axis) - pos.get(axis);
        if (axis == Direction.Axis.Z) {
            hitFraction = 1.0D - hitFraction;
        }
        int layerIndex = Math.max(0, Math.min(3, (int) (hitFraction * 4.0D)));
        BlockState updated = removeWaterIfFull(state.cycle(LAYER_PROPERTIES[layerIndex]));
        level.setBlock(pos, updated, Block.UPDATE_ALL);
        level.playSound(player, pos, updated.getSoundType().getPlaceSound(),
                player.getSoundSource(), 1.0F, 1.0F);
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public boolean canPlaceLiquid(LivingEntity placer, BlockGetter level, BlockPos pos,
                                  BlockState state, Fluid fluid) {
        return !isFull(state) && SimpleWaterloggedBlock.super.canPlaceLiquid(placer, level, pos, state, fluid);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return !isFull(state) && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluidState);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    private static boolean isFull(BlockState state) {
        for (BooleanProperty property : LAYER_PROPERTIES) {
            if (!state.getValue(property)) {
                return false;
            }
        }
        return true;
    }

    private static BlockState removeWaterIfFull(BlockState state) {
        return isFull(state) ? state.setValue(WATERLOGGED, false) : state;
    }

    private static ColumnType determineColumnType(BlockState state, BlockState positive, BlockState negative) {
        boolean connectsPositive = canConnect(state, positive);
        boolean connectsNegative = canConnect(state, negative);
        if (connectsPositive && !connectsNegative) return ColumnType.BOTTOM;
        if (!connectsPositive && connectsNegative) return ColumnType.TOP;
        if (connectsPositive) return ColumnType.MIDDLE;
        return ColumnType.NONE;
    }

    private static boolean canConnect(BlockState state, BlockState other) {
        return other.getBlock() instanceof ColumnBlock
                && state.getValue(AXIS) == other.getValue(AXIS)
                && isFull(state) == isFull(other);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYER_1_AABB, LAYER_2_AABB, LAYER_3_AABB, LAYER_4_AABB,
                WATERLOGGED, AXIS, TYPE);
    }
}
