package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.ExternalMaterialCatalog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registers the explicit C101 roots only after AMC's fixed C4 registry has completed. */
@Pseudo
@Mixin(targets = "dev.resivore.amc.ArchitecturalMaterialClosure", remap = false)
abstract class ArchitecturalMaterialClosureInitializationMixin {
    @Inject(method = "onInitialize", at = @At("RETURN"), require = 0)
    private void bge$registerAmcPatternRoots(CallbackInfo ci) {
        ExternalMaterialCatalog.registerProvider("architectural_material_closure");
    }
}
