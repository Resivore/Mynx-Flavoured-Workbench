package net.penumbra.enderscape;

import dev.aero.cnmterraincompat.fixture.ExternalFixtureRegistry;
import net.fabricmc.api.ModInitializer;

/** Test-only optional-provider entrypoint with the production Enderscape binary name. */
public final class Enderscape implements ModInitializer {
    @Override public void onInitialize() { ExternalFixtureRegistry.registerEnderscape(); }
}
