package tempeststudios.quickstacknearby.gametest;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;
import tempeststudios.quickstacknearby.QuickStackService;

import java.lang.reflect.Method;

/** Representative real block-entity checks.  They are controlled evidence, not gameplay evidence. */
public final class C10GameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void ordinaryChestQuickStackUsesNativePhysicalMerge(GameTestHelper helper) {
        ChestBlockEntity chest = chest(helper, new BlockPos(1, 2, 1));
        ServerPlayer player = player(helper);
        chest.setItem(0, new ItemStack(Items.STONE, 60));
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 12));
        QuickStackMoveEngine.Result result = QuickStackService.quickStack(player);
        helper.assertTrue(result.itemsMoved() == 12 && chest.getItem(0).getCount() == 64
                && chest.getItem(1).getCount() == 8 && player.getInventory().getItem(9).isEmpty(),
                "native physical merge and remainder placement changed");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void nestedShulkerRoutesOneLevelAndPreservesHostContents(GameTestHelper helper) {
        ChestBlockEntity chest = chest(helper, new BlockPos(1, 2, 1));
        ServerPlayer player = player(helper);
        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        contents.set(0, new ItemStack(Items.STONE, 60));
        ItemStack host = new ItemStack(Blocks.SHULKER_BOX);
        host.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        chest.setItem(0, host);
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 12));
        QuickStackMoveEngine.Result result = QuickStackService.quickStack(player);
        NonNullList<ItemStack> after = NonNullList.withSize(27, ItemStack.EMPTY);
        host.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(after);
        helper.assertTrue(result.itemsMoved() == 12 && after.get(0).getCount() == 64 && after.get(1).getCount() == 8
                && chest.getItem(0) == host && player.getInventory().getItem(9).isEmpty(),
                "one-level shulker routing lost its transactional host writeback");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void childOnlyPhysicalAffinityAdmitsTheOuterScanButNotTheParent(GameTestHelper helper) {
        ChestBlockEntity chest = chest(helper, new BlockPos(1, 2, 1));
        ServerPlayer player = player(helper);
        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        contents.set(4, new ItemStack(Items.STONE, 60));
        contents.set(9, new ItemStack(Items.DIRT, 3));
        ItemStack host = new ItemStack(Blocks.SHULKER_BOX);
        host.set(DataComponents.CUSTOM_NAME, Component.literal("metadata must survive"));
        host.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        chest.setItem(0, host);
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 12));

        QuickStackMoveEngine.Result result = QuickStackService.quickStack(player);
        NonNullList<ItemStack> after = NonNullList.withSize(27, ItemStack.EMPTY);
        host.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(after);
        helper.assertTrue(result.itemsMoved() == 12 && chest.getItem(0) == host && chest.getItem(1).isEmpty()
                        && after.get(4).getCount() == 64 && after.get(0).getCount() == 8
                        && after.get(9).is(Items.DIRT) && host.getHoverName().getString().equals("metadata must survive"),
                "child-only physical affinity either failed admission, wrote into the parent, or lost host metadata");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void parentAffinityRemainsBeforeNestedAffinity(GameTestHelper helper) {
        ChestBlockEntity chest = chest(helper, new BlockPos(1, 2, 1));
        ServerPlayer player = player(helper);
        chest.setItem(0, new ItemStack(Items.STONE, 60));
        NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
        contents.set(0, new ItemStack(Items.STONE, 60));
        ItemStack host = new ItemStack(Blocks.SHULKER_BOX);
        host.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        chest.setItem(2, host);
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 10));

        QuickStackMoveEngine.Result result = QuickStackService.quickStack(player);
        NonNullList<ItemStack> after = NonNullList.withSize(27, ItemStack.EMPTY);
        host.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(after);
        helper.assertTrue(result.itemsMoved() == 10 && chest.getItem(0).getCount() == 64
                        && chest.getItem(1).getCount() == 6 && after.get(0).getCount() == 60,
                "nested target was allowed ahead of a matching physical parent target");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void emptyChestDoesNotGainNativeAffinity(GameTestHelper helper) {
        ChestBlockEntity chest = chest(helper, new BlockPos(1, 2, 1));
        ServerPlayer player = player(helper);
        player.getInventory().setItem(9, new ItemStack(Items.DIRT, 8));
        QuickStackMoveEngine.Result result = QuickStackService.quickStack(player);
        helper.assertTrue(result.itemsMoved() == 0 && chest.isEmpty() && player.getInventory().getItem(9).getCount() == 8,
                "unreserved empty container was incorrectly admitted");
        helper.succeed();
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        return helper.getBlockEntity(pos, ChestBlockEntity.class);
    }
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 1));
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        return player;
    }
    @Override public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException { method.invoke(this, helper); }
}
