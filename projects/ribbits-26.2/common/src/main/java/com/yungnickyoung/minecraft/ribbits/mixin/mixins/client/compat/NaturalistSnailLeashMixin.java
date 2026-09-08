package com.yungnickyoung.minecraft.ribbits.mixin.mixins.client.compat;

import com.yungnickyoung.minecraft.ribbits.entity.WanderingRibbitEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-only, conditional adjustment for the two scheduler-created Naturalist
 * snails. The holder remains the Wandering Ribbit; this changes only the render
 * origin sampled by Minecraft's 26.2 leash renderer.
 */
@Pseudo
@Mixin(targets = "com.crispytwig.naturalist.server.entity.mob.Snail")
public abstract class NaturalistSnailLeashMixin {
    @Inject(method = "getLeashOffset", at = @At("HEAD"), cancellable = true, require = 0)
    private void ribbits$lowerLeashOrigin(CallbackInfoReturnable<Vec3> callback) {
        Entity snail = (Entity) (Object) this;
        if (!(snail instanceof Leashable leashable)
                || !(leashable.getLeashHolder() instanceof WanderingRibbitEntity)) return;
        callback.setReturnValue(new Vec3(0.0D, snail.getBbHeight() * 0.28D, 0.0D));
    }
}
