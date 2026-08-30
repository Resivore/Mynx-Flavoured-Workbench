package dev.resivore.matchafrost;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MatchaFrostProtection implements ModInitializer {
    public static final String MOD_ID = "matcha_frost_protection";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Matcha Frost Protection initialized");
    }
}
