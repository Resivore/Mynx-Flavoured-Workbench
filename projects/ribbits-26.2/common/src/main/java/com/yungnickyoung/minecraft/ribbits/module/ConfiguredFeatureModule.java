package com.yungnickyoung.minecraft.ribbits.module;

import com.yungnickyoung.minecraft.ribbits.RibbitsCommon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public final class ConfiguredFeatureModule {
    public static final ResourceKey<ConfiguredFeature<?, ?>> HUGE_RED_TOADSTOOL = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, RibbitsCommon.id("huge_red_toadstool"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> HUGE_BROWN_TOADSTOOL = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, RibbitsCommon.id("huge_brown_toadstool"));

    private ConfiguredFeatureModule() {
    }
}
