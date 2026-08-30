package dev.resivore.matchaheart.mixin;

import dev.resivore.matchaheart.RecipeMapEnforcer;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin {
    @Shadow @Final private HolderLookup.Provider registries;

    @ModifyVariable(
            method = "apply(Lnet/minecraft/world/item/crafting/RecipeMap;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private RecipeMap matchaHeart$enforceRecipes(RecipeMap resolved) {
        return RecipeMapEnforcer.enforce(resolved, this.registries);
    }
}
