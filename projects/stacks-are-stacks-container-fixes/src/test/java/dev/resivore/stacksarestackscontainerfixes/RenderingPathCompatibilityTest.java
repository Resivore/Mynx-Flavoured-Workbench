package dev.resivore.stacksarestackscontainerfixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RenderingPathCompatibilityTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void normallyNonStackableCountSurvivesClientContainerCopyWithEffectiveMaximum() {
        ItemStack staleClientSaddles = stack(Items.SADDLE, 3, 1);
        assertEquals(1, staleClientSaddles.getMaxStackSize());

        SimpleContainer beforeCompatibility = new SimpleContainer(1);
        beforeCompatibility.setItem(0, staleClientSaddles.copy());
        assertEquals(1, beforeCompatibility.getItem(0).getCount());

        ItemStack alignedClientSaddles = stack(Items.SADDLE, 3, 64);
        SimpleContainer afterCompatibility = new SimpleContainer(1);
        afterCompatibility.setItem(0, alignedClientSaddles);

        assertEquals(64, alignedClientSaddles.getMaxStackSize());
        assertTrue(alignedClientSaddles.getComponentsPatch().isEmpty());
        assertEquals(3, afterCompatibility.getItem(0).getCount());
    }

    @Test
    void shulkerTooltipMaterializationRetainsNormallyNonStackableCountWithEffectiveMaximum() {
        ItemStack staleClientSaddles = stack(Items.SADDLE, 3, 1);
        NonNullList<ItemStack> rejected = materialize(ItemContainerContents.fromItems(List.of(staleClientSaddles)));
        assertTrue(rejected.get(0).isEmpty());

        ItemStack alignedClientSaddles = stack(Items.SADDLE, 3, 64);
        NonNullList<ItemStack> retained = materialize(ItemContainerContents.fromItems(List.of(alignedClientSaddles)));

        assertTrue(alignedClientSaddles.getComponentsPatch().isEmpty());
        assertFalse(retained.get(0).isEmpty());
        assertEquals(Items.SADDLE, retained.get(0).getItem());
        assertEquals(3, retained.get(0).getCount());
    }

    @Test
    void ordinaryVanillaStackableItemsRemainUnchanged() {
        ItemStack stone = stack(Items.STONE, 3, 64);
        assertEquals(64, stone.getMaxStackSize());

        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, stone.copy());
        assertEquals(3, container.getItem(0).getCount());

        NonNullList<ItemStack> retained = materialize(ItemContainerContents.fromItems(List.of(stone)));
        assertEquals(Items.STONE, retained.get(0).getItem());
        assertEquals(3, retained.get(0).getCount());
    }

    private static ItemStack stack(Item item, int count, int maximum) {
        DataComponentMap components = DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, maximum)
                .build();
        return new ItemStack(Holder.direct(item, components), count);
    }

    private static NonNullList<ItemStack> materialize(ItemContainerContents contents) {
        NonNullList<ItemStack> destination = NonNullList.withSize(1, ItemStack.EMPTY);
        contents.copyInto(destination);
        return destination;
    }
}
