package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/** The permanent native Wandering Ribbit menu. */
final class WanderingRibbitNativeTradeProvider implements WanderingRibbitTradeProvider {
    static final int NATIVE_OFFER_COUNT = 3;
    /** C18 removes the three randomized Curiosity entries from the persisted native range. */
    static final int SCHEMA_VERSION = 4;

    private static final Identifier GLOWCAP = RibbitsCommon.id("glowcap");
    private static final Identifier CHUTE_LEAF = RibbitsCommon.id("chute_leaf");

    @Override
    public Identifier id() {
        return WanderingRibbitTradeProviders.NATIVE_PROVIDER_ID;
    }

    @Override
    public int schemaVersion() {
        return SCHEMA_VERSION;
    }

    @Override
    public void contributeOffers(WanderingRibbitTradeContext context, OfferCollector offers) {
        Item glowcap = requireItem(GLOWCAP);
        offers.add(new MerchantOffer(
                new ItemCost(glowcap, 20),
                new ItemStack(requireItem(CHUTE_LEAF)),
                1, 0, 0.0F));
        offers.add(new MerchantOffer(
                new ItemCost(glowcap, 8),
                WanderingRibbitMapOffer.materialize(context),
                1, 0, 0.0F));

        // C18 keeps the ordinary compass buyback after the two permanent native offers.
        offers.add(WanderingRibbitCompassTrades.buyback(
                new ItemStack(Items.COMPASS), glowcap, 4));
    }

    private static Item requireItem(Identifier id) {
        return BuiltInRegistries.ITEM.getOptional(id)
                .orElseThrow(() -> new IllegalStateException("Required Wandering Ribbit trade item is missing: " + id));
    }
}
