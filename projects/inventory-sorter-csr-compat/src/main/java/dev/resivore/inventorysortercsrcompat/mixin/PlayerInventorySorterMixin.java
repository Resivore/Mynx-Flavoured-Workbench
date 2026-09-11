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

/** Protects the separate upstream player-to-bundle route when a portable item is in the range. */
@Mixin(value = PlayerInventorySorter.class, remap = false)
public abstract class PlayerInventorySorterMixin {
    @Inject(
            method = "sort(Lnet/minecraft/server/level/ServerPlayer;Lnet/kyrptonaught/inventorysorter/network/SortSettings;Ljava/lang/String;)V",
            at = @At("HEAD"), cancellable = true, require = 1, remap = false
    )
    private static void inventorySorterCsrCompat$keepPortablePlayerSlotsFixed(
            ServerPlayer player, SortSettings settings, String languageCode, CallbackInfo callback
    ) {
        Inventory inventory = player.getInventory();
        for (int slot = 9; slot < 36; slot++) {
            if (FixedSortSlots.isPortableContainer(inventory.getItem(slot))) {
                // The upstream bundle-insertion branch necessarily opens a bundle target.  Its
                // fixed-slot semantics are incompatible, so use its ordinary sorter path only
                // for this already-masked player range.
                ContainerInventorySorter.sort(
                        inventory, 9, 27, settings.sortType(), languageCode,
                        settings.sortPriorityRules(), false);
                callback.cancel();
                return;
            }
        }
    }
}
