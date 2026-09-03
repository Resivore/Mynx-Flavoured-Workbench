package com.yungnickyoung.minecraft.ribbits.contract;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemAndRecipeResourceContractTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("projectRoot"));

    @Test
    void permanentMechanicalItemsAreRegisteredOnceAsPlainStackSixtyFourItems() throws IOException {
        String items = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/ItemModule.java");
        String creative = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/CreativeTabModule.java");

        assertItemRegistration(items, "glowcap", "GLOWCAP");
        assertItemRegistration(items, "toadstool_heart", "TOADSTOOL_HEART");
        assertEquals(1, occurrences(creative, "CreativeEntry.of(\"glowcap\", ItemModule.GLOWCAP::get)"));
        assertEquals(1, occurrences(creative,
                "CreativeEntry.of(\"toadstool_heart\", ItemModule.TOADSTOOL_HEART::get)"));
    }

    @Test
    void placeholdersReferenceExistingVisualModelsWithoutCommittingTextures() throws IOException {
        assertEquals("{\n  \"model\": {\n    \"type\": \"minecraft:model\",\n"
                        + "    \"model\": \"minecraft:item/warped_fungus\"\n  }\n}\n",
                read("common/src/main/resources/assets/ribbits/items/glowcap.json"));
        assertEquals("{\n  \"model\": {\n    \"type\": \"minecraft:model\",\n"
                        + "    \"model\": \"minecraft:item/heart_container\"\n  }\n}\n",
                read("common/src/main/resources/assets/ribbits/items/toadstool_heart.json"));

        assertFalse(Files.exists(PROJECT_ROOT.resolve(
                "common/src/main/resources/assets/ribbits/textures/item/glowcap.png")));
        assertFalse(Files.exists(PROJECT_ROOT.resolve(
                "common/src/main/resources/assets/ribbits/textures/item/toadstool_heart.png")));
    }

    @Test
    void customRecipeAndUnlockRemainBoundToExactIdsAndCrystalHeartComponent() throws IOException {
        String recipe = read("common/src/main/resources/data/ribbits/recipe/toadstool_heart.json");
        String advancement = read(
                "common/src/main/resources/data/ribbits/advancement/recipes/misc/toadstool_heart.json");
        String implementation = read(
                "common/src/main/java/com/yungnickyoung/minecraft/ribbits/recipe/ToadstoolHeartRecipe.java");

        assertTrue(recipe.contains("\"type\": \"ribbits:toadstool_heart\""));
        assertTrue(advancement.contains("\"items\": \"ribbits:toadstool\""));
        assertTrue(advancement.contains("\"items\": \"minecraft:poisonous_potato\""));
        assertTrue(advancement.contains("\"minecraft:item_model\": \"minecraft:heart_container\""));
        assertTrue(advancement.contains("\"translate\": \"item.kleispack.crystal_heart\""));
        assertTrue(advancement.contains("\"minecraft:rarity\": \"rare\""));
        assertTrue(advancement.contains("\"minecraft:enchantment_glint_override\": true"));
        assertTrue(advancement.contains("\"ribbits:toadstool_heart\""));
        JsonArray requirements = JsonParser.parseString(advancement).getAsJsonObject()
                .getAsJsonArray("requirements");
        assertEquals(2, requirements.size(), "both unlock ingredients are required");
        assertEquals(1, requirements.get(0).getAsJsonArray().size());
        assertEquals("has_small_toadstool", requirements.get(0).getAsJsonArray().get(0).getAsString());
        assertEquals(1, requirements.get(1).getAsJsonArray().size());
        assertEquals("has_original_crystal_heart", requirements.get(1).getAsJsonArray().get(0).getAsString());
        assertTrue(implementation.contains("input.ingredientCount() != 6"));
        assertEquals(5, occurrences(implementation, ".is(toadstool)"));
        assertTrue(implementation.contains("return new ItemStack(ItemModule.TOADSTOOL_HEART.get(), 1);"));
        assertTrue(implementation.contains("ItemStack.isSameItemSameComponents(stack, originalCrystalHeartStack())"));
        assertTrue(implementation.contains("stack.remove(DataComponents.CONSUMABLE);"));
        assertTrue(implementation.contains("return false;\n    }\n\n    @Override\n    public boolean showNotification()"),
                "recipe is non-special and exposes the normal unlock notification");
    }

    @Test
    void serializerIsRegisteredExactlyOnceDuringCommonInitialization() throws IOException {
        String recipes = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/module/RecipeModule.java");
        String common = read("common/src/main/java/com/yungnickyoung/minecraft/ribbits/RibbitsCommon.java");

        assertEquals(1, occurrences(recipes, "Registry.register("));
        assertTrue(recipes.contains("BuiltInRegistries.RECIPE_SERIALIZER"));
        assertTrue(recipes.contains("RibbitsCommon.id(\"toadstool_heart\")"));
        assertEquals(1, occurrences(common, "RecipeModule.init();"));
    }

    private static void assertItemRegistration(String source, String id, String field) {
        assertEquals(1, occurrences(source, "@AutoRegister(\"" + id + "\")"));
        assertEquals(1, occurrences(source, "public static final AutoRegisterItem " + field));
        assertEquals(1, occurrences(source, "RegisterHelper.itemKey(\"" + id + "\")"));
        assertTrue(source.contains("new Item.Properties().stacksTo(64).setId(RegisterHelper.itemKey(\"" + id + "\"))"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(PROJECT_ROOT.resolve(relativePath)).replace("\r\n", "\n");
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = value.indexOf(needle, fromIndex)) >= 0) {
            count++;
            fromIndex += needle.length();
        }
        return count;
    }
}
