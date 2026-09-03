package dev.resivore.quickstacknearbycompat.core;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;

/** Adds optional CSR affinity and reserved-empty-slot priority without taking over QSN routing. */
public final class CsrQuickStackIntegration {
    private CsrQuickStackIntegration() {}

    public static List<QuickStackMoveEngine.Target> augmentTargets(
            Container source,
            int firstSourceSlot,
            int exclusiveLastSourceSlot,
            List<QuickStackMoveEngine.Target> targets,
            QuickStackMoveEngine.SourceRules sourceRules) {
        if (!CsrReservationResolver.isAvailable()) {
            return targets;
        }
        return augmentTargets(
                source,
                firstSourceSlot,
                exclusiveLastSourceSlot,
                targets,
                sourceRules,
                (container, slot, incoming) ->
                        CsrReservationResolver.classify(container, slot, incoming)
                                == CsrReservationResolver.SlotClass.MATCHING_RESERVATION
        );
    }

    static List<QuickStackMoveEngine.Target> augmentTargets(
            Container source,
            int firstSourceSlot,
            int exclusiveLastSourceSlot,
            List<QuickStackMoveEngine.Target> targets,
            QuickStackMoveEngine.SourceRules sourceRules,
            ReservationMatcher reservations) {
        if (targets == null || targets.isEmpty()) {
            return targets;
        }

        QuickStackMoveEngine.SourceRules rules = sourceRules == null
                ? QuickStackMoveEngine.SourceRules.EMPTY
                : sourceRules;
        List<ItemStack> sourceStacks = new ArrayList<>();
        int lastSourceSlot = Math.min(exclusiveLastSourceSlot, source.getContainerSize());
        for (int sourceSlot = Math.max(0, firstSourceSlot); sourceSlot < lastSourceSlot; sourceSlot++) {
            ItemStack stack = source.getItem(sourceSlot);
            if (stack.isEmpty() || rules.isLocked(sourceSlot)
                    || rules.movableCount(sourceSlot, stack.getCount()) <= 0) {
                continue;
            }
            sourceStacks.add(stack);
        }
        if (sourceStacks.isEmpty()) {
            return targets;
        }

        List<QuickStackMoveEngine.Target> augmented = new ArrayList<>(targets.size());
        boolean changed = false;
        for (QuickStackMoveEngine.Target target : targets) {
            LinkedHashSet<QuickStackMoveEngine.StackKey> acceptedTypes =
                    new LinkedHashSet<>(target.acceptedTypes());
            Container targetContainer = target.container();
            for (int targetSlot = 0; targetSlot < targetContainer.getContainerSize(); targetSlot++) {
                if (!targetContainer.getItem(targetSlot).isEmpty()) {
                    continue;
                }
                for (ItemStack sourceStack : sourceStacks) {
                    if (reservations.matches(targetContainer, targetSlot, sourceStack)) {
                        // Construct the QSN key only after CSR has confirmed exact item+component identity.
                        acceptedTypes.add(QuickStackMoveEngine.StackKey.of(sourceStack));
                    }
                }
            }

            if (acceptedTypes.equals(target.acceptedTypes())) {
                augmented.add(target);
            } else {
                changed = true;
                augmented.add(new QuickStackMoveEngine.Target(
                        targetContainer,
                        Collections.unmodifiableSet(acceptedTypes)
                ));
            }
        }
        return changed ? List.copyOf(augmented) : targets;
    }

    /**
     * Returns empty when CSR is absent or no empty slot is reservation-governed for this stack,
     * allowing QSN's exact native empty-slot method to run.
     */
    public static OptionalInt insertIntoEmptySlots(ItemStack sourceStack, Container target) {
        if (!CsrReservationResolver.isAvailable()) {
            return OptionalInt.empty();
        }
        return insertIntoEmptySlotsIfReservationGoverned(
                sourceStack,
                target,
                CsrReservationResolver::classify
        );
    }

    static OptionalInt insertIntoEmptySlotsIfReservationGoverned(
            ItemStack sourceStack,
            Container target,
            SlotClassifier classifier) {
        if (!hasReservationGovernedEmptySlot(sourceStack, target, classifier)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(insertIntoEmptySlots(sourceStack, target, classifier));
    }

    private static boolean hasReservationGovernedEmptySlot(
            ItemStack sourceStack,
            Container target,
            SlotClassifier classifier) {
        for (int slot = 0; slot < target.getContainerSize(); slot++) {
            if (!target.getItem(slot).isEmpty()) {
                continue;
            }
            CsrReservationResolver.SlotClass slotClass = classifier.classify(target, slot, sourceStack);
            if (slotClass == CsrReservationResolver.SlotClass.MATCHING_RESERVATION
                    || slotClass == CsrReservationResolver.SlotClass.MISMATCHED_RESERVATION) {
                return true;
            }
        }
        return false;
    }

    static int insertIntoEmptySlots(
            ItemStack sourceStack,
            Container target,
            SlotClassifier classifier) {
        int moved = insertPass(
                sourceStack,
                target,
                classifier,
                CsrReservationResolver.SlotClass.MATCHING_RESERVATION
        );
        if (!sourceStack.isEmpty()) {
            moved += insertPass(
                    sourceStack,
                    target,
                    classifier,
                    CsrReservationResolver.SlotClass.ORDINARY_EMPTY
            );
        }
        return moved;
    }

    private static int insertPass(
            ItemStack sourceStack,
            Container target,
            SlotClassifier classifier,
            CsrReservationResolver.SlotClass passClass) {
        int moved = 0;
        for (int slot = 0; slot < target.getContainerSize() && !sourceStack.isEmpty(); slot++) {
            if (!target.getItem(slot).isEmpty()
                    || classifier.classify(target, slot, sourceStack) != passClass
                    || !target.canPlaceItem(slot, sourceStack)) {
                continue;
            }

            int maxCount = Math.min(
                    sourceStack.getMaxStackSize(),
                    target.getMaxStackSize(sourceStack)
            );
            int amount = Math.min(maxCount, sourceStack.getCount());
            target.setItem(slot, sourceStack.copyWithCount(amount));
            sourceStack.shrink(amount);
            moved += amount;
        }
        return moved;
    }

    @FunctionalInterface
    interface ReservationMatcher {
        boolean matches(Container container, int slot, ItemStack incoming);
    }

    @FunctionalInterface
    interface SlotClassifier {
        CsrReservationResolver.SlotClass classify(Container container, int slot, ItemStack incoming);
    }
}
