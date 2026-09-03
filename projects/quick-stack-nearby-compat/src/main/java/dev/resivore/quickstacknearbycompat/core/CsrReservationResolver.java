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

    enum SlotClass {
        MATCHING_RESERVATION,
        MISMATCHED_RESERVATION,
        ORDINARY_EMPTY,
        BLOCKED
    }
}
