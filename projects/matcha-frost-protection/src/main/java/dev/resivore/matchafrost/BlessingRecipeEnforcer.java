package dev.resivore.matchafrost;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

public final class BlessingRecipeEnforcer {
    static final Identifier TARGET_RECIPE =
            Identifier.parse("blessings:frost_walker_frost_protection");
    static final String CANONICAL_RESOURCE =
            "matcha_frost_protection/canonical/blessing_of_demeter.json";

    private BlessingRecipeEnforcer() {}

    public static RecipeMap enforce(RecipeMap resolved, HolderLookup.Provider registries) {
        return enforce(resolved, () -> AuthoritativeData.decodeRecipe(CANONICAL_RESOURCE, registries));
    }

    static RecipeMap enforce(
            RecipeMap resolved,
            Supplier<? extends Recipe<?>> replacementLoader) {
        try {
            ArrayList<RecipeHolder<?>> recipes = new ArrayList<>(resolved.values());
            int targetIndex = -1;
            for (int index = 0; index < recipes.size(); index++) {
                if (recipes.get(index).id().identifier().equals(TARGET_RECIPE)) {
                    if (targetIndex != -1) {
                        throw new IllegalStateException("Duplicate resolved Blessing of Demeter recipe");
                    }
                    targetIndex = index;
                }
            }
            if (targetIndex == -1) {
                throw new IllegalStateException("Missing resolved Blessing of Demeter recipe");
            }

            Recipe<?> replacement = Objects.requireNonNull(
                    replacementLoader.get(), "Canonical Blessing of Demeter recipe was null");
            RecipeHolder<?> original = recipes.get(targetIndex);
            recipes.set(targetIndex, new RecipeHolder<>(original.id(), replacement));

            RecipeMap enforced = RecipeMap.create(recipes);
            MatchaFrostProtection.LOGGER.info(
                    "Enforced the authoritative Blessing of Demeter result for {}",
                    TARGET_RECIPE);
            return enforced;
        } catch (RuntimeException exception) {
            MatchaFrostProtection.LOGGER.error(
                    "FATAL: could not enforce the Blessing of Demeter result", exception);
            throw new IllegalStateException("Unsafe Blessing of Demeter recipe contract", exception);
        }
    }
}
