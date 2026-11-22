package com.fizzware.dramaticdoors.test.mixin;

import com.fizzware.dramaticdoors.fabric.FabricUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Loom marks tests as development launches; exercise the mod's installed-provider path. */
@Mixin(value = FabricUtils.class, remap = false)
abstract class ProductionCompatMixin {
    @Inject(method = "isDev", at = @At("HEAD"), cancellable = true)
    private void useInstalledMods(CallbackInfoReturnable<Boolean> result) {
        result.setReturnValue(false);
    }
}
