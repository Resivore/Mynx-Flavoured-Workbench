package dev.resivore.blockfamilies.mixin;

import dev.resivore.blockfamilies.cnm.runtime.AuditedShapeRuntime;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

/**
 * At a shared HEAD injection point, priority 1100 executes after the accepted
 * Nibaru provider's default-priority contribution, making its edges part of
 * collision validation while preserving its behavior for every non-IBF item.
 */
@Mixin(value = ShapeMap.class, priority = 1_100, remap = false)
abstract class ShapeMapIntegrationMixin {
    @Inject(method = "setMappings", at = @At("HEAD"), require = 1)
    private static void interchangeableBlockFamilies$addAuditedFamilies(
            List<ShapeMap.Mapping> mappings,
            boolean detailedLogs,
            CallbackInfo ci
    ) {
        AuditedShapeRuntime.addMappings(mappings);
    }

    @Inject(method = "setMappings", at = @At("TAIL"), require = 1)
    private static void interchangeableBlockFamilies$assertAuditedComponents(
            List<ShapeMap.Mapping> mappings,
            boolean detailedLogs,
            CallbackInfo ci
    ) {
        AuditedShapeRuntime.assertResolvedShapeMap();
    }

    /**
     * Dedicated clients receive the resolved graph through CNM's payload and
     * therefore bypass {@code setMappings}. Validate that synchronized graph
     * and initialize the local audited-member index before UI/equality hooks
     * consume it.
     */
    @Inject(method = "setShapeMaps", at = @At("TAIL"), require = 1)
    private static void interchangeableBlockFamilies$assertSynchronizedComponents(
            Map<Item, List<Item>> shapesByParent,
            Map<Item, Item> parentByShape,
            CallbackInfo ci
    ) {
        AuditedShapeRuntime.acceptSynchronizedShapeMap();
    }
}
