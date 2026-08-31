package com.starfish_studios.bbb.mixin;

import com.starfish_studios.bbb.block.UrnBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Minecraft 26.2 moved BushBlock's placement floor hook to VegetationBlock. */
@Mixin(VegetationBlock.class)
public abstract class VegetationBlockMixin {
    @Inject(method = "mayPlaceOn", at = @At("RETURN"), cancellable = true)
    private void bbb$allowSoiledUrn(BlockState floor, BlockGetter level, BlockPos pos,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && floor.getBlock() instanceof UrnBlock && floor.getValue(UrnBlock.SOILED)) {
            cir.setReturnValue(true);
        }
    }
}
