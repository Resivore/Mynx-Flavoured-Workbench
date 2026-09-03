package com.yungnickyoung.minecraft.ribbits.recipe;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.CraftingInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToadstoolHeartRecipeTest {
    private static Item testFoliage;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var builtIns = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var vanillaData = VanillaRegistries.createLookup();
        HolderLookup.Provider registries = HolderLookup.Provider.create(
                java.util.stream.Stream.concat(
                        builtIns.listRegistries(),
                        vanillaData.listRegistries().filter(lookup -> builtIns.lookup(lookup.key()).isEmpty())
                )
        );
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
                .forEach(pending -> pending.apply());
        testFoliage = Items.RED_MUSHROOM;
    }

    @Test
    void exactThreeByThreeShapeAcceptsFiveFoliageItemsAndOriginalCrystalHeart() {
        assertTrue(ToadstoolHeartRecipe.matchesPattern(exactPattern(originalCrystalHeart()), testFoliage));
    }

    @Test
    void centerRejectsGenericBaseItemAndReinforcedCrystalHeart() {
        assertFalse(ToadstoolHeartRecipe.matchesPattern(
                exactPattern(new ItemStack(Items.POISONOUS_POTATO)), testFoliage));

        ItemStack reinforced = new ItemStack(Items.POISONOUS_POTATO);
        reinforced.set(
                DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath("matcha_heart_death_compat", "reinforced_crystal_heart")
        );
        assertFalse(ToadstoolHeartRecipe.matchesPattern(exactPattern(reinforced), testFoliage));
    }

    @Test
    void foliageSlotsRejectLargeMushroomBuildingMaterials() {
        for (Item rejected : List.of(Items.RED_MUSHROOM_BLOCK, Items.BROWN_MUSHROOM_BLOCK, Items.MUSHROOM_STEM)) {
            List<ItemStack> stacks = exactPattern(originalCrystalHeart()).items();
            List<ItemStack> changed = new ArrayList<>(stacks);
            changed.set(0, new ItemStack(rejected));
            assertFalse(ToadstoolHeartRecipe.matchesPattern(CraftingInput.of(3, 3, changed), testFoliage));
        }
    }

    @Test
    void emptySlotsAndExactGridSizeAreEnforced() {
        List<ItemStack> changed = new ArrayList<>(exactPattern(originalCrystalHeart()).items());
        changed.set(1, new ItemStack(testFoliage));
        assertFalse(ToadstoolHeartRecipe.matchesPattern(CraftingInput.of(3, 3, changed), testFoliage));

        assertFalse(ToadstoolHeartRecipe.matchesPattern(
                CraftingInput.of(2, 2, List.of(
                        new ItemStack(testFoliage), new ItemStack(testFoliage),
                        originalCrystalHeart(), new ItemStack(testFoliage)
                )),
                testFoliage
        ));
    }

    @Test
    void originalCrystalHeartIdentityIsBaseItemPlusExactMatchaModel() {
        ItemStack exact = originalCrystalHeart();
        assertTrue(ToadstoolHeartRecipe.isOriginalCrystalHeart(exact));

        ItemStack wrongBase = new ItemStack(Items.APPLE);
        wrongBase.set(DataComponents.ITEM_MODEL, ToadstoolHeartRecipe.CRYSTAL_HEART_MODEL);
        assertFalse(ToadstoolHeartRecipe.isOriginalCrystalHeart(wrongBase));

        for (var component : List.of(
                DataComponents.ITEM_NAME,
                DataComponents.RARITY,
                DataComponents.ENCHANTMENT_GLINT_OVERRIDE
        )) {
            ItemStack missingRequiredComponent = originalCrystalHeart();
            missingRequiredComponent.remove(component);
            assertFalse(ToadstoolHeartRecipe.isOriginalCrystalHeart(missingRequiredComponent));
        }

        ItemStack madeConsumableAgain = originalCrystalHeart();
        madeConsumableAgain.copyFrom(DataComponents.CONSUMABLE, new ItemStack(Items.POISONOUS_POTATO));
        assertFalse(ToadstoolHeartRecipe.isOriginalCrystalHeart(madeConsumableAgain));
    }

    private static CraftingInput exactPattern(ItemStack center) {
        return CraftingInput.of(3, 3, List.of(
                new ItemStack(testFoliage), ItemStack.EMPTY, new ItemStack(testFoliage),
                new ItemStack(testFoliage), center, new ItemStack(testFoliage),
                ItemStack.EMPTY, new ItemStack(testFoliage), ItemStack.EMPTY
        ));
    }

    private static ItemStack originalCrystalHeart() {
        ItemStack stack = new ItemStack(Items.POISONOUS_POTATO);
        stack.set(DataComponents.ITEM_MODEL, ToadstoolHeartRecipe.CRYSTAL_HEART_MODEL);
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.kleispack.crystal_heart"));
        stack.set(DataComponents.RARITY, Rarity.RARE);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.remove(DataComponents.CONSUMABLE);
        return stack;
    }

}
