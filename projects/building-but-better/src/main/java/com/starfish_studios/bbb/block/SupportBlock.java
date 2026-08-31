package com.starfish_studios.bbb.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SupportBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock, HammerableBlock {
    public static final MapCodec<SupportBlock> CODEC = simpleCodec(SupportBlock::new);
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty SUPPORT = BooleanProperty.create("support");

    public static final VoxelShape BOTTOM_NORTH_AABB = Shapes.or(
            Block.box(0, 0, 0, 16, 4, 16), Block.box(0, 0, 8, 16, 8, 16), Block.box(0, 0, 12, 16, 16, 16));
    public static final VoxelShape BOTTOM_EAST_AABB = Shapes.or(
            Block.box(0, 0, 0, 16, 4, 16), Block.box(0, 0, 0, 8, 8, 16), Block.box(0, 0, 0, 4, 16, 16));
    public static final VoxelShape BOTTOM_SOUTH_AABB = Shapes.or(
            Block.box(0, 0, 0, 16, 4, 16), Block.box(0, 0, 0, 16, 8, 8), Block.box(0, 0, 0, 16, 16, 4));
    public static final VoxelShape BOTTOM_WEST_AABB = Shapes.or(
            Block.box(0, 0, 0, 16, 4, 16), Block.box(8, 0, 0, 16, 8, 16), Block.box(12, 0, 0, 16, 16, 16));
    public static final VoxelShape TOP_NORTH_AABB = Shapes.or(
            Block.box(0, 0, 12, 16, 16, 16), Block.box(0, 8, 8, 16, 16, 16), Block.box(0, 12, 0, 16, 16, 16));
    public static final VoxelShape TOP_SOUTH_AABB = Shapes.or(
            Block.box(0, 0, 0, 16, 16, 4), Block.box(0, 8, 0, 16, 16, 8), Block.box(0, 12, 0, 16, 16, 16));
    public static final VoxelShape TOP_WEST_AABB = Shapes.or(
            Block.box(12, 0, 0, 16, 16, 16), Block.box(8, 8, 0, 16, 16, 16), Block.box(0, 12, 0, 16, 16, 16));
    public static final VoxelShape TOP_EAST_AABB = Shapes.or(
            Block.box(0, 0, 0, 4, 16, 16), Block.box(0, 8, 0, 8, 16, 16), Block.box(0, 12, 0, 16, 16, 16));

    public SupportBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, Half.BOTTOM)
                .setValue(WATERLOGGED, false)
                .setValue(SUPPORT, true));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> state.getValue(HALF) == Half.BOTTOM ? BOTTOM_NORTH_AABB : TOP_NORTH_AABB;
            case EAST -> state.getValue(HALF) == Half.BOTTOM ? BOTTOM_EAST_AABB : TOP_EAST_AABB;
            case SOUTH -> state.getValue(HALF) == Half.BOTTOM ? BOTTOM_SOUTH_AABB : TOP_SOUTH_AABB;
            case WEST -> state.getValue(HALF) == Half.BOTTOM ? BOTTOM_WEST_AABB : TOP_WEST_AABB;
            default -> Shapes.block();
        };
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
    }

    @Override
    public InteractionResult onHammerUse(BlockState state, Level level, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        BlockState updated = state.cycle(SUPPORT);
        level.setBlock(pos, updated, Block.UPDATE_ALL);
        level.playSound(player, pos, Blocks.SCAFFOLDING.defaultBlockState().getSoundType().getPlaceSound(),
                player.getSoundSource(), 1.0F, 1.0F);
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        Direction face = context.getClickedFace();
        BlockState state;
        if (!context.replacingClickedOnBlock() && face.getAxis().isHorizontal()) {
            state = defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(HALF, context.getClickLocation().y - context.getClickedPos().getY() > 0.5 ? Half.TOP : Half.BOTTOM);
        } else {
            state = defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(HALF, face == Direction.UP ? Half.BOTTOM : Half.TOP);
        }
        return state.setValue(WATERLOGGED, fluid.is(Fluids.WATER));
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
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, WATERLOGGED, SUPPORT);
    }
}
