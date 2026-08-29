package dev.resivore.slotreservations;

import net.fabricmc.api.ModInitializer;

public final class ContainerSlotReservations implements ModInitializer {
    public static final String MOD_ID = "container_slot_reservations";

    @Override
    public void onInitialize() {
        ModComponents.initialize();
        ShulkerReservationDrops.register();
        ReservationNetworking.register();
    }
}
