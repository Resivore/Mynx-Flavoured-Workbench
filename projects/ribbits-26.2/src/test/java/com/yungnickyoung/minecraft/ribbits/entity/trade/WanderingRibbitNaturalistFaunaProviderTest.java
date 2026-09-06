package com.yungnickyoung.minecraft.ribbits.entity.trade;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WanderingRibbitNaturalistFaunaProviderTest {
    @Test
    void approvedCatalogueIsExactAndNeverRestocks() {
        var provider = WanderingRibbitTradeProviders.providers().stream()
                .filter(value -> value.id().equals(WanderingRibbitTradeProviders.OPTIONAL_NATURALIST_FAUNA_PROVIDER_ID))
                .findFirst().orElseThrow();
        var fauna = WanderingRibbitNaturalistFaunaTradeProvider.fauna();
        assertEquals(28, fauna.size());
        assertEquals(2, WanderingRibbitNaturalistFaunaTradeProvider.OFFER_COUNT);
        assertEquals(WanderingRibbitTradeSnapshot.RestockPolicy.NEVER_RESTOCK, provider.restockPolicy());
        assertEquals(28, fauna.stream().map(value -> value.path() + ':' + value.baby()).distinct().count());
        assertEquals(Set.of(4, 6, 8, 12, 16, 20, 24), fauna.stream().map(value -> value.price()).collect(java.util.stream.Collectors.toSet()));
        assertTrue(fauna.stream().noneMatch(value -> Set.of("alligator", "ostrich", "tortoise", "snail", "crab", "rat")
                .contains(value.path())));
    }

    @Test
    void legacyRangesDefaultToOrdinaryRestockPolicy() {
        var range = new WanderingRibbitTradeSnapshot.ProviderRange(
                WanderingRibbitTradeProviders.NATIVE_PROVIDER_ID, 3, 0, 6);
        assertEquals(WanderingRibbitTradeSnapshot.RestockPolicy.ORDINARY, range.restockPolicy());
    }
}
