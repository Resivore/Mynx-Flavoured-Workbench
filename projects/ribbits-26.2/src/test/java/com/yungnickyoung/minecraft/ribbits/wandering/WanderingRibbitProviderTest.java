package com.yungnickyoung.minecraft.ribbits.wandering;

import com.yungnickyoung.minecraft.ribbits.entity.trade.WanderingRibbitTradeProviders;
import com.yungnickyoung.minecraft.ribbits.entity.trade.WanderingRibbitTradeSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WanderingRibbitProviderTest {
    @Test
    void nativeProviderIsPermanentAndSchemaVersioned() {
        var providers = WanderingRibbitTradeProviders.providers();
        assertTrue(providers.stream().anyMatch(provider ->
                provider.id().equals(WanderingRibbitTradeProviders.NATIVE_PROVIDER_ID)
                        && provider.schemaVersion() > 0));
        var nativeProvider = providers.stream()
                .filter(provider -> provider.id().equals(WanderingRibbitTradeProviders.NATIVE_PROVIDER_ID))
                .findFirst()
                .orElseThrow();
        assertThrows(IllegalArgumentException.class, () ->
                WanderingRibbitTradeProviders.register(nativeProvider));
    }

    @Test
    void providerSeedIsStableAndIsolatedById() {
        long first = WanderingRibbitTradeProviders.deriveProviderSeed(
                42L, WanderingRibbitTradeProviders.NATIVE_PROVIDER_ID);
        long replay = WanderingRibbitTradeProviders.deriveProviderSeed(
                42L, WanderingRibbitTradeProviders.NATIVE_PROVIDER_ID);
        long otherEntity = WanderingRibbitTradeProviders.deriveProviderSeed(
                43L, WanderingRibbitTradeProviders.NATIVE_PROVIDER_ID);
        assertEquals(first, replay);
        assertNotEquals(first, otherEntity);
    }

    @Test
    void snapshotRetainsExactProviderOfferRanges() {
        var nativeRange = new WanderingRibbitTradeSnapshot.ProviderRange(
                WanderingRibbitTradeProviders.NATIVE_PROVIDER_ID, 1, 0, 5);
        var snapshot = new WanderingRibbitTradeSnapshot(99L, List.of(nativeRange));
        assertEquals(99L, snapshot.seed());
        assertEquals(5, snapshot.totalOfferCount());
        assertEquals(nativeRange, snapshot.providers().getFirst());
    }
}
