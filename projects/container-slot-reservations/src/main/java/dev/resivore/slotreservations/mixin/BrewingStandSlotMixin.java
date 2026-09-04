package dev.resivore.slotreservations.mixin;

import dev.resivore.slotreservations.NativeInsertionPolicy;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Post-native hook shared by the three private BrewingStandMenu slot subtypes. */
@Mixin(targets = {
        "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot",
        "net.minecraft.world.inventory.BrewingStandMenu$IngredientsSlot",
        "net.minecraft.world.inventory.BrewingStandMenu$FuelSlot"
})
public abstract class BrewingStandSlotMixin {
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
