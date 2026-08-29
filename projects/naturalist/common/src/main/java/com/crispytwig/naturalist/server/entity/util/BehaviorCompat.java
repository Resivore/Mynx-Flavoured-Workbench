package com.crispytwig.naturalist.server.entity.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.phys.Vec3;

public final class BehaviorCompat {
    private BehaviorCompat() {
    }

    /**
     * Non-mutating equivalent of the pre-26.2 directional shield-block query.
     * Mirrors the eligibility and angle checks used by LivingEntity.applyItemBlocking
     * without damaging the blocking item or emitting block effects.
     */
    public static boolean wouldBlockDamage(LivingEntity target, DamageSource source, float incomingDamage) {
        ItemStack blockingStack = target.getItemBlockingWith();
        if (blockingStack == null) {
            return false;
        }

        BlocksAttacks blocksAttacks = blockingStack.get(DataComponents.BLOCKS_ATTACKS);
        if (blocksAttacks == null || blocksAttacks.bypassedBy()
                .map(types -> types.contains(source.typeHolder()))
                .orElse(false)) {
            return false;
        }

        Entity direct = source.getDirectEntity();
        if (direct instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) {
            return false;
        }

        Vec3 sourcePosition = source.getSourcePosition();
        double angle = Math.PI;
        if (sourcePosition != null) {
            Vec3 view = target.calculateViewVector(0.0F, target.getYHeadRot());
            Vec3 towardSource = sourcePosition.subtract(target.position());
            towardSource = new Vec3(towardSource.x, 0.0D, towardSource.z).normalize();
            angle = Math.acos(towardSource.dot(view));
        }

        return blocksAttacks.resolveBlockedDamage(source, incomingDamage, angle) > 0.0F;
    }

    /**
     * Client-safe equivalent of the shared, level-independent portion of
     * TargetingConditions.forNonCombat().range(range).
     */
    public static boolean matchesNonCombatRange(LivingEntity observer, LivingEntity target, double range) {
        if (observer == target || !target.canBeSeenByAnyone()) {
            return false;
        }
        double visibleRange = Math.max(range * target.getVisibilityPercent(observer), 2.0D);
        return observer.distanceToSqr(target.getX(), target.getY(), target.getZ()) <= visibleRange * visibleRange
                && (!(observer instanceof Mob mob) || mob.getSensing().hasLineOfSight(target));
    }
}
