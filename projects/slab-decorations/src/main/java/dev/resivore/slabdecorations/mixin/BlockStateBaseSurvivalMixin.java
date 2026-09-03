package dev.resivore.slabdecorations.mixin;

import dev.resivore.slabdecorations.CanonicalSurvivalProjection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives every structurally eligible foliage family the same projected vanilla survival seam. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseSurvivalMixin {
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void slabDecorations$evaluateCanonicalParent(
            LevelReader level,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        if (CanonicalSurvivalProjection.isEvaluating()) return;
        BlockState state = (BlockState) (Object) this;
        CanonicalSurvivalProjection.evaluate(state, level, pos).ifPresent(cir::setReturnValue);
    }
}
