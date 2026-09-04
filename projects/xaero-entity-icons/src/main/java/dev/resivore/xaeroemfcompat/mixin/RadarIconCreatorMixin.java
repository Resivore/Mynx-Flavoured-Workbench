package dev.resivore.xaeroemfcompat.mixin;
import dev.resivore.xaeroemfcompat.IconDiagnostics;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.icon.XaeroIcon;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.radar.icon.RadarIconManager;
import xaero.hud.minimap.radar.icon.creator.RadarIconCreator;
@Mixin(value=RadarIconCreator.class, remap=false)
abstract class RadarIconCreatorMixin {
    @Inject(method="create",at=@At("RETURN"),require=1)
    private void xaeroEmf$finalResult(MinimapElementGraphics graphics,EntityRenderer<?,?> renderer,
            EntityRenderState state,Entity entity,RenderTarget target,RadarIconCreator.Parameters parameters,
            CallbackInfoReturnable<XaeroIcon> callback) {
        if(!EntityType.getKey(entity.getType()).getNamespace().equals("minecraft"))return;
        IconDiagnostics.context(EntityType.getKey(entity.getType())+" renderer="+renderer.getClass().getName());
        try {
            var icon=callback.getReturnValue();
            IconDiagnostics.event(icon==null || icon==RadarIconManager.FAILED ? "DOWNSTREAM_DISCARDED_OR_FAILED":"DOWNSTREAM_ACCEPTED",String.valueOf(parameters.variant));
        } finally {IconDiagnostics.clearContext();}
    }
}
