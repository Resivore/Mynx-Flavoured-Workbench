package dev.resivore.coalconsolidation;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProductionRecipeCodecTest {
    private static final List<String> RECIPE_RESOURCES = List.of(
            "minecraft/charcoal",
            "minecraft/copper_torch",
            "minecraft/fire_charge",
            "minecraft/soul_torch",
            "minecraft/torch",
            "smoking/charcoal",
            "crafting/black_dye",
            "crafting/fire_charge",
            "crafting/torch",
            "coal_consolidation/charcoal_to_coal");
    private static final Map<String, Recipe<?>> RECIPES = new LinkedHashMap<>();

    private static HolderLookup.Provider registries;

    @BeforeAll
    static void decodePackagedRecipesThroughMinecraft() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        var builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var vanillaData = VanillaRegistries.createLookup();
        registries = HolderLookup.Provider.create(java.util.stream.Stream.concat(
                builtIns.listRegistries(),
                vanillaData.listRegistries().filter(lookup -> builtIns.lookup(lookup.key()).isEmpty())));
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(pending -> pending.apply());
        bindRecipeTagsForCodecTests();

        for (String recipeResource : RECIPE_RESOURCES) {
            String resource = "/data/" + recipeResource.replaceFirst("/", "/recipe/") + ".json";
            try (var stream = ProductionRecipeCodecTest.class.getResourceAsStream(resource)) {
                assertNotNull(stream, () -> "packaged recipe is missing: " + resource);
                var json = JsonParser.parseReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
                Recipe<?> decoded = Recipe.CODEC.parse(
                        registries.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow();
                RECIPES.put(recipeResource, decoded);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void bindRecipeTagsForCodecTests() {
        MappedRegistry<Item> items = (MappedRegistry<Item>) BuiltInRegistries.ITEM;
        Map<TagKey<Item>, List<net.minecraft.core.Holder<Item>>> tags = Map.of(
                TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("logs_that_burn")),
                List.of(items.wrapAsHolder(Items.OAK_LOG), items.wrapAsHolder(Items.SPRUCE_LOG),
                        items.wrapAsHolder(Items.OAK_WOOD)),
                TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("soul_fire_base_blocks")),
                List.of(items.wrapAsHolder(Items.SOUL_SAND)));
        items.bindAllTagsToEmpty();
        items.bindTags(tags);
        items.freeze();
    }

    @Test
    void allTenPackagedRecipesDecodeThroughMinecraftProductionCodec() {
        assertEquals(RECIPE_RESOURCES, List.copyOf(RECIPES.keySet()));
    }

    @Test
    void cookingOverridesRemainFurnaceAndSmokerRecipes() {
        SmeltingRecipe smelting = assertInstanceOf(SmeltingRecipe.class, RECIPES.get("minecraft/charcoal"));
        SmokingRecipe smoking = assertInstanceOf(SmokingRecipe.class, RECIPES.get("smoking/charcoal"));

        assertCookingRoute(smelting, 200);
        assertCookingRoute(smoking, 100);
    }

    @Test
    void legacyRecipeMatchesCharcoalOnlyAndAssemblesOneCoal() {
        Recipe<CraftingInput> migration = crafting("coal_consolidation/charcoal_to_coal");

        CraftingInput charcoal = CraftingInput.of(1, 1, List.of(new ItemStack(Items.CHARCOAL)));
        CraftingInput coal = CraftingInput.of(1, 1, List.of(new ItemStack(Items.COAL)));
        assertTrue(migration.matches(charcoal, null));
        assertFalse(migration.matches(coal, null));
        ItemStack result = migration.assemble(charcoal);
        assertTrue(result.is(Items.COAL));
        assertEquals(1, result.getCount());
    }

    @Test
    void representativeFormerExplicitConsumerAcceptsCoalButNotCharcoal() {
        Recipe<CraftingInput> torch = crafting("minecraft/torch");
        CraftingInput coal = CraftingInput.of(1, 2, List.of(
                new ItemStack(Items.COAL), new ItemStack(Items.STICK)));
        CraftingInput charcoal = CraftingInput.of(1, 2, List.of(
                new ItemStack(Items.CHARCOAL), new ItemStack(Items.STICK)));

        assertTrue(torch.matches(coal, null));
        assertFalse(torch.matches(charcoal, null));
        assertEquals(4, torch.assemble(coal).getCount());
    }

    @SuppressWarnings("unchecked")
    private static Recipe<CraftingInput> crafting(String id) {
        return (Recipe<CraftingInput>) RECIPES.get(id);
    }

    private static void assertCookingRoute(AbstractCookingRecipe recipe, int expectedTime) {
        for (var item : List.of(Items.OAK_LOG, Items.SPRUCE_LOG, Items.OAK_WOOD)) {
            SingleRecipeInput input = new SingleRecipeInput(new ItemStack(item));
            assertTrue(recipe.matches(input, null));
            ItemStack result = recipe.assemble(input);
            assertTrue(result.is(Items.COAL));
            assertEquals(1, result.getCount());
        }
        assertFalse(recipe.matches(new SingleRecipeInput(new ItemStack(Items.CRIMSON_STEM)), null));
        assertEquals(0.15F, recipe.experience());
        assertEquals(expectedTime, recipe.cookingTime());
    }
}
