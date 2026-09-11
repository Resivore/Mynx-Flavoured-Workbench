package dev.resivore.inventoryparticlesmatchacompat.mixin;

import dev.resivore.inventoryparticlesmatchacompat.ExclusiveParticleDispatch;
import java.util.List;
import net.lopymine.ip.element.mod.InventoryCursor;
import net.lopymine.ip.element.mod.InventoryParticle;
import net.lopymine.ip.element.mod.spawner.ParticleSpawner;
import net.lopymine.ip.element.mod.spawner.context.ParticleSpawnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The exact 2.6.0 renderer calls these three spawner methods for every slot, hover, cursor, and
 * GUI action context. The methods receive the real stack, so ordinary spawners are cancelled
 * before their predicate or cooldown can run for one of the six exact targets.
 */
@Mixin(value = ParticleSpawner.class, remap = false)
abstract class ParticleSpawnerExclusiveDispatchMixin {
    @Inject(
            method = "tickAndSpawn(Lnet/lopymine/ip/element/mod/spawner/context/ParticleSpawnContext;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void inventoryParticlesMatchaCompat$dispatchTick(
            ParticleSpawnContext context,
            CallbackInfoReturnable<List<InventoryParticle>> callbackInfo) {
        if (!ExclusiveParticleDispatch.permits((ParticleSpawner) (Object) this, context.getStack())) {
            callbackInfo.setReturnValue(List.of());
        }
    }

    @Inject(
            method = "spawn(Lnet/lopymine/ip/element/mod/spawner/context/ParticleSpawnContext;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void inventoryParticlesMatchaCompat$dispatchSpawn(
            ParticleSpawnContext context,
            CallbackInfoReturnable<List<InventoryParticle>> callbackInfo) {
        if (!ExclusiveParticleDispatch.permits((ParticleSpawner) (Object) this, context.getStack())) {
            callbackInfo.setReturnValue(List.of());
        }
    }

    @Inject(
            method = "spawnFromCursor(Lnet/lopymine/ip/element/mod/InventoryCursor;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void inventoryParticlesMatchaCompat$dispatchCursorTrail(
            InventoryCursor cursor,
            CallbackInfoReturnable<List<InventoryParticle>> callbackInfo) {
        if (!ExclusiveParticleDispatch.permits((ParticleSpawner) (Object) this, cursor.getCurrentStack())) {
            callbackInfo.setReturnValue(List.of());
        }
    }
}
