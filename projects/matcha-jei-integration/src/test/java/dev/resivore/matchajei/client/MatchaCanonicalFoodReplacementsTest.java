package dev.resivore.matchajei.client;

import dev.resivore.matchajei.data.MatchaFoodDiscoveryCatalog;
import dev.resivore.matchajei.data.MatchaFoodDiscoveryCatalog.Candidate;
import dev.resivore.matchajei.data.MatchaFoodDiscoveryCatalog.Source;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.food.FoodProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchaCanonicalFoodReplacementsTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindDefaultComponents(Items.BREAD, true);
        bindDefaultComponents(Items.COOKED_COD, true);
        bindDefaultComponents(Items.GLOW_BERRIES, true);
        bindDefaultComponents(Items.ROTTEN_FLESH, true);
        bindDefaultComponents(Items.RABBIT_FOOT, false);
        bindDefaultComponents(Items.CRAFTING_TABLE, false);
        bindDefaultComponents(Items.POTION, false);
    }

    @Test
    void braisedMushroomRecipeSupersedesPlainNonFoodCarrierEverywhere() {
        ItemStack plainRabbitFoot = new ItemStack(Items.RABBIT_FOOT);
        ItemStack braisedMushroom = enhancedFood(Items.RABBIT_FOOT, "heart effect indicator");
        ItemStack distinctRabbitFoot = new ItemStack(Items.RABBIT_FOOT);
        distinctRabbitFoot.set(DataComponents.CUSTOM_NAME, Component.literal("unrelated rabbit-foot variant"));

        MatchaFoodDiscoveryCatalog.Result result = MatchaFoodDiscoveryCatalog.canonicalize(List.of(
                new Candidate(braisedMushroom, Source.EFFECTIVE_RECIPE),
                new Candidate(distinctRabbitFoot, Source.NON_RECIPE)
        ));

        assertEquals(2, result.catalog().size());
        assertEquals(1, countEquivalent(result.catalog(), braisedMushroom));
        assertEquals(1, countEquivalent(result.catalog(), distinctRabbitFoot));
        assertEquals(1, result.canonicalDefaults().size());
        assertTrue(ItemStack.isSameItemSameComponents(plainRabbitFoot, result.canonicalDefaults().getFirst()));

        ItemStack craftingTable = new ItemStack(Items.CRAFTING_TABLE);
        List<ItemStack> search = new ArrayList<>(List.of(plainRabbitFoot, distinctRabbitFoot, craftingTable));
        Set<ItemStack> owned = identitySet();
        Set<ItemStack> suppressed = identitySet();
        MatchaCreativeSearchEntries.replaceOwned(
                search, owned, suppressed, result.catalog(), result.canonicalDefaults());

        assertFalse(search.contains(plainRabbitFoot));
        assertTrue(search.contains(distinctRabbitFoot));
        assertTrue(search.contains(craftingTable));
        assertEquals(1, countEquivalent(search, braisedMushroom));
        assertEquals(new ItemLore(List.of(Component.literal("heart effect indicator"))),
                search.stream().filter(stack -> ItemStack.isSameItemSameComponents(stack, braisedMushroom))
                        .findFirst().orElseThrow().get(DataComponents.LORE));

        List<ItemStack> categorySource = new ArrayList<>(List.of(
                plainRabbitFoot, distinctRabbitFoot, craftingTable));
        MatchaCreativeSearchEntries.suppressCanonicalDefaults(
                categorySource, result.canonicalDefaults(), identitySet());
        assertFalse(categorySource.contains(plainRabbitFoot));
        assertTrue(categorySource.contains(distinctRabbitFoot));
        assertTrue(categorySource.contains(craftingTable));
    }

    @Test
    void uniqueEffectiveRecipeWinsOnlyInsideItsExactPresentationFamily() {
        ItemStack recipe = enhancedFood(Items.BREAD, "recipe health");
        ItemStack staleTrade = enhancedFood(Items.BREAD, "stale trade health");
        ItemStack customModelFood = enhancedFood(Items.BREAD, "custom-model health");
        customModelFood.set(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace("naan"));
        ItemStack differentStackMechanics = enhancedFood(Items.BREAD, "different stack mechanics");
        differentStackMechanics.set(DataComponents.MAX_STACK_SIZE, 16);

        MatchaFoodDiscoveryCatalog.Result result = MatchaFoodDiscoveryCatalog.canonicalize(List.of(
                new Candidate(staleTrade, Source.NON_RECIPE),
                new Candidate(customModelFood, Source.NON_RECIPE),
                new Candidate(differentStackMechanics, Source.NON_RECIPE),
                new Candidate(recipe, Source.EFFECTIVE_RECIPE)
        ));

        assertEquals(3, result.catalog().size());
        assertEquals(1, countEquivalent(result.catalog(), recipe));
        assertEquals(0, countEquivalent(result.catalog(), staleTrade));
        assertEquals(1, countEquivalent(result.catalog(), customModelFood));
        assertEquals(1, countEquivalent(result.catalog(), differentStackMechanics));
        assertEquals(1, result.canonicalDefaults().size());
        assertTrue(ItemStack.isSameItemSameComponents(
                new ItemStack(Items.BREAD), result.canonicalDefaults().getFirst()));
    }

    @Test
    void ambiguousNonRecipeHealthVariantsAndUnrelatedComponentsRemainSeparate() {
        ItemStack firstLoot = enhancedFood(Items.BREAD, "loot duration 20");
        ItemStack secondLoot = enhancedFood(Items.BREAD, "loot duration 24");
        ItemStack potion = enhancedFood(Items.POTION, "potion food control");
        potion.set(DataComponents.CUSTOM_NAME, Component.literal("legitimate potion subtype"));

        MatchaFoodDiscoveryCatalog.Result result = MatchaFoodDiscoveryCatalog.canonicalize(List.of(
                new Candidate(firstLoot, Source.NON_RECIPE),
                new Candidate(secondLoot, Source.NON_RECIPE),
                new Candidate(potion, Source.EFFECTIVE_RECIPE)
        ));

        assertEquals(3, result.catalog().size());
        assertEquals(1, countEquivalent(result.catalog(), firstLoot));
        assertEquals(1, countEquivalent(result.catalog(), secondLoot));
        assertEquals(1, countEquivalent(result.catalog(), potion));
        assertEquals(1, result.canonicalDefaults().size());
        assertTrue(ItemStack.isSameItemSameComponents(
                new ItemStack(Items.BREAD), result.canonicalDefaults().getFirst()));
    }

    @Test
    void glowBerriesAndGlowBerryMashStillReplaceOnlyTheirPlainDefaults() {
        ItemStack glowBerries = consumableOnlyEnhancedFood(Items.GLOW_BERRIES);
        glowBerries.set(DataComponents.LORE,
                new ItemLore(List.of(Component.literal("heart effect indicator"))));
        ItemStack glowBerryMash = enhancedFood(Items.ROTTEN_FLESH, "heart and Aura indicators");

        MatchaFoodDiscoveryCatalog.Result result = MatchaFoodDiscoveryCatalog.canonicalize(List.of(
                new Candidate(glowBerries, Source.NON_RECIPE),
                new Candidate(glowBerryMash, Source.EFFECTIVE_RECIPE)
        ));

        assertEquals(2, result.catalog().size());
        assertEquals(2, result.canonicalDefaults().size());
        List<ItemStack> search = new ArrayList<>(List.of(
                new ItemStack(Items.GLOW_BERRIES),
                new ItemStack(Items.ROTTEN_FLESH),
                new ItemStack(Items.CRAFTING_TABLE)
        ));
        MatchaCreativeSearchEntries.replaceOwned(
                search, identitySet(), identitySet(), result.catalog(), result.canonicalDefaults());

        assertEquals(1, countEquivalent(search, glowBerries));
        assertEquals(1, countEquivalent(search, glowBerryMash));
        assertTrue(search.stream().anyMatch(stack -> stack.is(Items.CRAFTING_TABLE)));
        assertFalse(search.stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(
                stack, new ItemStack(Items.GLOW_BERRIES))));
        assertFalse(search.stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(
                stack, new ItemStack(Items.ROTTEN_FLESH))));
    }

    @Test
    void repeatedReloadAndReconnectReconciliationIsIdempotentAndRestoresDefaults() {
        MatchaFoodDiscoveryCatalog.Result bread = MatchaFoodDiscoveryCatalog.canonicalize(List.of(
                new Candidate(enhancedFood(Items.BREAD, "bread health"), Source.EFFECTIVE_RECIPE)));
        MatchaFoodDiscoveryCatalog.Result cod = MatchaFoodDiscoveryCatalog.canonicalize(List.of(
                new Candidate(enhancedFood(Items.COOKED_COD, "cod health"), Source.EFFECTIVE_RECIPE)));
        ItemStack plainBread = new ItemStack(Items.BREAD);
        ItemStack plainCod = new ItemStack(Items.COOKED_COD);
        ItemStack craftingTable = new ItemStack(Items.CRAFTING_TABLE);
        List<ItemStack> search = new ArrayList<>(List.of(plainBread, plainCod, craftingTable));
        Set<ItemStack> owned = identitySet();
        Set<ItemStack> suppressed = identitySet();

        MatchaCreativeSearchEntries.replaceOwned(
                search, owned, suppressed, bread.catalog(), bread.canonicalDefaults());
        MatchaCreativeSearchEntries.replaceOwned(
                search, owned, suppressed, bread.catalog(), bread.canonicalDefaults());
        assertEquals(3, search.size());
        assertFalse(search.contains(plainBread));
        assertTrue(search.contains(plainCod));

        // A vanilla/Fabric category rebuild replaces the global Search
        // collection between payloads. The saved plain default must outlive
        // that rebuild so a later catalog can restore it.
        ItemStack rebuiltBread = bread.catalog().getFirst().copyWithCount(1);
        search.clear();
        search.add(plainCod);
        search.add(craftingTable);
        search.add(rebuiltBread);
        MatchaCreativeSearchEntries.rememberOwned(rebuiltBread, owned);
        MatchaCreativeSearchEntries.retainActiveOwnership(search, owned);
        assertEquals(1, suppressed.size());

        MatchaCreativeSearchEntries.replaceOwned(
                search, owned, suppressed, cod.catalog(), cod.canonicalDefaults());
        assertTrue(search.contains(plainBread));
        assertFalse(search.contains(plainCod));
        assertEquals(3, search.size());

        MatchaCreativeSearchEntries.replaceOwned(
                search, owned, suppressed, List.of(), List.of());
        assertTrue(search.contains(plainBread));
        assertTrue(search.contains(plainCod));
        assertSame(craftingTable, search.getFirst());
        assertEquals(3, search.size());
        assertTrue(owned.isEmpty());
        assertTrue(suppressed.isEmpty());
    }

    @Test
    void earlyPayloadRebuildBeforeSafeReconciliationStillRestoresDefaultOnDisconnect() {
        MatchaFoodDiscoveryCatalog.Result bread = MatchaFoodDiscoveryCatalog.canonicalize(List.of(
                new Candidate(enhancedFood(Items.BREAD, "bread health"), Source.EFFECTIVE_RECIPE)));
        ItemStack plainBread = new ItemStack(Items.BREAD);
        ItemStack craftingTable = new ItemStack(Items.CRAFTING_TABLE);
        Set<ItemStack> owned = identitySet();
        Set<ItemStack> suppressed = identitySet();

        // MatchaClientData is already current, but safe-tick reconciliation is
        // still deferred. A category rebuild must retain the default that it
        // removes before global Search is constructed.
        List<ItemStack> categorySource = new ArrayList<>(List.of(plainBread, craftingTable));
        MatchaCreativeSearchEntries.suppressCanonicalDefaults(
                categorySource, bread.canonicalDefaults(), suppressed);
        assertFalse(categorySource.contains(plainBread));
        assertEquals(1, suppressed.size());

        ItemStack indexedBread = bread.catalog().getFirst().copyWithCount(1);
        List<ItemStack> search = new ArrayList<>(categorySource);
        search.add(indexedBread);
        MatchaCreativeSearchEntries.rememberOwned(indexedBread, owned);

        MatchaCreativeSearchEntries.replaceOwned(
                search, owned, suppressed, bread.catalog(), bread.canonicalDefaults());
        assertFalse(search.stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, plainBread)));
        assertEquals(1, countEquivalent(search, bread.catalog().getFirst()));
        assertEquals(1, suppressed.size());

        MatchaCreativeSearchEntries.replaceOwned(
                search, owned, suppressed, List.of(), List.of());
        assertEquals(1, countEquivalent(search, plainBread));
        assertTrue(search.contains(craftingTable));
        assertTrue(owned.isEmpty());
        assertTrue(suppressed.isEmpty());
    }

    @Test
    void canonicalizationItselfIsIdempotent() {
        ItemStack canonical = enhancedFood(Items.RABBIT_FOOT, "canonical");
        MatchaFoodDiscoveryCatalog.Result first = MatchaFoodDiscoveryCatalog.canonicalize(List.of(
                new Candidate(canonical, Source.EFFECTIVE_RECIPE),
                new Candidate(canonical.copy(), Source.NON_RECIPE)
        ));
        MatchaFoodDiscoveryCatalog.Result second = MatchaFoodDiscoveryCatalog.canonicalize(
                first.catalog().stream().map(stack -> new Candidate(stack, Source.EFFECTIVE_RECIPE)).toList());

        assertEquals(1, first.catalog().size());
        assertEquals(1, second.catalog().size());
        assertTrue(ItemStack.isSameItemSameComponents(first.catalog().getFirst(), second.catalog().getFirst()));
        assertEquals(1, first.canonicalDefaults().size());
        assertEquals(1, second.canonicalDefaults().size());
    }

    private static ItemStack enhancedFood(Item item, String lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.FOOD, new FoodProperties(0, 0.0F, true));
        stack.set(DataComponents.CONSUMABLE, markerConsumable());
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(lore))));
        return stack;
    }

    private static ItemStack consumableOnlyEnhancedFood(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CONSUMABLE, markerConsumable());
        return stack;
    }

    private static Consumable markerConsumable() {
        return new Consumable(
                1.6F,
                net.minecraft.world.item.ItemUseAnimation.EAT,
                null,
                true,
                List.of(new MarkerConsumeEffect())
        );
    }

    private static Set<ItemStack> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private static int countEquivalent(List<ItemStack> contents, ItemStack expected) {
        return (int) contents.stream()
                .filter(stack -> ItemStack.isSameItemSameComponents(stack, expected))
                .count();
    }

    private static void bindDefaultComponents(Item item, boolean food) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            DataComponentMap.Builder components = DataComponentMap.builder()
                    .set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(item));
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
        public boolean apply(
                net.minecraft.world.level.Level level,
                ItemStack stack,
                net.minecraft.world.entity.LivingEntity consumer
        ) {
            return false;
        }
    }
}
