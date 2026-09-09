package dev.resivore.naturalistxaeroicons.mixin;

import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.common.icon.XaeroIcon;
import xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache;
import xaero.hud.minimap.radar.icon.cache.id.RadarIconKey;

/** Reads Xaero's existing cache map solely to distinguish a Clam hit from a miss. */
@Mixin(value = RadarIconEntityCache.class, remap = false)
interface RadarIconEntityCacheStorageAccessor {
    @Accessor("storage")
    Map<RadarIconKey, XaeroIcon> naturalistXaeroIcons$getStorage();
}
