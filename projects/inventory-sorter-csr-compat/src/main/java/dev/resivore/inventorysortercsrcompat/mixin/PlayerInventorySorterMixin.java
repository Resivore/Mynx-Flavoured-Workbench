package dev.resivore.inventorysortercsrcompat.mixin;

import dev.resivore.inventorysortercsrcompat.core.FixedSortSlots;
import net.kyrptonaught.inventorysorter.inventory.ContainerInventorySorter;
import net.kyrptonaught.inventorysorter.inventory.PlayerInventorySorter;
import net.kyrptonaught.inventorysorter.network.SortSettings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces only Inventory Sorter's player bundle-content insertion route with ordinary sorting. */
@Mixin(value = PlayerInventorySorter.class, remap = false)
public abstract class PlayerInventorySorterMixin {
    @Inject(
            method = "sort(Lnet/minecraft/server/level/ServerPlayer;Lnet/kyrptonaught/inventorysorter/network/SortSettings;Ljava/lang/String;)V",
            at = @At("HEAD"), cancellable = true, require = 1, remap = false
    )
    private static void inventorySorterCsrCompat$disableSortIntoBundles(
            ServerPlayer player, SortSettings settings, String languageCode, CallbackInfo callback
    ) {
        Inventory inventory = player.getInventory();
        for (int slot = 9; slot < 36; slot++) {
            if (FixedSortSlots.isBundle(inventory.getItem(slot))) {
                // The upstream bundle-insertion branch necessarily opens a bundle target.  Its
                // content mutation is unwanted, so keep the same range on its ordinary path.
                // The ContainerInventorySorter mixin independently masks only CSR reservations.
                ContainerInventorySorter.sort(
                        inventory, 9, 27, settings.sortType(), languageCode,
                        settings.sortPriorityRules(), false);
                callback.cancel();
                return;
            }
        }
    }
}
