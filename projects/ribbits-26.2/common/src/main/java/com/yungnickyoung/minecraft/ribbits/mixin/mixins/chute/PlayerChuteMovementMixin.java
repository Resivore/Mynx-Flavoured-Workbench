package com.yungnickyoung.minecraft.ribbits.mixin.mixins.chute;

import com.yungnickyoung.minecraft.ribbits.chute.ChuteServerController;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the authoritative terminal-descent cap both before and after vanilla travel. */
@Mixin(Player.class)
public abstract class PlayerChuteMovementMixin {
    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void ribbits$capChuteBeforeTravel(Vec3 input, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            ChuteServerController.applyDescentCap(player);
        }
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
    private void ribbits$capChuteAfterTravel(Vec3 input, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            ChuteServerController.applyDescentCap(player);
        }
    }
}
