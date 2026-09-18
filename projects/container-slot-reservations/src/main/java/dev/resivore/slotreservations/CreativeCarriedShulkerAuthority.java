package dev.resivore.slotreservations;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * The Creative inventory screen owns a client-only cursor.  This admits that
 * cursor precisely once when vanilla has no server menu cursor, then leaves
 * all subsequent mutations on the normal server-owned cursor path.
 */
final class CreativeCarriedShulkerAuthority {
    private CreativeCarriedShulkerAuthority() {}

    static Optional<ItemStack> resolve(
            boolean infiniteMaterials,
            boolean inventoryMenu,
            int menuSlot,
            ItemStack serverCarried,
            ItemStack creativeCursor,
            String fingerprint,
            RegistryAccess registries
    ) {
        if (isExactSupported(serverCarried, fingerprint, registries)) {
            // Survival, and Creative after its first bridge mutation, retain
            // the ordinary menu cursor as the only authority.
            return Optional.of(serverCarried);
        }
        if (!serverCarried.isEmpty() || !infiniteMaterials || !inventoryMenu || menuSlot != -1) {
            return Optional.empty();
        }
        return isExactSupported(creativeCursor, fingerprint, registries)
                ? Optional.of(creativeCursor.copy()) : Optional.empty();
    }

    static boolean isExactSupported(ItemStack stack, String fingerprint, RegistryAccess registries) {
        return stack != null && !stack.isEmpty() && stack.getCount() == 1
                && fingerprint != null
                && SupportedContainerResolver.isSupportedShulkerItem(stack)
                && ShulkerHostFingerprint.of(stack, registries).equals(fingerprint);
    }
}
