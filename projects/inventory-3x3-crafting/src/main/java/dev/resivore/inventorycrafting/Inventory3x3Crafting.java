package dev.resivore.inventorycrafting;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Inventory3x3Crafting implements ModInitializer {
    public static final String MOD_ID = "inherent_3x3_inventory_crafting";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Inherent 3x3 Inventory Crafting Canary 4 initialized (GENERATED / UNTESTED)");
    }
}
