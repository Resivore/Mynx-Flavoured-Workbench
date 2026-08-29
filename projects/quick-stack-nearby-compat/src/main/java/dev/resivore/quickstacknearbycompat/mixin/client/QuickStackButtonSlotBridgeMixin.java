package dev.resivore.quickstacknearbycompat.mixin.client;

import dev.resivore.quickstacknearbycompat.core.QsnInventorySearchButtonPlacement;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tempeststudios.quickstacknearby.QuickStackButtonSlotBridge;
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
    private static void quickStackNearbyCompat$separateInventorySearchButton(
            AbstractContainerScreen<?> screen,
            String ownerId,
            String slotId,
            CallbackInfoReturnable<QuickStackButtonSlotBridge.SlotPlacement> callback
    ) {
        QuickStackButtonSlotBridge.SlotPlacement placement = callback.getReturnValue();
        if (placement == null) {
            return;
        }

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int baseY = QsnInventorySearchButtonPlacement.playerInventoryBaseY(
                accessor.getTopPos(),
                accessor.getImageHeight()
        );
        int adjustedY = QsnInventorySearchButtonPlacement.adjustedY(
                placement.y(),
                baseY,
                FabricLoader.getInstance().isModLoaded(QsnInventorySearchButtonPlacement.INVENTORY_SEARCH_MOD_ID),
                ownerId,
                slotId
        );
        if (adjustedY != placement.y()) {
            callback.setReturnValue(new QuickStackButtonSlotBridge.SlotPlacement(placement.x(), adjustedY));
        }
    }
}
