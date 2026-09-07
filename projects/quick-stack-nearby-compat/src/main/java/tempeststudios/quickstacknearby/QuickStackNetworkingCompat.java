package tempeststudios.quickstacknearby;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

final class QuickStackNetworkingCompat {
    private QuickStackNetworkingCompat() {
    }

    static void register() {
        PayloadTypeRegistry.serverboundPlay().register(QuickStackModernPayloadCompat.TYPE, QuickStackModernPayloadCompat.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NearbySearchPayload.REQUEST_TYPE, NearbySearchPayload.REQUEST_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NearbySearchPayload.TARGET_TYPE, NearbySearchPayload.TARGET_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NearbySearchPayload.SNAPSHOT_TYPE, NearbySearchPayload.SNAPSHOT_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NearbySearchPayload.TARGET_RESULT_TYPE, NearbySearchPayload.TARGET_RESULT_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
                QuickStackModernPayloadCompat.TYPE,
                (payload, context) -> QuickStackService.quickStack(
                        context.player(),
                        QuickStackMoveEngine.SourceRules.fromPayloadRules(payload.request().slotRules())
                )
        );
        ServerPlayNetworking.registerGlobalReceiver(NearbySearchPayload.REQUEST_TYPE,
                (payload, context) -> ServerPlayNetworking.send(context.player(),
                        new NearbySearchPayload.Snapshot(NearbySearchService.snapshot(context.player()))));
        ServerPlayNetworking.registerGlobalReceiver(NearbySearchPayload.TARGET_TYPE, (payload, context) -> {
            boolean accepted = NearbySearchService.validTarget(context.player(), payload.position(), payload.stack(), payload.nestedName());
            ServerPlayNetworking.send(context.player(), new NearbySearchPayload.TargetResult(payload.position(), accepted));
        });
    }
}
