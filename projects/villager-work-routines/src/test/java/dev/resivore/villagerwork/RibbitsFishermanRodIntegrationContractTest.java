package dev.resivore.villagerwork;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards VWR's optional-only ownership boundary and explicit line-coordinate contract. */
class RibbitsFishermanRodIntegrationContractTest {
    @Test
    void vwrUsesTheProviderArmLocalTipForTheLiveLineAndKeepsTheStickOnlyAsFallback() throws IOException {
        Path root = Path.of(System.getProperty("user.dir"));
        String layer = Files.readString(root.resolve(
                "src/main/java/dev/resivore/villagerwork/client/VwrFishingRodLayer.java"));
        String floatRenderer = Files.readString(root.resolve(
                "src/main/java/dev/resivore/villagerwork/client/FishingFloatRenderer.java"));
        String provider = Files.readString(root.resolve(
                "src/main/java/dev/resivore/villagerwork/client/RibbitsFishermanRodProvider.java"));
        String pose = Files.readString(root.resolve(
                "src/main/java/dev/resivore/villagerwork/FishingRodPose.java"));

        assertTrue(layer.contains("RibbitsFishermanRodProvider.submit"));
        assertTrue(layer.contains("RibbitsFishermanRodProvider.armLocalTip"));
        assertTrue(floatRenderer.contains("VwrFishingRodLayer.ribbitsRodTip(villager, partialTick)"));
        assertTrue(floatRenderer.contains("!VwrFishingRodLayer.ribbitsRodProviderAvailable()"));
        assertTrue(floatRenderer.contains("FishingRodPose.tip"),
                "the established C17 endpoint remains only for the unavailable-provider fallback");
        assertTrue(pose.contains("tipFromCrossedArms"));
        assertTrue(pose.contains("CROSSED_ARMS_X_ROTATION_RADIANS"));
        assertTrue(provider.contains("record ArmLocalRodTip"));
        assertTrue(provider.contains("armLocalTipMethod"));
        assertTrue(provider.contains("Class.forName(BRIDGE_CLASS"));
        assertFalse(layer.contains("cameraRenderState"),
                "the active provider path must not manufacture a world endpoint from camera state");
        assertFalse(provider.contains("Vec3"),
                "the bridge boundary must use an explicit coordinate-space type, not an implied vector");
        assertFalse(provider.contains("import com.yungnickyoung.minecraft.ribbits"),
                "VWR must not acquire a compile-time Ribbits dependency or its owned assets");
    }

    @Test
    void ribbitsRodWorkCannotChangeShepherdPresentation() throws IOException {
        Path root = Path.of(System.getProperty("user.dir"));
        String layer = Files.readString(root.resolve(
                "src/main/java/dev/resivore/villagerwork/client/VwrFishingRodLayer.java"));
        int shearsStart = layer.indexOf("private void submitShears");
        int shearsEnd = layer.indexOf("private static boolean hasLiveFloat", shearsStart);
        String shears = layer.substring(shearsStart, shearsEnd);

        assertTrue(layer.contains("hasLiveShears"));
        assertTrue(shears.contains("Items.SHEARS"));
        assertFalse(shears.contains("RibbitsFishermanRodProvider"),
                "the optional Fisherman visual bridge must not affect Shepherd rendering");
    }
}
