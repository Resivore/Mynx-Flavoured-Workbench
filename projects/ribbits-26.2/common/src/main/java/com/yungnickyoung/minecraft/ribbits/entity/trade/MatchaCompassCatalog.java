package com.yungnickyoung.minecraft.ribbits.entity.trade;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.List;

/**
 * Resolves the three optional component-bearing Matcha compass tiers without making Matcha a
 * required Ribbits runtime dependency.
 */
final class MatchaCompassCatalog {
    static final Identifier TITANIUM_LOOT_RESOURCE =
            Identifier.parse("minecraft:loot_table/chests/equipment/special_compass.json");
    static final String TITANIUM_ENTRY_CONTRACT_SHA256 =
            "d9cb0c40eaed8c05e7b634cf2043ac196927d56120be558be3bd091ae1661116";
    static final Identifier TITANIUM_MODEL = Identifier.parse("minecraft:titanium_compass");
    static final String TITANIUM_NAME_KEY = "item.kleispack.titanium_compass";

    private MatchaCompassCatalog() {
    }

    /**
     * SHA-256 values cover canonicalized recipe {@code result} JSON from immutable
     * Matcha_Flavoured_1_12.zip (SHA-256
     * 6209783021C358044ABEDABACEE471FAFF5BD4080437D4E3B5E51963F1804248).
     */
    enum RecipeCompass {
        COPPER("crafting:copper_compass",
                "afaf346942b288588aed7fdd018caae5d1a9dc1d1250d4ad8b1d7fdb1bb26a6e",
                "minecraft:copper_compass", "item.kleispack.copper_compass"),
        GOLDEN("crafting:golden_compass",
                "16c631e5ea09614dcdf7625c9631d7ffb2ad5f359e034052031209f1c5bd6fcc",
                "minecraft:golden_compass", "item.kleispack.golden_compass");

        private final Identifier recipeId;
        private final String resultContractSha256;
        private final Identifier itemModel;
        private final String itemNameKey;

        RecipeCompass(String recipeId, String resultContractSha256,
                      String itemModel, String itemNameKey) {
            this.recipeId = Identifier.parse(recipeId);
            this.resultContractSha256 = resultContractSha256;
            this.itemModel = Identifier.parse(itemModel);
            this.itemNameKey = itemNameKey;
        }

        Identifier recipeId() {
            return this.recipeId;
        }

        String resultContractSha256() {
            return this.resultContractSha256;
        }

        Identifier itemModel() {
            return this.itemModel;
        }

        String itemNameKey() {
            return this.itemNameKey;
        }
    }

    static List<ItemStack> resolveAll(ServerLevel level) {
        ItemStack copper = resolveRecipe(level, RecipeCompass.COPPER);
        ItemStack golden = resolveRecipe(level, RecipeCompass.GOLDEN);
        ItemStack titanium = resolveTitanium(level);
        return List.of(copper, golden, titanium);
    }

    private static ItemStack resolveRecipe(ServerLevel level, RecipeCompass compass) {
        Identifier resourceId = MatchaStackCatalog.recipeResource(compass.recipeId());
        JsonElement root = MatchaStackCatalog.readJson(
                level.getServer().getResourceManager(), resourceId);
        if (!root.isJsonObject() || !root.getAsJsonObject().has("result")) {
            throw new IllegalStateException("Optional Matcha compass recipe has no result: "
                    + resourceId);
        }
        MatchaStackCatalog.validateFingerprint(
                "Matcha compass recipe result " + compass.recipeId(),
                root.getAsJsonObject().get("result"), compass.resultContractSha256());

        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, compass.recipeId());
        RecipeHolder<?> holder = level.recipeAccess().byKey(key).orElseThrow(() ->
                new IllegalStateException("Missing optional Matcha compass recipe "
                        + compass.recipeId()));
        for (RecipeDisplay display : holder.value().display()) {
            ItemStack result = display.result().resolveForFirstStack(
                    SlotDisplayContext.fromLevel(level));
            if (!result.isEmpty()) {
                return verifyIdentity(result.copy(), compass.itemModel(), compass.itemNameKey(),
                        "Matcha recipe " + compass.recipeId());
            }
        }
        throw new IllegalStateException("Optional Matcha compass recipe has no resolvable result: "
                + compass.recipeId());
    }

    private static ItemStack resolveTitanium(ServerLevel level) {
        JsonElement root = MatchaStackCatalog.readJson(
                level.getServer().getResourceManager(), TITANIUM_LOOT_RESOURCE);
        JsonElement entry;
        JsonObject entryObject;
        JsonObject components;
        try {
            entry = root.getAsJsonObject().getAsJsonArray("pools").get(0)
                    .getAsJsonObject().getAsJsonArray("entries").get(0);
            entryObject = entry.getAsJsonObject();
            components = entryObject.getAsJsonArray("functions").get(0)
                    .getAsJsonObject().getAsJsonObject("components");
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Malformed optional Matcha Titanium Compass contract: "
                    + TITANIUM_LOOT_RESOURCE, exception);
        }
        MatchaStackCatalog.validateFingerprint("Matcha Titanium Compass loot entry", entry,
                TITANIUM_ENTRY_CONTRACT_SHA256);

        JsonObject encodedStack = new JsonObject();
        encodedStack.add("id", entryObject.get("name").deepCopy());
        encodedStack.addProperty("count", 1);
        encodedStack.add("components", components.deepCopy());
        RegistryOps<JsonElement> registryOps = RegistryOps.create(
                JsonOps.INSTANCE, level.registryAccess());
        ItemStack result = ItemStack.CODEC.parse(registryOps, encodedStack).getOrThrow();
        return verifyIdentity(result, TITANIUM_MODEL, TITANIUM_NAME_KEY,
                "Matcha Titanium Compass loot entry");
    }

    private static ItemStack verifyIdentity(ItemStack stack, Identifier expectedModel,
                                            String expectedNameKey, String source) {
        Identifier actualItem = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!Identifier.parse("minecraft:compass").equals(actualItem)) {
            throw new IllegalStateException(source + " resolved to " + actualItem
                    + ", expected minecraft:compass");
        }
        if (!expectedModel.equals(stack.get(DataComponents.ITEM_MODEL))) {
            throw new IllegalStateException(source + " has an unexpected item-model component");
        }
        if (!Component.translatable(expectedNameKey).equals(stack.get(DataComponents.ITEM_NAME))) {
            throw new IllegalStateException(source + " has an unexpected item-name component");
        }
        return stack;
    }
}
