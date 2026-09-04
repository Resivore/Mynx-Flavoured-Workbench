package dev.resivore.ribbitsxaeroicons;

import java.util.Objects;

/**
 * Extends Xaero's bounded variant identity while preserving its exact variant string lookup.
 * A record deliberately declares all three methods required by RadarIconEntityCache.
 */
public record RibbitCacheVariant(Object upstreamVariant, CacheIdentity identity) {
    public RibbitCacheVariant {
        Objects.requireNonNull(upstreamVariant, "upstreamVariant");
        Objects.requireNonNull(identity, "identity");
    }

    @Override
    public String toString() {
        return upstreamVariant.toString();
    }
}
