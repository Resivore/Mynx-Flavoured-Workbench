package dev.resivore.matchajei.client;

import dev.resivore.matchajei.network.MatchaJeiDataPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class MatchaJeiClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(MatchaJeiDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> MatchaClientData.publish(payload))
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                client.execute(() -> MatchaClientData.publish(MatchaJeiDataPayload.EMPTY))
        );
    }
}
