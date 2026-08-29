package com.crispytwig.naturalist.server.entity.ai.goal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.jetbrains.annotations.NotNull;

public class CloseMeleeAttackGoal extends MeleeAttackGoal {
    public CloseMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
    }

    @Override
    protected void checkAndPerformAttack(@NotNull LivingEntity target) {
        if (this.mob.distanceToSqr(target) <= Mth.square(this.mob.getBbWidth() * 1.2f) && this.isTimeToAttack()) {
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget((ServerLevel) this.mob.level(), target);
        }
    }
}
