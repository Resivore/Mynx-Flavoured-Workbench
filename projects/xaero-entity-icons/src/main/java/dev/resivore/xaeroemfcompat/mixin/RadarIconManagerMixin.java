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
import xaero.hud.minimap.radar.icon.definition.RadarIconDefinition;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.common.icon.XaeroIcon;
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

    /**
     * This runs before RadarIconManager reads its per-variant cache.  It is
     * deliberately structural: only a currently selected LivingEntity model
     * with an EMF root can retry one old FAILED sentinel.  This is what lets
     * EMFModelPartVanilla roots (allay/vex) reach the adapter after C4/C5.
     */
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
}
