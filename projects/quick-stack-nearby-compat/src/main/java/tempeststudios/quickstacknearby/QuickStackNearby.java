package tempeststudios.quickstacknearby;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QuickStackNearby implements ModInitializer {
    public static final String MOD_ID = "quick-stack-nearby";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        QuickStackServerConfig.getInstance();
        QuickStackCommands.register();
        QuickStackNetworking.register();
        QuickStackNearbyServerSmokeTest.registerIfEnabled();
        LOGGER.info("Quick Stack Nearby initialized.");
    }
}
