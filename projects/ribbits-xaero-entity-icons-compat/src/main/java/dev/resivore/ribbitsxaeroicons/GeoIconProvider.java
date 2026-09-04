package dev.resivore.ribbitsxaeroicons;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.radar.icon.creator.RadarIconCreator;

/** Internal opt-in provider boundary; Canary 2 registers exactly one implementation. */
public interface GeoIconProvider {
    boolean supports(
            Entity entity,
            EntityRenderer<?, ?> renderer,
            EntityRenderState renderState,
            boolean upstreamHandled);

    CacheIdentity cacheIdentity(
            Entity entity, EntityRenderer<?, ?> renderer, EntityRenderState renderState);

    boolean prerender(
            MinimapElementGraphics graphics,
            EntityRenderer<?, ?> renderer,
            EntityRenderState renderState,
            Entity entity,
            RadarIconCreator.Parameters parameters);
}
