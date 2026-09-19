package dev.aero.cnmterraincompat.fixture;

import net.fabricmc.api.ModInitializer;

/** Exact namespace fixture for the generic CNM-family bridge regression. */
public final class EnderscapeFixture implements ModInitializer {
    @Override public void onInitialize() { ExternalFixtureRegistry.registerEnderscape(); }
}
