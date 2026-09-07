package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.ExternalMaterialCatalog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional lifecycle hook: Macaw's path blocks exist only after this entrypoint returns. */
@Pseudo
@Mixin(targets = "com.mcwpaths.kikoz.MacawsPaths", remap = false)
abstract class MacawsPathsInitializationMixin {
    @Inject(method = "onInitialize", at = @At("RETURN"), require = 0)
    private void cnmTerrainCompat$registerMacawsPaths(CallbackInfo ci) {
        ExternalMaterialCatalog.registerProvider("mcwpaths");
    }
}
