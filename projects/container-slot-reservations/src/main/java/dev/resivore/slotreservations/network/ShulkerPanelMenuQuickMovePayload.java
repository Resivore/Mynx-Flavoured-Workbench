package dev.resivore.slotreservations.network;

import dev.resivore.slotreservations.ContainerSlotReservations;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Requests one server-authoritative player-menu-slot to carried-shulker quick move. */
public record ShulkerPanelMenuQuickMovePayload(int menuId, ShulkerHostLocator host, int sourceMenuSlot,
                                               String hostFingerprint) implements CustomPacketPayload {
    public static final Type<ShulkerPanelMenuQuickMovePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ContainerSlotReservations.MOD_ID, "shulker_panel_menu_quick_move"));
    public static final StreamCodec<FriendlyByteBuf, ShulkerPanelMenuQuickMovePayload> CODEC =
            CustomPacketPayload.codec(ShulkerPanelMenuQuickMovePayload::write,
                    ShulkerPanelMenuQuickMovePayload::new);

    private ShulkerPanelMenuQuickMovePayload(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), ShulkerHostLocator.read(buffer), readMenuSlot(buffer), buffer.readUtf(64));
    }

    private static int readMenuSlot(FriendlyByteBuf buffer) {
        int slot = buffer.readVarInt();
        if (slot < 0 || slot > 255) throw new DecoderException("Invalid panel menu quick-move source " + slot);
        return slot;
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(menuId);
        host.write(buffer);
        buffer.writeVarInt(sourceMenuSlot);
        buffer.writeUtf(hostFingerprint, 64);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
