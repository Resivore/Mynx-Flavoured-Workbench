package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.List;

/** The permanent native five-offer Wandering Ribbit menu. */
final class WanderingRibbitNativeTradeProvider implements WanderingRibbitTradeProvider {
    static final int NATIVE_OFFER_COUNT = 5;
    private static final int SCHEMA_VERSION = 1;

    private static final Identifier GLOWCAP = RibbitsCommon.id("glowcap");
    private static final Identifier CHUTE_LEAF = RibbitsCommon.id("chute_leaf");
    private static final List<Curiosity> CURIOSITIES = List.of(
            new Curiosity(RibbitsCommon.id("red_toadstool"), 8),
            new Curiosity(RibbitsCommon.id("brown_toadstool"), 8),
            new Curiosity(RibbitsCommon.id("toadstool_stem"), 8),
            new Curiosity(RibbitsCommon.id("mossy_oak_planks"), 8),
            new Curiosity(RibbitsCommon.id("swamp_lantern"), 4),
            new Curiosity(RibbitsCommon.id("umbrella_leaf"), 8),
            new Curiosity(RibbitsCommon.id("swamp_daisy"), 8),
            new Curiosity(RibbitsCommon.id("giant_lilypad"), 4)
    );

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
                java.util.Optional.of(new ItemCost(Items.COMPASS, 1)),
                WanderingRibbitMapOffer.materialize(context),
                1, 0, 0.0F));

        ArrayList<Curiosity> pool = new ArrayList<>(CURIOSITIES);
        for (int i = 0; i < 3; i++) {
            int selected = i + context.random().nextInt(pool.size() - i);
            Curiosity swap = pool.get(i);
            pool.set(i, pool.get(selected));
            pool.set(selected, swap);

            Curiosity curiosity = pool.get(i);
            offers.add(new MerchantOffer(
                    new ItemCost(glowcap, 1),
                    new ItemStack(requireItem(curiosity.itemId()), curiosity.count()),
                    2, 0, 0.0F));
        }
    }

    private static Item requireItem(Identifier id) {
        return BuiltInRegistries.ITEM.getOptional(id)
                .orElseThrow(() -> new IllegalStateException("Required Wandering Ribbit trade item is missing: " + id));
    }

    private record Curiosity(Identifier itemId, int count) {
    }
}
