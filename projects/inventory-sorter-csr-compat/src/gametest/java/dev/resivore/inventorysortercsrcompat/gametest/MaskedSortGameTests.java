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
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Controlled server-side coverage for the exact Inventory Sorter 3.0.0 seams. */
public final class MaskedSortGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void noReservationLayoutMatchesUnmodifiedUpstreamLayout(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        barrel.setItem(0, new ItemStack(Items.DIRT, 2));
        barrel.setItem(1, new ItemStack(Items.COBBLESTONE, 3));
        barrel.setItem(2, new ItemStack(Items.DIRT, 6));
        List<ItemStack> expected = ordinaryLayout(ContainerStacks.get(barrel, 0, 9));

        sort(barrel, false);
        assertLayout(helper, barrel, expected, "No-reservation layout diverged from upstream at ");
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

        sort(barrel, false);
        helper.assertTrue(ItemStack.matches(occupiedBefore, barrel.getItem(1))
                        && barrel.getItem(4).isEmpty()
                        && ContainerSlotReservationsApi.isReserved(barrel, 1)
                        && ContainerSlotReservationsApi.isReserved(barrel, 4),
                "Reserved physical slots changed or accepted an upstream merge");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void unreservedPortableContainersSortAsOuterStacksAndKeepAllComponents(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack shulker = filledNamedShulker("movable shulker");
        ItemStack bundle = filledNamedBundle("movable bundle");
        barrel.setItem(0, shulker);
        barrel.setItem(1, bundle);
        barrel.setItem(2, new ItemStack(Items.DIRT, 7));
        barrel.setItem(3, new ItemStack(Items.COBBLESTONE, 4));
        ItemStack shulkerBefore = shulker.copy();
        ItemStack bundleBefore = bundle.copy();
        List<ItemStack> expected = ordinaryLayout(ContainerStacks.get(barrel, 0, 9));

        sort(barrel, false);
        assertLayout(helper, barrel, expected, "Unreserved portable layout diverged from upstream at ");
        int shulkerTarget = indexOfExact(expected, shulkerBefore);
        int bundleTarget = indexOfExact(expected, bundleBefore);
        helper.assertTrue(shulkerTarget != 0 && bundleTarget != 1
                        && ItemStack.matches(shulkerBefore, barrel.getItem(shulkerTarget))
                        && ItemStack.matches(bundleBefore, barrel.getItem(bundleTarget)),
                "Unreserved shulker or bundle did not move as a complete outer ItemStack");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void reservedPortableContainersRemainExactAtTheirPhysicalSlots(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack bundle = filledNamedBundle("reserved bundle");
        ItemStack shulker = filledNamedShulker("reserved shulker");
        barrel.setItem(1, bundle);
        barrel.setItem(5, shulker);
        // C16 reservation templates intentionally reject filled carriers.  Reserve these exact
        // physical slots with eligible empty templates, then prove full current stacks are fixed.
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 1).orElseThrow(), new ItemStack(Items.BUNDLE));
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 5).orElseThrow(), new ItemStack(Blocks.SHULKER_BOX));
        barrel.setItem(0, new ItemStack(Items.DIRT, 7));
        barrel.setItem(2, new ItemStack(Items.COBBLESTONE, 4));
        ItemStack bundleBefore = bundle.copy();
        ItemStack shulkerBefore = shulker.copy();

        sort(barrel, false);
        helper.assertTrue(ContainerSlotReservationsApi.isReserved(barrel, 1)
                        && ContainerSlotReservationsApi.isReserved(barrel, 5)
                        && ItemStack.matches(bundleBefore, barrel.getItem(1))
                        && ItemStack.matches(shulkerBefore, barrel.getItem(5)),
                "CSR-reserved portable stack or its complete component data changed during a sort");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void bundleContentInsertionIsDisabledWithoutFreezingTheBundle(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack bundle = filledNamedBundle("no sorter insertion");
        ItemStack bundleBefore = bundle.copy();
        barrel.setItem(2, new ItemStack(Items.DIRT, 8));
        barrel.setItem(5, bundle);
        List<ItemStack> expected = ordinaryLayout(ContainerStacks.get(barrel, 0, 9));

        // true is the exact upstream flag that invokes BundleInsertionLayoutPass without C2.
        sort(barrel, true);
        assertLayout(helper, barrel, expected, "Bundle-suppressed layout diverged from ordinary upstream at ");
        int bundleTarget = indexOfExact(expected, bundleBefore);
        helper.assertTrue(bundleTarget != 5
                        && ItemStack.matches(bundleBefore, barrel.getItem(bundleTarget))
                        && containsOuterStack(barrel, Items.DIRT, 8),
                "Sorting inserted a loose item into a bundle or froze the bundle in place");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void distributedReservationsMaskOnlyTheirPhysicalSlots(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack reserved = new ItemStack(Items.STONE);
        barrel.setItem(0, new ItemStack(Items.DIRT, 1));
        barrel.setItem(1, reserved);
        barrel.setItem(2, new ItemStack(Items.COBBLESTONE, 1));
        barrel.setItem(3, filledNamedBundle("distributed movable bundle"));
        barrel.setItem(4, new ItemStack(Items.OAK_PLANKS, 1));
        barrel.setItem(5, filledNamedShulker("distributed movable shulker"));
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 1).orElseThrow(), reserved);
        ItemStack reservedBefore = reserved.copy();
        List<Integer> movableSlots = List.of(0, 2, 3, 4, 5, 6, 7, 8);
        List<ItemStack> movableBefore = new ArrayList<>();
        for (int slot : movableSlots) movableBefore.add(barrel.getItem(slot));
        List<ItemStack> expectedMovable = ordinaryLayout(movableBefore);

        sort(barrel, false);
        helper.assertTrue(ItemStack.matches(reservedBefore, barrel.getItem(1)),
                "A distributed CSR reservation was used as workspace");
        for (int index = 0; index < movableSlots.size(); index++) {
            int slot = movableSlots.get(index);
            helper.assertTrue(ItemStack.matches(expectedMovable.get(index), barrel.getItem(slot)),
                    "Only CSR-reserved slots should be masked; mismatch at physical slot " + slot);
        }
        helper.succeed();
    }

    private static ItemStack filledNamedShulker(String name) {
        ItemStack stack = new ItemStack(Blocks.SHULKER_BOX);
        stack.set(DataComponents.CONTAINER,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 2))));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static ItemStack filledNamedBundle(String name) {
        BundleContents.Mutable contents = new BundleContents.Mutable(BundleContents.EMPTY);
        contents.tryInsert(new ItemStack(Items.EMERALD, 3));
        ItemStack stack = new ItemStack(Items.BUNDLE);
        stack.set(DataComponents.BUNDLE_CONTENTS, contents.toImmutable());
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static List<ItemStack> ordinaryLayout(List<ItemStack> stacks) {
        return SortedInventoryLayout.from(stacks, SortType.NAME, "en_us", List.of(), false).stacks();
    }

    private static void assertLayout(GameTestHelper helper, BarrelBlockEntity barrel, List<ItemStack> expected, String message) {
        for (int slot = 0; slot < expected.size(); slot++) {
            int physicalSlot = slot;
            helper.assertTrue(ItemStack.matches(expected.get(slot), barrel.getItem(slot)), message + physicalSlot);
        }
    }

    private static int indexOfExact(List<ItemStack> stacks, ItemStack expected) {
        for (int index = 0; index < stacks.size(); index++) {
            if (ItemStack.matches(expected, stacks.get(index))) return index;
        }
        throw new AssertionError("Expected stack is absent from the upstream ordinary layout");
    }

    private static boolean containsOuterStack(BarrelBlockEntity barrel, net.minecraft.world.item.Item item, int count) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = barrel.getItem(slot);
            if (stack.is(item) && stack.getCount() == count) return true;
        }
        return false;
    }

    private static void sort(BarrelBlockEntity barrel, boolean sortIntoBundles) {
        ContainerInventorySorter.sort(barrel, 0, 9, SortType.NAME, "en_us", List.of(), sortIntoBundles);
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
