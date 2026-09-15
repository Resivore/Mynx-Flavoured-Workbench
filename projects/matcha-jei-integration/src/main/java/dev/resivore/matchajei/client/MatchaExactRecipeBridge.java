package dev.resivore.matchajei.client;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.types.IRecipeType;

import java.util.List;

/**
 * Routes a selected full-component Matcha ingredient back through JEI's real
 * vanilla recipe categories. It creates no recipes and does not replace JEI's
 * built-in subtype interpretation; the temporary vanilla focus is only the
 * navigation target for the existing recipe category.
 */
final class MatchaExactRecipeBridge implements IRecipeManagerPlugin {
    private static final List<IRecipeType<?>> VANILLA_RECIPE_TYPES = List.of(
            RecipeTypes.CRAFTING,
            RecipeTypes.STONECUTTING,
            RecipeTypes.SMELTING,
            RecipeTypes.SMOKING,
            RecipeTypes.BLASTING,
            RecipeTypes.CAMPFIRE_COOKING,
            RecipeTypes.SMITHING
    );

    @Override
    public <V> List<IRecipeType<?>> getRecipeTypes(IFocus<V> focus) {
        return exactIngredient(focus) == null ? List.of() : VANILLA_RECIPE_TYPES;
    }

    @Override
    public <T, V> List<T> getRecipes(IRecipeType<T> recipeType, IFocus<V> focus) {
        MatchaExactIngredient exact = exactIngredient(focus);
        if (exact == null || !VANILLA_RECIPE_TYPES.contains(recipeType)) {
            return List.of();
        }
        return MatchaJeiRuntimeData.findVanillaRecipes(
                recipeType,
                focus.getRole(),
                exact.stack()
        );
    }

    @Override
    public <T> List<T> getRecipes(IRecipeType<T> recipeType) {
        return List.of();
    }

    private static MatchaExactIngredient exactIngredient(IFocus<?> focus) {
        if (focus.getTypedValue().getType() != MatchaExactIngredient.TYPE) {
            return null;
        }
        Object ingredient = focus.getTypedValue().getIngredient();
        return ingredient instanceof MatchaExactIngredient exact ? exact : null;
    }
}
