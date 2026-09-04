package dev.resivore.slotreservations.mixin;

import dev.resivore.slotreservations.NativeInsertionPolicy;
import net.minecraft.world.inventory.FurnaceFuelSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Retains the exact FurnaceFuelSlot subtype and appends reservation admission. */
@Mixin(FurnaceFuelSlot.class)
public abstract class FurnaceFuelSlotMixin {
    @Inject(method = "mayPlace", at = @At("RETURN"), cancellable = true)
    private void containerSlotReservations$afterNativeAdmission(
            ItemStack incoming,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        Slot self = (Slot) (Object) this;
        callbackInfo.setReturnValue(NativeInsertionPolicy.applyReservation(
                self.container,
                self.getContainerSlot(),
                incoming,
                callbackInfo.getReturnValue()
        ));
    }
}
