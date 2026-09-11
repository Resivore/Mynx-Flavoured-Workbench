package dev.resivore.carriedrouting;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** One-shot server authority for the next vanilla take-item packet for this entity. */
public record RoutedPickupSoundPayload(int itemEntityId) implements CustomPacketPayload {
    public static final Type<RoutedPickupSoundPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(CarriedContainerAutoRouting.MOD_ID, "routed_pickup_sound")
    );
    public static final StreamCodec<FriendlyByteBuf, RoutedPickupSoundPayload> CODEC =
            CustomPacketPayload.codec(RoutedPickupSoundPayload::write, RoutedPickupSoundPayload::new);

    private RoutedPickupSoundPayload(FriendlyByteBuf buf) { this(buf.readVarInt()); }
    private void write(FriendlyByteBuf buf) { buf.writeVarInt(itemEntityId); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
