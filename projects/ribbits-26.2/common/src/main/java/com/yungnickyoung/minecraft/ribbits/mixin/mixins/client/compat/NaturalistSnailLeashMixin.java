package com.yungnickyoung.minecraft.ribbits.mixin.mixins.client.compat;

import com.yungnickyoung.minecraft.ribbits.entity.WanderingRibbitEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * Client-only, conditional adjustment for the two scheduler-created Naturalist
 * snails. Snail inherits Leashable's default method rather than declaring one,
 * so this mixin contributes the override itself; an injection would have no
 * target method to modify. The holder remains the Wandering Ribbit.
 */
@Pseudo
@Mixin(targets = "com.crispytwig.naturalist.server.entity.mob.Snail")
public abstract class NaturalistSnailLeashMixin {
    public Vec3 getLeashOffset() {
        Entity snail = (Entity) (Object) this;
        if (!(snail instanceof Leashable leashable)
                || !(leashable.getLeashHolder() instanceof WanderingRibbitEntity)) {
            return new Vec3(0.0D, snail.getEyeHeight(), snail.getBbWidth() * 0.4D);
        }
        return new Vec3(0.0D, snail.getBbHeight() * 0.48D, 0.0D);
    }
}
