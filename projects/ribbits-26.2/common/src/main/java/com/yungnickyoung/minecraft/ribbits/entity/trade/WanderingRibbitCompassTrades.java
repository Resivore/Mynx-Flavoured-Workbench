package com.yungnickyoung.minecraft.ribbits.entity.trade;

import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/** Component-aware compass buybacks shared by the native and optional trade providers. */
final class WanderingRibbitCompassTrades {
    private static final Identifier COMPASS_ID = Identifier.parse("minecraft:compass");
    private static final int MAX_USES = 2;

    private WanderingRibbitCompassTrades() {
    }

    static MerchantOffer buyback(ItemStack compass, Item glowcap, int glowcapCount) {
        if (glowcapCount <= 0) {
            throw new IllegalArgumentException("Compass buyback reward must be positive");
        }
        return new MerchantOffer(identityCost(compass),
                new ItemStack(glowcap, glowcapCount), MAX_USES, 0, 0.0F);
    }

    /**
     * Requires the audited item-model and item-name components while deliberately accepting
     * unrelated mutable data such as custom names and lodestone targets.
     */
    static ItemCost identityCost(ItemStack compass) {
        if (!COMPASS_ID.equals(compass.getItem().builtInRegistryHolder().key().identifier())) {
            throw new IllegalArgumentException("Compass buyback templates must use minecraft:compass");
        }
        if (!compass.has(DataComponents.ITEM_MODEL) || !compass.has(DataComponents.ITEM_NAME)) {
            throw new IllegalArgumentException(
                    "Compass buyback templates require item-model and item-name components");
        }
        ItemStack visibleCost = compass.copyWithCount(1);
        DataComponentExactPredicate identity = DataComponentExactPredicate.someOf(
                visibleCost.getComponents(), DataComponents.ITEM_MODEL, DataComponents.ITEM_NAME);
        return new ItemCost(visibleCost.typeHolder(), 1, identity, visibleCost);
    }
}
