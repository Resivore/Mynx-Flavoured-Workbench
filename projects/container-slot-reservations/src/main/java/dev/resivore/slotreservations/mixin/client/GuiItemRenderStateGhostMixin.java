package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.client.GhostGuiItemRenderStateAccess;
import dev.resivore.slotreservations.client.GhostItemRenderScope;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiItemRenderState.class)
abstract class GuiItemRenderStateGhostMixin implements GhostGuiItemRenderStateAccess {
    @Unique
    private int containerSlotReservations$alphaPlusOne;

    @Override
    public int containerSlotReservations$getAlpha() {
        return containerSlotReservations$alphaPlusOne == 0
                ? GhostItemRenderScope.OPAQUE_ALPHA
                : containerSlotReservations$alphaPlusOne - 1;
    }

    @Override
    public void containerSlotReservations$setAlpha(int alpha) {
        containerSlotReservations$alphaPlusOne = alpha + 1;
    }
}
