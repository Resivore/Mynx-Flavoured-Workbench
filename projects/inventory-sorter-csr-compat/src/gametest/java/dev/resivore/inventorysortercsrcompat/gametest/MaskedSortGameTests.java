package dev.resivore.inventorysortercsrcompat.gametest;

import dev.resivore.slotreservations.ModComponents;
import dev.resivore.slotreservations.PortableContainerIdentity;
import dev.resivore.slotreservations.ReservationData;
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
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    public void emptyAndPartialOrdinaryReservationsFillBeforeMaskedSorting(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack occupiedReservation = new ItemStack(Items.COBBLESTONE, 32);
        barrel.setItem(1, occupiedReservation);
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 1).orElseThrow(), occupiedReservation);
        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 4).orElseThrow(), new ItemStack(Items.DIRT));
        barrel.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        barrel.setItem(2, new ItemStack(Items.DIRT, 4));
        barrel.setItem(6, new ItemStack(Items.DIRT, 7));

        sort(barrel, false);
        helper.assertTrue(barrel.getItem(1).is(Items.COBBLESTONE)
                        && barrel.getItem(1).getCount() == 64
                        && barrel.getItem(4).is(Items.DIRT)
                        && barrel.getItem(4).getCount() == 11
                        && containsOuterStack(barrel, Items.COBBLESTONE, 32)
                        && ContainerSlotReservationsApi.isReserved(barrel, 1)
                        && ContainerSlotReservationsApi.isReserved(barrel, 4),
                "Matching empty/partial reservations did not fill before the masked sort");
        assertTotal(helper, barrel, Items.COBBLESTONE, 96);
        assertTotal(helper, barrel, Items.DIRT, 11);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void nonmatchingCandidateCannotEnterReservedWorkspace(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        reserve(barrel, 2, new ItemStack(Items.COBBLESTONE));
        barrel.setItem(0, new ItemStack(Items.DIRT, 8));
        barrel.setItem(5, new ItemStack(Items.OAK_PLANKS, 3));

        sort(barrel, false);

        helper.assertTrue(barrel.getItem(2).isEmpty(),
                "A nonmatching stack entered a CSR-reserved physical slot");
        assertTotal(helper, barrel, Items.DIRT, 8);
        assertTotal(helper, barrel, Items.OAK_PLANKS, 3);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void insufficientAndMultipleDonorsFillWithoutLoss(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        barrel.setItem(1, new ItemStack(Items.COBBLESTONE, 48));
        reserve(barrel, 1, new ItemStack(Items.COBBLESTONE));
        barrel.setItem(3, new ItemStack(Items.COBBLESTONE, 7));
        barrel.setItem(6, new ItemStack(Items.COBBLESTONE, 9));

        sort(barrel, false);

        helper.assertTrue(barrel.getItem(1).is(Items.COBBLESTONE)
                        && barrel.getItem(1).getCount() == 64,
                "Several donors did not deterministically top up the reservation");
        assertTotal(helper, barrel, Items.COBBLESTONE, 64);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void insufficientSingleDonorTransfersEverythingAvailable(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        barrel.setItem(3, new ItemStack(Items.COBBLESTONE, 50));
        reserve(barrel, 3, new ItemStack(Items.COBBLESTONE));
        barrel.setItem(7, new ItemStack(Items.COBBLESTONE, 10));

        sort(barrel, false);

        helper.assertTrue(barrel.getItem(3).getCount() == 60,
                "An insufficient matching donor was not transferred completely");
        assertTotal(helper, barrel, Items.COBBLESTONE, 60);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void equivalentReservationsFillInPhysicalOrder(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        reserve(barrel, 1, new ItemStack(Items.COBBLESTONE));
        reserve(barrel, 5, new ItemStack(Items.COBBLESTONE));
        barrel.setItem(8, new ItemStack(Items.COBBLESTONE, 64));

        sort(barrel, false);

        helper.assertTrue(barrel.getItem(1).getCount() == 64 && barrel.getItem(5).isEmpty(),
                "Equivalent reservations were not filled in physical slot order");
        assertTotal(helper, barrel, Items.COBBLESTONE, 64);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void excessRemainderUsesInventorySortersOrdinaryMovableLayout(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        barrel.setItem(4, new ItemStack(Items.COBBLESTONE, 32));
        reserve(barrel, 4, new ItemStack(Items.COBBLESTONE));
        barrel.setItem(7, new ItemStack(Items.COBBLESTONE, 64));
        barrel.setItem(0, new ItemStack(Items.DIRT, 5));

        List<Integer> movableSlots = List.of(0, 1, 2, 3, 5, 6, 7, 8);
        List<ItemStack> postFillMovable = new ArrayList<>();
        postFillMovable.add(new ItemStack(Items.DIRT, 5));
        postFillMovable.add(ItemStack.EMPTY);
        postFillMovable.add(ItemStack.EMPTY);
        postFillMovable.add(ItemStack.EMPTY);
        postFillMovable.add(ItemStack.EMPTY);
        postFillMovable.add(ItemStack.EMPTY);
        postFillMovable.add(new ItemStack(Items.COBBLESTONE, 32));
        postFillMovable.add(ItemStack.EMPTY);
        List<ItemStack> expected = ordinaryLayout(postFillMovable);

        sort(barrel, false);

        helper.assertTrue(barrel.getItem(4).getCount() == 64,
                "The matching partial reservation was not topped up");
        for (int index = 0; index < movableSlots.size(); index++) {
            helper.assertTrue(ItemStack.matches(expected.get(index), barrel.getItem(movableSlots.get(index))),
                    "Excess remainder diverged from Inventory Sorter's layout at movable index " + index);
        }
        assertTotal(helper, barrel, Items.COBBLESTONE, 96);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void wrongFullAndReservedDonorStacksRemainAnchored(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack reservedDonor = named(new ItemStack(Items.COBBLESTONE, 20), "reserved donor");
        reserve(barrel, 0, reservedDonor);
        barrel.setItem(2, reservedDonor);
        reserve(barrel, 2, reservedDonor);
        ItemStack wrong = named(new ItemStack(Items.DIRT, 13), "wrong occupant");
        barrel.setItem(4, wrong);
        reserve(barrel, 4, new ItemStack(Items.COBBLESTONE));
        ItemStack full = named(new ItemStack(Items.COBBLESTONE, 64), "full reservation");
        barrel.setItem(6, full);
        reserve(barrel, 6, full);
        ItemStack reservedDonorBefore = reservedDonor.copy();
        ItemStack wrongBefore = wrong.copy();
        ItemStack fullBefore = full.copy();

        sort(barrel, false);

        helper.assertTrue(barrel.getItem(0).isEmpty()
                        && ItemStack.matches(reservedDonorBefore, barrel.getItem(2))
                        && ItemStack.matches(wrongBefore, barrel.getItem(4))
                        && ItemStack.matches(fullBefore, barrel.getItem(6)),
                "A reserved donor, wrong occupant, or full reservation was mutated");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void componentSensitiveReservationRejectsIncompatibleMerge(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack target = named(new ItemStack(Items.COBBLESTONE, 32), "alpha");
        ItemStack other = named(new ItemStack(Items.COBBLESTONE, 16), "beta");
        barrel.setItem(1, target);
        reserve(barrel, 1, target);
        barrel.setItem(7, other);
        ItemStack targetBefore = target.copy();
        ItemStack otherBefore = other.copy();

        sort(barrel, false);

        helper.assertTrue(ItemStack.matches(targetBefore, barrel.getItem(1))
                        && containsExact(barrel, otherBefore),
                "Component-incompatible stacks merged into a reservation");
        assertTotal(helper, barrel, Items.COBBLESTONE, 48);
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
        // Generic empty portable reservations reject filled carriers. Reserve these physical
        // slots with eligible empty templates, then prove the wrong current occupants stay fixed.
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
    public void ordinaryEmptyPortableOuterStacksFillMatchingReservations(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack shulker = emptyNamedShulker("generic reserved shulker");
        ItemStack bundle = emptyNamedBundle("generic reserved bundle");
        reserve(barrel, 1, shulker);
        reserve(barrel, 4, bundle);
        barrel.setItem(7, shulker.copy());
        barrel.setItem(8, bundle.copy());
        ItemStack shulkerBefore = shulker.copy();
        ItemStack bundleBefore = bundle.copy();

        sort(barrel, true);

        helper.assertTrue(ItemStack.matches(shulkerBefore, barrel.getItem(1))
                        && ItemStack.matches(bundleBefore, barrel.getItem(4)),
                "Matching generic portable outer stacks did not fill their reservations exactly");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void specificPortableIdentityMovesOnlyTheExactOuterStacks(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack shulker = filledNamedShulker("specific shulker");
        UUID shulkerIdentity = PortableContainerIdentity.assign(shulker);
        ItemStack otherShulker = filledNamedShulker("specific shulker");
        PortableContainerIdentity.assign(otherShulker);
        ItemStack bundle = filledNamedBundle("specific bundle");
        UUID bundleIdentity = PortableContainerIdentity.assign(bundle);
        ItemStack otherBundle = filledNamedBundle("specific bundle");
        PortableContainerIdentity.assign(otherBundle);

        ReservationStore.setOwnerData(barrel, ReservationData.EMPTY
                .withSpecific(1, shulker, shulkerIdentity)
                .withSpecific(4, bundle, bundleIdentity));
        barrel.setItem(0, otherShulker);
        barrel.setItem(2, otherBundle);
        barrel.setItem(7, shulker);
        barrel.setItem(8, bundle);
        ItemStack shulkerBefore = shulker.copy();
        ItemStack otherShulkerBefore = otherShulker.copy();
        ItemStack bundleBefore = bundle.copy();
        ItemStack otherBundleBefore = otherBundle.copy();

        sort(barrel, true);

        helper.assertTrue(ItemStack.matches(shulkerBefore, barrel.getItem(1))
                        && ItemStack.matches(bundleBefore, barrel.getItem(4))
                        && containsExact(barrel, otherShulkerBefore)
                        && containsExact(barrel, otherBundleBefore)
                        && shulkerIdentity.equals(barrel.getItem(1).get(ModComponents.PORTABLE_CONTAINER_ID))
                        && bundleIdentity.equals(barrel.getItem(4).get(ModComponents.PORTABLE_CONTAINER_ID)),
                "Specific CSR identity matching admitted a lookalike or changed outer-stack identity/components");
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

        // true is the exact upstream flag that invokes BundleInsertionLayoutPass without C3.
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

    @GameTest(maxTicks = 40)
    public void mixedFillThenMaskedSortKeepsPortableContentsAndCountsExact(GameTestHelper helper) {
        BarrelBlockEntity barrel = barrel(helper);
        reserve(barrel, 2, new ItemStack(Items.COBBLESTONE));
        ItemStack shulker = filledNamedShulker("mixed movable shulker");
        ItemStack bundle = filledNamedBundle("mixed movable bundle");
        ItemStack shulkerBefore = shulker.copy();
        ItemStack bundleBefore = bundle.copy();
        barrel.setItem(0, new ItemStack(Items.DIRT, 5));
        barrel.setItem(4, shulker);
        barrel.setItem(6, bundle);
        barrel.setItem(8, new ItemStack(Items.COBBLESTONE, 40));

        sort(barrel, true);

        helper.assertTrue(barrel.getItem(2).is(Items.COBBLESTONE)
                        && barrel.getItem(2).getCount() == 40
                        && containsExact(barrel, shulkerBefore)
                        && containsExact(barrel, bundleBefore),
                "Mixed reservation fill changed a portable outer stack or failed to fill first");
        assertTotal(helper, barrel, Items.COBBLESTONE, 40);
        assertTotal(helper, barrel, Items.DIRT, 5);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void compoundContainerFillUsesCorrectPhysicalHalfAndLocalSlot(GameTestHelper helper) {
        BlockPos firstPos = new BlockPos(1, 2, 1);
        BlockPos secondPos = new BlockPos(3, 2, 1);
        helper.setBlock(firstPos, Blocks.CHEST);
        helper.setBlock(secondPos, Blocks.CHEST);
        ChestBlockEntity first = helper.getBlockEntity(firstPos, ChestBlockEntity.class);
        ChestBlockEntity second = helper.getBlockEntity(secondPos, ChestBlockEntity.class);
        CompoundContainer combined = new CompoundContainer(first, second);
        reserve(combined, 1, new ItemStack(Items.COBBLESTONE));
        reserve(combined, 29, new ItemStack(Items.DIRT));
        combined.setItem(26, new ItemStack(Items.COBBLESTONE, 12));
        combined.setItem(53, new ItemStack(Items.DIRT, 9));

        ContainerInventorySorter.sort(combined, 0, 54, SortType.NAME, "en_us", List.of(), false);

        helper.assertTrue(first.getItem(1).is(Items.COBBLESTONE)
                        && first.getItem(1).getCount() == 12
                        && second.getItem(2).is(Items.DIRT)
                        && second.getItem(2).getCount() == 9
                        && ContainerSlotReservationsApi.reservationMatches(combined, 1, first.getItem(1))
                        && ContainerSlotReservationsApi.reservationMatches(combined, 29, second.getItem(2)),
                "Compound-container reservation fill targeted the wrong physical half/local slot");
        helper.succeed();
    }

    private static void reserve(Container container, int slot, ItemStack template) {
        ReservationStore.set(SupportedContainerResolver.resolve(container, slot).orElseThrow(), template);
    }

    private static ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static ItemStack emptyNamedShulker(String name) {
        return named(new ItemStack(Blocks.SHULKER_BOX), name);
    }

    private static ItemStack emptyNamedBundle(String name) {
        return named(new ItemStack(Items.BUNDLE), name);
    }

    private static boolean containsExact(Container container, ItemStack expected) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (ItemStack.matches(expected, container.getItem(slot))) return true;
        }
        return false;
    }

    private static void assertTotal(GameTestHelper helper, Container container,
                                    net.minecraft.world.item.Item item, int expected) {
        int actual = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) actual += stack.getCount();
        }
        helper.assertTrue(actual == expected,
                "Item conservation failed for " + item + ": expected " + expected + ", got " + actual);
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
        return barrel(helper, new BlockPos(1, 2, 1));
    }

    private static BarrelBlockEntity barrel(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.BARREL);
        return helper.getBlockEntity(pos, BarrelBlockEntity.class);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
