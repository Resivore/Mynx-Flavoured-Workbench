package dev.resivore.matchajei.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.food.FoodProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MatchaCanonicalFoodReplacementsTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindDefaultComponents(Items.BREAD, true);
        bindDefaultComponents(Items.COOKED_COD, true);
        bindDefaultComponents(Items.GLOW_BERRIES, true);
        bindDefaultComponents(Items.ROTTEN_FLESH, true);
        bindDefaultComponents(Items.CRAFTING_TABLE, false);
        bindDefaultComponents(Items.POTION, false);
    }

    @Test
    void healthBearingBaseFoodReplacesOnlyItsExactPlainDefault() {
        ItemStack plainBread = new ItemStack(Items.BREAD);
        ItemStack enhancedBread = enhancedFood(Items.BREAD);
        ItemStack customBreadVariant = enhancedFood(Items.BREAD);
        customBreadVariant.set(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace("naan"));
        ItemStack ordinaryCraftingTable = new ItemStack(Items.CRAFTING_TABLE);
        ItemStack componentPotion = new ItemStack(Items.POTION);
        componentPotion.set(DataComponents.CUSTOM_NAME, Component.literal("legitimate potion variant"));

        List<ItemStack> defaults = MatchaCanonicalFoodReplacements.defaultsFor(
                List.of(enhancedBread, customBreadVariant, componentPotion));

        assertEquals(1, defaults.size());
        assertTrue(ItemStack.isSameItemSameComponents(plainBread, defaults.getFirst()));
        assertTrue(MatchaCanonicalFoodReplacements.isPlainDefaultReplacement(plainBread, defaults));
        assertFalse(MatchaCanonicalFoodReplacements.isPlainDefaultReplacement(customBreadVariant, defaults));
        assertFalse(MatchaCanonicalFoodReplacements.isPlainDefaultReplacement(componentPotion, defaults));
        assertFalse(MatchaCanonicalFoodReplacements.isPlainDefaultReplacement(ordinaryCraftingTable, defaults));
    }

    @Test
    void creativeReplacementPreservesEveryExactFoodVariantAndRestoresDefaultWhenCatalogChanges() {
        ItemStack plainBread = new ItemStack(Items.BREAD);
        ItemStack unrelated = new ItemStack(Items.CRAFTING_TABLE);
        ItemStack enhancedBread = enhancedFood(Items.BREAD);
        ItemStack enhancedCod = enhancedFood(Items.COOKED_COD);
        ItemStack customCod = enhancedFood(Items.COOKED_COD);
        customCod.set(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace("cooked_pufferfish"));
        List<ItemStack> searchContents = new ArrayList<>(List.of(plainBread, unrelated));
        Set<ItemStack> owned = identitySet();
        Set<ItemStack> suppressed = identitySet();

        MatchaCreativeSearchEntries.replaceOwned(
                searchContents, owned, suppressed, List.of(enhancedBread, enhancedCod, customCod));

        assertFalse(searchContents.contains(plainBread));
        assertSame(unrelated, searchContents.getFirst());
        assertEquals(3, owned.size());
        assertTrue(searchContents.stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, enhancedBread)));
        assertTrue(searchContents.stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, enhancedCod)));
        assertTrue(searchContents.stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, customCod)));

        MatchaCreativeSearchEntries.replaceOwned(
                searchContents, owned, suppressed, List.of(enhancedBread, enhancedCod, customCod));

        assertEquals(4, searchContents.size());
        assertFalse(searchContents.contains(plainBread));
        assertEquals(3, owned.size());
        assertEquals(1, suppressed.size());

        MatchaCreativeSearchEntries.replaceOwned(searchContents, owned, suppressed, List.of());

        assertTrue(searchContents.contains(plainBread));
        assertSame(unrelated, searchContents.getFirst());
        assertEquals(2, searchContents.size());
        assertTrue(owned.isEmpty());
        assertTrue(suppressed.isEmpty());
    }

    @Test
    void effectOnlyGlowBerriesAndGlowBerryMashReplaceDefaultsButKeepMatchaComponents() {
        ItemStack plainGlowBerries = new ItemStack(Items.GLOW_BERRIES);
        ItemStack plainRottenFlesh = new ItemStack(Items.ROTTEN_FLESH);
        ItemStack glowBerries = consumableOnlyEnhancedFood(Items.GLOW_BERRIES);
        glowBerries.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("heart effect indicator"))));
        ItemStack glowBerryMash = enhancedFood(Items.ROTTEN_FLESH);
        glowBerryMash.set(DataComponents.ITEM_NAME, Component.literal("Glow Berry Mash"));
        glowBerryMash.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("heart effect indicator"), Component.literal("Aura duration"))));
        ItemStack ordinaryVanilla = new ItemStack(Items.CRAFTING_TABLE);
        ItemStack legitimateVariant = new ItemStack(Items.POTION);
        legitimateVariant.set(DataComponents.CUSTOM_NAME, Component.literal("legitimate potion variant"));

        List<ItemStack> defaults = MatchaCanonicalFoodReplacements.defaultsFor(
                List.of(glowBerries, glowBerryMash, legitimateVariant));
        assertEquals(2, defaults.size());
        assertTrue(MatchaCanonicalFoodReplacements.isPlainDefaultReplacement(plainGlowBerries, defaults));
        assertTrue(MatchaCanonicalFoodReplacements.isPlainDefaultReplacement(plainRottenFlesh, defaults));
        assertFalse(MatchaCanonicalFoodReplacements.isPlainDefaultReplacement(ordinaryVanilla, defaults));
        assertFalse(MatchaCanonicalFoodReplacements.isPlainDefaultReplacement(legitimateVariant, defaults));

        List<ItemStack> searchContents = new ArrayList<>(List.of(
                plainGlowBerries, plainRottenFlesh, ordinaryVanilla, legitimateVariant));
        Set<ItemStack> owned = identitySet();
        Set<ItemStack> suppressed = identitySet();
        MatchaCreativeSearchEntries.replaceOwned(
                searchContents, owned, suppressed, List.of(glowBerries, glowBerryMash));

        assertFalse(searchContents.contains(plainGlowBerries));
        assertFalse(searchContents.contains(plainRottenFlesh));
        assertEquals(1, countEquivalent(searchContents, glowBerries));
        assertEquals(1, countEquivalent(searchContents, glowBerryMash));
        assertEquals(new ItemLore(List.of(Component.literal("heart effect indicator"))),
                searchContents.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, glowBerries))
                        .findFirst().orElseThrow().get(DataComponents.LORE));
        assertEquals(new ItemLore(List.of(
                        Component.literal("heart effect indicator"), Component.literal("Aura duration"))),
                searchContents.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, glowBerryMash))
                        .findFirst().orElseThrow().get(DataComponents.LORE));
        assertTrue(searchContents.contains(ordinaryVanilla));
        assertTrue(searchContents.contains(legitimateVariant));
    }

    @Test
    void categorySearchSourcesDropOnlyCanonicalDefaultsBeforeVanillaIndexesThem() {
        ItemStack plainGlowBerries = new ItemStack(Items.GLOW_BERRIES);
        ItemStack plainRottenFlesh = new ItemStack(Items.ROTTEN_FLESH);
        ItemStack glowBerries = consumableOnlyEnhancedFood(Items.GLOW_BERRIES);
        ItemStack glowBerryMash = enhancedFood(Items.ROTTEN_FLESH);
        ItemStack ordinaryVanilla = new ItemStack(Items.CRAFTING_TABLE);
        ItemStack legitimateVariant = new ItemStack(Items.POTION);
        legitimateVariant.set(DataComponents.CUSTOM_NAME, Component.literal("legitimate potion variant"));
        List<ItemStack> categorySearchEntries = new ArrayList<>(List.of(
                plainGlowBerries, plainRottenFlesh, ordinaryVanilla, legitimateVariant));

        MatchaCreativeSearchEntries.suppressCanonicalDefaults(
                categorySearchEntries, List.of(glowBerries, glowBerryMash));

        assertFalse(categorySearchEntries.contains(plainGlowBerries));
        assertFalse(categorySearchEntries.contains(plainRottenFlesh));
        assertTrue(categorySearchEntries.contains(ordinaryVanilla));
        assertTrue(categorySearchEntries.contains(legitimateVariant));
    }

    private static ItemStack enhancedFood(net.minecraft.world.item.Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.FOOD, new FoodProperties(0, 0.0F, true));
        stack.set(DataComponents.CONSUMABLE, new Consumable(
                1.6F, net.minecraft.world.item.ItemUseAnimation.EAT, null, true,
                List.of(new MarkerConsumeEffect())
        ));
        return stack;
    }

    private static ItemStack consumableOnlyEnhancedFood(net.minecraft.world.item.Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CONSUMABLE, new Consumable(
                1.6F, net.minecraft.world.item.ItemUseAnimation.EAT, null, true,
                List.of(new MarkerConsumeEffect())
        ));
        return stack;
    }

    private static Set<ItemStack> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private static int countEquivalent(List<ItemStack> contents, ItemStack expected) {
        return (int) contents.stream()
                .filter(stack -> ItemStack.isSameItemSameComponents(stack, expected))
                .count();
    }

    private static void bindDefaultComponents(net.minecraft.world.item.Item item, boolean food) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            DataComponentMap.Builder components = DataComponentMap.builder();
            if (food) {
                components.set(DataComponents.FOOD, new FoodProperties(1, 0.1F, false));
            }
            item.builtInRegistryHolder().bindComponents(components.build());
        }
    }

    private static final class MarkerConsumeEffect implements ConsumeEffect {
        @Override
        public Type<? extends ConsumeEffect> getType() {
            return null;
        }

        @Override
        public boolean apply(net.minecraft.world.level.Level level, ItemStack stack,
                             net.minecraft.world.entity.LivingEntity consumer) {
            return false;
        }
    }
}
