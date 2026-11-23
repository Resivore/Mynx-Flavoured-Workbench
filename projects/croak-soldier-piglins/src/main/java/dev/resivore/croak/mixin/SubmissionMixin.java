package dev.resivore.croak.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import dev.resivore.croak.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.EntityTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
abstract class SubmissionMixin {
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void croak$submit(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector nodes,
                              CameraRenderState camera, CallbackInfo ci) {
        if (!(this instanceof SoldierBinding.Owner owner)) return;
        if (state instanceof SoldierRenderer.State soldier) {
            try {
                owner.croak$binding().submitting();
                soldier.renderer.submit(soldier, pose, nodes, camera);
                ci.cancel();
            } catch (RuntimeException | LinkageError e) {
                LogUtils.getLogger().error("[Croak Soldier] Soldier state submission failed", e);
                throw e;
            }
        } else if (state.entityType == EntityTypes.PIGLIN && !state.isBaby) {
            owner.croak$binding().vanillaState();
        }
    }
}
