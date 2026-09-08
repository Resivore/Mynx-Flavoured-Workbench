package dev.resivore.quickstacknearbycompat.core;

import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import dev.resivore.slotreservations.api.ReservationSlotClass;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Direct CSR API references, isolated so this class loads only after the optional-mod gate passes. */
final class CsrReservationApi {
    private CsrReservationApi() {}

    static boolean isReserved(ItemStack host, int slot) {
        return ContainerSlotReservationsApi.isReserved(host, slot);
    }

    static boolean reservationMatches(ItemStack host, int slot, ItemStack incoming) {
        return ContainerSlotReservationsApi.reservationMatches(host, slot, incoming);
    }

    static CsrReservationResolver.NestedClass classify(ItemStack host, int slot, ItemStack incoming) {
        return switch (ContainerSlotReservationsApi.classify(host, slot, incoming)) {
            case RESERVED_MATCH -> CsrReservationResolver.NestedClass.RESERVED_MATCH;
            case UNRESERVED_EMPTY -> CsrReservationResolver.NestedClass.UNRESERVED_EMPTY;
            case OCCUPIED_COMPATIBLE -> CsrReservationResolver.NestedClass.OCCUPIED_COMPATIBLE;
            case RESERVED_OTHER, NON_WRITABLE, INELIGIBLE -> CsrReservationResolver.NestedClass.BLOCKED;
        };
    }

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
