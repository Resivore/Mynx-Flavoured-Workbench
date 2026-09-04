package dev.resivore.carriedrouting;

import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import net.minecraft.world.item.ItemStack;

/** The optional provider's public carried-stack overloads are the entire integration. */
final class CsrReservationAdmission implements ReservationAdmission {
    public Slot classify(ItemStack carrier, int slot, ItemStack incoming, ItemStack physical) {
        return Slot.valueOf(ContainerSlotReservationsApi.classify(carrier, slot, incoming).name());
    }
    public boolean permitsAffinity(ItemStack carrier, int slot, ItemStack incoming) {
        // Full physical matches still seed affinity, but conflicting reservations never do.
        return !ContainerSlotReservationsApi.isReserved(carrier, slot)
                || ContainerSlotReservationsApi.reservationMatches(carrier, slot, incoming);
    }
}
