package dev.resivore.naturalistxaeroicons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.resivore.naturalistxaeroicons.NaturalistIconPresentation;
import dev.resivore.naturalistxaeroicons.NaturalistModelContracts.Presentation;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.radar.icon.creator.render.form.model.part.RadarIconModelPartPrerenderer;

/** Scales only Brown Bear's already-successful native Xaero capture; no live model is changed. */
@Mixin(value = RadarIconModelPartPrerenderer.class, remap = false)
abstract class RadarIconModelPartPresentationMixin {
    @Inject(
            method = "renderPart",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            require = 1)
    private void naturalistXaeroIcons$pushNativePresentation(
            PoseStack pose, VertexConsumer consumer, ModelPart part, ModelPart mainPart,
            RadarIconModelPartPrerenderer.Parameters parameters, CallbackInfo callback) {
        Presentation presentation = NaturalistIconPresentation.currentNativePresentation();
        if (presentation == null) return;
        pose.pushPose();
        pose.scale(presentation.scale(), presentation.scale(), presentation.scale());
    }

    @Inject(
            method = "renderPart",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", shift = At.Shift.AFTER),
            require = 1)
    private void naturalistXaeroIcons$popNativePresentation(
            PoseStack pose, VertexConsumer consumer, ModelPart part, ModelPart mainPart,
            RadarIconModelPartPrerenderer.Parameters parameters, CallbackInfo callback) {
        if (NaturalistIconPresentation.currentNativePresentation() != null) pose.popPose();
    }
}
