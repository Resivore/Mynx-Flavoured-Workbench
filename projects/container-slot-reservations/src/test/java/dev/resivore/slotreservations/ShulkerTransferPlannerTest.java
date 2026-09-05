package dev.resivore.slotreservations;

import net.minecraft.SharedConstants;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ShulkerTransferPlannerTest {
    @BeforeAll static void bootstrap() throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); initializeComponentsAfterBootstrap();
        bind(Blocks.SHULKER_BOX.asItem()); bind(Items.STONE); bind(Items.DIRT); bind(Items.DIAMOND); bind(Items.SADDLE);
    }

    @Test void insertionOrderIsMergeThenMatchingReservationThenUnreserved() {
        ItemStack host = shulker();
        var contents = empty();
        contents.set(5, stack(Items.STONE, 63));
        ShulkerContents.replace(host, contents);
        ReservationStore.setData(host, ReservationData.EMPTY.with(20, stack(Items.STONE, 1)));
        var result = ShulkerTransferPlanner.planInsertion(host, stack(Items.STONE, 3));
        assertEquals(3, result.moved());
        assertEquals(64, result.contents().get(5).getCount());
        assertEquals(2, result.contents().get(20).getCount());
        assertTrue(result.contents().get(0).isEmpty());
        assertTrue(result.remainder().isEmpty());
    }

    @Test void reservationsBeatEarlierEmptyFallbacksAndMismatchesAreSkipped() {
        ItemStack host = shulker();
        ReservationData data = ReservationData.EMPTY
                .with(8, new ItemStack(Items.DIRT))
                .with(26, namedStone("exact"));
        ReservationStore.setData(host, data);
        ItemStack incoming = namedStone("exact"); incoming.setCount(4);
        var result = ShulkerTransferPlanner.planInsertion(host, incoming);
        assertEquals(4, result.contents().get(26).getCount());
        assertTrue(result.contents().get(0).isEmpty());
        assertTrue(result.contents().get(8).isEmpty());
        var mismatch = ShulkerTransferPlanner.planExactInsertion(host, namedStone("different"), 26, false);
        assertEquals(0, mismatch.moved());
    }

    @Test void exactPrimaryMovesAllSecondaryMovesOneAndNestingFailsClosed() {
        ItemStack host = shulker();
        var primary = ShulkerTransferPlanner.planExactInsertion(host, stack(Items.DIAMOND, 7), 9, false);
        assertEquals(7, primary.moved());
        var secondary = ShulkerTransferPlanner.planExactInsertion(host, stack(Items.DIAMOND, 7), 9, true);
        assertEquals(1, secondary.moved());
        assertEquals(6, secondary.remainder().getCount());
        assertEquals(0, ShulkerTransferPlanner.planInsertion(host, shulker()).moved());
        ItemStack stackedHost = shulker(); stackedHost.setCount(2);
        assertEquals(0, ShulkerTransferPlanner.planInsertion(stackedHost, new ItemStack(Items.STONE)).moved());
    }

    @Test void extractionUsesFullOrCeilingHalfHonorsCapacityAndPreservesReservations() {
        ItemStack host = shulker();
        var contents = empty(); contents.set(18, stack(Items.STONE, 5));
        ShulkerContents.replace(host, contents);
        ReservationStore.setData(host, ReservationData.EMPTY.with(18, new ItemStack(Items.DIRT)));
        var half = ShulkerTransferPlanner.planExtraction(host, 18, true, 64);
        assertEquals(3, half.moved());
        assertEquals(2, half.contents().get(18).getCount());
        assertTrue(ReservationStore.getData(half.shulker()).matches(18, new ItemStack(Items.DIRT)));
        var capped = ShulkerTransferPlanner.planExtraction(host, 18, false, 2);
        assertEquals(2, capped.moved());
        assertEquals(3, capped.contents().get(18).getCount());
    }

    @Test void occupiedTraversalWrapsAndSkipsEveryEmptyCell() {
        var contents = empty();
        contents.set(0, new ItemStack(Items.STONE)); contents.set(17, new ItemStack(Items.DIRT));
        assertEquals(0, ShulkerContents.firstOccupied(contents));
        assertEquals(17, ShulkerContents.nextOccupied(contents, 0));
        assertEquals(0, ShulkerContents.nextOccupied(contents, 17));
        assertEquals(17, ShulkerContents.previousOccupied(contents, 0));
        assertEquals(-1, ShulkerContents.firstOccupied(empty()));
    }

    @Test void liveEffectiveMaximumAllowsAHighCountNormallyNonstackableItem() {
        ItemStack saddle = new ItemStack(Items.SADDLE);
        saddle.set(DataComponents.MAX_STACK_SIZE, 16);
        saddle.setCount(9);
        var result = ShulkerTransferPlanner.planExactInsertion(shulker(), saddle, 0, false);
        assertEquals(9, result.moved());
        assertEquals(9, result.contents().get(0).getCount());
        assertEquals(16, result.contents().get(0).getMaxStackSize());
    }

    private static ItemStack shulker() { return new ItemStack(Blocks.SHULKER_BOX); }
    private static NonNullList<ItemStack> empty() { return NonNullList.withSize(27, ItemStack.EMPTY); }
    private static ItemStack namedStone(String name) {
        ItemStack stack = stack(Items.STONE, 1);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }
    private static ItemStack stack(Item item, int count) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.MAX_STACK_SIZE, 64);
        stack.setCount(count);
        return stack;
    }
    private static void bind(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }
    private static void initializeComponentsAfterBootstrap() throws Exception {
        var registry = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE;
        var frozen = net.minecraft.core.MappedRegistry.class.getDeclaredField("frozen");
        frozen.setAccessible(true);
        boolean wasFrozen = frozen.getBoolean(registry);
        frozen.setBoolean(registry, false);
        try { ModComponents.initialize(); } finally { frozen.setBoolean(registry, wasFrozen); }
    }
}
