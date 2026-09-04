package com.yungnickyoung.minecraft.ribbits.mixin.mixins.client.chute;

import com.yungnickyoung.minecraft.ribbits.client.chute.ChuteClientController;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Owner-only client mirror of the same pre/post-travel cap used by the server. */
@Mixin(Player.class)
public abstract class ClientPlayerChuteMovementMixin {
    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void ribbits$mirrorChuteBeforeTravel(Vec3 input, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer player) {
            ChuteClientController.applyOwnerDescentCap(player);
        }
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("RETURN"))
    private void ribbits$mirrorChuteAfterTravel(Vec3 input, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer player) {
            ChuteClientController.applyOwnerDescentCap(player);
        }
    }
}
