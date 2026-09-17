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
            Iterable<ItemStack> catalog
    ) {
        List<ItemStack> catalogEntries = new java.util.ArrayList<>();
        catalog.forEach(catalogEntries::add);
        restoreSuppressed(searchContents, suppressedDefaultEntries);
        searchContents.removeIf(ownedEntries::contains);
        ownedEntries.clear();

        List<ItemStack> canonicalDefaults = MatchaCanonicalFoodReplacements.defaultsFor(catalogEntries);
        searchContents.removeIf(candidate -> {
            if (!MatchaCanonicalFoodReplacements.isPlainDefaultReplacement(candidate, canonicalDefaults)) {
                return false;
            }
            suppressedDefaultEntries.add(candidate);
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
            Iterable<ItemStack> catalog
    ) {
        List<ItemStack> defaults = MatchaCanonicalFoodReplacements.defaultsFor(catalog);
        if (!defaults.isEmpty()) {
            categorySearchEntries.removeIf(candidate ->
                    MatchaCanonicalFoodReplacements.isPlainDefaultReplacement(candidate, defaults));
        }
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
