package dev.resivore.naturalistxaeroicons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.resivore.naturalistxaeroicons.BrownBearPathDiagnostic;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.radar.icon.creator.render.form.model.part.RadarIconModelPartPrerenderer;

/** Distinguishes the direct model-part raster path from the disproven renderModel path. */
@Mixin(value = RadarIconModelPartPrerenderer.class, remap = false)
abstract class RadarIconModelPartPrerendererMixin {
    @Inject(method = "renderPart", at = @At("HEAD"), require = 1)
    private void naturalistXaeroIcons$observeBrownBearRenderPart(
            PoseStack pose, VertexConsumer consumer, ModelPart part, ModelPart center,
            RadarIconModelPartPrerenderer.Parameters parameters, CallbackInfo callback) {
        BrownBearPathDiagnostic.modelPartPath("RadarIconModelPartPrerenderer#renderPart");
    }

    @Inject(method = "renderPartsIterable", at = @At("HEAD"), require = 1)
    private void naturalistXaeroIcons$observeBrownBearRenderPartsIterable(
            Iterable<ModelPart> parts, PoseStack pose, VertexConsumer consumer, ModelPart center,
            RadarIconModelPartPrerenderer.Parameters parameters, CallbackInfoReturnable<ModelPart> callback) {
        BrownBearPathDiagnostic.modelPartPath("RadarIconModelPartPrerenderer#renderPartsIterable");
    }
}
