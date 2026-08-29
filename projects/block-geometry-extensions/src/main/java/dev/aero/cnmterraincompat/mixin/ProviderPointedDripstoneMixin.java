package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends Nibaru's canonical Dripstone material contract to registered companion geometry. */
@Mixin(PointedDripstoneBlock.class)
public abstract class ProviderPointedDripstoneMixin {
    @Inject(method = "canGrow", at = @At("HEAD"), cancellable = true)
    private void cnmNibaru$allowProviderBoundDripstone(
            LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        var support = level.getBlockState(pos.above());
        var binding = NibaruProviderAdapter.runtimeBinding(support.getBlock()).orElse(null);
        if (binding == null || binding.profile().canonicalParent() != Blocks.DRIPSTONE_BLOCK) return;
        var sourceFluid = level.getFluidState(pos.above(2));
        cir.setReturnValue(support.getFluidState().is(FluidTags.WATER)
                || sourceFluid.is(FluidTags.WATER) && sourceFluid.isSource());
    }
}
