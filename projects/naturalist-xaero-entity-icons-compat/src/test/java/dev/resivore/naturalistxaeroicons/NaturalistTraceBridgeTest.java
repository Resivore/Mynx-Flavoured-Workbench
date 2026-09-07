package dev.resivore.naturalistxaeroicons;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import net.minecraft.client.model.geom.ModelPart;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelPartRenderTrace;
import xaero.hud.minimap.radar.icon.creator.render.trace.ModelRenderTrace;

class NaturalistTraceBridgeTest {
    @Test void naturalistAdapterResolvesItsRecordedOriginalTrace() {
        ModelPart original = part(Map.of());
        ModelPart adapter = NaturalistIconAdapter.build(part(Map.of("selected", original)), original);
        ModelRenderTrace trace = trace();
        trace.addVisibleModelPart(original, 0xFF123456);

        ModelPartRenderTrace resolved = NaturalistIconAdapter.resolveTrace(trace, adapter);
        assertNotNull(resolved);
        assertSame(original, resolved.modelPart);
        assertEquals(0xFF123456, resolved.color);
    }

    @Test void nonNaturalistAndEmfStyleAdaptersRemainUntouched() {
        ModelRenderTrace trace = trace();
        ModelPart ordinary = part(Map.of());
        ModelPart emfStyleAdapter = part(Map.of("adapter", part(Map.of())));
        trace.addVisibleModelPart(ordinary, 0xFF102030);

        assertNull(NaturalistIconAdapter.resolveTrace(trace, ordinary));
        assertSame(ordinary, trace.getModelPartRenderInfo(ordinary).modelPart);
        assertNull(NaturalistIconAdapter.resolveTrace(trace, emfStyleAdapter));
        assertNull(trace.getModelPartRenderInfo(emfStyleAdapter));
    }

    @Test void missingOrUntracedNaturalistMappingsFailClosed() {
        ModelPart original = part(Map.of());
        ModelPart adapter = NaturalistIconAdapter.build(part(Map.of("selected", original)), original);
        ModelRenderTrace trace = trace();

        assertNull(NaturalistIconAdapter.resolveTrace(trace, adapter));
        assertNull(NaturalistIconAdapter.resolveTrace(trace, part(Map.of())));
    }

    @Test void naturalistNoLongerCompetesForC9CallerRedirect() throws Exception {
        Path module = Path.of(System.getProperty("projectRoot"));
        String config = Files.readString(module.resolve("src/main/resources/naturalist_xaero_entity_icons_compat.mixins.json"));
        assertTrue(config.contains("ModelRenderTraceMixin"));
        assertFalse(config.contains("RadarIconModelPartPrerendererMixin"));
        assertFalse(Files.exists(module.resolve("src/main/java/dev/resivore/naturalistxaeroicons/mixin/RadarIconModelPartPrerendererMixin.java")));

        Path c9 = module.getParent().resolve("xaero-entity-icons/src/main/java/dev/resivore/xaeroemfcompat/mixin/RadarIconModelPartPrerendererMixin.java");
        String c9Source = Files.readString(c9);
        assertEquals(1, occurrences(c9Source, "ModelRenderTrace;getModelPartRenderInfo"));
        assertEquals(1, occurrences(c9Source, "xaeroEmfEntityIconCompat$resolveAdapterTrace"));
        assertTrue(c9Source.contains("require = 1"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int index = 0; (index = text.indexOf(needle, index)) >= 0; index += needle.length()) count++;
        return count;
    }

    private static ModelPart part(Map<String, ModelPart> children) {
        return new ModelPart(List.of(), children);
    }

    private static ModelRenderTrace trace() {
        return new ModelRenderTrace(null, Map.of(), null, false, false, false,
                null, null, null, null, 0xFFFFFFFF);
    }
}
