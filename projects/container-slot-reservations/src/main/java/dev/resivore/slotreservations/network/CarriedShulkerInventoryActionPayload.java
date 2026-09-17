package dev.resivore.slotreservations.network;

import dev.resivore.slotreservations.ContainerSlotReservations;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Requests one fingerprint-bound player-inventory stack insertion into the carried shulker. */
public record CarriedShulkerInventoryActionPayload(
        int menuId,
        int menuSlot,
        int physicalPlayerSlot,
        String carriedFingerprint,
        String sourceFingerprint
) implements CustomPacketPayload {
    public static final Type<CarriedShulkerInventoryActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(
                    ContainerSlotReservations.MOD_ID, "carried_shulker_inventory_action"));
    public static final StreamCodec<FriendlyByteBuf, CarriedShulkerInventoryActionPayload> CODEC =
            CustomPacketPayload.codec(
                    CarriedShulkerInventoryActionPayload::write,
                    CarriedShulkerInventoryActionPayload::new);

    private CarriedShulkerInventoryActionPayload(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readUtf(64), buffer.readUtf(64));
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(menuId);
        buffer.writeVarInt(menuSlot);
        buffer.writeVarInt(physicalPlayerSlot);
        buffer.writeUtf(carriedFingerprint, 64);
        buffer.writeUtf(sourceFingerprint, 64);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
