package dev.resivore.matchajei.data;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Small common rules for the synchronized Matcha item catalog.
 *
 * <p>The server supplies this catalog and both client integrations consume the
 * same normalized stacks. Counts are presentation-only, while every data
 * component remains part of an item's identity.</p>
 */
public final class MatchaExactCatalog {
    private MatchaExactCatalog() {
    }

    /**
     * Keeps a resolved output authoritative whenever its recipe ID is known to
     * the live recipe manager. A known but non-representable recipe deliberately
     * contributes no raw fallback: that avoids reviving a stale datapack result.
     */
    public static <K> Map<K, ItemStack> selectRecipeOutputs(
            Map<K, ItemStack> rawFallbacks,
            Map<K, Optional<ItemStack>> resolvedOutputs
    ) {
        LinkedHashMap<K, ItemStack> selected = new LinkedHashMap<>();
        for (Map.Entry<K, ItemStack> entry : rawFallbacks.entrySet()) {
            Optional<ItemStack> resolved = resolvedOutputs.get(entry.getKey());
            if (resolved != null) {
                resolved.filter(stack -> !stack.isEmpty())
                        .map(MatchaExactCatalog::normalize)
                        .ifPresent(stack -> selected.put(entry.getKey(), stack));
            } else if (!entry.getValue().isEmpty()) {
                selected.put(entry.getKey(), normalize(entry.getValue()));
            }
        }
        return Map.copyOf(selected);
    }

    /**
     * Normalizes stack counts and retains exactly one copy of every full
     * item-and-components identity in stable discovery order.
     */
    public static List<ItemStack> normalizeAndDeduplicate(Iterable<ItemStack> candidates) {
        List<ItemStack> catalog = new ArrayList<>();
        for (ItemStack candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            ItemStack normalized = normalize(candidate);
            boolean present = catalog.stream().anyMatch(existing ->
                    ItemStack.isSameItemSameComponents(existing, normalized));
            if (!present) {
                catalog.add(normalized);
            }
        }
        return List.copyOf(catalog);
    }

    public static ItemStack normalize(ItemStack stack) {
        return stack.copyWithCount(1);
    }
}
