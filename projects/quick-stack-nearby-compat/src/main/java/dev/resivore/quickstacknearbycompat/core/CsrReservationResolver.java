package dev.resivore.quickstacknearbycompat.core;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Optional, class-loading-safe bridge to CSR's read-only slot-classification API. */
final class CsrReservationResolver {
    static final String CSR_MOD_ID = "container_slot_reservations";

    private CsrReservationResolver() {}

    static boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded(CSR_MOD_ID);
    }

    static SlotClass classify(Container container, int slot, ItemStack incoming) {
        try {
            return CsrReservationApi.classify(container, slot, incoming);
        } catch (LinkageError unavailableApi) {
            throw new IllegalStateException(
                    "Loaded Container Slot Reservations does not expose the audited classification API",
                    unavailableApi
            );
        }
    }

    static NestedClass classify(ItemStack host, int slot, ItemStack incoming, ItemStack physical) {
        if (!isAvailable()) return physical.isEmpty() ? NestedClass.UNRESERVED_EMPTY
                : ItemStack.isSameItemSameComponents(physical, incoming) ? NestedClass.OCCUPIED_COMPATIBLE : NestedClass.BLOCKED;
        // A physical empty stack must not erase the slot's CSR owner.  Check ownership before
        // asking the provider to classify empty capacity: the provider's reservation matcher
        // deliberately carries C9's exact item-and-components semantics.
        if (physical.isEmpty()) {
            if (CsrReservationApi.reservationMatches(host, slot, incoming)) {
                return NestedClass.RESERVED_MATCH;
            }
            return CsrReservationApi.isReserved(host, slot)
                    ? NestedClass.BLOCKED
                    : NestedClass.UNRESERVED_EMPTY;
        }
        try { return CsrReservationApi.classify(host, slot, incoming); }
        catch (LinkageError unavailableApi) {
            throw new IllegalStateException("Loaded CSR lacks the audited ItemStack classification API", unavailableApi);
        }
    }
    enum NestedClass { RESERVED_MATCH, UNRESERVED_EMPTY, OCCUPIED_COMPATIBLE, BLOCKED }

    enum SlotClass {
        MATCHING_RESERVATION,
        MISMATCHED_RESERVATION,
        ORDINARY_EMPTY,
        BLOCKED
    }
}
