package dev.resivore.enderscapeintegration.recipe;

import dev.resivore.enderscapeintegration.IntegrationContract;
import dev.resivore.enderscapeintegration.IntegrationRecipeMap;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.library.plugins.vanilla.crafting.CraftingCategoryExtension;
import mezz.jei.library.plugins.vanilla.crafting.VanillaRecipes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Controlled proof against JEI 30.18's real native crafting integration. */
final class EnderscapeJeiRecipeIntegrationTest {
    private static final String VOID_CAMPFIRE = "enderscape:void_campfire";
    private static final String VEILED_SAPLING = "enderscape:veiled_sapling";

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        MinecraftRecipeTestBootstrap.initialize();
    }

    @Test
    void actualJeiCraftingExtensionConsumesTheAuthoritativeComponentDisplay() {
        MatchaVoidCampfireRecipe recipe = standInVoidCampfireRecipe();
        RecipeHolder<CraftingRecipe> holder = holder(VOID_CAMPFIRE, recipe);
        CraftingCategoryExtension extension = new CraftingCategoryExtension();

        assertFalse(recipe.isSpecial());
        assertTrue(extension.isHandled(holder));
        assertEquals(2, extension.getIngredients(holder).size());
        assertEquals(((ShapelessCraftingRecipeDisplay) recipe.display().getFirst()).ingredients(),
                extension.getIngredients(holder));

        SlotDisplay.ItemStackSlotDisplay kindling = assertInstanceOf(
                SlotDisplay.ItemStackSlotDisplay.class,
                extension.getIngredients(holder).getFirst());
        var stack = kindling.stack().create();
        assertTrue(MatchaVoidCampfireRecipe.isMatchaKindling(stack));
        assertEquals(Items.CHICKEN_SPAWN_EGG, stack.getItem());
        assertEquals(MatchaVoidCampfireRecipe.MATCHA_KINDLING_MODEL,
                stack.get(net.minecraft.core.component.DataComponents.ITEM_MODEL));
        assertFalse(MatchaVoidCampfireRecipe.isMatchaKindling(
                new net.minecraft.world.item.ItemStack(Items.CHICKEN_SPAWN_EGG)));
        assertFalse(MatchaVoidCampfireRecipe.isMatchaKindling(
                new net.minecraft.world.item.ItemStack(Items.STICK)));
    }

    @Test
    void filteredResolvedMapFeedsJeiEachEffectiveCraftingRecipeExactlyOnceAfterRefresh() {
        assertEffectiveJeiState(IntegrationRecipeMap.removeSuppressed(RecipeMap.create(recipeFixture())));

        // A second preparation/reload starts from a fresh resolved map, and an
        // already-filtered map remains idempotent. Neither route can accumulate
        // compatibility-owned duplicates.
        RecipeMap refreshed = IntegrationRecipeMap.removeSuppressed(RecipeMap.create(recipeFixture()));
        assertEffectiveJeiState(refreshed);
        assertEffectiveJeiState(IntegrationRecipeMap.removeSuppressed(refreshed));
    }

    private static void assertEffectiveJeiState(RecipeMap resolved) {
        CraftingCategoryExtension extension = new CraftingCategoryExtension();
        VanillaRecipes.CraftingRecipes recipes = new VanillaRecipes(resolved)
                .getCraftingRecipes(categoryUsing(extension));
        List<RecipeHolder<CraftingRecipe>> handled = recipes.getHandled();

        assertEquals(1, count(handled, VOID_CAMPFIRE));
        assertEquals(1, count(handled, VEILED_SAPLING));
        assertEquals(0, recipes.getUnhandled().stream()
                .filter(holder -> VOID_CAMPFIRE.equals(id(holder))).count());
        IntegrationContract.BLOCKED_RECIPE_IDS.forEach(blocked ->
                assertEquals(0, count(handled, blocked), blocked));

        RecipeHolder<CraftingRecipe> voidCampfire = handled.stream()
                .filter(holder -> VOID_CAMPFIRE.equals(id(holder)))
                .findFirst().orElseThrow();
        assertInstanceOf(MatchaVoidCampfireRecipe.class, voidCampfire.value(),
                "the upstream stick recipe is replaced at its original ID");
        assertEquals(2, extension.getIngredients(voidCampfire).size());
        assertInstanceOf(SlotDisplay.ItemStackSlotDisplay.class,
                extension.getIngredients(voidCampfire).getFirst());

        RecipeHolder<CraftingRecipe> veiledSapling = handled.stream()
                .filter(holder -> VEILED_SAPLING.equals(id(holder)))
                .findFirst().orElseThrow();
        assertInstanceOf(ShapelessRecipe.class, veiledSapling.value());
        assertEquals(1, extension.getIngredients(veiledSapling).size());
    }

    private static List<RecipeHolder<?>> recipeFixture() {
        List<RecipeHolder<?>> holders = new ArrayList<>();
        holders.add(holder(VOID_CAMPFIRE, standInVoidCampfireRecipe()));
        holders.add(holder(VEILED_SAPLING, ordinaryLeavesToSaplingRecipe()));
        IntegrationContract.BLOCKED_RECIPE_IDS.forEach(id ->
                holders.add(holder(id, ordinaryLeavesToSaplingRecipe())));
        holders.add(holder("minecraft:retained_control", ordinaryLeavesToSaplingRecipe()));
        return holders;
    }

    private static MatchaVoidCampfireRecipe standInVoidCampfireRecipe() {
        return new MatchaVoidCampfireRecipe(() -> Items.END_STONE, () -> Items.CAMPFIRE);
    }

    private static ShapelessRecipe ordinaryLeavesToSaplingRecipe() {
        return new ShapelessRecipe(
                new Recipe.CommonInfo(true),
                new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""),
                new ItemStackTemplate(Items.OAK_SAPLING),
                List.of(Ingredient.of(Items.OAK_LEAVES)));
    }

    private static RecipeHolder<CraftingRecipe> holder(String id, CraftingRecipe recipe) {
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.parse(id));
        return new RecipeHolder<>(key, recipe);
    }

    @SuppressWarnings("unchecked")
    private static IRecipeCategory<RecipeHolder<CraftingRecipe>> categoryUsing(
            CraftingCategoryExtension extension
    ) {
        return (IRecipeCategory<RecipeHolder<CraftingRecipe>>) Proxy.newProxyInstance(
                EnderscapeJeiRecipeIntegrationTest.class.getClassLoader(),
                new Class<?>[]{IRecipeCategory.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("isHandled")) {
                        return extension.isHandled((RecipeHolder<CraftingRecipe>) arguments[0]);
                    }
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "JEI crafting category test proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == arguments[0];
                            default -> throw new UnsupportedOperationException(method.toString());
                        };
                    }
                    throw new UnsupportedOperationException(method.toString());
                });
    }

    private static long count(List<RecipeHolder<CraftingRecipe>> recipes, String id) {
        return recipes.stream().filter(holder -> id.equals(id(holder))).count();
    }

    private static String id(RecipeHolder<?> holder) {
        return holder.id().identifier().toString();
    }
}
