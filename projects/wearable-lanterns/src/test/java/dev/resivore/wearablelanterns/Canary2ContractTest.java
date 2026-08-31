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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class Canary2ContractTest {
    private static final Path PROJECT_ROOT =
            Path.of(System.getProperty("projectRoot")).toAbsolutePath().normalize();
    private static final Path RESOURCES = PROJECT_ROOT.resolve("src/main/resources");
    private static final Path LANTERN_SPRITE = RESOURCES.resolve(
            "assets/wearable_lanterns/textures/gui/sprites/container/slots/lantern.png");

    private static final int TRANSPARENT = 0x00000000;
    private static final int HIGHLIGHT = 0xff7f664d;
    private static final int BODY = 0xff604d3a;
    private static final int SHADOW = 0xff291f18;
    private static final int INTERIOR = 0xff000000;

    private static final List<String> EXPECTED_SPRITE = List.of(
            "................",
            "................",
            "......HHBS......",
            ".....H....S.....",
            "......HBBS......",
            ".....H....S.....",
            "....H......S....",
            "....H..KK..S....",
            "....B..KK..S....",
            "....B..KK..S....",
            "....B......S....",
            ".....HBBBSS.....",
            "......B..S......",
            "................",
            "................",
            "................");

    @Test
    void successorVersionIsCanaryTwo() throws IOException {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(PROJECT_ROOT.resolve("gradle.properties"))) {
            properties.load(input);
        }
        assertEquals("0.1.0-canary2", properties.getProperty("mod_version"));
    }

    @Test
    void lanternSpriteKeepsGeometryAndUsesTheMatchaGuiPalette() throws IOException {
        BufferedImage image = ImageIO.read(LANTERN_SPRITE.toFile());
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());

        Map<Integer, Character> symbols = Map.of(
                TRANSPARENT, '.',
                HIGHLIGHT, 'H',
                BODY, 'B',
                SHADOW, 'S',
                INTERIOR, 'K');
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
                "Canary 2 is a recolor and must preserve the exact Canary 1 pixel mask");
        assertEquals(
                Map.of(TRANSPARENT, 220, HIGHLIGHT, 8, BODY, 10, SHADOW, 12, INTERIOR, 6),
                counts);
        assertEquals(Set.of(TRANSPARENT, HIGHLIGHT, BODY, SHADOW, INTERIOR), counts.keySet());
    }

    @Test
    void doesNotAddASecondLambDynamicLightsSource() throws IOException {
        Path javaRoot = PROJECT_ROOT.resolve("src/main/java");
        List<Path> javaFiles;
        try (Stream<Path> paths = Files.walk(javaRoot)) {
            javaFiles = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
        assertEquals(1, javaFiles.size(),
                "Canary 2 must retain the single universal Trinkets predicate initializer");

        String production = Files.readString(javaFiles.get(0), StandardCharsets.UTF_8);
        assertFalse(production.contains("dev.lambdaurora"));
        assertFalse(production.contains("DynamicLightsInitializer"));
        assertFalse(production.contains("DynamicLightBehavior"));
        assertFalse(production.contains("ItemLightSourceManager"));

        JsonObject metadata = JsonParser.parseString(
                Files.readString(RESOURCES.resolve("fabric.mod.json"), StandardCharsets.UTF_8))
                .getAsJsonObject();
        assertFalse(metadata.getAsJsonObject("depends").has("lambdynlights"));
        assertEquals(
                "4.12.2+26.2",
                metadata.getAsJsonObject("suggests").get("lambdynlights").getAsString());
        assertFalse(metadata.getAsJsonObject("entrypoints").has("client"));
    }
}
