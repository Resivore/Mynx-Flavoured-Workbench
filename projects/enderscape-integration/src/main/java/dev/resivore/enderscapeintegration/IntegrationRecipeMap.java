package dev.resivore.enderscapeintegration;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeMap;

import java.util.ArrayList;

/** Removes only C1 recipe identities after every datapack reload. */
public final class IntegrationRecipeMap {
    private IntegrationRecipeMap() {
    }

    public static RecipeMap removeSuppressed(RecipeMap resolved) {
        ArrayList<net.minecraft.world.item.crafting.RecipeHolder<?>> recipes = new ArrayList<>(resolved.values());
        recipes.removeIf(holder -> IntegrationContract.BLOCKED_RECIPE_IDS.contains(holder.id().identifier().toString()));
        return RecipeMap.create(recipes);
    }
}
