package dev.resivore.naturalistxaeroicons.mixin;

import dev.resivore.naturalistxaeroicons.NaturalistIconAdapter;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelPartRenderTrace;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace;

/**
 * Makes only Naturalist's detached ancestry bridge share its selected part's
 * already-recorded trace. This is intentionally below Xaero's caller seam, so
 * other companions retain ownership of their own caller-side adapters.
 */
@Mixin(value = ModelRenderTrace.class, remap = false)
abstract class ModelRenderTraceMixin {
    @Inject(method = "getModelPartRenderInfo", at = @At("HEAD"), cancellable = true)
    private void naturalistXaeroIcons$resolveAdapterTrace(
            ModelPart part,
            CallbackInfoReturnable<ModelPartRenderTrace> callback
    ) {
        ModelPartRenderTrace originalTrace = NaturalistIconAdapter.resolveTrace(
                (ModelRenderTrace) (Object) this, part);
        if (originalTrace != null) callback.setReturnValue(originalTrace);
    }
}
