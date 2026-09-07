package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.ExternalMaterialCatalog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional lifecycle hook: captures tree fire/stripping/fuel setup at entrypoint completion. */
@Pseudo
@Mixin(targets = "dev.resivore.mynxtrees.MynxTrees", remap = false)
abstract class MynxTreesInitializationMixin {
    @Inject(method = "onInitialize", at = @At("RETURN"), require = 0)
    private void cnmTerrainCompat$registerMynxTrees(CallbackInfo ci) {
        ExternalMaterialCatalog.registerProvider("mynx_trees");
    }
}
