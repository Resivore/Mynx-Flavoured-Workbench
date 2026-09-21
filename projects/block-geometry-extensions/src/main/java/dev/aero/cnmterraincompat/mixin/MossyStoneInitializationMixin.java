package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.ExternalMaterialCatalog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional lifecycle hook: Mossy Stone's four provider-owned roles exist after entrypoint return. */
@Pseudo
@Mixin(targets = "dev.resivore.mossystone.MossyStoneMod", remap = false)
abstract class MossyStoneInitializationMixin {
    @Inject(method = "onInitialize", at = @At("RETURN"), require = 0)
    private void cnmTerrainCompat$registerMossyStone(CallbackInfo ci) {
        ExternalMaterialCatalog.registerProvider("mossy_stone");
    }
}
