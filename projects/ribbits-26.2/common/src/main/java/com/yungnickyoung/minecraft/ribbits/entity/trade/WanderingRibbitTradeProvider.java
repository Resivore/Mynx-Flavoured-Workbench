package com.yungnickyoung.minecraft.ribbits.entity.trade;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Server-only extension point for permanent Wandering Ribbit offers.
 *
 * <p>Providers must use a stable ID and increment their schema version whenever the meaning or
 * deterministic output of their offers changes. Implementations may contribute no offers (for
 * example when an optional dependency is absent) without linking that dependency into Ribbits.</p>
 */
public interface WanderingRibbitTradeProvider {
    Identifier id();

    int schemaVersion();

    /**
     * Declares the persisted refresh boundary for the provider's exact offer range.  This is
     * deliberately independent of the Wandering Ribbit's current no-restock behavior so a
     * future retained-merchant feature can refresh ordinary merchandise without resurrecting
     * conservation-sensitive offers.
     */
    default WanderingRibbitTradeSnapshot.RestockPolicy restockPolicy() {
        return WanderingRibbitTradeSnapshot.RestockPolicy.ORDINARY;
    }

    void contributeOffers(WanderingRibbitTradeContext context, OfferCollector offers);

    @FunctionalInterface
    interface OfferCollector {
        void add(MerchantOffer offer);
    }
}
