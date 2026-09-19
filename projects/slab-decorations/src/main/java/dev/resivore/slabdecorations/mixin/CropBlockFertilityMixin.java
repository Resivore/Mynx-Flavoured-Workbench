package dev.resivore.slabdecorations.mixin;

import dev.resivore.slabdecorations.NibaruHorizontalSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Retains vanilla crop-growth math while projecting only exact BGE Farmland Slab support reads. */
@Mixin(CropBlock.class)
public abstract class CropBlockFertilityMixin {
    @Redirect(
            method = "getGrowthSpeed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/BlockGetter;getBlockState"
                            + "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState slabDecorations$projectExactBgeFarmlandForGrowth(
            BlockGetter level,
            BlockPos pos) {
        return NibaruHorizontalSurface.canonicalizeExactBgeFarmland(level.getBlockState(pos));
    }
}
