package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.ExternalMaterialCatalog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional lifecycle hook: AMC registers its patterned blocks inside its own post-Enderscape callback. */
@Pseudo
@Mixin(targets = "dev.resivore.amc.EnderscapeFamilies", remap = false)
abstract class AmcEnderscapeInitializationMixin {
    @Inject(method = "registerAfterProvider", at = @At("RETURN"), require = 0)
    private static void cnmTerrainCompat$registerAmcEnderscape(CallbackInfo ci) {
        ExternalMaterialCatalog.registerAmcEnderscapePatterns();
    }
}
