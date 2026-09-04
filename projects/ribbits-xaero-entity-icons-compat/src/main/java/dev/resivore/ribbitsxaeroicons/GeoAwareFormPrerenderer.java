package dev.resivore.ribbitsxaeroicons;

import java.util.List;
import java.util.Objects;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.radar.icon.creator.RadarIconCreator;
import xaero.hud.minimap.radar.icon.creator.render.form.IRadarIconFormPrerenderer;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace;

/** Lets Xaero retain its complete form/atlas lifecycle while substituting only the Geo draw. */
public final class GeoAwareFormPrerenderer implements IRadarIconFormPrerenderer {
    private final IRadarIconFormPrerenderer upstream;
    private final GeoIconProvider provider;
    private Boolean upstreamRequiresEntityModel;

    public GeoAwareFormPrerenderer(
            IRadarIconFormPrerenderer upstream, GeoIconProvider provider) {
        this.upstream = Objects.requireNonNull(upstream, "upstream");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public boolean requiresEntityModel() {
        if (upstreamRequiresEntityModel == null) {
            upstreamRequiresEntityModel = upstream.requiresEntityModel();
        }
        GeoIconLog.stage("requires-model", upstream.getClass().getName(),
                "upstream=" + upstreamRequiresEntityModel + " wrapper=false");
        // The Geo branch reuses the populated state without asking Xaero for a vanilla model.
        return false;
    }

    @Override
    public boolean isFlipped() {
        return upstream.isFlipped();
    }

    @Override
    public boolean isOutlined() {
        return upstream.isOutlined();
    }

    @Override
    public <S extends EntityRenderState> boolean prerender(
            MinimapElementGraphics graphics,
            EntityRenderer<?, ? super S> renderer,
            S renderState,
            EntityModel<S> unusedVanillaModel,
            Entity entity,
            List<ModelRenderTrace> unusedVanillaTraces,
            RadarIconCreator.Parameters parameters) {
        // Resolve the upstream protocol here too: no implicit call-order precondition.
        requiresEntityModel();
        if (!upstreamRequiresEntityModel) {
            return upstream.prerender(
                    graphics,
                    renderer,
                    renderState,
                    unusedVanillaModel,
                    entity,
                    unusedVanillaTraces,
                    parameters);
        }
        if (!provider.supports(entity, renderer, renderState, false)) {
            return false;
        }
        return provider.prerender(graphics, renderer, renderState, entity, parameters);
    }
}
