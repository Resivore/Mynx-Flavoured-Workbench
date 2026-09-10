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

    @Test void c27ScopesOnlyWhaleAndKeepsTheFailClosedBridge() throws Exception {
        Path root = Path.of(System.getProperty("projectRoot"));
        String mixins = Files.readString(root.resolve("src/main/resources/naturalist_xaero_entity_icons_compat.mixins.json"));
        assertTrue(mixins.contains("ModelRenderTraceMixin"));
        assertTrue(mixins.contains("RadarIconModelPrerendererMixin"));
        assertTrue(mixins.contains("RadarIconCreatorMixin"));
        assertTrue(mixins.contains("RadarIconModelFormPrerendererMixin"));
        assertTrue(mixins.contains("RadarIconModelPartPrerendererMixin"));
        assertTrue(mixins.contains("RadarIconEntityCacheMixin"));
        assertTrue(mixins.contains("RadarIconEntityCacheStorageAccessor"));
        String bridge = Files.readString(root.resolve("src/main/java/dev/resivore/naturalistxaeroicons/NaturalistIconAdapter.java"));
        assertTrue(bridge.contains("traceSources"));
        assertFalse(bridge.contains("@Redirect"));
        String prerenderer = Files.readString(root.resolve("src/main/java/dev/resivore/naturalistxaeroicons/mixin/RadarIconModelPrerendererMixin.java"));
        assertTrue(prerenderer.contains("ModelPart renderCenter = contract.renderCenter()"));
        assertTrue(prerenderer.contains("renderPart(pose, consumer, adapter, renderCenter, parameters)"));
        assertTrue(prerenderer.contains("if (!parameters.renderedDest.isEmpty()) callback.setReturnValue(selected)"));
        assertFalse(prerenderer.contains("renderedDest.contains(adapter)"));
        assertFalse(prerenderer.contains("callback.setReturnValue(adapter)"));
        assertFalse(prerenderer.contains("renderedDest.add("));
        assertFalse(prerenderer.contains("Axis.ZP.rotationDegrees(90.0F)"));
        assertFalse(prerenderer.contains("pose.scale("));
        String contracts = Files.readString(root.resolve("src/main/java/dev/resivore/naturalistxaeroicons/NaturalistModelContracts.java"));
        assertTrue(contracts.contains("p(.75F)), c(\"HippoBabyModel\", \"body/neck\", p(.70F))"));
        assertFalse(contracts.contains("p(.60F)), c(\"HippoBabyModel\", \"body/neck\", p(.70F))"));
        assertTrue(contracts.contains("-2.0F)), c(\"BlackBearBabyModel\", \"body/skull\")"));
        assertTrue(contracts.contains("cWithTraceCenter(\"WhaleModel\", \"body/skullRot\", \"body/skullRot/topJaw\", p(.30F, 0.0F, 1.5708F, 0.0F))"));
        assertTrue(contracts.contains("\"WhaleBabyModel\", \"body/skull\", p(.60F, 0.0F, .7854F, 0.0F)"));
        assertTrue(contracts.contains("cWithTraceCenter"));
        String manager = Files.readString(root.resolve("src/main/java/dev/resivore/naturalistxaeroicons/mixin/RadarIconManagerMixin.java"));
        assertTrue(manager.contains("@ModifyVariable"));
        assertTrue(manager.contains("@At(\"STORE\")"));
        assertTrue(manager.contains("index = 19"));
        assertTrue(manager.contains("BrownBearSpritePresentation.scaleForCurrentRequest"));
        assertTrue(manager.contains("observeStarfishCacheBeforeXaeroEmfRetry"));
        assertTrue(manager.contains("ScorpionCaptureDiagnostic.requestStarted"));
        assertTrue(manager.contains("ScorpionCaptureDiagnostic.cacheLookup"));
        assertTrue(manager.contains("WhaleCaptureDiagnostic.requestStarted"));
        assertTrue(manager.contains("WhaleCaptureDiagnostic.cacheLookup"));
        assertFalse(manager.contains("@Redirect"));
        String presentation = Files.readString(root.resolve("src/main/java/dev/resivore/naturalistxaeroicons/BrownBearSpritePresentation.java"));
        assertTrue(presentation.contains("\"naturalist:bear\""));
        assertTrue(presentation.contains("SCALE = 0.65F"));
        assertTrue(presentation.contains("RadarIconSpriteForm"));
        assertTrue(presentation.contains("Math.min(parameters.scale, SCALE)"));
        assertTrue(presentation.contains("!(parameters.form instanceof RadarIconSpriteForm)"));
        assertFalse(presentation.contains("pose.scale"));
        assertFalse(Files.exists(root.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/ClamCaptureDiagnostic.java")));
        String starfishDiagnostic = Files.readString(root.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/StarfishCaptureDiagnostic.java"));
        assertTrue(starfishDiagnostic.contains("RadarIconCreator#create form="));
        assertTrue(starfishDiagnostic.contains("RadarIconEntityCache#get initial"));
        assertTrue(starfishDiagnostic.contains("selectedGeometry="));
        assertTrue(starfishDiagnostic.contains("rotations="));
        assertTrue(starfishDiagnostic.contains("frameYOffset="));
        assertTrue(starfishDiagnostic.contains("renderCenterHasDirectMrt"));
        assertFalse(starfishDiagnostic.contains("pose.scale"));
        assertFalse(starfishDiagnostic.contains("new XaeroIcon"));
        String scorpionDiagnostic = Files.readString(root.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/ScorpionCaptureDiagnostic.java"));
        assertTrue(scorpionDiagnostic.contains("naturalist:desert_scorpion"));
        assertTrue(scorpionDiagnostic.contains("naturalist:jungle_scorpion"));
        assertTrue(scorpionDiagnostic.contains("renderCenterHasDirectMrt"));
        assertTrue(scorpionDiagnostic.contains("selectedAssemblyRecorded"));
        assertFalse(scorpionDiagnostic.contains("new XaeroIcon"));
        String whaleDiagnostic = Files.readString(root.resolve(
                "src/main/java/dev/resivore/naturalistxaeroicons/WhaleCaptureDiagnostic.java"));
        for (String expected : new String[] {"NaturalistXaero WhaleCapture", "naturalist:whale", "source direct-cube count=",
                "selected direct-cube count=", "render center identity=", "direct ModelRenderTrace entry=",
                "adapter trace resolution succeeds=", "fallback rendered destination before=", "cache write result",
                "final manager result", "topJaw"}) assertTrue(whaleDiagnostic.contains(expected), expected);
        assertFalse(whaleDiagnostic.contains("renderedDest.add("));
        assertFalse(whaleDiagnostic.contains("new XaeroIcon"));
        assertFalse(prerenderer.contains("renderedDest.add("));
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
