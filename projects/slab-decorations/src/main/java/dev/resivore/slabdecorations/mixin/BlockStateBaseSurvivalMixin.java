package dev.resivore.slabdecorations.mixin;

import dev.resivore.slabdecorations.CanonicalSurvivalProjection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.SporeBlossomBlock;
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

    /**
     * Some block implementations call their protected survival method directly from updateShape,
     * bypassing BlockState.canSurvive. Restore only an otherwise-removed state that the same
     * canonical attachment projection accepts; every non-air lifecycle transition is untouched.
     */
    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private void slabDecorations$preserveCanonicalParentAfterShapeUpdate(
            LevelReader level,
            ScheduledTickAccess scheduledTicks,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random,
            CallbackInfoReturnable<BlockState> cir) {
        BlockState state = (BlockState) (Object) this;
        if (slabDecorations$usesDirectUpdateSurvival(state)
                && cir.getReturnValue().isAir()
                && CanonicalSurvivalProjection.evaluate(state, level, pos).orElse(false)) {
            cir.setReturnValue(state);
        }
    }

    private static boolean slabDecorations$usesDirectUpdateSurvival(BlockState state) {
        return state.getBlock() instanceof BaseCoralPlantTypeBlock
                || state.getBlock() instanceof HangingRootsBlock
                || state.getBlock() instanceof SporeBlossomBlock;
    }
}
