package dev.resivore.quickstacknearbycompat.core;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsrQuickStackIntegrationTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindTestComponents(Items.POISONOUS_POTATO);
        bindTestComponents(Items.COBBLESTONE);
        bindTestComponents(Items.DIRT);
    }

    @Test
    void matchingEmptyReservationAddsOnlyTheExactSourceKeyWithoutReorderingTargets() {
        ItemStack exact = namedPotato(8, "reserved");
        ItemStack distinct = namedPotato(8, "distinct");
        SimpleContainer source = new SimpleContainer(4);
        source.setItem(0, exact);
        source.setItem(1, distinct);
        SimpleContainer firstContainer = new SimpleContainer(9);
        SimpleContainer secondContainer = new SimpleContainer(9);
        QuickStackMoveEngine.Target first = QuickStackMoveEngine.Target.fromCurrentContents(firstContainer);
        QuickStackMoveEngine.Target second = QuickStackMoveEngine.Target.fromCurrentContents(secondContainer);
        List<QuickStackMoveEngine.Target> original = List.of(first, second);

        List<QuickStackMoveEngine.Target> augmented = CsrQuickStackIntegration.augmentTargets(
                source,
                0,
                source.getContainerSize(),
                original,
                QuickStackMoveEngine.SourceRules.EMPTY,
                (container, slot, incoming) -> container == secondContainer
                        && slot == 4
                        && ItemStack.isSameItemSameComponents(incoming, exact)
        );

        assertEquals(2, augmented.size());
        assertSame(first, augmented.get(0));
        assertSame(firstContainer, augmented.get(0).container());
        assertSame(secondContainer, augmented.get(1).container());
        assertTrue(augmented.get(1).accepts(QuickStackMoveEngine.StackKey.of(exact)));
        assertFalse(augmented.get(1).accepts(QuickStackMoveEngine.StackKey.of(distinct)));
    }

    @Test
    void componentDistinctAndUnrelatedReservationsDoNotCreateAffinity() {
        ItemStack reservation = namedPotato(1, "reserved");
        ItemStack sourceStack = namedPotato(8, "distinct");
        SimpleContainer source = new SimpleContainer(sourceStack);
        SimpleContainer targetContainer = new SimpleContainer(9);
        List<QuickStackMoveEngine.Target> targets = List.of(
                QuickStackMoveEngine.Target.fromCurrentContents(targetContainer));

        List<QuickStackMoveEngine.Target> result = CsrQuickStackIntegration.augmentTargets(
                source,
                0,
                1,
                targets,
                QuickStackMoveEngine.SourceRules.EMPTY,
                (container, slot, incoming) -> ItemStack.isSameItemSameComponents(reservation, incoming)
        );

        assertSame(targets, result);
    }

    @Test
    void lockedAndFullyKeptSourcesCannotCreateReservationAffinity() {
        ItemStack sourceStack = namedPotato(8, "reserved");
        SimpleContainer source = new SimpleContainer(sourceStack);
        SimpleContainer targetContainer = new SimpleContainer(9);
        List<QuickStackMoveEngine.Target> targets = List.of(
                QuickStackMoveEngine.Target.fromCurrentContents(targetContainer));

        for (QuickStackMoveEngine.SlotRule rule : List.of(
                new QuickStackMoveEngine.SlotRule(true, 0),
                new QuickStackMoveEngine.SlotRule(false, sourceStack.getCount()))) {
            List<QuickStackMoveEngine.Target> result = CsrQuickStackIntegration.augmentTargets(
                    source,
                    0,
                    1,
                    targets,
                    new QuickStackMoveEngine.SourceRules(Map.of(0, rule)),
                    (container, slot, incoming) -> true
            );
            assertSame(targets, result);
        }
    }

    @Test
    void matchingReservationsFillInPhysicalOrderBeforeEarlierOrdinaryEmpties() {
        ItemStack moving = new ItemStack(Items.COBBLESTONE, 130);
        SimpleContainer target = new SimpleContainer(8);
        ItemStack occupied = new ItemStack(Items.DIRT, 17);
        target.setItem(2, occupied);

        int moved = CsrQuickStackIntegration.insertIntoEmptySlots(
                moving,
                target,
                (container, slot, incoming) -> switch (slot) {
                    case 3, 5 -> CsrReservationResolver.SlotClass.MATCHING_RESERVATION;
                    case 0, 2, 7 -> CsrReservationResolver.SlotClass.ORDINARY_EMPTY;
                    default -> CsrReservationResolver.SlotClass.BLOCKED;
                }
        );

        assertEquals(130, moved);
        assertTrue(moving.isEmpty());
        assertEquals(64, target.getItem(3).getCount());
        assertEquals(64, target.getItem(5).getCount());
        assertEquals(2, target.getItem(0).getCount());
        assertSame(occupied, target.getItem(2));
        assertTrue(target.getItem(7).isEmpty());
    }

    @Test
    void blockedReservationPreservesExactRemainderAndNeverFallsThroughAsOrdinary() {
        ItemStack moving = new ItemStack(Items.COBBLESTONE, 70);
        SimpleContainer target = new SimpleContainer(2);

        int moved = CsrQuickStackIntegration.insertIntoEmptySlots(
                moving,
                target,
                (container, slot, incoming) -> slot == 0
                        ? CsrReservationResolver.SlotClass.MATCHING_RESERVATION
                        : CsrReservationResolver.SlotClass.BLOCKED
        );

        assertEquals(64, moved);
        assertEquals(6, moving.getCount());
        assertEquals(64, target.getItem(0).getCount());
        assertTrue(target.getItem(1).isEmpty());
    }

    @Test
    void everyInsertionUsesTheTargetsNativePerStackCapacity() {
        ItemStack moving = new ItemStack(Items.COBBLESTONE, 20);
        SimpleContainer target = new CappedContainer(4, 7);

        int moved = CsrQuickStackIntegration.insertIntoEmptySlots(
                moving,
                target,
                (container, slot, incoming) -> switch (slot) {
                    case 1, 3 -> CsrReservationResolver.SlotClass.MATCHING_RESERVATION;
                    default -> CsrReservationResolver.SlotClass.ORDINARY_EMPTY;
                }
        );

        assertEquals(20, moved);
        assertTrue(moving.isEmpty());
        assertEquals(7, target.getItem(1).getCount());
        assertEquals(7, target.getItem(3).getCount());
        assertEquals(6, target.getItem(0).getCount());
        assertTrue(target.getItem(2).isEmpty());
    }

    @Test
    void interceptionDelegatesForUnreservedTargetsButRetainsMismatchedReservationProtection() {
        ItemStack unreservedMoving = new ItemStack(Items.COBBLESTONE, 9);
        SimpleContainer unreservedTarget = new SimpleContainer(2);

        var delegated = CsrQuickStackIntegration.insertIntoEmptySlotsIfReservationGoverned(
                unreservedMoving,
                unreservedTarget,
                (container, slot, incoming) -> CsrReservationResolver.SlotClass.ORDINARY_EMPTY
        );

        assertTrue(delegated.isEmpty());
        assertEquals(9, unreservedMoving.getCount());
        assertTrue(unreservedTarget.isEmpty());

        ItemStack mismatchedMoving = new ItemStack(Items.COBBLESTONE, 9);
        SimpleContainer physicalAffinityTarget = new SimpleContainer(2);
        physicalAffinityTarget.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        var intercepted = CsrQuickStackIntegration.insertIntoEmptySlotsIfReservationGoverned(
                mismatchedMoving,
                physicalAffinityTarget,
                (container, slot, incoming) -> slot == 1
                        ? CsrReservationResolver.SlotClass.MISMATCHED_RESERVATION
                        : CsrReservationResolver.SlotClass.BLOCKED
        );

        assertTrue(intercepted.isPresent());
        assertEquals(0, intercepted.getAsInt());
        assertEquals(9, mismatchedMoving.getCount());
        assertEquals(64, physicalAffinityTarget.getItem(0).getCount());
        assertTrue(physicalAffinityTarget.getItem(1).isEmpty());

        ItemStack matchingMoving = new ItemStack(Items.COBBLESTONE, 9);
        var matching = CsrQuickStackIntegration.insertIntoEmptySlotsIfReservationGoverned(
                matchingMoving,
                physicalAffinityTarget,
                (container, slot, incoming) -> slot == 1
                        ? CsrReservationResolver.SlotClass.MATCHING_RESERVATION
                        : CsrReservationResolver.SlotClass.BLOCKED
        );

        assertTrue(matching.isPresent());
        assertEquals(9, matching.getAsInt());
        assertTrue(matchingMoving.isEmpty());
        assertEquals(9, physicalAffinityTarget.getItem(1).getCount());
    }

    private static ItemStack namedPotato(int count, String name) {
        ItemStack stack = new ItemStack(Items.POISONOUS_POTATO, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static void bindTestComponents(Item item) {
        if (!item.builtInRegistryHolder().areComponentsBound()) {
            item.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                    .set(DataComponents.MAX_STACK_SIZE, 64)
                    .build());
        }
    }

    private static final class CappedContainer extends SimpleContainer {
        private final int maxStackSize;

        private CappedContainer(int size, int maxStackSize) {
            super(size);
            this.maxStackSize = maxStackSize;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return maxStackSize;
        }
    }
}
