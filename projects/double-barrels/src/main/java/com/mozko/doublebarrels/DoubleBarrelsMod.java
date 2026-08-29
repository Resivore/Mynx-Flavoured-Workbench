package com.mozko.doublebarrels;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DoubleBarrelsMod implements ModInitializer {
    public static final String MOD_ID = "doublebarrels";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Double Barrels 26.2 compatibility port loaded");
    }
}
