package dev.resivore.enderscapeintegration.recipe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Executable contract for C2's matcher and component-bearing display input. */
final class MatchaVoidCampfireRecipeTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        MinecraftRecipeTestBootstrap.initialize();
    }

    @Test
    void exactKindlingAndVoidShaleAreTheOnlyAcceptedTwoInputs() {
        MatchaVoidCampfireRecipe recipe = standInRecipe();
        ItemStack kindling = MatchaVoidCampfireRecipe.matchaKindlingDisplayStack();
        ItemStack ordinaryEgg = new ItemStack(Items.CHICKEN_SPAWN_EGG);
        ItemStack stick = new ItemStack(Items.STICK);
        ItemStack stickWithKindlingModel = stick.copy();
        stickWithKindlingModel.set(DataComponents.ITEM_MODEL, MatchaVoidCampfireRecipe.MATCHA_KINDLING_MODEL);
        ItemStack standInVoidShale = new ItemStack(Items.END_STONE);
        java.util.function.Predicate<ItemStack> voidShale = stack -> stack.is(Items.END_STONE);

        assertTrue(MatchaVoidCampfireRecipe.isMatchaKindling(kindling));
        assertFalse(MatchaVoidCampfireRecipe.isMatchaKindling(ordinaryEgg));
        assertFalse(MatchaVoidCampfireRecipe.isMatchaKindling(stickWithKindlingModel));
        assertTrue(matches(recipe, kindling, standInVoidShale));
        assertTrue(matches(recipe, standInVoidShale, kindling));
        assertFalse(matches(recipe, ordinaryEgg, standInVoidShale));
        assertFalse(matches(recipe, stick, standInVoidShale));
        assertFalse(matches(recipe, kindling, kindling));
        assertFalse(recipe.matches(CraftingInput.of(3, 1, List.of(kindling, standInVoidShale, stick)), null));
        assertFalse(MatchaVoidCampfireRecipe.matchesIngredients(
                List.of(kindling, standInVoidShale, stick), 3, voidShale));

        assertTrue(MatchaVoidCampfireRecipe.isVoidShaleId(
                Identifier.parse("enderscape:void_shale")));
        assertFalse(MatchaVoidCampfireRecipe.isVoidShaleId(
                Identifier.withDefaultNamespace("end_stone")));
    }

    @Test
    void authoritativeDisplayInputIsTheExactComponentBearingKindlingStack() {
        MatchaVoidCampfireRecipe recipe = standInRecipe();
        assertEquals(1, recipe.display().size());
        ShapelessCraftingRecipeDisplay recipeDisplay =
                (ShapelessCraftingRecipeDisplay) recipe.display().getFirst();
        assertEquals(2, recipeDisplay.ingredients().size());
        SlotDisplay.ItemStackSlotDisplay kindlingDisplay =
                (SlotDisplay.ItemStackSlotDisplay) recipeDisplay.ingredients().getFirst();
        ItemStack displayedKindling = kindlingDisplay.stack().create();
        assertEquals(1, displayedKindling.getCount());
        assertTrue(displayedKindling.is(Items.CHICKEN_SPAWN_EGG));
        assertEquals(MatchaVoidCampfireRecipe.MATCHA_KINDLING_MODEL,
                displayedKindling.get(DataComponents.ITEM_MODEL));
        assertFalse(displayedKindling.is(Items.STICK));
        assertFalse(MatchaVoidCampfireRecipe.isMatchaKindling(new ItemStack(Items.CHICKEN_SPAWN_EGG)));
        assertEquals(Items.END_STONE,
                ((SlotDisplay.ItemSlotDisplay) recipeDisplay.ingredients().get(1)).item().value());
        assertEquals(Items.CAMPFIRE,
                ((SlotDisplay.ItemSlotDisplay) recipeDisplay.result()).item().value());
        assertTrue(recipe.assemble(CraftingInput.EMPTY).is(Items.CAMPFIRE));
    }

    private static boolean matches(
            MatchaVoidCampfireRecipe recipe,
            ItemStack first,
            ItemStack second
    ) {
        return recipe.matches(CraftingInput.of(2, 1, List.of(first, second)), null);
    }

    private static MatchaVoidCampfireRecipe standInRecipe() {
        return new MatchaVoidCampfireRecipe(() -> Items.END_STONE, () -> Items.CAMPFIRE);
    }
}
