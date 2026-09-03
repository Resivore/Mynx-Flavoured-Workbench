package dev.resivore.quickstacknearbycompat.core;

import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import dev.resivore.slotreservations.api.ReservationSlotClass;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Direct CSR API references, isolated so this class loads only after the optional-mod gate passes. */
final class CsrReservationApi {
    private CsrReservationApi() {}

    static CsrReservationResolver.SlotClass classify(
            Container container,
            int slot,
            ItemStack incoming) {
        ReservationSlotClass classification = ContainerSlotReservationsApi.classify(container, slot, incoming);
        return switch (classification) {
            case RESERVED_MATCH -> CsrReservationResolver.SlotClass.MATCHING_RESERVATION;
            case UNRESERVED_EMPTY, INELIGIBLE -> CsrReservationResolver.SlotClass.ORDINARY_EMPTY;
            case RESERVED_OTHER -> CsrReservationResolver.SlotClass.MISMATCHED_RESERVATION;
            case OCCUPIED_COMPATIBLE, NON_WRITABLE -> CsrReservationResolver.SlotClass.BLOCKED;
        };
    }
}
