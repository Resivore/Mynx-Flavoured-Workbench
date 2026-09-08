package com.starfish_studios.bbb.mixin;

import com.starfish_studios.bbb.block.RopeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
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

    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void bbb$recognizeVerticalRope(CallbackInfoReturnable<Boolean> cir) {
        // In 26.2 these public methods are declared by Entity, not LivingEntity.
        // A @Shadow may only resolve a member declared by the mixin target, so use
        // the actual LivingEntity instance to access inherited public behavior.
        LivingEntity self = (LivingEntity) (Object) this;
        if (!cir.getReturnValue() && RopeBlock.isVerticalRope(self.getInBlockState())) {
            lastClimbablePos = Optional.of(self.blockPosition());
            cir.setReturnValue(true);
        }
    }
}
