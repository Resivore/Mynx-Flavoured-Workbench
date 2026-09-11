package dev.resivore.slotreservations;

import net.minecraft.world.item.ItemStack;

/** Creation policy only: historical templates remain readable and explicitly clearable. */
public final class ReservationTemplateEligibility {
    private ReservationTemplateEligibility() {}

    public static boolean allows(ItemStack template) {
        if (template.isEmpty()) return false;
        // Specific portable containers are represented by a reservation identity, never a copied
        // component template.  Empty portable containers intentionally remain generic, including
        // when a dormant CSR identity is present on the actual item.
        if (PortableContainerIdentity.familyOf(template).isEmpty()) return true;
        if (!PortableContainerIdentity.isEmpty(template)) return false;
        return !SupportedContainerResolver.isSupportedShulkerItem(template)
                || ReservationStore.getData(template).isEmpty();
    }
}
