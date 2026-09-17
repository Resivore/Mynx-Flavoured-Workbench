package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.client.CarriedShulkerMouseTweaks;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional exact seam before Mouse Tweaks rejects cursor-shulker/ordinary-stack mismatches. */
@Pseudo
@Mixin(targets = "yalter.mousetweaks.Main", remap = false)
abstract class MouseTweaksMainMixin {
    @Shadow private static Slot oldSelectedSlot;

    @Inject(method = "rmbTweakMaybeClickSlot", at = @At("HEAD"), cancellable = true,
            require = 1, remap = false)
    private static void containerSlotReservations$beforeNativeRmbSlotEntry(
            Slot slot, ItemStack mouseTweaksSnapshot, CallbackInfo callbackInfo) {
        if (CarriedShulkerMouseTweaks.beforeMouseTweaksSlot(slot)) callbackInfo.cancel();
    }

    @Inject(method = "rmbTweakMaybeClickSlot", at = @At("RETURN"), require = 1, remap = false)
    private static void containerSlotReservations$afterNativeRmbSlotEntry(
            Slot slot, ItemStack mouseTweaksSnapshot, CallbackInfo callbackInfo) {
        CarriedShulkerMouseTweaks.afterMouseTweaksSlot(slot);
    }

    @Inject(method = "onMouseDrag", at = @At("RETURN"), require = 1, remap = false)
    private static void containerSlotReservations$observeNativeRmbSlotIdentity(
            CallbackInfoReturnable<Boolean> callbackInfo) {
        CarriedShulkerMouseTweaks.observeMouseTweaksDrag(oldSelectedSlot);
    }
}
