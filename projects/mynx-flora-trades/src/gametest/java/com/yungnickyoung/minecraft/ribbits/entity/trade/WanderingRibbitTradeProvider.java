package com.yungnickyoung.minecraft.ribbits.entity.trade;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.trading.MerchantOffer;

/** Test-only API fixture. */
public interface WanderingRibbitTradeProvider {
    Identifier id();
    int schemaVersion();
    WanderingRibbitTradeSnapshot.RestockPolicy restockPolicy();
    void contributeOffers(WanderingRibbitTradeContext context, OfferCollector offers);

    interface OfferCollector {
        void add(MerchantOffer offer);
    }
}
