package dev.resivore.slotreservations.network;

import dev.resivore.slotreservations.ContainerSlotReservations;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ShulkerPanelContentActionPayload(int menuId, ShulkerHostLocator host, int internalSlot,
                                               Click click, String hostFingerprint)
        implements CustomPacketPayload {
    public enum Click { PRIMARY, SECONDARY, QUICK_MOVE }
    public static final Type<ShulkerPanelContentActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ContainerSlotReservations.MOD_ID, "shulker_panel_content"));
    public static final StreamCodec<FriendlyByteBuf, ShulkerPanelContentActionPayload> CODEC =
            CustomPacketPayload.codec(ShulkerPanelContentActionPayload::write, ShulkerPanelContentActionPayload::new);

    private ShulkerPanelContentActionPayload(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), ShulkerHostLocator.read(buffer), buffer.readVarInt(),
                readClick(buffer), buffer.readUtf(64));
    }

    private static Click readClick(FriendlyByteBuf buffer) {
        int id = buffer.readUnsignedByte();
        if (id < 0 || id >= Click.values().length) throw new DecoderException("Unknown panel click " + id);
        return Click.values()[id];
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(menuId); host.write(buffer); buffer.writeVarInt(internalSlot);
        buffer.writeByte(click.ordinal()); buffer.writeUtf(hostFingerprint, 64);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
