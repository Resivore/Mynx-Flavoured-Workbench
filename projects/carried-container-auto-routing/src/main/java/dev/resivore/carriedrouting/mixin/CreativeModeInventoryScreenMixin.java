package dev.resivore.carriedrouting.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.resivore.carriedrouting.compat.CreativeMenuSlotTracker;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenMixin {
    @Unique private int carriedRouting$trackedSlotCapacity;

    @Inject(method = "selectTab", at = @At("HEAD"))
    private void carriedRouting$captureTrackedCapacity(CreativeModeTab tab, CallbackInfo ci) {
        int currentSlots = ((CreativeModeInventoryScreen) (Object) this).getMenu().slots.size();
        this.carriedRouting$trackedSlotCapacity = Math.max(
                this.carriedRouting$trackedSlotCapacity,
                currentSlots
        );
    }

    /**
     * Creative's inventory tab directly replaces the picker menu's 54 slots with
     * wrappers around the live InventoryMenu. Inventory Extended makes that list
     * longer than 54, but the direct additions do not grow AbstractContainerMenu's
     * listener/remote snapshots. Use the normal addSlot path only once the existing
     * tracked capacity is exhausted; subsequent tab switches reuse that capacity.
     */
    @WrapOperation(
            method = "selectTab",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/NonNullList;add(Ljava/lang/Object;)Z"
            )
    )
    private boolean carriedRouting$keepCreativeMenuSnapshotsAligned(
            NonNullList<Object> receiver,
            Object value,
            Operation<Boolean> original
    ) {
        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
        AbstractContainerMenu menu = screen.getMenu();
        if (receiver != (Object) menu.slots || !(value instanceof Slot slot)) {
            return original.call(receiver, value);
        }

        int menuIndex = menu.slots.size();
        slot.index = menuIndex;
        if (menuIndex >= this.carriedRouting$trackedSlotCapacity) {
            ((CreativeMenuSlotTracker) menu).carriedRouting$addTrackedSlot(slot);
            this.carriedRouting$trackedSlotCapacity++;
            return true;
        }
        return original.call(receiver, value);
    }
}
