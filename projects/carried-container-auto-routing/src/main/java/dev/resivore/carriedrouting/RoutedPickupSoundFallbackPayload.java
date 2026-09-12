package dev.resivore.carriedrouting;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Collector-only sound cue for a custom world pickup that never reached Player.take. */
public record RoutedPickupSoundFallbackPayload(
        double x,
        double y,
        double z,
        boolean lowerPitch
) implements CustomPacketPayload {
    public static final Type<RoutedPickupSoundFallbackPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(CarriedContainerAutoRouting.MOD_ID, "routed_pickup_sound_fallback")
    );
    public static final StreamCodec<FriendlyByteBuf, RoutedPickupSoundFallbackPayload> CODEC =
            CustomPacketPayload.codec(RoutedPickupSoundFallbackPayload::write, RoutedPickupSoundFallbackPayload::new);

    private RoutedPickupSoundFallbackPayload(FriendlyByteBuf buf) {
        this(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeBoolean(lowerPitch);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
