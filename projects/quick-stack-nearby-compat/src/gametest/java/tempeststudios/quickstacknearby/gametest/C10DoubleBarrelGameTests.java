package tempeststudios.quickstacknearby.gametest;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;
import tempeststudios.quickstacknearby.QuickStackService;

import java.lang.reflect.Method;

/** Provider-specific test of the audited optional Double Barrels public contract. */
public final class C10DoubleBarrelGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void connectedDoubleBarrelIsOneLiveTargetAndPersistsCombinedInsert(GameTestHelper helper) throws Exception {
        BlockPos firstPos = new BlockPos(1, 2, 1), secondPos = new BlockPos(2, 2, 1);
        helper.setBlock(firstPos, Blocks.BARREL); helper.setBlock(secondPos, Blocks.BARREL);
        BarrelBlockEntity first = helper.getBlockEntity(firstPos, BarrelBlockEntity.class);
        BarrelBlockEntity second = helper.getBlockEntity(secondPos, BarrelBlockEntity.class);
        Class<?> access = Class.forName("com.mozko.doublebarrels.DoubleBarrelAccess", false, first.getClass().getClassLoader());
        access.getMethod("connectTo", BarrelBlockEntity.class).invoke(first, second);
        Container combined = (Container) access.getMethod("getCombinedInventory").invoke(first);
        helper.assertTrue(combined != null && combined.getContainerSize() == 54, "Double Barrels did not provide its audited combined inventory");
        combined.setItem(0, new ItemStack(Items.STONE, 60));
        ServerPlayer player = helper.makeMockServerPlayerInLevel(); BlockPos at = helper.absolutePos(new BlockPos(0, 2, 1)); player.setPos(at.getX() + .5, at.getY(), at.getZ() + .5);
        player.getInventory().setItem(9, new ItemStack(Items.STONE, 12));
        QuickStackMoveEngine.Result result = QuickStackService.quickStack(player);
        helper.assertTrue(result.itemsMoved() == 12 && combined.getItem(0).getCount() == 64 && combined.getItem(1).getCount() == 8,
                "C10 did not persist a single combined Double Barrels insertion");
        helper.succeed();
    }
    @Override public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException { method.invoke(this, helper); }
}
