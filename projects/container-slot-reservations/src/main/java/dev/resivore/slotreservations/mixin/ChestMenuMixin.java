package dev.resivore.slotreservations.mixin;

import dev.resivore.slotreservations.menu.ReservationAwareSlot;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChestMenu.class)
public abstract class ChestMenuMixin {
    @Redirect(
            method = "addChestGrid",
            at = @At(value = "NEW", target = "Lnet/minecraft/world/inventory/Slot;"),
            require = 1
    )
    private Slot containerSlotReservations$createReservationAwareSlot(
            Container container,
            int slot,
            int x,
            int y
    ) {
        return new ReservationAwareSlot(container, slot, x, y);
    }
}
