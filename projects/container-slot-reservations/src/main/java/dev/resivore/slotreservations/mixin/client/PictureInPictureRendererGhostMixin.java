package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.client.GhostGuiItemRenderStateAccess;
import dev.resivore.slotreservations.client.GhostItemRenderScope;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.OversizedItemRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Applies the same per-item alpha when vanilla routes an oversized GUI item through PIP. */
@Mixin(PictureInPictureRenderer.class)
abstract class PictureInPictureRendererGhostMixin {
    @ModifyConstant(
            method = "blitTexture(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;)V",
            constant = @Constant(intValue = -1),
            require = 1,
            expect = 1,
            allow = 1
    )
    private int containerSlotReservations$applyOversizedGhostAlpha(
            int originalColor,
            PictureInPictureRenderState state,
            GuiRenderState guiRenderState
    ) {
        if (!(state instanceof OversizedItemRenderState oversized)) {
            return originalColor;
        }
        int alpha = ((GhostGuiItemRenderStateAccess) (Object) oversized.guiItemRenderState())
                .containerSlotReservations$getAlpha();
        return alpha == GhostItemRenderScope.OPAQUE_ALPHA
                ? originalColor
                : GhostItemRenderScope.premultipliedWhite(alpha);
    }
}
