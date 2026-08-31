package games.twinhead.moreslabsstairsandwalls.block.dirt;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.base.BaseStairs;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("deprecation")
public class PathStairs extends BaseStairs implements PathGeometry {
    protected static final VoxelShape TOP_SHAPE;
    protected static final VoxelShape BOTTOM_SHAPE;
    protected static final VoxelShape BOTTOM_NORTH_WEST_CORNER_SHAPE = Block.box(0.0, 0.0, 0.0, 8.0, 7.0, 8.0);
    protected static final VoxelShape BOTTOM_SOUTH_WEST_CORNER_SHAPE = Block.box(0.0, 0.0, 8.0, 8.0, 7.0, 16.0);
    protected static final VoxelShape TOP_NORTH_WEST_CORNER_SHAPE = Block.box(0.0, 7.0, 0.0, 8.0, 15.0, 8.0);
    protected static final VoxelShape TOP_SOUTH_WEST_CORNER_SHAPE = Block.box(0.0, 7.0, 8.0, 8.0, 15.0, 16.0);
    protected static final VoxelShape BOTTOM_NORTH_EAST_CORNER_SHAPE = Block.box(8.0, 0.0, 0.0, 16.0, 7.0, 8.0);
    protected static final VoxelShape BOTTOM_SOUTH_EAST_CORNER_SHAPE = Block.box(8.0, 0.0, 8.0, 16.0, 7.0, 16.0);
    protected static final VoxelShape TOP_NORTH_EAST_CORNER_SHAPE = Block.box(8.0, 7.0, 0.0, 16.0, 15.0, 8.0);
    protected static final VoxelShape TOP_SOUTH_EAST_CORNER_SHAPE = Block.box(8.0, 7.0, 8.0, 16.0, 15.0, 16.0);
    protected static final VoxelShape[] TOP_SHAPES;
    protected static final VoxelShape[] BOTTOM_SHAPES;
    private static final int[] SHAPE_INDICES = new int[]{12, 5, 3, 10, 14, 13, 7, 11, 13, 7, 11, 14, 8, 4, 1, 2, 4, 1, 2, 8};

    public PathStairs(ModBlocks modBlocks, BlockState defaultState, Properties settings) {
        super(modBlocks, defaultState, settings);
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction direction = ctx.getClickedFace();
        BlockPos blockPos = ctx.getClickedPos();
        FluidState fluidState = ctx.getLevel().getFluidState(blockPos);
        BlockState blockState = this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection()).setValue(HALF, direction == Direction.DOWN || direction != Direction.UP && ctx.getClickLocation().y - (double)blockPos.getY() > 0.5 ? Half.TOP : Half.BOTTOM).setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
        BlockState placed = blockState.setValue(SHAPE, getStairsShape(blockState, ctx.getLevel(), blockPos));
        return PathSemantics.placementState(placed,
                ModBlocks.DIRT.getBlock(ModBlocks.BlockType.STAIRS).getStateForPlacement(ctx), ctx.getLevel(), blockPos);
    }


    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return (state.getValue(HALF) == Half.TOP ? TOP_SHAPES : BOTTOM_SHAPES)[SHAPE_INDICES[state.getValue(SHAPE).ordinal() * 4 + state.getValue(FACING).get2DDataValue()]];
    }

    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        PathSemantics.scheduleConversionIfNeeded(state, world, tickAccess, pos, direction);

        return super.updateShape(state, world, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        PathSemantics.revertIfObstructed(world, pos, state, ModBlocks.DIRT.getBlock(ModBlocks.BlockType.STAIRS).defaultBlockState());
    }

    private static StairsShape getStairsShape(BlockState state, BlockGetter world, BlockPos pos) {
        Direction direction3;
        Direction direction2;
        Direction direction = state.getValue(FACING);
        BlockState blockState = world.getBlockState(pos.relative(direction));
        if (StairBlock.isStairs(blockState) && state.getValue(HALF) == blockState.getValue(HALF) && (direction2 = blockState.getValue(FACING)).getAxis() != state.getValue(FACING).getAxis() && canTakeShape(state, world, pos, direction2.getOpposite())) {
            if (direction2 == direction.getCounterClockWise()) {
                return StairsShape.OUTER_LEFT;
            }
            return StairsShape.OUTER_RIGHT;
        }
        BlockState blockState2 = world.getBlockState(pos.relative(direction.getOpposite()));
        if (StairBlock.isStairs(blockState2) && state.getValue(HALF) == blockState2.getValue(HALF) && (direction3 = blockState2.getValue(FACING)).getAxis() != state.getValue(FACING).getAxis() && canTakeShape(state, world, pos, direction3)) {
            if (direction3 == direction.getCounterClockWise()) {
                return StairsShape.INNER_LEFT;
            }
            return StairsShape.INNER_RIGHT;
        }
        return StairsShape.STRAIGHT;
    }

    private static boolean canTakeShape(BlockState state, BlockGetter world, BlockPos pos, Direction dir) {
        BlockState blockState = world.getBlockState(pos.relative(dir));
        return !StairBlock.isStairs(blockState) || blockState.getValue(FACING) != state.getValue(FACING) || blockState.getValue(HALF) != state.getValue(HALF);
    }

    private static VoxelShape[] makePathShapes(VoxelShape base, VoxelShape northWest, VoxelShape northEast,
                                           VoxelShape southWest, VoxelShape southEast) {
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

    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return PathSemantics.canSurvive(state, world, pos);
    }

    @Override
    public boolean pathSurfaceRequiresClearAbove(BlockState state) {
        return true;
    }

    static {
        BOTTOM_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 7.0, 16.0);
        TOP_SHAPE = Block.box(0.0, 7.0, 0.0, 16.0, 15.0, 16.0);
        // As with vanilla stairs, the base half is combined with corners in the opposite vertical half.
        // Combining same-half boxes merely reproduces a flat slab and breaks outline/raycast placement.
        TOP_SHAPES = makePathShapes(TOP_SHAPE, BOTTOM_NORTH_WEST_CORNER_SHAPE, BOTTOM_NORTH_EAST_CORNER_SHAPE,
                BOTTOM_SOUTH_WEST_CORNER_SHAPE, BOTTOM_SOUTH_EAST_CORNER_SHAPE);
        BOTTOM_SHAPES = makePathShapes(BOTTOM_SHAPE, TOP_NORTH_WEST_CORNER_SHAPE, TOP_NORTH_EAST_CORNER_SHAPE,
                TOP_SOUTH_WEST_CORNER_SHAPE, TOP_SOUTH_EAST_CORNER_SHAPE);
    }




}
