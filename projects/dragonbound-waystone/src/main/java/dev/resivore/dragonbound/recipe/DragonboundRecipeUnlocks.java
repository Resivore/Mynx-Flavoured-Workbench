package dev.resivore.dragonbound.recipe;

import dev.resivore.dragonbound.DragonboundWaystone;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Makes the loaded Dragonbound crafting recipes known to each joining player. */
public final class DragonboundRecipeUnlocks {
    private static final List<ResourceKey<Recipe<?>>> DRAGONBOUND_RECIPE_KEYS = List.of(
            recipeKey("dragonbound_waystone"),
            recipeKey("waystone_material"),
            recipeKey("imbued_void_pearl"),
            recipeKey("dragonbound_staff"));

    private DragonboundRecipeUnlocks() {
    }

    public static void install() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                handler.getPlayer().awardRecipes(loadedRecipes(server.getRecipeManager())));
    }

    static List<ResourceKey<Recipe<?>>> recipeKeys() {
        return DRAGONBOUND_RECIPE_KEYS;
    }

    static List<RecipeHolder<?>> loadedRecipes(RecipeManager recipeManager) {
        return selectLoaded(recipeManager::byKey);
    }

    static <T> List<T> selectLoaded(
            Function<ResourceKey<Recipe<?>>, Optional<T>> loadedRecipeLookup) {
        return DRAGONBOUND_RECIPE_KEYS.stream()
                .map(loadedRecipeLookup)
                .flatMap(Optional::stream)
                .toList();
    }

    private static ResourceKey<Recipe<?>> recipeKey(String path) {
        return ResourceKey.create(
                Registries.RECIPE,
                Identifier.fromNamespaceAndPath(DragonboundWaystone.MOD_ID, path));
    }
}
