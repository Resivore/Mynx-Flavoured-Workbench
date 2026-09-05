package dev.resivore.slotreservations.network;

import dev.resivore.slotreservations.ContainerSlotReservations;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ShulkerSelectionPayload(int menuId, ShulkerHostLocator host, int internalSlot,
                                      String hostFingerprint) implements CustomPacketPayload {
    public static final Type<ShulkerSelectionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ContainerSlotReservations.MOD_ID, "shulker_selection"));
    public static final StreamCodec<FriendlyByteBuf, ShulkerSelectionPayload> CODEC =
            CustomPacketPayload.codec(ShulkerSelectionPayload::write, ShulkerSelectionPayload::new);
    private ShulkerSelectionPayload(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), ShulkerHostLocator.read(buffer), buffer.readVarInt(), buffer.readUtf(64));
    }
    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(menuId); host.write(buffer); buffer.writeVarInt(internalSlot);
        buffer.writeUtf(hostFingerprint, 64);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
