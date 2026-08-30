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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Enforces only the effective vanilla furnace charcoal route after pack resolution. */
public final class CharcoalRecipeEnforcer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Coal Consolidation");
    private static final ResourceKey<Recipe<?>> CHARCOAL_RECIPE = ResourceKey.create(
            Registries.RECIPE,
            Identifier.parse("minecraft:charcoal"));

    private CharcoalRecipeEnforcer() {}

    public static RecipeMap enforce(RecipeMap resolved) {
        try {
            ArrayList<RecipeHolder<?>> recipes = new ArrayList<>(resolved.values());
            int targetIndex = -1;

            for (int index = 0; index < recipes.size(); index++) {
                if (recipes.get(index).id().equals(CHARCOAL_RECIPE)) {
                    if (targetIndex != -1) {
                        throw new IllegalStateException("Duplicate resolved minecraft:charcoal recipe");
                    }
                    targetIndex = index;
                }
            }

            if (targetIndex == -1) {
                throw new IllegalStateException("Missing resolved minecraft:charcoal recipe");
            }

            RecipeHolder<?> originalHolder = recipes.get(targetIndex);
            if (!(originalHolder.value() instanceof SmeltingRecipe original)) {
                throw new IllegalStateException(
                        "Resolved minecraft:charcoal is not a smelting recipe: "
                                + originalHolder.value().getClass().getName());
            }

            SmeltingRecipe replacement = new SmeltingRecipe(
                    new Recipe.CommonInfo(original.showNotification()),
                    new AbstractCookingRecipe.CookingBookInfo(original.category(), original.group()),
                    original.input(),
                    new ItemStackTemplate(Items.COAL),
                    original.experience(),
                    original.cookingTime());
            recipes.set(targetIndex, new RecipeHolder<>(originalHolder.id(), replacement));

            RecipeMap enforced = RecipeMap.create(recipes);
            LOGGER.info("Enforced minecraft:charcoal furnace output as one minecraft:coal after recipe reload");
            return enforced;
        } catch (RuntimeException exception) {
            LOGGER.error("FATAL: could not enforce minecraft:charcoal furnace output", exception);
            throw new IllegalStateException("Unsafe minecraft:charcoal recipe contract", exception);
        }
    }
}
