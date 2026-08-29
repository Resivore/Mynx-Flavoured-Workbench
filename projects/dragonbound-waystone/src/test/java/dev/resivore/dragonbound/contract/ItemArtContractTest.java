package dev.resivore.dragonbound.contract;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ItemArtContractTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/dragonbound_waystone");
    private static final Path ITEMS = ASSETS.resolve("items");
    private static final Path MODELS = ASSETS.resolve("models/item");
    private static final Path TEXTURES = ASSETS.resolve("textures/item");

    @Test
    void staffUsesOnlyItsProjectOwnedModelAndTexturePath() throws IOException {
        assertItemModel("dragonbound_staff", "dragonbound_waystone:item/dragonbound_staff");
        assertGeneratedTextureModel("dragonbound_staff", "dragonbound_waystone:item/dragonbound_staff");

        String definition = Files.readString(ITEMS.resolve("dragonbound_staff.json"));
        String model = Files.readString(MODELS.resolve("dragonbound_staff.json"));
        assertFalse(definition.contains("minecraft:item/stick"));
        assertFalse(model.contains("minecraft:item/stick"));
    }

    @Test
    void suppliedStaffTextureIsSixteenPixelsWithPureWhiteMadeTransparent() throws Exception {
        Path texture = TEXTURES.resolve("dragonbound_staff.png");
        BufferedImage image = readImage(texture);

        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        assertTrue(image.getColorModel().hasAlpha());
        assertEquals(167, countAlpha(image, 0));
        assertEquals(89, countAlpha(image, 255));
        assertEquals(0, countOpaquePureWhite(image));
        assertEquals(
                "C2CA094BEBDDCB09DCA5DC263ECF3D149962885C4C507D57CF870C12EE5D3412",
                sha256(texture));
    }

    @Test
    void imbuedVoidUsesAnIndependentDragonboundModelAndTexturePath() throws IOException {
        assertItemModel("imbued_void_pearl", "dragonbound_waystone:item/imbued_void_pearl");
        assertGeneratedTextureModel("imbued_void_pearl", "dragonbound_waystone:item/imbued_void_pearl");

        String definition = Files.readString(ITEMS.resolve("imbued_void_pearl.json"));
        String model = Files.readString(MODELS.resolve("imbued_void_pearl.json"));
        String wiring = definition + model;
        assertFalse(wiring.contains("minecraft:item/ender_pearl"));
        assertFalse(wiring.toLowerCase(Locale.ROOT).contains("matcha"));
    }

    @Test
    void imbuedVoidTextureIsTheExactAuditedStableVoidSnapshot() throws Exception {
        Path texture = TEXTURES.resolve("imbued_void_pearl.png");
        BufferedImage image = readImage(texture);

        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        assertTrue(image.getColorModel().hasAlpha());
        assertEquals(135, countAlpha(image, 0));
        assertEquals(121, countAlpha(image, 255));
        assertEquals(
                "999560B23FEC9BE33DFECC93FE4B3BFD083E7349D5FE963C162D28A6926FE62F",
                sha256(texture));
    }

    @Test
    void packagedNoticeKeepsTheMatchaAssetExceptionAndExactSourceIdentity() throws IOException {
        String notice = Files.readString(ASSETS.resolve("NOTICE_MATCHA_STABLE_VOID.txt"));
        JsonObject metadata = JsonParser.parseString(
                Files.readString(Path.of("src/main/resources/fabric.mod.json"))).getAsJsonObject();

        assertTrue(notice.contains("CC BY-NC-SA 4.0"));
        assertTrue(notice.contains("not covered by Dragonbound Waystone's MIT license"));
        assertTrue(notice.contains("Matcha_Flavoured_1_10.zip"));
        assertTrue(notice.contains("Matcha_Flavoured_1_12.zip"));
        assertTrue(notice.contains("assets/minecraft/textures/item/ender_pearl.png"));
        assertTrue(notice.contains("999560B23FEC9BE33DFECC93FE4B3BFD083E7349D5FE963C162D28A6926FE62F"));
        assertEquals(
                java.util.Set.of("MIT", "CC-BY-NC-SA-4.0"),
                metadata.getAsJsonArray("license").asList().stream()
                        .map(element -> element.getAsString())
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }

    private static void assertItemModel(String itemName, String expectedModel) throws IOException {
        JsonObject item = JsonParser.parseString(
                Files.readString(ITEMS.resolve(itemName + ".json"))).getAsJsonObject();
        JsonObject model = item.getAsJsonObject("model");

        assertEquals("minecraft:model", model.get("type").getAsString());
        assertEquals(expectedModel, model.get("model").getAsString());
    }

    private static void assertGeneratedTextureModel(String itemName, String expectedTexture) throws IOException {
        JsonObject model = JsonParser.parseString(
                Files.readString(MODELS.resolve(itemName + ".json"))).getAsJsonObject();

        assertEquals("minecraft:item/generated", model.get("parent").getAsString());
        assertEquals(expectedTexture, model.getAsJsonObject("textures").get("layer0").getAsString());
    }

    private static BufferedImage readImage(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        assertNotNull(image, () -> "ImageIO could not read " + path);
        return image;
    }

    private static int countAlpha(BufferedImage image, int expectedAlpha) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) == expectedAlpha) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countOpaquePureWhite(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == 0xFFFFFFFF) {
                    count++;
                }
            }
        }
        return count;
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
        return HexFormat.of().formatHex(digest).toUpperCase(Locale.ROOT);
    }
}
