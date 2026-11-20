package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.ExternalMaterialCatalog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional lifecycle hook: AMC C21 registers its last four patterned parents in this entrypoint. */
@Pseudo
@Mixin(targets = "dev.resivore.amc.EndStoneBricksBlocks", remap = false)
abstract class AmcPatternsInitializationMixin {
    @Inject(method = "onInitialize", at = @At("RETURN"), require = 0)
    private void cnmTerrainCompat$registerAmcPatterns(CallbackInfo ci) {
        ExternalMaterialCatalog.registerProvider("architectural_material_closure");
    }
}
