package dev.resivore.carriedrouting;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsrHeaderBadgeContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final String SOURCE = "assets/carried_container_auto_routing/textures/gui/lock_source.png";
    private static final String LOCKED = "assets/carried_container_auto_routing/textures/gui/lock_locked.png";
    private static final String UNLOCKED = "assets/carried_container_auto_routing/textures/gui/lock_unlocked.png";

    @Test
    void trimmedSpritesPreserveEverySuppliedOpaquePixelAndRenderAtOneToOneScale() throws Exception {
        BufferedImage source = ImageIO.read(ROOT.resolve("src/main/resources").resolve(SOURCE).toFile());
        assertNotNull(source); assertEquals(20, source.getWidth()); assertEquals(11, source.getHeight());
        assertGlyph(source, 0, 1, 7, 9, 29); assertGlyph(source, 10, 1, 10, 9, 29);
        Path artifact = ROOT.resolve("build/libs/carried-container-auto-routing-0.3.9-csr-header-badge-canary1.jar");
        try (ZipFile zip = new ZipFile(artifact.toFile())) {
            assertFalse(zip.stream().anyMatch(entry -> entry.getName().equals(SOURCE)),
                    "Only the tightly cropped runtime sprites belong in the artifact");
            assertTrimmedSprite(zip, LOCKED, source, 0, 1, 7, 9);
            assertTrimmedSprite(zip, UNLOCKED, source, 10, 1, 10, 9);
        }
        String bridge = Files.readString(ROOT.resolve("src/main/java/dev/resivore/carriedrouting/client/CsrHeaderBadgeIntegration.java"));
        assertTrue(bridge.contains("LOCKED_WIDTH = 7, UNLOCKED_WIDTH = 10, HEIGHT = 9"));
        assertTrue(bridge.contains("width, HEIGHT, width, HEIGHT"), "The badge must be a 1:1 unscaled texture blit");
        assertTrue(bridge.contains("instanceof ShulkerBoxBlock"));
        assertFalse(bridge.contains("BundleItem"));
    }

    @Test
    void optionalCsrBridgeIsIsolatedAndExistingTooltipAndSilentNoTargetBehaviorRemain() throws Exception {
        String client = Files.readString(ROOT.resolve("src/main/java/dev/resivore/carriedrouting/client/CarriedContainerAutoRoutingClient.java"));
        String bridge = Files.readString(ROOT.resolve("src/main/java/dev/resivore/carriedrouting/client/CsrHeaderBadgeIntegration.java"));
        String server = Files.readString(ROOT.resolve("src/main/java/dev/resivore/carriedrouting/CarriedContainerAutoRouting.java"));
        assertTrue(client.contains("FabricLoader.getInstance().isModLoaded(\"container_slot_reservations\")"));
        assertTrue(client.contains("Class.forName(\"dev.resivore.carriedrouting.client.CsrHeaderBadgeIntegration\")"));
        assertFalse(client.contains("ShulkerPanelHeaderDecorations"));
        assertTrue(bridge.contains("RoutingLock.isLocked(stack)"));
        assertTrue(client.contains("ItemTooltipCallback.EVENT.register"));
        assertFalse(server.contains("text.carried_container_auto_routing.no_target"));
    }

    private static void assertGlyph(BufferedImage image, int x, int y, int width, int height, int opaque) {
        int count = 0;
        for (int yy = 0; yy < image.getHeight(); yy++) for (int xx = 0; xx < image.getWidth(); xx++)
            if (((image.getRGB(xx, yy) >>> 24) & 0xFF) != 0) count++;
        // Each side carries the same 29-pixel glyph; scope the count to its bounding rectangle.
        int local = 0;
        for (int yy = y; yy < y + height; yy++) for (int xx = x; xx < x + width; xx++)
            if (((image.getRGB(xx, yy) >>> 24) & 0xFF) != 0) local++;
        assertEquals(58, count); assertEquals(opaque, local);
    }

    private static void assertTrimmedSprite(ZipFile zip, String resource, BufferedImage source,
                                            int sourceX, int sourceY, int width, int height) throws Exception {
        try (InputStream input = zip.getInputStream(zip.getEntry(resource))) {
            BufferedImage sprite = ImageIO.read(input);
            assertNotNull(sprite); assertEquals(width, sprite.getWidth()); assertEquals(height, sprite.getHeight());
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++)
                assertEquals(source.getRGB(sourceX + x, sourceY + y), sprite.getRGB(x, y), resource + " pixel " + x + "," + y);
        }
    }
}
