package games.twinhead.moreslabsstairsandwalls.block.soulsand;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/** Geometry-neutral Soul Sand bubble-column lifecycle. */
public final class SoulSandSemantics {
    public static final int BUBBLE_DELAY = 20;
    public static final double SURFACE_INSET_PIXELS = 2.0;

    private SoulSandSemantics() {}

    public static void schedule(Level level, BlockPos pos, Block block) {
        level.scheduleTick(pos, block, BUBBLE_DELAY);
    }

    public static void scheduleIfNeeded(BlockState state, ScheduledTickAccess ticks, BlockPos pos,
            Block block, Direction direction, BlockState neighbor) {
        if (direction == Direction.UP && neighbor.is(Blocks.WATER)
                || state.getFluidState().is(Fluids.WATER))
            ticks.scheduleTick(pos, block, BUBBLE_DELAY);
    }

    public static void updateBubbleColumn(BlockState state, ServerLevel level, BlockPos pos) {
        BubbleColumnBlock.updateColumn(Blocks.BUBBLE_COLUMN, level, pos.above(), state);
    }
}
