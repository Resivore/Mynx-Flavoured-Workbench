package dev.resivore.dragonbound.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RecipeContractTest {
    private static final Path RECIPE_ROOT = Path.of("src/main/resources/data/dragonbound_waystone/recipe");

    @Test
    void waystoneRecipeHasExactBottomWeightedPatternAndTwoEndStoneBricks() throws IOException {
        JsonObject recipe = recipe("dragonbound_waystone.json");

        assertEquals("minecraft:crafting_shaped", string(recipe, "type"));
        assertEquals(Set.of("   ", " E ", "BFB"), orderedPatternAsSet(recipe));
        assertEquals("   ", pattern(recipe).get(0).getAsString());
        assertEquals(" E ", pattern(recipe).get(1).getAsString());
        assertEquals("BFB", pattern(recipe).get(2).getAsString());

        JsonObject key = recipe.getAsJsonObject("key");
        assertEquals("minecraft:dragon_egg", string(key, "E"));
        assertEquals("minecraft:nether_star", string(key, "F"));
        assertEquals("minecraft:end_stone_bricks", string(key, "B"));
        assertEquals(2, characterCount(pattern(recipe), 'B'));
        assertResult(recipe, "dragonbound_waystone:dragonbound_waystone");
    }

    @Test
    void imbuedVoidPearlRecipeIsExactlyPearlAndDragonBreath() throws IOException {
        JsonObject recipe = recipe("imbued_void_pearl.json");
        JsonArray ingredients = recipe.getAsJsonArray("ingredients");
        Set<String> ids = new HashSet<>();
        ingredients.forEach(element -> ids.add(element.getAsString()));

        assertEquals("minecraft:crafting_shapeless", string(recipe, "type"));
        assertEquals(2, ingredients.size());
        assertEquals(Set.of("minecraft:ender_pearl", "minecraft:dragon_breath"), ids);
        assertResult(recipe, "dragonbound_waystone:imbued_void_pearl");
    }

    @Test
    void staffRecipeUsesExactVerticalPearlFavourAndStickPattern() throws IOException {
        JsonObject recipe = recipe("dragonbound_staff.json");
        JsonArray pattern = pattern(recipe);

        assertEquals(" P ", pattern.get(0).getAsString());
        assertEquals(" F ", pattern.get(1).getAsString());
        assertEquals(" S ", pattern.get(2).getAsString());
        assertEquals(1, characterCount(pattern, 'P'));
        assertEquals(1, characterCount(pattern, 'F'));
        assertEquals(1, characterCount(pattern, 'S'));

        JsonObject key = recipe.getAsJsonObject("key");
        assertEquals("dragonbound_waystone:imbued_void_pearl", string(key, "P"));
        assertEquals("minecraft:nether_star", string(key, "F"));
        assertEquals("minecraft:stick", string(key, "S"));
        assertEquals(3, key.size());
        assertResult(recipe, "dragonbound_waystone:dragonbound_staff");
    }

    private static JsonObject recipe(String filename) throws IOException {
        return JsonParser.parseString(Files.readString(RECIPE_ROOT.resolve(filename))).getAsJsonObject();
    }

    private static JsonArray pattern(JsonObject recipe) {
        return recipe.getAsJsonArray("pattern");
    }

    private static Set<String> orderedPatternAsSet(JsonObject recipe) {
        Set<String> pattern = new HashSet<>();
        recipe.getAsJsonArray("pattern").forEach(element -> pattern.add(element.getAsString()));
        return pattern;
    }

    private static int characterCount(JsonArray pattern, char target) {
        int count = 0;
        for (int row = 0; row < pattern.size(); row++) {
            String value = pattern.get(row).getAsString();
            for (int index = 0; index < value.length(); index++) {
                if (value.charAt(index) == target) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void assertResult(JsonObject recipe, String expectedId) {
        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals(expectedId, string(result, "id"));
        assertEquals(1, result.get("count").getAsInt());
    }

    private static String string(JsonObject object, String key) {
        return object.get(key).getAsString();
    }
}
