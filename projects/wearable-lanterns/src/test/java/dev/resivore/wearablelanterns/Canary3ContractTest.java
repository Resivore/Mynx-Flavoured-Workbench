package dev.resivore.wearablelanterns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class Canary3ContractTest {
    private static final Path PROJECT_ROOT =
            Path.of(System.getProperty("projectRoot")).toAbsolutePath().normalize();
    private static final Path RESOURCES = PROJECT_ROOT.resolve("src/main/resources");
    private static final Path LANTERN_SPRITE = RESOURCES.resolve(
            "assets/wearable_lanterns/textures/gui/sprites/container/slots/lantern.png");

    private static final int TRANSPARENT = 0x00000000;
    private static final int GLYPH = 0xff1b1511;
    private static final String USER_SPRITE_SHA256 =
            "dc606d99651a3e46fe87fc3110a80529ee2a945f57317d1235e12c6035a89d10";

    private static final List<String> EXPECTED_SPRITE = List.of(
            "................",
            "........G.......",
            "........G.......",
            ".......G........",
            ".......G........",
            "......GGGG......",
            "......G..G......",
            ".....G.GG.G.....",
            ".....G....G.....",
            ".....G....G.....",
            ".....G....G.....",
            ".....G....G.....",
            ".....G....G.....",
            ".....GGGGGG.....",
            "................",
            "................");

    @Test
    void lanternSpriteIsTheExactUserSuppliedPng() throws Exception {
        assertEquals(1_749L, Files.size(LANTERN_SPRITE));
        assertEquals(
                USER_SPRITE_SHA256,
                HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(LANTERN_SPRITE))),
                "The supplied PNG must remain byte-for-byte unchanged");

        BufferedImage image = ImageIO.read(LANTERN_SPRITE.toFile());
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());

        Map<Integer, Character> symbols = Map.of(
                TRANSPARENT, '.',
                GLYPH, 'G');
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        List<String> actualSprite = new ArrayList<>();

        for (int y = 0; y < image.getHeight(); y++) {
            StringBuilder row = new StringBuilder(image.getWidth());
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                counts.merge(argb, 1, Integer::sum);
                Character symbol = symbols.get(argb);
                assertTrue(symbol != null, "Unexpected ARGB color at " + x + "," + y
                        + ": 0x" + Integer.toHexString(argb));
                row.append(symbol);
            }
            actualSprite.add(row.toString());
        }

        assertEquals(EXPECTED_SPRITE, actualSprite,
                "Canary 3 must preserve the exact user-supplied pixel map");
        assertEquals(Map.of(TRANSPARENT, 226, GLYPH, 30), counts);
        assertEquals(Set.of(TRANSPARENT, GLYPH), counts.keySet());
    }

    @Test
    void preservesTheNoSecondLambDynamicLightsSourceBoundary() throws IOException {
        Path javaRoot = PROJECT_ROOT.resolve("src/main/java");
        List<Path> javaFiles;
        try (Stream<Path> paths = Files.walk(javaRoot)) {
            javaFiles = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
        String production = javaFiles.stream()
                .map(path -> {
                    try {
                        return Files.readString(path, StandardCharsets.UTF_8);
                    } catch (IOException exception) {
                        throw new IllegalStateException(exception);
                    }
                })
                .reduce("", String::concat);
        assertFalse(production.contains("dev.lambdaurora"));
        assertFalse(production.contains("DynamicLightsInitializer"));
        assertFalse(production.contains("DynamicLightBehavior"));
        assertFalse(production.contains("ItemLightSourceManager"));

        JsonObject metadata = JsonParser.parseString(
                Files.readString(RESOURCES.resolve("fabric.mod.json"), StandardCharsets.UTF_8))
                .getAsJsonObject();
        assertFalse(metadata.getAsJsonObject("depends").has("lambdynlights"));
        assertEquals(
                "*",
                metadata.getAsJsonObject("suggests").get("lambdynlights").getAsString());
        assertFalse(metadata.getAsJsonObject("depends").has("iris"));
    }
}
