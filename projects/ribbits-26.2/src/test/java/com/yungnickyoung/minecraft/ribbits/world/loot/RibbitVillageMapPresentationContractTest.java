package com.yungnickyoung.minecraft.ribbits.world.loot;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RibbitVillageMapPresentationContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void userAuthoredPngsRemainExactRgbaAssets() throws Exception {
        assertPng(
                "common/src/main/resources/assets/ribbits/textures/map/decorations/"
                        + "ribbit_village.png",
                168,
                "df63eb91eda13e91e3b984b11cdffc3d2f3e8c8482d090d4ee1c744ec428b2ba",
                222,
                34);
        assertPng(
                "common/src/main/resources/assets/ribbits/textures/item/"
                        + "ribbit_village_explorer_map.png",
                506,
                "6065e126da4d3d70725cc3adca725e2ce2812ba8a0155a07c1e510b373aa38f5",
                33,
                223);
    }

    @Test
    void markerArtworkRetainsItsExactApprovedVisibleBounds() throws IOException {
        BufferedImage image = ImageIO.read(PROJECT_ROOT.resolve(
                "common/src/main/resources/assets/ribbits/textures/map/decorations/"
                        + "ribbit_village.png").toFile());
        int minX = 16;
        int minY = 16;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xff) != 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        assertEquals(5, minX);
        assertEquals(4, minY);
        assertEquals(10, maxX);
        assertEquals(10, maxY);
    }

    @Test
    void chainPreservingWrapperClaimsOnlyTheExactSuccessMarker() throws IOException {
        String client = Files.readString(PROJECT_ROOT.resolve(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/client/render/"
                        + "RibbitVillageMapItemModel.java"));
        assertTrue(client.contains("extends WrapperBakedItemModel"));
        assertTrue(client.contains("minecraft:filled_map"));
        assertTrue(client.contains("RibbitVillageExplorerMap.isSuccessfulMap(stack)"));
        assertTrue(client.contains("super.update(renderState, stack"));
        assertFalse(client.contains("SUCCESS_NAME_KEY"));
        assertFalse(client.contains("TranslatableContents"));

        String model = Files.readString(PROJECT_ROOT.resolve(
                "common/src/main/resources/assets/ribbits/items/"
                        + "ribbit_village_explorer_map.json"));
        assertTrue(model.contains("\"type\": \"minecraft:model\""));
        assertTrue(model.contains("ribbits:item/ribbit_village_explorer_map"));
        String bakedModel = Files.readString(PROJECT_ROOT.resolve(
                "common/src/main/resources/assets/ribbits/models/item/"
                        + "ribbit_village_explorer_map.json"));
        assertTrue(bakedModel.contains("minecraft:item/generated"));
        assertTrue(bakedModel.contains("ribbits:item/ribbit_village_explorer_map"));
    }

    private static void assertPng(
            String relative,
            int expectedSize,
            String expectedSha256,
            int expectedTransparent,
            int expectedOpaque) throws IOException, NoSuchAlgorithmException {
        Path path = PROJECT_ROOT.resolve(relative);
        byte[] bytes = Files.readAllBytes(path);
        assertEquals(expectedSize, bytes.length);
        assertEquals(expectedSha256,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));

        BufferedImage image = ImageIO.read(path.toFile());
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        assertTrue(image.getColorModel().hasAlpha());
        int transparent = 0;
        int opaque = 0;
        int partial = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xff;
                if (alpha == 0) {
                    transparent++;
                } else if (alpha == 255) {
                    opaque++;
                } else {
                    partial++;
                }
            }
        }
        assertEquals(expectedTransparent, transparent);
        assertEquals(expectedOpaque, opaque);
        assertEquals(0, partial);
    }
}
