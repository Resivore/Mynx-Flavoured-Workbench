package dev.resivore.xaeroemfcompat.mixin;
import dev.resivore.xaeroemfcompat.IconDiagnostics;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.icon.XaeroIcon;
import xaero.hud.minimap.radar.icon.RadarIconManager;
import xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache;
import xaero.hud.minimap.radar.icon.cache.id.RadarIconKey;
@Mixin(value=RadarIconEntityCache.class, remap=false)
abstract class RadarIconEntityCacheMixin {
    @Shadow @Final private EntityType<?> entityType;
    @Inject(method="add",at=@At("RETURN"),require=1)
    private void xaeroEmf$cache(RadarIconKey key,XaeroIcon icon,CallbackInfoReturnable<XaeroIcon> callback) {
        if(IconDiagnostics.owns(entityType)) {
            IconDiagnostics.context(EntityType.getKey(entityType).toString());
            try {IconDiagnostics.event(icon==RadarIconManager.FAILED?"CACHED_FAILED":"CACHED_SUCCESS",String.valueOf(key.getVariant()));}
            finally {IconDiagnostics.clearContext();}
        }
    }
}
