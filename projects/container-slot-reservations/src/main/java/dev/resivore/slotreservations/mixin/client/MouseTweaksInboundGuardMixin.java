package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.client.CarriedShulkerRmbCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents exact Mouse Tweaks 2.31 from mutating an owned inbound RMB drag before Screen input. */
@Pseudo
@Mixin(targets = "yalter.mousetweaks.Main", remap = false)
abstract class MouseTweaksInboundGuardMixin {
    @Inject(method = "onMouseDrag", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void containerSlotReservations$keepOwnedInboundDragServerAuthoritative(
            CallbackInfoReturnable<Boolean> callbackInfo) {
        if (CarriedShulkerRmbCollector.ownsInboundRmb()) callbackInfo.setReturnValue(false);
    }
}
