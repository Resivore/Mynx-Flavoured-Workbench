package dev.resivore.slabdecorations.mixin;

import dev.resivore.slabdecorations.CanonicalSurvivalProjection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bridges pale hanging moss's private scheduled survival check to the canonical attachment. */
@Mixin(HangingMossBlock.class)
public abstract class HangingMossSurvivalMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void slabDecorations$preserveCanonicalCeilingAttachment(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci) {
        if (CanonicalSurvivalProjection.evaluate(state, level, pos).orElse(false)) {
            ci.cancel();
        }
    }
}
