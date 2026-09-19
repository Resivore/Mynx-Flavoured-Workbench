package dev.resivore.enderscapepruning;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeMap;

import java.util.ArrayList;

/** Removes only C1 recipe identities after every datapack reload. */
public final class PruningRecipeMap {
    private PruningRecipeMap() {
    }

    public static RecipeMap removeSuppressed(RecipeMap resolved) {
        ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>> recipes = new ArrayList<>(resolved.values());
        recipes.removeIf(holder -> PruningContract.BLOCKED_RECIPE_IDS.contains(holder.id().identifier().toString()));
        return RecipeMap.create(recipes);
    }
}
