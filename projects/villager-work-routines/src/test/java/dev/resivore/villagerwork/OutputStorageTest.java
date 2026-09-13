package dev.resivore.villagerwork;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OutputStorageTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        for (var item : new net.minecraft.world.item.Item[] {
                Blocks.WOOL.white().asItem(), Blocks.WOOL.red().asItem(), Blocks.DIAMOND_BLOCK.asItem()
        }) item.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, 64).build());
    }

    @Test void matchingStackIsFilledBeforeEmptySlotAndOtherContentsStayPut() {
        SimpleContainer barrel = new SimpleContainer(3);
        barrel.setItem(0, new ItemStack(Blocks.WOOL.white(), 63));
        barrel.setItem(1, new ItemStack(Blocks.DIAMOND_BLOCK, 7));
        ItemStack incoming = new ItemStack(Blocks.WOOL.white(), 3);
        assertEquals(3, OutputStorage.insert(barrel, incoming, incoming.getCount()));
        assertEquals(64, barrel.getItem(0).getCount());
        assertTrue(barrel.getItem(1).is(Blocks.DIAMOND_BLOCK.asItem()));
        assertEquals(7, barrel.getItem(1).getCount());
        assertTrue(barrel.getItem(2).is(Blocks.WOOL.white().asItem()));
        assertEquals(2, barrel.getItem(2).getCount());
    }

    @Test void fullContainerNeverOverwritesOrDeletes() {
        SimpleContainer barrel = new SimpleContainer(2);
        barrel.setItem(0, new ItemStack(Blocks.WOOL.white(), 64));
        barrel.setItem(1, new ItemStack(Blocks.DIAMOND_BLOCK, 64));
        ItemStack incoming = new ItemStack(Blocks.WOOL.white(), 3);
        assertFalse(OutputStorage.fits(barrel, incoming, 3));
        assertEquals(0, OutputStorage.insert(barrel, incoming, 3));
        assertEquals(64, barrel.getItem(0).getCount());
        assertEquals(64, barrel.getItem(1).getCount());
    }

    @Test void componentMismatchAndWoolColorRemainSeparate() {
        SimpleContainer barrel = new SimpleContainer(3);
        ItemStack named = new ItemStack(Blocks.WOOL.white(), 8);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("player wool"));
        barrel.setItem(0, named);
        ItemStack red = new ItemStack(Blocks.WOOL.red(), 2);
        barrel.setItem(1, red);
        ItemStack plain = new ItemStack(Blocks.WOOL.white(), 2);
        assertEquals(2, OutputStorage.insert(barrel, plain, 2));
        assertEquals(8, barrel.getItem(0).getCount());
        assertEquals(2, barrel.getItem(1).getCount());
        assertTrue(barrel.getItem(2).is(Blocks.WOOL.white().asItem()));
        assertNull(barrel.getItem(2).get(DataComponents.CUSTOM_NAME));
    }

    @Test void matchingWoolInLaterAdjacentBarrelBeatsEarlierEmptyBarrel() {
        SimpleContainer first = new SimpleContainer(2);
        SimpleContainer second = new SimpleContainer(2);
        second.setItem(1, new ItemStack(Blocks.WOOL.white(), 62));
        ItemStack incoming = new ItemStack(Blocks.WOOL.white(), 3);
        assertEquals(3, OutputStorage.insertAcross(List.of(first, second), incoming, 3));
        assertEquals(64, second.getItem(1).getCount());
        assertEquals(1, first.getItem(0).getCount());
        assertTrue(first.getItem(1).isEmpty());
    }
}
