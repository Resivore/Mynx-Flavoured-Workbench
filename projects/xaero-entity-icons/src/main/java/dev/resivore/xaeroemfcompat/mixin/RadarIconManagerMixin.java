package dev.resivore.xaeroemfcompat.mixin;
import dev.resivore.xaeroemfcompat.IconDiagnostics;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.radar.icon.RadarIconManager;
import xaero.hud.minimap.radar.icon.cache.RadarIconCache;
@Mixin(value=RadarIconManager.class, remap=false)
abstract class RadarIconManagerMixin {
    @Shadow @Final private RadarIconCache iconCache;
    @Inject(method="resetResources",at=@At("TAIL"),require=1)
    private void xaeroEmf$reload(CallbackInfo callback) {
        var caches=((RadarIconCacheAccessor)(Object)iconCache).xaeroEmf$getCache();
        int before=caches.size();
        caches.keySet().removeIf(IconDiagnostics::owns);
        IconDiagnostics.reload();
        IconDiagnostics.event("SUCCESS_AND_FAILED_CACHE_INVALIDATED","entityTypes="+(before-caches.size()));
    }
}
