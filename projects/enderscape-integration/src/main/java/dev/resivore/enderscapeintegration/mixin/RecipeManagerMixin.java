package dev.resivore.enderscapeintegration.mixin;

import dev.resivore.enderscapeintegration.IntegrationRecipeMap;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Applies recipe removal after every normal datapack recipe preparation. */
@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin {
    @ModifyVariable(
            method = "apply(Lnet/minecraft/world/item/crafting/RecipeMap;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private RecipeMap enderscapeIntegration$removeSuppressedRecipes(RecipeMap resolved) {
        return IntegrationRecipeMap.removeSuppressed(resolved);
    }
}
