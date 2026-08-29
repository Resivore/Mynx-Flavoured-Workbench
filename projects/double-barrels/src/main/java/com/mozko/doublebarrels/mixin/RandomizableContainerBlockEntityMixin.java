package com.mozko.doublebarrels.mixin;

import com.mozko.doublebarrels.DoubleBarrelAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerBlockEntityMixin {
    private Container doublebarrels$combinedIfConnected() {
        if (!((Object) this instanceof BarrelBlockEntity barrel)) return null;
        DoubleBarrelAccess access = (DoubleBarrelAccess) barrel;
        return access.isConnected() ? access.getCombinedInventory() : null;
    }

    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
    private void doublebarrels$getItem(int slot, CallbackInfoReturnable<ItemStack> cir) {
        Container combined = doublebarrels$combinedIfConnected();
        if (combined != null) cir.setReturnValue(combined.getItem(slot));
    }

    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void doublebarrels$setItem(int slot, ItemStack stack, CallbackInfo ci) {
        Container combined = doublebarrels$combinedIfConnected();
        if (combined != null) {
            combined.setItem(slot, stack);
            ci.cancel();
        }
    }

    @Inject(method = "removeItem", at = @At("HEAD"), cancellable = true)
    private void doublebarrels$removeItem(
            int slot, int amount, CallbackInfoReturnable<ItemStack> cir) {
        Container combined = doublebarrels$combinedIfConnected();
        if (combined != null) cir.setReturnValue(combined.removeItem(slot, amount));
    }

    @Inject(method = "removeItemNoUpdate", at = @At("HEAD"), cancellable = true)
    private void doublebarrels$removeItemNoUpdate(
            int slot, CallbackInfoReturnable<ItemStack> cir) {
        Container combined = doublebarrels$combinedIfConnected();
        if (combined != null) cir.setReturnValue(combined.removeItemNoUpdate(slot));
    }

    @Inject(method = "isEmpty", at = @At("HEAD"), cancellable = true)
    private void doublebarrels$isEmpty(CallbackInfoReturnable<Boolean> cir) {
        Container combined = doublebarrels$combinedIfConnected();
        if (combined != null) cir.setReturnValue(combined.isEmpty());
    }
}
