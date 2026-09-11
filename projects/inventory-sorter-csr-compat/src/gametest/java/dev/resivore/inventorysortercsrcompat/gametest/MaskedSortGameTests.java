package dev.resivore.inventorysortercsrcompat.gametest;

import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.SupportedContainerResolver;
import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.kyrptonaught.inventorysorter.inventory.ContainerInventorySorter;
import net.kyrptonaught.inventorysorter.inventory.container.ContainerStacks;
import net.kyrptonaught.inventorysorter.sort.SortedInventoryLayout;
import net.kyrptonaught.inventorysorter.sort.SortType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

import java.lang.reflect.Method;
import java.util.List;

/** Controlled server-side integration coverage for the exact Inventory Sorter 3.0.0 seam. */
public final class MaskedSortGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void noSpecialSlotsMatchTheUnmodifiedUpstreamLayout(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        barrel.setItem(0, new ItemStack(Items.DIRT, 2));
        barrel.setItem(1, new ItemStack(Items.COBBLESTONE, 3));
        barrel.setItem(2, new ItemStack(Items.DIRT, 6));
        List<ItemStack> expected = SortedInventoryLayout.from(
                ContainerStacks.get(barrel, 0, 9), SortType.NAME, "en_us", List.of(), false).stacks();

        sort(barrel);
        for (int slot = 0; slot < 9; slot++) {
            helper.assertTrue(ItemStack.matches(expected.get(slot), barrel.getItem(slot)),
                    "No-special-slot layout diverged from Inventory Sorter's own result at " + slot);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void emptyAndOccupiedReservationsAreUntouchedAndNeverMerge(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack occupiedReservation = new ItemStack(Items.COBBLESTONE, 32);
        barrel.setItem(1, occupiedReservation);
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 1).orElseThrow(), occupiedReservation);
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 4).orElseThrow(), new ItemStack(Items.DIRT));
        barrel.setItem(0, new ItemStack(Items.COBBLESTONE, 12));
        barrel.setItem(2, new ItemStack(Items.DIRT, 4));
        ItemStack occupiedBefore = occupiedReservation.copy();

        sort(barrel);
        helper.assertTrue(ItemStack.matches(occupiedBefore, barrel.getItem(1))
                        && barrel.getItem(4).isEmpty()
                        && ContainerSlotReservationsApi.isReserved(barrel, 1)
                        && ContainerSlotReservationsApi.isReserved(barrel, 4),
                "Reserved physical slots changed or accepted an upstream merge");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void bundlesAndShulkersKeepTheirExactComponentsAtTheirPhysicalSlots(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.CUSTOM_NAME, Component.literal("fixed bundle"));
        ItemStack shulker = new ItemStack(Blocks.SHULKER_BOX);
        shulker.set(DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 2))));
        shulker.set(DataComponents.CUSTOM_NAME, Component.literal("fixed shulker"));
        barrel.setItem(1, bundle);
        barrel.setItem(5, shulker);
        barrel.setItem(0, new ItemStack(Items.DIRT, 7));
        barrel.setItem(2, new ItemStack(Items.COBBLESTONE, 4));
        ItemStack bundleBefore = bundle.copy();
        ItemStack shulkerBefore = shulker.copy();

        sort(barrel);
        helper.assertTrue(ItemStack.matches(bundleBefore, barrel.getItem(1))
                        && ItemStack.matches(shulkerBefore, barrel.getItem(5)),
                "Portable container stack or its complete component data changed during a sort");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void distributedFixedSlotsLeaveOrdinarySpaceSortable(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        barrel.setItem(0, new ItemStack(Items.DIRT, 1));
        barrel.setItem(2, new ItemStack(Items.COBBLESTONE, 1));
        barrel.setItem(3, new ItemStack(Items.BUNDLE));
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 1).orElseThrow(), new ItemStack(Items.STONE));
        ItemStack bundleBefore = barrel.getItem(3).copy();

        sort(barrel);
        helper.assertTrue(barrel.getItem(1).isEmpty()
                        && ItemStack.matches(bundleBefore, barrel.getItem(3))
                        && !barrel.getItem(0).isEmpty()
                        && !barrel.getItem(2).isEmpty(),
                "Fixed distributed slots were used as workspace or blocked all ordinary sorting");
        helper.succeed();
    }

    private static void sort(BarrelBlockEntity barrel) {
        ContainerInventorySorter.sort(barrel, 0, 9, SortType.NAME, "en_us", List.of(), false);
    }

    private static BarrelBlockEntity barrel(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.BARREL);
        return helper.getBlockEntity(pos, BarrelBlockEntity.class);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
