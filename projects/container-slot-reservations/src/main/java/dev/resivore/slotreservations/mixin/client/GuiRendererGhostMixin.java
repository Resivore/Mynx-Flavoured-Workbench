package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.client.GhostGuiItemRenderStateAccess;
import dev.resivore.slotreservations.client.GhostItemRenderScope;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(GuiRenderer.class)
abstract class GuiRendererGhostMixin {
    @ModifyConstant(
            method = "submitBlitFromItemAtlas(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;Lnet/minecraft/client/gui/render/GuiItemAtlas$SlotView;)V",
            constant = @Constant(intValue = -1),
            require = 1,
            expect = 1,
            allow = 1
    )
    private int containerSlotReservations$applyGhostAlpha(
            int originalColor,
            GuiItemRenderState state,
            GuiItemAtlas.SlotView slotView
    ) {
        int alpha = ((GhostGuiItemRenderStateAccess) (Object) state)
                .containerSlotReservations$getAlpha();
        return alpha == GhostItemRenderScope.OPAQUE_ALPHA
                ? originalColor
                : GhostItemRenderScope.premultipliedWhite(alpha);
    }
}
