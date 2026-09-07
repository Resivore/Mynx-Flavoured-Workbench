package dev.resivore.naturalistxaeroicons.mixin;

import dev.resivore.naturalistxaeroicons.NaturalistModelContracts;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.radar.icon.RadarIconManager;
import xaero.hud.minimap.radar.icon.cache.RadarIconCache;
import xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache;

/** Resource reload may change a selected Naturalist model or texture; evict only our owned types. */
@Mixin(value = RadarIconManager.class, remap = false)
abstract class RadarIconManagerMixin {
    @Shadow @Final private RadarIconCache iconCache;
    @Inject(method = "resetResources()V", at = @At("TAIL"), require = 1)
    private void naturalistXaeroIcons$evictOwnedResults(CallbackInfo callback) {
        try {
            Map<EntityType<?>, RadarIconEntityCache> caches = ((RadarIconCacheAccessor) (Object) iconCache).naturalistXaeroIcons$getIconCacheMap();
            caches.keySet().removeIf(type -> EntityType.getKey(type).getNamespace().equals("naturalist")
                    && NaturalistModelContracts.isTargetId(EntityType.getKey(type).getPath()));
        } catch (RuntimeException ignored) { /* reload remains Xaero-owned if the private seam changed */ }
    }
}
