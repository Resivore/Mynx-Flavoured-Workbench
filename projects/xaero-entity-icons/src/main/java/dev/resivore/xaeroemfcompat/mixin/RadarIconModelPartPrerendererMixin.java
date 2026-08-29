package dev.resivore.xaeroemfcompat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.resivore.xaeroemfcompat.EmfIconPartResolver;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.hud.minimap.radar.icon.creator.render.form.model.part.RadarIconModelPartPrerenderer;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelPartRenderTrace;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace;

/**
 * Lets Xaero's existing renderer obtain the real traced head color when the
 * selected part is this companion's temporary icon-only adapter.
 */
@Mixin(value = RadarIconModelPartPrerenderer.class, remap = false)
abstract class RadarIconModelPartPrerendererMixin {
    @Redirect(
            method = "renderPart",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/hud/minimap/radar/icon/creator/render/trace/"
                            + "ModelRenderTrace;getModelPartRenderInfo("
                            + "Lnet/minecraft/client/model/geom/ModelPart;)"
                            + "Lxaero/hud/minimap/radar/icon/creator/render/trace/"
                            + "ModelPartRenderTrace;"
            ),
            require = 1
    )
    private ModelPartRenderTrace xaeroEmfEntityIconCompat$resolveAdapterTrace(
            ModelRenderTrace trace,
            ModelPart part
    ) {
        ModelPartRenderTrace direct = trace.getModelPartRenderInfo(part);
        EmfIconPartResolver.AdapterMetadata metadata =
                EmfIconPartResolver.adapterMetadata(part);
        if (direct != null || metadata == null) {
            return direct;
        }
        return trace.getModelPartRenderInfo(metadata.tracedHead());
    }

    @Redirect(
            method = "renderPart",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;render("
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
                    remap = true
            ),
            require = 1
    )
    private void xaeroEmfEntityIconCompat$renderCanonicalFrameAdapter(
            ModelPart part,
            PoseStack pose,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            int color
    ) {
        if (!EmfIconPartResolver.renderAdapter(
                part, pose, vertexConsumer, packedLight, packedOverlay, color)) {
            part.render(pose, vertexConsumer, packedLight, packedOverlay, color);
        }
    }
}
