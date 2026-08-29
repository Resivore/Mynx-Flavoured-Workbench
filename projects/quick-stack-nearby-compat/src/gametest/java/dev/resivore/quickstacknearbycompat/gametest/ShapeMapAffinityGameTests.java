package dev.resivore.quickstacknearbycompat.gametest;

import dev.resivore.quickstacknearbycompat.core.ShapeMapTargetAffinity;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedGeometrySupport;
import games.twinhead.moreslabsstairsandwalls.api.material.DerivedMaterialTraits;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfile;
import games.twinhead.moreslabsstairsandwalls.api.material.NibaruMaterialProfiles;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public final class ShapeMapAffinityGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 40)
    public void oakPlanksAndSlabsMergeIntoExistingShapeBothDirections(GameTestHelper helper) {
        assertMerged(helper, Blocks.OAK_PLANKS, Blocks.OAK_SLAB, 20, 16,
                "oak slabs did not merge into existing oak planks");
        assertMerged(helper, Blocks.OAK_SLAB, Blocks.OAK_PLANKS, 20, 16,
                "oak planks did not merge into existing oak slabs");

        helper.assertTrue(!ShapeMap.inSameShapeSet(Blocks.OAK_PLANKS.asItem(), Blocks.OAK_PLANKS.asItem()),
                "Exact CNM 2.0.7 parent/parent ShapeMap semantics changed");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void verticalStepAndStairsMergeIntoExistingShapeBothDirections(GameTestHelper helper) {
        Block vertical = derived(Blocks.OAK_PLANKS, DerivedGeometrySupport.Geometry.VERTICAL_SLAB);
        Block step = derived(Blocks.OAK_PLANKS, DerivedGeometrySupport.Geometry.STEP);

        assertMerged(helper, Blocks.OAK_STAIRS, vertical, 20, 16,
                "CNM vertical slab did not merge into existing oak stairs");
        assertMerged(helper, vertical, Blocks.OAK_STAIRS, 20, 16,
                "oak stairs did not merge into existing CNM vertical slab");
        assertMerged(helper, Blocks.OAK_STAIRS, step, 20, 16,
                "CNM step did not merge into existing oak stairs");
        assertMerged(helper, step, Blocks.OAK_STAIRS, 20, 16,
                "oak stairs did not merge into existing CNM step");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void capacityRemainderAndFullDestinationUseUnmodifiedQsnRules(GameTestHelper helper) {
        SimpleContainer partialSource = container(9, new ItemStack(Blocks.OAK_SLAB, 16));
        SimpleContainer partialTarget = container(1, new ItemStack(Blocks.OAK_PLANKS, 60));
        QuickStackMoveEngine.Result partial = move(partialSource, 0, partialSource.getContainerSize(),
                partialTarget, QuickStackMoveEngine.SourceRules.EMPTY);
        helper.assertTrue(partial.itemsMoved() == 4
                        && partialTarget.getItem(0).getItem() == Blocks.OAK_PLANKS.asItem()
                        && partialTarget.getItem(0).getCount() == 64
                        && partialSource.getItem(0).getItem() == Blocks.OAK_SLAB.asItem()
                        && partialSource.getItem(0).getCount() == 12,
                "Partial family merge did not preserve the exact QSN remainder");

        SimpleContainer spillSource = container(9, new ItemStack(Blocks.OAK_SLAB, 16));
        SimpleContainer spillTarget = container(2, new ItemStack(Blocks.OAK_PLANKS, 60));
        QuickStackMoveEngine.Result spill = move(spillSource, 0, spillSource.getContainerSize(),
                spillTarget, QuickStackMoveEngine.SourceRules.EMPTY);
        helper.assertTrue(spill.itemsMoved() == 16 && spillSource.isEmpty()
                        && spillTarget.getItem(0).getItem() == Blocks.OAK_PLANKS.asItem()
                        && spillTarget.getItem(0).getCount() == 64
                        && spillTarget.getItem(1).getItem() == Blocks.OAK_SLAB.asItem()
                        && spillTarget.getItem(1).getCount() == 12,
                "QSN did not merge available family capacity before copying the exact remainder to an empty slot");

        SimpleContainer fullSource = container(9, new ItemStack(Blocks.OAK_SLAB, 16));
        SimpleContainer fullTargetWithEmpty = container(2, new ItemStack(Blocks.OAK_PLANKS, 64));
        QuickStackMoveEngine.Result fullWithEmpty = move(fullSource, 0, fullSource.getContainerSize(),
                fullTargetWithEmpty, QuickStackMoveEngine.SourceRules.EMPTY);
        helper.assertTrue(fullWithEmpty.itemsMoved() == 16 && fullSource.isEmpty()
                        && fullTargetWithEmpty.getItem(0).getItem() == Blocks.OAK_PLANKS.asItem()
                        && fullTargetWithEmpty.getItem(0).getCount() == 64
                        && fullTargetWithEmpty.getItem(1).getItem() == Blocks.OAK_SLAB.asItem()
                        && fullTargetWithEmpty.getItem(1).getCount() == 16,
                "A full family stack did not fall through to QSN's native exact-source empty-slot insertion");

        SimpleContainer blockedSource = container(9, new ItemStack(Blocks.OAK_SLAB, 16));
        SimpleContainer blockedTarget = container(1, new ItemStack(Blocks.OAK_PLANKS, 64));
        QuickStackMoveEngine.Result blocked = move(blockedSource, 0, blockedSource.getContainerSize(),
                blockedTarget, QuickStackMoveEngine.SourceRules.EMPTY);
        helper.assertTrue(blocked.itemsMoved() == 0 && blockedSource.getItem(0).getCount() == 16
                        && blockedTarget.getItem(0).getCount() == 64,
                "A full target without legal capacity consumed a family source remainder");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void spruceOtherFamiliesAndOrdinaryComponentsRemainDistinct(GameTestHelper helper) {
        assertRejected(helper, Blocks.OAK_PLANKS, Blocks.SPRUCE_SLAB,
                "spruce slab entered an oak ShapeMap destination");
        assertRejected(helper, Blocks.OAK_SLAB, Blocks.STONE_SLAB,
                "an unrelated CNM shape family entered the oak destination");
        helper.assertTrue(!ShapeMap.inSameShapeSet(Blocks.OAK_PLANKS.asItem(), Blocks.SPRUCE_SLAB.asItem())
                        && !ShapeMap.inSameShapeSet(Blocks.OAK_SLAB.asItem(), Blocks.STONE_SLAB.asItem()),
                "Exact CNM ShapeMap unexpectedly joined unrelated material families");

        ItemStack namedTarget = new ItemStack(Items.POISONOUS_POTATO, 10);
        namedTarget.set(DataComponents.CUSTOM_NAME, Component.literal("target identity"));
        ItemStack namedSource = new ItemStack(Items.POISONOUS_POTATO, 5);
        namedSource.set(DataComponents.CUSTOM_NAME, Component.literal("source identity"));
        SimpleContainer componentTarget = container(9, namedTarget);
        SimpleContainer componentSource = container(9, namedSource);
        QuickStackMoveEngine.Result distinct = move(componentSource, 0, componentSource.getContainerSize(),
                componentTarget, QuickStackMoveEngine.SourceRules.EMPTY);
        helper.assertTrue(!ShapeMap.contains(Items.POISONOUS_POTATO)
                        && distinct.itemsMoved() == 0
                        && componentSource.getItem(0).getCount() == 5
                        && componentTarget.getItem(0).getCount() == 10
                        && componentTarget.getItem(1).isEmpty(),
                "Ordinary component-distinct stacks stopped using QSN's exact StackKey identity");

        ItemStack exactTargetStack = namedTarget.copyWithCount(10);
        ItemStack exactSourceStack = namedTarget.copyWithCount(5);
        SimpleContainer exactTarget = container(9, exactTargetStack);
        SimpleContainer exactSource = container(9, exactSourceStack);
        QuickStackMoveEngine.Result exact = move(exactSource, 0, exactSource.getContainerSize(),
                exactTarget, QuickStackMoveEngine.SourceRules.EMPTY);
        helper.assertTrue(exact.itemsMoved() == 5 && exactSource.isEmpty()
                        && exactTarget.getItem(0).getCount() == 15,
                "Ordinary same-item/same-components QSN merge changed");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void acceptedNibaruAndCnmGeometriesUseTheirActualShapeMapFamily(GameTestHelper helper) {
        NibaruMaterialProfile profile = profile(Blocks.SEA_LANTERN);
        Block slab = profile.effectiveSlabSource().orElseThrow();
        Block stair = profile.effectiveStairSource().orElseThrow();
        Block wall = profile.nativeWall().orElseThrow();
        Block vertical = derived(Blocks.SEA_LANTERN, DerivedGeometrySupport.Geometry.VERTICAL_SLAB);
        Block step = derived(Blocks.SEA_LANTERN, DerivedGeometrySupport.Geometry.STEP);
        List<Block> family = List.of(Blocks.SEA_LANTERN, slab, stair, wall, vertical, step);
        List<net.minecraft.world.item.Item> actualShapeSet = ShapeMap.getShapes(Blocks.SEA_LANTERN.asItem());

        for (Block member : family) {
            helper.assertTrue(actualShapeSet.contains(member.asItem()),
                    "Accepted CNM/Nibaru integration omitted a representative item from the actual ShapeMap: "
                            + member);
        }
        for (Block member : List.of(slab, stair, wall, vertical, step)) {
            helper.assertTrue(ShapeMap.inSameShapeSet(Blocks.SEA_LANTERN.asItem(), member.asItem()),
                    "Actual ShapeMap did not declare the representative geometry equivalent: " + member);
        }

        assertMerged(helper, slab, stair, 20, 16, "Nibaru stair did not merge into Nibaru slab");
        assertMerged(helper, stair, wall, 20, 16, "Nibaru wall did not merge into Nibaru stair");
        assertMerged(helper, wall, vertical, 20, 16, "CNM vertical did not merge into Nibaru wall");
        assertMerged(helper, vertical, step, 20, 16, "CNM step did not merge into CNM vertical");
        assertMerged(helper, step, Blocks.SEA_LANTERN, 20, 16,
                "Full source block did not merge into the existing CNM step");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void destinationAdmissionRemainsFrozenForTheRequest(GameTestHelper helper) {
        SimpleContainer mixedSource = new SimpleContainer(9);
        mixedSource.setItem(0, new ItemStack(Blocks.OAK_SLAB, 2));
        mixedSource.setItem(1, new ItemStack(Blocks.SPRUCE_SLAB, 2));
        SimpleContainer oakTarget = container(9, new ItemStack(Blocks.OAK_PLANKS, 20));
        QuickStackMoveEngine.Result mixed = move(mixedSource, 0, mixedSource.getContainerSize(),
                oakTarget, QuickStackMoveEngine.SourceRules.EMPTY);
        helper.assertTrue(mixed.itemsMoved() == 2 && mixedSource.getItem(0).isEmpty()
                        && mixedSource.getItem(1).getCount() == 2
                        && oakTarget.getItem(0).getCount() == 22
                        && oakTarget.getItem(1).isEmpty(),
                "A newly processed family changed unrelated admission in the same request");

        SimpleContainer frozenSource = container(9, new ItemStack(Blocks.SPRUCE_SLAB, 2));
        SimpleContainer frozenTarget = container(9, new ItemStack(Blocks.OAK_PLANKS, 20));
        List<QuickStackMoveEngine.Target> frozen = augmented(
                frozenSource, 0, frozenSource.getContainerSize(), frozenTarget,
                QuickStackMoveEngine.SourceRules.EMPTY);
        frozenTarget.setItem(1, new ItemStack(Blocks.SPRUCE_STAIRS, 1));
        QuickStackMoveEngine.Result frozenResult = QuickStackMoveEngine.moveMatchingItems(
                frozenSource, 0, frozenSource.getContainerSize(), frozen,
                QuickStackMoveEngine.SourceRules.EMPTY);
        helper.assertTrue(frozenResult.itemsMoved() == 0 && frozenSource.getItem(0).getCount() == 2,
                "Target ShapeMap admission was recomputed after the frozen destination snapshot");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void inventoryExtendedRowsLocksAndKeepCountsRemainInForce(GameTestHelper helper) {
        SimpleContainer extendedSource = new SimpleContainer(63);
        extendedSource.setItem(36, new ItemStack(Blocks.OAK_SLAB, 2));
        extendedSource.setItem(45, new ItemStack(Blocks.OAK_STAIRS, 2));
        extendedSource.setItem(54, new ItemStack(
                profile(Blocks.OAK_PLANKS).nativeWall().orElseThrow(), 2));
        SimpleContainer extendedTarget = container(9, new ItemStack(Blocks.OAK_PLANKS, 20));
        QuickStackMoveEngine.Result extended = move(extendedSource, 9, 63, extendedTarget,
                QuickStackMoveEngine.SourceRules.EMPTY);
        helper.assertTrue(extended.itemsMoved() == 6
                        && extendedSource.getItem(36).isEmpty()
                        && extendedSource.getItem(45).isEmpty()
                        && extendedSource.getItem(54).isEmpty()
                        && extendedTarget.getItem(0).getCount() == 26,
                "One or more Inventory Extended rows 4-6 fell outside family routing");

        SimpleContainer ruledSource = new SimpleContainer(63);
        ruledSource.setItem(61, new ItemStack(Blocks.OAK_SLAB, 8));
        ruledSource.setItem(62, new ItemStack(Blocks.OAK_SLAB, 16));
        QuickStackMoveEngine.SourceRules rules = new QuickStackMoveEngine.SourceRules(Map.of(
                61, new QuickStackMoveEngine.SlotRule(true, 0),
                62, new QuickStackMoveEngine.SlotRule(false, 6)
        ));
        SimpleContainer ruledTarget = container(9, new ItemStack(Blocks.OAK_PLANKS, 20));
        QuickStackMoveEngine.Result ruled = move(ruledSource, 9, 63, ruledTarget, rules);
        helper.assertTrue(ruled.itemsMoved() == 10
                        && ruledSource.getItem(61).getCount() == 8
                        && ruledSource.getItem(62).getCount() == 6
                        && ruledTarget.getItem(0).getCount() == 30,
                "Inventory Extended lock or keep-count semantics changed");
        helper.succeed();
    }

    private static void assertMerged(
            GameTestHelper helper,
            Block targetItem,
            Block sourceItem,
            int targetCount,
            int sourceCount,
            String label) {
        SimpleContainer source = container(9, new ItemStack(sourceItem, sourceCount));
        SimpleContainer target = container(9, new ItemStack(targetItem, targetCount));
        QuickStackMoveEngine.Result result = move(source, 0, source.getContainerSize(), target,
                QuickStackMoveEngine.SourceRules.EMPTY);
        helper.assertTrue(result.itemsMoved() == sourceCount && source.isEmpty()
                        && target.getItem(0).getItem() == targetItem.asItem()
                        && target.getItem(0).getCount() == targetCount + sourceCount
                        && target.getItem(1).isEmpty(),
                label);
    }

    private static void assertRejected(GameTestHelper helper, Block targetItem, Block sourceItem, String label) {
        SimpleContainer source = container(9, new ItemStack(sourceItem, 2));
        SimpleContainer target = container(9, new ItemStack(targetItem, 20));
        QuickStackMoveEngine.Result result = move(source, 0, source.getContainerSize(), target,
                QuickStackMoveEngine.SourceRules.EMPTY);
        helper.assertTrue(result.itemsMoved() == 0 && source.getItem(0).getCount() == 2
                        && target.getItem(0).getCount() == 20 && target.getItem(1).isEmpty(),
                label);
    }

    private static QuickStackMoveEngine.Result move(
            SimpleContainer source,
            int firstSourceSlot,
            int exclusiveLastSourceSlot,
            SimpleContainer target,
            QuickStackMoveEngine.SourceRules sourceRules) {
        List<QuickStackMoveEngine.Target> targets = augmented(
                source, firstSourceSlot, exclusiveLastSourceSlot, target, sourceRules);
        return QuickStackMoveEngine.moveMatchingItems(
                source, firstSourceSlot, exclusiveLastSourceSlot, targets, sourceRules);
    }

    private static List<QuickStackMoveEngine.Target> augmented(
            SimpleContainer source,
            int firstSourceSlot,
            int exclusiveLastSourceSlot,
            SimpleContainer target,
            QuickStackMoveEngine.SourceRules sourceRules) {
        return ShapeMapTargetAffinity.augmentTargets(
                source,
                firstSourceSlot,
                exclusiveLastSourceSlot,
                List.of(QuickStackMoveEngine.Target.fromCurrentContents(target)),
                sourceRules
        );
    }

    private static SimpleContainer container(int size, ItemStack firstStack) {
        SimpleContainer container = new SimpleContainer(size);
        container.setItem(0, firstStack);
        return container;
    }

    private static NibaruMaterialProfile profile(Block parent) {
        return NibaruMaterialProfiles.fromBlock(parent).orElseThrow();
    }

    private static Block derived(Block parent, DerivedGeometrySupport.Geometry geometry) {
        return DerivedMaterialTraits.entries().stream()
                .filter(entry -> entry.canonicalParent() == parent && entry.geometry() == geometry)
                .map(DerivedMaterialTraits.Entry::derived)
                .findFirst()
                .orElseThrow();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        method.invoke(this, helper);
    }
}
