package dev.resivore.sweetberryhorseimmunity.mixin;

import dev.resivore.sweetberryhorseimmunity.SweetBerryHorseImmunity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    private static final String CAN_FREEZE_METHOD = "canFreeze()Z";

    @Inject(method = CAN_FREEZE_METHOD, at = @At("HEAD"), cancellable = true, require = 1)
    private void horseImmunities$preventPowderSnowFreezing(
        CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        Entity entity = (Entity) (Object) this;
        if (SweetBerryHorseImmunity.shouldBypassPowderSnowEffects(entity)) {
            callbackInfo.setReturnValue(false);
        }
    }
}
