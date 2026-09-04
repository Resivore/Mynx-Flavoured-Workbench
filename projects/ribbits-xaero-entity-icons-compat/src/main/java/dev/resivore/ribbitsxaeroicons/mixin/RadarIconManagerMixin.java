package dev.resivore.ribbitsxaeroicons.mixin;

import dev.resivore.ribbitsxaeroicons.GeoIconLog;
import dev.resivore.ribbitsxaeroicons.ReloadGeneration;
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

/** Resource reload generation and targeted successful/FAILED Ribbit cache invalidation. */
@Mixin(value = RadarIconManager.class, remap = false)
abstract class RadarIconManagerMixin {
    @Shadow
    @Final
    private RadarIconCache iconCache;

    @Inject(method = "resetResources()V", at = @At("TAIL"), require = 1)
    private void ribbitsXaeroIcons$invalidateRibbitResources(CallbackInfo callback) {
        long generation = ReloadGeneration.invalidate();
        boolean evicted = false;
        try {
            Map<EntityType<?>, RadarIconEntityCache> caches =
                    ((RadarIconCacheAccessor) (Object) iconCache)
                            .ribbitsXaeroIcons$getIconCacheMap();
            evicted = ReloadGeneration.evictRibbitOuterCache(
                    caches, type -> EntityType.getKey(type).toString());
        } catch (Throwable failure) {
            GeoIconLog.failure("reload-eviction", failure);
        }
        GeoIconLog.reloaded(generation, evicted);
    }
}
