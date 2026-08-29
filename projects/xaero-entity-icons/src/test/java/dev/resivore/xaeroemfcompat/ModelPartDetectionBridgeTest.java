package dev.resivore.xaeroemfcompat;

import org.junit.jupiter.api.Test;
import net.minecraft.client.model.geom.ModelPart;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace;

import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelPartDetectionBridgeTest {
    @Test
    void representativeEmfBackedPartProducesANonEmptyDetectedPartSet() {
        RepresentativeEmfPart part = new RepresentativeEmfPart(
                "fresh_animations:creeper", "head", new RenderState(true, false, 1.25F, -0.5F));
        Map<RepresentativeEmfPart, Integer> detected = new IdentityHashMap<>();

        ModelPartDetectionBridge.forward(part, 0xA1B2C3D4, detected::put);

        assertFalse(detected.isEmpty());
        assertEquals(1, detected.size());
        assertSame(part, detected.keySet().iterator().next());
        assertEquals(0xA1B2C3D4, detected.get(part));
    }

    @Test
    void bridgeForwardsOnceWithoutChangingRenderState() {
        RenderState state = new RenderState(true, false, 2.0F, 0.75F);
        RepresentativeEmfPart part = new RepresentativeEmfPart(
                "fresh_animations:creeper", "body", state);
        AtomicInteger calls = new AtomicInteger();

        ModelPartDetectionBridge.forward(part, -1, (detected, color) -> {
            assertSame(part, detected);
            assertEquals(-1, color);
            calls.incrementAndGet();
        });

        assertEquals(1, calls.get());
        assertSame(state, part.renderState());
        assertEquals(new RenderState(true, false, 2.0F, 0.75F), part.renderState());
    }

    @Test
    void emfEquivalentCallbackMakesARealXaeroModelTraceNonEmpty() {
        ModelPart part = new ModelPart(List.of(), Map.of());
        ModelRenderTrace trace = new ModelRenderTrace(
                null, Map.of(), null, false, false, false,
                null, null, null, null, 0xFFFFFFFF);

        assertTrue(trace.isEmpty());
        ModelPartDetectionBridge.forward(part, 0xFF112233, trace::addVisibleModelPart);

        assertFalse(trace.isEmpty());
        assertSame(part, trace.getModelPartRenderInfo(part).modelPart);
        assertEquals(0xFF112233, trace.getModelPartRenderInfo(part).color);
    }

    private record RepresentativeEmfPart(String modelId, String partId, RenderState renderState) {
    }

    private record RenderState(boolean visible, boolean skipDraw, float xRotation, float yRotation) {
    }
}
