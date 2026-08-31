package dev.aero.shulkertrowel.client;

import dev.aero.shulkertrowel.geometry.CnmNibaruGeometryResolver;
import dev.aero.shulkertrowel.geometry.TargetGeometry;
import dev.aero.shulkertrowel.geometry.TrowelGeometryState;
import dev.aero.shulkertrowel.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Method;
import java.util.List;

public final class TrowelClientModeCacheGameTests implements CustomTestMethodInvoker {
    private static final TargetGeometry STEP = bge("step");

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

        TrowelClientModeCache.record(first, 2, STEP);
        helper.assertTrue(TrowelClientModeCache.displayed(second, 2) == TargetGeometry.FULL,
                "Pending mode crossed stack identity");

        TrowelClientModeCache.record(first, 2, TargetGeometry.WALL);
        helper.assertTrue(TrowelClientModeCache.displayed(first, 3) == TargetGeometry.FULL,
                "Pending mode crossed selected-slot identity");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void selectorConsumesCompleteBgeCatalogWithActualResolvedIcons(GameTestHelper helper) {
        List<TargetGeometry> modes = TargetGeometry.ordered();
        List<Item> icons = TrowelGeometryIcons.items();
        CnmNibaruGeometryResolver resolver = new CnmNibaruGeometryResolver();

        helper.assertTrue(modes.size() == 9
                        && modes.stream().map(TargetGeometry::displayName).toList().equals(List.of(
                                "Full Block", "Slab", "Stair", "Wall", "Vertical Slab",
                                "Step", "Corner", "Quarter Column", "Layer"))
                        && modes.stream().map(TargetGeometry::networkId).toList().equals(List.of(
                                0, 1, 2, 3, 4, 5, 7, 8, 6))
                        && icons.size() == modes.size(),
                "Selector did not project the complete ordered BGE catalog with stable IDs");
        for (int index = 0; index < modes.size(); index++) {
            var expected = resolver.resolveGeometry(Blocks.OAK_PLANKS, modes.get(index))
                    .orElseThrow()
                    .asItem();
            helper.assertTrue(icons.get(index) == expected
                            && TrowelGeometryIcons.stack(index).is(expected),
                    "Selector icon " + index + " did not use its actual resolved geometry item");
        }
        helper.assertTrue(TrowelGeometryIcons.stack(-1).is(Blocks.OAK_PLANKS.asItem())
                        && TrowelGeometryIcons.stack(Integer.MAX_VALUE)
                                .is(Blocks.OAK_PLANKS.asItem()),
                "Invalid selector index did not fall back safely to Full");
        helper.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }

    private static TargetGeometry bge(String path) {
        return TargetGeometry.byKey(Identifier.fromNamespaceAndPath(
                "cnm_terrain_slabs_compat", path)).orElseThrow();
    }
}
