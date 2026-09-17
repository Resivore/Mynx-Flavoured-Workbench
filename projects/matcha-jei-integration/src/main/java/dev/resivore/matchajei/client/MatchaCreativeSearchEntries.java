package dev.resivore.matchajei.client;

import dev.resivore.matchajei.data.MatchaExactCatalog;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Reconciles only the exact Search entries inserted by this integration.
 *
 * <p>Ownership is object-identity based. If another provider already supplied
 * an equivalent exact stack, Matcha does not claim or later remove it.</p>
 */
final class MatchaCreativeSearchEntries {
    private MatchaCreativeSearchEntries() {
    }

    static void replaceOwned(
            Collection<ItemStack> searchContents,
            Set<ItemStack> ownedEntries,
            Set<ItemStack> suppressedDefaultEntries,
            Iterable<ItemStack> catalog,
            Iterable<ItemStack> canonicalDefaults
    ) {
        List<ItemStack> catalogEntries = new java.util.ArrayList<>();
        catalog.forEach(catalogEntries::add);
        restoreSuppressed(searchContents, suppressedDefaultEntries);
        searchContents.removeIf(ownedEntries::contains);
        ownedEntries.clear();

        searchContents.removeIf(candidate -> {
            if (!isCanonicalDefault(candidate, canonicalDefaults)) {
                return false;
            }
            rememberSuppressed(candidate, suppressedDefaultEntries);
            return true;
        });

        for (ItemStack stack : catalogEntries) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemStack contribution = MatchaExactCatalog.normalize(stack);
            boolean alreadyPresent = searchContents.stream().anyMatch(existing ->
                    ItemStack.isSameItemSameComponents(existing, contribution));
            if (!alreadyPresent && searchContents.add(contribution)) {
                ownedEntries.add(contribution);
            }
        }
    }

    /**
     * Applies the same canonical-default rule to a category's source entries
     * before vanilla builds the global Search tab from them.
     */
    static void suppressCanonicalDefaults(
            Collection<ItemStack> categorySearchEntries,
            Iterable<ItemStack> canonicalDefaults,
            Set<ItemStack> suppressedDefaultEntries
    ) {
        categorySearchEntries.removeIf(candidate -> {
            if (!isCanonicalDefault(candidate, canonicalDefaults)) {
                return false;
            }
            rememberSuppressed(candidate, suppressedDefaultEntries);
            return true;
        });
    }

    private static void restoreSuppressed(
            Collection<ItemStack> searchContents,
            Set<ItemStack> suppressedDefaultEntries
    ) {
        for (ItemStack suppressed : suppressedDefaultEntries) {
            boolean present = searchContents.stream().anyMatch(existing ->
                    ItemStack.isSameItemSameComponents(existing, suppressed));
            if (!present) {
                searchContents.add(suppressed);
            }
        }
        suppressedDefaultEntries.clear();
    }

    private static void rememberSuppressed(
            ItemStack candidate,
            Set<ItemStack> suppressedDefaultEntries
    ) {
        boolean alreadyRemembered = suppressedDefaultEntries.stream().anyMatch(existing ->
                ItemStack.isSameItemSameComponents(existing, candidate));
        if (!alreadyRemembered) {
            suppressedDefaultEntries.add(candidate);
        }
    }

    private static boolean isCanonicalDefault(ItemStack candidate, Iterable<ItemStack> canonicalDefaults) {
        if (candidate == null || candidate.isEmpty()) {
            return false;
        }
        return java.util.stream.StreamSupport.stream(canonicalDefaults.spliterator(), false)
                .anyMatch(defaultStack -> ItemStack.isSameItemSameComponents(candidate, defaultStack));
    }

    static void rememberOwned(ItemStack contribution, Set<ItemStack> ownedEntries) {
        ownedEntries.add(contribution);
    }

    static void retainActiveOwnership(
            Collection<ItemStack> activeSearchContents,
            Set<ItemStack> ownedEntries
    ) {
        ownedEntries.removeIf(owned -> activeSearchContents.stream().noneMatch(active -> active == owned));
    }
}
