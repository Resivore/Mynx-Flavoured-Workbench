package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RibbitGeoIconProviderTest {
    private final RibbitGeoIconProvider provider = new RibbitGeoIconProvider();

    @Test
    void activatesOnlyForTheExactRibbitGeoFailurePath() {
        assertTrue(provider.supports("ribbits:ribbit", true, false));

        assertFalse(provider.supports("minecraft:frog", true, false));
        assertFalse(provider.supports("othermod:gecko_mob", true, false));
        assertFalse(provider.supports("ribbits:wandering_ribbit", true, false));
        assertFalse(provider.supports("ribbits:ribbit", false, false));
        assertFalse(provider.supports("ribbits:ribbit", true, true));
        assertFalse(provider.supports("ribbits:ribbit", false, true));
        assertFalse(provider.supports(null, true, false));
    }

    @Test
    void vanillaEmfAndUpstreamSuccessRemainOutsideTheGeoBranch() {
        assertFalse(provider.supports("minecraft:sheep", false, false));
        assertFalse(provider.supports("minecraft:horse", false, false));
        assertFalse(provider.supports("minecraft:sheep", false, true));
        assertFalse(provider.supports("ribbits:ribbit", true, true));
    }

    @Test
    void exactRibbitOwnsOneVariantClassEvenWhenFullRenderSupportDeclines() {
        assertTrue(RibbitGeoIconProvider.owns("ribbits:ribbit"));
        assertFalse(RibbitGeoIconProvider.owns("minecraft:frog"));
        assertFalse(RibbitGeoIconProvider.owns((String) null));

        CacheIdentity valid = new CacheIdentity(
                "ribbits:ribbit", "provider", "selector", "model", "texture",
                "profession", "normalized", false, 1L);
        CacheIdentity unresolved = new CacheIdentity(
                "ribbits:ribbit", "provider", "selector", "<unresolved>", "<unresolved>",
                "<unresolved>", "normalized", false, 1L);
        assertTrue(new RibbitCacheVariant("default", valid).getClass()
                == new RibbitCacheVariant("default", unresolved).getClass());
    }
}
