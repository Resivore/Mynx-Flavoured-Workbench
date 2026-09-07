package tempeststudios.quickstacknearby;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Fast guardrails for the integrated C10 shape; runtime behavior remains GameTest work. */
class C10StaticContractTest {
    @Test
    void nearbySearchIsLiveAndHasNoHistoricalOrInventorySearchRuntimeBridge() throws Exception {
        String main = Files.walk(Path.of("src/main")).filter(path -> path.toString().endsWith(".java"))
                .map(path -> {
                    try { return Files.readString(path); } catch (Exception e) { throw new RuntimeException(e); }
                }).reduce("", String::concat);
        String client = Files.walk(Path.of("src/client")).filter(path -> path.toString().endsWith(".java"))
                .map(path -> {
                    try { return Files.readString(path); } catch (Exception e) { throw new RuntimeException(e); }
                }).reduce("", String::concat);
        String all = (main + client).toLowerCase();
        assertTrue(all.contains("nearbysearchservice"));
        assertTrue(all.contains("validtarget"));
        assertTrue(all.contains("itemcontainercontents"));
        assertFalse(all.contains("itemlocationtracker"));
        assertFalse(all.contains("inventoryscreenbuttonslots"));
        assertFalse(all.contains("inventorysort_core"));
    }

    @Test
    void packagedMetadataOwnsTheUpstreamQsnIdentityWithoutSidecarDependency() throws Exception {
        String metadata = Files.readString(Path.of("src/main/resources/fabric.mod.json"));
        assertTrue(metadata.contains("\"id\": \"quick-stack-nearby\""));
        assertFalse(metadata.contains("quick-stack-nearby-compat"));
        assertFalse(metadata.contains("inventorysearch"));
        assertFalse(metadata.contains("inventorysort_core"));
    }

    @Test
    void searchProtocolIsBoundedAndRevalidatesTheSelectedLiveStack() throws Exception {
        String payload = Files.readString(Path.of("src/main/java/tempeststudios/quickstacknearby/NearbySearchPayload.java"));
        String service = Files.readString(Path.of("src/main/java/tempeststudios/quickstacknearby/NearbySearchService.java"));
        assertTrue(payload.contains("MAX_RECORDS") && payload.contains("writeUtf(nestedName, 128)"));
        assertTrue(payload.contains("record Target(BlockPos position, ItemStack stack, String nestedName)"));
        assertTrue(service.contains("containsLiveResult") && service.contains("isSameItemSameComponents"));
        assertTrue(service.contains("horizontalRadius()") && service.contains("verticalRadius()"));
    }

    @Test
    void modalKeepsTheDocumentedIndependentUiAndScrollContract() throws Exception {
        String screen = Files.readString(Path.of("src/client/java/tempeststudios/quickstacknearby/NearbySearchScreen.java"));
        String notice = Files.readString(Path.of("NOTICE"));
        assertTrue(screen.contains("Math.min(480") && screen.contains("Math.min(290") && screen.contains("scrollRow"));
        assertTrue(screen.contains("Search items, ids, :category, or <components"));
        assertTrue(notice.contains("Inventory Search v3.4.0") && notice.contains("independently implemented"));
    }
}
