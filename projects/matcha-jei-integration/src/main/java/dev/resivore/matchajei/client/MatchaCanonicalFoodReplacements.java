package dev.resivore.matchajei.client;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.food.FoodProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Derives the ordinary food identities that Matcha's synchronized catalog
 * canonically replaces in discovery UIs.
 *
 * <p>This is deliberately narrower than an item-ID comparison. Frozen Matcha
 * 1.12 food recipes mark their health-bearing stack by overriding both the
 * {@code minecraft:food} and {@code minecraft:consumable} components; the
 * latter carries non-empty consume effects. A replacement is only accepted
 * when it still presents as its normal food base (no model override, or its
 * own default model). Custom-model carrier foods and non-food carriers stay
 * independent identities.</p>
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
        Optional<FoodProperties> food = patched(stack, DataComponents.FOOD);
        Optional<Consumable> consumable = patched(stack, DataComponents.CONSUMABLE);
        if (food.isEmpty() || consumable.isEmpty() || consumable.get().onConsumeEffects().isEmpty()) {
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
