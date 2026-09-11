package dev.resivore.inventorysortercsrcompat.mixin;

import dev.resivore.inventorysortercsrcompat.core.MaskedServerSort;
import net.kyrptonaught.inventorysorter.inventory.ContainerInventorySorter;
import net.kyrptonaught.inventorysorter.network.SortPriorityRuleSetting;
import net.kyrptonaught.inventorysorter.sort.SortType;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ContainerInventorySorter.class, remap = false)
public abstract class ContainerInventorySorterMixin {
    @Inject(
            method = "sort(Lnet/minecraft/world/Container;IILnet/kyrptonaught/inventorysorter/sort/SortType;Ljava/lang/String;Ljava/util/List;Z)V",
            at = @At("HEAD"), cancellable = true, require = 1, remap = false
    )
    private static void inventorySorterCsrCompat$maskFixedSlots(
            Container container, int firstSlot, int slotCount, SortType sortType, String languageCode,
            List<SortPriorityRuleSetting> priorityRules, boolean sortIntoBundles, CallbackInfo callback
    ) {
        if (MaskedServerSort.sortIfNeeded(
                container, firstSlot, slotCount, sortType, languageCode, priorityRules, sortIntoBundles)) {
            callback.cancel();
        }
    }
}
