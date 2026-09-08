package dev.resivore.naturalistxaeroicons.mixin;

import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.common.icon.XaeroIcon;
import xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache;
import xaero.hud.minimap.radar.icon.cache.id.RadarIconKey;

/** Exposes Xaero's per-entity map only to remove the one stale native Bear raster by exact key. */
@Mixin(value = RadarIconEntityCache.class, remap = false)
interface RadarIconEntityCacheStorageAccessor {
    @Accessor("storage")
    Map<RadarIconKey, XaeroIcon> naturalistXaeroIcons$getStorage();
}
