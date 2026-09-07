package tempeststudios.quickstacknearby;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

final class QuickStackModernPayloadCompat {
    static final CustomPacketPayload.Type<Packet> TYPE = new CustomPacketPayload.Type<>(
            ResourceIdCompat.quickStackId("quick_stack")
    );
    static final StreamCodec<RegistryFriendlyByteBuf, Packet> CODEC =
            CustomPacketPayload.codec(Packet::write, Packet::read);

    private QuickStackModernPayloadCompat() {
    }

    record Packet(QuickStackRequestPayload request) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private void write(RegistryFriendlyByteBuf buf) {
            request.write(buf);
        }

        private static Packet read(RegistryFriendlyByteBuf buf) {
            return new Packet(QuickStackRequestPayload.read(buf));
        }
    }
}
