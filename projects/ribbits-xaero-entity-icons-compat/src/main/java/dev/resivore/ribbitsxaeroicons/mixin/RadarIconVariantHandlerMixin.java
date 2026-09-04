package dev.resivore.ribbitsxaeroicons.mixin;

import dev.resivore.ribbitsxaeroicons.GeoIconProviders;
import dev.resivore.ribbitsxaeroicons.RibbitCacheVariant;
import dev.resivore.ribbitsxaeroicons.RibbitGeoIconProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.radar.icon.cache.id.variant.RadarIconVariantHandler;
import xaero.hud.minimap.radar.icon.definition.RadarIconDefinition;

/** Adds bounded Geo visual identity while retaining Xaero's upstream variant string. */
@Mixin(value = RadarIconVariantHandler.class, remap = false)
abstract class RadarIconVariantHandlerMixin {
    @Inject(
            method = "getEntityVariant(Lxaero/hud/minimap/radar/icon/definition/"
                    + "RadarIconDefinition;Lnet/minecraft/world/entity/Entity;"
                    + "Lnet/minecraft/client/renderer/entity/EntityRenderer;"
                    + "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)"
                    + "Ljava/lang/Object;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1)
    private void ribbitsXaeroIcons$extendRibbitVariant(
            RadarIconDefinition definition,
            Entity entity,
            EntityRenderer<?, ?> renderer,
            EntityRenderState renderState,
            CallbackInfoReturnable<Object> callback) {
        Object upstream = callback.getReturnValue();
        if (upstream == null) {
            return;
        }
        if (RibbitGeoIconProvider.owns(entity)) {
            callback.setReturnValue(new RibbitCacheVariant(
                    upstream, GeoIconProviders.cacheIdentityForOwned(
                            entity, renderer, renderState)));
        }
    }
}
