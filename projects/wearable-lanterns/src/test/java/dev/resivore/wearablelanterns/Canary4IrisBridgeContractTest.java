package dev.resivore.wearablelanterns;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class Canary4IrisBridgeContractTest {
    private static final Path PROJECT_ROOT =
            Path.of(System.getProperty("projectRoot")).toAbsolutePath().normalize();
    private static final Path CLIENT = PROJECT_ROOT.resolve("src/client/java/dev/resivore/wearablelanterns");
    private static final Path RESOURCES = PROJECT_ROOT.resolve("src/main/resources");

    @Test
    void successorVersionIsCanaryFour() throws IOException {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(PROJECT_ROOT.resolve("gradle.properties"))) {
            properties.load(input);
        }
        assertEquals("0.1.0-canary4", properties.getProperty("mod_version"));
    }

    @Test
    void irisIsOptionalAndTheExactAuditedVersionIsFailClosed() throws IOException {
        JsonObject metadata = JsonParser.parseString(
                Files.readString(RESOURCES.resolve("fabric.mod.json"), UTF_8)).getAsJsonObject();
        assertFalse(metadata.getAsJsonObject("depends").has("iris"));
        assertEquals(">=1.11.2", metadata.getAsJsonObject("suggests").get("iris").getAsString());
        assertEquals(
                "CAPABILITY_OR_PROVIDER",
                metadata.getAsJsonObject("custom").getAsJsonObject("runtime_dependency_policy")
                        .get("contract").getAsString());

        String config = Files.readString(RESOURCES.resolve("wearable_lanterns.iris.mixins.json"), UTF_8);
        assertTrue(config.contains("WearableLanternsIrisMixinPlugin"));
        assertTrue(config.contains("IrisHeldItemSupplierMixin"));
        assertTrue(config.contains("\"defaultRequire\": 1"));

        String plugin = Files.readString(CLIENT.resolve("WearableLanternsIrisMixinPlugin.java"), UTF_8);
        assertTrue(plugin.contains("AUDITED_IRIS_VERSION = \"1.11.2+mc26.2\""));
        assertTrue(plugin.contains("getModContainer(IRIS_ID)"));
        assertTrue(plugin.contains("AUDITED_IRIS_VERSION.equals"));
        assertFalse(plugin.contains("Class.forName"));
    }

    @Test
    void hookPinsTheAuditedPerFrameIrisOffhandSupplierSignature() throws IOException {
        String mixin = Files.readString(CLIENT.resolve("mixin/IrisHeldItemSupplierMixin.java"), UTF_8);
        assertTrue(mixin.contains(
                "net.irisshaders.iris.uniforms.IdMapUniforms$HeldItemSupplier"));
        assertTrue(mixin.contains("method = \"update()V\""));
        assertTrue(mixin.contains(
                "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"));
        assertTrue(mixin.contains("hand == InteractionHand.OFF_HAND"));
        assertTrue(mixin.contains("selectSecondaryHandLightStack"));
        assertTrue(mixin.contains("require = 1"));
    }

    @Test
    void bridgeUsesOnlyTheExactSupportedLanternSlotAndActualStack() throws IOException {
        String bridge = Files.readString(CLIENT.resolve("WearableLanternsIrisBridge.java"), UTF_8);
        assertTrue(bridge.contains("getSlotAccess(WearableLanterns.LANTERN_SLOT, 0)"));
        assertTrue(bridge.contains("slot.isValid() ? slot.get() : ItemStack.EMPTY"));
        assertTrue(bridge.contains("wornLantern.is(WearableLanterns.LANTERN_SLOT_ITEMS)"));
        assertTrue(bridge.contains("IrisItemLightProvider"));
        assertTrue(bridge.contains("getLightEmission\", Player.class, ItemStack.class"));
        assertFalse(bridge.contains("minecraft:lantern"), "No hard-coded regular-lantern luminance path");
        assertFalse(bridge.contains("minecraft:soul_lantern"), "No hard-coded soul-lantern luminance path");
    }

    @Test
    void coexistencePreservesEqualOrBrighterKnownRealOffhandLight() {
        assertFalse(WearableLanternsIrisBridge.shouldUseWearable(
                OptionalInt.of(15), OptionalInt.of(15)), "Equal real offhand identity wins");
        assertFalse(WearableLanternsIrisBridge.shouldUseWearable(
                OptionalInt.of(15), OptionalInt.of(10)), "Brighter real offhand wins");
        assertTrue(WearableLanternsIrisBridge.shouldUseWearable(
                OptionalInt.of(0), OptionalInt.of(15)), "Non-light offhand yields to worn lantern");
        assertTrue(WearableLanternsIrisBridge.shouldUseWearable(
                OptionalInt.of(10), OptionalInt.of(15)), "Brighter wearable lantern supplies secondary light");
        assertFalse(WearableLanternsIrisBridge.shouldUseWearable(
                OptionalInt.empty(), OptionalInt.of(15)), "Unknown Iris state fails closed");
        assertFalse(WearableLanternsIrisBridge.shouldUseWearable(
                OptionalInt.of(0), OptionalInt.empty()), "Unresolved wearable stack cannot substitute");
    }

    @Test
    void bridgeCannotMutateInventoryWorldLightOrLambDynamicLights() throws IOException {
        String bridge = Files.readString(CLIENT.resolve("WearableLanternsIrisBridge.java"), UTF_8);
        String mixin = Files.readString(CLIENT.resolve("mixin/IrisHeldItemSupplierMixin.java"), UTF_8);
        String combined = bridge + mixin;
        for (String forbidden : List.of(
                "setItem", "setOffhandItem", "Inventory", "ClientPlayNetworking", "ServerPlayNetworking",
                "CustomPayload", "LightBlock", "setBlock", "BlockPos", "dev.lambdaurora",
                "DynamicLightBehavior", "ItemLightSourceManager")) {
            assertFalse(combined.contains(forbidden), () -> "Forbidden bridge mechanism: " + forbidden);
        }
        assertTrue(mixin.contains("player.getItemInHand(hand)"));
        assertTrue(mixin.contains("return hand == InteractionHand.OFF_HAND"));
    }
}
