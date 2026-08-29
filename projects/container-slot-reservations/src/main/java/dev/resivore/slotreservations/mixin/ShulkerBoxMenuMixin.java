package dev.resivore.slotreservations.mixin;

import dev.resivore.slotreservations.menu.ReservationAwareShulkerBoxSlot;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShulkerBoxMenu.class)
public abstract class ShulkerBoxMenuMixin {
    @Redirect(
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;)V",
            at = @At(value = "NEW", target = "Lnet/minecraft/world/inventory/ShulkerBoxSlot;"),
            require = 1
    )
    private ShulkerBoxSlot containerSlotReservations$createReservationAwareSlot(
            Container container,
            int slot,
            int x,
            int y
    ) {
        return new ReservationAwareShulkerBoxSlot(container, slot, x, y);
    }
}
