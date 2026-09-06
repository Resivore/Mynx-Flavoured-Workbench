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
        dev.resivore.xaeroemfcompat.IconDiagnostics.context(
                net.minecraft.world.entity.EntityType.getKey(entity.getType()) + " model=" + model.getClass().getName());
        try {
            if (EmfIconPartResolver.isSupportedEmfRoot(model.root())) {
                dev.resivore.xaeroemfcompat.IconDiagnostics.observe(entity.getType());
            }
            if (!parameters.renderedDest.isEmpty()) {
                dev.resivore.xaeroemfcompat.IconDiagnostics.event("UPSTREAM_NONEMPTY_DESTINATION", "upstream");
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
            dev.resivore.xaeroemfcompat.IconDiagnostics.event(
                    parameters.renderedDest.isEmpty() ? "DRAW_NO_DESTINATION" : "DRAW_NONEMPTY_DESTINATION",
                    resolution.geometryPath());
            if (!parameters.renderedDest.isEmpty()) {
                callbackInfo.setReturnValue(resolution.centeringPart());
            }
        } catch (RuntimeException failure) {
            parameters.renderedDest.clear();
            try { bufferSource.endBatch(); } catch (RuntimeException flushFailure) {
                failure.addSuppressed(flushFailure);
            }
            dev.resivore.xaeroemfcompat.IconDiagnostics.event("ADAPTER_BUILD_OR_DRAW_FAILURE", failure.getClass().getSimpleName());
        } finally {
            dev.resivore.xaeroemfcompat.IconDiagnostics.clearContext();
        }
    }
}
