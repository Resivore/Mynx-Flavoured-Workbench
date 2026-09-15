package dev.resivore.villagerwork;

/** Pure state decisions for approaching and shearing one already-selected sheep. */
final class ShearActionRules {
    static final double IDEAL_APPROACH_DISTANCE = 1.8;
    static final double HARD_SAFETY_DISTANCE = 2.4;
    static final double RECOVERY_DISTANCE = 6.0;

    private ShearActionRules() {}

    enum Phase {
        APPROACH,
        TELEGRAPH,
        FINAL_VALIDATION
    }

    enum Action {
        START_TELEGRAPH,
        CONTINUE_TELEGRAPH,
        REPOSITION,
        SHEAR,
        ABANDON
    }

    enum Reason {
        WORK_INVALID,
        SHEEP_DEAD,
        SHEEP_IS_BABY,
        SHEEP_NOT_SHEARABLE,
        SHEEP_NOT_LOCAL,
        OUTPUT_UNAVAILABLE,
        INVALID_DISTANCE,
        BEYOND_RECOVERY_DISTANCE,
        UNSAFE_FOOTING,
        CLEAR_LINE_LOST,
        OUTSIDE_HARD_DISTANCE,
        OUTSIDE_IDEAL_DISTANCE,
        READY_TO_TELEGRAPH,
        TELEGRAPH_IN_PROGRESS,
        FINAL_VALIDATION_PASSED
    }

    record Eligibility(boolean workValid, boolean sheepAlive, boolean sheepAdult,
                       boolean sheepShearable, boolean sheepLocal, boolean outputAvailable) {}

    record Geometry(double sheepDistance, boolean footingSafe, boolean clearLine) {}

    record Decision(Action action, Reason reason) {}

    static Decision decide(Phase phase, Eligibility eligibility, Geometry geometry) {
        if (!eligibility.workValid()) return abandon(Reason.WORK_INVALID);
        if (!eligibility.sheepAlive()) return abandon(Reason.SHEEP_DEAD);
        if (!eligibility.sheepAdult()) return abandon(Reason.SHEEP_IS_BABY);
        if (!eligibility.sheepShearable()) return abandon(Reason.SHEEP_NOT_SHEARABLE);
        if (!eligibility.sheepLocal()) return abandon(Reason.SHEEP_NOT_LOCAL);
        if (!eligibility.outputAvailable()) return abandon(Reason.OUTPUT_UNAVAILABLE);

        double distance = geometry.sheepDistance();
        if (!Double.isFinite(distance) || distance < 0.0) return abandon(Reason.INVALID_DISTANCE);
        if (distance > RECOVERY_DISTANCE) return abandon(Reason.BEYOND_RECOVERY_DISTANCE);

        // Geometry can change as the sheep wanders. These conditions request another approach
        // without discarding the selected sheep; the caller decides whether recovery is reachable.
        if (!geometry.footingSafe()) return reposition(Reason.UNSAFE_FOOTING);
        if (!geometry.clearLine()) return reposition(Reason.CLEAR_LINE_LOST);
        if (distance > HARD_SAFETY_DISTANCE) return reposition(Reason.OUTSIDE_HARD_DISTANCE);

        return switch (phase) {
            case APPROACH -> distance <= IDEAL_APPROACH_DISTANCE
                    ? new Decision(Action.START_TELEGRAPH, Reason.READY_TO_TELEGRAPH)
                    : reposition(Reason.OUTSIDE_IDEAL_DISTANCE);
            case TELEGRAPH -> new Decision(Action.CONTINUE_TELEGRAPH, Reason.TELEGRAPH_IN_PROGRESS);
            case FINAL_VALIDATION -> new Decision(Action.SHEAR, Reason.FINAL_VALIDATION_PASSED);
        };
    }

    private static Decision reposition(Reason reason) {
        return new Decision(Action.REPOSITION, reason);
    }

    private static Decision abandon(Reason reason) {
        return new Decision(Action.ABANDON, reason);
    }
}
