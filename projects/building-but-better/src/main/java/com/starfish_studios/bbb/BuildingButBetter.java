package com.starfish_studios.bbb;

import com.starfish_studios.bbb.registry.BBBContent;
import net.fabricmc.api.ModInitializer;

public final class BuildingButBetter implements ModInitializer {
    public static final String MOD_ID = "bbb";

    @Override
    public void onInitialize() {
        BBBContent.initialize();
    }
}
