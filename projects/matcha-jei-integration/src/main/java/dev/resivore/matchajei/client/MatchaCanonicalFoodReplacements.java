package dev.resivore.matchajei.client;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Derives the ordinary food identities that Matcha's synchronized catalog
 * canonically replaces in discovery UIs.
 *
 * <p>This is deliberately narrower than an item-ID comparison. The
 * synchronized catalog is already restricted to Matcha-owned effective
 * outputs and acquisitions. Within that catalog, a replacement is an ordinary
 * food base whose {@code minecraft:consumable} patch supplies non-empty
 * consume effects. Some Matcha foods, including loot-derived health foods,
 * deliberately override only {@code minecraft:consumable}; requiring a food
 * patch would leave their vanilla default beside the Matcha stack. A
 * replacement is only accepted when it still presents as its normal food base
 * (no model override, or its own default model). Custom-model carrier foods
 * and non-food carriers stay independent identities.</p>
 */
final class MatchaCanonicalFoodReplacements {
    private MatchaCanonicalFoodReplacements() {
    }

    static List<ItemStack> defaultsFor(Iterable<ItemStack> catalog) {
        List<ItemStack> defaults = new ArrayList<>();
        for (ItemStack stack : catalog) {
            if (isCanonicalHealthFood(stack)) {
                ItemStack defaultStack = stack.getItem().getDefaultInstance().copyWithCount(1);
                boolean present = defaults.stream().anyMatch(existing ->
                        ItemStack.isSameItemSameComponents(existing, defaultStack));
                if (!present) {
                    defaults.add(defaultStack);
                }
            }
        }
        return List.copyOf(defaults);
    }

    static boolean isPlainDefaultReplacement(ItemStack candidate, Iterable<ItemStack> defaults) {
        if (candidate == null || candidate.isEmpty()) {
            return false;
        }
        return java.util.stream.StreamSupport.stream(defaults.spliterator(), false)
                .anyMatch(defaultStack -> ItemStack.isSameItemSameComponents(candidate, defaultStack));
    }

    private static boolean isCanonicalHealthFood(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.getItem().components().has(DataComponents.FOOD)) {
            return false;
        }
        Optional<Consumable> consumable = patched(stack, DataComponents.CONSUMABLE);
        if (consumable.isEmpty() || consumable.get().onConsumeEffects().isEmpty()) {
            return false;
        }
        return patched(stack, DataComponents.ITEM_MODEL)
                .map(model -> model.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())))
                .orElse(true);
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> patched(ItemStack stack, DataComponentType<T> type) {
        DataComponentPatch patch = stack.getComponentsPatch();
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
            if (entry.getKey() == type) {
                return (Optional<T>) entry.getValue();
            }
        }
        return Optional.empty();
    }
}
