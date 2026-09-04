package dev.resivore.slotreservations;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Separates vanilla admission from the reservation-aware post-native gate. */
public final class NativeInsertionPolicy {
    private static final ThreadLocal<Integer> NATIVE_QUERY_DEPTH =
            ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> NON_INSERTION_QUERY_DEPTH =
            ThreadLocal.withInitial(() -> 0);

    private NativeInsertionPolicy() {
    }

    public static boolean applyReservation(
            Container logicalContainer,
            int logicalSlot,
            ItemStack incoming,
            boolean nativeAllowed
    ) {
        if (!nativeAllowed || isNativeQuery() || isNonInsertionQuery()) {
            return nativeAllowed;
        }
        return SupportedContainerResolver.resolve(logicalContainer, logicalSlot)
                .map(resolved -> ReservationStore.reservationAllows(resolved, incoming))
                .orElse(true);
    }

    public static boolean nativeMayInsert(
            SupportedContainerResolver.ResolvedSlot slot,
            ItemStack incoming
    ) {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(incoming, "incoming");
        if (incoming.isEmpty()
                || SupportedContainerResolver.isShulkerOwner(slot)
                && !incoming.getItem().canFitInsideContainerItems()) {
            return false;
        }

        int previousDepth = NATIVE_QUERY_DEPTH.get();
        NATIVE_QUERY_DEPTH.set(previousDepth + 1);
        try {
            return slot.owner().canPlaceItem(slot.localSlot(), incoming);
        } finally {
            if (previousDepth == 0) {
                NATIVE_QUERY_DEPTH.remove();
            } else {
                NATIVE_QUERY_DEPTH.set(previousDepth);
            }
        }
    }

    public static boolean isNativeQuery() {
        return NATIVE_QUERY_DEPTH.get() > 0;
    }

    public static void beginNonInsertionQuery() {
        NON_INSERTION_QUERY_DEPTH.set(NON_INSERTION_QUERY_DEPTH.get() + 1);
    }

    public static void endNonInsertionQuery() {
        int depth = NON_INSERTION_QUERY_DEPTH.get();
        if (depth <= 1) {
            NON_INSERTION_QUERY_DEPTH.remove();
        } else {
            NON_INSERTION_QUERY_DEPTH.set(depth - 1);
        }
    }

    private static boolean isNonInsertionQuery() {
        return NON_INSERTION_QUERY_DEPTH.get() > 0;
    }
}
