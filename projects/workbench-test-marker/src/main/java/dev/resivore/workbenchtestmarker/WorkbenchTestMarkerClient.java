package dev.resivore.workbenchtestmarker;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorkbenchTestMarkerClient implements ClientModInitializer {
    public static final String MOD_ID = "workbench_test_marker";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        MarkerState state = MarkerState.current();
        LOGGER.info("Workbench title state active: status='{}', lines={}", state.status(), state.lines());
    }
}
