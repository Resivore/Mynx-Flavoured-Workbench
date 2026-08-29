package dev.resivore.quickstacknearbycompat.mixin.client;

import dev.resivore.quickstacknearbycompat.core.InventorySearchContainerClassification;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "tempeststudios.inventorysort.api.InventoryScreenButtonSlots",
        remap = false
)
public abstract class InventoryScreenButtonSlotsMixin {
    @Inject(
            method = "isInventoryModsContainer(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;)Z",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void quickStackNearbyCompat$allowExtendedPlayerInventorySearch(
            AbstractContainerScreen<?> screen,
            CallbackInfoReturnable<Boolean> callback
    ) {
        boolean upstreamIsContainer = callback.getReturnValueZ();
        boolean effectiveIsContainer = InventorySearchContainerClassification.effectiveIsContainer(
                upstreamIsContainer,
                FabricLoader.getInstance().isModLoaded(
                        InventorySearchContainerClassification.INVENTORY_SEARCH_MOD_ID
                ),
                FabricLoader.getInstance().isModLoaded(
                        InventorySearchContainerClassification.INVENTORY_EXTENDED_MOD_ID
                ),
                screen instanceof InventoryScreen
        );
        if (effectiveIsContainer != upstreamIsContainer) {
            callback.setReturnValue(effectiveIsContainer);
        }
    }
}
