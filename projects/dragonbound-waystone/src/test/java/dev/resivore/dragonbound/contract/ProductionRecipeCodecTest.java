package dev.resivore.dragonbound.contract;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.resivore.dragonbound.DragonboundContent;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProductionRecipeCodecTest {
    private static final List<String> RECIPE_NAMES = List.of(
            "dragonbound_waystone",
            "waystone_material",
            "imbued_void_pearl",
            "dragonbound_staff");
    private static final Map<String, JsonObject> PACKAGED_RECIPES = new LinkedHashMap<>();
    private static final Map<String, Recipe<CraftingInput>> DECODED_RECIPES = new LinkedHashMap<>();

    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrapProductionRecipeCodecs() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (!BuiltInRegistries.ITEM.containsKey(DragonboundContent.WAYSTONE_ID)) {
            DragonboundContent.register();
        }

        var builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var vanillaData = VanillaRegistries.createLookup();
        registries = HolderLookup.Provider.create(
                java.util.stream.Stream.concat(
                        builtIns.listRegistries(),
                        vanillaData.listRegistries().filter(lookup -> builtIns.lookup(lookup.key()).isEmpty())));
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(pending -> pending.apply());

        for (String recipeName : RECIPE_NAMES) {
            String resource = "/data/dragonbound_waystone/recipe/" + recipeName + ".json";
            try (var stream = ProductionRecipeCodecTest.class.getResourceAsStream(resource)) {
                assertNotNull(stream, () -> "Packaged recipe is missing: " + resource);
                JsonObject json = JsonParser.parseReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                PACKAGED_RECIPES.put(recipeName, json);

                @SuppressWarnings("unchecked")
                Recipe<CraftingInput> decoded = (Recipe<CraftingInput>) Recipe.CODEC.parse(
                        registries.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow();
                DECODED_RECIPES.put(recipeName, decoded);
            }
        }
    }

    @Test
    void allFourPackagedRecipesDecodeThroughProductionRecipeCodec() {
        assertEquals(RECIPE_NAMES, List.copyOf(DECODED_RECIPES.keySet()));
        DECODED_RECIPES.forEach((name, recipe) ->
                assertNotNull(recipe, () -> "Recipe.CODEC returned null for " + name));
    }

    @Test
    void allFourExposeStandardDisplaysHandledByJeiVanillaCraftingCategory() {
        DECODED_RECIPES.forEach((name, recipe) -> {
            assertFalse(recipe.isSpecial(), () -> name + " must remain an ordinary crafting recipe");
            assertFalse(recipe.display().isEmpty(), () -> name + " has no synchronized recipe display");
            assertTrue(
                    recipe.display().getFirst() instanceof ShapedCraftingRecipeDisplay
                            || recipe.display().getFirst() instanceof ShapelessCraftingRecipeDisplay,
                    () -> name + " does not use a vanilla crafting display");
        });
    }

    @Test
    void staffMatchesExactVerticalPearlFavourAndStickRecipe() {
        Recipe<CraftingInput> staff = DECODED_RECIPES.get("dragonbound_staff");

        assertTrue(staff.matches(staffInput(
                new ItemStack(DragonboundContent.IMBUED_VOID_PEARL),
                new ItemStack(Items.NETHER_STAR),
                new ItemStack(Items.STICK)), null));
        assertFalse(staff.matches(staffInput(
                new ItemStack(DragonboundContent.IMBUED_VOID_PEARL),
                new ItemStack(Items.STICK),
                new ItemStack(Items.NETHER_STAR)), null));
        assertFalse(staff.matches(staffInput(
                new ItemStack(DragonboundContent.IMBUED_VOID_PEARL),
                new ItemStack(Items.NETHER_STAR),
                new ItemStack(Items.TURTLE_SCUTE)), null));
    }

    @Test
    void waystoneMatchesOnlyTheNewBottomWeightedCost() {
        Recipe<CraftingInput> waystone = DECODED_RECIPES.get("dragonbound_waystone");

        assertTrue(waystone.matches(CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, new ItemStack(Items.DRAGON_EGG), ItemStack.EMPTY,
                new ItemStack(Items.END_STONE_BRICKS), new ItemStack(Items.NETHER_STAR),
                new ItemStack(Items.END_STONE_BRICKS))), null));
        assertFalse(waystone.matches(CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, new ItemStack(Items.DRAGON_EGG), ItemStack.EMPTY,
                new ItemStack(Items.END_STONE_BRICKS), new ItemStack(Items.NETHER_STAR),
                new ItemStack(Items.END_STONE_BRICKS),
                new ItemStack(Items.END_STONE_BRICKS), new ItemStack(Items.END_STONE_BRICKS),
                new ItemStack(Items.END_STONE_BRICKS))), null));
    }

    @Test
    void imbuedVoidRecipeRemainsExactlyPearlAndDragonBreath() {
        Recipe<CraftingInput> imbuedVoid = DECODED_RECIPES.get("imbued_void_pearl");

        assertTrue(imbuedVoid.matches(CraftingInput.of(2, 1, List.of(
                new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.DRAGON_BREATH))), null));
        assertFalse(imbuedVoid.matches(CraftingInput.of(3, 1, List.of(
                new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.DRAGON_BREATH),
                new ItemStack(Items.STICK))), null));
    }

    private static CraftingInput staffInput(ItemStack top, ItemStack middle, ItemStack bottom) {
        return CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, top, ItemStack.EMPTY,
                ItemStack.EMPTY, middle, ItemStack.EMPTY,
                ItemStack.EMPTY, bottom, ItemStack.EMPTY));
    }
}
