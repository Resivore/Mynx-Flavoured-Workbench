package dev.resivore.blockfamilies.mixin;

import dev.resivore.blockfamilies.cnm.runtime.AuditedShapeRuntime;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

/**
 * CNM 2.0.7 represents its disabled startup option by calling setMappings with
 * immutable List.of(). At a shared HEAD injection point, the lower Mixin
 * priority executes first; this guard therefore preserves that off switch and
 * returns before mapping providers (including the accepted Nibaru provider)
 * can append to the immutable sentinel.
 */
@Mixin(value = ShapeMap.class, priority = 800, remap = false)
abstract class ShapeMapDisabledModeMixin {
    @Inject(method = "setMappings", at = @At("HEAD"), cancellable = true, require = 1)
    private static void interchangeableBlockFamilies$honorDisabledShapeMap(
            List<ShapeMap.Mapping> mappings,
            boolean detailedLogs,
            CallbackInfo ci
    ) {
        if (!mappings.isEmpty()) {
            return;
        }

        AuditedShapeRuntime.disable();
        ShapeMap.setShapeMaps(Map.of(), Map.of());
        ci.cancel();
    }
}
