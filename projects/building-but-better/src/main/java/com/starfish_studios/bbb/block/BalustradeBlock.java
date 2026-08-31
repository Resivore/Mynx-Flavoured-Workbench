package com.starfish_studios.bbb.block;

import com.starfish_studios.bbb.block.properties.BBBBlockStateProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BalustradeBlock extends Block implements SimpleWaterloggedBlock, HammerableBlock {
    public static final BooleanProperty TOP = BBBBlockStateProperties.TOP;
    public static final BooleanProperty BOTTOM = BBBBlockStateProperties.BOTTOM;
    public static final BooleanProperty TILTED = BooleanProperty.create("tilted");
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final VoxelShape BOTTOM_AABB = Block.box(0, 0, 0, 16, 2, 16);
    public static final VoxelShape TOP_AABB = Block.box(0, 13, 0, 16, 16, 16);
    public static final VoxelShape PILLAR_AABB = Block.box(4, 0, 4, 12, 16, 12);
    public static final VoxelShape NORTH_TILTED_AABB = Shapes.or(
            Block.box(0, 12, 0, 16, 17, 8), Block.box(0, 15, 8, 16, 20, 16));
    public static final VoxelShape SOUTH_TILTED_AABB = Shapes.or(
            Block.box(0, 12, 8, 16, 17, 16), Block.box(0, 15, 0, 16, 20, 8));
    public static final VoxelShape EAST_TILTED_AABB = Shapes.or(
            Block.box(8, 10, 0, 16, 17, 16), Block.box(0, 15, 0, 8, 20, 16));
    public static final VoxelShape WEST_TILTED_AABB = Shapes.or(
            Block.box(0, 10, 0, 8, 17, 16), Block.box(8, 15, 0, 16, 20, 16));

    public BalustradeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TOP, true)
                .setValue(BOTTOM, true)
                .setValue(TILTED, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public InteractionResult onHammerUse(BlockState state, Level level, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        BlockState updated = state;
        if (player.isShiftKeyDown()) {
            updated = state.cycle(TILTED);
        } else {
            double y = hit.getLocation().y - pos.getY();
            if (y > 0.5D) {
                updated = state.cycle(TOP);
            } else if (y < 0.5D) {
                updated = state.cycle(BOTTOM);
            }
        }
        level.setBlockAndUpdate(pos, updated);
        level.playSound(player, pos, updated.getSoundType().getPlaceSound(),
                player.getSoundSource(), 1.0F, 1.0F);
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = PILLAR_AABB;
        if (state.getValue(TILTED) && state.getValue(TOP)) {
            shape = Shapes.or(shape, switch (state.getValue(FACING)) {
                case NORTH -> NORTH_TILTED_AABB;
                case SOUTH -> SOUTH_TILTED_AABB;
                case EAST -> EAST_TILTED_AABB;
                case WEST -> WEST_TILTED_AABB;
                default -> Shapes.empty();
            });
        } else if (state.getValue(TOP)) {
            shape = Shapes.or(shape, TOP_AABB);
        }
        return state.getValue(BOTTOM) ? Shapes.or(shape, BOTTOM_AABB) : shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP, BOTTOM, WATERLOGGED, FACING, TILTED);
    }
}
