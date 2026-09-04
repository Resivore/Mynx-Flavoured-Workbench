package dev.resivore.slotreservations.mixin;

import dev.resivore.slotreservations.NativeInsertionPolicy;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Post-native admission hook for ordinary Slot instances; unsupported owners are unchanged. */
@Mixin(Slot.class)
public abstract class SlotMixin {
    @Shadow public Container container;

    @Shadow public abstract int getContainerSlot();

    @Inject(method = "mayPlace", at = @At("RETURN"), cancellable = true)
    private void containerSlotReservations$afterNativeAdmission(
            ItemStack incoming,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        callbackInfo.setReturnValue(NativeInsertionPolicy.applyReservation(
                container,
                getContainerSlot(),
                incoming,
                callbackInfo.getReturnValue()
        ));
    }

    @Inject(method = "allowModification", at = @At("HEAD"))
    private void containerSlotReservations$beginNonInsertionQuery(
            Player player,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        NativeInsertionPolicy.beginNonInsertionQuery();
    }

    @Inject(method = "allowModification", at = @At("RETURN"))
    private void containerSlotReservations$endNonInsertionQuery(
            Player player,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        NativeInsertionPolicy.endNonInsertionQuery();
    }
}
