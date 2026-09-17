package dev.resivore.matchajei.data;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Builds the one canonical Matcha food view consumed by discovery clients.
 *
 * <p>Exact stack identity remains the default. Recipe authority groups only
 * health-bearing candidates whose base item and every effective component
 * other than food, consumable, and health lore agree. A unique effective
 * recipe output is authoritative inside such a family; otherwise every exact
 * identity is retained. Default-presentation matching additionally recognizes
 * Matcha's audited stack-size and use-remainder enrichment. Custom models,
 * names, potions, and unrelated component variants remain independent.</p>
 */
public final class MatchaFoodDiscoveryCatalog {
    private static final Set<DataComponentType<?>> SOURCE_VARIANT_COMPONENTS = Set.of(
            DataComponents.FOOD,
            DataComponents.CONSUMABLE,
            DataComponents.LORE
    );
    private static final Set<DataComponentType<?>> DEFAULT_ENRICHMENT_COMPONENTS = Set.of(
            DataComponents.FOOD,
            DataComponents.CONSUMABLE,
            DataComponents.LORE,
            DataComponents.MAX_STACK_SIZE,
            DataComponents.USE_REMAINDER
    );

    private MatchaFoodDiscoveryCatalog() {
    }

    public enum Source {
        EFFECTIVE_RECIPE,
        NON_RECIPE
    }

    public record Candidate(ItemStack stack, Source source) {
        public Candidate {
            stack = MatchaExactCatalog.normalize(stack);
        }
    }

    public record Result(List<ItemStack> catalog, List<ItemStack> canonicalDefaults) {
        public Result {
            catalog = List.copyOf(catalog);
            canonicalDefaults = List.copyOf(canonicalDefaults);
        }
    }

    /**
     * Canonicalizes the exact catalog while provenance is still available and
     * derives the literal vanilla/default identities superseded by its foods.
     */
    public static Result canonicalize(Iterable<Candidate> candidates) {
        List<Candidate> normalized = normalizeAndMergeSources(candidates);
        Map<FoodFamily, List<Candidate>> recipeFoods = new LinkedHashMap<>();
        for (Candidate candidate : normalized) {
            if (candidate.source() == Source.EFFECTIVE_RECIPE && isHealthBearingFood(candidate.stack())) {
                recipeFoods.computeIfAbsent(sourceFamilyOf(candidate.stack()), ignored -> new ArrayList<>())
                        .add(candidate);
            }
        }

        Map<FoodFamily, ItemStack> authoritativeRecipes = new LinkedHashMap<>();
        recipeFoods.forEach((family, members) -> {
            if (members.size() == 1) {
                authoritativeRecipes.put(family, members.getFirst().stack());
            }
        });

        List<Candidate> retained = new ArrayList<>();
        for (Candidate candidate : normalized) {
            ItemStack authoritative = authoritativeRecipes.get(sourceFamilyOf(candidate.stack()));
            if (candidate.source() == Source.NON_RECIPE && authoritative != null
                    && isHealthBearingFood(candidate.stack())
                    && !ItemStack.isSameItemSameComponents(authoritative, candidate.stack())) {
                continue;
            }
            retained.add(candidate);
        }

        List<ItemStack> catalog = retained.stream().map(Candidate::stack).toList();
        List<ItemStack> defaults = new ArrayList<>();
        for (Candidate candidate : retained) {
            ItemStack stack = candidate.stack();
            if (!isHealthBearingFood(stack)) {
                continue;
            }
            boolean intrinsicFood = stack.getItem().components().has(DataComponents.FOOD);
            boolean authoritativeFoodRecipe = candidate.source() == Source.EFFECTIVE_RECIPE
                    && hasPatchedValue(stack, DataComponents.FOOD);
            if (!intrinsicFood && !authoritativeFoodRecipe) {
                continue;
            }
            ItemStack defaultStack = stack.getItem().getDefaultInstance().copyWithCount(1);
            if (!defaultFamilyOf(stack).equals(defaultFamilyOf(defaultStack))) {
                continue;
            }
            if (defaults.stream().noneMatch(existing ->
                    ItemStack.isSameItemSameComponents(existing, defaultStack))) {
                defaults.add(defaultStack);
            }
        }
        return new Result(catalog, defaults);
    }

    private static List<Candidate> normalizeAndMergeSources(Iterable<Candidate> candidates) {
        List<Candidate> normalized = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (candidate == null || candidate.stack() == null || candidate.stack().isEmpty()) {
                continue;
            }
            ItemStack stack = MatchaExactCatalog.normalize(candidate.stack());
            int existingIndex = -1;
            for (int index = 0; index < normalized.size(); index++) {
                if (ItemStack.isSameItemSameComponents(normalized.get(index).stack(), stack)) {
                    existingIndex = index;
                    break;
                }
            }
            if (existingIndex < 0) {
                normalized.add(new Candidate(stack, candidate.source()));
            } else if (candidate.source() == Source.EFFECTIVE_RECIPE
                    && normalized.get(existingIndex).source() != Source.EFFECTIVE_RECIPE) {
                normalized.set(existingIndex, new Candidate(stack, Source.EFFECTIVE_RECIPE));
            }
        }
        return List.copyOf(normalized);
    }

    private static boolean isHealthBearingFood(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.getComponents().has(DataComponents.FOOD)) {
            return false;
        }
        return patchedValue(stack, DataComponents.CONSUMABLE)
                .map(consumable -> !consumable.onConsumeEffects().isEmpty())
                .orElse(false);
    }

    private static FoodFamily sourceFamilyOf(ItemStack stack) {
        return familyOf(stack, SOURCE_VARIANT_COMPONENTS);
    }

    private static FoodFamily defaultFamilyOf(ItemStack stack) {
        return familyOf(stack, DEFAULT_ENRICHMENT_COMPONENTS);
    }

    private static FoodFamily familyOf(ItemStack stack, Set<DataComponentType<?>> excludedComponents) {
        var defaultModel = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Map<DataComponentType<?>, Object> identity = new LinkedHashMap<>();
        stack.getComponents().forEach(component -> {
            DataComponentType<?> type = component.type();
            if (excludedComponents.contains(type)) {
                return;
            }
            if (type == DataComponents.ITEM_MODEL
                    && defaultModel.equals(stack.get(DataComponents.ITEM_MODEL))) {
                return;
            }
            identity.put(type, component.value());
        });
        return new FoodFamily(stack.getItem(), Map.copyOf(identity));
    }

    private static <T> boolean hasPatchedValue(ItemStack stack, DataComponentType<T> type) {
        DataComponentPatch patch = stack.getComponentsPatch();
        return patch.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == type && entry.getValue().isPresent());
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> patchedValue(ItemStack stack, DataComponentType<T> type) {
        DataComponentPatch patch = stack.getComponentsPatch();
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
            if (entry.getKey() == type) {
                return (Optional<T>) entry.getValue();
            }
        }
        return Optional.empty();
    }

    private record FoodFamily(Item item, Map<DataComponentType<?>, Object> identityComponents) {
    }
}
