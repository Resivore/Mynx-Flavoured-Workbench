package dev.resivore.slotreservations;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** Pure server-side transition logic; it never mutates either physical input stack. */
final class ReservationTransition {
    private ReservationTransition() {
    }

    static Result fromOccupied(ReservationData current, int slot, ItemStack physical) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(physical, "physical");
        if (physical.isEmpty()) throw new IllegalArgumentException("Occupied transition requires a physical stack");

        if (current.matches(slot, physical)) {
            return new Result(current.without(slot), Outcome.CLEARED);
        }

        if (!ReservationTemplateEligibility.allows(physical)) return new Result(current, Outcome.REJECTED);
        return new Result(current.with(slot, physical), Outcome.SET);
    }

    static Result fromCursor(ReservationData current, int slot, ItemStack cursor) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(cursor, "cursor");
        if (cursor.isEmpty()) throw new IllegalArgumentException("Cursor transition requires a carried stack");

        if (!ReservationTemplateEligibility.allows(cursor)) return new Result(current, Outcome.REJECTED);
        ReservationData changed = current.with(slot, cursor);
        return new Result(changed, changed == current ? Outcome.UNCHANGED : Outcome.SET);
    }

    static Result clear(ReservationData current, int slot) {
        Objects.requireNonNull(current, "current");
        ReservationData cleared = current.without(slot);
        return new Result(cleared, cleared == current ? Outcome.UNCHANGED : Outcome.CLEARED);
    }

    enum Outcome {
        REJECTED,
        UNCHANGED,
        SET,
        CLEARED
    }

    record Result(ReservationData data, Outcome outcome) {
        Result {
            Objects.requireNonNull(data, "data");
            Objects.requireNonNull(outcome, "outcome");
        }

        boolean changed() {
            return outcome == Outcome.SET || outcome == Outcome.CLEARED;
        }
    }
}
