package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.Test;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.radar.icon.creator.RadarIconCreator;
import xaero.hud.minimap.radar.icon.creator.render.form.IRadarIconFormPrerenderer;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace;

class GeoAwareFormPrerendererTest {
    @Test
    void nonModelUpstreamIsMemoizedAndItsEntirePrerenderPathIsDelegated() {
        CountingUpstream upstream = new CountingUpstream(false, true, false, true);
        CountingProvider provider = new CountingProvider(true, false);
        GeoAwareFormPrerenderer wrapper = new GeoAwareFormPrerenderer(upstream, provider);

        assertFalse(wrapper.requiresEntityModel());
        assertFalse(wrapper.requiresEntityModel());
        assertEquals(1, upstream.requirementCalls);
        assertTrue(prerender(wrapper));
        assertEquals(1, upstream.prerenderCalls);
        assertEquals(0, provider.supportCalls);
        assertEquals(0, provider.prerenderCalls);
    }

    @Test
    void modelRequiringUpstreamUsesOnlyTheOwnedGeoProviderPath() {
        CountingUpstream upstream = new CountingUpstream(true, false, true, false);
        CountingProvider provider = new CountingProvider(true, true);
        GeoAwareFormPrerenderer wrapper = new GeoAwareFormPrerenderer(upstream, provider);

        assertFalse(wrapper.requiresEntityModel());
        assertFalse(wrapper.requiresEntityModel());
        assertEquals(1, upstream.requirementCalls);
        assertTrue(prerender(wrapper));
        assertEquals(0, upstream.prerenderCalls);
        assertEquals(1, provider.supportCalls);
        assertEquals(1, provider.prerenderCalls);
    }

    @Test
    void ownershipDriftAfterWrappingFailsClosedWithoutSubmittingEitherRenderer() {
        CountingUpstream upstream = new CountingUpstream(true, false, false, true);
        CountingProvider provider = new CountingProvider(false, true);
        GeoAwareFormPrerenderer wrapper = new GeoAwareFormPrerenderer(upstream, provider);

        assertFalse(wrapper.requiresEntityModel());
        assertFalse(prerender(wrapper));
        assertEquals(0, upstream.prerenderCalls);
        assertEquals(1, provider.supportCalls);
        assertEquals(0, provider.prerenderCalls);
    }

    @Test
    void prerenderBeforeXaeroAsksForModelRequirementFailsClosedAndFlagsDelegate() {
        CountingUpstream upstream = new CountingUpstream(true, true, false, true);
        CountingProvider provider = new CountingProvider(true, true);
        GeoAwareFormPrerenderer wrapper = new GeoAwareFormPrerenderer(upstream, provider);

        assertFalse(prerender(wrapper));
        assertEquals(0, upstream.requirementCalls);
        assertEquals(0, upstream.prerenderCalls);
        assertEquals(0, provider.supportCalls);
        assertEquals(0, provider.prerenderCalls);
        assertTrue(wrapper.isFlipped());
        assertFalse(wrapper.isOutlined());
    }

    private static boolean prerender(GeoAwareFormPrerenderer wrapper) {
        return wrapper.<EntityRenderState>prerender(
                null, null, null, null, null, List.of(), null);
    }

    private static final class CountingUpstream implements IRadarIconFormPrerenderer {
        private final boolean requiresModel;
        private final boolean flipped;
        private final boolean outlined;
        private final boolean prerenderResult;
        private int requirementCalls;
        private int prerenderCalls;

        private CountingUpstream(
                boolean requiresModel,
                boolean flipped,
                boolean outlined,
                boolean prerenderResult) {
            this.requiresModel = requiresModel;
            this.flipped = flipped;
            this.outlined = outlined;
            this.prerenderResult = prerenderResult;
        }

        @Override
        public boolean requiresEntityModel() {
            requirementCalls++;
            return requiresModel;
        }

        @Override
        public boolean isFlipped() {
            return flipped;
        }

        @Override
        public boolean isOutlined() {
            return outlined;
        }

        @Override
        public <S extends EntityRenderState> boolean prerender(
                MinimapElementGraphics graphics,
                EntityRenderer<?, ? super S> renderer,
                S renderState,
                EntityModel<S> model,
                Entity entity,
                List<ModelRenderTrace> traces,
                RadarIconCreator.Parameters parameters) {
            prerenderCalls++;
            return prerenderResult;
        }
    }

    private static final class CountingProvider implements GeoIconProvider {
        private final boolean supported;
        private final boolean prerenderResult;
        private int supportCalls;
        private int prerenderCalls;

        private CountingProvider(boolean supported, boolean prerenderResult) {
            this.supported = supported;
            this.prerenderResult = prerenderResult;
        }

        @Override
        public boolean supports(
                Entity entity,
                EntityRenderer<?, ?> renderer,
                EntityRenderState renderState,
                boolean upstreamHandled) {
            supportCalls++;
            return supported;
        }

        @Override
        public CacheIdentity cacheIdentity(
                Entity entity, EntityRenderer<?, ?> renderer, EntityRenderState renderState) {
            throw new AssertionError("cache identity is not part of prerender dispatch");
        }

        @Override
        public boolean prerender(
                MinimapElementGraphics graphics,
                EntityRenderer<?, ?> renderer,
                EntityRenderState renderState,
                Entity entity,
                RadarIconCreator.Parameters parameters) {
            prerenderCalls++;
            return prerenderResult;
        }
    }
}
