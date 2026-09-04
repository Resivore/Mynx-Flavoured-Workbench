package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import com.yungnickyoung.minecraft.ribbits.world.loot.RibbitVillageExplorerMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Invokes the canonical Phase C reloadable item modifier exactly once for a merchant output. */
final class WanderingRibbitMapOffer {
    static final Identifier MODIFIER_ID = RibbitsCommon.id("ribbit_village_explorer_result");
    private static final ResourceKey<LootItemFunction> MODIFIER_KEY =
            ResourceKey.create(Registries.ITEM_MODIFIER, MODIFIER_ID);

    private WanderingRibbitMapOffer() {
    }

    static ItemStack materialize(WanderingRibbitTradeContext context) {
        LootItemFunction modifier = context.level()
                .getServer()
                .reloadableRegistries()
                .lookup()
                .lookupOrThrow(Registries.ITEM_MODIFIER)
                .getOrThrow(MODIFIER_KEY)
                .value();

        LootParams params = new LootParams.Builder(context.level())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(context.origin()))
                .create(LootContextParamSets.CHEST);
        LootContext lootContext = new LootContext.Builder(params)
                .withOptionalRandomSource(RandomSource.create(context.providerSeed()))
                .create(Optional.of(MODIFIER_ID));
        ItemStack result = modifier.apply(new ItemStack(Items.MAP), lootContext);
        if (!RibbitVillageExplorerMap.isSuccessfulMap(result)
                && !RibbitVillageExplorerMap.isFailedMap(result)) {
            throw new IllegalStateException(
                    "Canonical Ribbit Village explorer modifier returned an invalid result stack");
        }
        result.setCount(1);
        return result;
    }
}
