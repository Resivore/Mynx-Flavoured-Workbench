package dev.resivore.strippingtogglefallingtreecompat.client;

import dev.resivore.strippingtogglefallingtreecompat.ToggleStatePayload;
import dev.resivore.strippingtogglefallingtreecompat.ToggleAuthority;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import yungando.strippingtoggle.StrippingToggle;

public final class StrippingToggleFallingTreeCompatClient implements ClientModInitializer {
    private static final String REQUIRED_STRIPPING_TOGGLE_VERSION = "1.2.6+26.2";

    private final ToggleSyncTracker syncTracker = new ToggleSyncTracker();
    private LocalPlayer connectedPlayer;

    @Override
    public void onInitializeClient() {
        requireAuditedStrippingToggle();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            syncTracker.reset();
            synchronize(client, true);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (connectedPlayer != null) ToggleAuthority.clearNativeState(connectedPlayer);
            connectedPlayer = null;
            syncTracker.reset();
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> synchronize(client, false));
    }

    private static void requireAuditedStrippingToggle() {
        String installedVersion = FabricLoader.getInstance()
                .getModContainer("strippingtoggle")
                .orElseThrow(() -> new IllegalStateException(
                        "StrippingToggle 1.2.6+26.2 is required on the client"))
                .getMetadata()
                .getVersion()
                .getFriendlyString();
        if (!REQUIRED_STRIPPING_TOGGLE_VERSION.equals(installedVersion)) {
            throw new IllegalStateException(
                    "Unsupported StrippingToggle client version: " + installedVersion);
        }
    }

    private void synchronize(Minecraft client, boolean force) {
        if (client.player == null || client.getConnection() == null) return;

        if (client.player != connectedPlayer) {
            if (connectedPlayer != null) ToggleAuthority.clearNativeState(connectedPlayer);
            connectedPlayer = client.player;
            syncTracker.reset();
            force = true;
        }

        boolean current = StrippingToggle.strippingEnabled;
        int decision = syncTracker.evaluate(current, force);
        if ((decision & ToggleSyncTracker.APPLY_NATIVE_STATE) != 0) {
            ToggleAuthority.synchronizeNativeState(client.player, current);
        }
        if ((decision & ToggleSyncTracker.SEND_PENDING) != 0
                && ClientPlayNetworking.canSend(ToggleStatePayload.TYPE)) {
            ClientPlayNetworking.send(new ToggleStatePayload(current));
            syncTracker.markSent(current);
        }
    }
}
