package dev.resivore.enderscapepruning.mixin;

import dev.resivore.enderscapepruning.MatchaHealing;
import dev.resivore.enderscapepruning.PruningContract;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces only Enderscape native-food hunger with current Matcha heart timing. */
@Mixin(FoodProperties.class)
abstract class FoodPropertiesMixin {
    @Redirect(
            method = "onConsume",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V"),
            require = 1)
    private void enderscapePruning$skipNativeHunger(
            FoodData foodData,
            FoodProperties food,
            Level level,
            LivingEntity user,
            ItemStack stack,
            Consumable consumable) {
        if (!PruningContract.isNativeFood(stack)) {
            foodData.eat(food);
        }
    }

    @Inject(method = "onConsume", at = @At("TAIL"), require = 1)
    private void enderscapePruning$applyMatchaNativeHealing(
            Level level,
            LivingEntity user,
            ItemStack stack,
            Consumable consumable,
            CallbackInfo ci) {
        if (level instanceof ServerLevel server && user instanceof Player player && PruningContract.isNativeFood(stack)) {
            MatchaHealing.grantOneHeart(server, player);
        }
    }
}
