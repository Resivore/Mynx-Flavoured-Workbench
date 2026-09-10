package dev.resivore.naturalistxaeroicons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.resivore.naturalistxaeroicons.NaturalistIconAdapter;
import dev.resivore.naturalistxaeroicons.NaturalistModelContracts;
import dev.resivore.naturalistxaeroicons.StarfishCaptureDiagnostic;
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

/** Runs the closed fallback contracts only after Xaero's native path returns no visible parts. */
@Mixin(value = RadarIconModelPrerenderer.class, remap = false)
abstract class RadarIconModelPrerendererMixin {
    @Inject(method = "renderModel", at = @At("RETURN"), cancellable = true, require = 1)
    private void naturalistXaeroIcons$renderContractPart(
            PoseStack pose, XaeroBufferProvider buffers, EntityRenderState state, Model model,
            Entity entity, ModelPart upstreamPart, RadarIconModelPrerenderer.Parameters parameters,
            CallbackInfoReturnable<ModelPart> callback) {
        StarfishCaptureDiagnostic.nativePathObserved(model, parameters.renderedDest.size());
        if (!parameters.renderedDest.isEmpty()) {
            StarfishCaptureDiagnostic.fallbackSkipped("native rendered parts");
            return;
        }
        if (!NaturalistModelContracts.owns(entity)) return;
        try {
            var resolved = NaturalistModelContracts.resolve(entity, model);
            if (resolved.isEmpty()) {
                StarfishCaptureDiagnostic.fallbackSkipped("contract unresolved");
                return;
            }
            var contract = resolved.orElseThrow();
            StarfishCaptureDiagnostic.contractResolved(contract.contract());
            ModelPart selected = contract.selected();
            ModelPart adapter = NaturalistIconAdapter.build(
                    model.root(), contract.source(), selected, contract.trace(), contract.contract().presentation(),
                    contract.contract().neutralizeRootRotation(), contract.contract().normalizeSelectedRootTransform(), contract.contract().path(),
                    contract.contract().preserveAncestorTransforms());
            if (adapter == null) {
                StarfishCaptureDiagnostic.fallbackSkipped("adapter build failed");
                return;
            }
            boolean traceExists = NaturalistIconAdapter.traceExists(parameters.mrt, adapter);
            StarfishCaptureDiagnostic.adapterBuilt(traceExists);
            if (!traceExists) return;
            RadarIconModelPrerenderer self = (RadarIconModelPrerenderer) (Object) this;
            VertexConsumer consumer = self.getLayerModelVertexConsumer(
                    buffers, parameters.textures, parameters.textureAtlasSprite, parameters.mrt);
            int before = parameters.renderedDest.size();
            ModelPart renderCenter = contract.renderCenter();
            StarfishCaptureDiagnostic.renderCenter(contract, selected, renderCenter, parameters.mrt);
            self.getPartPrerenderer().renderPart(pose, consumer, adapter, renderCenter, parameters);
            buffers.endBatch();
            StarfishCaptureDiagnostic.fallbackRendered(before, parameters.renderedDest.size(), adapter, selected, renderCenter,
                    parameters.renderedDest);
            // Xaero's bounded detector records the rendered visible ModelPart, which may be a
            // drawable child rather than this assembly wrapper. This method starts only after an
            // empty upstream result, so any nonempty destination here came from this exact
            // bridge draw. Never treat an empty result as success or cache a blank icon.
            if (!parameters.renderedDest.isEmpty()) callback.setReturnValue(selected);
        } catch (RuntimeException ignored) {
            StarfishCaptureDiagnostic.failed(ignored);
            // Do not turn a malformed/modded Naturalist tree into a partial icon.
            parameters.renderedDest.clear();
            try { buffers.endBatch(); } catch (RuntimeException suppressed) { /* best effort */ }
        }
    }
}
