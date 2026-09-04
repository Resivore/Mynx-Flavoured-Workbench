package dev.resivore.ribbitsxaeroicons.mixin;
import dev.resivore.ribbitsxaeroicons.GeoIconLog;
import dev.resivore.ribbitsxaeroicons.RibbitCacheVariant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.icon.XaeroIcon;
import xaero.hud.minimap.radar.icon.RadarIconManager;
import xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache;
import xaero.hud.minimap.radar.icon.cache.id.RadarIconKey;
/** Observes insertion without changing Xaero's cache or return value. */
@Mixin(value = RadarIconEntityCache.class, remap = false)
abstract class RadarIconEntityCacheMixin {
    @Inject(method = "add", at = @At("RETURN"), require = 1)
    private void ribbitsXaeroIcons$cacheResult(RadarIconKey key, XaeroIcon icon,
            CallbackInfoReturnable<XaeroIcon> callback) {
        if (key.getVariant() instanceof RibbitCacheVariant v) {
            GeoIconLog.stage("cache-insertion", v.identity(),
                    icon == RadarIconManager.FAILED ? "FAILED" : icon == null ? "null" : "success");
        }
    }
}
