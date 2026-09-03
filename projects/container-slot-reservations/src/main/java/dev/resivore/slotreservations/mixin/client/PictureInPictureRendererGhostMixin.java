package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.client.GhostGuiItemRenderStateAccess;
import dev.resivore.slotreservations.client.GhostItemRenderPipeline;
import dev.resivore.slotreservations.client.GhostItemRenderScope;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.OversizedItemRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the same per-item alpha when vanilla routes an oversized GUI item through PIP. */
@Mixin(PictureInPictureRenderer.class)
abstract class PictureInPictureRendererGhostMixin {
    @Shadow
    private GpuTextureView textureView;

    @Inject(
            method = "blitTexture(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            expect = 1,
            allow = 1
    )
    private void containerSlotReservations$submitOversizedAlphaOnlyGhost(
            PictureInPictureRenderState state,
            GuiRenderState guiRenderState,
            CallbackInfo callback
    ) {
        if (!(state instanceof OversizedItemRenderState oversized)) {
            return;
        }
        int alpha = ((GhostGuiItemRenderStateAccess) (Object) oversized.guiItemRenderState())
                .containerSlotReservations$getAlpha();
        if (alpha == GhostItemRenderScope.OPAQUE_ALPHA) {
            return;
        }
        guiRenderState.addBlitToCurrentLayer(GhostItemRenderPipeline.blit(
                textureView,
                state.pose(),
                state.x0(),
                state.y0(),
                state.x1(),
                state.y1(),
                0.0F,
                1.0F,
                1.0F,
                0.0F,
                alpha,
                state.scissorArea()
        ));
        callback.cancel();
    }
}
