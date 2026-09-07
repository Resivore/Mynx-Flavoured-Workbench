package tempeststudios.quickstacknearby;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

final class QuickStackClientNetworkingCompat {
    private QuickStackClientNetworkingCompat() {
    }

    static boolean canSendQuickStack() {
        return ClientPlayNetworking.canSend(QuickStackModernPayloadCompat.TYPE);
    }

    static void sendQuickStackRequest(QuickStackRequestPayload request) {
        ClientPlayNetworking.send(new QuickStackModernPayloadCompat.Packet(request));
    }

    static boolean canSendNearbySearch() { return ClientPlayNetworking.canSend(NearbySearchPayload.REQUEST_TYPE); }
    static void requestNearbySearch() { ClientPlayNetworking.send(new NearbySearchPayload.Request()); }
    static void targetNearbyContainer(net.minecraft.core.BlockPos position, net.minecraft.world.item.ItemStack stack, String nestedName) {
        ClientPlayNetworking.send(new NearbySearchPayload.Target(position, stack, nestedName == null ? "" : nestedName));
    }

    static void registerNearbySearchReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(NearbySearchPayload.SNAPSHOT_TYPE,
                (payload, context) -> context.client().execute(() -> NearbySearchClientState.acceptSnapshot(payload.entries())));
        ClientPlayNetworking.registerGlobalReceiver(NearbySearchPayload.TARGET_RESULT_TYPE,
                (payload, context) -> context.client().execute(() -> NearbySearchClientState.acceptTargetResult(payload)));
    }
}
