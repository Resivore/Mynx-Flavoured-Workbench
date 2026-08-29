package dev.resivore.slotreservations.api;

/** Reservation-aware slot state only; this enum does not prescribe routing order. */
public enum ReservationSlotClass {
    OCCUPIED_COMPATIBLE(true),
    RESERVED_MATCH(true),
    UNRESERVED_EMPTY(true),
    RESERVED_OTHER(false),
    NON_WRITABLE(false),
    INELIGIBLE(false);

    private final boolean acceptsIncoming;

    ReservationSlotClass(boolean acceptsIncoming) {
        this.acceptsIncoming = acceptsIncoming;
    }

    public boolean acceptsIncoming() {
        return acceptsIncoming;
    }
}
