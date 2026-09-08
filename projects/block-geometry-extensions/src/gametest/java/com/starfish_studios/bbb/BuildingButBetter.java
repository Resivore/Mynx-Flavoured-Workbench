package com.starfish_studios.bbb;

import com.starfish_studios.bbb.registry.BBBContent;
import net.fabricmc.api.ModInitializer;

/** Minimal production-name BBB lifecycle fixture; its catalog class is the actual mixin target. */
public final class BuildingButBetter implements ModInitializer {
    @Override public void onInitialize() { BBBContent.initialize(); }
}
