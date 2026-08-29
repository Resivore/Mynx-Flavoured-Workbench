package dev.resivore.mapmarkerextension;

import dev.resivore.mapmarkerextension.core.MapMarkerDecorationTypes;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MapMarkerExtension implements ModInitializer {
    public static final String MOD_ID = "map_marker_extension";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        MapMarkerDecorationTypes.register();
    }
}
