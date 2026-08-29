package dev.resivore.dragonbound.mixin;

import dev.resivore.dragonbound.DragonboundContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
abstract class ProtectedWaystoneItemEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void dragonboundWaystone$preventNaturalDespawn(CallbackInfo callback) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.getItem().is(DragonboundContent.WAYSTONE_ITEM)) {
            self.setUnlimitedLifetime();
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void dragonboundWaystone$preventDamage(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> callback
    ) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.getItem().is(DragonboundContent.WAYSTONE_ITEM)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "ignoreExplosion", at = @At("HEAD"), cancellable = true)
    private void dragonboundWaystone$ignoreExplosion(
            Explosion explosion,
            CallbackInfoReturnable<Boolean> callback
    ) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.getItem().is(DragonboundContent.WAYSTONE_ITEM)) {
            callback.setReturnValue(true);
        }
    }
}
