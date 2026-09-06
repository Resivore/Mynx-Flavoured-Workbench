package com.crispytwig.naturalist.registry;

import com.crispytwig.naturalist.Naturalist;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import com.crispytwig.naturalist.platform.registry.DeferredHolder;
import com.crispytwig.naturalist.platform.registry.DeferredRegister;

public class NaturalistFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(BuiltInRegistries.FEATURE, Naturalist.MOD_ID);

}
