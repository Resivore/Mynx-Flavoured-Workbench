package dev.resivore.villagerwork;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards VWR's optional-only ownership boundary for the Ribbits Fisherman visual. */
class RibbitsFishermanRodIntegrationContractTest {
    @Test
    void vwrUsesTheProviderTipForTheLiveLineAndKeepsTheStickOnlyAsFallback() throws IOException {
        Path root = Path.of(System.getProperty("user.dir"));
        String layer = Files.readString(root.resolve(
                "src/main/java/dev/resivore/villagerwork/client/VwrFishingRodLayer.java"));
        String floatRenderer = Files.readString(root.resolve(
                "src/main/java/dev/resivore/villagerwork/client/FishingFloatRenderer.java"));
        String provider = Files.readString(root.resolve(
                "src/main/java/dev/resivore/villagerwork/client/RibbitsFishermanRodProvider.java"));

        assertTrue(layer.contains("RibbitsFishermanRodProvider.submit"));
        assertTrue(layer.contains("RIBBITS_ROD_TIPS"));
        assertTrue(floatRenderer.contains("VwrFishingRodLayer.ribbitsRodTip"));
        assertTrue(floatRenderer.contains("!VwrFishingRodLayer.ribbitsRodProviderAvailable()"));
        assertTrue(floatRenderer.contains("FishingRodPose.tip"),
                "the established C17 endpoint remains only for the unavailable-provider fallback");
        assertTrue(provider.contains("Class.forName(BRIDGE_CLASS"));
        assertFalse(provider.contains("import com.yungnickyoung.minecraft.ribbits"),
                "VWR must not acquire a compile-time Ribbits dependency or its owned assets");
    }
}
