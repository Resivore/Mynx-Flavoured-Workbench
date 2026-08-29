package dev.resivore.slotreservations.network;

import dev.resivore.slotreservations.ContainerSlotReservations;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ReservationActionPayload(int menuId, int menuSlotIndex, Source source)
        implements CustomPacketPayload {
    public static final Type<ReservationActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            ContainerSlotReservations.MOD_ID, "reservation_action"));
    public static final StreamCodec<FriendlyByteBuf, ReservationActionPayload> CODEC =
            CustomPacketPayload.codec(ReservationActionPayload::write, ReservationActionPayload::new);

    private ReservationActionPayload(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarInt(), Source.fromNetworkId(buffer.readUnsignedByte()));
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(menuId);
        buffer.writeVarInt(menuSlotIndex);
        buffer.writeByte(source.networkId());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Source {
        SLOT_STACK(0),
        CARRIED_STACK(1),
        CLEAR_EMPTY(2);

        private final int networkId;

        Source(int networkId) {
            this.networkId = networkId;
        }

        public int networkId() {
            return networkId;
        }

        private static Source fromNetworkId(int networkId) {
            for (Source source : values()) {
                if (source.networkId == networkId) return source;
            }
            throw new DecoderException("Unknown reservation action source " + networkId);
        }
    }
}
