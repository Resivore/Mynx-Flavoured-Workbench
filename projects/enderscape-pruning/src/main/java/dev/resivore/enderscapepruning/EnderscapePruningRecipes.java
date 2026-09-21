package dev.resivore.enderscapepruning;

import dev.resivore.enderscapepruning.recipe.MatchaVoidCampfireRecipe;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Registers C2's one component-aware recipe serializer before datapacks load. */
public final class EnderscapePruningRecipes {
    public static final RecipeSerializer<MatchaVoidCampfireRecipe> MATCHA_VOID_CAMPFIRE =
            new RecipeSerializer<>(MatchaVoidCampfireRecipe.CODEC, MatchaVoidCampfireRecipe.STREAM_CODEC);

    private EnderscapePruningRecipes() {
    }

    public static void register() {
        Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(EnderscapePruning.MOD_ID, "matcha_void_campfire"),
                MATCHA_VOID_CAMPFIRE);
        // Fabric only synchronizes opt-in mod serializers. JEI consumes that
        // synchronized RecipeHolder and its real RecipeDisplay directly.
        RecipeSynchronization.synchronizeRecipeSerializer(MATCHA_VOID_CAMPFIRE);
    }
}
