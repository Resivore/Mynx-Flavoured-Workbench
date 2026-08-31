package games.twinhead.moreslabsstairsandwalls.block.falling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Geometry-neutral falling-material rules shared by native and derived shapes. */
public final class FallingSemantics {
    public static final int FALL_DELAY = 2;
    private FallingSemantics() {}

    public static boolean canFallThrough(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE) || state.liquid() || state.canBeReplaced();
    }

    public static void schedule(ScheduledTickAccess ticks, BlockPos pos, Block block) {
        ticks.scheduleTick(pos, block, FALL_DELAY);
    }

    public static boolean shouldFall(Level level, BlockPos pos) {
        return pos.getY() >= level.getMinY() && canFallThrough(level.getBlockState(pos.below()));
    }

    public static void animateDust(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(16) != 0 || !canFallThrough(level.getBlockState(pos.below()))) return;
        level.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, state),
                pos.getX() + random.nextDouble(), pos.getY() - 0.05, pos.getZ() + random.nextDouble(), 0, 0, 0);
    }
}
