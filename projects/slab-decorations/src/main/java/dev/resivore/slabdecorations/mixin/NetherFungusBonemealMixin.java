package dev.resivore.slabdecorations.mixin;

import dev.resivore.slabdecorations.NibaruHorizontalSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.NetherFungusBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the small fungus while deferring half-height huge-fungus generation. */
@Mixin(NetherFungusBlock.class)
public abstract class NetherFungusBonemealMixin {
    @Inject(method = "isValidBonemealTarget", at = @At("HEAD"), cancellable = true)
    private void slabDecorations$rejectHugeFungusFromLoweredPlant(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            CallbackInfoReturnable<Boolean> cir) {
        NibaruHorizontalSurface.Surface surface =
                NibaruHorizontalSurface.supporting(state, level, pos).orElse(null);
        if (surface != null && surface.type() == SlabType.BOTTOM) {
            cir.setReturnValue(false);
        }
    }
}
