package dev.resivore.slotreservations;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Creation policy only: historical templates remain readable and explicitly clearable. */
public final class ReservationTemplateEligibility {
    private ReservationTemplateEligibility() {}

    public static boolean allows(ItemStack template) {
        if (template.isEmpty()) return false;
        if (!SupportedContainerResolver.isSupportedShulkerItem(template)) return true;
        return template.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream().findAny().isEmpty()
                && ReservationStore.getData(template).isEmpty();
    }
}
