package dev.resivore.slabdecorations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.resivore.slabdecorations.StructureGrowthTransaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/** Covers random-tick and bonemeal tree growth through SaplingBlock's shared advanceTree path. */
@Mixin(SaplingBlock.class)
public abstract class SaplingGrowthMixin {
    @WrapMethod(method = "advanceTree")
    private void slabDecorations$growAgainstCanonicalParents(
            ServerLevel level, BlockPos pos, BlockState state, RandomSource random, Operation<Void> original) {
        StructureGrowthTransaction.run(level, pos, state, () -> {
            original.call(level, pos, state, random);
            return !level.getBlockState(pos).is(state.getBlock());
        });
    }
}
