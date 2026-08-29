package dev.resivore.xaerodiscovery;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class XaeroDiscoveryRadiusClient implements ClientModInitializer {
    public static final String MOD_ID = "xaero_discovery_radius";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        DiscoveryService.initialize(
                FabricLoader.getInstance().getConfigDir(),
                FabricLoader.getInstance().getGameDir(),
                LOGGER
        );
        ServerLifecycleEvents.SERVER_STARTING.register(DiscoveryService::onServerStarting);
        ServerLevelEvents.LOAD.register(DiscoveryService::onServerLevelLoad);
        ServerLifecycleEvents.SERVER_STOPPED.register(DiscoveryService::onServerStopped);
        ClientTickEvents.END_CLIENT_TICK.register(DiscoveryService::onEndClientTick);
    }
}
