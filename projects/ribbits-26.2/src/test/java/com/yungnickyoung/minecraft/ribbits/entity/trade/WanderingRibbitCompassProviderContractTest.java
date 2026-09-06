package com.yungnickyoung.minecraft.ribbits.entity.trade;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WanderingRibbitCompassProviderContractTest {
    @Test
    void nativeAndOptionalCompassProvidersHaveStableIdsSchemasAndOrdering() {
        List<WanderingRibbitTradeProvider> providers = WanderingRibbitTradeProviders.providers();
        assertEquals(List.of(
                        WanderingRibbitTradeProviders.NATIVE_PROVIDER_ID,
                        WanderingRibbitTradeProviders.OPTIONAL_MATCHA_COMPASSES_PROVIDER_ID,
                        WanderingRibbitTradeProviders.OPTIONAL_NATURALIST_FAUNA_PROVIDER_ID),
                providers.stream().map(WanderingRibbitTradeProvider::id).toList());
        assertEquals(3, providers.get(0).schemaVersion());
        assertEquals(1, providers.get(1).schemaVersion());
        assertEquals(1, providers.get(2).schemaVersion());
        assertEquals(6, WanderingRibbitNativeTradeProvider.NATIVE_OFFER_COUNT);
    }
}
