package dev.resivore.slotreservations;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Registration holder for the reservation component shared by blocks and shulker items. */
public final class ModComponents {
    public static final DataComponentType<ReservationData> RESERVATIONS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath("container_slot_reservations", "reservations"),
            DataComponentType.<ReservationData>builder()
                    .persistent(ReservationData.CODEC)
                    .networkSynchronized(ReservationData.STREAM_CODEC)
                    .build()
    );

    private ModComponents() {
    }

    /** Forces class initialization during the mod entrypoint's registry phase. */
    public static void register() {
    }

    /** Conventional entrypoint alias. */
    public static void initialize() {
        register();
    }
}
