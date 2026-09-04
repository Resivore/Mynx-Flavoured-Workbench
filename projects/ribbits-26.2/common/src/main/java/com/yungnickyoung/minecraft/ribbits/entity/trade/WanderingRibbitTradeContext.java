package com.yungnickyoung.minecraft.ribbits.entity.trade;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import java.util.Objects;

/** Immutable server context and provider-isolated random stream for one offer materialization. */
public record WanderingRibbitTradeContext(
        ServerLevel level,
        BlockPos origin,
        long entitySeed,
        long providerSeed,
        RandomSource random
) {
    public WanderingRibbitTradeContext {
        Objects.requireNonNull(level, "level");
        origin = Objects.requireNonNull(origin, "origin").immutable();
        Objects.requireNonNull(random, "random");
    }
}
