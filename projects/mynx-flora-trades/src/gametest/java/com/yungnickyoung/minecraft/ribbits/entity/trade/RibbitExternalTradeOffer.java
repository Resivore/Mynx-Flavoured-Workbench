package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.yungnickyoung.minecraft.ribbits.module.RibbitTradeModule;

/** Test-only ABI fixture for bootstrapping the Florist POI GameTest. */
public record RibbitExternalTradeOffer(String id, String profession, int tier,
        RibbitTradeModule.CostSpec first, RibbitTradeModule.CostSpec second,
        RibbitTradeModule.StackRef result, int resultCount, int maxUses, int merchantXp,
        String selectionKey, int selectionOptions, int selectionOption) { }
