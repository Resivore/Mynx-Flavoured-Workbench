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
    private static final String LOCKED_SHA256 = "bbe0d7393b626c1bc2d6b7d9f938888d4a99ae130eb90ecd2cb5711a909e226d";
    private static final String UNLOCKED_SHA256 = "a87026fa1ab3a82cf76cd0dd5a68cf79fa49ef2e0fbfed27964ad18c7d48c656";
    private static final int DARK = 0xFF1B1511;
    private static final String[] LOCKED_MASK = {
            "..###..", ".#...#.", ".#...#.", "#######", "#.....#", "#..#..#", "#..#..#", "#.....#", ".#####."
    };
    private static final String[] UNLOCKED_MASK = {
            ".###......", "#...#.....", "#...#.....", "...#######", "...#.....#", "...#..#..#", "...#..#..#", "...#.....#", "....#####."
    };

    @Test
    void finalSpritesConvertOnlyFormerOpaqueWhitePixelsToTransparent() throws Exception {
        Path resources = ROOT.resolve("src/main/resources");
        assertFinalSprite(resources.resolve(LOCKED), LOCKED_SHA256, LOCKED_MASK, 34);
        assertFinalSprite(resources.resolve(UNLOCKED), UNLOCKED_SHA256, UNLOCKED_MASK, 61);
        assertFalse(Files.exists(resources.resolve(OBSOLETE_SOURCE)));

        String version = Files.readAllLines(ROOT.resolve("gradle.properties")).stream()
                .filter(line -> line.startsWith("mod_version="))
                .map(line -> line.substring("mod_version=".length()))
                .findFirst()
                .orElseThrow();
        Path artifact = ROOT.resolve("build/libs/carried-container-auto-routing-" + version + ".jar");
        try (ZipFile zip = new ZipFile(artifact.toFile())) {
            assertFalse(zip.stream().anyMatch(entry -> entry.getName().equals(OBSOLETE_SOURCE)));
            assertPackagedSprite(zip, LOCKED, LOCKED_SHA256, LOCKED_MASK, 34);
            assertPackagedSprite(zip, UNLOCKED, UNLOCKED_SHA256, UNLOCKED_MASK, 61);
        }
    }

    @Test
    void directResourceBuildAndBadgeKeepNativeDimensionsAndUseExactlyOnePixelDownwardOffset() throws Exception {
        String build = Files.readString(ROOT.resolve("build.gradle"));
        String bridge = Files.readString(ROOT.resolve("src/main/java/dev/resivore/carriedrouting/client/CsrHeaderBadgeIntegration.java"));
        assertFalse(build.contains("lock_source.png"));
        assertFalse(build.contains("getSubimage"));
        assertFalse(build.contains("ImageIO"));
        assertTrue(bridge.contains("LOCKED_WIDTH = 7, LOCKED_HEIGHT = 9"));
        assertTrue(bridge.contains("UNLOCKED_WIDTH = 10, UNLOCKED_HEIGHT = 9"));
        assertTrue(bridge.contains("sprite, x, y + 1,"), "The badge must move down by exactly one GUI pixel");
        assertTrue(bridge.contains("width, height, width, height"), "The badge must remain a 1:1 unscaled texture blit");
        assertTrue(bridge.contains("instanceof ShulkerBoxBlock"));
        assertTrue(bridge.contains("RoutingService.isSupported(stack)"));
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
        assertTrue(bridge.contains("Optional<ShulkerPanelHeaderDecorations.Interaction> interaction"));
        assertTrue(bridge.contains("ClientPlayNetworking.send(new ToggleLockPayload(context.menuId(), context.menuSlot(), 0))"));
        assertTrue(client.contains("ItemTooltipCallback.EVENT.register"));
        assertFalse(server.contains("text.carried_container_auto_routing.no_target"));
    }

    private static void assertFinalSprite(Path file, String expectedHash, String[] expectedMask, int expectedTransparent) throws Exception {
        assertTrue(Files.isRegularFile(file));
        assertEquals(expectedHash, sha256(Files.readAllBytes(file)), file.toString());
        try (InputStream input = Files.newInputStream(file)) {
            assertSprite(ImageIO.read(input), expectedMask, expectedTransparent);
        }
    }

    private static void assertPackagedSprite(ZipFile zip, String resource, String expectedHash,
                                             String[] expectedMask, int expectedTransparent) throws Exception {
        var entry = zip.getEntry(resource);
        assertNotNull(entry, resource);
        try (InputStream input = zip.getInputStream(entry)) {
            byte[] bytes = input.readAllBytes();
            assertEquals(expectedHash, sha256(bytes), resource);
            assertSprite(ImageIO.read(new java.io.ByteArrayInputStream(bytes)), expectedMask, expectedTransparent);
        }
    }

    private static void assertSprite(BufferedImage sprite, String[] expectedMask, int expectedTransparent) {
        assertNotNull(sprite);
        assertEquals(expectedMask[0].length(), sprite.getWidth());
        assertEquals(expectedMask.length, sprite.getHeight());
        int transparent = 0;
        int dark = 0;
        for (int y = 0; y < sprite.getHeight(); y++) for (int x = 0; x < sprite.getWidth(); x++) {
            int pixel = sprite.getRGB(x, y);
            if (expectedMask[y].charAt(x) == '#') {
                assertEquals(DARK, pixel, "non-white source pixel at " + x + "," + y);
                dark++;
            } else {
                assertEquals(0x00000000, pixel, "former white pixel at " + x + "," + y);
                transparent++;
            }
        }
        assertEquals(29, dark, "all non-white opaque source pixels remain unchanged");
        assertEquals(expectedTransparent, transparent, "only former opaque-white source pixels became transparent");
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
