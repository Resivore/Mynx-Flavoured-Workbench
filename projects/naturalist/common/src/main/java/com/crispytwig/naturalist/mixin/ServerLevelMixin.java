package com.crispytwig.naturalist.mixin;

import com.crispytwig.naturalist.server.entity.util.MobPart;
import com.crispytwig.naturalist.server.entity.util.MultipartLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "getEntityOrPart(I)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN"),
            cancellable = true)
    private void naturalist$resolveMobPart(int id, CallbackInfoReturnable<Entity> cir) {
        if (cir.getReturnValue() == null) {
            MobPart part = ((MultipartLevel) this).naturalist$getMobPart(id);
            if (part != null) {
                cir.setReturnValue(part);
            }
        }
    }
}
