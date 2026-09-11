package dev.resivore.quickstacknearbycompat.core;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * C18's deliberately narrow exception to C17's populated-shulker loose-source lock.
 *
 * <p>These carriers are visible to CSR only while QSN discovers an already-nearby target. They
 * never become native loose sources. Once a matching CSR slot accepts one, its matching
 * action-start carried-content snapshot is explicitly retired before C16's drain phase.</p>
 */
public final class ReservationOnlyOuterCarriers {
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<>();

    private ReservationOnlyOuterCarriers() {
    }

    public static <T> T scoped(
            Inventory inventory,
            PlayerStorageSlots.Window window,
            QuickStackMoveEngine.SourceRules userRules,
            Supplier<T> action
    ) {
        Scope previous = ACTIVE.get();
        ACTIVE.set(new Scope(snapshot(inventory, window, userRules)));
        try {
            return action.get();
        } finally {
            if (previous == null) {
                ACTIVE.remove();
            } else {
                ACTIVE.set(previous);
            }
        }
    }

    /** Extra CSR-affinity inputs only; callers must not feed these to native loose movement. */
    static List<ItemStack> discoveryStacks() {
        Scope scope = ACTIVE.get();
        if (scope == null || scope.candidates().isEmpty()) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>(scope.candidates().size());
        for (Candidate candidate : scope.candidates()) {
            stacks.add(candidate.stack());
        }
        return stacks;
    }

    /**
     * Returns each action-start populated, count-one shulker only to the first CSR matching empty
     * slot in QSN's already-established target and physical-slot order.
     */
    public static QuickStackMoveEngine.Result returnMatchingHomes(List<QuickStackMoveEngine.Target> targets) {
        Scope scope = ACTIVE.get();
        if (scope == null || scope.candidates().isEmpty() || targets == null || targets.isEmpty()
                || !CsrReservationResolver.isAvailable()) {
            return QuickStackMoveEngine.Result.empty();
        }

        int moved = 0;
        int sourcesTouched = 0;
        for (Candidate candidate : scope.candidates()) {
            // The real source may have changed only through an earlier C18 return in this same
            // action. A different source is never allowed to substitute for the snapshot.
            if (candidate.inventory().getItem(candidate.slot()) != candidate.stack()
                    || candidate.stack().isEmpty() || candidate.stack().getCount() != 1) {
                continue;
            }
            QuickStackMoveEngine.StackKey key = QuickStackMoveEngine.StackKey.of(candidate.stack());
            for (QuickStackMoveEngine.Target target : targets) {
                // This confirms the candidate itself was what kept a reservation-only target in
                // QSN's frozen target list. It also preserves all upstream target exclusions.
                if (!target.accepts(key)) {
                    continue;
                }
                int inserted = CsrQuickStackIntegration.insertIntoMatchingReservationOnly(
                        candidate.stack(), target.container());
                if (inserted != 1 || !candidate.stack().isEmpty()) {
                    continue;
                }
                candidate.inventory().setItem(candidate.slot(), ItemStack.EMPTY);
                candidate.inventory().setChanged();
                target.container().setChanged();
                scope.touchedTargets().add(target.container());
                CarriedContainerSources.skipReturnedCarrier(candidate.stack());
                moved++;
                sourcesTouched++;
                break;
            }
        }
        return moved == 0
                ? QuickStackMoveEngine.Result.empty()
                : new QuickStackMoveEngine.Result(moved, sourcesTouched, scope.touchedTargets().size());
    }

    /** Records QSN's native loose and carried phases so C18 reports upstream-style unique targets. */
    public static void recordQsnTarget(Container target, int inserted) {
        Scope scope = ACTIVE.get();
        if (scope != null && inserted > 0) {
            scope.touchedTargets().add(target);
        }
    }

    /** The full action's unique target count, including any C18 return and later QSN phases. */
    public static int targetContainersTouched(int fallback) {
        Scope scope = ACTIVE.get();
        return scope == null ? fallback : scope.touchedTargets().size();
    }

    private static List<Candidate> snapshot(
            Inventory inventory,
            PlayerStorageSlots.Window window,
            QuickStackMoveEngine.SourceRules userRules
    ) {
        QuickStackMoveEngine.SourceRules rules = userRules == null
                ? QuickStackMoveEngine.SourceRules.EMPTY
                : userRules;
        List<Candidate> candidates = new ArrayList<>();
        for (int slot = window.firstInclusive(); slot < window.endExclusive(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            // A specific CSR portable reservation is count-one. Keep C17's broad protection for
            // every other populated shulker, including any invalid multi-count data.
            if (stack.getCount() != 1 || !PopulatedShulkerOuterProtection.isPhysicallyNonEmptyShulker(stack)
                    || rules.isLocked(slot) || rules.movableCount(slot, stack.getCount()) <= 0) {
                continue;
            }
            candidates.add(new Candidate(inventory, slot, stack));
        }
        return List.copyOf(candidates);
    }

    private record Candidate(Inventory inventory, int slot, ItemStack stack) {
    }

    private record Scope(List<Candidate> candidates, Set<Container> touchedTargets) {
        private Scope(List<Candidate> candidates) {
            this(candidates, Collections.newSetFromMap(new IdentityHashMap<>()));
        }
    }
}
