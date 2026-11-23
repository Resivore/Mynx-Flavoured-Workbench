package dev.resivore.mynxtrees.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.resivore.mynxtrees.MossyOakGeneration;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ConfiguredFeature.class)
abstract class ConfiguredOakMixin {
    @WrapMethod(method = "place")
    private boolean mynx$scope(WorldGenLevel level, ChunkGenerator gen, RandomSource random, BlockPos pos, Operation<Boolean> original) {
        int chance = MossyOakGeneration.denominator((ConfiguredFeature<?, ?>)(Object)this, level);
        if (chance == 0 && !MossyOakGeneration.CHANCE.isBound()) return original.call(level, gen, random, pos);
        return ScopedValue.where(MossyOakGeneration.CHANCE, chance).call(() -> original.call(level, gen, random, pos));
    }
}
