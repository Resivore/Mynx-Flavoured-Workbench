package dev.resivore.enderscapepruning;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Static boundary checks for optional JEI linkage and reload-safe native ingestion. */
final class EnderscapeJeiLoadingContractTest {
    private static final String EXPECTED_JEI_SHA256 =
            "20bc7f0ebe5f36f84c8c4d571469968be54a6e1989b4e85a736136dc41ea8fe2";

    @Test
    void serializerSyncsOnceAndThePluginNeverFabricatesRecipes() throws Exception {
        String recipes = source("EnderscapePruningRecipes.java");
        String plugin = source("client/EnderscapePruningJeiPlugin.java");
        String filtering = source("PruningRecipeMap.java");

        String sync = "RecipeSynchronization.synchronizeRecipeSerializer(MATCHA_VOID_CAMPFIRE)";
        assertEquals(1, occurrences(recipes, sync));
        assertTrue(recipes.indexOf("Registry.register(") < recipes.indexOf(sync));
        assertTrue(filtering.contains("recipes.removeIf"));
        assertTrue(filtering.contains("RecipeMap.create(recipes)"));

        assertFalse(plugin.contains("registerRecipes"));
        assertFalse(plugin.contains("IRecipeRegistration"));
        assertFalse(plugin.contains("addRecipes"));
        assertFalse(plugin.contains("RecipeHolder"));
        assertFalse(plugin.contains("RecipeDisplay"));
        assertTrue(plugin.contains("onRuntimeAvailable"));
        assertTrue(plugin.contains("getAllIngredients"));
        assertTrue(plugin.contains("isSuppressedItem"));
        assertTrue(plugin.contains("hasSuppressedStoredEnchantment"));
        assertTrue(plugin.contains("removeIngredientsAtRuntime"));
    }

    @Test
    void packagedCommonInitializationHasNoJeiLinkage() throws Exception {
        try (ZipFile jar = new ZipFile(packagedJar().toFile())) {
            JsonObject metadata = JsonParser.parseString(text(jar, "fabric.mod.json")).getAsJsonObject();
            assertFalse(metadata.getAsJsonObject("depends").has("jei"));
            assertTrue(metadata.getAsJsonObject("recommends").has("jei"));
            assertEquals("*", metadata.get("environment").getAsString());
            assertTrue(metadata.getAsJsonObject("entrypoints").has("main"));
            assertTrue(metadata.getAsJsonObject("entrypoints").has("jei_mod_plugin"));

            assertFalse(jar.stream().map(ZipEntry::getName)
                    .anyMatch(name -> name.startsWith("mezz/jei/")),
                    "JEI stays compile/test-only and is not bundled");
            Set<String> classesWithJeiReferences = new LinkedHashSet<>();
            for (ZipEntry entry : java.util.Collections.list(jar.entries())) {
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                if (text(jar, entry.getName()).contains("mezz/jei/")) {
                    classesWithJeiReferences.add(entry.getName());
                }
            }
            assertEquals(Set.of(
                    "dev/resivore/enderscapepruning/client/EnderscapePruningJeiPlugin.class"),
                    classesWithJeiReferences);
        }
    }

    @Test
    void exactJeiVersionUsesSyncedCraftingMapAndRestartsAfterRecipeRefresh() throws Exception {
        assertEquals(EXPECTED_JEI_SHA256, sha256(jeiJar()));
        try (ZipFile jei = new ZipFile(jeiJar().toFile())) {
            String vanillaPlugin = text(jei, "mezz/jei/library/plugins/vanilla/VanillaPlugin.class");
            assertTrue(vanillaPlugin.contains("getClientSyncedRecipes"));
            assertTrue(vanillaPlugin.contains("CRAFTING"));

            String vanillaRecipes = text(jei,
                    "mezz/jei/library/plugins/vanilla/crafting/VanillaRecipes.class");
            assertTrue(vanillaRecipes.contains("byType"));
            assertTrue(vanillaRecipes.contains("CRAFTING"));
            assertTrue(vanillaRecipes.contains("isHandled"));

            String extension = text(jei,
                    "mezz/jei/library/plugins/vanilla/crafting/CraftingCategoryExtension.class");
            assertTrue(extension.contains("ShapelessCraftingRecipeDisplay"));
            assertTrue(extension.contains("display"));
            assertTrue(extension.contains("ingredients"));

            String client = text(jei, "mezz/jei/fabric/JustEnoughItemsClient.class");
            assertTrue(client.contains("ClientRecipeSynchronizedEvent"));
            assertTrue(client.contains("setClientSyncedRecipes"));

            String lifecycle = text(jei,
                    "mezz/jei/fabric/startup/ClientLifecycleHandler.class");
            assertTrue(lifecycle.contains("AFTER_RECIPES_UPDATED"));
            assertTrue(lifecycle.contains("stopJei"));
            assertTrue(lifecycle.contains("startJei"));

            String initializer = text(jei, "mezz/jei/fabric/JustEnoughItems.class");
            assertTrue(initializer.contains("synchronizeRecipeSerializer"));
        }
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int index = 0; (index = text.indexOf(needle, index)) >= 0; index += needle.length()) {
            count++;
        }
        return count;
    }

    private static String source(String relative) throws Exception {
        return Files.readString(projectRoot()
                .resolve("src/main/java/dev/resivore/enderscapepruning")
                .resolve(relative));
    }

    private static String text(ZipFile zip, String name) throws Exception {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            throw new AssertionError("Missing " + name);
        }
        try (InputStream input = zip.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            for (int count; (count = input.read(buffer)) > 0;) {
                digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static Path projectRoot() {
        return Path.of(System.getProperty("projectRoot")).toAbsolutePath().normalize();
    }

    private static Path packagedJar() {
        return Path.of(System.getProperty("packagedJar")).toAbsolutePath().normalize();
    }

    private static Path jeiJar() {
        return Path.of(System.getProperty("jeiJar")).toAbsolutePath().normalize();
    }
}
