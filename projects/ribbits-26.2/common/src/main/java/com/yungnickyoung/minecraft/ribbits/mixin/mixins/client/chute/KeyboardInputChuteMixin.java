package com.yungnickyoung.minecraft.ribbits.mixin.mixins.client.chute;

import com.yungnickyoung.minecraft.ribbits.client.chute.ChuteClientController;
import net.minecraft.client.Options;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Samples the physical binding immediately after vanilla rebuilds KeyboardInput. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputChuteMixin {
    @Shadow
    @Final
    private Options options;

    @Inject(method = "tick", at = @At("RETURN"))
    private void ribbits$capturePhysicalJumpBeforeSyntheticInput(CallbackInfo ci) {
        ChuteClientController.capturePhysicalJump(this.options.keyJump.isDown());
    }
}
