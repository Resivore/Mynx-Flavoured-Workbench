package com.starfish_studios.bbb.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FacingSlabBlock extends SlabBlock {
    public static final EnumProperty<SlabType> TYPE = BlockStateProperties.SLAB_TYPE;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final VoxelShape SLAB_BOTTOM_UP = Block.box(0, 0, 0, 16, 8, 16);
    public static final VoxelShape SLAB_BOTTOM_DOWN = Block.box(0, 8, 0, 16, 16, 16);
    public static final VoxelShape SLAB_BOTTOM_NORTH = Block.box(0, 0, 8, 16, 16, 16);
    public static final VoxelShape SLAB_BOTTOM_EAST = Block.box(0, 0, 0, 8, 16, 16);
    public static final VoxelShape SLAB_BOTTOM_SOUTH = Block.box(0, 0, 0, 16, 16, 8);
    public static final VoxelShape SLAB_BOTTOM_WEST = Block.box(8, 0, 0, 16, 16, 16);
    public static final VoxelShape SLAB_TOP_UP = Block.box(0, 8, 0, 16, 16, 16);
    public static final VoxelShape SLAB_TOP_DOWN = Block.box(0, 0, 0, 16, 8, 16);
    public static final VoxelShape SLAB_TOP_NORTH = Block.box(0, 0, 8, 16, 16, 16);
    public static final VoxelShape SLAB_TOP_EAST = Block.box(8, 0, 0, 16, 16, 16);
    public static final VoxelShape SLAB_TOP_SOUTH = Block.box(0, 0, 0, 16, 16, 8);
    public static final VoxelShape SLAB_TOP_WEST = Block.box(0, 0, 0, 8, 16, 16);
    public static final VoxelShape SLAB_DOUBLE = Block.box(0, 0, 0, 16, 16, 16);

    public FacingSlabBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(TYPE, SlabType.BOTTOM)
                .setValue(FACING, Direction.UP)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(TYPE) == SlabType.DOUBLE) return SLAB_DOUBLE;
        boolean top = state.getValue(TYPE) == SlabType.TOP;
        return switch (state.getValue(FACING)) {
            case NORTH -> top ? SLAB_TOP_NORTH : SLAB_BOTTOM_NORTH;
            case SOUTH -> top ? SLAB_TOP_SOUTH : SLAB_BOTTOM_SOUTH;
            case EAST -> top ? SLAB_TOP_EAST : SLAB_BOTTOM_EAST;
            case WEST -> top ? SLAB_TOP_WEST : SLAB_BOTTOM_WEST;
            case UP -> top ? SLAB_TOP_UP : SLAB_BOTTOM_UP;
            case DOWN -> top ? SLAB_TOP_DOWN : SLAB_BOTTOM_DOWN;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState current = context.getLevel().getBlockState(context.getClickedPos());
        if (current.is(this) && current.getValue(TYPE) != SlabType.DOUBLE) {
            return current.setValue(TYPE, SlabType.DOUBLE).setValue(WATERLOGGED, false);
        }
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(TYPE, SlabType.BOTTOM)
                .setValue(WATERLOGGED, fluid.is(Fluids.WATER));
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (state.getValue(FACING) != context.getClickedFace()) return false;
        return context.getItemInHand().is(asItem()) && state.getValue(TYPE) != SlabType.DOUBLE
                || super.canBeReplaced(state, context);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE, FACING, WATERLOGGED);
    }
}
