package dev.resivore.matchaheart.client;

import dev.resivore.matchaheart.HeartDataContract;
import dev.resivore.matchaheart.MatchaHeartDeathCompat;
import java.util.LinkedHashSet;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Exposes only this project's component-bearing recipe output to JEI search. */
@JeiPlugin
public final class HeartJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(MatchaHeartDeathCompat.MOD_ID, "jei");
    private static final Identifier RECIPE_ID = Identifier.parse(HeartDataContract.REINFORCED_RECIPE_ID);

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        var category = runtime.getRecipeManager().getRecipeCategory(RecipeTypes.CRAFTING);
        var output = new LinkedHashSet<ItemStack>();
        runtime.getRecipeManager().createRecipeLookup(RecipeTypes.CRAFTING).includeHidden().get()
                .filter(holder -> holder.id().identifier().equals(RECIPE_ID))
                .forEach(holder -> runtime.getRecipeManager().getRecipeIngredients(category, holder)
                        .getIngredients(RecipeIngredientRole.OUTPUT).stream()
                        .map(ITypedIngredient::getItemStack)
                        .flatMap(java.util.Optional::stream)
                        .map(ItemStack::copy)
                        .forEach(output::add));
        runtime.getIngredientManager().addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, output);
    }
}
