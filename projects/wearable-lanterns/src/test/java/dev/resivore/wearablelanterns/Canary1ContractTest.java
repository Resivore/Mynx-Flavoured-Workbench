package dev.resivore.wearablelanterns;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class Canary1ContractTest {
    private static final Path PROJECT_ROOT =
            Path.of(System.getProperty("projectRoot")).toAbsolutePath().normalize();
    private static final Path RESOURCES = PROJECT_ROOT.resolve("src/main/resources");

    @Test
    void definesOneIndependentLanternSlotImmediatelyAfterBelt() throws IOException {
        JsonObject slot = resourceJson("data/trinkets/slots/legs/lantern.json");

        assertEquals(1, slot.get("amount").getAsInt());
        assertEquals(1, slot.get("max_stack_size").getAsInt());
        assertEquals(-1023, slot.get("order").getAsInt());
        assertEquals(
                "wearable_lanterns:container/slots/lantern",
                slot.get("icon").getAsString());
        assertEquals(
                List.of("wearable_lanterns:lantern_only"),
                strings(slot.getAsJsonArray("validator_predicates")));
        assertEquals("replace", slot.get("validator_predicates:merge_type").getAsString());

        JsonObject assignment = resourceJson("data/trinkets/entities/wearable_lanterns.json");
        assertFalse(assignment.get("replace").getAsBoolean());
        assertEquals(List.of("minecraft:player"), strings(assignment.getAsJsonArray("entities")));
        assertEquals(List.of("legs/lantern"), strings(assignment.getAsJsonArray("slots")));

        assertFalse(Files.exists(RESOURCES.resolve("data/trinkets/slots/legs/belt.json")),
                "Canary 1 must not replace or redefine legs/belt");
    }

    @Test
    void canonicalTagStillRequiresTheTwoInitialVanillaLanterns() throws IOException {
        JsonObject tag = resourceJson("data/trinkets/tags/item/legs/lantern.json");
        assertFalse(tag.get("replace").getAsBoolean(), "The tag must remain extensible");

        JsonArray values = tag.getAsJsonArray("values");
        assertEquals(
                Set.of("minecraft:lantern", "minecraft:soul_lantern"),
                values.asList().stream()
                        .filter(JsonElement::isJsonPrimitive)
                        .map(JsonElement::getAsString)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void validatorUsesOnlyTheCanonicalLanternTag() throws IOException {
        String source = Files.readString(
                PROJECT_ROOT.resolve(
                        "src/main/java/dev/resivore/wearablelanterns/WearableLanterns.java"),
                UTF_8);

        assertTrue(source.contains("public static final String LANTERN_SLOT = \"legs/lantern\""));
        assertTrue(source.contains("Identifier.fromNamespaceAndPath(\"trinkets\", LANTERN_SLOT)"));
        assertTrue(source.contains("LANTERN_SLOT.equals(slot.slotType().getId())"));
        assertTrue(source.contains("stack.is(LANTERN_SLOT_ITEMS)"));
        assertFalse(source.contains("\"trinkets\", \"all\""));
        assertFalse(source.contains("TrinketRendererRegistry"));
        assertFalse(source.contains("legs/belt"));
    }

    @Test
    void rendererTargetsTheTagButRunsOnlyInTheLanternSlot() throws IOException {
        JsonObject root = resourceJson("assets/wearable_lanterns/trinkets/lantern.json");
        assertEquals("#trinkets:legs/lantern", root.get("target").getAsString());

        JsonObject gate = root.getAsJsonObject("render");
        assertEquals("minecraft:if_slot", gate.get("type").getAsString());
        assertEquals("legs/lantern", gate.get("slot").getAsString());

        JsonObject item = gate.getAsJsonObject("then");
        assertEquals("minecraft:item", item.get("type").getAsString());
        assertEquals("body", item.get("model_part").getAsString());
        assertEquals("none", item.get("display_context").getAsString());
        assertEquals(List.of(-1.4, -1.25, 0.0), numbers(item.getAsJsonArray("offset")));
        assertEquals(
                List.of(0.65, 0.65, 0.65),
                numbers(item.getAsJsonObject("transformation").getAsJsonArray("scale")));
    }

    @Test
    void keepsCanaryOneFreeOfGuiNetworkingAndWorldLightImplementations() throws IOException {
        Path javaRoot = PROJECT_ROOT.resolve("src/main/java");
        List<Path> javaFiles;
        try (Stream<Path> paths = Files.walk(javaRoot)) {
            javaFiles = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
        assertEquals(1, javaFiles.size(), "Canary 1 needs only the exact tag validator initializer");

        String production = Files.readString(javaFiles.get(0), UTF_8);
        for (String forbidden : List.of(
                "@Mixin", "ClientPlayNetworking", "ServerPlayNetworking", "CustomPayload",
                "LightBlock", "BlockPos", "setBlock", "TrinketRendererRegistry")) {
            assertFalse(production.contains(forbidden), () -> "Forbidden C1 mechanism: " + forbidden);
        }

        try (Stream<Path> paths = Files.walk(RESOURCES)) {
            List<String> mixinResources = paths
                    .filter(path -> path.getFileName().toString().contains("mixins"))
                    .map(path -> path.getFileName().toString())
                    .toList();
            assertEquals(List.of("wearable_lanterns.iris.mixins.json"), mixinResources,
                    "Only the narrow optional Iris uniform bridge may add a mixin resource");
        }
    }

    @Test
    void metadataKeepsDynamicLightingOptional() throws IOException {
        JsonObject metadata = resourceJson("fabric.mod.json");
        JsonObject depends = metadata.getAsJsonObject("depends");
        assertEquals(">=4.1.0-beta.3", depends.get("trinkets_updated").getAsString());
        assertFalse(depends.has("lambdynlights"));
        assertEquals(
                "*",
                metadata.getAsJsonObject("suggests").get("lambdynlights").getAsString());
        assertTrue(metadata.has("mixins"));
        assertFalse(depends.has("iris"));
    }

    @Test
    void shipsOnlyTheDedicatedGuiSpriteAndNoLanternModelOrTextureCopies() throws IOException {
        Path assets = RESOURCES.resolve("assets");
        Path icon = assets.resolve(
                "wearable_lanterns/textures/gui/sprites/container/slots/lantern.png");
        List<String> pngs;
        try (Stream<Path> paths = Files.walk(assets)) {
            pngs = paths.filter(path -> path.toString().endsWith(".png"))
                    .map(path -> assets.relativize(path).toString().replace('\\', '/'))
                    .toList();
        }

        assertEquals(
                List.of("wearable_lanterns/textures/gui/sprites/container/slots/lantern.png"),
                pngs);
        var image = ImageIO.read(icon.toFile());
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        assertTrue(image.getRGB(8, 2) != 0, "The dedicated sprite must contain lantern artwork");
        assertFalse(Files.exists(assets.resolve("minecraft")));
        assertFalse(Files.exists(assets.resolve("wearable_lanterns/models")));
    }

    private static JsonObject resourceJson(String relative) throws IOException {
        return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative), UTF_8))
                .getAsJsonObject();
    }

    private static List<String> strings(JsonArray array) {
        return array.asList().stream()
                .map(JsonElement::getAsString)
                .toList();
    }

    private static List<Double> numbers(JsonArray array) {
        return array.asList().stream().map(JsonElement::getAsDouble).toList();
    }
}
