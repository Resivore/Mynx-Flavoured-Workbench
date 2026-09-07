package com.yungnickyoung.minecraft.ribbits.module;

import com.yungnickyoung.minecraft.ribbits.entity.trade.RibbitExternalTradeOffer;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Test-only API fixture: the POI GameTest does not execute Ribbits gameplay. */
public final class RibbitTradeModule {
    private RibbitTradeModule() { }

    public static void registerExternalOffers(Identifier id, List<RibbitExternalTradeOffer> offers) { }

    public record StackRef(String registryId) {
        public static StackRef item(String id) { return new StackRef(id); }
    }

    public record CostSpec(StackRef stack, int count, boolean exactComponents) { }
}
