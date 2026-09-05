package dev.resivore.slotreservations.network;

import dev.resivore.slotreservations.ContainerSlotReservations;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Carries target coordinates and a host fingerprint, never a reservation template. */
public record NestedReservationActionPayload(int menuId, int stateId, int hostSlot, int nestedSlot,
        ReservationActionPayload.Source source, String hostFingerprint) implements CustomPacketPayload {
    public static final Type<NestedReservationActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ContainerSlotReservations.MOD_ID, "nested_reservation_action"));
    public static final StreamCodec<FriendlyByteBuf, NestedReservationActionPayload> CODEC =
            CustomPacketPayload.codec(NestedReservationActionPayload::write, NestedReservationActionPayload::new);
    private NestedReservationActionPayload(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                readSource(buffer), buffer.readUtf(64));
    }
    private static ReservationActionPayload.Source readSource(FriendlyByteBuf buffer) {
        int id = buffer.readUnsignedByte();
        for (var value : ReservationActionPayload.Source.values()) if (value.networkId() == id) return value;
        throw new DecoderException("Unknown nested reservation source " + id);
    }
    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(menuId); buffer.writeVarInt(stateId); buffer.writeVarInt(hostSlot);
        buffer.writeVarInt(nestedSlot); buffer.writeByte(source.networkId()); buffer.writeUtf(hostFingerprint, 64);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
