package dev.resivore.matchaheart;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

/** Rebuilds the resolved recipe map with this project's exact authoritative recipes. */
public final class RecipeMapEnforcer {
    private static final Identifier REINFORCED_TARGET =
            Identifier.parse(HeartDataContract.REINFORCED_RECIPE_ID);

    private RecipeMapEnforcer() {}

    public static RecipeMap enforce(RecipeMap resolved, HolderLookup.Provider registries) {
        return enforce(resolved, () -> {
            LinkedHashMap<Identifier, Recipe<?>> replacements = new LinkedHashMap<>();
            HeartDataContract.RECIPE_RESOURCES.forEach((id, path) -> replacements.put(
                    Identifier.parse(id), AuthoritativeData.decodeRecipe(path, registries)));
            return replacements;
        });
    }

    static RecipeMap enforce(
            RecipeMap resolved,
            Supplier<? extends Map<Identifier, Recipe<?>>> replacementsLoader) {
        try {
            LinkedHashMap<Identifier, Recipe<?>> replacements = new LinkedHashMap<>(
                    Objects.requireNonNull(replacementsLoader.get(), "Authoritative recipe map was null"));
            ArrayList<RecipeHolder<?>> recipes = new ArrayList<>(resolved.values());
            if (recipes.stream().noneMatch(holder -> holder.id().identifier().equals(REINFORCED_TARGET))) {
                throw new IllegalStateException("Required Reinforced Crystal Heart recipe did not decode: "
                        + REINFORCED_TARGET);
            }
            recipes.removeIf(holder -> replacements.containsKey(holder.id().identifier()));
            replacements.forEach((id, recipe) -> {
                ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
                recipes.add(new RecipeHolder<>(key, recipe));
            });
            RecipeMap enforced = RecipeMap.create(recipes);
            MatchaHeartDeathCompat.LOGGER.info(
                    "Enforced authoritative recipe contracts for {} and verified {} after recipe preparation",
                    replacements.keySet(), REINFORCED_TARGET);
            return enforced;
        } catch (RuntimeException exception) {
            MatchaHeartDeathCompat.LOGGER.error(
                    "FATAL: could not enforce authoritative recipe contracts; aborting data reload", exception);
            throw new IllegalStateException("Unsafe Matcha recipe contracts", exception);
        }
    }
}
