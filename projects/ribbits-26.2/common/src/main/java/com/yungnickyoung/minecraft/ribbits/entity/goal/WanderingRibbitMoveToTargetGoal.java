package com.yungnickyoung.minecraft.ribbits.entity.goal;

import com.yungnickyoung.minecraft.ribbits.entity.WanderingRibbitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Moves a scheduled merchant toward the player-area target captured by its spawn attempt. */
public final class WanderingRibbitMoveToTargetGoal extends Goal {
    private static final double ARRIVAL_DISTANCE_SQUARED = 16.0D;

    private final WanderingRibbitEntity ribbit;
    private final double speedModifier;

    public WanderingRibbitMoveToTargetGoal(WanderingRibbitEntity ribbit, double speedModifier) {
        this.ribbit = ribbit;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        BlockPos target = ribbit.getWanderTarget();
        return target != null
                && !ribbit.isTrading()
                && distanceToTargetSquared(target) > ARRIVAL_DISTANCE_SQUARED;
    }

    @Override
    public boolean canContinueToUse() {
        BlockPos target = ribbit.getWanderTarget();
        return target != null
                && !ribbit.isTrading()
                && !ribbit.getNavigation().isDone()
                && distanceToTargetSquared(target) > ARRIVAL_DISTANCE_SQUARED;
    }

    @Override
    public void start() {
        moveToTarget();
    }

    @Override
    public void tick() {
        if (ribbit.getNavigation().isDone()) {
            moveToTarget();
        }
    }

    private void moveToTarget() {
        BlockPos target = ribbit.getWanderTarget();
        if (target != null) {
            ribbit.getNavigation().moveTo(
                    target.getX() + 0.5D,
                    target.getY(),
                    target.getZ() + 0.5D,
                    speedModifier);
        }
    }

    private double distanceToTargetSquared(BlockPos target) {
        double x = target.getX() + 0.5D - ribbit.getX();
        double y = target.getY() - ribbit.getY();
        double z = target.getZ() + 0.5D - ribbit.getZ();
        return x * x + y * y + z * z;
    }
}
