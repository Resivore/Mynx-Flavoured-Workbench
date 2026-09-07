package dev.resivore.mynxtrees;

import dev.aero.cnmterraincompat.fixture.ExternalFixtureRegistry;
import net.fabricmc.api.ModInitializer;

public final class MynxTrees implements ModInitializer {
    @Override public void onInitialize() { ExternalFixtureRegistry.registerMynxTrees(); }
}
