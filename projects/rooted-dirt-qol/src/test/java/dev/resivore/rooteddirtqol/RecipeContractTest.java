package dev.resivore.rooteddirtqol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));
    private static final Path RECIPE = PROJECT_ROOT.resolve(
        "src/main/resources/data/rooted_dirt_qol/recipe/rooted_dirt.json"
    );

    @Test
    void recipeIsExactlyTheRequestedShapelessConversion() throws Exception {
        JsonObject recipe = JsonParser.parseString(Files.readString(RECIPE)).getAsJsonObject();

        assertEquals(Set.of("type", "ingredients", "result"), recipe.keySet());
        assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString());

        JsonArray ingredients = recipe.getAsJsonArray("ingredients");
        assertEquals(2, ingredients.size());
        Map<String, Integer> ingredientCounts = new HashMap<>();
        ingredients.forEach(ingredient -> {
            assertTrue(ingredient.isJsonPrimitive(), "Ingredients must be single item IDs");
            ingredientCounts.merge(ingredient.getAsString(), 1, Integer::sum);
        });
        assertEquals(Map.of(
            "minecraft:dirt", 1,
            "minecraft:hanging_roots", 1
        ), ingredientCounts);

        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals(Set.of("id", "count"), result.keySet());
        assertEquals("minecraft:rooted_dirt", result.get("id").getAsString());
        assertEquals(1, result.get("count").getAsInt());
    }
}
