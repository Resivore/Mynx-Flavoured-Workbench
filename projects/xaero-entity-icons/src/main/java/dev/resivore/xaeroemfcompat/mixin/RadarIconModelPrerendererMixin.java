package dev.resivore.xaeroemfcompat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.resivore.xaeroemfcompat.EmfIconPartResolver;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.radar.icon.creator.render.form.model.RadarIconModelPrerenderer;
import xaero.lib.client.graphics.XaeroBufferProvider;

import java.util.Optional;

/**
 * Repairs only a failed upstream selection by rendering the traced EMF head
 * through an icon-only canonical-frame adapter.
 */
@Mixin(value = RadarIconModelPrerenderer.class, remap = false)
abstract class RadarIconModelPrerendererMixin {
    @Inject(
            method = "renderModel",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private void xaeroEmfEntityIconCompat$renderRelocatedHead(
            PoseStack matrixStack,
            XaeroBufferProvider bufferSource,
            EntityRenderState entityRenderState,
            Model model,
            Entity entity,
            ModelPart incomingMainPart,
            RadarIconModelPrerenderer.Parameters parameters,
            CallbackInfoReturnable<ModelPart> callbackInfo
    ) {
        if (!parameters.renderedDest.isEmpty()) {
            return;
        }

        Optional<EmfIconPartResolver.Resolution> resolved =
                EmfIconPartResolver.resolve(
                        model.root(),
                        callbackInfo.getReturnValue(),
                        parameters.mrt,
                        parameters.config.modelPartsRotationReset
                );
        if (resolved.isEmpty()) {
            return;
        }

        EmfIconPartResolver.Resolution resolution = resolved.orElseThrow();
        RadarIconModelPrerenderer prerenderer =
                (RadarIconModelPrerenderer) (Object) this;
        VertexConsumer vertexConsumer = prerenderer.getLayerModelVertexConsumer(
                bufferSource,
                parameters.textures,
                parameters.textureAtlasSprite,
                parameters.mrt
        );
        ModelPart adapter = resolution.renderAdapter();
        prerenderer.getPartPrerenderer().renderPart(
                matrixStack,
                vertexConsumer,
                adapter,
                resolution.centeringPart(),
                parameters
        );
        bufferSource.endBatch();
        if (!parameters.renderedDest.isEmpty()) {
            callbackInfo.setReturnValue(resolution.centeringPart());
        }
    }
}
