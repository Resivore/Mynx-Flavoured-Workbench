package dev.resivore.naturalistxaeroicons.mixin;

import java.util.Map;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.hud.minimap.radar.icon.cache.RadarIconCache;
import xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache;

@Mixin(value = RadarIconCache.class, remap = false)
interface RadarIconCacheAccessor {
    @Accessor("iconCacheMap")
    Map<EntityType<?>, RadarIconEntityCache> naturalistXaeroIcons$getIconCacheMap();
}
