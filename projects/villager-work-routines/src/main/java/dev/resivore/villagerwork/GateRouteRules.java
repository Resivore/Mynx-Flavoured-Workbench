package dev.resivore.villagerwork;

import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/** Deterministic decisions for one already-selected sheep's gate route. World checks belong to the caller. */
final class GateRouteRules {
    private GateRouteRules() {}

    record Candidate(BlockPos gate, BlockPos near, BlockPos far, BlockPos interaction,
                     double approachCost, double sheepCost,
                     boolean nearReachable, boolean farConnectsToSheep) {
        boolean valid() {
            return gate != null && near != null && far != null && interaction != null
                    && !gate.equals(near) && !gate.equals(far) && !near.equals(far)
                    && nearReachable && farConnectsToSheep
                    && Double.isFinite(approachCost) && approachCost >= 0
                    && Double.isFinite(sheepCost) && sheepCost >= 0
                    && Double.isFinite(approachCost + sheepCost);
        }

        double totalCost() { return approachCost + sheepCost; }
    }

    private static final Comparator<Candidate> ROUTE_ORDER = Comparator
            .comparingDouble(Candidate::totalCost)
            .thenComparing(c -> c.gate, GateRouteRules::comparePos)
            .thenComparing(c -> c.near, GateRouteRules::comparePos)
            .thenComparing(c -> c.far, GateRouteRules::comparePos)
            .thenComparing(c -> c.interaction, GateRouteRules::comparePos);

    static Optional<Candidate> select(boolean directRouteAccepted, Collection<Candidate> candidates) {
        if (directRouteAccepted || candidates == null) return Optional.empty();
        return candidates.stream().filter(Objects::nonNull).filter(Candidate::valid).min(ROUTE_ORDER);
    }

    private static int comparePos(BlockPos a, BlockPos b) {
        int x = Integer.compare(a.getX(), b.getX());
        if (x != 0) return x;
        int y = Integer.compare(a.getY(), b.getY());
        return y != 0 ? y : Integer.compare(a.getZ(), b.getZ());
    }

    /** An already-open gate belongs to its prior opener, even if this routine traverses it. */
    static boolean ownsOpenTransition(boolean initiallyOpen, boolean successfullyOpened) {
        return !initiallyOpen && successfullyOpened;
    }

    /** A powered, occupied, changed, or uncleared passage must be left alone. */
    static boolean mayClose(boolean ownedOpen, boolean sameGate, boolean stillOpen,
                            boolean externallyChanged, boolean powered,
                            boolean passageClear, boolean physicallyCleared) {
        return planClosure(ownedOpen, sameGate, stillOpen, externallyChanged, powered,
                passageClear, physicallyCleared, false) == ClosurePlan.ATTEMPT_CLOSE;
    }

    enum ClosurePlan {
        COMPLETE_UNOWNED,
        RELEASE_EXTERNAL,
        RELEASE_SHARED,
        WAIT_FOR_CLEARANCE,
        ATTEMPT_CLOSE
    }

    /**
     * An owned, still-open gate never becomes complete merely because closing is delayed.
     * Shared owners keep the physical gate open; the last owner performs the safe close.
     */
    static ClosurePlan planClosure(boolean ownedOpen, boolean sameGate, boolean stillOpen,
                                   boolean externallyChanged, boolean powered,
                                   boolean passageClear, boolean physicallyCleared,
                                   boolean otherOwners) {
        if (!ownedOpen) return ClosurePlan.COMPLETE_UNOWNED;
        if (!sameGate || !stillOpen || externallyChanged || powered)
            return ClosurePlan.RELEASE_EXTERNAL;
        if (otherOwners) return ClosurePlan.RELEASE_SHARED;
        if (!passageClear || !physicallyCleared) return ClosurePlan.WAIT_FOR_CLEARANCE;
        return ClosurePlan.ATTEMPT_CLOSE;
    }

    enum Stage {
        APPROACH_ENTRY, OPEN_ENTRY, CROSS_ENTRY, CLOSE_ENTRY, APPROACH_SHEEP,
        SHEAR, APPROACH_EXIT, OPEN_EXIT, CROSS_EXIT, CLOSE_EXIT,
        RETURN_TO_LOOM, CANCELLED
    }

    /**
     * Physical side of a gate, measured against the collision shape the closed gate would have.
     * PASSAGE is deliberately neither side: a villager in that thin transition must retain its
     * crossing obligation rather than letting timeout cleanup close the gate around or behind it.
     */
    enum GateSide { NEAR, FAR, PASSAGE }

    static GateSide classifyGateSide(boolean intersectsClosedGate, double projectionTowardFar) {
        if (intersectsClosedGate || !Double.isFinite(projectionTowardFar)
                || projectionTowardFar == 0.0) return GateSide.PASSAGE;
        return projectionTowardFar > 0.0 ? GateSide.FAR : GateSide.NEAR;
    }

    static boolean reachedSide(GateSide current, GateSide expected) {
        return expected != GateSide.PASSAGE && current == expected;
    }

    /** A timed-out exit is never complete until the villager is demonstrably clear outside. */
    static Stage recoverExitTimeout(GateSide side) {
        return switch (side) {
            case NEAR -> Stage.CLOSE_EXIT;
            case FAR -> Stage.APPROACH_EXIT;
            case PASSAGE -> Stage.CROSS_EXIT;
        };
    }

    /** The caller supplies a verified physical milestone; false never skips a stage. */
    static Stage advance(Stage stage, boolean milestoneReached) {
        if (!milestoneReached) return stage;
        return switch (stage) {
            case APPROACH_ENTRY -> Stage.OPEN_ENTRY;
            case OPEN_ENTRY -> Stage.CROSS_ENTRY;
            case CROSS_ENTRY -> Stage.CLOSE_ENTRY;
            case CLOSE_ENTRY -> Stage.APPROACH_SHEEP;
            case APPROACH_SHEEP -> Stage.SHEAR;
            case SHEAR -> Stage.APPROACH_EXIT;
            case APPROACH_EXIT -> Stage.OPEN_EXIT;
            case OPEN_EXIT -> Stage.CROSS_EXIT;
            case CROSS_EXIT -> Stage.CLOSE_EXIT;
            case CLOSE_EXIT -> Stage.RETURN_TO_LOOM;
            case RETURN_TO_LOOM, CANCELLED -> stage;
        };
    }

    static Stage cancel(Stage stage) { return Stage.CANCELLED; }
}
