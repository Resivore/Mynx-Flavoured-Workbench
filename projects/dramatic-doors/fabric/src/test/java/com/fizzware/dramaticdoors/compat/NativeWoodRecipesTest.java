package com.fizzware.dramaticdoors.compat;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeWoodRecipesTest {
    @Test
    void cuttingRecipesAndUnlocksUseTheSameKeyWithEverySupportedSawmillCombination() {
        check(Set.of(), List.of(""), List.of("minecraft:stonecutting"));
        check(Set.of("sawmill"), List.of(""), List.of("minecraft:stonecutting"));
        check(Set.of("woodworks"), List.of("_sawing"), List.of("woodworks:sawmill"));
        check(Set.of("aurorasdeco"), List.of("_aurorasdeco_sawmill"), List.of("aurorasdeco:woodcutting"));
        check(Set.of("woodworks", "aurorasdeco", "sawmill"), List.of("_sawing", "_aurorasdeco_sawmill"),
                List.of("woodworks:sawmill", "aurorasdeco:woodcutting"));
    }

    private static void check(Set<String> mods, List<String> suffixes, List<String> serializers) {
        var previous = Compats.modChecker;
        Compats.modChecker = new CompatChecker() {
            @Override public boolean isModLoaded(String mod) { return mods.contains(mod); }
        };
        String output = "short_murushroom_door";
        String source = "enderscape:murublight_door";
        try {
            DDCompatRecipe.createNativeWoodRecipes(output, "tall_murushroom_door", Identifier.parse(source));
            assertEquals(suffixes.size(), DDCompatRecipe.NATIVE_CUTTING_RECIPES.size());
            assertEquals(suffixes.size() + 1, DDCompatAdvancement.RECIPE_ADVANCEMENTS.size());
            for (int i = 0; i < suffixes.size(); i++) {
                Identifier id = Identifier.parse("dramaticdoors:" + output + suffixes.get(i));
                var recipe = DDCompatRecipe.NATIVE_CUTTING_RECIPES.get(id);
                assertEquals(serializers.get(i), recipe.get("type").getAsString());
                assertEquals(source, recipe.get("ingredient").getAsString());
                assertEquals("dramaticdoors:" + output, recipe.getAsJsonObject("result").get("id").getAsString());
                var advancement = DDCompatAdvancement.RECIPE_ADVANCEMENTS.get(i);
                assertEquals(id.toString(), advancement.getAsJsonObject("rewards").getAsJsonArray("recipes").get(0).getAsString());
                var criteria = advancement.getAsJsonObject("criteria");
                assertEquals(id.toString(), criteria.getAsJsonObject("has_the_recipe").getAsJsonObject("conditions").get("recipe").getAsString());
                assertEquals(source, criteria.getAsJsonObject("has_items").getAsJsonObject("conditions")
                        .getAsJsonArray("items").get(0).getAsJsonObject().getAsJsonArray("items").get(0).getAsString());
            }
        } finally {
            DDCompatRecipe.NATIVE_CUTTING_RECIPES.clear();
            DDCompatRecipe.TALL_DOOR_RECIPES.clear();
            DDCompatAdvancement.RECIPE_ADVANCEMENTS.clear();
            Compats.modChecker = previous;
        }
    }
}
