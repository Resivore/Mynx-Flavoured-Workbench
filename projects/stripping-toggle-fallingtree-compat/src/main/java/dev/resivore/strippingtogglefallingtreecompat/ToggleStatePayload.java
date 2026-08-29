package dev.resivore.strippingtogglefallingtreecompat;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleStatePayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<ToggleStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(StrippingToggleFallingTreeCompat.MOD_ID, "toggle_state"));
    public static final StreamCodec<FriendlyByteBuf, ToggleStatePayload> CODEC =
            CustomPacketPayload.codec(ToggleStatePayload::write, ToggleStatePayload::new);

    private ToggleStatePayload(FriendlyByteBuf buffer) { this(buffer.readBoolean()); }
    private void write(FriendlyByteBuf buffer) { buffer.writeBoolean(enabled); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
