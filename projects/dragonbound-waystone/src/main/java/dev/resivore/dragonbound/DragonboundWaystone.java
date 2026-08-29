package dev.resivore.dragonbound;

import dev.resivore.dragonbound.config.DragonboundConfigManager;
import dev.resivore.dragonbound.channel.DragonboundChannelManager;
import dev.resivore.dragonbound.recipe.DragonboundRecipeUnlocks;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DragonboundWaystone implements ModInitializer {
    public static final String MOD_ID = "dragonbound_waystone";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        DragonboundContent.register();
        DragonboundConfigManager.load();
        DragonboundChannelManager.install();
        DragonboundRecipeUnlocks.install();
    }
}
