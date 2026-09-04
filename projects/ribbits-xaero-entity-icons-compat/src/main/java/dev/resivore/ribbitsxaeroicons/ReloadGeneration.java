package dev.resivore.ribbitsxaeroicons;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/** Resource generations make both successful and failed icon results reload-safe. */
public final class ReloadGeneration {
    private static final AtomicLong GENERATION = new AtomicLong();

    private ReloadGeneration() {
    }

    public static long current() {
        return GENERATION.get();
    }

    public static long invalidate() {
        return GENERATION.incrementAndGet();
    }

    /** Removes the entire Ribbit entity cache, including both successful and FAILED variants. */
    public static <K> boolean evictRibbitOuterCache(
            Map<K, ?> outerCache, Function<K, String> entityTypeIdentity) {
        Objects.requireNonNull(outerCache, "outerCache");
        Objects.requireNonNull(entityTypeIdentity, "entityTypeIdentity");
        return outerCache.keySet().removeIf(key ->
                RibbitGeoIconProvider.owns(entityTypeIdentity.apply(key)));
    }
}
