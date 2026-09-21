package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.StepBlock;
import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shape-only forms of Nibaru's lowered Dirt Path geometry.
 *
 * <p>External path materials need the 15/16 surface and its occlusion contract, but must not
 * claim Dirt Path's obstruction/reversion lifecycle.  These carriers therefore mirror only the
 * authored geometry used by the real Path blocks.</p>
 */
final class LoweredPathStairsBlock extends StairBlock {
    private static final VoxelShape TOP_SHAPE = Block.box(0, 7, 0, 16, 15, 16);
    private static final VoxelShape BOTTOM_SHAPE = Block.box(0, 0, 0, 16, 7, 16);
    private static final VoxelShape BOTTOM_NORTH_WEST = Block.box(0, 0, 0, 8, 7, 8);
    private static final VoxelShape BOTTOM_SOUTH_WEST = Block.box(0, 0, 8, 8, 7, 16);
    private static final VoxelShape TOP_NORTH_WEST = Block.box(0, 7, 0, 8, 15, 8);
    private static final VoxelShape TOP_SOUTH_WEST = Block.box(0, 7, 8, 8, 15, 16);
    private static final VoxelShape BOTTOM_NORTH_EAST = Block.box(8, 0, 0, 16, 7, 8);
    private static final VoxelShape BOTTOM_SOUTH_EAST = Block.box(8, 0, 8, 16, 7, 16);
    private static final VoxelShape TOP_NORTH_EAST = Block.box(8, 7, 0, 16, 15, 8);
    private static final VoxelShape TOP_SOUTH_EAST = Block.box(8, 7, 8, 16, 15, 16);
    private static final int[] SHAPE_INDICES = {
            12, 5, 3, 10, 14, 13, 7, 11, 13, 7, 11, 14, 8, 4, 1, 2, 4, 1, 2, 8
    };
    private static final VoxelShape[] TOP_SHAPES = makeShapes(TOP_SHAPE,
            BOTTOM_NORTH_WEST, BOTTOM_NORTH_EAST, BOTTOM_SOUTH_WEST, BOTTOM_SOUTH_EAST);
    private static final VoxelShape[] BOTTOM_SHAPES = makeShapes(BOTTOM_SHAPE,
            TOP_NORTH_WEST, TOP_NORTH_EAST, TOP_SOUTH_WEST, TOP_SOUTH_EAST);

    LoweredPathStairsBlock(BlockState baseState, Properties properties) {
        super(baseState, properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return loweredShape(state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return loweredShape(state);
    }

    private static VoxelShape loweredShape(BlockState state) {
        VoxelShape[] shapes = state.getValue(HALF) == Half.TOP ? TOP_SHAPES : BOTTOM_SHAPES;
        int index = state.getValue(SHAPE).ordinal() * 4 + state.getValue(FACING).get2DDataValue();
        return shapes[SHAPE_INDICES[index]];
    }

    private static VoxelShape[] makeShapes(VoxelShape base, VoxelShape northWest,
            VoxelShape northEast, VoxelShape southWest, VoxelShape southEast) {
        VoxelShape[] shapes = new VoxelShape[16];
        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = base;
            if ((mask & 1) != 0) shape = Shapes.or(shape, northWest);
            if ((mask & 2) != 0) shape = Shapes.or(shape, northEast);
            if ((mask & 4) != 0) shape = Shapes.or(shape, southWest);
            if ((mask & 8) != 0) shape = Shapes.or(shape, southEast);
            shapes[mask] = shape;
        }
        return shapes;
    }
}

final class LoweredPathVerticalSlabBlock extends VerticalSlabBlock {
    LoweredPathVerticalSlabBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return loweredShape(state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return loweredShape(state);
    }

    private static VoxelShape loweredShape(BlockState state) {
        if (state.getValue(DOUBLE)) return Block.box(0, 0, 0, 16, 15, 16);
        return switch (state.getValue(FACING)) {
            case NORTH -> Block.box(0, 0, 0, 16, 15, 8);
            case EAST -> Block.box(8, 0, 0, 16, 15, 16);
            case SOUTH -> Block.box(0, 0, 8, 16, 15, 16);
            case WEST -> Block.box(0, 0, 0, 8, 15, 16);
            default -> Shapes.empty();
        };
    }
}

final class LoweredPathStepBlock extends StepBlock {
    LoweredPathStepBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return loweredShape(state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return loweredShape(state);
    }

    private static VoxelShape loweredShape(BlockState state) {
        Direction facing = state.getValue(FACING);
        SlabType type = state.getValue(SLAB_TYPE);
        if (type == SlabType.DOUBLE) {
            return Shapes.or(shape(facing, 7, 15), shape(facing.getOpposite(), 0, 7));
        }
        return type == SlabType.TOP ? shape(facing, 7, 15) : shape(facing, 0, 7);
    }

    private static VoxelShape shape(Direction direction, double minY, double maxY) {
        return switch (direction) {
            case NORTH -> Block.box(0, minY, 0, 16, maxY, 8);
            case EAST -> Block.box(8, minY, 0, 16, maxY, 16);
            case SOUTH -> Block.box(0, minY, 8, 16, maxY, 16);
            case WEST -> Block.box(0, minY, 0, 8, maxY, 16);
            default -> Shapes.empty();
        };
    }
}
