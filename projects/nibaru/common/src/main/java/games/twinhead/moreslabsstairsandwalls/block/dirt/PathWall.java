package games.twinhead.moreslabsstairsandwalls.block.dirt;

import games.twinhead.moreslabsstairsandwalls.block.ModBlocks;
import games.twinhead.moreslabsstairsandwalls.block.base.BaseWall;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("deprecation")
public class PathWall extends BaseWall implements PathGeometry {

    public PathWall(ModBlocks modBlocks, Properties settings) {
        super(modBlocks,settings);
    }

    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState placed = super.getStateForPlacement(ctx);
        return PathSemantics.placementState(placed,
                ModBlocks.DIRT.getBlock(ModBlocks.BlockType.WALL).getStateForPlacement(ctx), ctx.getLevel(), ctx.getClickedPos());
    }

    public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        PathSemantics.scheduleConversionIfNeeded(state, world, tickAccess, pos, direction);

        return super.updateShape(state, world, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        PathSemantics.revertIfObstructed(world, pos, state, ModBlocks.DIRT.getBlock(ModBlocks.BlockType.WALL).defaultBlockState());
    }


    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return PathSemantics.canSurvive(state, world, pos);
    }

    @Override
    public boolean pathSurfaceRequiresClearAbove(BlockState state) { return true; }


}
