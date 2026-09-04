package dev.resivore.slotreservations.mixin;

import dev.resivore.slotreservations.EnderChestReservationHolder;
import dev.resivore.slotreservations.ReservationData;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

@Mixin(PlayerEnderChestContainer.class)
public abstract class PlayerEnderChestContainerMixin implements EnderChestReservationHolder {
    @Unique
    private ReservationData containerSlotReservations$reservations = ReservationData.EMPTY;

    @Override
    public ReservationData containerSlotReservations$getReservations() {
        return containerSlotReservations$reservations;
    }

    @Override
    public void containerSlotReservations$setReservations(ReservationData data) {
        containerSlotReservations$reservations = Objects.requireNonNull(data, "data");
    }
}
