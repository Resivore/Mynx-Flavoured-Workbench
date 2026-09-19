package dev.resivore.enderscapepruning.mixin;

import dev.resivore.enderscapepruning.MatchaHealing;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Exact 3.0.2 bridge for the Cake Roll's direct FoodData.eat call. */
@Mixin(targets = "net.penumbra.enderscape.block.ChorusCakeRollBlock", remap = false)
abstract class ChorusCakeRollMixin {
    @Redirect(
            method = "eat",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;canEat(Z)Z"),
            require = 1,
            remap = false)
    private static boolean enderscapePruning$allowCakeHealingAtFullHunger(Player player, boolean canAlwaysEat) {
        return true;
    }

    @Redirect(
            method = "eat",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"),
            require = 1,
            remap = false)
    private static void enderscapePruning$skipCakeHunger(FoodData foodData, int nutrition, float saturation) {
        // Cake retains all upstream bite, comparator, sound, particle, and final teleport logic.
    }

    @Inject(method = "eat", at = @At("HEAD"), require = 1, remap = false)
    private static void enderscapePruning$applyCakeMatchaHealing(
            LevelAccessor level,
            BlockPos pos,
            BlockState state,
            Player player,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (level instanceof ServerLevel server) {
            MatchaHealing.grantTwoHearts(server, player);
        }
    }
}
