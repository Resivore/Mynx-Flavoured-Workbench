package dev.resivore.slabdecorations.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.resivore.slabdecorations.OptionalSurfaceAdapters;
import dev.resivore.slabdecorations.CanonicalSurvivalProjection;
import dev.resivore.slabdecorations.StructureGrowthTransaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Optional transaction around Enderscape's Veiled Sapling configured-feature attempt. */
@Pseudo
@Mixin(targets = "net.penumbra.enderscape.block.VeiledSaplingBlock", remap = false)
public abstract class EnderscapeVeiledSaplingGrowthMixin {
    @WrapMethod(method = "isValidBonemealTarget")
    private boolean slabDecorations$evaluateNativeBonemealTargetAgainstCanonicalSupport(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Operation<Boolean> original) {
        return CanonicalSurvivalProjection.evaluateWithCanonicalSupport(
                        state, level, pos, projected -> original.call(projected, pos, state))
                .orElseGet(() -> original.call(level, pos, state));
    }

    @WrapMethod(method = "advanceGrowth")
    private void slabDecorations$growAgainstCanonicalParent(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Operation<Void> original) {
        if (!OptionalSurfaceAdapters.isEnderscapeStructureGrowthReady(state)) {
            original.call(level, pos, state, random);
            return;
        }
        StructureGrowthTransaction.runEnderscapeGrowth(level, pos, state, () -> {
            original.call(level, pos, state, random);
            return !level.getBlockState(pos).is(state.getBlock());
        });
    }
}
