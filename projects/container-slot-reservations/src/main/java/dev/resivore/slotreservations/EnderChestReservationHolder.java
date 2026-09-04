package dev.resivore.slotreservations;

/** Internal storage surface mixed into the player's stable Ender Chest inventory object. */
public interface EnderChestReservationHolder {
    ReservationData containerSlotReservations$getReservations();

    void containerSlotReservations$setReservations(ReservationData data);
}
