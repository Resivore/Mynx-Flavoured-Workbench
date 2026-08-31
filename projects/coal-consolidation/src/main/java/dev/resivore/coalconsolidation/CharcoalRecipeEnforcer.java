package dev.resivore.coalconsolidation;

import java.util.ArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Enforces only the effective furnace and Matcha smoker charcoal routes after pack resolution. */
public final class CharcoalRecipeEnforcer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Coal Consolidation");
    private static final ResourceKey<Recipe<?>> FURNACE_CHARCOAL_RECIPE = ResourceKey.create(
            Registries.RECIPE,
            Identifier.parse("minecraft:charcoal"));
    private static final ResourceKey<Recipe<?>> SMOKER_CHARCOAL_RECIPE = ResourceKey.create(
            Registries.RECIPE,
            Identifier.parse("smoking:charcoal"));

    private CharcoalRecipeEnforcer() {}

    public static RecipeMap enforce(RecipeMap resolved) {
        try {
            ArrayList<RecipeHolder<?>> recipes = new ArrayList<>(resolved.values());
            int furnaceIndex = findUnique(recipes, FURNACE_CHARCOAL_RECIPE, "minecraft:charcoal");
            int smokerIndex = findUnique(recipes, SMOKER_CHARCOAL_RECIPE, "smoking:charcoal");

            RecipeHolder<?> furnaceHolder = recipes.get(furnaceIndex);
            if (furnaceHolder.value().getClass() != SmeltingRecipe.class) {
                throw new IllegalStateException(
                        "Resolved minecraft:charcoal is not a smelting recipe: "
                                + furnaceHolder.value().getClass().getName());
            }
            SmeltingRecipe furnaceRecipe = (SmeltingRecipe) furnaceHolder.value();

            RecipeHolder<?> smokerHolder = recipes.get(smokerIndex);
            if (smokerHolder.value().getClass() != SmokingRecipe.class) {
                throw new IllegalStateException(
                        "Resolved smoking:charcoal is not a smoking recipe: "
                                + smokerHolder.value().getClass().getName());
            }
            SmokingRecipe smokerRecipe = (SmokingRecipe) smokerHolder.value();

            SmeltingRecipe furnaceReplacement = new SmeltingRecipe(
                    new Recipe.CommonInfo(furnaceRecipe.showNotification()),
                    new AbstractCookingRecipe.CookingBookInfo(furnaceRecipe.category(), furnaceRecipe.group()),
                    furnaceRecipe.input(),
                    new ItemStackTemplate(Items.COAL),
                    furnaceRecipe.experience(),
                    furnaceRecipe.cookingTime());
            SmokingRecipe smokerReplacement = new SmokingRecipe(
                    new Recipe.CommonInfo(smokerRecipe.showNotification()),
                    new AbstractCookingRecipe.CookingBookInfo(smokerRecipe.category(), smokerRecipe.group()),
                    smokerRecipe.input(),
                    new ItemStackTemplate(Items.COAL),
                    smokerRecipe.experience(),
                    smokerRecipe.cookingTime());
            recipes.set(furnaceIndex, new RecipeHolder<>(furnaceHolder.id(), furnaceReplacement));
            recipes.set(smokerIndex, new RecipeHolder<>(smokerHolder.id(), smokerReplacement));

            RecipeMap enforced = RecipeMap.create(recipes);
            LOGGER.info(
                    "Enforced minecraft:charcoal furnace and smoking:charcoal smoker outputs "
                            + "as one minecraft:coal after recipe reload");
            return enforced;
        } catch (RuntimeException exception) {
            LOGGER.error("FATAL: could not enforce owned charcoal cooking outputs", exception);
            throw new IllegalStateException("Unsafe charcoal cooking recipe contracts", exception);
        }
    }

    private static int findUnique(
            ArrayList<RecipeHolder<?>> recipes,
            ResourceKey<Recipe<?>> target,
            String id) {
        int targetIndex = -1;
        for (int index = 0; index < recipes.size(); index++) {
            if (recipes.get(index).id().equals(target)) {
                if (targetIndex != -1) {
                    throw new IllegalStateException("Duplicate resolved " + id + " recipe");
                }
                targetIndex = index;
            }
        }
        if (targetIndex == -1) {
            throw new IllegalStateException("Missing resolved " + id + " recipe");
        }
        return targetIndex;
    }
}
