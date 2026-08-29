package dev.resivore.slabdecorations.mixin;

import dev.resivore.slabdecorations.NibaruHorizontalSurface;
import dev.resivore.slabdecorations.PlantFamilyEligibility;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applied after Nibaru's mixin so this callback is inserted ahead of its broad bottom-BaseSlab
 * rejection at the shared HEAD. It resolves only this project's exact native-horizontal path.
 */
@Mixin(value = VegetationBlock.class, priority = 900)
public abstract class VegetationBlockSurvivalMixin {
    @Shadow
    protected abstract boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos);

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void slabDecorations$allowCanonicalHorizontalSurface(
            BlockState plantState,
            LevelReader level,
            BlockPos plantPos,
            CallbackInfoReturnable<Boolean> cir) {
        NibaruHorizontalSurface.Surface surface =
                NibaruHorizontalSurface.candidate(plantState, level, plantPos).orElse(null);
        if (surface != null) {
            boolean accepted = PlantFamilyEligibility.acceptsCanonicalParent(
                    plantState.getBlock(), surface.canonicalParentState());
            // Newly admitted feature outputs stay narrow to their emitting substrate. Outside that
            // mapping, preserve the pre-Canary-2 vanilla/Nibaru decision instead of imposing a ban.
            if (PlantFamilyEligibility.isSubstrateFeatureOutput(plantState.getBlock()) && !accepted) {
                return;
            }
            boolean allowed = !surface.waterlogged()
                    && accepted
                    && this.mayPlaceOn(surface.canonicalParentState(), level, surface.supportPos());
            // Resolve both true and false for this exact project-owned path. Otherwise a top or
            // double slab carrying a broad derived tag could bypass Canary 1's water/material gate.
            cir.setReturnValue(allowed);
        }
    }
}
