package dev.resivore.inventorysortercsrcompat.core;

import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** The one compatibility predicate shared by the server sorter and client fallback planner. */
public final class FixedSortSlots {
    private FixedSortSlots() {}

    public static boolean isFixed(Container container, int localSlot, ItemStack stack) {
        return ContainerSlotReservationsApi.isReserved(container, localSlot) || isPortableContainer(stack);
    }

    public static boolean isFixed(Slot slot) {
        return isFixed(slot.container, slot.getContainerSlot(), slot.getItem());
    }

    public static boolean isPortableContainer(ItemStack stack) {
        return stack.is(Items.BUNDLE)
                || stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }
}
