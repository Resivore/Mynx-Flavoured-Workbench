package com.mcwpaths.kikoz;

import dev.aero.cnmterraincompat.fixture.ExternalFixtureRegistry;
import net.fabricmc.api.ModInitializer;

public final class MacawsPaths implements ModInitializer {
    @Override public void onInitialize() { ExternalFixtureRegistry.registerMacawsPaths(); }
}
