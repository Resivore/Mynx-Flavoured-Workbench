package games.twinhead.moreslabsstairsandwalls.block.magma;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Geometry-neutral native Magma contact and downward bubble-column contract. */
public final class MagmaSemantics {
    public static final int BUBBLE_DELAY = 20;
    public static final float HOT_FLOOR_DAMAGE = 1.0F;

    private MagmaSemantics() {}

    public static boolean shouldDamage(Entity entity) {
        return entity instanceof LivingEntity && !entity.isSteppingCarefully();
    }

    public static void hurtIfNeeded(Level level, Entity entity) {
        if (shouldDamage(entity)) entity.hurt(level.damageSources().hotFloor(), HOT_FLOOR_DAMAGE);
    }

    public static void schedule(Level level, BlockPos pos, Block block) {
        level.scheduleTick(pos, block, BUBBLE_DELAY);
    }

    public static void scheduleForWaterAbove(ScheduledTickAccess ticks, BlockPos pos, Block block,
            Direction direction, BlockState neighborState) {
        if (direction == Direction.UP && neighborState.is(Blocks.WATER))
            ticks.scheduleTick(pos, block, BUBBLE_DELAY);
    }

    public static void updateBubbleColumn(ServerLevel level, BlockPos pos, BlockState magmaState) {
        BubbleColumnBlock.updateColumn(Blocks.BUBBLE_COLUMN, level, pos.above(), magmaState);
    }
}
