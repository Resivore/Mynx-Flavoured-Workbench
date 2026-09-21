package dev.resivore.villagerwork;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards C23's exact C27 boundary and user-authored Frog Villager render contract. */
class RibbitsFishermanC27IntegrationContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void vwrRequiresTheRetainedC27ProviderWithoutFallbackOrPayloadCopy() throws IOException {
        String build = read("build.gradle");
        String metadata = read("src/main/resources/fabric.mod.json");
        String layer = read("src/main/java/dev/resivore/villagerwork/client/VwrFishingRodLayer.java");

        assertTrue(build.contains("ribbits-private-reconstruction-4.1.6+26.2-mynx-canary27.jar"));
        assertTrue(build.contains("implementation files(ribbitsC27Artifact)"));
        assertTrue(metadata.contains("\"ribbits\": \"=4.1.6+26.2-mynx-canary27\""));
        assertTrue(metadata.contains("\"relationship\": \"depends\""));
        assertTrue(layer.contains("RibbitsFishermanRodRenderer.submit"));
        assertFalse(layer.contains("Items.STICK"));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/resivore/villagerwork/client/RibbitsFishermanRodProvider.java")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/resivore/villagerwork/FishingRodPose.java")));
    }

    @Test
    void effectiveFoldedArmPathUsesRenderedStructureAndThenExactAuthoredGrip() throws IOException {
        String layer = read("src/main/java/dev/resivore/villagerwork/client/VwrFishingRodLayer.java");
        String path = read("src/main/java/dev/resivore/villagerwork/client/FoldedArmRenderPath.java");
        String pose = read("src/main/java/dev/resivore/villagerwork/FrogVillagerRodPose.java");
        String accessor = read(
                "src/main/java/dev/resivore/villagerwork/mixin/client/ModelPartChildrenAccessor.java");

        assertTrue(path.contains("model.translateToArms(state, poseStack)"));
        assertTrue(path.contains("selection.steps().get(index).node().translateAndRotate(poseStack)"));
        assertTrue(path.contains("return !part.isEmpty()"));
        assertTrue(path.contains("contributing.size() != 1"));
        assertTrue(path.contains("!view.skipDraw(current) && view.hasDirectGeometry(current)"));
        assertTrue(accessor.contains("@Accessor(\"children\")"));
        assertFalse(path.contains("getChild("));
        assertFalse(path.contains("EMF_arms"));
        assertFalse(path.contains("arms_rotation"));
        assertFalse(path.contains("rotationDegrees("));

        String fishingBranch = blockBody(layer, "if (fishing)");
        int effectivePath = fishingBranch.indexOf("FoldedArmRenderPath.apply(");
        int authoredGrip = fishingBranch.indexOf("FrogVillagerRodPose.applyReferenceGrip(poseStack)");
        int rodSubmit = fishingBranch.indexOf("RibbitsFishermanRodRenderer.submit(");
        assertTrue(effectivePath >= 0 && authoredGrip > effectivePath && rodSubmit > authoredGrip);
        assertFalse(fishingBranch.contains("poseStack.translate("));
        assertFalse(fishingBranch.contains("poseStack.mulPose("));
        assertFalse(fishingBranch.contains("poseStack.scale("));

        assertTrue(pose.contains("JEM_ARMS_ROTATION_DEGREES = 43.0F"));
        assertTrue(pose.contains("AUTHORED_GRIP_X_PIXELS = 0.0F"));
        assertTrue(pose.contains("AUTHORED_GRIP_Y_PIXELS = -7.0F"));
        assertTrue(pose.contains("AUTHORED_GRIP_Z_PIXELS = -6.0F"));
        assertMethodBodyEquals("""
                poseStack.translate(AUTHORED_GRIP_X, AUTHORED_GRIP_Y, AUTHORED_GRIP_Z);
                """, pose, "public static void applyReferenceGrip(PoseStack poseStack)");
    }

    @Test
    void c27RodPoseIsDerivedAndPhysicalTipIsTheSoleLineAuthority() throws IOException {
        String renderer = read(
                "src/main/java/dev/resivore/villagerwork/client/RibbitsFishermanRodRenderer.java");
        String layer = read("src/main/java/dev/resivore/villagerwork/client/VwrFishingRodLayer.java");
        String floatRenderer = read(
                "src/main/java/dev/resivore/villagerwork/client/FishingFloatRenderer.java");
        String lineGeometry = read("src/main/java/dev/resivore/villagerwork/FishingLineGeometry.java");
        String pose = read("src/main/java/dev/resivore/villagerwork/FrogVillagerRodPose.java");

        assertTrue(renderer.contains("GeoBone bone = snapshot.getBone()"));
        assertTrue(renderer.contains(
                "snapshot.setRotation(-bone.baseRotX(), -bone.baseRotY(), -bone.baseRotZ())"));
        assertFalse(renderer.contains("rotationDegrees("));
        assertFalse(renderer.contains("com.mojang.math.Axis"));
        assertTrue(renderer.contains("bones.ifPresent(\"fishing_rod_2\""));
        assertTrue(renderer.contains("bones.ifPresent(\"fishing_rod_3\""));
        assertTrue(renderer.contains("snapshot.skipChildrenRender(true)"));
        assertTrue(renderer.contains("physicalOuterTip = transform(poseStack, 0.0F, 0.0F"));
        assertTrue(renderer.contains("FrogVillagerRodPose.OUTER_SHAFT_TIP_FROM_GRIP_Z"));
        assertTrue(pose.contains("OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS = -9.5F"));

        int rodSubmit = layer.indexOf("RibbitsFishermanRodRenderer.submit(");
        int physicalTip = layer.indexOf("inspection.physicalOuterTip()", rodSubmit);
        int lineSubmit = layer.indexOf("FishingFloatRenderer.submitLineFromPhysicalRod(", rodSubmit);
        assertTrue(rodSubmit >= 0 && physicalTip > rodSubmit && lineSubmit > physicalTip);
        assertTrue(layer.contains("inspection.exactLiveCapture()"));
        assertFalse(layer.contains("ribbitsRodTip"));
        assertFalse(layer.contains("Mth.rotLerp"));
        assertFalse(pose.contains("outerShaftTip("));

        assertTrue(floatRenderer.contains("segmentsFromRodTip("));
        assertTrue(floatRenderer.contains("PoseStack linePose = new PoseStack()"));
        assertTrue(floatRenderer.contains("physicalTipRender.add(cameraWorld)"));
        assertFalse(floatRenderer.contains("state.line"));
        assertTrue(lineGeometry.contains("public static List<Segment> segmentsFromRodTip"));
        assertTrue(lineGeometry.contains("for (int index = floatToRod.size() - 1; index >= 0; index--)"));
        assertTrue(lineGeometry.contains("SEGMENT_COUNT = 16"));
    }

    @Test
    void c23DiagnosticsExposeEffectivePathAuthoredGripAndSharedPhysicalTip() throws IOException {
        String diagnostics = read(
                "src/main/java/dev/resivore/villagerwork/client/VwrRodDiagnostics.java");
        String markers = read("src/main/java/dev/resivore/villagerwork/client/RodDiagnosticMarkers.java");
        String renderer = read(
                "src/main/java/dev/resivore/villagerwork/client/RibbitsFishermanRodRenderer.java");
        String presentation = read(
                "src/main/java/dev/resivore/villagerwork/client/VwrFishingRodPresentation.java");
        String stateMixin = read(
                "src/main/java/dev/resivore/villagerwork/mixin/client/VillagerRenderStateMixin.java");
        String rendererMixin = read(
                "src/main/java/dev/resivore/villagerwork/mixin/client/VillagerRendererMixin.java");

        assertTrue(renderer.contains("C27_MODEL_SHA256"));
        assertTrue(renderer.contains("sourcePackId()"));
        assertTrue(renderer.contains("addPerBoneRender"));
        assertTrue(renderer.contains("capturePhysicalRod"));
        assertTrue(renderer.contains("live_render_snapshot"));
        assertTrue(diagnostics.contains("effective_folded_arm_path_actually_used"));
        assertTrue(diagnostics.contains("after_effective_folded_arm_path"));
        assertTrue(diagnostics.contains("after_C23_authored_grip_[0,-7,-6]px"));
        assertTrue(diagnostics.contains("C23_line_start_authority=live_fishing_rod_physical_outer_tip"));
        assertTrue(diagnostics.contains("C23_line_start_minus_visible_tip"));
        assertFalse(diagnostics.contains("C20_line_start_currently_used"));
        assertTrue(markers.contains("Shape.SQUARE"));
        assertTrue(markers.contains("Shape.PLUS"));
        assertTrue(markers.contains("Shape.X"));
        assertTrue(markers.contains("Shape.DIAMOND"));
        assertTrue(markers.contains("Shape.STAR"));
        assertTrue(presentation.contains("villagerWork$partialTick()"));
        assertTrue(stateMixin.contains("villagerWork$partialTick"));
        assertTrue(rendererMixin.contains("villagerWork$setPartialTick(partialTick)"));
    }

    @Test
    void vwrNeverPackagesRibbitsResourcesAndNonvisualBehaviorRemainsUnchanged()
            throws IOException, NoSuchAlgorithmException {
        String build = read("build.gradle");
        assertTrue(build.contains("verifyNoRibbitsPayload"));
        try (var paths = Files.walk(ROOT.resolve("src/main/resources"))) {
            assertTrue(paths.noneMatch(path -> path.toString().replace('\\', '/')
                    .contains("assets/ribbits/") || path.toString().replace('\\', '/')
                    .contains("data/ribbits/")));
        }

        assertFilesRemainAtC19Content(List.of(
                "src/main/java/dev/resivore/villagerwork/WorkCoordinator.java",
                "src/main/java/dev/resivore/villagerwork/FishingRodLifecycle.java",
                "src/main/java/dev/resivore/villagerwork/FishingFloat.java",
                "src/main/java/dev/resivore/villagerwork/ShearActionRules.java",
                "src/main/java/dev/resivore/villagerwork/ShearCapture.java",
                "src/main/java/dev/resivore/villagerwork/ShearingToolMarker.java",
                "src/main/java/dev/resivore/villagerwork/client/ShearingToolMarkerRenderer.java"),
                List.of(
                        "42428dd51ee7770a5c57be5d84060a0c91e20092195396f15079bc4597246eed",
                        "27aea97287d0ba025ae1b8dce8271ddff8f0f56d4253784d29463e863982a67d",
                        "47988ce2cfdd1f5a06f1126b6702faf95e7fcd2e457f02ba8653db52e37bb5a2",
                        "bb399d10530a55bad04d6e7a9d937ccfccaf8f2c9d5d3593dbafadb50f0c418f",
                        "ce47d98c61e56f0c451c04d2b18e60d8f698067c895c552b334cdfc95a946e19",
                        "77fc35f08ef060aa9e98b75015176cd3578e906f2f55dc305c2bfdeb46d766af",
                        "3dda30dad8eaf6e3f3e714f4d252d670d5b907ec33ced44266e5a8c1e6083f1d"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    private static void assertMethodBodyEquals(String expectedBody, String source, String declaration) {
        assertEquals(normalizeJava(expectedBody), normalizeJava(blockBody(source, declaration)), declaration);
    }

    private static String blockBody(String source, String anchor) {
        int anchorIndex = source.indexOf(anchor);
        assertTrue(anchorIndex >= 0, "Missing source anchor: " + anchor);
        int openingBrace = source.indexOf('{', anchorIndex + anchor.length());
        assertTrue(openingBrace >= 0, "Missing opening brace after: " + anchor);
        int depth = 1;
        for (int index = openingBrace + 1; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') depth++;
            if (current == '}' && --depth == 0) return source.substring(openingBrace + 1, index);
        }
        throw new AssertionError("Unclosed source block after: " + anchor);
    }

    private static String normalizeJava(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("(?m)//.*$", "")
                .replaceAll("\\s+", "");
    }

    private static void assertFilesRemainAtC19Content(List<String> files, List<String> expectedHashes)
            throws IOException, NoSuchAlgorithmException {
        assertEquals(files.size(), expectedHashes.size());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int index = 0; index < files.size(); index++) {
            byte[] hash = digest.digest(Files.readAllBytes(ROOT.resolve(files.get(index))));
            assertEquals(expectedHashes.get(index), HexFormat.of().formatHex(hash), files.get(index));
        }
    }
}
