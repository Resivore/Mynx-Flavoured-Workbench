package dev.resivore.slotreservations.mixin;

import dev.resivore.slotreservations.NativeInsertionPolicy;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin {
    @Inject(method = "canPlaceItem", at = @At("RETURN"), cancellable = true)
    private void containerSlotReservations$afterNativeAdmission(
            int slot,
            ItemStack incoming,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        callbackInfo.setReturnValue(NativeInsertionPolicy.applyReservation(
                (Container) (Object) this,
                slot,
                incoming,
                callbackInfo.getReturnValue()
        ));
    }
}
