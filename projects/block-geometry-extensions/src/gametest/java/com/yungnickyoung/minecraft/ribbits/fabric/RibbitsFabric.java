package com.yungnickyoung.minecraft.ribbits.fabric;

import dev.aero.cnmterraincompat.fixture.ExternalFixtureRegistry;
import net.fabricmc.api.ModInitializer;

public final class RibbitsFabric implements ModInitializer {
    @Override public void onInitialize() { ExternalFixtureRegistry.registerRibbits(); }
}
