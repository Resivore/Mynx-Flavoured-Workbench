package dev.resivore.xaerodiscovery.mixin;

import dev.resivore.xaerodiscovery.DiscoveryService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapWriter;

@Mixin(value = MapWriter.class, remap = false)
abstract class WorldMapWriterMixin {
    @Inject(
            method = "getWriteDistance()I",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            allow = 1,
            remap = false
    )
    private void xaeroDiscoveryRadius$clampWriteDistance(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(DiscoveryService.clampWorldMapWriteDistance(cir.getReturnValue()));
    }
}
