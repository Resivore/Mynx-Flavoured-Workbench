package dev.aero.cnmterraincompat;

import dev.tazer.clutternomore.common.blocks.VerticalSlabBlock;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.function.Supplier;

/** CNM half-width geometry composed with Nibaru Dirt Path lifecycle semantics. */
final class PathVerticalSlabBlock extends VerticalSlabBlock implements PathGeometry {
    private final Supplier<Block> dirtGeometry;

    PathVerticalSlabBlock(BlockBehaviour.Properties properties, Supplier<Block> dirtGeometry) {
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
        if (state.getValue(DOUBLE)) return Block.box(0, 0, 0, 16, 15, 16);
        return switch (state.getValue(FACING)) {
            case NORTH -> Block.box(0, 0, 0, 16, 15, 8);
            case EAST -> Block.box(8, 0, 0, 16, 15, 16);
            case SOUTH -> Block.box(0, 0, 8, 16, 15, 16);
            case WEST -> Block.box(0, 0, 0, 8, 15, 16);
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
    public boolean pathSurfaceRequiresClearAbove(BlockState state) { return true; }
}
