package dev.resivore.slotreservations;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-side reservation transition planning. Identity attachment is explicit and authoritative. */
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

        if (PortableContainerIdentity.familyOf(physical).isPresent() && !PortableContainerIdentity.isEmpty(physical)) {
            if (physical.getCount() != 1) return new Result(current, Outcome.REJECTED);
            UUID identity = PortableContainerIdentity.get(physical).orElseGet(UUID::randomUUID);
            return new Result(current.withSpecific(slot, physical, identity), Outcome.SET, Optional.of(identity));
        }
        if (!ReservationTemplateEligibility.allows(physical)) return new Result(current, Outcome.REJECTED);
        return new Result(current.with(slot, physical), Outcome.SET);
    }

    static Result fromCursor(ReservationData current, int slot, ItemStack cursor) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(cursor, "cursor");
        if (cursor.isEmpty()) throw new IllegalArgumentException("Cursor transition requires a carried stack");

        if (PortableContainerIdentity.familyOf(cursor).isPresent() && !PortableContainerIdentity.isEmpty(cursor)) {
            if (cursor.getCount() != 1) return new Result(current, Outcome.REJECTED);
            UUID identity = PortableContainerIdentity.get(cursor).orElseGet(UUID::randomUUID);
            ReservationData changed = current.withSpecific(slot, cursor, identity);
            return new Result(changed, changed == current ? Outcome.UNCHANGED : Outcome.SET, Optional.of(identity));
        }
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

    record Result(ReservationData data, Outcome outcome, Optional<UUID> identityToAttach) {
        Result(ReservationData data, Outcome outcome) {
            this(data, outcome, Optional.empty());
        }
        Result {
            Objects.requireNonNull(data, "data");
            Objects.requireNonNull(outcome, "outcome");
            Objects.requireNonNull(identityToAttach, "identityToAttach");
        }

        boolean changed() {
            return outcome == Outcome.SET || outcome == Outcome.CLEARED;
        }

        void attachIdentity(ItemStack physical) {
            identityToAttach.ifPresent(identity -> {
                if (physical.getCount() != 1 || PortableContainerIdentity.familyOf(physical).isEmpty()) {
                    throw new IllegalArgumentException("Identity assignment target changed before commit");
                }
                UUID existing = physical.get(ModComponents.PORTABLE_CONTAINER_ID);
                if (existing == null) physical.set(ModComponents.PORTABLE_CONTAINER_ID, identity);
                else if (!existing.equals(identity)) throw new IllegalStateException("Physical portable-container identity changed before commit");
            });
        }
    }
}
