package com.yungnickyoung.minecraft.ribbits.world.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

/**
 * Final step of the shared explorer-map modifier. A successful vanilla map is retained; every
 * other result becomes a brand-new failed map so no persistent, transient, or modded stale
 * component can cross the failure boundary.
 */
public final class RibbitVillageExplorerResultFunction implements LootItemFunction {
    public static final RibbitVillageExplorerResultFunction INSTANCE =
            new RibbitVillageExplorerResultFunction();
    public static final MapCodec<RibbitVillageExplorerResultFunction> CODEC =
            MapCodec.unit(() -> INSTANCE);

    private RibbitVillageExplorerResultFunction() {
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext context) {
        return RibbitVillageExplorerMap.finalizeSearchResult(stack);
    }

    @Override
    public MapCodec<RibbitVillageExplorerResultFunction> codec() {
        return CODEC;
    }
}
