package dev.resivore.slotreservations.mixin;

import dev.resivore.slotreservations.EnderChestReservationHolder;
import dev.resivore.slotreservations.ReservationData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Persists player-owned reservations immediately beside vanilla EnderItems. */
@Mixin(Player.class)
public abstract class PlayerEnderChestPersistenceMixin {
    @Unique
    private static final String RESERVATION_KEY =
            "container_slot_reservations:ender_chest_reservations";

    @Shadow
    public abstract PlayerEnderChestContainer getEnderChestInventory();

    @Inject(
            method = "readAdditionalSaveData",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/PlayerEnderChestContainer;fromSlots(Lnet/minecraft/world/level/storage/ValueInput$TypedInputList;)V",
                    shift = At.Shift.AFTER
            ),
            require = 1
    )
    private void containerSlotReservations$loadEnderChestReservations(
            ValueInput input,
            CallbackInfo callbackInfo
    ) {
        ReservationData data = input.read(RESERVATION_KEY, ReservationData.CODEC)
                .orElse(ReservationData.EMPTY);
        ((EnderChestReservationHolder) getEnderChestInventory())
                .containerSlotReservations$setReservations(data);
    }

    @Inject(
            method = "addAdditionalSaveData",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/PlayerEnderChestContainer;storeAsSlots(Lnet/minecraft/world/level/storage/ValueOutput$TypedOutputList;)V",
                    shift = At.Shift.AFTER
            ),
            require = 1
    )
    private void containerSlotReservations$saveEnderChestReservations(
            ValueOutput output,
            CallbackInfo callbackInfo
    ) {
        ReservationData data = ((EnderChestReservationHolder) getEnderChestInventory())
                .containerSlotReservations$getReservations();
        if (!data.isEmpty()) {
            output.store(RESERVATION_KEY, ReservationData.CODEC, data);
        }
    }
}
