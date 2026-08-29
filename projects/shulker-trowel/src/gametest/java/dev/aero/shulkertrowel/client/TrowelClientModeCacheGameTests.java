package dev.aero.shulkertrowel.client;

import dev.aero.shulkertrowel.geometry.TargetGeometry;
import dev.aero.shulkertrowel.geometry.TrowelGeometryState;
import dev.aero.shulkertrowel.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

public final class TrowelClientModeCacheGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void pendingModeBridgesRapidInputWithoutMutatingStack(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.TROWEL);
        int slot = 4;

        helper.assertTrue(TrowelGeometryState.get(stack) == TargetGeometry.FULL,
                "Fresh stack did not start authoritative Full");
        TrowelClientModeCache.record(stack, slot, TargetGeometry.SLAB);
        helper.assertTrue(TrowelGeometryState.get(stack) == TargetGeometry.FULL,
                "Client pending mode mutated authoritative stack state");
        helper.assertTrue(TrowelClientModeCache.displayed(stack, slot) == TargetGeometry.SLAB,
                "Pending mode did not bridge rapid input");

        TrowelGeometryState.set(stack, TargetGeometry.SLAB);
        helper.assertTrue(TrowelClientModeCache.displayed(stack, slot) == TargetGeometry.SLAB,
                "Server acknowledgement was not observed");
        TrowelGeometryState.set(stack, TargetGeometry.FULL);
        helper.assertTrue(TrowelClientModeCache.displayed(stack, slot) == TargetGeometry.FULL,
                "Acknowledged pending mode was not cleared");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void pendingModeDoesNotCrossStackOrSlotIdentity(GameTestHelper helper) {
        ItemStack first = new ItemStack(ModItems.TROWEL);
        ItemStack second = new ItemStack(ModItems.TROWEL);

        TrowelClientModeCache.record(first, 2, TargetGeometry.STEP);
        helper.assertTrue(TrowelClientModeCache.displayed(second, 2) == TargetGeometry.FULL,
                "Pending mode crossed stack identity");

        TrowelClientModeCache.record(first, 2, TargetGeometry.WALL);
        helper.assertTrue(TrowelClientModeCache.displayed(first, 3) == TargetGeometry.FULL,
                "Pending mode crossed selected-slot identity");
        helper.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
