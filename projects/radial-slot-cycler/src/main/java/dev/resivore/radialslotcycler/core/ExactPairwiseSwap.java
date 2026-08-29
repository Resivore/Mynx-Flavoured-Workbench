package dev.resivore.radialslotcycler.core;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/** Directly exchanges two complete ItemStack references without merging. */
public final class ExactPairwiseSwap {
    private ExactPairwiseSwap() {}

    public static boolean exchange(List<ItemStack> ordinaryItems, int firstSlot, int secondSlot) {
        Objects.requireNonNull(ordinaryItems, "ordinaryItems");
        requireIndex(ordinaryItems, firstSlot);
        requireIndex(ordinaryItems, secondSlot);
        if (firstSlot == secondSlot) {
            return false;
        }

        ItemStack first = ordinaryItems.get(firstSlot);
        ItemStack second = ordinaryItems.get(secondSlot);
        ordinaryItems.set(firstSlot, second);
        ordinaryItems.set(secondSlot, first);
        return true;
    }

    private static void requireIndex(List<ItemStack> ordinaryItems, int slot) {
        if (slot < 0 || slot >= ordinaryItems.size()) {
            throw new IndexOutOfBoundsException(
                    "Ordinary inventory slot " + slot + " outside [0," + ordinaryItems.size() + ")");
        }
    }
}
