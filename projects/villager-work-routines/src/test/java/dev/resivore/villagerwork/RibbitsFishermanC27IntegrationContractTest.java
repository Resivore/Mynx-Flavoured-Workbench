package dev.resivore.villagerwork;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

/** Guards the intentional C27 boundary and the user-authored Frog Villager rendering contract. */
class RibbitsFishermanC27IntegrationContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    /** Exact bytes of FishingLineGeometry.java at C20 source checkpoint 6d13248d. */
    private static final String C20_FISHING_LINE_GEOMETRY_SHA256 =
            "e59b46514dafaa0a159492a13fb1dc7aff64afdc8d9053b29060e27e74a32e6f";

    @Test
    void vwrRequiresRetainedC27DirectlyAndRetiresTheOptionalReflectionFallback() throws IOException {
        String build = read("build.gradle");
        String metadata = read("src/main/resources/fabric.mod.json");
        String layer = read("src/main/java/dev/resivore/villagerwork/client/VwrFishingRodLayer.java");
        String floatRenderer = read("src/main/java/dev/resivore/villagerwork/client/FishingFloatRenderer.java");

        assertTrue(build.contains("ribbits-private-reconstruction-4.1.6+26.2-mynx-canary27.jar"));
        assertTrue(build.contains("implementation files(ribbitsC27Artifact)"));
        assertTrue(metadata.contains("\"ribbits\": \"=4.1.6+26.2-mynx-canary27\""));
        assertTrue(metadata.contains("\"relationship\": \"depends\""));
        assertTrue(layer.contains("RibbitsFishermanRodRenderer.submit"));
        assertTrue(floatRenderer.contains("VwrFishingRodLayer.ribbitsRodTip(villager, partialTick)"));
        assertFalse(layer.contains("Items.STICK"));
        assertFalse(floatRenderer.contains("FishingRodPose"));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/resivore/villagerwork/client/RibbitsFishermanRodProvider.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/resivore/villagerwork/FishingRodPose.java")));
    }

    @Test
    void rawRibbitsRodRestoresTheC20TranslateToArmsAndAuthoredGripPath() throws IOException {
        String layer = read("src/main/java/dev/resivore/villagerwork/client/VwrFishingRodLayer.java");
        String pose = read("src/main/java/dev/resivore/villagerwork/FrogVillagerRodPose.java");
        String renderer = read("src/main/java/dev/resivore/villagerwork/client/RibbitsFishermanRodRenderer.java");

        int translateToArms = layer.indexOf("getParentModel().translateToArms(state, poseStack)");
        int authoredGrip = layer.indexOf("FrogVillagerRodPose.applyReferenceGrip(poseStack)");
        int rodSubmit = layer.indexOf("RibbitsFishermanRodRenderer.submit(poseStack, collector, light)");
        assertTrue(translateToArms >= 0 && authoredGrip > translateToArms && rodSubmit > authoredGrip,
                "C20's actual arm -> authored grip -> rod submission order must remain intact");
        assertTrue(pose.contains("AUTHORED_GRIP_Y_PIXELS = -7.0F"));
        assertTrue(pose.contains("AUTHORED_GRIP_Z_PIXELS = -6.0F"));
        assertTrue(pose.contains("JEM_ARMS_ROTATION_DEGREES = 43.0F"));
        assertTrue(renderer.contains("snapshot.setRotation(0.0F, 0.0F, 0.0F)"));
        assertTrue(renderer.contains("getBone(\"fishing_rod\")"));
        assertFalse(renderer.contains("rotationDegrees("));
        assertFalse(renderer.contains("com.mojang.math.Axis"));
        assertFalse(layer.contains("arms_rotation"));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/resivore/villagerwork/client/FrogVillagerCemRodPose.java")));
    }

    @Test
    void restoredC20TransformBodiesRemainExactAroundTheDiagnosticProbes()
            throws IOException, NoSuchAlgorithmException {
        String pose = read("src/main/java/dev/resivore/villagerwork/FrogVillagerRodPose.java");
        String renderer = read("src/main/java/dev/resivore/villagerwork/client/RibbitsFishermanRodRenderer.java");
        String layer = read("src/main/java/dev/resivore/villagerwork/client/VwrFishingRodLayer.java");

        assertMethodBodyEquals("""
                poseStack.translate(AUTHORED_GRIP_X, AUTHORED_GRIP_Y, AUTHORED_GRIP_Z);
                """, pose, "public static void applyReferenceGrip(PoseStack poseStack)");
        assertMethodBodyEquals("""
                GeoBone rod = getGeoModel().getBakedModel(MODEL).getBone("fishing_rod")
                        .orElseThrow(() -> new IllegalStateException("Ribbits C27 Fisherman rod is missing"));
                PoseStack modelPose = new PoseStack();
                applyRibbitsGeometryRoot(modelPose);
                RenderUtil.transformToBone(modelPose, rod);
                Vec3 grip = transform(modelPose, 0.0F, 0.0F, 0.0F);
                return new StandaloneRodBasis(grip);
                """, renderer, "private StandaloneRodBasis standaloneBasis()");
        assertMethodBodyEquals("""
                poseStack.translate(0.5F, 0.51F, 0.5F);
                """, renderer, "private static void applyRibbitsGeometryRoot(PoseStack poseStack)");
        assertMethodBodyEquals("""
                poseStack.translate(-rawGripPosition.x, -rawGripPosition.y, -rawGripPosition.z);
                """, renderer, "private void attachGripAtOrigin(PoseStack poseStack)");
        assertMethodBodyEquals("""
                bones.ifPresent("main", snapshot -> snapshot.skipRender(true));
                bones.ifPresent("body", snapshot -> snapshot.skipRender(true));
                bones.ifPresent("left_arm", snapshot -> snapshot.skipRender(true));
                bones.ifPresent("right_arm", snapshot -> snapshot.skipRender(true));
                bones.ifPresent("right_leg", snapshot -> snapshot.skipRender(true));
                bones.ifPresent("left_leg", snapshot -> snapshot.skipRender(true));
                bones.ifPresent("fishing_rod", snapshot -> {
                    snapshot.setRotation(0.0F, 0.0F, 0.0F);
                });
                bones.ifPresent("fishing_rod_2", snapshot -> {
                    snapshot.skipRender(true);
                    snapshot.skipChildrenRender(true);
                });
                bones.ifPresent("fishing_rod_3", snapshot -> snapshot.skipRender(true));
                """, renderer, "private static void applyC20BoneAdjustments(BoneSnapshots bones)");

        String fishingBranch = blockBody(layer, "if (fishing)");
        assertEquals(1, occurrences(fishingBranch,
                "getParentModel().translateToArms(state, poseStack)"));
        assertEquals(1, occurrences(fishingBranch,
                "FrogVillagerRodPose.applyReferenceGrip(poseStack)"));
        assertEquals(1, occurrences(fishingBranch,
                "RibbitsFishermanRodRenderer.submit(poseStack, collector, light)"));
        assertTrue(fishingBranch.indexOf("getParentModel().translateToArms(state, poseStack)")
                < fishingBranch.indexOf("FrogVillagerRodPose.applyReferenceGrip(poseStack)"));
        assertTrue(fishingBranch.indexOf("FrogVillagerRodPose.applyReferenceGrip(poseStack)")
                < fishingBranch.indexOf("RibbitsFishermanRodRenderer.submit(poseStack, collector, light)"));
        assertFalse(fishingBranch.contains("poseStack.translate("),
                "C22 must not add a direct translation around C20's pose calls");
        assertFalse(fishingBranch.contains("poseStack.mulPose("),
                "C22 must not add a direct rotation around C20's pose calls");
        assertFalse(fishingBranch.contains("poseStack.scale("),
                "C22 must not add a direct scale around C20's pose calls");

        assertEquals(C20_FISHING_LINE_GEOMETRY_SHA256,
                sha256("src/main/java/dev/resivore/villagerwork/FishingLineGeometry.java"),
                "C20's complete 16-segment line implementation must remain byte-identical");
    }

    @Test
    void restoredC20AnalyticalLineStartBodyRemainsExact() throws IOException {
        String layer = read("src/main/java/dev/resivore/villagerwork/client/VwrFishingRodLayer.java");
        assertMethodBodyEquals("""
                Vec3 position = villager.getPosition(partialTick);
                float bodyYaw = Mth.rotLerp(partialTick, villager.yBodyRotO, villager.yBodyRot);
                FrogVillagerRodPose.Point worldTip = FrogVillagerRodPose.outerShaftTip(position.x, position.y,
                        position.z, bodyYaw);
                return worldTip.isFinite() ? new Vec3(worldTip.x(), worldTip.y(), worldTip.z()) : null;
                """, layer, "static Vec3 ribbitsRodTip(Villager villager, float partialTick)");
    }

    @Test
    void sourceDecorationsAreSuppressedAndTheLineRestoresC20WithoutUsingTheMeasuredTip() throws IOException {
        String pose = read("src/main/java/dev/resivore/villagerwork/FrogVillagerRodPose.java");
        String renderer = read("src/main/java/dev/resivore/villagerwork/client/RibbitsFishermanRodRenderer.java");
        String layer = read("src/main/java/dev/resivore/villagerwork/client/VwrFishingRodLayer.java");
        String floatRenderer = read("src/main/java/dev/resivore/villagerwork/client/FishingFloatRenderer.java");
        String lineGeometry = read("src/main/java/dev/resivore/villagerwork/FishingLineGeometry.java");

        assertTrue(renderer.contains("bones.ifPresent(\"fishing_rod_2\""));
        assertTrue(renderer.contains("bones.ifPresent(\"fishing_rod_3\""));
        assertTrue(renderer.contains("snapshot.skipChildrenRender(true)"));
        assertTrue(pose.contains("OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS = -9.5F"));
        assertTrue(pose.contains("outerShaftTipFromFoldedArms"));
        assertTrue(layer.contains("FrogVillagerRodPose.outerShaftTip"));
        assertTrue(layer.contains("Mth.rotLerp"));
        assertTrue(floatRenderer.contains("state.line = ribbitsTip.subtract(entity.getPosition(partialTick))"));
        assertTrue(floatRenderer.contains("FishingLineGeometry.segments("));
        assertFalse(floatRenderer.contains("physicalOuterTip"));
        assertFalse(floatRenderer.contains("cameraRenderState"));
        assertFalse(lineGeometry.contains("segmentsBetween"));
        assertFalse(floatRenderer.contains("FishingRodPose.tip"));
    }

    @Test
    void diagnosticProbesObserveTheLivePathWithoutBecomingPoseOrLineAuthority() throws IOException {
        String layer = read("src/main/java/dev/resivore/villagerwork/client/VwrFishingRodLayer.java");
        String renderer = read("src/main/java/dev/resivore/villagerwork/client/RibbitsFishermanRodRenderer.java");
        String diagnostics = read("src/main/java/dev/resivore/villagerwork/client/VwrRodDiagnostics.java");
        String markers = read("src/main/java/dev/resivore/villagerwork/client/RodDiagnosticMarkers.java");
        String floatRenderer = read("src/main/java/dev/resivore/villagerwork/client/FishingFloatRenderer.java");
        String armsAccessor = read(
                "src/main/java/dev/resivore/villagerwork/mixin/client/VillagerModelArmsAccessor.java");
        String childrenAccessor = read(
                "src/main/java/dev/resivore/villagerwork/mixin/client/ModelPartChildrenAccessor.java");
        String mixins = read("src/main/resources/villager_work_routines.mixins.json");

        assertTrue(renderer.contains("C27_MODEL_SHA256"));
        assertTrue(renderer.contains("sourcePackId()"));
        assertTrue(renderer.contains("addPerBoneRender"));
        assertTrue(renderer.contains("capturePhysicalRod"));
        assertTrue(renderer.contains("live_render_snapshot"));
        assertTrue(diagnostics.contains("arms_direct_children_exact_names"));
        assertTrue(diagnostics.contains("arms_rotation_direct_child_exists"));
        assertTrue(diagnostics.contains("MAX_HIERARCHY_DEPTH"));
        assertTrue(diagnostics.contains("REPORTED_CASTS"));
        assertTrue(diagnostics.contains("C20_line_start_currently_used_world"));
        assertTrue(diagnostics.contains("diagnostic_only_line_start_minus_visible_tip"));
        assertTrue(markers.contains("Shape.SQUARE"));
        assertTrue(markers.contains("Shape.X"));
        assertTrue(markers.contains("Shape.DIAMOND"));
        assertTrue(markers.contains("Shape.STAR"));
        assertTrue(layer.contains("RodDiagnosticMarkers.submit"));
        assertTrue(floatRenderer.contains("VwrRodDiagnostics.observeLinePath"));
        assertTrue(armsAccessor.contains("@Accessor(\"arms\")"));
        assertTrue(childrenAccessor.contains("@Accessor(\"children\")"));
        assertTrue(mixins.contains("\"client.VillagerModelArmsAccessor\""));
        assertTrue(mixins.contains("\"client.ModelPartChildrenAccessor\""));
        assertFalse(layer.contains("villagerWork$getArms"));
        assertFalse(layer.contains("getChild(\"arms_rotation\")"));
    }

    @Test
    void vwrNeverPackagesRibbitsOwnedResourcesAndVisualOnlyChangeLeavesBehaviorUnchanged()
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

    /** Extracts a brace-balanced block; all asserted C20 blocks contain no brace-bearing literals. */
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

    private static int occurrences(String source, String target) {
        int count = 0;
        for (int index = 0; (index = source.indexOf(target, index)) >= 0; index += target.length()) count++;
        return count;
    }

    private static String sha256(String relative) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(ROOT.resolve(relative))));
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
