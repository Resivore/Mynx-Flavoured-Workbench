package com.starfish_studios.bbb.mixin;

import com.starfish_studios.bbb.block.RopeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Adds the one state-aware climbability case that the global CLIMBABLE tag cannot express. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityRopeClimbMixin {
    @Shadow private Optional<BlockPos> lastClimbablePos;

    @Shadow public abstract BlockState getInBlockState();

    @Shadow public abstract BlockPos blockPosition();

    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void bbb$recognizeVerticalRope(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && RopeBlock.isVerticalRope(getInBlockState())) {
            lastClimbablePos = Optional.of(blockPosition());
            cir.setReturnValue(true);
        }
    }
}
