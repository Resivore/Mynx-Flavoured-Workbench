package dev.resivore.slabdecorations.mixin;

import dev.resivore.slabdecorations.NibaruHorizontalSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents a half-block-lowered mushroom from producing a misaligned giant structure. */
@Mixin(MushroomBlock.class)
public abstract class MushroomBonemealMixin {
    @Inject(method = "isValidBonemealTarget", at = @At("HEAD"), cancellable = true)
    private void slabDecorations$rejectGiantGrowthFromLoweredMushroom(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            CallbackInfoReturnable<Boolean> cir) {
        if (!(state.getBlock() instanceof MushroomBlock)) return;
        NibaruHorizontalSurface.Surface surface =
                NibaruHorizontalSurface.supporting(state, level, pos).orElse(null);
        if (surface != null && surface.type() == SlabType.BOTTOM) {
            cir.setReturnValue(false);
        }
    }
}
