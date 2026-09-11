package dev.resivore.naturalistxaeroicons.mixin;

import dev.resivore.naturalistxaeroicons.StarfishCaptureDiagnostic;
import dev.resivore.naturalistxaeroicons.ScorpionCaptureDiagnostic;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.icon.XaeroIcon;
import xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache;
import xaero.hud.minimap.radar.icon.cache.id.RadarIconKey;

/** Records the post-miss cache write, including Xaero's failed sentinel when present. */
@Mixin(value = RadarIconEntityCache.class, remap = false)
abstract class RadarIconEntityCacheMixin {
    @Shadow @Final private EntityType<?> entityType;

    @Inject(method = "add", at = @At("RETURN"), require = 1)
    private void naturalistXaeroIcons$observeStarfishCacheWrite(
            RadarIconKey key, XaeroIcon icon, CallbackInfoReturnable<XaeroIcon> callback) {
        StarfishCaptureDiagnostic.cacheWritten(entityType, icon);
        ScorpionCaptureDiagnostic.cacheWritten(entityType, icon);
    }
}
