package com.yungnickyoung.minecraft.ribbits.entity.trade;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WanderingRibbitNaturalistFaunaProviderTest {
    @Test
    void approvedCatalogueIsExactAndNeverRestocks() {
        var provider = WanderingRibbitTradeProviders.providers().stream()
                .filter(value -> value.id().equals(WanderingRibbitTradeProviders.OPTIONAL_NATURALIST_FAUNA_PROVIDER_ID))
                .findFirst().orElseThrow();
        var fauna = WanderingRibbitNaturalistFaunaTradeProvider.fauna();
        assertEquals(28, fauna.size());
        assertEquals(19, fauna.stream().filter(WanderingRibbitNaturalistFaunaTradeProvider.Fauna::baby).count());
        assertEquals(9, fauna.stream().filter(value -> !value.baby()).count());
        assertEquals(2, WanderingRibbitNaturalistFaunaTradeProvider.OFFER_COUNT);
        assertEquals(WanderingRibbitTradeSnapshot.RestockPolicy.NEVER_RESTOCK, provider.restockPolicy());
        assertEquals(28, fauna.stream().map(value -> value.path() + ':' + value.baby()).distinct().count());
        assertEquals(Set.of(4, 6, 8, 12, 16, 20, 24), fauna.stream().map(value -> value.price()).collect(java.util.stream.Collectors.toSet()));
        assertTrue(fauna.stream().noneMatch(value -> Set.of("alligator", "ostrich", "tortoise", "snail", "crab", "rat")
                .contains(value.path())));
    }

    @Test
    void babyFaunaKeepsTheNaturalistComponentArgumentAndTranslationKey() throws Exception {
        String source = Files.readString(Path.of(System.getProperty("projectRoot")).resolve(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/entity/trade/"
                        + "WanderingRibbitNaturalistFaunaTradeProvider.java"));

        assertTrue(source.contains("Component.translatable(\"trade.ribbits.naturalist_fauna.baby\", speciesName(path))"));
        assertTrue(source.contains("Component.translatable(\"entity.naturalist.\" + path)"));
        assertFalse(source.contains("Component.literal(\"Baby "));
    }

    @Test
    void legacyRangesDefaultToOrdinaryRestockPolicy() {
        var range = new WanderingRibbitTradeSnapshot.ProviderRange(
                WanderingRibbitTradeProviders.NATIVE_PROVIDER_ID, 3, 0, 6);
        assertEquals(WanderingRibbitTradeSnapshot.RestockPolicy.ORDINARY, range.restockPolicy());
    }
}
