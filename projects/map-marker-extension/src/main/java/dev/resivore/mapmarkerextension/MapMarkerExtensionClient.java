package dev.resivore.mapmarkerextension;

import dev.resivore.mapmarkerextension.client.CarriedMapScanner;
import dev.resivore.mapmarkerextension.client.MapMarkerItemModel;
import dev.resivore.mapmarkerextension.client.MapMarkerTargetRepository;
import dev.resivore.mapmarkerextension.compat.XaeroIntegration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class MapMarkerExtensionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MapMarkerItemModel.register();

        FabricLoader loader = FabricLoader.getInstance();
        if (!loader.isModLoaded("xaerominimap") || !loader.isModLoaded("xaeroworldmap")) {
            MapMarkerExtension.LOGGER.info(
                "Xaero integration disabled because both Xaero map mods are not present"
            );
            return;
        }

        MapMarkerTargetRepository targets = new MapMarkerTargetRepository();
        CarriedMapScanner scanner = new CarriedMapScanner(targets);
        XaeroIntegration xaeroIntegration = new XaeroIntegration(targets);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            scanner.refresh(client);
            xaeroIntegration.installIfReady();
        });
    }
}
