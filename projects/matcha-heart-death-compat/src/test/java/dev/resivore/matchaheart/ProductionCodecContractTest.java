package dev.resivore.matchaheart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Lifecycle;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.storage.loot.LootTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ProductionCodecContractTest {
    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrapProductionCodecs() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        var builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var vanillaData = VanillaRegistries.createLookup();
        var lootTables = new MappedRegistry<LootTable>(Registries.LOOT_TABLE, Lifecycle.stable());
        lootTables.register(
                ResourceKey.create(Registries.LOOT_TABLE,
                        Identifier.parse("minecraft:gameplay/fishing/fish/echo_fish")),
                LootTable.EMPTY,
                RegistrationInfo.BUILT_IN);
        lootTables.freeze();

        registries = HolderLookup.Provider.create(Stream.concat(
                Stream.concat(
                        builtIns.listRegistries(),
                        vanillaData.listRegistries().filter(lookup -> builtIns.lookup(lookup.key()).isEmpty())),
                Stream.of(lootTables)));
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(pending -> pending.apply());
    }

    @Test
    void everyCanonicalResourceDecodesThroughItsProductionCodec() {
        HeartDataContract.RECIPE_RESOURCES.values()
                .forEach(path -> AuthoritativeData.decodeRecipe(path, registries));
        HeartDataContract.ADVANCEMENT_RESOURCES.values()
                .forEach(path -> AuthoritativeData.decodeAdvancement(path, registries));
        HeartDataContract.LOOT_TABLE_RESOURCES.values()
                .forEach(path -> AuthoritativeData.decodeLootTable(path, registries));
    }

    @Test
    void recipeMapEnforcementReplacesOnlyTheThreeOwnedIdsAndPreservesEverySentinel() {
        Recipe<?> upstreamCrystal = craftingRecipe("crafting:sculk_sensor");
        Recipe<?> upstreamSensor = craftingRecipe("crafting:sculk_shrieker");
        Recipe<?> upstreamShrieker = craftingRecipe("crafting:crystal_heart");
        Recipe<?> unrelated = craftingRecipe("crafting:sculk_sensor");
        Recipe<?> dramaticDoors = craftingRecipe("crafting:sculk_sensor");
        Recipe<?> reinforced = craftingRecipe("crafting:sculk_sensor");
        Recipe<?> resonantFavour = craftingRecipe("crafting:sculk_sensor");
        RecipeMap resolved = RecipeMap.create(List.of(
                holder("crafting:crystal_heart", upstreamCrystal),
                holder("crafting:sculk_sensor", upstreamSensor),
                holder("crafting:sculk_shrieker", upstreamShrieker),
                holder("other:untouched", unrelated),
                holder("dramaticdoors:tall_oak_door", dramaticDoors),
                holder(HeartDataContract.REINFORCED_RECIPE_ID, reinforced),
                holder(HeartDataContract.RESONANT_FAVOUR_RECIPE_ID, resonantFavour)));

        RecipeMap enforced = RecipeMapEnforcer.enforce(resolved, registries);

        assertEquals(7, enforced.values().size());
        assertNotSame(upstreamCrystal, recipe(enforced, "crafting:crystal_heart"));
        assertNotSame(upstreamSensor, recipe(enforced, "crafting:sculk_sensor"));
        assertNotSame(upstreamShrieker, recipe(enforced, "crafting:sculk_shrieker"));
        assertSame(unrelated, recipe(enforced, "other:untouched"));
        assertSame(dramaticDoors, recipe(enforced, "dramaticdoors:tall_oak_door"));
        assertSame(reinforced, recipe(enforced, HeartDataContract.REINFORCED_RECIPE_ID));
        assertSame(resonantFavour, recipe(enforced, HeartDataContract.RESONANT_FAVOUR_RECIPE_ID));
    }

    @Test
    void recipeMapEnforcementPropagatesDecodeAndRebuildFailuresFailClosed() {
        Recipe<?> reinforced = craftingRecipe("crafting:sculk_sensor");
        Recipe<?> resonantFavour = craftingRecipe("crafting:sculk_sensor");
        RecipeMap resolved = RecipeMap.create(List.of(
                holder(HeartDataContract.REINFORCED_RECIPE_ID, reinforced),
                holder(HeartDataContract.RESONANT_FAVOUR_RECIPE_ID, resonantFavour)));

        IllegalStateException decodeFailure = assertThrows(IllegalStateException.class,
                () -> RecipeMapEnforcer.enforce(resolved, () -> {
                    throw new IllegalArgumentException("canonical decode failed");
                }));
        assertEquals("Unsafe Matcha recipe contracts", decodeFailure.getMessage());
        assertEquals("canonical decode failed", decodeFailure.getCause().getMessage());

        LinkedHashMap<Identifier, Recipe<?>> brokenReplacements = new LinkedHashMap<>();
        brokenReplacements.put(Identifier.parse("crafting:crystal_heart"),
                recipeThatFailsMapRebuild());
        IllegalStateException rebuildFailure = assertThrows(IllegalStateException.class,
                () -> RecipeMapEnforcer.enforce(resolved, () -> brokenReplacements));
        assertEquals("Unsafe Matcha recipe contracts", rebuildFailure.getMessage());
        assertEquals("recipe map rebuild failed", rebuildFailure.getCause().getMessage());
    }

    @Test
    void recipeMapEnforcementFailsClosedWhenEitherOwnedCanary9RecipeDidNotDecode() {
        Recipe<?> reinforced = craftingRecipe("crafting:sculk_sensor");
        RecipeMap missingResonant = RecipeMap.create(List.of(
                holder(HeartDataContract.REINFORCED_RECIPE_ID, reinforced)));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> RecipeMapEnforcer.enforce(missingResonant, LinkedHashMap::new));

        assertEquals("Unsafe Matcha recipe contracts", failure.getMessage());
        assertTrue(failure.getCause().getMessage().contains(
                HeartDataContract.RESONANT_FAVOUR_RECIPE_ID));
    }

    @Test
    void sensorRecipeHasTheExactTwoRowSculkLayout() {
        JsonObject json = AuthoritativeData.readJson(
                HeartDataContract.RECIPE_RESOURCES.get("crafting:sculk_sensor")).getAsJsonObject();
        assertEquals(List.of("v v", "sss"), strings(json.getAsJsonArray("pattern")));
        assertEquals("minecraft:sculk_vein", json.getAsJsonObject("key").get("v").getAsString());
        assertEquals("minecraft:sculk", json.getAsJsonObject("key").get("s").getAsString());
        assertFalse(json.toString().contains("minecraft:echo_shard"));

        Recipe<CraftingInput> recipe = craftingRecipe("crafting:sculk_sensor");
        assertTrue(recipe.matches(CraftingInput.of(3, 2, List.of(
                new ItemStack(Items.SCULK_VEIN), ItemStack.EMPTY, new ItemStack(Items.SCULK_VEIN),
                new ItemStack(Items.SCULK), new ItemStack(Items.SCULK), new ItemStack(Items.SCULK))), null));
    }

    @Test
    void shriekerRecipeHasTheExactThreeRowSculkAndBoneLayout() {
        JsonObject json = AuthoritativeData.readJson(
                HeartDataContract.RECIPE_RESOURCES.get("crafting:sculk_shrieker")).getAsJsonObject();
        assertEquals(List.of("b b", "v v", "sss"), strings(json.getAsJsonArray("pattern")));
        assertEquals("minecraft:bone", json.getAsJsonObject("key").get("b").getAsString());
        assertEquals("minecraft:sculk_vein", json.getAsJsonObject("key").get("v").getAsString());
        assertEquals("minecraft:sculk", json.getAsJsonObject("key").get("s").getAsString());
        assertFalse(json.toString().contains("minecraft:echo_shard"));

        Recipe<CraftingInput> recipe = craftingRecipe("crafting:sculk_shrieker");
        assertTrue(recipe.matches(CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.BONE), ItemStack.EMPTY, new ItemStack(Items.BONE),
                new ItemStack(Items.SCULK_VEIN), ItemStack.EMPTY, new ItemStack(Items.SCULK_VEIN),
                new ItemStack(Items.SCULK), new ItemStack(Items.SCULK), new ItemStack(Items.SCULK))), null));
    }

    @Test
    void fishingKeepsMatchaOutcomeWeightsWithAnEmptyShardSlot() {
        JsonObject json = AuthoritativeData.readJson(
                HeartDataContract.LOOT_TABLE_RESOURCES.get(
                        "minecraft:gameplay/fishing/deep_dark")).getAsJsonObject();
        JsonArray entries = json.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        assertEquals(3, entries.size());
        assertEquals("minecraft:loot_table", entries.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("minecraft:gameplay/fishing/fish/echo_fish",
                entries.get(0).getAsJsonObject().get("value").getAsString());
        assertEquals(7, entries.get(0).getAsJsonObject().get("weight").getAsInt());
        assertEquals("minecraft:sculk_vein", entries.get(1).getAsJsonObject().get("name").getAsString());
        assertEquals(20, entries.get(1).getAsJsonObject().get("weight").getAsInt());
        assertEquals("minecraft:empty", entries.get(2).getAsJsonObject().get("type").getAsString());
        assertEquals(1, entries.get(2).getAsJsonObject().get("weight").getAsInt());
        assertFalse(json.toString().contains("minecraft:echo_shard"));
    }

    @Test
    void blockTablesAreExactSilkTouchOnlyContracts() {
        assertSilkOnly("minecraft:blocks/sculk_sensor", "minecraft:sculk_sensor");
        assertSilkOnly("minecraft:blocks/calibrated_sculk_sensor", "minecraft:calibrated_sculk_sensor");
        assertSilkOnly("minecraft:blocks/sculk_shrieker", "minecraft:sculk_shrieker");
    }

    @Test
    void echoShardAdvancementUnlocksOnlyTheDiscRecipe() {
        JsonObject json = AuthoritativeData.readJson(
                HeartDataContract.ADVANCEMENT_RESOURCES.get(
                        "main:recipe_unlocks/echo_shard")).getAsJsonObject();
        assertEquals(List.of("crafting:music_disc_5"),
                strings(json.getAsJsonObject("rewards").getAsJsonArray("recipes")));
        assertEquals("minecraft:echo_shard", json.getAsJsonObject("criteria")
                .getAsJsonObject("has_primary_material")
                .getAsJsonObject("conditions")
                .getAsJsonArray("items").get(0).getAsJsonObject().get("items").getAsString());
    }

    @SuppressWarnings("unchecked")
    private static Recipe<CraftingInput> craftingRecipe(String id) {
        return (Recipe<CraftingInput>) AuthoritativeData.decodeRecipe(
                HeartDataContract.RECIPE_RESOURCES.get(id), registries);
    }

    private static RecipeHolder<?> holder(String id, Recipe<?> recipe) {
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.parse(id));
        return new RecipeHolder<>(key, recipe);
    }

    private static Recipe<?> recipe(RecipeMap recipes, String id) {
        return recipes.byKey(ResourceKey.create(Registries.RECIPE, Identifier.parse(id))).value();
    }

    private static Recipe<?> recipeThatFailsMapRebuild() {
        return (Recipe<?>) Proxy.newProxyInstance(
                Recipe.class.getClassLoader(),
                new Class<?>[] {Recipe.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getType")) {
                        throw new IllegalStateException("recipe map rebuild failed");
                    }
                    throw new AssertionError("Unexpected broken-recipe method: " + method.getName());
                });
    }

    private static void assertSilkOnly(String id, String blockItem) {
        JsonObject json = AuthoritativeData.readJson(
                HeartDataContract.LOOT_TABLE_RESOURCES.get(id)).getAsJsonObject();
        JsonObject pool = json.getAsJsonArray("pools").get(0).getAsJsonObject();
        assertEquals(1, json.getAsJsonArray("pools").size());
        assertEquals(1, pool.getAsJsonArray("conditions").size());
        assertEquals(1, pool.getAsJsonArray("entries").size());
        assertEquals("minecraft:match_tool",
                pool.getAsJsonArray("conditions").get(0).getAsJsonObject().get("condition").getAsString());
        assertTrue(pool.getAsJsonArray("conditions").get(0).toString().contains("minecraft:silk_touch"));
        assertEquals(blockItem,
                pool.getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
        assertFalse(json.toString().contains("minecraft:echo_shard"));
    }

    private static List<String> strings(JsonArray array) {
        return array.asList().stream().map(element -> element.getAsString()).toList();
    }
}
