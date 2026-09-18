package dev.resivore.slabdecorations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.resivore.slabdecorations.StructureGrowthTransaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Optional Ribbits seam around only the private huge-feature attempt.  The public bonemeal
 * method, its 0.4 roll, feature choice, and same-colour spread fallback remain entirely owned by
 * Ribbits; a missing Ribbits installation simply has no mixin target.
 */
@Pseudo
@Mixin(targets = "com.yungnickyoung.minecraft.ribbits.block.ToadstoolBlock", remap = false)
public abstract class RibbitsToadstoolGrowthMixin {
    @WrapMethod(method = "growHugeToadstool")
    private boolean slabDecorations$growHugeToadstoolAgainstCanonicalParent(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Operation<Boolean> original) {
        return StructureGrowthTransaction.run(level, pos, state,
                () -> original.call(level, pos, state, random));
    }
}
