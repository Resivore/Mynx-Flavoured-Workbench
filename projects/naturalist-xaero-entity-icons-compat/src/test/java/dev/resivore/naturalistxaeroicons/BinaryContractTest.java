package dev.resivore.naturalistxaeroicons;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class BinaryContractTest {
    @Test void exactValidationInputsExposeRequiredXaeroAndNaturalistSeams() throws Exception {
        try (JarFile xaero = new JarFile(Path.of(System.getProperty("xaeroJar")).toFile());
             JarFile naturalist = new JarFile(Path.of(System.getProperty("naturalistJar")).toFile())) {
            assertNotNull(xaero.getEntry("xaero/hud/minimap/radar/icon/creator/render/form/model/RadarIconModelPrerenderer.class"));
            assertNotNull(xaero.getEntry("xaero/hud/minimap/radar/icon/cache/RadarIconCache.class"));
            assertNotNull(xaero.getEntry("xaero/hud/minimap/radar/icon/cache/RadarIconEntityCache.class"));
            assertNotNull(xaero.getEntry("xaero/hud/minimap/radar/icon/RadarIconManager.class"));
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

    @Test void c13ScopesTheBrownBearCorrectionToTheEvidencedSpriteCreatorArgument() throws Exception {
        Path root = Path.of(System.getProperty("projectRoot"));
        String mixins = Files.readString(root.resolve("src/main/resources/naturalist_xaero_entity_icons_compat.mixins.json"));
        assertTrue(mixins.contains("ModelRenderTraceMixin"));
        assertTrue(mixins.contains("RadarIconModelPrerendererMixin"));
        assertFalse(mixins.contains("RadarIconCreatorMixin"));
        assertFalse(mixins.contains("RadarIconModelFormPrerendererMixin"));
        assertFalse(mixins.contains("RadarIconModelPartPrerendererMixin"));
        assertFalse(mixins.contains("RadarIconEntityCacheMixin"));
        assertFalse(mixins.contains("RadarIconEntityCacheStorageAccessor"));
        String bridge = Files.readString(root.resolve("src/main/java/dev/resivore/naturalistxaeroicons/NaturalistIconAdapter.java"));
        assertTrue(bridge.contains("traceSources"));
        assertFalse(bridge.contains("@Redirect"));
        String prerenderer = Files.readString(root.resolve("src/main/java/dev/resivore/naturalistxaeroicons/mixin/RadarIconModelPrerendererMixin.java"));
        assertTrue(prerenderer.contains("if (!parameters.renderedDest.isEmpty()) callback.setReturnValue(selected)"));
        assertFalse(prerenderer.contains("renderedDest.contains(adapter)"));
        assertFalse(prerenderer.contains("callback.setReturnValue(adapter)"));
        assertFalse(prerenderer.contains("Axis.ZP.rotationDegrees(90.0F)"));
        assertFalse(prerenderer.contains("pose.scale("));
        String manager = Files.readString(root.resolve("src/main/java/dev/resivore/naturalistxaeroicons/mixin/RadarIconManagerMixin.java"));
        assertTrue(manager.contains("@ModifyVariable"));
        assertTrue(manager.contains("@At(\"STORE\")"));
        assertTrue(manager.contains("index = 19"));
        assertTrue(manager.contains("BrownBearSpritePresentation.scaleForCurrentRequest"));
        assertFalse(manager.contains("@Redirect"));
        String presentation = Files.readString(root.resolve("src/main/java/dev/resivore/naturalistxaeroicons/BrownBearSpritePresentation.java"));
        assertTrue(presentation.contains("SCALE = 0.12F"));
        assertTrue(presentation.contains("RadarIconSpriteForm"));
        assertTrue(presentation.contains("Math.min(parameters.scale, SCALE)"));
        assertFalse(presentation.contains("pose.scale"));
        String genericManager = Files.readString(root.getParent().resolve(
                "xaero-entity-icons/src/main/java/dev/resivore/xaeroemfcompat/mixin/RadarIconManagerMixin.java"));
        assertEquals(1, genericManager.split("@Redirect", -1).length - 1);
        assertTrue(genericManager.contains("xaeroEmf$retryFailedAtActualPrerender"));
    }

    @Test void xaeroC13AuditEstablishesCacheBeforeCreatorAndTheSpriteScaleSeam() throws Exception {
        try (JarFile xaero = new JarFile(Path.of(System.getProperty("xaeroJar")).toFile())) {
            ClassNode cache = readClass(xaero, "xaero/hud/minimap/radar/icon/cache/RadarIconEntityCache.class");
            assertTrue(cache.fields.stream().map(field -> field.name).anyMatch("storage"::equals));

            ClassNode manager = readClass(xaero, "xaero/hud/minimap/radar/icon/RadarIconManager.class");
            MethodNode get = manager.methods.stream()
                    .filter(method -> method.name.equals("get") && method.desc.contains("RadarIconDefinition"))
                    .findFirst().orElseThrow();
            int cacheRead = callIndex(get, "xaero/hud/minimap/radar/icon/cache/RadarIconEntityCache", "get");
            int creator = callIndex(get, "xaero/hud/minimap/radar/icon/creator/RadarIconCreator", "create");
            assertTrue(cacheRead >= 0 && cacheRead < creator,
                    "Xaero must consult the entity/variant cache before native icon creation");

            assertNotNull(xaero.getEntry("xaero/hud/minimap/radar/icon/creator/RadarIconCreator.class"));
            assertNotNull(xaero.getEntry("xaero/hud/minimap/radar/icon/creator/RadarIconCreator$Parameters.class"));
            assertNotNull(xaero.getEntry("xaero/hud/minimap/radar/icon/definition/form/sprite/RadarIconSpriteForm.class"));
            assertNotNull(xaero.getEntry("xaero/hud/minimap/radar/icon/creator/render/form/sprite/RadarIconSpriteFormPrerenderer.class"));
            assertNotNull(xaero.getEntry("xaero/hud/minimap/radar/icon/creator/render/form/model/part/RadarIconModelPartPrerenderer.class"));
            assertNotNull(xaero.getEntry("xaero/hud/minimap/radar/icon/creator/render/form/model/RadarIconModelPrerenderer$Parameters.class"));
        }
    }

    private static ClassNode readClass(JarFile jar, String path) throws Exception {
        var entry = jar.getJarEntry(path);
        assertNotNull(entry, path);
        try (var stream = jar.getInputStream(entry)) {
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }

    private static int callIndex(MethodNode method, String owner, String name) {
        int index = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner) && call.name.equals(name)) return index;
            index++;
        }
        return -1;
    }
}
