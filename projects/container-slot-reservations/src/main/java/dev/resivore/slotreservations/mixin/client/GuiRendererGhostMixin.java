package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.client.GhostGuiItemRenderStateAccess;
import dev.resivore.slotreservations.client.GhostItemRenderPipeline;
import dev.resivore.slotreservations.client.GhostItemRenderScope;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
abstract class GuiRendererGhostMixin {
    @Shadow
    @Final
    private GuiRenderState renderState;

    @Inject(
            method = "submitBlitFromItemAtlas(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;Lnet/minecraft/client/gui/render/GuiItemAtlas$SlotView;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            expect = 1,
            allow = 1
    )
    private void containerSlotReservations$submitAlphaOnlyGhost(
            GuiItemRenderState state,
            GuiItemAtlas.SlotView slotView,
            CallbackInfo callback
    ) {
        int alpha = ((GhostGuiItemRenderStateAccess) (Object) state)
                .containerSlotReservations$getAlpha();
        if (alpha == GhostItemRenderScope.OPAQUE_ALPHA) {
            return;
        }
        renderState.addBlitToCurrentLayer(GhostItemRenderPipeline.blit(
                slotView.textureView(),
                state.pose(),
                state.x(),
                state.y(),
                state.x() + 16,
                state.y() + 16,
                slotView.u0(),
                slotView.u1(),
                slotView.v0(),
                slotView.v1(),
                alpha,
                state.scissorArea()
        ));
        callback.cancel();
    }
}
