package dev.resivore.inventorycrafting;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryGuiAlignmentTest {
    private static final Path ROOT = Path.of(System.getProperty("projectRoot"));
    private static final Path ARTWORK = ROOT.resolve(
            "src/main/resources/assets/inherent_3x3_inventory_crafting/textures/gui/container/inventory.png"
    );
    private static final String ARTWORK_SHA256 =
            "8E7BAD5FEC6571199D651D68C4F1114A5CC70A7D98F59B713345A0F7866614D8";
    private static final String BACKGROUND_BLIT_DESCRIPTOR =
            "(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V";

    @Test
    void onlyOutputXMovesTwoPixelsFromCanary3() {
        assertEquals(-7, InventoryCraftingLayout.GRID_X - 84);
        assertEquals(77, InventoryCraftingLayout.GRID_X);
        assertEquals(8, InventoryCraftingLayout.GRID_Y);
        assertEquals(2, InventoryCraftingLayout.RESULT_X - 150);
        assertEquals(152, InventoryCraftingLayout.RESULT_X);
        assertEquals(27, InventoryCraftingLayout.RESULT_Y);
        assertEquals(100, InventoryCraftingLayout.RECIPE_BOOK_X);
        assertEquals(62, InventoryCraftingLayout.RECIPE_BOOK_Y);

        assertArrayEquals(
                new int[]{77, 95, 113, 77, 95, 113, 77, 95, 113},
                java.util.stream.IntStream.range(0, 9)
                        .map(InventoryCraftingLayout::gridX)
                        .toArray()
        );
        assertArrayEquals(
                new int[]{8, 8, 8, 26, 26, 26, 44, 44, 44},
                java.util.stream.IntStream.range(0, 9)
                        .map(InventoryCraftingLayout::gridY)
                        .toArray()
        );
        assertArrayEquals(
                new int[]{1, 2, 74, 3, 4, 75, 76, 77, 78},
                InventoryCraftingLayout.EXPECTED_ROW_MAJOR_MENU_IDS
        );
    }

    @Test
    void bundledArtworkIsTheExactAuthoritativePng() throws IOException, NoSuchAlgorithmException {
        assertTrue(Files.isRegularFile(ARTWORK));
        assertEquals(2_040L, Files.size(ARTWORK));
        assertEquals(ARTWORK_SHA256, sha256(ARTWORK));

        BufferedImage image = ImageIO.read(ARTWORK.toFile());
        assertNotNull(image);
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());

        for (int y : new int[]{7, 25, 43}) {
            for (int x : new int[]{76, 94, 112}) {
                assertSlotFrame(image, x, y);
            }
        }
        assertSlotFrame(image, 151, 26);
    }

    @Test
    void inventoryScreenUsesOneRequiredNamespacedBackgroundHook() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/resivore/inventorycrafting/mixin/client/InventoryScreenMixin.java"
        ));
        assertTrue(source.contains("@ModifyArg("));
        assertTrue(source.contains("method = \"extractBackground\""));
        assertTrue(source.contains("index = 1"));
        assertTrue(source.contains("require = 1"));
        assertTrue(source.contains("\"textures/gui/container/inventory.png\""));
        assertTrue(source.contains("this.leftPos + InventoryCraftingLayout.RECIPE_BOOK_X"));
        assertTrue(source.contains("this.topPos + InventoryCraftingLayout.RECIPE_BOOK_Y"));

        ClassNode target = readClass(Type.getInternalName(InventoryScreen.class) + ".class");
        long matchingBlits = 0;
        for (var method : target.methods) {
            if (!method.name.equals("extractBackground")) {
                continue;
            }
            for (int index = 0; index < method.instructions.size(); index++) {
                if (method.instructions.get(index) instanceof MethodInsnNode call
                        && call.owner.equals("net/minecraft/client/gui/GuiGraphicsExtractor")
                        && call.name.equals("blit")
                        && call.desc.equals(BACKGROUND_BLIT_DESCRIPTOR)) {
                    matchingBlits++;
                }
            }
        }
        assertEquals(1L, matchingBlits, "InventoryScreen must retain one narrow background blit seam");
    }

    private static void assertSlotFrame(BufferedImage image, int x, int y) {
        int dark = 0xFF1B1511;
        int light = 0xFF7F664D;
        for (int offset = 0; offset <= 16; offset++) {
            assertEquals(dark, image.getRGB(x + offset, y));
            assertEquals(dark, image.getRGB(x, y + offset));
        }
        for (int offset = 1; offset <= 16; offset++) {
            assertEquals(light, image.getRGB(x + 17, y + offset));
        }
        for (int offset = 1; offset <= 17; offset++) {
            assertEquals(light, image.getRGB(x + offset, y + 17));
        }
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().withUpperCase().formatHex(digest.digest(Files.readAllBytes(path)));
    }

    private static ClassNode readClass(String resource) throws IOException {
        try (InputStream stream = Objects.requireNonNull(
                InventoryGuiAlignmentTest.class.getClassLoader().getResourceAsStream(resource),
                resource
        )) {
            ClassNode node = new ClassNode();
            new org.objectweb.asm.ClassReader(stream).accept(node, 0);
            return node;
        }
    }
}
