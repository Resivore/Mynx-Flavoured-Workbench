package dev.resivore.radialslotcycler.core;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExactPairwiseSwapTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindTestComponents(Items.DIAMOND_PICKAXE);
        bindTestComponents(Items.TORCH);
        bindTestComponents(Items.COBBLESTONE);
    }

    @Test
    void occupiedAndOccupiedExchangeExactReferences() {
        List<ItemStack> items = emptyInventory(36);
        ItemStack hotbar = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack storage = new ItemStack(Items.TORCH, 64);
        items.set(4, hotbar);
        items.set(13, storage);

        assertTrue(ExactPairwiseSwap.exchange(items, 4, 13));

        assertSame(storage, items.get(4));
        assertSame(hotbar, items.get(13));
    }

    @Test
    void occupiedAndEmptyExchangeWithoutAlternativeDestinationSearch() {
        List<ItemStack> items = emptyInventory(36);
        ItemStack hotbar = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack empty = items.get(13);
        items.set(4, hotbar);

        ExactPairwiseSwap.exchange(items, 4, 13);

        assertSame(empty, items.get(4));
        assertSame(hotbar, items.get(13));
    }

    @Test
    void emptyAndOccupiedExchangeWithoutInsertion() {
        List<ItemStack> items = emptyInventory(36);
        ItemStack empty = items.get(4);
        ItemStack storage = new ItemStack(Items.DIAMOND_PICKAXE);
        items.set(13, storage);

        ExactPairwiseSwap.exchange(items, 4, 13);

        assertSame(storage, items.get(4));
        assertSame(empty, items.get(13));
    }

    @Test
    void componentHeavyStacksKeepCompleteIdentityAndCounts() {
        List<ItemStack> items = emptyInventory(63);
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.set(DataComponents.CUSTOM_NAME, Component.literal("Canary pick"));
        pickaxe.set(DataComponents.DAMAGE, 27);
        pickaxe.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        ItemStack torches = new ItemStack(Items.TORCH, 37);
        var pickaxePatch = pickaxe.getComponentsPatch();
        var torchPatch = torches.getComponentsPatch();
        items.set(4, pickaxe);
        items.set(58, torches);

        ExactPairwiseSwap.exchange(items, 4, 58);

        assertSame(torches, items.get(4));
        assertSame(pickaxe, items.get(58));
        assertEquals(37, items.get(4).getCount());
        assertEquals(1, items.get(58).getCount());
        assertEquals(torchPatch, items.get(4).getComponentsPatch());
        assertEquals(pickaxePatch, items.get(58).getComponentsPatch());
    }

    @Test
    void identicalPartialStacksSwapRatherThanMerge() {
        List<ItemStack> items = emptyInventory(36);
        ItemStack first = new ItemStack(Items.COBBLESTONE, 12);
        ItemStack second = new ItemStack(Items.COBBLESTONE, 48);
        items.set(0, first);
        items.set(9, second);

        ExactPairwiseSwap.exchange(items, 0, 9);

        assertSame(second, items.get(0));
        assertSame(first, items.get(9));
        assertEquals(48, items.get(0).getCount());
        assertEquals(12, items.get(9).getCount());
    }

    @Test
    void selectingTheAnchorIsAnExplicitNoOp() {
        List<ItemStack> items = emptyInventory(36);
        ItemStack hotbar = new ItemStack(Items.TORCH, 7);
        items.set(4, hotbar);

        assertFalse(ExactPairwiseSwap.exchange(items, 4, 4));
        assertSame(hotbar, items.get(4));
    }

    private static List<ItemStack> emptyInventory(int size) {
        return new ArrayList<>(Collections.nCopies(size, ItemStack.EMPTY));
    }

    private static void bindTestComponents(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
        }
    }
}
