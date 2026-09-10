package dev.resivore.quickstacknearbycompat.mixin.client;

import dev.resivore.quickstacknearbycompat.core.QsnInventorySearchButtonPlacement;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tempeststudios.quickstacknearby.QuickStackButtonSlotBridge;
import tempeststudios.quickstacknearby.QuickStackIconButton;
import tempeststudios.quickstacknearby.mixin.AbstractContainerScreenAccessor;

@Mixin(value = QuickStackButtonSlotBridge.class, remap = false)
public abstract class QuickStackButtonSlotBridgeMixin {
    @Inject(
            method = "reservePlayerInventorySlot(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;Ljava/lang/String;Ljava/lang/String;)Ltempeststudios/quickstacknearby/QuickStackButtonSlotBridge$SlotPlacement;",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void quickStackNearbyCompat$placeSurvivalUtilityButton(
            AbstractContainerScreen<?> screen,
            String ownerId,
            String slotId,
            CallbackInfoReturnable<QuickStackButtonSlotBridge.SlotPlacement> callback
    ) {
        QuickStackButtonSlotBridge.SlotPlacement placement = callback.getReturnValue();
        if (placement == null) {
            return;
        }

        if (!(screen instanceof InventoryScreen)
                || !QsnInventorySearchButtonPlacement.isQsnActionReservation(ownerId, slotId)) {
            return;
        }

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        QsnInventorySearchButtonPlacement.Position position =
                QsnInventorySearchButtonPlacement.firstBottomUpFreePosition(
                        accessor.getLeftPos(),
                        accessor.getTopPos(),
                        accessor.getImageWidth(),
                        accessor.getImageHeight(),
                        screen.width,
                        screen.height,
                        Screens.getWidgets(screen).stream()
                                .map(QuickStackButtonSlotBridgeMixin::boundsOf)
                                .toList()
                );
        if (position != null) {
            callback.setReturnValue(new QuickStackButtonSlotBridge.SlotPlacement(position.x(), position.y()));
        }
    }

    private static QsnInventorySearchButtonPlacement.Bounds boundsOf(AbstractWidget widget) {
        return new QsnInventorySearchButtonPlacement.Bounds(
                widget.getX(),
                widget.getY(),
                widget.getWidth(),
                widget.getHeight(),
                widget.visible,
                widget instanceof QuickStackIconButton
        );
    }
}
