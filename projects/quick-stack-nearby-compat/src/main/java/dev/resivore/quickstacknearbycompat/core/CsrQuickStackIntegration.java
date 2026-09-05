package dev.resivore.quickstacknearbycompat.core;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;

/** Adds optional CSR affinity and reserved-empty-slot priority without taking over QSN routing. */
public final class CsrQuickStackIntegration {
    private static final ThreadLocal<DiscoveryRequest> ACTIVE_DISCOVERY = new ThreadLocal<>();

    private CsrQuickStackIntegration() {}

    /**
     * Scopes the exact source window and rules to QSN's native nearby-container scan.
     * The scan and its accepted-type prefilter are synchronous on the server thread;
     * restoring the previous value in {@code finally} makes nested calls and failures safe.
     */
    public static <T> T withDiscoverySource(
            Container source,
            int firstSourceSlot,
            int exclusiveLastSourceSlot,
            QuickStackMoveEngine.SourceRules sourceRules,
            Supplier<T> action) {
        DiscoveryRequest previous = ACTIVE_DISCOVERY.get();
        ACTIVE_DISCOVERY.set(new DiscoveryRequest(
                source,
                firstSourceSlot,
                exclusiveLastSourceSlot,
                normalizedRules(sourceRules)
        ));
        try {
            return action.get();
        } finally {
            if (previous == null) {
                ACTIVE_DISCOVERY.remove();
            } else {
                ACTIVE_DISCOVERY.set(previous);
            }
        }
    }

    /**
     * Adds reservation-only affinity at QSN's raw accepted-types seam, after QSN has
     * already accepted the container's access/validity and before it discards an empty set.
     */
    public static Set<QuickStackMoveEngine.StackKey> augmentDiscoveredAcceptedTypes(
            Container target,
            Set<QuickStackMoveEngine.StackKey> nativeAcceptedTypes) {
        if (!CsrReservationResolver.isAvailable()) {
            return nativeAcceptedTypes;
        }
        return augmentActiveDiscoveryAcceptedTypes(
                target,
                nativeAcceptedTypes,
                (container, slot, incoming) ->
                        CsrReservationResolver.classify(container, slot, incoming)
                                == CsrReservationResolver.SlotClass.MATCHING_RESERVATION
        );
    }

    static Set<QuickStackMoveEngine.StackKey> augmentActiveDiscoveryAcceptedTypes(
            Container target,
            Set<QuickStackMoveEngine.StackKey> nativeAcceptedTypes,
            ReservationMatcher reservations) {
        DiscoveryRequest request = ACTIVE_DISCOVERY.get();
        if (request == null) {
            return nativeAcceptedTypes;
        }
        return augmentAcceptedTypes(
                request.source(),
                request.firstSourceSlot(),
                request.exclusiveLastSourceSlot(),
                target,
                nativeAcceptedTypes,
                request.sourceRules(),
                reservations
        );
    }

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
                ? QuickStackMoveEngine.SourceRules.EMPTY : sourceRules;

        List<QuickStackMoveEngine.Target> augmented = new ArrayList<>(targets.size());
        boolean changed = false;
        for (QuickStackMoveEngine.Target target : targets) {
            Container targetContainer = target.container();
            Set<QuickStackMoveEngine.StackKey> acceptedTypes = augmentAcceptedTypes(
                    source,
                    firstSourceSlot,
                    exclusiveLastSourceSlot,
                    targetContainer,
                    target.acceptedTypes(),
                    rules,
                    reservations
            );

            if (acceptedTypes == target.acceptedTypes()) {
                augmented.add(target);
            } else {
                changed = true;
                augmented.add(new QuickStackMoveEngine.Target(targetContainer, acceptedTypes));
            }
        }
        return changed ? List.copyOf(augmented) : targets;
    }

    static Set<QuickStackMoveEngine.StackKey> augmentAcceptedTypes(
            Container source,
            int firstSourceSlot,
            int exclusiveLastSourceSlot,
            Container target,
            Set<QuickStackMoveEngine.StackKey> nativeAcceptedTypes,
            QuickStackMoveEngine.SourceRules sourceRules,
            ReservationMatcher reservations) {
        List<ItemStack> sourceStacks = movableSourceStacks(
                source,
                firstSourceSlot,
                exclusiveLastSourceSlot,
                normalizedRules(sourceRules)
        );
        if (sourceStacks.isEmpty()) {
            return nativeAcceptedTypes;
        }

        LinkedHashSet<QuickStackMoveEngine.StackKey> acceptedTypes = null;
        for (int targetSlot = 0; targetSlot < target.getContainerSize(); targetSlot++) {
            if (!target.getItem(targetSlot).isEmpty()) {
                continue;
            }
            for (ItemStack sourceStack : sourceStacks) {
                if (!reservations.matches(target, targetSlot, sourceStack)) {
                    continue;
                }

                // Construct the QSN key only after CSR has confirmed exact item+component identity.
                QuickStackMoveEngine.StackKey key = QuickStackMoveEngine.StackKey.of(sourceStack);
                if (nativeAcceptedTypes.contains(key)
                        || acceptedTypes != null && acceptedTypes.contains(key)) {
                    continue;
                }
                if (acceptedTypes == null) {
                    acceptedTypes = new LinkedHashSet<>(nativeAcceptedTypes);
                }
                acceptedTypes.add(key);
            }
        }
        return acceptedTypes == null
                ? nativeAcceptedTypes
                : Collections.unmodifiableSet(acceptedTypes);
    }

    static List<ItemStack> activeSourceStacks() {
        DiscoveryRequest request = ACTIVE_DISCOVERY.get();
        return request == null ? List.of() : movableSourceStacks(request.source(), request.firstSourceSlot(),
                request.exclusiveLastSourceSlot(), request.sourceRules());
    }
    private static List<ItemStack> movableSourceStacks(
            Container source,
            int firstSourceSlot,
            int exclusiveLastSourceSlot,
            QuickStackMoveEngine.SourceRules sourceRules) {
        List<ItemStack> sourceStacks = new ArrayList<>();
        int lastSourceSlot = Math.min(exclusiveLastSourceSlot, source.getContainerSize());
        for (int sourceSlot = Math.max(0, firstSourceSlot); sourceSlot < lastSourceSlot; sourceSlot++) {
            ItemStack stack = source.getItem(sourceSlot);
            if (stack.isEmpty() || sourceRules.isLocked(sourceSlot)
                    || sourceRules.movableCount(sourceSlot, stack.getCount()) <= 0) {
                continue;
            }
            sourceStacks.add(stack);
        }
        return sourceStacks;
    }

    private static QuickStackMoveEngine.SourceRules normalizedRules(
            QuickStackMoveEngine.SourceRules sourceRules) {
        return sourceRules == null ? QuickStackMoveEngine.SourceRules.EMPTY : sourceRules;
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

    private record DiscoveryRequest(
            Container source,
            int firstSourceSlot,
            int exclusiveLastSourceSlot,
            QuickStackMoveEngine.SourceRules sourceRules) {
    }
}
