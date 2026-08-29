package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.StepBlock;
import games.twinhead.moreslabsstairsandwalls.block.dirt.PathSemantics;
import games.twinhead.moreslabsstairsandwalls.block.dirt.PathGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.function.Supplier;

/** CNM Step topology composed with Nibaru's lowered Dirt Path contract. */
final class PathStepBlock extends StepBlock implements PathGeometry {
    private final Supplier<Block> dirtGeometry;

    PathStepBlock(BlockBehaviour.Properties properties, Supplier<Block> dirtGeometry) {
        super(properties);
        this.dirtGeometry = dirtGeometry;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState placed = super.getStateForPlacement(context);
        if (placed == null) return null;
        return PathSemantics.placementState(placed, dirtGeometry.get().defaultBlockState(),
                context.getLevel(), context.getClickedPos());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        SlabType type = state.getValue(SLAB_TYPE);
        if (type == SlabType.DOUBLE) {
            return Shapes.or(createPathShape(facing, 7, 15), createPathShape(facing.getOpposite(), 0, 7));
        }
        return type == SlabType.TOP ? createPathShape(facing, 7, 15) : createPathShape(facing, 0, 7);
    }

    static VoxelShape createPathShape(Direction direction, double minY, double maxY) {
        return switch (direction) {
            case NORTH -> Block.box(0, minY, 0, 16, maxY, 8);
            case EAST -> Block.box(8, minY, 0, 16, maxY, 16);
            case SOUTH -> Block.box(0, minY, 8, 16, maxY, 16);
            case WEST -> Block.box(0, minY, 0, 8, maxY, 16);
            default -> Shapes.empty();
        };
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        PathSemantics.scheduleConversionIfNeeded(state, level, ticks, pos, direction);
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        PathSemantics.revertIfObstructed(level, pos, state, dirtGeometry.get().defaultBlockState());
    }

    @Override
    public boolean pathSurfaceRequiresClearAbove(BlockState state) {
        return state.getValue(SLAB_TYPE) != SlabType.BOTTOM;
    }
}
