package dev.resivore.carriedrouting;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleLockPayload(int menuId, int slotIndex, int hand) implements CustomPacketPayload {
    public static final Type<ToggleLockPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(CarriedContainerAutoRouting.MOD_ID, "toggle_lock"));
    public static final StreamCodec<FriendlyByteBuf, ToggleLockPayload> CODEC = CustomPacketPayload.codec(ToggleLockPayload::write, ToggleLockPayload::new);
    private ToggleLockPayload(FriendlyByteBuf buf) { this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()); }
    private void write(FriendlyByteBuf buf) { buf.writeVarInt(menuId); buf.writeVarInt(slotIndex); buf.writeVarInt(hand); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
