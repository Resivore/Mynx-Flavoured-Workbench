package dev.resivore.matchafrost.mixin;

import dev.resivore.matchafrost.FrostProtectionTraversal;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    private static final String CAN_FREEZE_METHOD = "canFreeze()Z";

    @Inject(
            method = CAN_FREEZE_METHOD,
            at = @At("RETURN"),
            cancellable = true,
            require = 1)
    private void matchaFrost$preventVanillaFreezing(
            CallbackInfoReturnable<Boolean> callbackInfo) {
        if (callbackInfo.getReturnValueZ()
                && FrostProtectionTraversal.preventsVanillaFreezing(
                        (LivingEntity) (Object) this)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
