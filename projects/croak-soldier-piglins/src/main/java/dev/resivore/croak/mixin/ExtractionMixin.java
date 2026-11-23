package dev.resivore.croak.mixin;

import dev.resivore.croak.SoldierBinding;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.piglin.Piglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
abstract class ExtractionMixin {
    @Unique private boolean croak$reportedUnbound;

    @Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("HEAD"), cancellable = true)
    private void croak$extract(Entity entity, float tick, CallbackInfoReturnable<EntityRenderState> ci) {
        if (entity.getType() != EntityTypes.PIGLIN ||
                !(entity instanceof Piglin piglin) || piglin.isBaby()) return;
        if (!(this instanceof SoldierBinding.Owner owner)) {
            if (!croak$reportedUnbound) {
                croak$reportedUnbound = true;
                LogUtils.getLogger().warn("[Croak Soldier] Adult dispatch reached renderer without binding: {}", getClass().getName());
            }
            return;
        }
        var binding = owner.croak$binding();
        var visual = binding.visual();
        if (visual == null) return;
        try {
            ci.setReturnValue(visual.createRenderState(piglin, tick));
            binding.extracted();
        } catch (RuntimeException | LinkageError e) {
            LogUtils.getLogger().error("[Croak Soldier] Adult dispatch failed during state extraction", e);
            throw e;
        }
    }
}
