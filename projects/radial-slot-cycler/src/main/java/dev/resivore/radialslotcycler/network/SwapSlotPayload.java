package dev.resivore.radialslotcycler.network;

import dev.resivore.radialslotcycler.RadialSlotCycler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SwapSlotPayload(
        int selectedHotbarSlot,
        int storageSlot,
        int ordinarySize
) implements CustomPacketPayload {
    public static final Type<SwapSlotPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RadialSlotCycler.MOD_ID, "swap_slot"));
    public static final StreamCodec<FriendlyByteBuf, SwapSlotPayload> CODEC =
            CustomPacketPayload.codec(SwapSlotPayload::write, SwapSlotPayload::new);

    private SwapSlotPayload(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(selectedHotbarSlot);
        buffer.writeVarInt(storageSlot);
        buffer.writeVarInt(ordinarySize);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
