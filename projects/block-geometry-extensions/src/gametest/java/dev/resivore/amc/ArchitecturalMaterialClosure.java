package dev.resivore.amc;

import dev.aero.cnmterraincompat.fixture.ExternalFixtureRegistry;
import net.fabricmc.api.ModInitializer;

/** Minimal production-name AMC lifecycle fixture for the C101 provider-completion seam. */
public final class ArchitecturalMaterialClosure implements ModInitializer {
    @Override public void onInitialize() { ExternalFixtureRegistry.registerArchitecturalMaterialClosure(); }
}
