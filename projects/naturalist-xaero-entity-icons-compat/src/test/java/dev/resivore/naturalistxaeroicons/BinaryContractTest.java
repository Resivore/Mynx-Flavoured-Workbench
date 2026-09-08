package dev.resivore.naturalistxaeroicons;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;

class BinaryContractTest {
    @Test void exactValidationInputsExposeRequiredXaeroAndNaturalistSeams() throws Exception {
        try (JarFile xaero = new JarFile(Path.of(System.getProperty("xaeroJar")).toFile());
             JarFile naturalist = new JarFile(Path.of(System.getProperty("naturalistJar")).toFile())) {
            assertNotNull(xaero.getEntry("xaero/hud/minimap/radar/icon/creator/render/form/model/RadarIconModelPrerenderer.class"));
            assertNotNull(xaero.getEntry("xaero/hud/minimap/radar/icon/cache/RadarIconCache.class"));
            assertNotNull(naturalist.getEntry("com/crispytwig/naturalist/client/renderer/NaturalistMobRenderer.class"));
            assertNotNull(naturalist.getEntry("com/crispytwig/naturalist/client/model/RhinoModel.class"));
            assertNotNull(naturalist.getEntry("com/crispytwig/naturalist/client/model/WhaleBabyModel.class"));
        }
    }

    @Test void packagedArtifactContainsOnlyThisCompanion() throws Exception {
        try (JarFile jar = new JarFile(Path.of(System.getProperty("patchJar")).toFile())) {
            assertNotNull(jar.getEntry("fabric.mod.json"));
            assertNotNull(jar.getEntry("dev/resivore/naturalistxaeroicons/NaturalistModelContracts.class"));
            assertNull(jar.getEntry("com/crispytwig/naturalist/client/model/RhinoModel.class"));
            assertNull(jar.getEntry("xaero/hud/minimap/radar/icon/creator/RadarIconCreator.class"));
        }
    }

    @Test void c7UsesTheEnclosingNativeCapturePoseWithoutRedirectsOrBlankIcons() throws Exception {
        Path root = Path.of(System.getProperty("projectRoot"));
        String mixins = Files.readString(root.resolve("src/main/resources/naturalist_xaero_entity_icons_compat.mixins.json"));
        assertFalse(mixins.contains("RadarIconModelPartPrerendererMixin"));
        assertTrue(mixins.contains("ModelRenderTraceMixin"));
        assertTrue(mixins.contains("RadarIconModelPrerendererMixin"));
        String bridge = Files.readString(root.resolve("src/main/java/dev/resivore/naturalistxaeroicons/NaturalistIconAdapter.java"));
        assertTrue(bridge.contains("traceSources"));
        assertFalse(bridge.contains("@Redirect"));
        String prerenderer = Files.readString(root.resolve("src/main/java/dev/resivore/naturalistxaeroicons/mixin/RadarIconModelPrerendererMixin.java"));
        assertTrue(prerenderer.contains("renderedDest.contains(adapter)"));
        assertFalse(prerenderer.contains("callback.setReturnValue(adapter)"));
        assertTrue(prerenderer.contains("method = \"renderModel\", at = @At(\"HEAD\")"));
        assertTrue(prerenderer.contains("pose.pushPose()"));
        assertTrue(prerenderer.contains("pose.popPose()"));
    }
}
