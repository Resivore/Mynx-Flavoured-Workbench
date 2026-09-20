package dev.resivore.bgebushyleaves.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReloadCacheTest {
    @Test void resourceReloadInvalidatesCanonicalAppearanceCapture() {
        ReloadCache<Object, String> cache = new ReloadCache<>();
        Object state = new Object();
        cache.put(state, "first-pack-sample");
        assertEquals("first-pack-sample", cache.find(state).orElseThrow());
        cache.clear();
        assertTrue(cache.find(state).isEmpty());
        cache.put(state, "replacement-pack-sample");
        assertEquals("replacement-pack-sample", cache.find(state).orElseThrow());
    }
}
