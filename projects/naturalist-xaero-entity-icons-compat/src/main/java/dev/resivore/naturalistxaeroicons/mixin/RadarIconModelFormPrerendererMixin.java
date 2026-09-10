package dev.resivore.naturalistxaeroicons.mixin;

import dev.resivore.naturalistxaeroicons.StarfishCaptureDiagnostic;
import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.radar.icon.creator.RadarIconCreator;
import xaero.hud.minimap.radar.icon.creator.render.form.model.RadarIconModelFormPrerenderer;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace;

/** Records whether a Starfish cache miss enters Xaero's model-form path before the bridge. */
@Mixin(value = RadarIconModelFormPrerenderer.class, remap = false)
abstract class RadarIconModelFormPrerendererMixin {
    @Inject(method = "prerender", at = @At("HEAD"), require = 1)
    private void naturalistXaeroIcons$observeStarfishModelForm(
            MinimapElementGraphics graphics, EntityRenderer<?, ?> renderer, EntityRenderState state,
            EntityModel<?> model, Entity entity, List<ModelRenderTrace> traces, RadarIconCreator.Parameters parameters,
            CallbackInfoReturnable<Boolean> callback) {
        String textures = traces == null ? "null" : traces.stream()
                .map(trace -> String.valueOf(trace.textures)).distinct().limit(4).toList().toString();
        StarfishCaptureDiagnostic.modelFormStarted(entity, traces == null ? -1 : traces.size(), textures);
    }
}
