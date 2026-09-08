package dev.resivore.naturalistxaeroicons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.resivore.naturalistxaeroicons.BrownBearDiagnostic;
import dev.resivore.naturalistxaeroicons.NaturalistIconAdapter;
import dev.resivore.naturalistxaeroicons.NaturalistIconPresentation;
import dev.resivore.naturalistxaeroicons.NaturalistModelContracts;
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

/** Scopes Brown Bear's native capture and runs fallback contracts only after an empty native path. */
@Mixin(value = RadarIconModelPrerenderer.class, remap = false)
abstract class RadarIconModelPrerendererMixin {
    @Inject(method = "renderModel", at = @At("HEAD"), require = 1)
    private void naturalistXaeroIcons$beginNativePresentation(
            PoseStack pose, XaeroBufferProvider buffers, EntityRenderState state, Model model,
            Entity entity, ModelPart upstreamPart, RadarIconModelPrerenderer.Parameters parameters,
            CallbackInfoReturnable<ModelPart> callback) {
        NaturalistIconPresentation.begin(entity);
        var presentation = NaturalistIconPresentation.currentNativePresentation();
        if (presentation != null) {
            // This is the enclosing native-capture pose, before Xaero renders/caches its selected
            // ModelPart.  It replaces the C6 late part-render hook, which could be bypassed by
            // the accepted Xaero × EMF render redirect.
            pose.pushPose();
            pose.scale(presentation.scale(), presentation.scale(), presentation.scale());
            // C11 is deliberately not a sizing attempt. This must make the final radar raster
            // visibly sideways if this native capture seam controls the displayed Bear icon.
            pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
            BrownBearDiagnostic.nativePresentationEntered();
        }
    }

    @Inject(method = "renderModel", at = @At("RETURN"), require = 1)
    private void naturalistXaeroIcons$endNativePresentation(
            PoseStack pose, XaeroBufferProvider buffers, EntityRenderState state, Model model,
            Entity entity, ModelPart upstreamPart, RadarIconModelPrerenderer.Parameters parameters,
            CallbackInfoReturnable<ModelPart> callback) {
        if (NaturalistIconPresentation.currentNativePresentation() != null) {
            BrownBearDiagnostic.nativePrerenderReturned(!parameters.renderedDest.isEmpty());
            pose.popPose();
        }
        NaturalistIconPresentation.end();
    }

    @Inject(method = "renderModel", at = @At("RETURN"), cancellable = true, require = 1)
    private void naturalistXaeroIcons$renderContractPart(
            PoseStack pose, XaeroBufferProvider buffers, EntityRenderState state, Model model,
            Entity entity, ModelPart upstreamPart, RadarIconModelPrerenderer.Parameters parameters,
            CallbackInfoReturnable<ModelPart> callback) {
        if (!parameters.renderedDest.isEmpty() || !NaturalistModelContracts.owns(entity)) return;
        try {
            var resolved = NaturalistModelContracts.resolve(entity, model);
            if (resolved.isEmpty()) return;
            var contract = resolved.orElseThrow();
            ModelPart selected = contract.selected();
            ModelPart adapter = NaturalistIconAdapter.build(
                    model.root(), contract.source(), selected, contract.trace(), contract.contract().presentation(),
                    contract.contract().neutralizeRootRotation(), contract.contract().normalizeSelectedRootTransform(), contract.contract().path(),
                    contract.contract().preserveAncestorTransforms());
            if (adapter == null || !NaturalistIconAdapter.traceExists(parameters.mrt, adapter)) return;
            RadarIconModelPrerenderer self = (RadarIconModelPrerenderer) (Object) this;
            VertexConsumer consumer = self.getLayerModelVertexConsumer(
                    buffers, parameters.textures, parameters.textureAtlasSprite, parameters.mrt);
            self.getPartPrerenderer().renderPart(pose, consumer, adapter, selected, parameters);
            buffers.endBatch();
            // Xaero's bounded detector records the rendered visible ModelPart, which may be a
            // drawable child rather than this assembly wrapper. This method starts only after an
            // empty upstream result, so any nonempty destination here came from this exact
            // bridge draw. Never treat an empty result as success or cache a blank icon.
            if (!parameters.renderedDest.isEmpty()) callback.setReturnValue(selected);
        } catch (RuntimeException ignored) {
            // Do not turn a malformed/modded Naturalist tree into a partial icon.
            parameters.renderedDest.clear();
            try { buffers.endBatch(); } catch (RuntimeException suppressed) { /* best effort */ }
        }
    }
}
