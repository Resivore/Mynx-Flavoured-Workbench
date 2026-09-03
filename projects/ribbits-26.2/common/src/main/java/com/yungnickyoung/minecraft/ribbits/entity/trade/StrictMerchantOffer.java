package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.yungnickyoung.minecraft.ribbits.world.loot.RibbitVillageExplorerMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Optional;

/**
 * A normal serialized MerchantOffer with absence-aware component matching for audited fixed stacks.
 * Vanilla ItemCost predicates require listed components but permit unrelated extras; this subclass
 * additionally requires the complete component map for costs explicitly marked exact.
 */
public final class StrictMerchantOffer extends MerchantOffer {
    private final boolean exactA;
    private final boolean exactB;
    private final boolean failedMapA;
    private final boolean failedMapB;

    public StrictMerchantOffer(ItemCost first, Optional<ItemCost> second, ItemStack result,
                               int maxUses, int xp, float priceMultiplier,
                               boolean exactA, boolean exactB) {
        this(first, second, result, maxUses, xp, priceMultiplier,
                exactA, exactB, false, false);
    }

    public StrictMerchantOffer(ItemCost first, Optional<ItemCost> second, ItemStack result,
                               int maxUses, int xp, float priceMultiplier,
                               boolean exactA, boolean exactB,
                               boolean failedMapA, boolean failedMapB) {
        super(first, second, result, maxUses, xp, priceMultiplier);
        this.exactA = exactA;
        this.exactB = exactB;
        this.failedMapA = failedMapA;
        this.failedMapB = failedMapB;
    }

    private StrictMerchantOffer(MerchantOffer saved, StrictMerchantOffer template) {
        super(template.getItemCostA(), template.getItemCostB(), template.getResult().copy(),
                saved.getUses(), template.getMaxUses(), template.getXp(),
                template.getPriceMultiplier(), saved.getDemand());
        this.setSpecialPriceDiff(saved.getSpecialPriceDiff());
        this.exactA = template.exactA;
        this.exactB = template.exactB;
        this.failedMapA = template.failedMapA;
        this.failedMapB = template.failedMapB;
    }

    /**
     * Restores the server-only full-stack matcher after vanilla MerchantOffer codec decoding.
     * In particular, Benzene is defined by the absence of spawn-egg entity data, which the
     * positive-only ItemCost component predicate cannot serialize by itself.
     */
    public static MerchantOffer restoreFromTemplate(MerchantOffer saved, MerchantOffer template) {
        if (saved instanceof StrictMerchantOffer || !(template instanceof StrictMerchantOffer strict)
                || (!strict.exactA && !strict.exactB
                    && !strict.failedMapA && !strict.failedMapB)) {
            return saved;
        }
        return new StrictMerchantOffer(saved, strict);
    }

    @Override
    public boolean satisfiedBy(ItemStack first, ItemStack second) {
        if (this.exactA && !ItemStack.isSameItemSameComponents(
                first, this.getItemCostA().itemStack())) {
            return false;
        }
        if (this.exactB && !ItemStack.isSameItemSameComponents(
                second, this.getItemCostB().orElseThrow().itemStack())) {
            return false;
        }
        if (this.failedMapA && !RibbitVillageExplorerMap.isFailedMap(first)) {
            return false;
        }
        if (this.failedMapB && !RibbitVillageExplorerMap.isFailedMap(second)) {
            return false;
        }
        return super.satisfiedBy(first, second);
    }

    @Override
    public boolean take(ItemStack first, ItemStack second) {
        // Vanilla calls our overridden satisfiedBy() virtually, then consumes its computed
        // cost A (including any special-price adjustment) and exact cost B consistently.
        return super.take(first, second);
    }

    @Override
    public MerchantOffer copy() {
        return new StrictMerchantOffer(this, this);
    }
}
