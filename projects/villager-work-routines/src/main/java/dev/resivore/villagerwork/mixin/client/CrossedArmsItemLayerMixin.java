package dev.resivore.villagerwork.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.resivore.villagerwork.client.VwrFishingRodPresentation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses only VWR's generic GROUND item so the isolated hand-posed rod is never duplicated. */
@Mixin(CrossedArmsItemLayer.class)
abstract class CrossedArmsItemLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HoldingEntityRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void villagerWork$replaceSyntheticRod(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                                  HoldingEntityRenderState state, float yRot, float xRot,
                                                  CallbackInfo ci) {
        if (state instanceof VwrFishingRodPresentation presentation
                && presentation.villagerWork$renderFishingRod()) ci.cancel();
    }
}
