package dev.resivore.coalconsolidation.mixin;

import dev.resivore.coalconsolidation.CharcoalRecipeEnforcer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin {
    @ModifyVariable(
            method = "apply(Lnet/minecraft/world/item/crafting/RecipeMap;"
                    + "Lnet/minecraft/server/packs/resources/ResourceManager;"
                    + "Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 1)
    private RecipeMap coalConsolidation$enforceRecipes(RecipeMap resolved) {
        return CharcoalRecipeEnforcer.enforce(resolved);
    }
}
