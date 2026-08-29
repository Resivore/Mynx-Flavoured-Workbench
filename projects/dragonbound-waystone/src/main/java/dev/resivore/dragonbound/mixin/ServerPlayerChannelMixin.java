package dev.resivore.dragonbound.mixin;

import dev.resivore.dragonbound.channel.DragonboundChannelManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerChannelMixin {
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void dragonboundWaystone$cancelAfterAcceptedDamage(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (callback.getReturnValueZ()) {
            DragonboundChannelManager.onAcceptedDamage((ServerPlayer) (Object) this);
        }
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void dragonboundWaystone$cancelOnDeath(DamageSource source, CallbackInfo callback) {
        DragonboundChannelManager.onDeath((ServerPlayer) (Object) this);
    }
}
