package dev.resivore.slotreservations.network;

import dev.resivore.slotreservations.ContainerSlotReservations;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ShulkerPanelReservationActionPayload(int menuId, ShulkerHostLocator host, int internalSlot,
                                                   ReservationActionPayload.Source source,
                                                   String hostFingerprint) implements CustomPacketPayload {
    public static final Type<ShulkerPanelReservationActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ContainerSlotReservations.MOD_ID, "shulker_panel_reservation"));
    public static final StreamCodec<FriendlyByteBuf, ShulkerPanelReservationActionPayload> CODEC =
            CustomPacketPayload.codec(ShulkerPanelReservationActionPayload::write,
                    ShulkerPanelReservationActionPayload::new);
    private ShulkerPanelReservationActionPayload(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), ShulkerHostLocator.read(buffer), buffer.readVarInt(),
                readSource(buffer), buffer.readUtf(64));
    }
    private static ReservationActionPayload.Source readSource(FriendlyByteBuf buffer) {
        int id = buffer.readUnsignedByte();
        for (var source : ReservationActionPayload.Source.values()) if (source.networkId() == id) return source;
        throw new DecoderException("Unknown reservation source " + id);
    }
    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(menuId); host.write(buffer); buffer.writeVarInt(internalSlot);
        buffer.writeByte(source.networkId()); buffer.writeUtf(hostFingerprint, 64);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
