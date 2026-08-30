package dev.aero.shulkertrowel.client;

import dev.aero.shulkertrowel.ShulkerTrowel;
import net.fabricmc.api.ClientModInitializer;

public final class ShulkerTrowelClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ShulkerTrowel.LOGGER.info("Shulker Trowel Canary 5 client integration initialized");
    }
}
