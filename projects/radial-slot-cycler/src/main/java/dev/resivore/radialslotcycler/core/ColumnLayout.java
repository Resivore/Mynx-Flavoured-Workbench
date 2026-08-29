package dev.resivore.radialslotcycler.core;

import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Maps a physical hotbar index to the same backing-inventory column in every
 * complete ordinary storage row. Menu wrapper indices are intentionally not
 * part of this contract.
 */
public final class ColumnLayout {
    private ColumnLayout() {}

    public static List<Integer> liveRadialSlots(Inventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        return radialSlots(inventory.getSelectedSlot(), inventory.getNonEquipmentItems().size());
    }

    public static List<Integer> radialSlots(int hotbarSlot, int ordinarySize) {
        List<Integer> result = new ArrayList<>();
        result.add(requireHotbarSlot(hotbarSlot));
        result.addAll(storageCandidates(hotbarSlot, ordinarySize));
        return List.copyOf(result);
    }

    public static List<Integer> storageCandidates(int hotbarSlot, int ordinarySize) {
        requireHotbarSlot(hotbarSlot);
        requireCompleteOrdinaryLayout(ordinarySize);

        List<Integer> result = new ArrayList<>((ordinarySize / Inventory.SELECTION_SIZE) - 1);
        for (int slot = Inventory.SELECTION_SIZE + hotbarSlot;
             slot < ordinarySize;
             slot += Inventory.SELECTION_SIZE) {
            result.add(slot);
        }
        return List.copyOf(result);
    }

    public static boolean isStorageCandidate(int hotbarSlot, int targetSlot, int ordinarySize) {
        if (!isHotbarSlot(hotbarSlot) || !isCompleteOrdinaryLayout(ordinarySize)) {
            return false;
        }
        return targetSlot >= Inventory.SELECTION_SIZE
                && targetSlot < ordinarySize
                && targetSlot % Inventory.SELECTION_SIZE == hotbarSlot;
    }

    public static boolean isCompleteOrdinaryLayout(int ordinarySize) {
        return ordinarySize >= Inventory.SELECTION_SIZE
                && ordinarySize % Inventory.SELECTION_SIZE == 0;
    }

    public static boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot < Inventory.SELECTION_SIZE;
    }

    private static int requireHotbarSlot(int hotbarSlot) {
        if (!isHotbarSlot(hotbarSlot)) {
            throw new IllegalArgumentException("Not a physical hotbar slot: " + hotbarSlot);
        }
        return hotbarSlot;
    }

    private static void requireCompleteOrdinaryLayout(int ordinarySize) {
        if (!isCompleteOrdinaryLayout(ordinarySize)) {
            throw new IllegalArgumentException(
                    "Ordinary inventory is not a complete set of nine-wide rows: " + ordinarySize);
        }
    }
}
