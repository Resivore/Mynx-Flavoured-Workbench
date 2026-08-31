package games.twinhead.moreslabsstairsandwalls.block.coral;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Shared delayed wet/dry lifecycle for every Nibaru coral geometry. */
public final class CoralSemantics {
    private CoralSemantics() {}

    public static boolean hasAdjacentWater(BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.values())
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) return true;
        return false;
    }

    public static boolean survives(BlockState state, BlockGetter level, BlockPos pos) {
        return hasAdjacentWater(level, pos) || state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED);
    }

    public static void scheduleIfDry(LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Block block, RandomSource random) {
        if (!hasAdjacentWater(level, pos)) ticks.scheduleTick(pos, block, 60 + random.nextInt(40));
    }

    public static BlockState deadState(BlockState live, Block dead) {
        return dead.withPropertiesOf(live);
    }
}
