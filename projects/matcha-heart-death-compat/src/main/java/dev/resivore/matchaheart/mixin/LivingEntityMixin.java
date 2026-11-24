package dev.resivore.matchaheart.mixin;

import dev.resivore.matchaheart.MatchaHeartDeathCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"))
    private void matchaHeart$sanitizeHeldHearts(DamageSource source, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof ServerPlayer player) {
            MatchaHeartDeathCompat.sanitizeCrystal(player.getMainHandItem());
            MatchaHeartDeathCompat.sanitizeCrystal(player.getOffhandItem());
        }
    }
}
