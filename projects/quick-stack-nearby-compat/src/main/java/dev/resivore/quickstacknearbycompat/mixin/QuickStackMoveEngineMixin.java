package dev.resivore.quickstacknearbycompat.mixin;

import dev.resivore.quickstacknearbycompat.core.CsrQuickStackIntegration;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;

import java.util.OptionalInt;

@Mixin(value = QuickStackMoveEngine.class, remap = false)
public abstract class QuickStackMoveEngineMixin {
    @Inject(
            method = "insertIntoEmptySlots(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/Container;)I",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void quickStackNearbyCompat$prioritizeMatchingReservations(
            ItemStack sourceStack,
            Container target,
            CallbackInfoReturnable<Integer> callback) {
        OptionalInt moved = CsrQuickStackIntegration.insertIntoEmptySlots(sourceStack, target);
        if (moved.isPresent()) {
            callback.setReturnValue(moved.getAsInt());
        }
    }
}
