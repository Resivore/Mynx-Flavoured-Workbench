package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.ExternalMaterialCatalog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional lifecycle hook: Ribbits wood semantics have settled when this entrypoint returns. */
@Pseudo
@Mixin(targets = "com.yungnickyoung.minecraft.ribbits.fabric.RibbitsFabric", remap = false)
abstract class RibbitsInitializationMixin {
    @Inject(method = "onInitialize", at = @At("RETURN"), require = 0)
    private void cnmTerrainCompat$registerRibbits(CallbackInfo ci) {
        ExternalMaterialCatalog.registerProvider("ribbits");
    }
}
