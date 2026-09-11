package dev.resivore.inventorysortercsrcompat.core;

import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The one compatibility predicate shared by the server sorter and client fallback planner. */
public final class FixedSortSlots {
    private FixedSortSlots() {}

    /** A physical slot is fixed exclusively when CSR says that exact owner/local slot is reserved. */
    public static boolean isFixed(Container container, int localSlot) {
        return ContainerSlotReservationsApi.isReserved(container, localSlot);
    }

    public static boolean isFixed(Slot slot) {
        return isFixed(slot.container, slot.getContainerSlot());
    }

    /**
     * Inventory Sorter's optional content-insertion pass has bundle targets only.  This is not a
     * fixed-slot predicate: an outer bundle remains an ordinary movable ItemStack.
     */
    public static boolean isBundle(ItemStack stack) {
        return stack.is(Items.BUNDLE);
    }
}
