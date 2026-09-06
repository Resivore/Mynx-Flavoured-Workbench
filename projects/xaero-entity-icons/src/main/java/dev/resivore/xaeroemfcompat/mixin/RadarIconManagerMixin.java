package dev.resivore.xaeroemfcompat.mixin;
import dev.resivore.xaeroemfcompat.IconDiagnostics;
import dev.resivore.xaeroemfcompat.EmfIconPartResolver;
import net.minecraft.client.model.EntityModel;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.radar.icon.RadarIconManager;
import xaero.hud.minimap.radar.icon.cache.RadarIconCache;
import xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache;
import xaero.hud.minimap.radar.icon.cache.id.RadarIconKey;
import xaero.hud.minimap.radar.icon.definition.RadarIconDefinition;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.common.icon.XaeroIcon;
@Mixin(value=RadarIconManager.class, remap=false)
abstract class RadarIconManagerMixin {
    @Shadow private boolean canPrerender;
    @Shadow @Final private RadarIconCache iconCache;
    @Inject(method="resetResources",at=@At("TAIL"),require=1)
    private void xaeroEmf$reload(CallbackInfo callback) {
        var caches=((RadarIconCacheAccessor)(Object)iconCache).xaeroEmf$getCache();
        int before=caches.size();
        caches.keySet().removeIf(IconDiagnostics::owns);
        IconDiagnostics.reload();
        IconDiagnostics.event("SUCCESS_AND_FAILED_CACHE_INVALIDATED","entityTypes="+(before-caches.size()));
    }

    /** Observes both supported root families before Xaero queries its cache. */
    @Inject(
            method="get(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityType;Lxaero/hud/minimap/radar/icon/definition/RadarIconDefinition;Lnet/minecraft/client/renderer/entity/EntityRenderer;FZZLxaero/hud/minimap/element/render/MinimapElementGraphics;Lcom/mojang/blaze3d/pipeline/RenderTarget;)Lxaero/common/icon/XaeroIcon;",
            at=@At("HEAD"),require=1)
    private void xaeroEmf$observeCurrentEmfModel(
            Entity entity, EntityType<?> type, RadarIconDefinition definition,
            EntityRenderer<?,?> renderer, float partialTick, boolean debug,
            boolean showVariant, MinimapElementGraphics graphics, RenderTarget target,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<XaeroIcon> callback) {
        if(entity instanceof LivingEntity && renderer instanceof LivingEntityRenderer<?,?,?> livingRenderer) {
            EntityModel<?> model=livingRenderer.getModel();
            if(EmfIconPartResolver.isSupportedEmfRoot(model.root())) {
                IconDiagnostics.observe(type);
            }
        }
    }

    /**
     * C6 changed a FAILED cache read into null unconditionally.  Xaero reads
     * the same cache while prerendering is unavailable; it then returns null,
     * retains FAILED, and C6 has already spent its sole retry.  Intercepting
     * this exact call lets a known EMF type retry only when Xaero can invoke
     * RadarIconCreator and replace the stored sentinel.
     */
    @Redirect(
            method="get(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityType;Lxaero/hud/minimap/radar/icon/definition/RadarIconDefinition;Lnet/minecraft/client/renderer/entity/EntityRenderer;FZZLxaero/hud/minimap/element/render/MinimapElementGraphics;Lcom/mojang/blaze3d/pipeline/RenderTarget;)Lxaero/common/icon/XaeroIcon;",
            at=@At(value="INVOKE", target="Lxaero/hud/minimap/radar/icon/cache/RadarIconEntityCache;get(Lxaero/hud/minimap/radar/icon/cache/id/RadarIconKey;)Lxaero/common/icon/XaeroIcon;"),
            require=1)
    private XaeroIcon xaeroEmf$retryFailedAtActualPrerender(
            RadarIconEntityCache cache, RadarIconKey key) {
        XaeroIcon cached=cache.get(key);
        EntityType<?> type=((RadarIconEntityCacheTypeAccessor)(Object)cache).xaeroEmf$getEntityType();
        if(cached!=RadarIconManager.FAILED || !IconDiagnostics.owns(type)) return cached;
        IconDiagnostics.context(EntityType.getKey(type).toString());
        try {
            if(IconDiagnostics.retryFailedOnceAtPrerender(type,key.getVariant(),canPrerender)) {
                IconDiagnostics.event("FAILED_RETRY_AT_PRERENDER",String.valueOf(key.getVariant()));
                return null;
            }
            if(!canPrerender) IconDiagnostics.event("FAILED_RETRY_DEFERRED_NO_PRERENDER",String.valueOf(key.getVariant()));
            return cached;
        } finally {IconDiagnostics.clearContext();}
    }

    @Inject(
            method="get(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityType;Lxaero/hud/minimap/radar/icon/definition/RadarIconDefinition;Lnet/minecraft/client/renderer/entity/EntityRenderer;FZZLxaero/hud/minimap/element/render/MinimapElementGraphics;Lcom/mojang/blaze3d/pipeline/RenderTarget;)Lxaero/common/icon/XaeroIcon;",
            at=@At("RETURN"),require=1)
    private void xaeroEmf$observeFinalManagerResult(
            Entity entity, EntityType<?> type, RadarIconDefinition definition,
            EntityRenderer<?,?> renderer, float partialTick, boolean debug,
            boolean showVariant, MinimapElementGraphics graphics, RenderTarget target,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<XaeroIcon> callback) {
        if(!IconDiagnostics.owns(type)) return;
        IconDiagnostics.context(EntityType.getKey(type).toString());
        try {
            XaeroIcon icon=callback.getReturnValue();
            IconDiagnostics.event(icon==null ? "MANAGER_RETURNED_NULL"
                    : icon==RadarIconManager.FAILED ? "MANAGER_RETURNED_FAILED"
                    : "MANAGER_RETURNED_ICON", "final");
        } finally {IconDiagnostics.clearContext();}
    }
}
