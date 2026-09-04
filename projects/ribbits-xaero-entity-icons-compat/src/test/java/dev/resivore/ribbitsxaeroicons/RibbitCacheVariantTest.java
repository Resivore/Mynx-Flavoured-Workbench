package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RibbitCacheVariantTest {
    @Test
    void cacheIdentityContainsOnlyBoundedStableInputs() {
        assertTrue(CacheIdentity.class.isRecord());
        assertEquals(List.of(
                        "entityType", "providerId", "selectorVersion", "modelId", "textureId",
                        "professionId", "babyPolicy", "pride", "reloadGeneration"),
                Arrays.stream(CacheIdentity.class.getRecordComponents())
                        .map(component -> component.getName())
                        .toList());
    }

    @Test
    void everyStableRenderingInputDistinguishesTheCacheIdentity() {
        CacheIdentity baseline = identity(
                "ribbits:ribbit", "ribbits", "main-body-direct-v1",
                "ribbits:geckolib/models/nitwit_ribbit.geo.json",
                "ribbits:textures/entity/ribbit.png", "nitwit", "normalize", false, 7L);

        assertNotEquals(baseline, identity("other:entity", "ribbits", "main-body-direct-v1",
                baseline.modelId(), baseline.textureId(), "nitwit", "normalize", false, 7L));
        assertNotEquals(baseline, identity("ribbits:ribbit", "other-provider", "main-body-direct-v1",
                baseline.modelId(), baseline.textureId(), "nitwit", "normalize", false, 7L));
        assertNotEquals(baseline, identity("ribbits:ribbit", "ribbits", "selector-v2",
                baseline.modelId(), baseline.textureId(), "nitwit", "normalize", false, 7L));
        assertNotEquals(baseline, identity("ribbits:ribbit", "ribbits", "main-body-direct-v1",
                "ribbits:geckolib/models/umbrella/nitwit/umbrella_1.geo.json",
                baseline.textureId(), "nitwit", "normalize", false, 7L));
        assertNotEquals(baseline, identity("ribbits:ribbit", "ribbits", "main-body-direct-v1",
                baseline.modelId(), "ribbits:textures/entity/chef.png", "nitwit", "normalize", false, 7L));
        assertNotEquals(baseline, identity("ribbits:ribbit", "ribbits", "main-body-direct-v1",
                baseline.modelId(), baseline.textureId(), "chef", "normalize", false, 7L));
        assertNotEquals(baseline, identity("ribbits:ribbit", "ribbits", "main-body-direct-v1",
                baseline.modelId(), baseline.textureId(), "nitwit", "preserve", false, 7L));
        assertNotEquals(baseline, identity("ribbits:ribbit", "ribbits", "main-body-direct-v1",
                baseline.modelId(), baseline.textureId(), "nitwit", "normalize", true, 7L));
        assertNotEquals(baseline, identity("ribbits:ribbit", "ribbits", "main-body-direct-v1",
                baseline.modelId(), baseline.textureId(), "nitwit", "normalize", false, 8L));
    }

    @Test
    void equalStableInputsReuseTheSameBoundedVariant() {
        Object upstream = new UpstreamVariant("xaero-variant-3");
        CacheIdentity identity = identity(
                "ribbits:ribbit", "ribbits", "main-body-direct-v1",
                "ribbits:model", "ribbits:texture", "merchant", "normalize", false, 2L);

        assertEquals(new RibbitCacheVariant(upstream, identity),
                new RibbitCacheVariant(upstream, identity));
        assertEquals(new RibbitCacheVariant(upstream, identity).hashCode(),
                new RibbitCacheVariant(upstream, identity).hashCode());
    }

    @Test
    void upstreamXaeroVariantRemainsPartOfIdentityButOwnsDisplayString() {
        CacheIdentity identity = identity(
                "ribbits:ribbit", "ribbits", "main-body-direct-v1",
                "ribbits:model", "ribbits:texture", "guard", "normalize", false, 0L);
        Object first = new UpstreamVariant("upstream-visible-value");
        Object second = new UpstreamVariant("different-upstream-value");

        RibbitCacheVariant wrapped = new RibbitCacheVariant(first, identity);
        assertEquals("upstream-visible-value", wrapped.toString());
        assertNotEquals(wrapped, new RibbitCacheVariant(second, identity));
    }

    private static CacheIdentity identity(
            String entityType,
            String providerId,
            String selectorVersion,
            String modelId,
            String textureId,
            String professionId,
            String babyPolicy,
            boolean pride,
            long reloadGeneration) {
        return new CacheIdentity(entityType, providerId, selectorVersion, modelId, textureId,
                professionId, babyPolicy, pride, reloadGeneration);
    }

    private record UpstreamVariant(String value) {
        @Override
        public String toString() {
            return value;
        }
    }
}
