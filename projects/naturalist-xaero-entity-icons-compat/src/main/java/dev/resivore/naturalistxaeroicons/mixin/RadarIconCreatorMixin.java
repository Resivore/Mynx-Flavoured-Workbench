package dev.resivore.naturalistxaeroicons.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.resivore.naturalistxaeroicons.StarfishCaptureDiagnostic;
import dev.resivore.naturalistxaeroicons.ScorpionCaptureDiagnostic;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.icon.XaeroIcon;
import xaero.hud.minimap.element.render.MinimapElementGraphics;
import xaero.hud.minimap.radar.icon.creator.RadarIconCreator;

/** C21 observes the exact form selected after a Starfish cache miss, without altering it. */
@Mixin(value = RadarIconCreator.class, remap = false)
abstract class RadarIconCreatorMixin {
    @Inject(method = "create", at = @At("HEAD"), require = 1)
    private void naturalistXaeroIcons$observeStarfishCreatorStart(
            MinimapElementGraphics graphics, EntityRenderer<?, ?> renderer, EntityRenderState state,
            Entity entity, RenderTarget target, RadarIconCreator.Parameters parameters,
            CallbackInfoReturnable<XaeroIcon> callback) {
        Identifier texture = null;
        if (renderer instanceof LivingEntityRenderer<?, ?, ?> living && state instanceof LivingEntityRenderState livingState) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            LivingEntityRenderer rawLivingRenderer = (LivingEntityRenderer) living;
            texture = (Identifier) rawLivingRenderer.getTextureLocation(livingState);
        }
        StarfishCaptureDiagnostic.creatorStarted(entity, parameters.form, texture);
        ScorpionCaptureDiagnostic.creatorStarted(entity, parameters.form, texture);
    }

    @Inject(method = "create", at = @At("RETURN"), require = 1)
    private void naturalistXaeroIcons$observeStarfishCreatorResult(
            MinimapElementGraphics graphics, EntityRenderer<?, ?> renderer, EntityRenderState state,
            Entity entity, RenderTarget target, RadarIconCreator.Parameters parameters,
            CallbackInfoReturnable<XaeroIcon> callback) {
        StarfishCaptureDiagnostic.creatorFinished(entity, callback.getReturnValue());
        ScorpionCaptureDiagnostic.creatorFinished(entity, callback.getReturnValue());
    }
}
