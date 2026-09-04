package com.yungnickyoung.minecraft.ribbits.entity.trade;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Optional all-or-zero buybacks for the three component-bearing Matcha compass tiers. */
final class WanderingRibbitMatchaCompassTradeProvider implements WanderingRibbitTradeProvider {
    private static final int SCHEMA_VERSION = 1;

    @Override
    public Identifier id() {
        return WanderingRibbitTradeProviders.OPTIONAL_MATCHA_COMPASSES_PROVIDER_ID;
    }

    @Override
    public int schemaVersion() {
        return SCHEMA_VERSION;
    }

    @Override
    public void contributeOffers(WanderingRibbitTradeContext context, OfferCollector offers) {
        // Resolve all three before contributing any. The outer provider staging is a second
        // atomicity boundary and persists a zero-width range if Matcha is absent or has drifted.
        List<ItemStack> compasses = MatchaCompassCatalog.resolveAll(context.level());
        Item glowcap = MatchaStackCatalog.requiredItem("ribbits:glowcap");
        resolvedOffers(compasses, glowcap).forEach(offers::add);
    }

    static List<net.minecraft.world.item.trading.MerchantOffer> resolvedOffers(
            List<ItemStack> compasses, Item glowcap) {
        if (compasses.size() != 3) {
            throw new IllegalArgumentException("Expected copper, golden and titanium compasses");
        }
        return List.of(
                WanderingRibbitCompassTrades.buyback(compasses.get(0), glowcap, 2),
                WanderingRibbitCompassTrades.buyback(compasses.get(1), glowcap, 4),
                WanderingRibbitCompassTrades.buyback(compasses.get(2), glowcap, 8)
        );
    }
}
