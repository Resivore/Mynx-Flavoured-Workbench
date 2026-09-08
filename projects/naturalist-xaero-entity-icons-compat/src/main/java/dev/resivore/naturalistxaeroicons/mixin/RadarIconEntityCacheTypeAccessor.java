package dev.resivore.naturalistxaeroicons.mixin;

import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache;

/** Reads Xaero's existing cache owner solely to gate Brown Bear's stale-raster retry. */
@Mixin(value = RadarIconEntityCache.class, remap = false)
interface RadarIconEntityCacheTypeAccessor {
    @Accessor("entityType")
    EntityType<?> naturalistXaeroIcons$getEntityType();
}
