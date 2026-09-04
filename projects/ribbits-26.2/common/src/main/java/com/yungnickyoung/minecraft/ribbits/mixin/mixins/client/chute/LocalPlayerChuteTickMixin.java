package com.yungnickyoung.minecraft.ribbits.mixin.mixins.client.chute;

import com.yungnickyoung.minecraft.ribbits.client.chute.ChuteClientController;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Flushes a captured edge after vanilla's normal player-input packet path. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerChuteTickMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void ribbits$flushChutePressAfterVanillaInput(CallbackInfo ci) {
        ChuteClientController.flushAfterVanillaInput((LocalPlayer) (Object) this);
    }
}
