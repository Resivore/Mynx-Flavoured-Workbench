package dev.resivore.coalconsolidation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientBoundaryContractTest {
    private static final Path MAIN = Path.of("src", "main");

    @Test
    void jeiIsOptionalAndHasNoCommonOrDedicatedServerEntrypoint() throws IOException {
        JsonObject metadata = JsonParser.parseString(
                Files.readString(MAIN.resolve("resources/fabric.mod.json"))).getAsJsonObject();
        JsonObject entrypoints = metadata.getAsJsonObject("entrypoints");
        JsonObject depends = metadata.getAsJsonObject("depends");

        assertFalse(entrypoints.has("main"));
        assertTrue(entrypoints.has("client"));
        assertEquals(1, metadata.getAsJsonArray("mixins").size());
        assertEquals("coal_consolidation.mixins.json",
                metadata.getAsJsonArray("mixins").get(0).getAsString());
        assertFalse(entrypoints.has("jei_mod_plugin"));
        assertFalse(depends.has("jei"));
        assertTrue(metadata.getAsJsonObject("recommends").has("jei"));

        try (var sources = Files.walk(MAIN.resolve("java"))) {
            assertTrue(sources.filter(path -> path.toString().endsWith(".java"))
                    .allMatch(path -> {
                        try {
                            return !Files.readString(path).contains("mezz.jei");
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    }));
        }
    }

    @Test
    void creativeRemovalUsesSupportedFabricOutputMutationAndDoesNotTouchTheRegistry() throws IOException {
        String source = Files.readString(MAIN.resolve(
                "java/dev/resivore/coalconsolidation/client/CoalConsolidationClient.java"));

        assertTrue(source.contains("CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)"));
        assertTrue(source.contains("getDisplayStacks().removeIf"));
        assertTrue(source.contains("getSearchTabStacks().removeIf"));
        assertTrue(source.contains("stack.is(Items.CHARCOAL)"));
        assertFalse(source.contains("Registry.unregister"));
        assertFalse(source.contains("mezz.jei"));
    }

    @Test
    void jeiVisibilityUsesItsSupportedIndependentHiddenIngredientTag() throws IOException {
        JsonObject tag = JsonParser.parseString(Files.readString(MAIN.resolve(
                "resources/data/c/tags/item/hidden_from_recipe_viewers.json"))).getAsJsonObject();

        assertFalse(tag.get("replace").getAsBoolean());
        assertEquals(1, tag.getAsJsonArray("values").size());
        assertEquals("minecraft:charcoal", tag.getAsJsonArray("values").get(0).getAsString());
    }
}
