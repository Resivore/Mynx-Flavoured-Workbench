package dev.resivore.slotreservations.mixin;

import dev.resivore.slotreservations.DoubleBarrelBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Attaches ephemeral Double Barrels views to their verified physical owners without hard linkage. */
@Mixin(value = BarrelBlockEntity.class, priority = 900)
public abstract class DoubleBarrelIntegrationMixin {
    @Inject(
            method = "getCombinedInventory()Lnet/minecraft/world/Container;",
            at = @At("RETURN"),
            cancellable = false,
            require = 0,
            remap = false
    )
    private void containerSlotReservations$attachCombinedInventory(
            CallbackInfoReturnable<Container> callbackInfo
    ) {
        Container combined = callbackInfo.getReturnValue();
        if (combined != null) {
            DoubleBarrelBridge.associate((BarrelBlockEntity) (Object) this, combined);
        }
    }

    /** Existing vanilla seam provides a fail-safe association for the menu wrapper itself. */
    @Inject(
            method = "createMenu(ILnet/minecraft/world/entity/player/Inventory;)Lnet/minecraft/world/inventory/AbstractContainerMenu;",
            at = @At("RETURN"),
            require = 1
    )
    private void containerSlotReservations$attachReturnedMenu(
            int containerId,
            Inventory inventory,
            CallbackInfoReturnable<AbstractContainerMenu> callbackInfo
    ) {
        if (callbackInfo.getReturnValue() instanceof ChestMenu menu) {
            DoubleBarrelBridge.associate((BarrelBlockEntity) (Object) this, menu.getContainer());
        }
    }
}
