package dev.resivore.matchabeacon;

import dev.resivore.matchabeacon.runtime.BeaconKindlingCoordinator;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MatchaBeaconKindlingCompat implements ModInitializer {
    public static final String MOD_ID = "matcha_beacon_kindling_compat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        BeaconKindlingCoordinator.install();
    }
}
