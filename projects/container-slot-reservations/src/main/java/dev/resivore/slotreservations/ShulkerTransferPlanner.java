package dev.resivore.slotreservations;

import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import dev.resivore.slotreservations.api.ReservationSlotClass;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Pure deterministic planning for CSR-owned shulker transfers. */
public final class ShulkerTransferPlanner {
    private ShulkerTransferPlanner() {}

    public static Insertion planInsertion(ItemStack shulker, ItemStack incoming) {
        if (!eligibleHost(shulker) || incoming.isEmpty() || !incoming.getItem().canFitInsideContainerItems()) {
            return Insertion.unchanged(shulker, incoming);
        }
        NonNullList<ItemStack> contents = ShulkerContents.copy(shulker);
        int remaining = incoming.getCount();
        for (ReservationSlotClass pass : List.of(
                ReservationSlotClass.OCCUPIED_COMPATIBLE,
                ReservationSlotClass.RESERVED_MATCH,
                ReservationSlotClass.UNRESERVED_EMPTY)) {
            for (int slot = 0; slot < ReservationData.SLOT_COUNT && remaining > 0; slot++) {
                if (ContainerSlotReservationsApi.classify(shulkerWith(shulker, contents), slot, incoming) != pass) continue;
                ItemStack current = contents.get(slot);
                int capacity = current.isEmpty() ? incoming.getMaxStackSize() : current.getMaxStackSize();
                int moved = Math.min(remaining, Math.max(0, capacity - current.getCount()));
                if (moved == 0) continue;
                if (current.isEmpty()) contents.set(slot, incoming.copyWithCount(moved));
                else current.grow(moved);
                remaining -= moved;
            }
        }
        return insertionResult(shulker, incoming, contents, remaining);
    }

    public static Insertion planExactInsertion(ItemStack shulker, ItemStack incoming, int slot, boolean secondary) {
        if (!eligibleHost(shulker) || incoming.isEmpty() || slot < 0 || slot >= ReservationData.SLOT_COUNT
                || !incoming.getItem().canFitInsideContainerItems()) {
            return Insertion.unchanged(shulker, incoming);
        }
        NonNullList<ItemStack> contents = ShulkerContents.copy(shulker);
        ReservationSlotClass classification = ContainerSlotReservationsApi.classify(shulker, slot, incoming);
        if (classification != ReservationSlotClass.OCCUPIED_COMPATIBLE
                && classification != ReservationSlotClass.RESERVED_MATCH
                && classification != ReservationSlotClass.UNRESERVED_EMPTY) {
            return Insertion.unchanged(shulker, incoming);
        }
        ItemStack current = contents.get(slot);
        int capacity = current.isEmpty() ? incoming.getMaxStackSize() : current.getMaxStackSize();
        int moved = Math.min(secondary ? 1 : incoming.getCount(), Math.max(0, capacity - current.getCount()));
        if (moved == 0) return Insertion.unchanged(shulker, incoming);
        if (current.isEmpty()) contents.set(slot, incoming.copyWithCount(moved));
        else current.grow(moved);
        return insertionResult(shulker, incoming, contents, incoming.getCount() - moved);
    }

    public static Extraction planExtraction(ItemStack shulker, int slot, boolean secondary, int capacity) {
        if (!eligibleHost(shulker) || slot < 0 || slot >= ReservationData.SLOT_COUNT || capacity <= 0) {
            return Extraction.unchanged(shulker);
        }
        NonNullList<ItemStack> contents = ShulkerContents.copy(shulker);
        ItemStack physical = contents.get(slot);
        if (physical.isEmpty()) return Extraction.unchanged(shulker);
        int requested = secondary ? (physical.getCount() + 1) / 2 : physical.getCount();
        int moved = Math.min(requested, capacity);
        ItemStack extracted = physical.copyWithCount(moved);
        ItemStack remainder = physical.copy();
        remainder.shrink(moved);
        contents.set(slot, remainder);
        ItemStack changed = shulker.copy();
        ShulkerContents.replace(changed, contents);
        return new Extraction(changed, extracted, List.copyOf(contents), moved);
    }

    private static boolean eligibleHost(ItemStack shulker) {
        return shulker.getCount() == 1 && SupportedContainerResolver.isSupportedShulkerItem(shulker);
    }

    private static ItemStack shulkerWith(ItemStack original, List<ItemStack> contents) {
        ItemStack view = original.copy();
        ShulkerContents.replace(view, contents);
        return view;
    }

    private static Insertion insertionResult(ItemStack shulker, ItemStack incoming,
                                             List<ItemStack> contents, int remaining) {
        int moved = incoming.getCount() - remaining;
        if (moved == 0) return Insertion.unchanged(shulker, incoming);
        ItemStack changed = shulker.copy();
        ShulkerContents.replace(changed, contents);
        return new Insertion(changed, incoming.copyWithCount(remaining), List.copyOf(contents), moved);
    }

    public record Insertion(ItemStack shulker, ItemStack remainder, List<ItemStack> contents, int moved) {
        static Insertion unchanged(ItemStack shulker, ItemStack incoming) {
            return new Insertion(shulker.copy(), incoming.copy(), new ArrayList<>(ShulkerContents.copy(shulker)), 0);
        }
    }

    public record Extraction(ItemStack shulker, ItemStack extracted, List<ItemStack> contents, int moved) {
        static Extraction unchanged(ItemStack shulker) {
            return new Extraction(shulker.copy(), ItemStack.EMPTY,
                    new ArrayList<>(ShulkerContents.copy(shulker)), 0);
        }
    }
}
