package dev.resivore.naturalistxaeroicons.mixin;

import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.common.icon.XaeroIcon;
import xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache;
import xaero.hud.minimap.radar.icon.cache.id.RadarIconKey;

/** Exposes Xaero's per-entity map only to observe the Brown Bear cache result for C11 logging. */
@Mixin(value = RadarIconEntityCache.class, remap = false)
interface RadarIconEntityCacheStorageAccessor {
    @Accessor("storage")
    Map<RadarIconKey, XaeroIcon> naturalistXaeroIcons$getStorage();
}
