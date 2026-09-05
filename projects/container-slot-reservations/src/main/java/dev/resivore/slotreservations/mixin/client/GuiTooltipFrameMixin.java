package dev.resivore.slotreservations.mixin.client;

import dev.resivore.slotreservations.client.NestedTooltipEditor;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One GUI extraction encloses screen scheduling and deferred tooltip image extraction. */
@Mixin(Gui.class)
abstract class GuiTooltipFrameMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At("HEAD"))
    private void containerSlotReservations$beginFrame(CallbackInfo ci) { NestedTooltipEditor.beginFrame(); }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At("RETURN"))
    private void containerSlotReservations$endFrame(CallbackInfo ci) { NestedTooltipEditor.endFrame(); }
}
