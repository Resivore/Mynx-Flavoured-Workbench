package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.client.GhostGuiItemRenderStateAccess;
import dev.resivore.slotreservations.client.GhostItemRenderScope;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiGraphicsExtractor.class)
abstract class GuiGraphicsExtractorGhostMixin {
    @ModifyArg(
            method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addItem(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;)V"
            ),
            index = 0,
            require = 1,
            expect = 1,
            allow = 1
    )
    private GuiItemRenderState containerSlotReservations$markGhost(GuiItemRenderState state) {
        int alpha = GhostItemRenderScope.activeAlpha();
        if (alpha != GhostItemRenderScope.OPAQUE_ALPHA) {
            ((GhostGuiItemRenderStateAccess) (Object) state)
                    .containerSlotReservations$setAlpha(alpha);
        }
        return state;
    }
}
