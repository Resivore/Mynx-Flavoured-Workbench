package com.yungnickyoung.minecraft.ribbits.entity.trade;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * A one-use offer whose server-side payment path rejects a stale completion after exhaustion.
 *
 * <p>Vanilla's menu normally hides an exhausted result, but {@link MerchantOffer#take(ItemStack,
 * ItemStack)} deliberately only validates payment.  That is too weak for a persisted, never-
 * restocking entitlement: a stale result-slot completion can otherwise consume payment and advance
 * uses past its limit.  The ordinary offer codec intentionally serializes this as a normal
 * {@code MerchantOffer}; {@link #restore(MerchantOffer)} reattaches this narrow guard after load.</p>
 */
final class WanderingRibbitOneShotOffer extends MerchantOffer {
    WanderingRibbitOneShotOffer(ItemCost cost, ItemStack result) {
        super(cost, result, 1, 0, 0.0F);
    }

    private WanderingRibbitOneShotOffer(MerchantOffer saved) {
        super(saved.getItemCostA(), saved.getItemCostB(), saved.getResult().copy(),
                saved.getUses(), saved.getMaxUses(), saved.getXp(),
                saved.getPriceMultiplier(), saved.getDemand());
        this.setSpecialPriceDiff(saved.getSpecialPriceDiff());
    }

    static MerchantOffer restore(MerchantOffer saved) {
        if (saved instanceof WanderingRibbitOneShotOffer || saved.getMaxUses() != 1) {
            return saved;
        }
        return new WanderingRibbitOneShotOffer(saved);
    }

    @Override
    public boolean take(ItemStack first, ItemStack second) {
        return !this.isOutOfStock() && super.take(first, second);
    }

    @Override
    public MerchantOffer copy() {
        return new WanderingRibbitOneShotOffer(this);
    }
}
