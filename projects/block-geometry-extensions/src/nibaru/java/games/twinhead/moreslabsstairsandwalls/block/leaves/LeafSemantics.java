package games.twinhead.moreslabsstairsandwalls.block.leaves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Geometry-neutral leaf lifecycle semantics shared by Nibaru and compatible derived geometries.
 */
public final class LeafSemantics {
    private LeafSemantics() {
    }

    public static BlockState applyDefaultState(BlockState state) {
        return state.setValue(BlockStateProperties.DISTANCE, 7)
                .setValue(BlockStateProperties.PERSISTENT, false);
    }

    public static BlockState applyPlayerPlacementState(BlockState state) {
        return state.setValue(BlockStateProperties.PERSISTENT, true);
    }

    public static boolean shouldDecay(BlockState state) {
        return !state.getValue(BlockStateProperties.PERSISTENT)
                && state.getValue(BlockStateProperties.DISTANCE) == 7;
    }

    public static boolean isRandomlyTicking(BlockState state) {
        return shouldDecay(state);
    }

    public static void decayIfNeeded(BlockState state, ServerLevel level, BlockPos pos) {
        if (shouldDecay(state)) {
            Block.dropResources(state, level, pos);
            level.removeBlock(pos, false);
        }
    }

    public static BlockState updateDistanceFromLogs(BlockState state, LevelAccessor level, BlockPos pos) {
        int distance = 7;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            cursor.setWithOffset(pos, direction);
            distance = Math.min(distance, distanceFromNeighbor(level.getBlockState(cursor)) + 1);
            if (distance == 1) {
                break;
            }
        }
        return state.setValue(BlockStateProperties.DISTANCE, distance);
    }

    public static int distanceFromNeighbor(BlockState state) {
        if (state.is(BlockTags.LOGS)) {
            return 0;
        }
        return isLeafDistanceCarrier(state)
                ? state.getValue(BlockStateProperties.DISTANCE)
                : 7;
    }

    public static boolean isLeafDistanceCarrier(BlockState state) {
        return state.getBlock() instanceof LeavesBlock
                || state.getBlock() instanceof LeafDistanceCarrier;
    }

    public static void scheduleDistanceUpdate(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Block block, BlockState neighborState) {
        int distance = distanceFromNeighbor(neighborState) + 1;
        if (distance != 1 || state.getValue(BlockStateProperties.DISTANCE) != distance) {
            ticks.scheduleTick(pos, block, 1);
        }
    }

    public static void animateRainDrip(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.isRainingAt(pos.above()) && random.nextInt(15) == 1) {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (!belowState.canOcclude() || !belowState.isFaceSturdy(level, below, Direction.UP)) {
                level.addParticle(ParticleTypes.DRIPPING_WATER,
                        pos.getX() + random.nextDouble(), pos.getY() - 0.05, pos.getZ() + random.nextDouble(),
                        0.0, 0.0, 0.0);
            }
        }
    }
}
