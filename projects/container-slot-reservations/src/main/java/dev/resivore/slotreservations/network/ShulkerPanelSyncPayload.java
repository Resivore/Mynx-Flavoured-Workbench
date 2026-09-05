package dev.resivore.slotreservations.network;

import dev.resivore.slotreservations.ContainerSlotReservations;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ShulkerPanelSyncPayload(int menuId, ShulkerHostLocator host, String hostFingerprint,
                                      int selectedSlot) implements CustomPacketPayload {
    public static final Type<ShulkerPanelSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ContainerSlotReservations.MOD_ID, "shulker_panel_sync"));
    public static final StreamCodec<FriendlyByteBuf, ShulkerPanelSyncPayload> CODEC =
            CustomPacketPayload.codec(ShulkerPanelSyncPayload::write, ShulkerPanelSyncPayload::new);
    private ShulkerPanelSyncPayload(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), ShulkerHostLocator.read(buffer), buffer.readUtf(64), buffer.readVarInt());
    }
    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(menuId); host.write(buffer); buffer.writeUtf(hostFingerprint, 64);
        buffer.writeVarInt(selectedSlot);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
