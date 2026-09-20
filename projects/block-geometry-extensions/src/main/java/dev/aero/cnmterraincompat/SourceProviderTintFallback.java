package dev.aero.cnmterraincompat;

/**
 * Provider-owned JSON/model tint can legitimately have no BlockColors callback.  Minecraft's
 * identity color multiplier preserves that authored appearance without manufacturing a biome
 * color or dereferencing a missing callback.
 */
final class SourceProviderTintFallback {
    static final int IDENTITY_MULTIPLIER = -1;

    private SourceProviderTintFallback() {}
}
