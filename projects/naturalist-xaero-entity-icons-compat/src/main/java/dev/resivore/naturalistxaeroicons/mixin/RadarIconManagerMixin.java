package dev.resivore.naturalistxaeroicons.mixin;

import dev.resivore.naturalistxaeroicons.BrownBearDiagnostic;
import dev.resivore.naturalistxaeroicons.NaturalistModelContracts;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.radar.icon.cache.id.armor.RadarIconArmor;
import xaero.common.icon.XaeroIcon;
import xaero.hud.minimap.radar.icon.RadarIconManager;
import xaero.hud.minimap.radar.icon.cache.RadarIconCache;
import xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache;
import xaero.hud.minimap.radar.icon.cache.id.RadarIconKey;

/** Resource reload may change a selected Naturalist model or texture; evict only our owned types. */
@Mixin(value = RadarIconManager.class, remap = false)
abstract class RadarIconManagerMixin {
    @Shadow @Final private RadarIconCache iconCache;
    @Shadow private boolean canPrerender;
    @Inject(method = "resetResources()V", at = @At("TAIL"), require = 1)
    private void naturalistXaeroIcons$evictOwnedResults(CallbackInfo callback) {
        try {
            Map<EntityType<?>, RadarIconEntityCache> caches = ((RadarIconCacheAccessor) (Object) iconCache).naturalistXaeroIcons$getIconCacheMap();
            caches.keySet().removeIf(type -> EntityType.getKey(type).getNamespace().equals("naturalist")
                    && NaturalistModelContracts.isTargetId(EntityType.getKey(type).getPath()));
            BrownBearDiagnostic.resourceReloaded();
        } catch (RuntimeException ignored) { /* reload remains Xaero-owned if the private seam changed */ }
    }

    @Inject(
            method = "get(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityType;Lxaero/hud/minimap/radar/icon/definition/RadarIconDefinition;Lnet/minecraft/client/renderer/entity/EntityRenderer;FZZLxaero/hud/minimap/element/render/MinimapElementGraphics;Lcom/mojang/blaze3d/pipeline/RenderTarget;)Lxaero/common/icon/XaeroIcon;",
            at = @At("HEAD"), require = 0)
    private void naturalistXaeroIcons$startBrownBearDiagnostic(
            net.minecraft.world.entity.Entity entity, EntityType<?> type,
            xaero.hud.minimap.radar.icon.definition.RadarIconDefinition definition,
            net.minecraft.client.renderer.entity.EntityRenderer<?, ?> renderer, float partialTick,
            boolean debug, boolean showVariant,
            xaero.hud.minimap.element.render.MinimapElementGraphics graphics,
            com.mojang.blaze3d.pipeline.RenderTarget target,
            CallbackInfoReturnable<XaeroIcon> callback) {
        BrownBearDiagnostic.requestStarted(entity, canPrerender);
    }

    /** Observes the exact native cache read without changing cache contents or redirect ownership. */
    @Inject(
            method = "get(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityType;Lxaero/hud/minimap/radar/icon/definition/RadarIconDefinition;Lnet/minecraft/client/renderer/entity/EntityRenderer;FZZLxaero/hud/minimap/element/render/MinimapElementGraphics;Lcom/mojang/blaze3d/pipeline/RenderTarget;)Lxaero/common/icon/XaeroIcon;",
            at = @At(value = "INVOKE", target = "Lxaero/hud/minimap/radar/icon/cache/RadarIconEntityCache;get(Lxaero/hud/minimap/radar/icon/cache/id/RadarIconKey;)Lxaero/common/icon/XaeroIcon;", shift = At.Shift.BEFORE),
            locals = org.spongepowered.asm.mixin.injection.callback.LocalCapture.CAPTURE_FAILHARD,
            require = 0)
    private void naturalistXaeroIcons$observeBrownBearCacheBeforeXaeroEmfRetry(
            net.minecraft.world.entity.Entity entity, EntityType<?> type,
            xaero.hud.minimap.radar.icon.definition.RadarIconDefinition definition,
            net.minecraft.client.renderer.entity.EntityRenderer<?, ?> renderer, float partialTick,
            boolean debug, boolean showVariant,
            xaero.hud.minimap.element.render.MinimapElementGraphics graphics,
            com.mojang.blaze3d.pipeline.RenderTarget target,
            CallbackInfoReturnable<XaeroIcon> callback,
            net.minecraft.client.renderer.entity.state.EntityRenderState state, Object variant,
            RadarIconArmor armor, RadarIconEntityCache cache, RadarIconKey key) {
        Map<RadarIconKey, XaeroIcon> storage = ((RadarIconEntityCacheStorageAccessor) (Object) cache)
                .naturalistXaeroIcons$getStorage();
        if (BrownBearDiagnostic.isBrownBear(entity)) BrownBearDiagnostic.cacheLookup(storage.containsKey(key));
    }

    @Inject(
            method = "get(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityType;Lxaero/hud/minimap/radar/icon/definition/RadarIconDefinition;Lnet/minecraft/client/renderer/entity/EntityRenderer;FZZLxaero/hud/minimap/element/render/MinimapElementGraphics;Lcom/mojang/blaze3d/pipeline/RenderTarget;)Lxaero/common/icon/XaeroIcon;",
            at = @At("RETURN"), require = 0)
    private void naturalistXaeroIcons$finishBrownBearDiagnostic(
            net.minecraft.world.entity.Entity entity, EntityType<?> type,
            xaero.hud.minimap.radar.icon.definition.RadarIconDefinition definition,
            net.minecraft.client.renderer.entity.EntityRenderer<?, ?> renderer, float partialTick,
            boolean debug, boolean showVariant,
            xaero.hud.minimap.element.render.MinimapElementGraphics graphics,
            com.mojang.blaze3d.pipeline.RenderTarget target,
            CallbackInfoReturnable<XaeroIcon> callback) {
        BrownBearDiagnostic.requestFinished(callback.getReturnValue());
    }
}
