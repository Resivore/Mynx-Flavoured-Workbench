package com.starfish_studios.bbb.registry;

import dev.aero.cnmterraincompat.fixture.ExternalFixtureRegistry;

/** Minimal BBBContent shape preserving the authoritative static completion seam. */
public final class BBBContent {
    private static boolean initialized;

    public static synchronized void initialize() {
        if (initialized) return;
        ExternalFixtureRegistry.registerBuildingButBetter();
        initialized = true;
    }

    private BBBContent() {}
}
