package dev.resivore.croak.mixin;

import dev.resivore.croak.*;

import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.piglin.Piglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
abstract class DispatcherMixin {
    @Inject(method = "getRenderer(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/client/renderer/entity/EntityRenderer;", at = @At("HEAD"), cancellable = true)
    private void croak$adult(Entity entity, CallbackInfoReturnable<EntityRenderer<?, ?>> ci) {
        if (SoldierRenderer.current != null && entity.getType() == EntityTypes.PIGLIN
                && entity instanceof Piglin piglin && !piglin.isBaby())
            ci.setReturnValue(SoldierRenderer.current);
    }

    @Inject(method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;", at = @At("HEAD"), cancellable = true)
    private void croak$state(EntityRenderState state, CallbackInfoReturnable<EntityRenderer<?, ?>> ci) {
        if (state instanceof SoldierRenderer.State soldier)
            ci.setReturnValue(soldier.renderer);
    }
}
