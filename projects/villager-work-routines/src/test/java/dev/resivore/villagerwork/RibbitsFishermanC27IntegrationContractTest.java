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
        assertTrue(layer.contains("FishingFloatRenderer.submitLineFromSharedRodPose"));
        assertTrue(floatRenderer.contains("FrogVillagerCemRodPose.outerShaftTipInRenderSpace(rodPose)"));
        assertFalse(layer.contains("Items.STICK"));
        assertFalse(floatRenderer.contains("FishingRodPose"));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/resivore/villagerwork/client/RibbitsFishermanRodProvider.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/resivore/villagerwork/FishingRodPose.java")));
    }

    @Test
    void rawRibbitsRodUsesTheLiveCemFoldedArmChildWithoutAnIndependentRotation() throws IOException {
        String layer = read("src/main/java/dev/resivore/villagerwork/client/VwrFishingRodLayer.java");
        String pose = read("src/main/java/dev/resivore/villagerwork/client/FrogVillagerCemRodPose.java");
        String renderer = read("src/main/java/dev/resivore/villagerwork/client/RibbitsFishermanRodRenderer.java");
        String accessor = read("src/main/java/dev/resivore/villagerwork/mixin/client/VillagerModelArmsAccessor.java");

        assertTrue(layer.contains("FrogVillagerCemRodPose.apply(getParentModel(), state, poseStack)"));
        assertTrue(pose.contains("FOLDED_ARMS_CHILD = \"arms_rotation\""));
        assertTrue(pose.contains("applyFoldedArmsAndReferenceGrip(arms.getChild(FOLDED_ARMS_CHILD), poseStack)"));
        assertTrue(pose.contains("foldedArms.translateAndRotate(poseStack)"));
        assertTrue(accessor.contains("@Accessor(\"arms\")"));
        assertTrue(pose.contains("AUTHORED_GRIP_Y_PIXELS = -7.0F"));
        assertTrue(pose.contains("AUTHORED_GRIP_Z_PIXELS = -6.0F"));
        assertFalse(pose.contains("JEM_ARMS_ROTATION_DEGREES"));
        assertFalse(pose.contains("rotationDegrees("));
        assertTrue(renderer.contains("snapshot.setRotation(0.0F, 0.0F, 0.0F)"));
        assertTrue(renderer.contains("getBone(\"fishing_rod\")"));
        assertFalse(renderer.contains("rotationDegrees("));
        assertFalse(renderer.contains("mulPose("));
    }

    @Test
    void sourceDecorationsAreSuppressedAndTheLineUsesTheSamePhysicalOuterTip() throws IOException {
        String pose = read("src/main/java/dev/resivore/villagerwork/client/FrogVillagerCemRodPose.java");
        String renderer = read("src/main/java/dev/resivore/villagerwork/client/RibbitsFishermanRodRenderer.java");
        String layer = read("src/main/java/dev/resivore/villagerwork/client/VwrFishingRodLayer.java");
        String floatRenderer = read("src/main/java/dev/resivore/villagerwork/client/FishingFloatRenderer.java");

        assertTrue(renderer.contains("bones.ifPresent(\"fishing_rod_2\""));
        assertTrue(renderer.contains("bones.ifPresent(\"fishing_rod_3\""));
        assertTrue(renderer.contains("snapshot.skipChildrenRender(true)"));
        assertTrue(pose.contains("OUTER_SHAFT_TIP_FROM_GRIP_Z_PIXELS = -9.5F"));
        assertTrue(layer.contains("FishingFloatRenderer.submitLineFromSharedRodPose"));
        assertTrue(floatRenderer.contains("FrogVillagerCemRodPose.outerShaftTipInRenderSpace(rodPose)"));
        assertTrue(floatRenderer.contains("FishingLineGeometry.segmentsBetween"));
        assertTrue(floatRenderer.contains("cameraRenderState.pos"));
        assertFalse(layer.contains("Mth.rotLerp"));
        assertFalse(floatRenderer.contains("ribbitsRodTip"));
        assertFalse(floatRenderer.contains("FishingRodPose.tip"));
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
