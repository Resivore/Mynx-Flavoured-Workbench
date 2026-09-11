package dev.resivore.quickstacknearbycompat.gametest;

import dev.resivore.slotreservations.ReservationStore;
import dev.resivore.slotreservations.SupportedContainerResolver;
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

    private static ChestBlockEntity chest(GameTestHelper helper) {
        BlockPos position = new BlockPos(1, 2, 1);
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
