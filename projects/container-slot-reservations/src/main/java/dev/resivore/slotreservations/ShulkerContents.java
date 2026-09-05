package dev.resivore.slotreservations;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

public final class ShulkerContents {
    private ShulkerContents() {}

    public static NonNullList<ItemStack> copy(ItemStack shulker) {
        NonNullList<ItemStack> contents = NonNullList.withSize(ReservationData.SLOT_COUNT, ItemStack.EMPTY);
        shulker.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
        return contents;
    }

    public static void replace(ItemStack shulker, List<ItemStack> contents) {
        if (contents.size() != ReservationData.SLOT_COUNT) {
            throw new IllegalArgumentException("A shulker must have exactly 27 logical slots");
        }
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
    }

    public static int firstOccupied(List<ItemStack> contents) {
        return nextOccupied(contents, -1);
    }

    public static int nextOccupied(List<ItemStack> contents, int after) {
        for (int offset = 1; offset <= ReservationData.SLOT_COUNT; offset++) {
            int slot = Math.floorMod(after + offset, ReservationData.SLOT_COUNT);
            if (!contents.get(slot).isEmpty()) return slot;
        }
        return -1;
    }

    public static int previousOccupied(List<ItemStack> contents, int before) {
        for (int offset = 1; offset <= ReservationData.SLOT_COUNT; offset++) {
            int slot = Math.floorMod(before - offset, ReservationData.SLOT_COUNT);
            if (!contents.get(slot).isEmpty()) return slot;
        }
        return -1;
    }
}
