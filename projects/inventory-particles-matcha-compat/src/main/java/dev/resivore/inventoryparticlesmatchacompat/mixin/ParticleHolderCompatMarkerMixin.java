package dev.resivore.inventoryparticlesmatchacompat.mixin;

import dev.resivore.inventoryparticlesmatchacompat.ExclusiveParticleDispatch;
import java.util.function.Function;
import net.lopymine.ip.config.particle.ParticleHolder;
import net.lopymine.ip.element.mod.InventoryParticle;
import net.lopymine.ip.element.mod.spawner.ParticleSpawner;
import net.lopymine.ip.element.mod.spawner.context.ParticleSpawnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures only the uniquely named C4 data-defined spawners as IP links them. */
@Mixin(value = ParticleHolder.class, remap = false)
abstract class ParticleHolderCompatMarkerMixin {
    @Shadow
    public abstract String getName();

    @Inject(method = "createSpawner", at = @At("RETURN"), require = 0, remap = false)
    private void inventoryParticlesMatchaCompat$markCompatSpawner(
            Function<ParticleSpawnContext, InventoryParticle> particleFactory,
            CallbackInfoReturnable<ParticleSpawner> callbackInfo) {
        ExclusiveParticleDispatch.register(callbackInfo.getReturnValue(), getName());
    }
}
