package dev.resivore.matchafrost.mixin;

import dev.resivore.matchafrost.FrostProtectionTraversal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
abstract class PowderSnowBlockMixin {
    private static final String CAN_ENTITY_WALK_ON_POWDER_SNOW_METHOD =
            "canEntityWalkOnPowderSnow(Lnet/minecraft/world/entity/Entity;)Z";

    @Inject(
            method = CAN_ENTITY_WALK_ON_POWDER_SNOW_METHOD,
            at = @At("RETURN"),
            cancellable = true,
            require = 1)
    private static void matchaFrost$allowTraversal(
            Entity entity,
            CallbackInfoReturnable<Boolean> callbackInfo) {
        if (!callbackInfo.getReturnValueZ()
                && FrostProtectionTraversal.allowsPowderSnowTraversal(entity)) {
            callbackInfo.setReturnValue(true);
        }
    }
}
