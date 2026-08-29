package dev.aero.shulkertrowel.network;

import dev.aero.shulkertrowel.ShulkerTrowel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChangeTrowelGeometryPayload(int geometryId) implements CustomPacketPayload {
    public static final Type<ChangeTrowelGeometryPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ShulkerTrowel.MOD_ID, "change_geometry")
    );
    public static final StreamCodec<FriendlyByteBuf, ChangeTrowelGeometryPayload> CODEC =
            CustomPacketPayload.codec(ChangeTrowelGeometryPayload::write, ChangeTrowelGeometryPayload::new);

    private ChangeTrowelGeometryPayload(FriendlyByteBuf buffer) {
        this(buffer.readVarInt());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(geometryId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
