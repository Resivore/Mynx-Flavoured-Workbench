package dev.resivore.ribbitsxaeroicons.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.resivore.ribbitsxaeroicons.GeoAwareFormPrerenderer;
import dev.resivore.ribbitsxaeroicons.GeoIconProvider;
import dev.resivore.ribbitsxaeroicons.GeoIconProviders;
import java.util.Optional;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.radar.icon.creator.RadarIconCreator;
import xaero.hud.minimap.radar.icon.creator.render.form.IRadarIconFormPrerenderer;
import xaero.hud.minimap.radar.icon.definition.form.RadarIconForm;

/** Wraps the one form-prerenderer lookup, before Xaero attempts a vanilla EntityModel trace. */
@Mixin(value = RadarIconCreator.class, remap = false)
abstract class RadarIconCreatorMixin {
    @Redirect(
            method = "create(Lxaero/hud/minimap/element/render/MinimapElementGraphics;"
                    + "Lnet/minecraft/client/renderer/entity/EntityRenderer;"
                    + "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
                    + "Lnet/minecraft/world/entity/Entity;"
                    + "Lcom/mojang/blaze3d/pipeline/RenderTarget;"
                    + "Lxaero/hud/minimap/radar/icon/creator/RadarIconCreator$Parameters;)"
                    + "Lxaero/common/icon/XaeroIcon;",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/hud/minimap/radar/icon/definition/form/RadarIconForm;"
                            + "getPrerenderer()"
                            + "Lxaero/hud/minimap/radar/icon/creator/render/form/"
                            + "IRadarIconFormPrerenderer;"),
            require = 1)
    private <S extends EntityRenderState> IRadarIconFormPrerenderer
            ribbitsXaeroIcons$wrapGeoPrerenderer(
                    RadarIconForm form,
                    MinimapElementGraphics graphics,
                    EntityRenderer<?, ? super S> renderer,
                    S renderState,
                    Entity entity,
                    RenderTarget defaultTarget,
                    RadarIconCreator.Parameters parameters) {
        IRadarIconFormPrerenderer upstream = form.getPrerenderer();
        if (upstream == null) {
            return upstream;
        }
        if (dev.resivore.ribbitsxaeroicons.RibbitGeoIconProvider.owns(entity)) {
            dev.resivore.ribbitsxaeroicons.GeoIconLog.stage("wrapper-reached", "ribbits:ribbit", "provider lookup attempted");
        }
        Optional<GeoIconProvider> provider =
                GeoIconProviders.find(entity, renderer, renderState, false);
        if (provider.isEmpty()) {
            return upstream;
        }
        dev.resivore.ribbitsxaeroicons.GeoIconLog.stage("provider-selected", "ribbits:ribbit", "Ribbit provider selected");
        return new GeoAwareFormPrerenderer(upstream, provider.orElseThrow());
    }
    @org.spongepowered.asm.mixin.injection.Inject(method = "create", at = @At("RETURN"), require = 1)
    private void ribbitsXaeroIcons$result(MinimapElementGraphics graphics,
            EntityRenderer<?, ?> renderer, EntityRenderState state, Entity entity,
            RenderTarget target, RadarIconCreator.Parameters parameters,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<xaero.common.icon.XaeroIcon> callback) {
        if (dev.resivore.ribbitsxaeroicons.RibbitGeoIconProvider.owns(entity)) {
            var result = callback.getReturnValue();
            Object identity = parameters.variant instanceof dev.resivore.ribbitsxaeroicons.RibbitCacheVariant v
                    ? v.identity() : "ribbits:ribbit:no-variant";
            dev.resivore.ribbitsxaeroicons.GeoIconLog.stage("xaero-result", identity,
                    result == null || result == xaero.hud.minimap.radar.icon.RadarIconManager.FAILED
                    ? "discarded-or-failed" : "accepted atlas=" + (result.getTextureAtlas() != null));
        }
    }

}
