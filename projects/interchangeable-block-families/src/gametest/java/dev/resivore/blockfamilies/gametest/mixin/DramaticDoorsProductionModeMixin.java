package dev.resivore.blockfamilies.gametest.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Loom marks GameTest launches as development environments. Dramatic Doors
 * interprets that flag as a request to instantiate every optional compatibility
 * family, including providers absent from this pinned stack. Force only its
 * development-mode query to production semantics so the harness registers the
 * same Macaw/vanilla families as an installed artifact would.
 */
@Mixin(targets = "com.fizzware.dramaticdoors.fabric.FabricUtils", remap = false)
abstract class DramaticDoorsProductionModeMixin {
    @Inject(method = "isDev", at = @At("HEAD"), cancellable = true, require = 1)
    private void interchangeableBlockFamilies$useProductionCompatSet(
            CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(false);
    }
}
