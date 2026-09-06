package dev.resivore.xaeroemfcompat.mixin;

import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache;

/** Reads only the cache's existing entity identity for the narrow retry gate. */
@Mixin(value = RadarIconEntityCache.class, remap = false)
interface RadarIconEntityCacheTypeAccessor {
    @Accessor("entityType")
    EntityType<?> xaeroEmf$getEntityType();
}
