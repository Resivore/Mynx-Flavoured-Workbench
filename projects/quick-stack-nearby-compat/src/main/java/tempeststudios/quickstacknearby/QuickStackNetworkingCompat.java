package tempeststudios.quickstacknearby;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

final class QuickStackNetworkingCompat {
    private QuickStackNetworkingCompat() {
    }

    static void register() {
        PayloadTypeRegistry.serverboundPlay().register(QuickStackModernPayloadCompat.TYPE, QuickStackModernPayloadCompat.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
                QuickStackModernPayloadCompat.TYPE,
                (payload, context) -> QuickStackService.quickStack(
                        context.player(),
                        QuickStackMoveEngine.SourceRules.fromPayloadRules(payload.request().slotRules())
                )
        );
    }
}
