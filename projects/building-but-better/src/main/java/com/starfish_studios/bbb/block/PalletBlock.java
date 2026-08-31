package com.starfish_studios.bbb.block;

import com.mojang.serialization.MapCodec;
import com.starfish_studios.bbb.block.properties.BBBBlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PalletBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock, HammerableBlock {
    public static final MapCodec<PalletBlock> CODEC = simpleCodec(PalletBlock::new);
    protected static final VoxelShape BOTTOM_AABB = Block.box(0, 0, 0, 16, 6, 16);
    protected static final VoxelShape TOP_AABB = Block.box(0, 10, 0, 16, 16, 16);
    protected static final VoxelShape NORTH_AABB = Block.box(0, 0, 10, 16, 16, 16);
    protected static final VoxelShape EAST_AABB = Block.box(0, 0, 0, 6, 16, 16);
    protected static final VoxelShape SOUTH_AABB = Block.box(0, 0, 0, 16, 16, 6);
    protected static final VoxelShape WEST_AABB = Block.box(10, 0, 0, 16, 16, 16);

    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
    public static final BooleanProperty LAYER_1 = BBBBlockStateProperties.LAYER_1;
    public static final BooleanProperty LAYER_2 = BBBBlockStateProperties.LAYER_2;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public PalletBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(WATERLOGGED, false)
                .setValue(HALF, Half.BOTTOM)
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(LAYER_1, true)
                .setValue(LAYER_2, true));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(OPEN)) {
            return switch (state.getValue(FACING)) {
                case NORTH -> NORTH_AABB;
                case SOUTH -> SOUTH_AABB;
                case WEST -> WEST_AABB;
                case EAST -> EAST_AABB;
                default -> BOTTOM_AABB;
            };
        }
        return state.getValue(HALF) == Half.TOP ? TOP_AABB : BOTTOM_AABB;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return !state.getValue(OPEN) && super.isPathfindable(state, type);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        BlockState updated = state.cycle(OPEN);
        level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        if (updated.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        level.playSound(player, pos,
                updated.getValue(OPEN) ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, updated.getValue(OPEN) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult onHammerUse(BlockState state, Level level, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        double x = hit.getLocation().x - pos.getX();
        double y = hit.getLocation().y - pos.getY();
        double z = hit.getLocation().z - pos.getZ();
        BlockState updated = state;
        if (z > 0.5 && state.getValue(FACING) == Direction.NORTH) {
            updated = updated.cycle(z < 0.75 ? LAYER_1 : LAYER_2);
        } else if (z < 0.5 && state.getValue(FACING) == Direction.SOUTH) {
            updated = updated.cycle(z > 0.25 ? LAYER_1 : LAYER_2);
        } else if (x > 0.5 && state.getValue(FACING) == Direction.WEST) {
            updated = updated.cycle(x < 0.75 ? LAYER_1 : LAYER_2);
        } else if (x < 0.5 && state.getValue(FACING) == Direction.EAST) {
            updated = updated.cycle(x > 0.25 ? LAYER_1 : LAYER_2);
        } else if (y < 0.5 && state.getValue(HALF) == Half.BOTTOM) {
            updated = updated.cycle(y < 0.25 ? LAYER_1 : LAYER_2);
        }
        // This is intentionally a separate check, matching 2.0pre4's exact
        // top-half hit ordering even when an open pallet also matched above.
        if (y > 0.5 && state.getValue(HALF) == Half.TOP) {
            updated = updated.cycle(y < 0.75 ? LAYER_1 : LAYER_2);
        }
        if (!updated.getValue(LAYER_1) && !updated.getValue(LAYER_2)) {
            updated = updated.setValue(LAYER_1, true).setValue(LAYER_2, true);
        }
        level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        level.playSound(player, pos, Blocks.SCAFFOLDING.defaultBlockState().getSoundType().getPlaceSound(),
                player.getSoundSource(), 1.0F, 1.0F);
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
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
        builder.add(HALF, FACING, WATERLOGGED, OPEN, LAYER_1, LAYER_2);
    }
}
