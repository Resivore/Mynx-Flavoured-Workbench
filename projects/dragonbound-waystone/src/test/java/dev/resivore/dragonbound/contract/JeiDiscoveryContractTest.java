package dev.resivore.dragonbound.contract;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JeiDiscoveryContractTest {
    private static final Path JAVA_ROOT = Path.of("src/main/java");
    private static final Path CONTENT = JAVA_ROOT.resolve("dev/resivore/dragonbound/DragonboundContent.java");
    private static final Path RECIPES = Path.of("src/main/resources/data/dragonbound_waystone/recipe");

    @Test
    void exactThreeRegisteredOutputsAreExposedThroughOneVanillaCreativeTab() throws IOException {
        String content = Files.readString(CONTENT);
        int registration = content.indexOf("CreativeModeTabEvents.modifyOutputEvent(TOOLS_AND_UTILITIES_TAB)");
        assertTrue(registration >= 0);

        int registrationEnd = content.indexOf("        });", registration);
        assertTrue(registrationEnd > registration);
        String exposure = content.substring(registration, registrationEnd);

        assertTrue(content.contains("Identifier.withDefaultNamespace(\"tools_and_utilities\")"));
        assertEquals(3, countOccurrences(exposure, "output.accept("));
        assertTrue(exposure.contains("output.accept(WAYSTONE_ITEM)"));
        assertTrue(exposure.contains("output.accept(IMBUED_VOID_PEARL)"));
        assertTrue(exposure.contains("output.accept(DRAGONBOUND_STAFF)"));
    }

    @Test
    void recipeDiscoveryIncludesOneDynamicShapelessMaterialRecipeWithoutJeiOwnership() throws IOException {
        List<String> recipeNames;
        try (Stream<Path> paths = Files.list(RECIPES)) {
            recipeNames = paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }

        assertEquals(List.of(
                "dragonbound_staff.json",
                "dragonbound_waystone.json",
                "imbued_void_pearl.json",
                "waystone_material.json"), recipeNames);
        for (String recipeName : recipeNames.stream()
                .filter(name -> !name.equals("waystone_material.json"))
                .toList()) {
            JsonObject recipe = JsonParser.parseString(Files.readString(RECIPES.resolve(recipeName))).getAsJsonObject();
            assertTrue(recipe.get("type").getAsString().startsWith("minecraft:crafting_"));
        }
        JsonObject material = JsonParser.parseString(Files.readString(RECIPES.resolve("waystone_material.json")))
                .getAsJsonObject();
        assertEquals("dragonbound_waystone:waystone_material", material.get("type").getAsString());
    }

    @Test
    void commonArchitectureHasNoJeiDependencyPluginOrRegistration() throws IOException {
        String allJava = readAllJava();
        JsonObject metadata = JsonParser.parseString(
                Files.readString(Path.of("src/main/resources/fabric.mod.json"))).getAsJsonObject();
        String build = Files.readString(Path.of("build.gradle"));

        assertFalse(allJava.contains("mezz.jei"));
        assertFalse(allJava.contains("IRecipeCategory"));
        assertFalse(allJava.contains("registerRecipes("));
        assertFalse(allJava.contains("registerItemSubtypes("));
        assertFalse(metadata.getAsJsonObject("entrypoints").has("jei_mod_plugin"));
        assertFalse(metadata.has("recommends") && metadata.getAsJsonObject("recommends").has("jei"));
        assertFalse(metadata.getAsJsonObject("depends").has("jei"));
        assertFalse(build.contains("jei-26.2-fabric"));
    }

    @Test
    void currentRecipesNeedNoHeartCompatibilityDependencyOrCustomIngredientBootstrap() throws IOException {
        String allJava = readAllJava();
        JsonObject metadata = JsonParser.parseString(
                Files.readString(Path.of("src/main/resources/fabric.mod.json"))).getAsJsonObject();
        String build = Files.readString(Path.of("build.gradle"));
        String staffRecipe = Files.readString(RECIPES.resolve("dragonbound_staff.json"));

        assertFalse(metadata.getAsJsonObject("depends").has("matcha_heart_death_compat"));
        assertFalse(build.contains("matcha-heart-death-compat"));
        assertFalse(allJava.contains("CustomIngredientInit"));
        assertFalse(staffRecipe.contains("fabric:components"));
        assertFalse(staffRecipe.contains("reinforced_crystal_heart"));
        assertFalse(staffRecipe.contains("minecraft:poisonous_potato"));
        assertFalse(staffRecipe.contains("minecraft:dragon_head"));
        assertFalse(staffRecipe.contains("minecraft:turtle_scute"));
    }

    private static String readAllJava() throws IOException {
        StringBuilder source = new StringBuilder();
        try (Stream<Path> paths = Files.walk(JAVA_ROOT)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".java")).sorted().toList()) {
                source.append(Files.readString(path)).append('\n');
            }
        }
        return source.toString();
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        for (int index = 0; (index = haystack.indexOf(needle, index)) >= 0; index += needle.length()) {
            count++;
        }
        return count;
    }
}
