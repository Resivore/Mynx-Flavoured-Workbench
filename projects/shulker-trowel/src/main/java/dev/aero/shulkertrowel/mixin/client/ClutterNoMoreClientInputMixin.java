package dev.aero.shulkertrowel.mixin.client;

import dev.aero.shulkertrowel.client.TrowelShapeInput;
import dev.tazer.clutternomore.ClutterNoMoreClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClutterNoMoreClient.class, remap = false)
abstract class ClutterNoMoreClientInputMixin {
    @Inject(method = "onKeyInput(II)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void shulkerTrowel$routeExistingShapeKey(int key, int action, CallbackInfo ci) {
        if (TrowelShapeInput.tryHandle(key, action)) ci.cancel();
    }
}
