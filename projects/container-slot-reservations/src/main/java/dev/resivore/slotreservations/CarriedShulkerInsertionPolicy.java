package dev.resivore.slotreservations;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Objects;

/**
 * Filters Item Interactions' carried-container destination candidates without
 * taking ownership of its insertion or component write-back workflow.
 */
public final class CarriedShulkerInsertionPolicy {
    private CarriedShulkerInsertionPolicy() {
    }

    /** Keeps compatible occupied candidates in the external mod's original order. */
    public static int[] occupiedCandidates(
            ItemStack sourceShulker,
            Container liveContents,
            ItemStack incoming,
            int[] upstreamCandidates
    ) {
        Objects.requireNonNull(sourceShulker, "sourceShulker");
        Objects.requireNonNull(liveContents, "liveContents");
        Objects.requireNonNull(incoming, "incoming");
        Objects.requireNonNull(upstreamCandidates, "upstreamCandidates");

        ReservationData reservations = activeReservations(sourceShulker);
        if (reservations == null) {
            return upstreamCandidates;
        }
        return occupiedCandidates(reservations, liveContents, incoming, upstreamCandidates);
    }

    static int[] occupiedCandidates(
            ReservationData reservations,
            Container liveContents,
            ItemStack incoming,
            int[] upstreamCandidates
    ) {
        Objects.requireNonNull(reservations, "reservations");
        Objects.requireNonNull(liveContents, "liveContents");
        Objects.requireNonNull(incoming, "incoming");
        Objects.requireNonNull(upstreamCandidates, "upstreamCandidates");
        if (reservations.isEmpty()) {
            return upstreamCandidates;
        }
        if (!validInsertionContext(liveContents, incoming)) {
            return new int[0];
        }

        int[] filtered = new int[upstreamCandidates.length];
        int count = 0;
        for (int slot : upstreamCandidates) {
            if (validSlot(slot)
                    && !liveContents.getItem(slot).isEmpty()
                    && reservationAllows(reservations, slot, incoming)) {
                filtered[count++] = slot;
            }
        }
        return Arrays.copyOf(filtered, count);
    }

    /**
     * Orders matching reserved empties before unreserved empties and removes
     * every mismatched or non-writable destination.
     */
    public static int[] emptyCandidates(
            ItemStack sourceShulker,
            Container liveContents,
            ItemStack incoming,
            int[] upstreamCandidates
    ) {
        Objects.requireNonNull(sourceShulker, "sourceShulker");
        Objects.requireNonNull(liveContents, "liveContents");
        Objects.requireNonNull(incoming, "incoming");
        Objects.requireNonNull(upstreamCandidates, "upstreamCandidates");

        ReservationData reservations = activeReservations(sourceShulker);
        if (reservations == null) {
            return upstreamCandidates;
        }
        return emptyCandidates(reservations, liveContents, incoming, upstreamCandidates);
    }

    static int[] emptyCandidates(
            ReservationData reservations,
            Container liveContents,
            ItemStack incoming,
            int[] upstreamCandidates
    ) {
        Objects.requireNonNull(reservations, "reservations");
        Objects.requireNonNull(liveContents, "liveContents");
        Objects.requireNonNull(incoming, "incoming");
        Objects.requireNonNull(upstreamCandidates, "upstreamCandidates");
        if (reservations.isEmpty()) {
            return upstreamCandidates;
        }
        if (!validInsertionContext(liveContents, incoming)) {
            return new int[0];
        }

        int[] ordered = new int[upstreamCandidates.length];
        int count = 0;
        for (int slot : upstreamCandidates) {
            if (isEmpty(liveContents, slot)
                    && reservations.get(slot).filter(template ->
                            ItemStack.isSameItemSameComponents(template, incoming)
                    ).isPresent()) {
                ordered[count++] = slot;
            }
        }
        for (int slot : upstreamCandidates) {
            if (isEmpty(liveContents, slot) && reservations.get(slot).isEmpty()) {
                ordered[count++] = slot;
            }
        }
        return Arrays.copyOf(ordered, count);
    }

    private static ReservationData activeReservations(ItemStack sourceShulker) {
        if (!SupportedContainerResolver.isSupportedShulkerItem(sourceShulker)) {
            return null;
        }
        ReservationData reservations = ReservationStore.getData(sourceShulker);
        return reservations.isEmpty() ? null : reservations;
    }

    private static boolean validInsertionContext(Container liveContents, ItemStack incoming) {
        return liveContents.getContainerSize() == ReservationData.SLOT_COUNT
                && !incoming.isEmpty()
                && incoming.getItem().canFitInsideContainerItems();
    }

    private static boolean reservationAllows(
            ReservationData reservations,
            int slot,
            ItemStack incoming
    ) {
        return reservations.get(slot)
                .map(template -> ItemStack.isSameItemSameComponents(template, incoming))
                .orElse(true);
    }

    private static boolean isEmpty(Container liveContents, int slot) {
        return validSlot(slot) && liveContents.getItem(slot).isEmpty();
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < ReservationData.SLOT_COUNT;
    }
}
