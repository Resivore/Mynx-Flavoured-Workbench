package dev.resivore.matchajei.client;

import dev.resivore.matchajei.data.MatchaExactCatalog;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
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
            Iterable<ItemStack> catalog
    ) {
        searchContents.removeIf(ownedEntries::contains);
        ownedEntries.clear();

        for (ItemStack stack : catalog) {
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
