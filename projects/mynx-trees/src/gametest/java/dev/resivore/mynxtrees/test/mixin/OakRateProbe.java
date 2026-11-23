package dev.resivore.mynxtrees.test.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.resivore.mynxtrees.MossyOakGeneration;
import dev.resivore.mynxtrees.test.OakGrowthTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MossyOakGeneration.class)
abstract class OakRateProbe {
    // The control tree runs the same grower and decorators with only the timber roll disabled.
    @ModifyReturnValue(method = "denominator", at = @At("RETURN"))
    private static int observe(int rate, ConfiguredFeature<?, ?> feature, WorldGenLevel level, BlockPos origin) {
        if (!OakGrowthTests.TRACE.isBound()) return rate;
        var trace = OakGrowthTests.TRACE.get();
        var key = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getKey(feature);
        trace.features().add(key + "/" + rate);
        return trace.control() ? 0 : rate;
    }
}
