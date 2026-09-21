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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class Canary6ProviderSupportContractTest {
    private static final Path PROJECT_ROOT =
            Path.of(System.getProperty("projectRoot")).toAbsolutePath().normalize();
    private static final Path RESOURCES = PROJECT_ROOT.resolve("src/main/resources");
    private static final Set<String> APPROVED_PROVIDER_ITEMS = Set.of(
            "enderscape:void_lantern",
            "enderscape:bulb_lantern",
            "ribbits:swamp_lantern",
            "auroraslanterns:amethyst_lantern",
            "auroraslanterns:redstone_lantern");
    private static final Set<String> EXCLUDED_ITEMS = Set.of(
            "enderscape:end_lamp",
            "enderscape:blinklamp",
            "auroraslanterns:chandelier/iron",
            "auroraslanterns:wall_lantern/amethyst",
            "auroraslanterns:wall_lantern/redstone",
            "bbb:oak_lantern");

    @Test
    void tagContainsExactlyTwoRequiredVanillaAndFiveOptionalProviderItems() throws IOException {
        JsonObject tag = resourceJson("data/trinkets/tags/item/legs/lantern.json");
        assertFalse(tag.get("replace").getAsBoolean());
        JsonArray values = tag.getAsJsonArray("values");
        assertEquals(7, values.size());

        List<String> required = values.asList().stream()
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .toList();
        assertEquals(List.of("minecraft:lantern", "minecraft:soul_lantern"), required);

        Map<String, Boolean> optional = values.asList().stream()
                .filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> entry.get("id").getAsString(),
                        entry -> entry.get("required").getAsBoolean()));
        assertEquals(APPROVED_PROVIDER_ITEMS, optional.keySet());
        assertTrue(optional.values().stream().noneMatch(Boolean::booleanValue),
                "Every optional provider entry must use required=false");
        assertTrue(EXCLUDED_ITEMS.stream().noneMatch(id -> required.contains(id) || optional.containsKey(id)),
                "An explicitly excluded light entered the support tag");
    }

    @Test
    void providerModsRemainOptionalAndHaveNoProductionCodeBranches() throws IOException {
        JsonObject metadata = resourceJson("fabric.mod.json");
        JsonObject depends = metadata.getAsJsonObject("depends");
        for (String provider : List.of("enderscape", "ribbits", "auroraslanterns")) {
            assertFalse(depends.has(provider), provider + " became a hard runtime dependency");
        }

        String production = readJava(PROJECT_ROOT.resolve("src/main/java"))
                + readJava(PROJECT_ROOT.resolve("src/client/java"));
        for (String namespace : List.of("enderscape:", "ribbits:", "auroraslanterns:")) {
            assertFalse(production.contains(namespace),
                    "Provider-specific production branch found for " + namespace);
        }
        for (String providerPackage : List.of(
                "net.penumbra.enderscape",
                "com.yungnickyoung.minecraft.ribbits",
                "dev.lambdaurora.auroraslanterns")) {
            assertFalse(production.contains(providerPackage),
                    "Provider class linkage found for " + providerPackage);
        }
    }

    @Test
    void rendererStillConsumesTheActualTaggedItemStackWithoutModelOverrides() throws IOException {
        JsonObject root = resourceJson("assets/wearable_lanterns/trinkets/lantern.json");
        assertEquals(Set.of("priority", "target", "render"), root.keySet());
        assertEquals("#trinkets:legs/lantern", root.get("target").getAsString());
        JsonObject gate = root.getAsJsonObject("render");
        assertEquals("minecraft:if_slot", gate.get("type").getAsString());
        assertEquals("legs/lantern", gate.get("slot").getAsString());
        JsonObject item = gate.getAsJsonObject("then");
        assertEquals("minecraft:item", item.get("type").getAsString());
        assertEquals("body", item.get("model_part").getAsString());
        assertEquals("none", item.get("display_context").getAsString());
        assertFalse(item.has("model"));
        assertFalse(item.has("item"));
        assertFalse(item.has("texture"));
    }

    @Test
    void providerAssetsClassesAndLightingImplementationsAreNotBundledInSources() throws IOException {
        Path assets = RESOURCES.resolve("assets");
        Path data = RESOURCES.resolve("data");
        for (String namespace : List.of("enderscape", "ribbits", "auroraslanterns")) {
            assertFalse(Files.exists(assets.resolve(namespace)),
                    "Provider assets were copied into Wearable Lanterns: " + namespace);
            assertFalse(Files.exists(data.resolve(namespace)),
                    "Provider data were copied into Wearable Lanterns: " + namespace);
        }

        String production = readJava(PROJECT_ROOT.resolve("src/main/java"))
                + readJava(PROJECT_ROOT.resolve("src/client/java"));
        for (String forbidden : List.of(
                "ItemLightSourceManager", "DynamicLightBehavior", "DynamicLightsInitializer",
                "setBlock", "LightBlock", "BLOCK_STATE", "getLightEmission() ==",
                "return 15", "return 14", "return 12", "return 7")) {
            assertFalse(production.contains(forbidden),
                    "Wearable Lanterns introduced a substitute/provider luminance path: " + forbidden);
        }
    }

    @Test
    void irisIdentityEmissionColorAndRealOffhandPrecedenceRemainIntact() throws IOException {
        String bridge = Files.readString(PROJECT_ROOT.resolve(
                "src/client/java/dev/resivore/wearablelanterns/WearableLanternsIrisBridge.java"), UTF_8);
        String mixin = Files.readString(PROJECT_ROOT.resolve(
                "src/client/java/dev/resivore/wearablelanterns/mixin/IrisHeldItemSupplierMixin.java"), UTF_8);
        assertTrue(bridge.contains("wornLantern.is(WearableLanterns.LANTERN_SLOT_ITEMS)"));
        assertTrue(bridge.contains("emission.invoke(stack.getItem(), player, stack)"));
        assertTrue(bridge.contains("wearableEmission.getAsInt() > offhandEmission.getAsInt()"));
        assertTrue(mixin.contains("ItemStack realOffhand = player.getItemInHand(hand)"));
        assertTrue(mixin.contains("? WearableLanternsIrisBridge.selectSecondaryHandLightStack(player, realOffhand)"));
        assertFalse(bridge.contains("copy()"), "The Iris bridge must evaluate the actual equipped stack");
    }

    @Test
    void slotAndMenuBoundaryContractRemainDataDrivenAndUnchanged() throws IOException {
        JsonObject slot = resourceJson("data/trinkets/slots/legs/lantern.json");
        assertEquals(1, slot.get("amount").getAsInt());
        assertEquals(1, slot.get("max_stack_size").getAsInt());
        assertEquals(-1023, slot.get("order").getAsInt());
        assertEquals(List.of("wearable_lanterns:lantern_only"),
                slot.getAsJsonArray("validator_predicates").asList().stream()
                        .map(JsonElement::getAsString).toList());
        assertEquals("replace", slot.get("validator_predicates:merge_type").getAsString());
        assertFalse(Files.exists(RESOURCES.resolve("data/trinkets/slots/legs/belt.json")));

        String production = readJava(PROJECT_ROOT.resolve("src/main/java"))
                + readJava(PROJECT_ROOT.resolve("src/client/java"));
        for (String forbidden : List.of(
                "inventoryextended", "InventoryExtended", "AbstractContainerMenu", "SlotAccessor",
                "quickMoveStack", "addSlot", "legs/belt")) {
            assertFalse(production.contains(forbidden),
                    "The accepted native menu boundary was replaced: " + forbidden);
        }
    }

    private static JsonObject resourceJson(String relative) throws IOException {
        return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative), UTF_8))
                .getAsJsonObject();
    }

    private static String readJava(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .map(path -> {
                        try {
                            return Files.readString(path, UTF_8);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
    }
}
