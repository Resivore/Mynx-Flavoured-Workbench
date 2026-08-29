package dev.resivore.coalconsolidation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RecipeContractTest {
    private static final Path DATA = Path.of("src", "main", "resources", "data");
    private static final List<Path> EXPLICIT_CONSUMER_OVERRIDES = List.of(
            Path.of("minecraft", "recipe", "copper_torch.json"),
            Path.of("minecraft", "recipe", "fire_charge.json"),
            Path.of("minecraft", "recipe", "soul_torch.json"),
            Path.of("minecraft", "recipe", "torch.json"),
            Path.of("crafting", "recipe", "black_dye.json"),
            Path.of("crafting", "recipe", "fire_charge.json"),
            Path.of("crafting", "recipe", "torch.json"));

    @Test
    void vanillaSmeltingKeepsEveryBurnableLogAndWoodVariantButProducesCoal() throws IOException {
        JsonObject recipe = recipe("minecraft/recipe/charcoal.json");

        assertEquals("minecraft:smelting", string(recipe, "type"));
        assertEquals("#minecraft:logs_that_burn", string(recipe, "ingredient"));
        assertEquals(0.15F, recipe.get("experience").getAsFloat());
        assertFalse(recipe.has("cookingtime"), "omission preserves vanilla's 200-tick smelting default");
        assertResult(recipe, "minecraft:coal", 1);
    }

    @Test
    void matchaSmokingKeepsItsLogTagTimingAndExperienceButProducesCoal() throws IOException {
        JsonObject recipe = recipe("smoking/recipe/charcoal.json");

        assertEquals("minecraft:smoking", string(recipe, "type"));
        assertEquals("#minecraft:logs_that_burn", string(recipe, "ingredient"));
        assertEquals(0.15F, recipe.get("experience").getAsFloat());
        assertEquals(100, recipe.get("cookingtime").getAsInt());
        assertResult(recipe, "minecraft:coal", 1);
    }

    @Test
    void sharedVanillaTagPreservesRepresentativeOakNonOakAndBarkInputs() throws IOException {
        for (String path : List.of("minecraft/recipe/charcoal.json", "smoking/recipe/charcoal.json")) {
            assertEquals("#minecraft:logs_that_burn", string(recipe(path), "ingredient"),
                    "Minecraft 26.2 expands this tag over oak_log, spruce_log, and oak_wood representatives");
        }
    }

    @Test
    void legacyConversionIsExactlyOneWayAndOneToOne() throws IOException {
        JsonObject migration = recipe("coal_consolidation/recipe/charcoal_to_coal.json");
        assertEquals("minecraft:crafting_shapeless", string(migration, "type"));
        assertEquals(1, migration.getAsJsonArray("ingredients").size());
        assertEquals("minecraft:charcoal", migration.getAsJsonArray("ingredients").get(0).getAsString());
        assertResult(migration, "minecraft:coal", 1);

        for (Path path : recipeFiles()) {
            JsonObject candidate = parse(path);
            assertFalse(hasResult(candidate, "minecraft:charcoal"),
                    () -> "coal-to-charcoal or another charcoal output remains in " + DATA.relativize(path));
        }
    }

    @Test
    void noAuditedCookingRecipeStillOutputsCharcoal() throws IOException {
        for (Path path : recipeFiles()) {
            JsonObject candidate = parse(path);
            String type = string(candidate, "type");
            if (type.equals("minecraft:smelting") || type.equals("minecraft:smoking")) {
                assertFalse(hasResult(candidate, "minecraft:charcoal"),
                        () -> "active cooking output is still charcoal in " + DATA.relativize(path));
            }
        }
    }

    @Test
    void everyExplicitVanillaAndMatchaCharcoalConsumerNowUsesCoal() throws IOException {
        for (Path relative : EXPLICIT_CONSUMER_OVERRIDES) {
            String body = Files.readString(DATA.resolve(relative));
            assertTrue(body.contains("minecraft:coal"), () -> "coal missing from " + relative);
            assertFalse(body.contains("minecraft:charcoal"), () -> "charcoal remains in " + relative);
        }
    }

    @Test
    void ordinaryCoalsTagRecipesAndUnrelatedCoalRecipesRemainUntouched() throws IOException {
        assertFalse(Files.exists(DATA.resolve("minecraft/recipe/campfire.json")));
        assertFalse(Files.exists(DATA.resolve("crafting/recipe/coal_alternate.json")));
        assertFalse(Files.exists(DATA.resolve("minecraft/tags/item/coals.json")));

        Set<String> minecraftOverrides = directJsonNames(DATA.resolve("minecraft/recipe"));
        assertEquals(Set.of("charcoal.json", "copper_torch.json", "fire_charge.json", "soul_torch.json", "torch.json"),
                minecraftOverrides);
    }

    @Test
    void charcoalLiteralExistsOnlyAsTheLegacyMigrationInput() throws IOException {
        List<Path> references = recipeFiles().stream()
                .filter(path -> {
                    try {
                        return Files.readString(path).contains("minecraft:charcoal");
                    } catch (IOException exception) {
                        throw new IllegalStateException(exception);
                    }
                })
                .toList();

        assertEquals(List.of(DATA.resolve("coal_consolidation/recipe/charcoal_to_coal.json")), references);
    }

    private static JsonObject recipe(String relative) throws IOException {
        return parse(DATA.resolve(relative));
    }

    private static JsonObject parse(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static List<Path> recipeFiles() throws IOException {
        try (var files = Files.walk(DATA)) {
            return files.filter(path -> path.toString().endsWith(".json"))
                    .filter(path -> path.getParent() != null
                            && path.getParent().getFileName().toString().equals("recipe"))
                    .sorted()
                    .toList();
        }
    }

    private static Set<String> directJsonNames(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return files.filter(path -> path.toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null ? "" : value.getAsString();
    }

    private static boolean hasResult(JsonObject recipe, String id) {
        JsonObject result = recipe.getAsJsonObject("result");
        return result != null && id.equals(string(result, "id"));
    }

    private static void assertResult(JsonObject recipe, String id, int count) {
        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals(id, string(result, "id"));
        assertEquals(count, result.has("count") ? result.get("count").getAsInt() : 1);
    }
}
