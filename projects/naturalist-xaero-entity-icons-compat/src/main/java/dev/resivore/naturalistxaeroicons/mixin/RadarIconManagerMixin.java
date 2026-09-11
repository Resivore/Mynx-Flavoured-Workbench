package dev.resivore.naturalistxaeroicons.mixin;

import dev.resivore.naturalistxaeroicons.BrownBearSpritePresentation;
import dev.resivore.naturalistxaeroicons.StarfishCaptureDiagnostic;
import dev.resivore.naturalistxaeroicons.ScorpionCaptureDiagnostic;
import dev.resivore.naturalistxaeroicons.NaturalistModelContracts;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.icon.XaeroIcon;
import xaero.hud.minimap.radar.icon.RadarIconManager;
import xaero.hud.minimap.radar.icon.cache.RadarIconCache;
import xaero.hud.minimap.radar.icon.cache.RadarIconEntityCache;
import xaero.hud.minimap.radar.icon.cache.id.RadarIconKey;
import xaero.hud.minimap.radar.icon.cache.id.armor.RadarIconArmor;
import xaero.hud.minimap.radar.icon.creator.RadarIconCreator;

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
                    && (NaturalistModelContracts.isTargetId(EntityType.getKey(type).getPath())
                    || "bear".equals(EntityType.getKey(type).getPath())));
        } catch (RuntimeException ignored) { /* reload remains Xaero-owned if the private seam changed */ }
        StarfishCaptureDiagnostic.resourceReloaded();
        ScorpionCaptureDiagnostic.resourceReloaded();
    }

    @Inject(
            method = "get(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityType;Lxaero/hud/minimap/radar/icon/definition/RadarIconDefinition;Lnet/minecraft/client/renderer/entity/EntityRenderer;FZZLxaero/hud/minimap/element/render/MinimapElementGraphics;Lcom/mojang/blaze3d/pipeline/RenderTarget;)Lxaero/common/icon/XaeroIcon;",
            at = @At("HEAD"), require = 1)
    private void naturalistXaeroIcons$startBrownBearSpritePresentation(
            net.minecraft.world.entity.Entity entity, EntityType<?> type,
            xaero.hud.minimap.radar.icon.definition.RadarIconDefinition definition,
            net.minecraft.client.renderer.entity.EntityRenderer<?, ?> renderer, float partialTick,
            boolean debug, boolean showVariant,
            xaero.hud.minimap.element.render.MinimapElementGraphics graphics,
            com.mojang.blaze3d.pipeline.RenderTarget target,
            CallbackInfoReturnable<XaeroIcon> callback) {
        BrownBearSpritePresentation.requestStarted(entity, canPrerender);
        StarfishCaptureDiagnostic.requestStarted(entity, canPrerender);
        ScorpionCaptureDiagnostic.requestStarted(entity, canPrerender);
    }

    /** C21 records Xaero's Starfish cache decision without replacing its lookup or retry behavior. */
    @Inject(
            method = "get(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityType;Lxaero/hud/minimap/radar/icon/definition/RadarIconDefinition;Lnet/minecraft/client/renderer/entity/EntityRenderer;FZZLxaero/hud/minimap/element/render/MinimapElementGraphics;Lcom/mojang/blaze3d/pipeline/RenderTarget;)Lxaero/common/icon/XaeroIcon;",
            at = @At(value = "INVOKE", target = "Lxaero/hud/minimap/radar/icon/cache/RadarIconEntityCache;get(Lxaero/hud/minimap/radar/icon/cache/id/RadarIconKey;)Lxaero/common/icon/XaeroIcon;", shift = At.Shift.BEFORE),
            locals = LocalCapture.CAPTURE_FAILHARD,
            require = 1)
    private void naturalistXaeroIcons$observeStarfishCacheBeforeXaeroEmfRetry(
            net.minecraft.world.entity.Entity entity, EntityType<?> type,
            xaero.hud.minimap.radar.icon.definition.RadarIconDefinition definition,
            net.minecraft.client.renderer.entity.EntityRenderer<?, ?> renderer, float partialTick,
            boolean debug, boolean showVariant,
            xaero.hud.minimap.element.render.MinimapElementGraphics graphics,
            com.mojang.blaze3d.pipeline.RenderTarget target,
            CallbackInfoReturnable<XaeroIcon> callback,
            net.minecraft.client.renderer.entity.state.EntityRenderState state, Object variant,
            RadarIconArmor armor, RadarIconEntityCache cache, RadarIconKey key) {
        if (StarfishCaptureDiagnostic.isStarfish(entity)) {
            StarfishCaptureDiagnostic.cacheLookup(((RadarIconEntityCacheStorageAccessor) (Object) cache)
                    .naturalistXaeroIcons$getStorage().containsKey(key));
        }
        if (ScorpionCaptureDiagnostic.isScorpion(entity)) {
            ScorpionCaptureDiagnostic.cacheLookup(((RadarIconEntityCacheStorageAccessor) (Object) cache)
                    .naturalistXaeroIcons$getStorage().containsKey(key));
        }
    }

    /**
     * C12 runtime evidence proves Brown Bear bypasses Xaero's model path and is rasterized by
     * RadarIconSpriteFormPrerenderer from its renderer texture. Its immutable Parameters object
     * is recreated only for that in-flight bear request, preserving Xaero's chosen form, variant,
     * config, and debug setting while supplying the sprite-specific scale.
     */
    @ModifyVariable(
            method = "get(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityType;Lxaero/hud/minimap/radar/icon/definition/RadarIconDefinition;Lnet/minecraft/client/renderer/entity/EntityRenderer;FZZLxaero/hud/minimap/element/render/MinimapElementGraphics;Lcom/mojang/blaze3d/pipeline/RenderTarget;)Lxaero/common/icon/XaeroIcon;",
            at = @At("STORE"),
            index = 19,
            require = 1)
    private RadarIconCreator.Parameters naturalistXaeroIcons$scaleBrownBearSprite(
            RadarIconCreator.Parameters parameters) {
        return BrownBearSpritePresentation.scaleForCurrentRequest(parameters);
    }

    @Inject(
            method = "get(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityType;Lxaero/hud/minimap/radar/icon/definition/RadarIconDefinition;Lnet/minecraft/client/renderer/entity/EntityRenderer;FZZLxaero/hud/minimap/element/render/MinimapElementGraphics;Lcom/mojang/blaze3d/pipeline/RenderTarget;)Lxaero/common/icon/XaeroIcon;",
            at = @At("RETURN"), require = 1)
    private void naturalistXaeroIcons$finishBrownBearSpritePresentation(
            net.minecraft.world.entity.Entity entity, EntityType<?> type,
            xaero.hud.minimap.radar.icon.definition.RadarIconDefinition definition,
            net.minecraft.client.renderer.entity.EntityRenderer<?, ?> renderer, float partialTick,
            boolean debug, boolean showVariant,
            xaero.hud.minimap.element.render.MinimapElementGraphics graphics,
            com.mojang.blaze3d.pipeline.RenderTarget target,
            CallbackInfoReturnable<XaeroIcon> callback) {
        BrownBearSpritePresentation.requestFinished();
        StarfishCaptureDiagnostic.requestFinished(callback.getReturnValue());
        ScorpionCaptureDiagnostic.requestFinished(entity, callback.getReturnValue());
    }
}
