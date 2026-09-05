package com.crispytwig.naturalist.fabric.compat;

import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FieldGuideMixinPluginTest {
    private static final String PACKAGE = "com.crispytwig.naturalist.compat.fieldguide.mixin.";
    private static final List<String> MIXINS = List.of(
            "EntryRenderHelperMixin", "FieldGuideEntryScreenMixin", "IconCacheManagerMixin");
    private static final List<String> TARGETS = List.of(
            "com.evandev.fieldguide.client.gui.util.EntryRenderHelper",
            "com.evandev.fieldguide.client.gui.screens.FieldGuideEntryScreen",
            "com.evandev.fieldguide.client.gui.util.IconCacheManager");

    private static FabricLoader loader(boolean installed, EnvType environment) {
        return (FabricLoader) Proxy.newProxyInstance(FabricLoader.class.getClassLoader(),
                new Class<?>[]{FabricLoader.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("getEnvironmentType")) return environment;
                    if (method.getName().equals("isModLoaded")) {
                        assertEquals("fieldguide", arguments[0]);
                        return installed;
                    }
                    throw new AssertionError("Unexpected loader access: " + method.getName());
                });
    }

    @ParameterizedTest
    @CsvSource({"false, CLIENT, false", "true, CLIENT, true",
                "false, SERVER, false", "true, SERVER, false"})
    void filtersEveryOptionalTarget(boolean installed, EnvType environment, boolean expected) {
        var plugin = new FieldGuideMixinPlugin(loader(installed, environment));
        plugin.onLoad(PACKAGE);
        for (int i = 0; i < MIXINS.size(); i++) {
            assertEquals(expected, plugin.shouldApplyMixin(TARGETS.get(i), PACKAGE + MIXINS.get(i)));
        }
    }

    @Test
    void gateLoadsWithoutFieldGuideClasses() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName(TARGETS.getFirst(), false, getClass().getClassLoader()));
        var plugin = new FieldGuideMixinPlugin(loader(false, EnvType.CLIENT));
        assertFalse(plugin.shouldApplyMixin(TARGETS.getFirst(), PACKAGE + MIXINS.getFirst()));
    }

    @Test
    void processedConfigurationUsesGateAndRetainsAllClientMixins() throws Exception {
        try (var input = getClass().getResourceAsStream("/naturalist.fieldguide.mixins.json")) {
            assertNotNull(input);
            var config = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(FieldGuideMixinPlugin.class.getName(), config.get("plugin").getAsString());
            assertEquals(PACKAGE.substring(0, PACKAGE.length() - 1), config.get("package").getAsString());
            assertEquals(MIXINS, config.getAsJsonArray("client").asList().stream()
                    .map(value -> value.getAsString()).toList());
            assertFalse(config.has("mixins"));
            assertFalse(config.get("required").getAsBoolean());
            assertEquals(0, config.getAsJsonObject("injectors").get("defaultRequire").getAsInt());
        }
    }
}
