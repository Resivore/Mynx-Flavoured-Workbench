package com.fizzware.dramaticdoors.mixin;

import java.util.LinkedHashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.fizzware.dramaticdoors.compat.DDCompatRecipe;
import com.google.gson.JsonObject;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin 
{
    @Shadow
    @Final
    private HolderLookup.Provider registries;

    @Shadow
    protected static RecipeHolder<?> fromJson(ResourceKey<Recipe<?>> recipeId, JsonObject recipeJson, HolderLookup.Provider registries) {
        throw new AssertionError();
    }

    @Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/world/item/crafting/RecipeMap;", at = @At("RETURN"), cancellable = true)
    private void interceptPrepare(ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfoReturnable<RecipeMap> cir) {
        Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> recipes = new LinkedHashMap<>();
        for (RecipeHolder<?> recipe : cir.getReturnValue().values()) {
            recipes.put(recipe.id(), recipe);
        }

        DDCompatRecipe.SHORT_DOOR_RECIPES.forEach(recipe -> addRecipe(recipes, recipe));
        DDCompatRecipe.TALL_DOOR_RECIPES.forEach(recipe -> addRecipe(recipes, recipe));
        cir.setReturnValue(RecipeMap.create(recipes.values()));
    }

    private void addRecipe(Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> recipes, JsonObject recipeJson) {
        Identifier recipeId = Identifier.parse(recipeJson.getAsJsonObject("result").get("id").getAsString());
        ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, recipeId);
        recipes.put(recipeKey, fromJson(recipeKey, recipeJson, registries));
    }
}
