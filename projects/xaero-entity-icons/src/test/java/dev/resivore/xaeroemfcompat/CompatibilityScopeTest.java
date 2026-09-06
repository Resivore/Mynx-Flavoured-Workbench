package dev.resivore.xaeroemfcompat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityScopeTest {
    private static final Path PROJECT = Path.of(System.getProperty("projectRoot"));

    @Test
    void metadataIsClientOnlyAndDeclaresNoEntrypointOrNetworking() throws Exception {
        String metadata = Files.readString(PROJECT.resolve("src/main/resources/fabric.mod.json"));
        assertTrue(metadata.contains("\"environment\": \"client\""));
        assertFalse(metadata.contains("\"entrypoints\""));
        assertFalse(metadata.contains("\"server\""));
        assertFalse(metadata.contains("network"));
        assertTrue(metadata.contains("\"xaerominimap\": \"*\""));
        assertTrue(metadata.contains("\"entity_model_features\": \"*\""));
        assertTrue(metadata.contains(
                "\"workbench:classification\": \"GENERATED / CONTROLLED STATIC PASS / RUNTIME UNTESTED\""));
        assertTrue(metadata.contains("\"workbench:deployment\": \"NOT DEPLOYED\""));
    }

    @Test
    void runtimeSourcesContainNoServerNetworkOrAssetReplacementCode() throws Exception {
        String source = allJava(PROJECT.resolve("src/main/java"));
        String lower = source.toLowerCase();
        assertFalse(source.contains("net.minecraft.network"));
        assertFalse(source.contains("net.minecraft.server"));
        assertFalse(lower.contains("clientplaynetwork"));
        assertFalse(lower.contains("serverplaynetwork"));
        assertFalse(lower.contains("resourcepack"));
        assertFalse(lower.contains("texturemanager"));
        assertFalse(lower.contains("animationhandler"));
    }

    @Test
    void requiredMixinsCoverDetectionSelectionAndObservedEmfCacheReload() throws Exception {
        String emfMixin = Files.readString(PROJECT.resolve(
                "src/main/java/dev/resivore/xaeroemfcompat/mixin/EMFModelPartMixin.java"));
        String xaeroMixin = Files.readString(PROJECT.resolve(
                "src/main/java/dev/resivore/xaeroemfcompat/mixin/"
                        + "RadarIconModelPrerendererMixin.java"));
        String xaeroPartMixin = Files.readString(PROJECT.resolve(
                "src/main/java/dev/resivore/xaeroemfcompat/mixin/"
                        + "RadarIconModelPartPrerendererMixin.java"));
        String cacheMixin = Files.readString(PROJECT.resolve(
                "src/main/java/dev/resivore/xaeroemfcompat/mixin/"
                        + "RadarIconEntityCacheMixin.java"));
        String managerMixin = Files.readString(PROJECT.resolve(
                "src/main/java/dev/resivore/xaeroemfcompat/mixin/"
                        + "RadarIconManagerMixin.java"));
        String config = Files.readString(PROJECT.resolve(
                "src/main/resources/xaero_emf_entity_icon_compat.mixins.json"));

        assertTrue(emfMixin.contains(
                "@Mixin(targets = \"traben.entity_model_features.models.parts.EMFModelPart\", remap = false)"));
        assertTrue(emfMixin.contains(
                "compile(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"));
        assertTrue(emfMixin.contains("at = @At(\"HEAD\")"));
        assertTrue(emfMixin.contains("require = 1"));
        assertFalse(emfMixin.contains("net.minecraft.client.model.geom.ModelPart\", remap"));
        assertFalse(emfMixin.contains("cancellable = true"));
        assertFalse(emfMixin.contains("callbackInfo.cancel"));
        assertEquals(1, occurrences(emfMixin, "ModelPartDetectionBridge.forwardToXaero"));

        assertTrue(xaeroMixin.contains("@Mixin(value = RadarIconModelPrerenderer.class, remap = false)"));
        assertTrue(xaeroMixin.contains("method = \"renderModel\""));
        assertTrue(xaeroMixin.contains("at = @At(\"RETURN\")"));
        assertTrue(xaeroMixin.contains("if (!parameters.renderedDest.isEmpty())"));
        assertTrue(xaeroMixin.contains("callbackInfo.getReturnValue()"));
        assertTrue(xaeroMixin.contains("parameters.config.modelPartsRotationReset"));
        assertTrue(xaeroMixin.contains("getPartPrerenderer().renderPart"));
        assertFalse(xaeroMixin.contains("callbackInfo.cancel"));

        assertTrue(xaeroPartMixin.contains(
                "@Mixin(value = RadarIconModelPartPrerenderer.class, remap = false)"));
        assertTrue(xaeroPartMixin.contains("method = \"renderPart\""));
        assertTrue(xaeroPartMixin.contains("ModelRenderTrace;getModelPartRenderInfo"));
        assertTrue(xaeroPartMixin.contains("ModelPart;render("));
        assertTrue(xaeroPartMixin.contains("remap = true"));
        assertTrue(xaeroPartMixin.contains("EmfIconPartResolver.renderAdapter"));
        assertTrue(xaeroMixin.contains("EmfIconPartResolver.isSupportedEmfRoot"));
        assertTrue(cacheMixin.contains("retryFailedOnce"));
        assertTrue(managerMixin.contains("LivingEntityRenderer"));
        assertTrue(managerMixin.contains("EmfIconPartResolver.isSupportedEmfRoot"));
        assertFalse(xaeroPartMixin.contains("@Inject"));
        assertTrue(config.contains("\"required\": true"));
        assertTrue(config.contains("\"defaultRequire\": 1"));
        assertEquals(1, occurrences(config, "EMFModelPartMixin"));
        assertEquals(1, occurrences(config, "RadarIconModelPrerendererMixin"));
        assertEquals(1, occurrences(config, "RadarIconModelPartPrerendererMixin"));
        assertEquals(1, occurrences(config, "RadarIconEntityCacheMixin"));
        assertEquals(1, occurrences(config, "RadarIconManagerMixin"));
    }

    @Test
    void pluginGatesOnBothModsAndTheBridgeUsesXaerosExistingCallback() throws Exception {
        String plugin = Files.readString(PROJECT.resolve(
                "src/main/java/dev/resivore/xaeroemfcompat/mixin/XaeroEmfCompatMixinPlugin.java"));
        String bridge = Files.readString(PROJECT.resolve(
                "src/main/java/dev/resivore/xaeroemfcompat/ModelPartDetectionBridge.java"));

        assertTrue(plugin.contains("loader.getModContainer(XAERO_MOD_ID)"));
        assertTrue(plugin.contains("loader.getModContainer(EMF_MOD_ID)"));
        assertTrue(plugin.contains("CompatibilityActivation.shouldApply(xaeroVersion, emfVersion)"));
        assertTrue(plugin.contains("XAERO_PRERENDER_TARGET"));
        assertTrue(plugin.contains("XAERO_PART_PRERENDER_TARGET"));
        assertTrue(bridge.contains("XaeroMinimapCore::onEntityIconsModelPartRenderDetection"));
        assertFalse(bridge.contains("EntityRenderTracer"));
        assertFalse(bridge.contains("RadarIconManager"));
    }

    private static String allJava(Path root) throws Exception {
        StringBuilder result = new StringBuilder();
        try (Stream<Path> files = Files.walk(root)) {
            List<Path> javaFiles = files.filter(path -> path.toString().endsWith(".java")).sorted().toList();
            for (Path file : javaFiles) {
                result.append(Files.readString(file)).append('\n');
            }
        }
        return result.toString();
    }

    private static int occurrences(String text, String needle) {
        return (text.length() - text.replace(needle, "").length()) / needle.length();
    }
}
