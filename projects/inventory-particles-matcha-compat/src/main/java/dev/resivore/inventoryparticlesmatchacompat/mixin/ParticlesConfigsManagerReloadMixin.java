package dev.resivore.inventoryparticlesmatchacompat.mixin;

import dev.resivore.inventoryparticlesmatchacompat.ExclusiveParticleDispatch;
import net.lopymine.ip.resourcepack.manager.ParticlesConfigsManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A reload always replaces spawner instances, so discard the prior identity map first. */
@Mixin(value = ParticlesConfigsManager.class, remap = false)
abstract class ParticlesConfigsManagerReloadMixin {
    @Inject(method = "reload()V", at = @At("HEAD"), require = 0, remap = false)
    private void inventoryParticlesMatchaCompat$resetCompatSpawners(CallbackInfo callbackInfo) {
        ExclusiveParticleDispatch.reset();
    }
}
