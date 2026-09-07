package dev.resivore.naturalistxaeroicons.mixin;

import dev.resivore.naturalistxaeroicons.NaturalistIconAdapter;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.hud.minimap.radar.icon.creator.render.form.model.part.RadarIconModelPartPrerenderer;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelPartRenderTrace;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace;

/** Lets Xaero retain the current model's traced texture/color for our detached ancestry bridge. */
@Mixin(value = RadarIconModelPartPrerenderer.class, remap = false)
abstract class RadarIconModelPartPrerendererMixin {
    @Redirect(method = "renderPart", at = @At(value = "INVOKE", target =
            "Lxaero/hud/minimap/radar/icon/creator/render/trace/ModelRenderTrace;getModelPartRenderInfo(Lnet/minecraft/client/model/geom/ModelPart;)Lxaero/hud/minimap/radar/icon/creator/render/trace/ModelPartRenderTrace;"), require = 1)
    private ModelPartRenderTrace naturalistXaeroIcons$resolveAdapterTrace(ModelRenderTrace trace, ModelPart part) {
        ModelPartRenderTrace direct = trace.getModelPartRenderInfo(part);
        if (direct != null) return direct;
        ModelPart original = NaturalistIconAdapter.tracePart(part);
        return original == null ? null : trace.getModelPartRenderInfo(original);
    }
}
