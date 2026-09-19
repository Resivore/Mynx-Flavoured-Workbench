package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.ExternalMaterialCatalog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional lifecycle hook for the Enderscape source catalog. */
@Pseudo
@Mixin(targets = "net.penumbra.enderscape.Enderscape", remap = false)
abstract class EnderscapeInitializationMixin {
    @Inject(method = "onInitialize", at = @At("RETURN"), require = 0)
    private void cnmTerrainCompat$registerEnderscape(CallbackInfo ci) {
        ExternalMaterialCatalog.registerProvider("enderscape");
    }
}
