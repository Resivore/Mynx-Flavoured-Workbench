package dev.resivore.mynxfloratrades;

import com.yungnickyoung.minecraft.ribbits.entity.trade.RibbitExternalTradeOffer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FloraTradeCatalogTest {
    @Test
    void farmerChoicesAlwaysSpendOneGlowcapForTheExactFloraStack() {
        List<RibbitExternalTradeOffer> offers = FloraTradeCatalog.farmerOffers();
        assertEquals(4, offers.size());
        assertFarmer(offers.get(0), 1, 1, "tier_1", 0, "mynx_regions_unexplored:clover", 64);
        assertFarmer(offers.get(1), 1, 1, "tier_1", 1, "mynx_regions_unexplored:stone_bud", 32);
        assertFarmer(offers.get(2), 2, 2, "tier_2", 0, "mynx_regions_unexplored:barley", 32);
        assertFarmer(offers.get(3), 2, 2, "tier_2", 1, "mynx_regions_unexplored:windswept_grass", 32);
    }

    @Test
    void wanderingGroupsAlwaysSpendOneGlowcapAndRetainTheGlowleafRegistryId() {
        List<List<FloraTradeCatalog.Offer>> groups = FloraTradeCatalog.wanderingGroups();
        assertEquals(2, groups.size());
        assertWandering(groups.get(0).get(0), "mynx_regions_unexplored:dropleaf", 16);
        assertWandering(groups.get(0).get(1), "mynx_regions_unexplored:mycotoxic_daisy", 16);
        assertWandering(groups.get(1).get(0), "mynx_regions_unexplored:cattail", 16);
        assertWandering(groups.get(1).get(1), "mynx_regions_unexplored:duckweed", 32);
    }

    private static void assertFarmer(RibbitExternalTradeOffer offer, int tier, int merchantXp, String selectionKey,
                                     int selectionOption, String output, int outputCount) {
        assertEquals("farmer", offer.profession());
        assertEquals(tier, offer.tier());
        assertEquals("ribbits:glowcap", offer.first().stack().registryId());
        assertEquals(1, offer.first().count());
        assertNull(offer.second());
        assertEquals(output, offer.result().registryId());
        assertEquals(outputCount, offer.resultCount());
        assertEquals(merchantXp, offer.merchantXp());
        assertEquals(selectionKey, offer.selectionKey());
        assertEquals(2, offer.selectionOptions());
        assertEquals(selectionOption, offer.selectionOption());
    }

    private static void assertWandering(FloraTradeCatalog.Offer offer, String output, int outputCount) {
        assertEquals("ribbits:glowcap", offer.inputId());
        assertEquals(1, offer.inputCount());
        assertEquals(output, offer.outputId());
        assertEquals(outputCount, offer.outputCount());
    }
}
