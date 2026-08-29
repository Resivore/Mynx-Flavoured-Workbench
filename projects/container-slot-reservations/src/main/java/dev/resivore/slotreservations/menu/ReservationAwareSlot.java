package dev.resivore.slotreservations.menu;

import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.SupportedContainerResolver;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Container-menu slot whose only extra policy is reservation admissibility. */
public class ReservationAwareSlot extends Slot {
    public ReservationAwareSlot(Container container, int slot, int x, int y) {
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
