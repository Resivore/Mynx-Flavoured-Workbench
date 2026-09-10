package dev.resivore.carriedrouting;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsrHeaderBadgeContractTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final String LOCKED = "assets/carried_container_auto_routing/textures/gui/lock_locked.png";
    private static final String UNLOCKED = "assets/carried_container_auto_routing/textures/gui/lock_unlocked.png";
    private static final String OBSOLETE_SOURCE = "assets/carried_container_auto_routing/textures/gui/lock_source.png";
    private static final String LOCKED_SHA256 = "cde847dba65a29823d62fee4f70fc4c1e83fd40e4fbbc4b81871b9164f11e617";
    private static final String UNLOCKED_SHA256 = "10a5f3205c311974eea2798e1c97cca248798f12b7156c202c59afbb850fc39c";

    @Test
    void finalSuppliedSpritesArePackagedByteForByteAtNativeDimensions() throws Exception {
        Path resources = ROOT.resolve("src/main/resources");
        assertFinalSprite(resources.resolve(LOCKED), 7, 9, LOCKED_SHA256);
        assertFinalSprite(resources.resolve(UNLOCKED), 10, 9, UNLOCKED_SHA256);
        assertFalse(Files.exists(resources.resolve(OBSOLETE_SOURCE)));

        Path artifact = ROOT.resolve("build/libs/carried-container-auto-routing-0.3.10-csr-lock-sprite-art-canary1.jar");
        try (ZipFile zip = new ZipFile(artifact.toFile())) {
            assertFalse(zip.stream().anyMatch(entry -> entry.getName().equals(OBSOLETE_SOURCE)));
            assertPackagedSprite(zip, LOCKED, 7, 9, LOCKED_SHA256);
            assertPackagedSprite(zip, UNLOCKED, 10, 9, UNLOCKED_SHA256);
        }
    }

    @Test
    void directResourceBuildAndBadgeUseNativeOneToOneSpriteDimensions() throws Exception {
        String build = Files.readString(ROOT.resolve("build.gradle"));
        String bridge = Files.readString(ROOT.resolve("src/main/java/dev/resivore/carriedrouting/client/CsrHeaderBadgeIntegration.java"));
        assertFalse(build.contains("lock_source.png"));
        assertFalse(build.contains("getSubimage"));
        assertFalse(build.contains("ImageIO"));
        assertTrue(bridge.contains("LOCKED_WIDTH = 7, LOCKED_HEIGHT = 9"));
        assertTrue(bridge.contains("UNLOCKED_WIDTH = 10, UNLOCKED_HEIGHT = 9"));
        assertTrue(bridge.contains("width, height, width, height"), "The badge must be a 1:1 unscaled texture blit");
        assertTrue(bridge.contains("instanceof ShulkerBoxBlock"));
        assertTrue(bridge.contains("RoutingLock.isLocked(stack)"));
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

    private static void assertFinalSprite(Path file, int width, int height, String expectedHash) throws Exception {
        assertTrue(Files.isRegularFile(file));
        assertEquals(expectedHash, sha256(Files.readAllBytes(file)), file.toString());
        BufferedImage sprite = ImageIO.read(file.toFile());
        assertNotNull(sprite); assertEquals(width, sprite.getWidth()); assertEquals(height, sprite.getHeight());
    }

    private static void assertPackagedSprite(ZipFile zip, String resource, int width, int height, String expectedHash) throws Exception {
        var entry = zip.getEntry(resource);
        assertNotNull(entry, resource);
        try (InputStream input = zip.getInputStream(entry)) {
            byte[] bytes = input.readAllBytes();
            assertEquals(expectedHash, sha256(bytes), resource);
            BufferedImage sprite = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            assertNotNull(sprite); assertEquals(width, sprite.getWidth()); assertEquals(height, sprite.getHeight());
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
