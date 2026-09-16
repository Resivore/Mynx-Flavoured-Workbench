package dev.resivore.matchajei.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MatchaCreativeSearchEntriesTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindDefaultComponents(Items.ENCHANTED_BOOK);
        bindDefaultComponents(Items.CRAFTING_TABLE);
        bindDefaultComponents(Items.DIAMOND);
    }

    @Test
    void payloadReplacementPreservesForeignSearchEntriesAndRetiresOnlyOwnedIdentity() {
        ItemStack craftingTable = new ItemStack(Items.CRAFTING_TABLE);
        ItemStack unrelated = new ItemStack(Items.DIAMOND);
        ItemStack oldMatcha = namedBook("Blessing of Demeter", "Frost Walker II");
        ItemStack replacement = namedBook("Blessing of Demeter", "Frost Protection III");
        List<ItemStack> searchContents = new ArrayList<>(List.of(craftingTable, unrelated, oldMatcha));
        Set<ItemStack> owned = identitySet();
        owned.add(oldMatcha);

        MatchaCreativeSearchEntries.replaceOwned(searchContents, owned, List.of(replacement));

        assertSame(craftingTable, searchContents.get(0));
        assertSame(unrelated, searchContents.get(1));
        assertFalse(searchContents.contains(oldMatcha));
        assertEquals(3, searchContents.size());
        assertEquals(1, countEquivalent(searchContents, replacement));
        assertEquals(1, owned.size());
        assertTrue(owned.contains(searchContents.get(2)));
        assertEquals(1, searchContents.get(2).getCount());
    }

    @Test
    void repeatedPublicationIsIdempotentAndKeepsExactComponents() {
        ItemStack vanilla = new ItemStack(Items.CRAFTING_TABLE);
        ItemStack exact = namedBook("Blessing of Demeter", "Frost Protection III");
        exact.setCount(12);
        List<ItemStack> searchContents = new ArrayList<>(List.of(vanilla));
        Set<ItemStack> owned = identitySet();

        MatchaCreativeSearchEntries.replaceOwned(searchContents, owned, List.of(exact, exact.copy()));
        MatchaCreativeSearchEntries.replaceOwned(searchContents, owned, List.of(exact, exact.copy()));

        assertEquals(2, searchContents.size());
        assertSame(vanilla, searchContents.getFirst());
        assertEquals(1, countEquivalent(searchContents, exact));
        assertEquals(1, owned.size());
        ItemStack installed = searchContents.getLast();
        assertEquals(1, installed.getCount());
        assertEquals(Component.literal("Blessing of Demeter"), installed.get(DataComponents.ITEM_NAME));
        assertEquals(new ItemLore(List.of(Component.literal("Frost Protection III"))),
                installed.get(DataComponents.LORE));
    }

    @Test
    void equivalentForeignEntryIsNeitherDuplicatedNorClaimed() {
        ItemStack foreignExact = namedBook("Blessing of Demeter", "Frost Protection III");
        List<ItemStack> searchContents = new ArrayList<>(List.of(foreignExact));
        Set<ItemStack> owned = identitySet();

        MatchaCreativeSearchEntries.replaceOwned(searchContents, owned, List.of(foreignExact.copy()));
        MatchaCreativeSearchEntries.replaceOwned(searchContents, owned, List.of(foreignExact.copy()));

        assertEquals(1, searchContents.size());
        assertSame(foreignExact, searchContents.getFirst());
        assertTrue(owned.isEmpty());
    }

    @Test
    void rebuildOwnershipTrackingPrunesHistoricalReferencesByObjectIdentity() {
        ItemStack activeOwned = namedBook("Active", "Identity");
        ItemStack historicalOwned = namedBook("Historical", "Identity");
        ItemStack merelyEquivalent = activeOwned.copy();
        Set<ItemStack> owned = identitySet();
        owned.add(activeOwned);
        owned.add(historicalOwned);
        owned.add(merelyEquivalent);

        MatchaCreativeSearchEntries.retainActiveOwnership(List.of(activeOwned), owned);

        assertEquals(1, owned.size());
        assertTrue(owned.contains(activeOwned));
        assertFalse(owned.contains(merelyEquivalent));
    }

    @Test
    void realCreativeSearchSetSupportsOwnedRemovalAndExactInsertion() {
        ItemStack vanilla = new ItemStack(Items.CRAFTING_TABLE);
        ItemStack oldMatcha = namedBook("Blessing of Demeter", "Frost Walker II");
        ItemStack replacement = namedBook("Blessing of Demeter", "Frost Protection III");
        Set<ItemStack> searchContents = ItemStackLinkedSet.createTypeAndComponentsSet();
        searchContents.add(vanilla);
        searchContents.add(oldMatcha);
        Set<ItemStack> owned = identitySet();
        owned.add(oldMatcha);

        MatchaCreativeSearchEntries.replaceOwned(searchContents, owned, List.of(replacement));

        assertEquals(2, searchContents.size());
        assertTrue(searchContents.stream().anyMatch(stack -> stack == vanilla));
        assertEquals(1, countEquivalent(searchContents, replacement));
        assertEquals(1, owned.size());
    }

    private static int countEquivalent(Collection<ItemStack> contents, ItemStack expected) {
        return (int) contents.stream()
                .filter(stack -> ItemStack.isSameItemSameComponents(stack, expected))
                .count();
    }

    private static Set<ItemStack> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private static ItemStack namedBook(String name, String lore) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        stack.set(DataComponents.ITEM_NAME, Component.literal(name));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(lore))));
        return stack;
    }

    private static void bindDefaultComponents(net.minecraft.world.item.Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }
}
