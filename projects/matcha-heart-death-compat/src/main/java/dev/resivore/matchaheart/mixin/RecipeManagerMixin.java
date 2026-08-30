package dev.resivore.matchaheart.mixin;

import dev.resivore.matchaheart.AuthoritativeData;
import dev.resivore.matchaheart.HeartDataContract;
import dev.resivore.matchaheart.MatchaHeartDeathCompat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin {
    private static final Identifier REINFORCED_TARGET = Identifier.parse(HeartDataContract.REINFORCED_RECIPE_ID);

    @Shadow @Final private HolderLookup.Provider registries;

    @ModifyVariable(
            method = "apply(Lnet/minecraft/world/item/crafting/RecipeMap;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private RecipeMap matchaHeart$enforceRecipes(RecipeMap resolved) {
        try {
            LinkedHashMap<Identifier, Recipe<?>> replacements = new LinkedHashMap<>();
            HeartDataContract.RECIPE_RESOURCES.forEach((id, path) -> replacements.put(
                    Identifier.parse(id), AuthoritativeData.decodeRecipe(path, this.registries)));

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
            MatchaHeartDeathCompat.LOGGER.info(
                    "Enforced authoritative recipe contracts for {} and verified {} after recipe preparation",
                    replacements.keySet(), REINFORCED_TARGET);
            return RecipeMap.create(recipes);
        } catch (RuntimeException exception) {
            MatchaHeartDeathCompat.LOGGER.error(
                    "FATAL: could not enforce authoritative recipe contracts; aborting data reload", exception);
            throw new IllegalStateException("Unsafe Matcha recipe contracts", exception);
        }
    }
}
