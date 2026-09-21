package dev.aero.cnmterraincompat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Thin wooden post-and-arm wall topology used by plank and Beam families. */
public final class WoodenPlankWallBlock extends WallBlock {
    private static final EnumProperty<WallSide> EAST = BlockStateProperties.EAST_WALL;
    private static final EnumProperty<WallSide> NORTH = BlockStateProperties.NORTH_WALL;
    private static final EnumProperty<WallSide> SOUTH = BlockStateProperties.SOUTH_WALL;
    private static final EnumProperty<WallSide> WEST = BlockStateProperties.WEST_WALL;
    private static final VoxelShape POST = Block.box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape NORTH_ARM = Block.box(4, 0, 0, 12, 16, 4);
    private static final VoxelShape EAST_ARM = Block.box(12, 0, 4, 16, 16, 12);
    private static final VoxelShape SOUTH_ARM = Block.box(4, 0, 12, 12, 16, 16);
    private static final VoxelShape WEST_ARM = Block.box(0, 0, 4, 4, 16, 12);
    private static final VoxelShape COLLISION_POST = Block.box(4, 0, 4, 12, 24, 12);
    private static final VoxelShape COLLISION_NORTH = Block.box(4, 0, 0, 12, 24, 4);
    private static final VoxelShape COLLISION_EAST = Block.box(12, 0, 4, 16, 24, 12);
    private static final VoxelShape COLLISION_SOUTH = Block.box(4, 0, 12, 12, 24, 16);
    private static final VoxelShape COLLISION_WEST = Block.box(0, 0, 4, 4, 24, 12);

    public WoodenPlankWallBlock(Properties properties) { super(properties); }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        VoxelShape result = initial(state, level, pos, POST);
        if (state.getValue(NORTH) != WallSide.NONE) result = Shapes.or(result, NORTH_ARM);
        if (state.getValue(EAST) != WallSide.NONE) result = Shapes.or(result, EAST_ARM);
        if (state.getValue(SOUTH) != WallSide.NONE) result = Shapes.or(result, SOUTH_ARM);
        if (state.getValue(WEST) != WallSide.NONE) result = Shapes.or(result, WEST_ARM);
        return result;
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        VoxelShape result = initial(state, level, pos, COLLISION_POST);
        if (state.getValue(NORTH) != WallSide.NONE) result = Shapes.or(result, COLLISION_NORTH);
        if (state.getValue(EAST) != WallSide.NONE) result = Shapes.or(result, COLLISION_EAST);
        if (state.getValue(SOUTH) != WallSide.NONE) result = Shapes.or(result, COLLISION_SOUTH);
        if (state.getValue(WEST) != WallSide.NONE) result = Shapes.or(result, COLLISION_WEST);
        return result;
    }

    private static VoxelShape initial(BlockState state, BlockGetter level, BlockPos pos, VoxelShape post) {
        boolean northSouth = state.getValue(NORTH) != WallSide.NONE && state.getValue(SOUTH) != WallSide.NONE;
        boolean eastWest = state.getValue(EAST) != WallSide.NONE && state.getValue(WEST) != WallSide.NONE;
        if (northSouth && eastWest) return Shapes.empty();
        if (!state.getValue(UP)) return post;
        BlockState above = level.getBlockState(pos.above());
        return above.getBlock() instanceof WallBlock && above.getValue(WallBlock.UP) ? post
                : (northSouth || eastWest ? Shapes.empty() : post);
    }
}
