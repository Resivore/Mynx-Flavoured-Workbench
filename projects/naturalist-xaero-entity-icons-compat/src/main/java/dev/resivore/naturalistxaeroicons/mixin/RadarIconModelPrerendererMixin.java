package dev.resivore.naturalistxaeroicons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.resivore.naturalistxaeroicons.NaturalistIconAdapter;
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

/** Runs only after Xaero's ordinary ModelPart path produced no destination. */
@Mixin(value = RadarIconModelPrerenderer.class, remap = false)
abstract class RadarIconModelPrerendererMixin {
    @Inject(method = "renderModel", at = @At("RETURN"), cancellable = true, require = 1)
    private void naturalistXaeroIcons$renderContractPart(
            PoseStack pose, XaeroBufferProvider buffers, EntityRenderState state, Model model,
            Entity entity, ModelPart upstreamPart, RadarIconModelPrerenderer.Parameters parameters,
            CallbackInfoReturnable<ModelPart> callback) {
        if (!parameters.renderedDest.isEmpty() || !NaturalistModelContracts.owns(entity)) return;
        try {
            var resolved = NaturalistModelContracts.resolve(entity, model);
            if (resolved.isEmpty()) return;
            ModelPart selected = resolved.orElseThrow().selected();
            ModelPart adapter = NaturalistIconAdapter.build(model.root(), selected);
            if (adapter == null || !NaturalistIconAdapter.traceExists(parameters.mrt, adapter)) return;
            RadarIconModelPrerenderer self = (RadarIconModelPrerenderer) (Object) this;
            VertexConsumer consumer = self.getLayerModelVertexConsumer(
                    buffers, parameters.textures, parameters.textureAtlasSprite, parameters.mrt);
            self.getPartPrerenderer().renderPart(pose, consumer, adapter, selected, parameters);
            buffers.endBatch();
            if (!parameters.renderedDest.isEmpty()) callback.setReturnValue(selected);
        } catch (RuntimeException ignored) {
            // Do not turn a malformed/modded Naturalist tree into a partial icon.
            parameters.renderedDest.clear();
            try { buffers.endBatch(); } catch (RuntimeException suppressed) { /* best effort */ }
        }
    }
}
