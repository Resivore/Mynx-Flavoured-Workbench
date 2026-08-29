package dev.resivore.strippingtogglefallingtreecompat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class StrippingToggleFallingTreeCompat implements ModInitializer {
    public static final String MOD_ID = "strippingtoggle_fallingtree_compat";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(ToggleStatePayload.TYPE, ToggleStatePayload.CODEC);
        ServerPlayerEvents.JOIN.register(player ->
                ToggleAuthority.synchronizeNativeState(player, false));
        ServerPlayNetworking.registerGlobalReceiver(ToggleStatePayload.TYPE, (payload, context) ->
                ToggleAuthority.synchronizeNativeState(context.player(), payload.enabled()));
        ServerPlayerEvents.LEAVE.register(ToggleAuthority::clearNativeState);
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                server.getPlayerList().getPlayers().forEach(ToggleAuthority::clearNativeState));
    }
}
