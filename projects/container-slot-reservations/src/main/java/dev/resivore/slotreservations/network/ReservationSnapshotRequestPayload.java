package dev.resivore.slotreservations.network;

import dev.resivore.slotreservations.ContainerSlotReservations;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ReservationSnapshotRequestPayload(int menuId) implements CustomPacketPayload {
    public static final Type<ReservationSnapshotRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            ContainerSlotReservations.MOD_ID, "reservation_snapshot_request"));
    public static final StreamCodec<FriendlyByteBuf, ReservationSnapshotRequestPayload> CODEC =
            CustomPacketPayload.codec(ReservationSnapshotRequestPayload::write,
                    ReservationSnapshotRequestPayload::new);

    private ReservationSnapshotRequestPayload(FriendlyByteBuf buffer) {
        this(buffer.readVarInt());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(menuId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
