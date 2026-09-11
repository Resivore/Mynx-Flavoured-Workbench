package dev.resivore.notebook.mixin.client;

import dev.resivore.notebook.NotebookClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Moves the existing Notebook widget when recipe-book visibility changes the inventory bounds. */
@Mixin(InventoryScreen.class)
abstract class InventoryScreenButtonPositionMixin {
    @Inject(method = "extractBackground", at = @At("HEAD"), require = 1)
    private void notebook$refreshButtonForLiveBounds(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        NotebookClient.refreshSurvivalInventoryButton((InventoryScreen) (Object) this);
    }
}
