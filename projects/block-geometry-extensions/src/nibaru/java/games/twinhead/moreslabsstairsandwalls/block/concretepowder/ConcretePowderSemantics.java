package games.twinhead.moreslabsstairsandwalls.block.concretepowder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Geometry-neutral vanilla-style concrete-powder water contact semantics. */
public final class ConcretePowderSemantics {
    private ConcretePowderSemantics() {}

    public static boolean hardensIn(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    public static boolean hardensOnAnySide(BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (Direction direction : Direction.values()) {
            BlockState current = level.getBlockState(cursor);
            if (direction == Direction.DOWN && !hardensIn(current)) continue;
            cursor.setWithOffset(pos, direction);
            BlockState neighbor = level.getBlockState(cursor);
            if (hardensIn(neighbor) && !neighbor.isFaceSturdy(level, pos, direction.getOpposite())) return true;
        }
        return false;
    }

    public static boolean shouldHarden(BlockGetter level, BlockPos pos, BlockState state) {
        return hardensIn(state) || hardensOnAnySide(level, pos);
    }

    public static BlockState hardenedState(BlockState powder, Block hardened) {
        return hardened.withPropertiesOf(powder);
    }
}
