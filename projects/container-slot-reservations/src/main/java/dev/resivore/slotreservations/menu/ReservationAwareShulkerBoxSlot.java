package dev.resivore.slotreservations.menu;

import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.SupportedContainerResolver;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.ItemStack;

/** Retains vanilla's no-container-items rule before applying reservation admissibility. */
public final class ReservationAwareShulkerBoxSlot extends ShulkerBoxSlot {
    public ReservationAwareShulkerBoxSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack incoming) {
        return super.mayPlace(incoming)
                && SupportedContainerResolver.resolve(container, getContainerSlot())
                .map(resolved -> ReservationStore.reservationAllows(resolved, incoming))
                .orElse(true);
    }
}
