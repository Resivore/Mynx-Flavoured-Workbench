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

/** Optional shared feature seam for Celestial and vertical Murublight chanterelles. */
@Pseudo
@Mixin(targets = {
        "net.penumbra.enderscape.block.CelestialChanterelleBlock",
        "net.penumbra.enderscape.block.MurublightChanterelleBlock"
}, remap = false)
public abstract class EnderscapeChanterelleGrowthMixin {
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

    @WrapMethod(method = "performBonemeal")
    private void slabDecorations$growAgainstCanonicalParent(
            ServerLevel level,
            RandomSource random,
            BlockPos pos,
            BlockState state,
            Operation<Void> original) {
        if (!OptionalSurfaceAdapters.isEnderscapeStructureGrowthReady(state)) {
            original.call(level, random, pos, state);
            return;
        }
        StructureGrowthTransaction.runEnderscapeGrowth(level, pos, state, () -> {
            original.call(level, random, pos, state);
            return !level.getBlockState(pos).is(state.getBlock());
        });
    }
}
