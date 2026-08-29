package dev.resivore.matchaheart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientInit;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ReinforcedRecipeContractTest {
    private static JsonObject recipe;
    private static JsonObject metadata;
    private static HolderLookup.Provider registries;
    private static Recipe<CraftingInput> reinforcedRecipe;

    @BeforeAll
    static void bootstrapProductionIngredientCodec() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        new CustomIngredientInit().onInitialize();

        var builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var vanillaData = VanillaRegistries.createLookup();
        registries = HolderLookup.Provider.create(
                java.util.stream.Stream.concat(
                        builtIns.listRegistries(),
                        vanillaData.listRegistries().filter(lookup -> builtIns.lookup(lookup.key()).isEmpty())));
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(pending -> pending.apply());

        try (var stream = ReinforcedRecipeContractTest.class.getResourceAsStream(
                "/data/matcha_heart_death_compat/recipe/reinforced_crystal_heart.json")) {
            assertTrue(stream != null, "Packaged Reinforced recipe is missing");
            recipe = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        try (var reader = Files.newBufferedReader(
                Path.of("src", "main", "resources", "fabric.mod.json"), StandardCharsets.UTF_8)) {
            metadata = JsonParser.parseReader(reader).getAsJsonObject();
        }

        @SuppressWarnings("unchecked")
        Recipe<CraftingInput> decoded = (Recipe<CraftingInput>) Recipe.CODEC.parse(
                registries.createSerializationContext(JsonOps.INSTANCE), recipe).getOrThrow();
        reinforcedRecipe = decoded;
    }

    @Test
    void packagedShapedRecipeAndHeartIngredientDecodeThroughProductionMinecraftFabricCodecs() {
        assertTrue(reinforcedRecipe != null,
                "Full Recipe.CODEC must decode the packaged shaped recipe");

        JsonObject heart = recipe.getAsJsonObject("key").getAsJsonObject("H");
        var decoded = Ingredient.CODEC.parse(
                registries.createSerializationContext(JsonOps.INSTANCE), heart);

        assertTrue(decoded.result().isPresent(),
                () -> "Production Ingredient.CODEC rejected the packaged heart ingredient: "
                        + decoded.error().map(Object::toString).orElse("unknown error"));
    }

    @Test
    void exactSixShardGridMatchesOnlyTheSemanticMatchaHeart() {
        assertTrue(reinforcedRecipe.matches(reinforcedInput(actualMatchaHeart()), null));
        assertFalse(reinforcedRecipe.matches(
                reinforcedInput(new ItemStack(Items.POISONOUS_POTATO)), null));

        ItemStack otherComponentPotato = new ItemStack(Items.POISONOUS_POTATO);
        otherComponentPotato.set(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace("potato"));
        assertFalse(reinforcedRecipe.matches(reinforcedInput(otherComponentPotato), null));

        assertFalse(reinforcedRecipe.matches(CraftingInput.of(3, 3, java.util.List.of(
                echoShard(), echoShard(), echoShard(),
                echoShard(), actualMatchaHeart(), echoShard(),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)), null),
                "The bottom-center Echo Shard is required");
        assertFalse(reinforcedRecipe.matches(CraftingInput.of(3, 3, java.util.List.of(
                echoShard(), echoShard(), echoShard(),
                echoShard(), actualMatchaHeart(), echoShard(),
                echoShard(), echoShard(), ItemStack.EMPTY)), null),
                "An Echo Shard in an intentionally empty corner must reject the recipe");
    }

    @Test
    void packagedRecipePinsTheExactSixShardLayoutAndFabricComponentIngredient() {
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        var pattern = recipe.getAsJsonArray("pattern");
        assertEquals(3, pattern.size());
        assertEquals("EEE", pattern.get(0).getAsString());
        assertEquals("EHE", pattern.get(1).getAsString());
        assertEquals(" E ", pattern.get(2).getAsString());
        String layout = pattern.get(0).getAsString()
                + pattern.get(1).getAsString()
                + pattern.get(2).getAsString();
        assertEquals(6L, layout.chars().filter(character -> character == 'E').count());
        assertEquals("minecraft:echo_shard",
                recipe.getAsJsonObject("key").get("E").getAsString());

        JsonObject heart = recipe.getAsJsonObject("key").getAsJsonObject("H");
        assertEquals("fabric:components", heart.get("fabric:type").getAsString());
        assertEquals("minecraft:poisonous_potato", heart.get("base").getAsString());
        assertEquals("minecraft:heart_container",
                heart.getAsJsonObject("components").get("minecraft:item_model").getAsString());
    }

    @Test
    void outputAndLiveMatchaIdentityContractRemainExact() {
        JsonObject result = recipe.getAsJsonObject("result");
        JsonObject outputComponents = result.getAsJsonObject("components");
        assertEquals("minecraft:poisonous_potato", result.get("id").getAsString());
        assertEquals(1, result.get("count").getAsInt());
        assertEquals("matcha_heart_death_compat:reinforced_crystal_heart",
                outputComponents.get("minecraft:item_model").getAsString());
        assertEquals("item.matcha_heart_death_compat.reinforced_crystal_heart",
                outputComponents.getAsJsonObject("minecraft:item_name").get("translate").getAsString());
        assertEquals("epic", outputComponents.get("minecraft:rarity").getAsString());
        assertTrue(outputComponents.get("minecraft:enchantment_glint_override").getAsBoolean());
        assertTrue(outputComponents.has("!minecraft:consumable"));

        assertEquals("minecraft:poisonous_potato", HeartDataContract.MATCHA_BASE_ITEM);
        assertEquals("minecraft:heart_container", HeartDataContract.MATCHA_ITEM_MODEL);
        assertEquals("item.kleispack.crystal_heart", HeartDataContract.MATCHA_ITEM_NAME);

        ItemStack assembled = reinforcedRecipe.assemble(reinforcedInput(actualMatchaHeart()));
        assertTrue(assembled.is(Items.POISONOUS_POTATO));
        assertEquals(Identifier.fromNamespaceAndPath(
                        MatchaHeartDeathCompat.MOD_ID, "reinforced_crystal_heart"),
                assembled.get(DataComponents.ITEM_MODEL));
        assertEquals(Component.translatable("item.matcha_heart_death_compat.reinforced_crystal_heart"),
                assembled.get(DataComponents.ITEM_NAME));
        assertEquals(Rarity.EPIC, assembled.get(DataComponents.RARITY));
        assertEquals(Boolean.TRUE, assembled.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        assertNull(assembled.get(DataComponents.CONSUMABLE));
    }

    @Test
    void jeiExposureIsHeartOwnedOptionalAndClientEntrypointOnly() {
        assertEquals("dev.resivore.matchaheart.client.HeartJeiPlugin",
                metadata.getAsJsonObject("entrypoints").getAsJsonArray("jei_mod_plugin").get(0).getAsString());
        assertEquals("30.18.0.144",
                metadata.getAsJsonObject("suggests").get("jei").getAsString());
        assertFalse(metadata.getAsJsonObject("depends").has("jei"));
        assertEquals("dev.resivore.matchaheart.MatchaHeartDeathCompat",
                metadata.getAsJsonObject("entrypoints").getAsJsonArray("main").get(0).getAsString());
    }

    private static ItemStack actualMatchaHeart() {
        ItemStack stack = new ItemStack(Items.POISONOUS_POTATO);
        stack.set(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace("heart_container"));
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.kleispack.crystal_heart"));
        stack.set(DataComponents.RARITY, Rarity.RARE);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.remove(DataComponents.CONSUMABLE);
        return stack;
    }

    private static ItemStack echoShard() {
        return new ItemStack(Items.ECHO_SHARD);
    }

    private static CraftingInput reinforcedInput(ItemStack heart) {
        return CraftingInput.of(3, 3, java.util.List.of(
                echoShard(), echoShard(), echoShard(),
                echoShard(), heart, echoShard(),
                ItemStack.EMPTY, echoShard(), ItemStack.EMPTY));
    }
}
