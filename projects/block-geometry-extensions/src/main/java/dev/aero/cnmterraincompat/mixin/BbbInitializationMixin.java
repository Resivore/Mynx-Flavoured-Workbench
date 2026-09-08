package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.ExternalMaterialCatalog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registers BBB's authoritative beam catalog only after all beam forms are present. */
@Pseudo
@Mixin(targets = "com.starfish_studios.bbb.registry.BBBContent", remap = false)
abstract class BbbInitializationMixin {
    @Inject(method = "initialize", at = @At("RETURN"))
    private static void cnmTerrainCompat$registerBbbBeams(CallbackInfo ci) {
        ExternalMaterialCatalog.registerProvider("bbb");
    }
}
