package dev.resivore.quickstacknearbycompat.gametest;

import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.SupportedContainerResolver;
import dev.resivore.slotreservations.api.ContainerSlotReservationsApi;
import dev.resivore.slotreservations.api.ReservationSlotClass;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;
import tempeststudios.quickstacknearby.QuickStackService;

import java.lang.reflect.Method;
import java.util.Map;

/** Real QSN-action regressions for C17's action-start populated-shulker source admission. */
public final class PopulatedShulkerSourceGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void identicalPopulatedShulkersDrainInternalsWithoutMovingOuterCarrier(GameTestHelper helper) {
        ChestBlockEntity chest = chest(helper);
        ServerPlayer player = player(helper);
        ItemStack destination = stackableShulker(32);
        ItemStack emptyMatch = stackableShulker(0);
        ItemStack source = stackableShulker(32);
        chest.setItem(0, destination);
        chest.setItem(1, emptyMatch);
        player.getInventory().setItem(9, source);

        QuickStackMoveEngine.Result first = QuickStackService.quickStack(player);

        helper.assertTrue(first.itemsMoved() == 32 && chest.getItem(0) == destination
                        && chest.getItem(1) == emptyMatch && chest.getItem(1).getCount() == 1
                        && player.getInventory().getItem(9) == source && source.getCount() == 1
                        && planks(destination) == 64 && planks(source) == 0,
                "C17 did not retain the populated outer source while routing exactly 32 internal planks");

        QuickStackMoveEngine.Result second = QuickStackService.quickStack(player);
        helper.assertTrue(second.itemsMoved() == 1 && player.getInventory().getItem(9).isEmpty()
                        && chest.getItem(1).getCount() == 2 && planks(destination) == 64,
                "An emptied carrier was not eligible again on the next QSN action");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void emptyMatchingShulkersRetainNativeLooseStacking(GameTestHelper helper) {
        ChestBlockEntity chest = chest(helper);
        ServerPlayer player = player(helper);
        ItemStack destination = stackableShulker(0);
        chest.setItem(0, destination);
        player.getInventory().setItem(9, stackableShulker(0));

        QuickStackMoveEngine.Result result = QuickStackService.quickStack(player);
        helper.assertTrue(result.itemsMoved() == 1 && player.getInventory().getItem(9).isEmpty()
                        && chest.getItem(0).getCount() == 2,
                "Physically empty shulkers no longer followed QSN's normal exact stack rules");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void partialInternalDrainRetainsExactCarrierRemainder(GameTestHelper helper) {
        ChestBlockEntity chest = chest(helper);
        ServerPlayer player = player(helper);
        ItemStack destination = stackableShulker(60);
        ItemStack source = stackableShulker(32);
        fillRemainingSlots(destination);
        chest.setItem(0, destination);
        player.getInventory().setItem(9, source);

        QuickStackMoveEngine.Result result = QuickStackService.quickStack(player);
        helper.assertTrue(result.itemsMoved() == 4 && player.getInventory().getItem(9) == source
                        && planks(destination) == 64 && planks(source) == 28,
                "Partial carried-shulker routing moved the outer carrier or changed an exact remainder");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void userLockedCarrierDoesNotDrainItsInternals(GameTestHelper helper) {
        ChestBlockEntity chest = chest(helper);
        ServerPlayer player = player(helper);
        ItemStack destination = stackableShulker(32);
        ItemStack source = stackableShulker(32);
        chest.setItem(0, destination);
        player.getInventory().setItem(9, source);

        QuickStackMoveEngine.Result result = QuickStackService.quickStack(player,
                new QuickStackMoveEngine.SourceRules(Map.of(9, new QuickStackMoveEngine.SlotRule(true, 0))));
        helper.assertTrue(result.itemsMoved() == 0 && player.getInventory().getItem(9) == source
                        && planks(source) == 32 && planks(destination) == 32,
                "The transient outer protection was incorrectly treated differently from a real user lock");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void populatedOuterShulkerCannotSeedCsrReservationButInternalsCan(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BarrelBlockEntity barrel = barrel(helper);
        ItemStack source = stackableShulker(3);
        player.getInventory().setItem(9, source);

        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 0).orElseThrow(), source.copy());
        QuickStackMoveEngine.Result outer = QuickStackService.quickStack(player);
        helper.assertTrue(outer.itemsMoved() == 0 && barrel.getItem(0).isEmpty()
                        && player.getInventory().getItem(9) == source && planks(source) == 3,
                "A populated outer shulker seeded or moved into a CSR reservation");

        ReservationStore.set(SupportedContainerResolver.resolve(barrel, 1).orElseThrow(), new ItemStack(Items.OAK_PLANKS));
        QuickStackMoveEngine.Result inner = QuickStackService.quickStack(player);
        helper.assertTrue(inner.itemsMoved() == 3 && barrel.getItem(0).isEmpty()
                        && barrel.getItem(1).is(Items.OAK_PLANKS) && barrel.getItem(1).getCount() == 3
                        && player.getInventory().getItem(9) == source && planks(source) == 0,
                "Eligible internal contents did not retain CSR affinity while outer admission was blocked");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void c18SpecificHomeReturnsChangedCarrierBeforeAnyContentDrain(GameTestHelper helper) {
        requireSpecificCsrC17(helper);
        BarrelBlockEntity home = barrel(helper);
        ChestBlockEntity ordinaryContentsTarget = chest(helper, new BlockPos(2, 2, 1));
        ServerPlayer player = player(helper);
        ItemStack original = stackableShulker(2);
        original.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("before reservation"));
        home.setItem(4, original);
        reserveSpecificOccupied(home, 4, original);

        ItemStack carrier = home.removeItemNoUpdate(4);
        setContents(carrier, new ItemStack(Items.STONE, 7), new ItemStack(Items.DIAMOND, 3));
        carrier.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("changed after reservation"));
        ItemStack expected = carrier.copy();
        ordinaryContentsTarget.setItem(0, new ItemStack(Items.STONE, 32));
        player.getInventory().setItem(9, carrier);

        helper.assertTrue(QuickStackMoveEngine.acceptedTypes(home).isEmpty()
                        && ContainerSlotReservationsApi.classify(home, 4, carrier)
                        == ReservationSlotClass.RESERVED_MATCH,
                "The C18 fixture did not use an empty CSR C17 specific-reservation-only target");
        QuickStackMoveEngine.Result result = QuickStackService.quickStack(player);

        helper.assertTrue(result.itemsMoved() == 1
                        && result.sourceStacksTouched() == 1
                        && result.targetContainersTouched() == 1
                        && player.getInventory().getItem(9).isEmpty()
                        && ItemStack.matches(expected, home.getItem(4))
                        && ordinaryContentsTarget.getItem(0).getCount() == 32,
                "C18 did not return the changed specific carrier intact before its contents could drain");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void c18LookalikeAndUserLockedCarrierCannotUseSpecificHome(GameTestHelper helper) {
        requireSpecificCsrC17(helper);
        BarrelBlockEntity home = barrel(helper);
        ServerPlayer player = player(helper);
        ItemStack reserved = stackableShulker(5);
        reserved.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("same visible shulker"));
        home.setItem(3, reserved);
        reserveSpecificOccupied(home, 3, reserved);
        ItemStack identified = home.removeItemNoUpdate(3);
        ItemStack lookalike = stackableShulker(5);
        lookalike.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("same visible shulker"));
        player.getInventory().setItem(9, lookalike);

        helper.assertTrue(ContainerSlotReservationsApi.classify(home, 3, lookalike)
                        == ReservationSlotClass.RESERVED_OTHER,
                "The C18 negative fixture unexpectedly matched CSR's specific home");
        QuickStackMoveEngine.Result lookalikeResult = QuickStackService.quickStack(player);
        helper.assertTrue(lookalikeResult.itemsMoved() == 0 && home.getItem(3).isEmpty()
                        && player.getInventory().getItem(9) == lookalike,
                "A CSR-specific lookalike entered the reserved home");

        player.getInventory().setItem(9, identified);
        QuickStackMoveEngine.Result lockedResult = QuickStackService.quickStack(player,
                new QuickStackMoveEngine.SourceRules(Map.of(9, new QuickStackMoveEngine.SlotRule(true, 0))));
        helper.assertTrue(lockedResult.itemsMoved() == 0 && home.getItem(3).isEmpty()
                        && player.getInventory().getItem(9) == identified,
                "C18 ignored a user source lock for an otherwise matching carrier");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void c18EmptyIdentifiedShulkerStillUsesNormalLooseCsrPath(GameTestHelper helper) {
        requireSpecificCsrC17(helper);
        BarrelBlockEntity home = barrel(helper);
        ServerPlayer player = player(helper);
        ItemStack reserved = stackableShulker(1);
        home.setItem(2, reserved);
        reserveSpecificOccupied(home, 2, reserved);
        ItemStack emptied = home.removeItemNoUpdate(2);
        emptied.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        ItemStack expected = emptied.copy();
        player.getInventory().setItem(9, emptied);

        helper.assertTrue(!isPhysicallyPopulated(emptied)
                        && ContainerSlotReservationsApi.classify(home, 2, emptied)
                        == ReservationSlotClass.RESERVED_MATCH,
                "The empty identified-shulker regression fixture is not a normal CSR match");
        QuickStackMoveEngine.Result result = QuickStackService.quickStack(player);
        helper.assertTrue(result.itemsMoved() == 1 && player.getInventory().getItem(9).isEmpty()
                        && home.getItem(2).getCount() == 1
                        && ItemStack.matches(expected, home.getItem(2)),
                "An empty identity-bearing shulker no longer returned through normal QSN/CSR routing");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void c18HomeAndLooseMoveUseConservedUniqueResultAccounting(GameTestHelper helper) {
        requireSpecificCsrC17(helper);
        BarrelBlockEntity home = barrel(helper);
        ChestBlockEntity looseTarget = chest(helper, new BlockPos(2, 2, 1));
        ServerPlayer player = player(helper);
        ItemStack reserved = stackableShulker(4);
        home.setItem(1, reserved);
        reserveSpecificOccupied(home, 1, reserved);
        ItemStack carrier = home.removeItemNoUpdate(1);
        looseTarget.setItem(0, new ItemStack(Items.COBBLESTONE, 50));
        player.getInventory().setItem(9, carrier);
        player.getInventory().setItem(10, new ItemStack(Items.COBBLESTONE, 20));

        QuickStackMoveEngine.Result result = QuickStackService.quickStack(player);
        helper.assertTrue(result.itemsMoved() == 21 && result.sourceStacksTouched() == 2
                        && result.targetContainersTouched() == 2 && home.getItem(1).getCount() == 1
                        && looseTarget.getItem(0).getCount() == 64
                        && looseTarget.getItem(1).getCount() == 6,
                "C18 did not aggregate returned carriers and ordinary loose movement like QSN results");
        helper.succeed();
    }

    private static ChestBlockEntity chest(GameTestHelper helper) {
        return chest(helper, new BlockPos(1, 2, 1));
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos position) {
        helper.setBlock(position, Blocks.CHEST);
        return helper.getBlockEntity(position, ChestBlockEntity.class);
    }

    private static BarrelBlockEntity barrel(GameTestHelper helper) {
        BlockPos position = new BlockPos(1, 2, 1);
        helper.setBlock(position, Blocks.BARREL);
        return helper.getBlockEntity(position, BarrelBlockEntity.class);
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos position = helper.absolutePos(new BlockPos(0, 2, 1));
        player.setPos(position.getX() + .5, position.getY(), position.getZ() + .5);
        return player;
    }

    private static ItemStack stackableShulker(int planks) {
        ItemStack shulker = new ItemStack(Blocks.SHULKER_BOX);
        shulker.set(DataComponents.MAX_STACK_SIZE, 64);
        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        if (planks > 0) contents.set(0, new ItemStack(Items.OAK_PLANKS, planks));
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        return shulker;
    }

    private static int planks(ItemStack shulker) {
        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        shulker.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
        return contents.stream().filter(stack -> stack.is(Items.OAK_PLANKS)).mapToInt(ItemStack::getCount).sum();
    }

    private static boolean isPhysicallyPopulated(ItemStack shulker) {
        return planks(shulker) > 0 || shulker.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyItemCopyStream().findAny().isPresent();
    }

    private static void setContents(ItemStack shulker, ItemStack... contents) {
        NonNullList<ItemStack> physical = NonNullList.withSize(27, ItemStack.EMPTY);
        for (int index = 0; index < contents.length; index++) {
            physical.set(index, contents[index]);
        }
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(physical));
    }

    /**
     * Test setup only: exercise CSR C17's real occupied-slot transition without allowing QSN
     * production code to reference its portable-container implementation or data component.
     */
    private static void reserveSpecificOccupied(BarrelBlockEntity owner, int slot, ItemStack physical) {
        try {
            Class<?> dataType = Class.forName("dev.resivore.slotreservations.ReservationData");
            Class<?> transitionType = Class.forName("dev.resivore.slotreservations.ReservationTransition");
            Class<?> storeType = Class.forName("dev.resivore.slotreservations.ReservationStore");
            Object current = storeType.getMethod("getData", net.minecraft.world.level.block.entity.BlockEntity.class)
                    .invoke(null, owner);
            Method transition = transitionType.getDeclaredMethod("fromOccupied", dataType, int.class, ItemStack.class);
            transition.setAccessible(true);
            Object result = transition.invoke(null, current, slot, physical);
            Method data = result.getClass().getDeclaredMethod("data");
            Method attachIdentity = result.getClass().getDeclaredMethod("attachIdentity", ItemStack.class);
            data.setAccessible(true);
            attachIdentity.setAccessible(true);
            storeType.getMethod("setOwnerData", net.minecraft.world.Container.class, dataType)
                    .invoke(null, owner, data.invoke(result));
            attachIdentity.invoke(result, physical);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("CSR C17 specific-reservation fixture is unavailable", failure);
        }
    }

    private static void requireSpecificCsrC17(GameTestHelper helper) {
        try {
            Class<?> transition = Class.forName("dev.resivore.slotreservations.ReservationTransition");
            helper.assertTrue(transition.getDeclaredMethod("fromOccupied", Class.forName(
                            "dev.resivore.slotreservations.ReservationData"), int.class, ItemStack.class) != null,
                    "CSR C17 specific portable-container transition is required for C18 integration coverage");
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("CSR C17 specific portable-container transition is required", failure);
        }
    }

    private static void fillRemainingSlots(ItemStack shulker) {
        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        shulker.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
        for (int slot = 1; slot < contents.size(); slot++) contents.set(slot, new ItemStack(Items.DIRT, 64));
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
