package dev.aero.cnmterraincompat.mixin;

import dev.aero.cnmterraincompat.CanonicalShapeMapAudit;
import dev.aero.cnmterraincompat.NibaruProviderAdapter;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ShapeMap.class, remap = false)
abstract class ShapeMapOrderMixin {
    @Inject(method = "setMappings", at = @At("HEAD"), require = 1)
    private static void cnmTerrainCompat$addExactProviderFamilies(List<ShapeMap.Mapping> mappings,
            boolean logCircular, CallbackInfo ci) {
        NibaruProviderAdapter.addExactShapeMapEdges(mappings);
    }

    @Inject(method = "setMappings", at = @At("TAIL"), require = 1)
    private static void cnmTerrainCompat$orderProviderGeometryRoles(List<?> mappings, boolean logCircular, CallbackInfo ci) {
        NibaruProviderAdapter.applyProviderParentSegmentOrder();
        CanonicalShapeMapAudit.requireExplicitFamilies();
    }
}
