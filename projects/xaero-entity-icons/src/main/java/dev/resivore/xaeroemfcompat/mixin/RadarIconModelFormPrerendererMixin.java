package dev.resivore.xaeroemfcompat.mixin;

import dev.resivore.xaeroemfcompat.IconPresentationPolicy;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.radar.icon.creator.RadarIconCreator;
import xaero.hud.minimap.radar.icon.creator.render.form.model.RadarIconModelFormPrerenderer;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace;

import java.util.List;

/** C11's exact upscale hook, immediately before Xaero applies baseScale. */
@Mixin(value = RadarIconModelFormPrerenderer.class, remap = false)
abstract class RadarIconModelFormPrerendererMixin {
    @Inject(
            method = "prerender",
            at = @At(
                    value = "FIELD",
                    target = "Lxaero/hud/minimap/radar/icon/definition/form/model/config/"
                            + "RadarIconModelConfig;baseScale:F",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 0,
                    shift = At.Shift.BEFORE),
            require = 1)
    private <S extends EntityRenderState> void xaeroEmf$applyTargetedUpscale(
            MinimapElementGraphics graphics,
            EntityRenderer<?, ? super S> renderer,
            S state,
            EntityModel<S> model,
            Entity entity,
            List<ModelRenderTrace> traces,
            RadarIconCreator.Parameters parameters,
            CallbackInfoReturnable<Boolean> callback) {
        IconPresentationPolicy.applyUpscaleForCurrentRequest(graphics.pose());
    }
}
