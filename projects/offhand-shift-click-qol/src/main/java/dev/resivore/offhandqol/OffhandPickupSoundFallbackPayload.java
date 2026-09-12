package dev.resivore.offhandqol;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Collector-only sound cue for a standalone custom pickup that never reached Player.take. */
public record OffhandPickupSoundFallbackPayload(double x, double y, double z) implements CustomPacketPayload {
    public static final Type<OffhandPickupSoundFallbackPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(OffhandShiftClickQol.MOD_ID, "routed_pickup_sound_fallback")
    );
    public static final StreamCodec<FriendlyByteBuf, OffhandPickupSoundFallbackPayload> CODEC =
            CustomPacketPayload.codec(OffhandPickupSoundFallbackPayload::write, OffhandPickupSoundFallbackPayload::new);

    private OffhandPickupSoundFallbackPayload(FriendlyByteBuf buf) {
        this(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
