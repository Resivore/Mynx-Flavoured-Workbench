package dev.resivore.ribbitsxaeroicons.mixin;

import java.util.Map;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.hud.minimap.radar.icon.cache.RadarIconCache;
import xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache;

/** Exact private-cache seam needed to evict only Ribbits during resource reload. */
@Mixin(value = RadarIconCache.class, remap = false)
interface RadarIconCacheAccessor {
    @Accessor("iconCacheMap")
    Map<EntityType<?>, RadarIconEntityCache> ribbitsXaeroIcons$getIconCacheMap();
}
