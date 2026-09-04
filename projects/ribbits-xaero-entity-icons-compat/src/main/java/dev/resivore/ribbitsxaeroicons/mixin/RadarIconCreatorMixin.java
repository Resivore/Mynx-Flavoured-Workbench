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
        Optional<GeoIconProvider> provider =
                GeoIconProviders.find(entity, renderer, renderState, false);
        if (provider.isEmpty()) {
            return upstream;
        }
        return new GeoAwareFormPrerenderer(upstream, provider.orElseThrow());
    }
}
