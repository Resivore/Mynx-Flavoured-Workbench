package dev.resivore.matchajei.client;

import dev.resivore.matchajei.data.MatchaDisplayData;
import dev.resivore.matchajei.network.MatchaJeiDataPayload;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class MatchaJeiRuntimeData {
    private static final Map<String, InstalledData> ARCHIVE = new HashMap<>();
    private static final java.util.function.Consumer<MatchaJeiDataPayload> DATA_LISTENER =
            MatchaJeiRuntimeData::apply;
    private static IJeiRuntime runtime;
    private static ActiveData active;

    private MatchaJeiRuntimeData() {
    }

    static void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        ARCHIVE.clear();
        active = null;
        MatchaClientData.addListener(DATA_LISTENER);
        apply(MatchaClientData.current());
    }

    static void onRuntimeUnavailable() {
        MatchaClientData.removeListener(DATA_LISTENER);
        runtime = null;
        ARCHIVE.clear();
        active = null;
    }

    private static void apply(MatchaJeiDataPayload payload) {
        if (runtime == null || active != null && active.installed().revision().equals(payload.revision())) {
            return;
        }
        if (active != null) {
            InstalledData installed = active.installed();
            if (!installed.trades().isEmpty()) {
                runtime.getRecipeManager().hideRecipes(MatchaJeiPlugin.MATCHA_VILLAGER_TRADES, installed.trades());
            }
            if (!installed.acquisitions().isEmpty()) {
                runtime.getRecipeManager().hideRecipes(MatchaJeiPlugin.MATCHA_ACQUISITIONS, installed.acquisitions());
            }
            IntroducedIngredients introduced = active.introducedIngredients();
            if (!introduced.vanilla().isEmpty()) {
                runtime.getIngredientManager().removeIngredientsAtRuntime(
                        VanillaTypes.ITEM_STACK,
                        introduced.vanilla()
                );
            }
            if (!introduced.exact().isEmpty()) {
                runtime.getIngredientManager().removeIngredientsAtRuntime(
                        MatchaExactIngredient.TYPE,
                        introduced.exact()
                );
            }
            if (!introduced.suppressedVanilla().isEmpty()) {
                addNewIngredients(VanillaTypes.ITEM_STACK, introduced.suppressedVanilla());
            }
            active = null;
        }
        if (payload.revision().isEmpty()) {
            return;
        }

        InstalledData installed = ARCHIVE.get(payload.revision());
        if (installed == null) {
            installed = new InstalledData(
                    payload.revision(),
                    payload.trades(),
                    payload.acquisitions(),
                    payload.catalog().stream().map(ItemStack::copy).toList(),
                    payload.canonicalDefaults().stream().map(ItemStack::copy).toList()
            );
            if (!installed.trades().isEmpty()) {
                runtime.getRecipeManager().addRecipes(MatchaJeiPlugin.MATCHA_VILLAGER_TRADES, installed.trades());
            }
            if (!installed.acquisitions().isEmpty()) {
                runtime.getRecipeManager().addRecipes(MatchaJeiPlugin.MATCHA_ACQUISITIONS, installed.acquisitions());
            }
            ARCHIVE.put(installed.revision(), installed);
        } else {
            if (!installed.trades().isEmpty()) {
                runtime.getRecipeManager().unhideRecipes(MatchaJeiPlugin.MATCHA_VILLAGER_TRADES, installed.trades());
            }
            if (!installed.acquisitions().isEmpty()) {
                runtime.getRecipeManager().unhideRecipes(MatchaJeiPlugin.MATCHA_ACQUISITIONS, installed.acquisitions());
            }
        }
        active = new ActiveData(installed, addNewIngredients(
                installed.catalog(), installed.canonicalDefaults()));
    }

    private static IntroducedIngredients addNewIngredients(
            List<ItemStack> candidates,
            List<ItemStack> canonicalDefaults
    ) {
        List<ItemStack> vanillaCandidates = candidates.stream()
                .filter(candidate -> !MatchaJeiPlugin.usesJeiOwnedSubtype(candidate))
                .map(candidate -> candidate.copyWithCount(1))
                .toList();
        List<MatchaExactIngredient> exactCandidates = candidates.stream()
                .filter(MatchaJeiPlugin::usesJeiOwnedSubtype)
                .map(candidate -> MatchaExactIngredient.of(candidate.copyWithCount(1)))
                .toList();
        List<ItemStack> suppressedVanilla = removeCanonicalFoodDefaults(canonicalDefaults);
        return new IntroducedIngredients(
                addNewIngredients(VanillaTypes.ITEM_STACK, vanillaCandidates),
                addNewIngredients(MatchaExactIngredient.TYPE, exactCandidates),
                suppressedVanilla
        );
    }

    /**
     * JEI owns its ordinary ingredient list. We only temporarily remove the
     * exact default food identities selected by the server's shared catalog
     * canonicalization, and
     * retain them for restoration before a changed catalog is installed.
     */
    private static List<ItemStack> removeCanonicalFoodDefaults(List<ItemStack> canonicalDefaults) {
        if (canonicalDefaults.isEmpty()) {
            return List.of();
        }
        var manager = runtime.getIngredientManager();
        IIngredientHelper<ItemStack> helper = manager.getIngredientHelper(VanillaTypes.ITEM_STACK);
        Set<Object> knownUids = manager.getAllIngredients(VanillaTypes.ITEM_STACK).stream()
                .map(ingredient -> helper.getUid(ingredient, UidContext.Ingredient))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<ItemStack> present = canonicalDefaults.stream()
                .filter(defaultStack -> knownUids.contains(helper.getUid(defaultStack, UidContext.Ingredient)))
                .map(ItemStack::copy)
                .toList();
        if (!present.isEmpty()) {
            manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, present);
        }
        return present;
    }

    private static <T> List<T> addNewIngredients(IIngredientType<T> type, List<T> candidates) {
        var manager = runtime.getIngredientManager();
        IIngredientHelper<T> helper = manager.getIngredientHelper(type);
        Set<Object> knownUids = manager.getAllIngredients(type).stream()
                .map(ingredient -> helper.getUid(ingredient, UidContext.Ingredient))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<T> introduced = new ArrayList<>();
        for (T candidate : candidates) {
            if (knownUids.add(helper.getUid(candidate, UidContext.Ingredient))) {
                introduced.add(helper.copyIngredient(candidate));
            }
        }
        if (!introduced.isEmpty()) {
            manager.addIngredientsAtRuntime(type, introduced);
        }
        return List.copyOf(introduced);
    }

    /**
     * The exact-stack bridge asks JEI's own vanilla category lookup for the
     * existing recipe. No companion recipe object is created or registered.
     */
    static <T> List<T> findVanillaRecipes(
            IRecipeType<T> recipeType,
            RecipeIngredientRole role,
            ItemStack exactStack
    ) {
        IJeiRuntime currentRuntime = runtime;
        if (currentRuntime == null) {
            return List.of();
        }
        var focus = currentRuntime.getJeiHelpers().getFocusFactory().createFocus(
                role,
                VanillaTypes.ITEM_STACK,
                exactStack.copyWithCount(1)
        );
        return currentRuntime.getRecipeManager().createRecipeLookup(recipeType)
                .limitFocus(List.of(focus))
                .get()
                .toList();
    }

    private record InstalledData(
            String revision,
            List<MatchaDisplayData.Trade> trades,
            List<MatchaDisplayData.Acquisition> acquisitions,
            List<ItemStack> catalog,
            List<ItemStack> canonicalDefaults
    ) {
    }

    private record IntroducedIngredients(
            List<ItemStack> vanilla,
            List<MatchaExactIngredient> exact,
            List<ItemStack> suppressedVanilla
    ) {
    }

    private record ActiveData(InstalledData installed, IntroducedIngredients introducedIngredients) {
    }
}
