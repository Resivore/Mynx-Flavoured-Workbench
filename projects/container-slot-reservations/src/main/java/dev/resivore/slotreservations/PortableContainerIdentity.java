package dev.resivore.slotreservations;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.Optional;
import java.util.UUID;

/** Server-owned identity rules for the two portable container families CSR supports specially. */
public final class PortableContainerIdentity {
    private PortableContainerIdentity() {
    }

    public enum Family {
        SHULKER,
        BUNDLE
    }

    public static Optional<Family> familyOf(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        if (SupportedContainerResolver.isSupportedShulkerItem(stack)) return Optional.of(Family.SHULKER);
        return stack.getItem() instanceof BundleItem ? Optional.of(Family.BUNDLE) : Optional.empty();
    }

    public static boolean isEmpty(ItemStack stack) {
        return switch (familyOf(stack).orElse(null)) {
            case SHULKER -> stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                    .nonEmptyItemCopyStream().findAny().isEmpty();
            case BUNDLE -> stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).isEmpty();
            case null -> false;
        };
    }

    public static Optional<UUID> get(ItemStack stack) {
        return Optional.ofNullable(stack.get(ModComponents.PORTABLE_CONTAINER_ID));
    }

    public static UUID assign(ItemStack stack) {
        if (stack.getCount() != 1 || familyOf(stack).isEmpty()) {
            throw new IllegalArgumentException("CSR identities require a count-one portable container");
        }
        UUID existing = stack.get(ModComponents.PORTABLE_CONTAINER_ID);
        if (existing != null) return existing;
        UUID created = UUID.randomUUID();
        stack.set(ModComponents.PORTABLE_CONTAINER_ID, created);
        return created;
    }

    /** Creates a display/template stack that can never materialize CSR's physical identity. */
    public static ItemStack withoutIdentity(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        copy.remove(ModComponents.PORTABLE_CONTAINER_ID);
        return copy;
    }

    public static boolean matches(Family family, UUID identity, ItemStack incoming) {
        return incoming.getCount() == 1
                && familyOf(incoming).filter(family::equals).isPresent()
                && identity.equals(incoming.get(ModComponents.PORTABLE_CONTAINER_ID));
    }

    public static boolean genericEmptyMatches(ItemStack template, ItemStack incoming) {
        return familyOf(template).isPresent() && isEmpty(template)
                && familyOf(incoming).isPresent() && isEmpty(incoming)
                && ItemStack.isSameItemSameComponents(template, withoutIdentity(incoming));
    }
}
