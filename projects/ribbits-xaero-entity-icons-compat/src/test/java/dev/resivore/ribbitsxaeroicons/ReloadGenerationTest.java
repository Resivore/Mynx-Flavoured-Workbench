package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReloadGenerationTest {
    @Test
    void invalidationAdvancesTheBoundedGenerationExactlyOnce() {
        long before = ReloadGeneration.current();

        ReloadGeneration.invalidate();
        assertEquals(before + 1L, ReloadGeneration.current());

        ReloadGeneration.invalidate();
        assertEquals(before + 2L, ReloadGeneration.current());
    }

    @Test
    void outerCacheEvictionDeclinesWhenNoExactRibbitEntityTypeExists() {
        CacheKey vanilla = new CacheKey("vanilla", "minecraft:frog");
        CacheKey emf = new CacheKey("emf", "minecraft:sheep");
        CacheKey unsupportedGeo = new CacheKey("other-gecko", "othermod:gecko_mob");
        CacheKey unresolved = new CacheKey("unresolved", null);
        Map<CacheKey, Object> cache = new LinkedHashMap<>();
        cache.put(vanilla, "VANILLA_ICON");
        cache.put(emf, "EMF_ICON");
        cache.put(unsupportedGeo, "FAILED");
        cache.put(unresolved, "FAILED");

        assertFalse(ReloadGeneration.evictRibbitOuterCache(cache, CacheKey::entityType));
        assertEquals(List.of(vanilla, emf, unsupportedGeo, unresolved),
                List.copyOf(cache.keySet()));
    }

    @Test
    void reloadEvictsEntireSuccessfulAndFailedRibbitOuterCachesOnly() {
        CacheKey ribbit = new CacheKey("ribbit", "ribbits:ribbit");
        CacheKey vanilla = new CacheKey("vanilla", "minecraft:frog");
        CacheKey emf = new CacheKey("emf", "minecraft:sheep");
        CacheKey unsupportedGeo = new CacheKey("other-gecko", "othermod:gecko_mob");
        Map<CacheKey, Object> cache = new LinkedHashMap<>();
        cache.put(ribbit, Map.of(
                "normal", "VALID_ICON",
                "umbrella", "FAILED"));
        cache.put(vanilla, Map.of("normal", "VANILLA_ICON"));
        cache.put(emf, Map.of("fresh-animations", "EMF_ICON"));
        cache.put(unsupportedGeo, Map.of("normal", "FAILED"));

        assertTrue(ReloadGeneration.evictRibbitOuterCache(cache, CacheKey::entityType));

        assertEquals(List.of(vanilla, emf, unsupportedGeo), List.copyOf(cache.keySet()));
        assertFalse(cache.containsKey(ribbit));
    }

    private record CacheKey(String name, String entityType) {
    }
}
