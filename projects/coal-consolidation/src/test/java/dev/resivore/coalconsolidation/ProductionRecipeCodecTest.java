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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void effectiveReloadOverridesALaterVanillaCharcoalResource(@TempDir Path tempDir)
            throws IOException {
        Path competingPack = tempDir.resolve("later-vanilla");
        writeRecipe(competingPack, "minecraft", "charcoal", """
                {
                  "type": "minecraft:smelting",
                  "experience": 0.15,
                  "ingredient": "#minecraft:logs_that_burn",
                  "result": { "id": "minecraft:charcoal" }
                }
                """);
        Path productionPack = Path.of("src", "main", "resources").toAbsolutePath().normalize();
        try (var resources = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(
                new PathPackResources(packLocation("coal-consolidation-lower"), productionPack),
                new PathPackResources(packLocation("later-vanilla"), competingPack)))) {
            assertEquals("later-vanilla",
                    resources.getResource(Identifier.parse("minecraft:recipe/charcoal.json"))
                            .orElseThrow().sourcePackId());

            TestRecipeManager manager = new TestRecipeManager(registries);
            RecipeMap prepared = manager.prepareForTest(resources);
            assertCookingRoute(cooking(prepared, "minecraft:charcoal", SmeltingRecipe.class),
                    200, Items.CHARCOAL);

            manager.applyForTest(prepared, resources);

            SmeltingRecipe effectiveSmelting = assertInstanceOf(SmeltingRecipe.class,
                    manager.byKey(recipeKey("minecraft:charcoal")).orElseThrow().value());
            assertCookingRoute(effectiveSmelting, 200, Items.COAL);
        }
    }

    @Test
    void effectiveReloadOverridesALaterSmokingCharcoalResourceAndPreservesMetadata(
            @TempDir Path tempDir) throws IOException {
        Path competingPack = tempDir.resolve("later-smoking");
        writeRecipe(competingPack, "smoking", "charcoal", """
                {
                  "type": "minecraft:smoking",
                  "category": "food",
                  "group": "later-smoking-charcoal",
                  "show_notification": false,
                  "experience": 0.15,
                  "cookingtime": 100,
                  "ingredient": "#minecraft:logs_that_burn",
                  "result": { "id": "minecraft:charcoal" }
                }
                """);
        Path productionPack = Path.of("src", "main", "resources").toAbsolutePath().normalize();
        try (var resources = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(
                new PathPackResources(packLocation("coal-consolidation-lower"), productionPack),
                new PathPackResources(packLocation("later-smoking"), competingPack)))) {
            assertEquals("later-smoking",
                    resources.getResource(Identifier.parse("smoking:recipe/charcoal.json"))
                            .orElseThrow().sourcePackId());

            TestRecipeManager manager = new TestRecipeManager(registries);
            RecipeMap prepared = manager.prepareForTest(resources);
            RecipeHolder<?> preparedHolder = prepared.byKey(recipeKey("smoking:charcoal"));
            assertEquals(recipeKey("smoking:charcoal"), preparedHolder.id());
            SmokingRecipe preparedSmoking = assertInstanceOf(
                    SmokingRecipe.class, preparedHolder.value());
            assertCookingRoute(preparedSmoking, 100, Items.CHARCOAL);
            assertEquals("later-smoking-charcoal", preparedSmoking.group());
            assertFalse(preparedSmoking.showNotification());

            manager.applyForTest(prepared, resources);

            RecipeHolder<?> effectiveHolder = manager.byKey(recipeKey("smoking:charcoal"))
                    .orElseThrow();
            assertEquals(preparedHolder.id(), effectiveHolder.id());
            SmokingRecipe effectiveSmoking = assertInstanceOf(
                    SmokingRecipe.class, effectiveHolder.value());
            assertSame(preparedSmoking.input(), effectiveSmoking.input());
            assertEquals(preparedSmoking.category(), effectiveSmoking.category());
            assertEquals(preparedSmoking.group(), effectiveSmoking.group());
            assertEquals(preparedSmoking.showNotification(), effectiveSmoking.showNotification());
            assertCookingRoute(effectiveSmoking, 100, Items.COAL);
        }
    }

    @Test
    void enforcementFailsClosedWhenEitherOwnedTargetIsMissingOrHasTheWrongType() {
        RecipeHolder<?> furnace = holder("minecraft:charcoal", RECIPES.get("minecraft/charcoal"));
        RecipeHolder<?> smoker = holder("smoking:charcoal", RECIPES.get("smoking/charcoal"));

        assertThrows(IllegalStateException.class,
                () -> CharcoalRecipeEnforcer.enforce(RecipeMap.create(List.of(smoker))));
        assertThrows(IllegalStateException.class,
                () -> CharcoalRecipeEnforcer.enforce(RecipeMap.create(List.of(furnace))));
        assertThrows(IllegalStateException.class, () -> CharcoalRecipeEnforcer.enforce(
                RecipeMap.create(List.of(
                        holder("minecraft:charcoal", RECIPES.get("smoking/charcoal")),
                        smoker))));
        assertThrows(IllegalStateException.class, () -> CharcoalRecipeEnforcer.enforce(
                RecipeMap.create(List.of(
                        furnace,
                        holder("smoking:charcoal", RECIPES.get("minecraft/charcoal"))))));
    }

    @SuppressWarnings("unchecked")
    private static Recipe<CraftingInput> crafting(String id) {
        return (Recipe<CraftingInput>) RECIPES.get(id);
    }

    private static void assertCookingRoute(AbstractCookingRecipe recipe, int expectedTime) {
        assertCookingRoute(recipe, expectedTime, Items.COAL);
    }

    private static void assertCookingRoute(
            AbstractCookingRecipe recipe,
            int expectedTime,
            Item expectedResult) {
        for (var item : List.of(Items.OAK_LOG, Items.SPRUCE_LOG, Items.OAK_WOOD)) {
            SingleRecipeInput input = new SingleRecipeInput(new ItemStack(item));
            assertTrue(recipe.matches(input, null));
            ItemStack result = recipe.assemble(input);
            assertTrue(result.is(expectedResult));
            assertEquals(1, result.getCount());
        }
        assertFalse(recipe.matches(new SingleRecipeInput(new ItemStack(Items.CRIMSON_STEM)), null));
        assertEquals(0.15F, recipe.experience());
        assertEquals(expectedTime, recipe.cookingTime());
    }

    private static void writeRecipe(
            Path root,
            String namespace,
            String id,
            String json) throws IOException {
        Path recipe = root.resolve("data").resolve(namespace).resolve("recipe").resolve(id + ".json");
        Files.createDirectories(recipe.getParent());
        Files.writeString(recipe, json, StandardCharsets.UTF_8);
    }

    private static PackLocationInfo packLocation(String id) {
        return new PackLocationInfo(
                id,
                Component.literal(id),
                PackSource.BUILT_IN,
                Optional.empty());
    }

    private static ResourceKey<Recipe<?>> recipeKey(String id) {
        return ResourceKey.create(Registries.RECIPE, Identifier.parse(id));
    }

    private static RecipeHolder<?> holder(String id, Recipe<?> recipe) {
        return new RecipeHolder<>(recipeKey(id), recipe);
    }

    private static <T extends AbstractCookingRecipe> T cooking(
            RecipeMap map,
            String id,
            Class<T> type) {
        return assertInstanceOf(type, map.byKey(recipeKey(id)).value());
    }

    private static final class TestRecipeManager extends RecipeManager {
        private TestRecipeManager(HolderLookup.Provider registries) {
            super(registries);
        }

        private RecipeMap prepareForTest(ResourceManager resources) {
            return super.prepare(resources, InactiveProfiler.INSTANCE);
        }

        private void applyForTest(RecipeMap recipes, ResourceManager resources) {
            super.apply(recipes, resources, InactiveProfiler.INSTANCE);
        }
    }
}
