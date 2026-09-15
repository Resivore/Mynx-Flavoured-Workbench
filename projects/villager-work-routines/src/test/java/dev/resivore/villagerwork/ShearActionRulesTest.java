package dev.resivore.villagerwork;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ShearActionRulesTest {
    private static final ShearActionRules.Eligibility ELIGIBLE =
            new ShearActionRules.Eligibility(true, true, true, true, true, true);

    private static ShearActionRules.Decision decide(ShearActionRules.Phase phase, double distance,
                                                     boolean footingSafe, boolean clearLine) {
        return ShearActionRules.decide(phase, ELIGIBLE,
                new ShearActionRules.Geometry(distance, footingSafe, clearLine));
    }

    @Test void sheepInsideIdealDistanceStartsTelegraph() {
        assertEquals(new ShearActionRules.Decision(
                        ShearActionRules.Action.START_TELEGRAPH,
                        ShearActionRules.Reason.READY_TO_TELEGRAPH),
                decide(ShearActionRules.Phase.APPROACH,
                        ShearActionRules.IDEAL_APPROACH_DISTANCE, true, true));
    }

    @Test void approachClosesRemainingMarginBeforeStartingTelegraph() {
        assertEquals(new ShearActionRules.Decision(
                        ShearActionRules.Action.REPOSITION,
                        ShearActionRules.Reason.OUTSIDE_IDEAL_DISTANCE),
                decide(ShearActionRules.Phase.APPROACH,
                        ShearActionRules.IDEAL_APPROACH_DISTANCE + 0.01, true, true));
    }

    @Test void slightMovementWithinHardLimitContinuesTelegraph() {
        assertEquals(new ShearActionRules.Decision(
                        ShearActionRules.Action.CONTINUE_TELEGRAPH,
                        ShearActionRules.Reason.TELEGRAPH_IN_PROGRESS),
                decide(ShearActionRules.Phase.TELEGRAPH,
                        ShearActionRules.HARD_SAFETY_DISTANCE - 0.01, true, true));
    }

    @Test void movementBeyondHardLimitRepositionsTheSameRecoverableTarget() {
        assertEquals(new ShearActionRules.Decision(
                        ShearActionRules.Action.REPOSITION,
                        ShearActionRules.Reason.OUTSIDE_HARD_DISTANCE),
                decide(ShearActionRules.Phase.TELEGRAPH,
                        ShearActionRules.HARD_SAFETY_DISTANCE + 0.01, true, true));
    }

    @Test void movementBeyondRecoveryDistanceAbandonsTarget() {
        assertEquals(new ShearActionRules.Decision(
                        ShearActionRules.Action.ABANDON,
                        ShearActionRules.Reason.BEYOND_RECOVERY_DISTANCE),
                decide(ShearActionRules.Phase.TELEGRAPH,
                        ShearActionRules.RECOVERY_DISTANCE + 0.01, true, true));
    }

    @Test void lostClearLineAndUnsafeFootingRequestReposition() {
        assertEquals(new ShearActionRules.Decision(
                        ShearActionRules.Action.REPOSITION,
                        ShearActionRules.Reason.CLEAR_LINE_LOST),
                decide(ShearActionRules.Phase.TELEGRAPH, 1.7, true, false));
        assertEquals(new ShearActionRules.Decision(
                        ShearActionRules.Action.REPOSITION,
                        ShearActionRules.Reason.UNSAFE_FOOTING),
                decide(ShearActionRules.Phase.TELEGRAPH, 1.7, false, true));
    }

    @Test void meaningfulIneligibilityAbandonsInsteadOfRepositioning() {
        assertAbandoned(new ShearActionRules.Eligibility(false, true, true, true, true, true),
                ShearActionRules.Reason.WORK_INVALID);
        assertAbandoned(new ShearActionRules.Eligibility(true, false, true, true, true, true),
                ShearActionRules.Reason.SHEEP_DEAD);
        assertAbandoned(new ShearActionRules.Eligibility(true, true, false, true, true, true),
                ShearActionRules.Reason.SHEEP_IS_BABY);
        assertAbandoned(new ShearActionRules.Eligibility(true, true, true, false, true, true),
                ShearActionRules.Reason.SHEEP_NOT_SHEARABLE);
        assertAbandoned(new ShearActionRules.Eligibility(true, true, true, true, false, true),
                ShearActionRules.Reason.SHEEP_NOT_LOCAL);
        assertAbandoned(new ShearActionRules.Eligibility(true, true, true, true, true, false),
                ShearActionRules.Reason.OUTPUT_UNAVAILABLE);
    }

    @Test void actualShearRequiresCompletedTelegraphAndEveryFinalGeometryCheck() {
        var validGeometry = new ShearActionRules.Geometry(
                ShearActionRules.HARD_SAFETY_DISTANCE, true, true);
        assertNotEquals(ShearActionRules.Action.SHEAR,
                ShearActionRules.decide(ShearActionRules.Phase.APPROACH, ELIGIBLE, validGeometry).action());
        assertNotEquals(ShearActionRules.Action.SHEAR,
                ShearActionRules.decide(ShearActionRules.Phase.TELEGRAPH, ELIGIBLE, validGeometry).action());
        assertNotEquals(ShearActionRules.Action.SHEAR,
                decide(ShearActionRules.Phase.FINAL_VALIDATION, 2.0, false, true).action());
        assertNotEquals(ShearActionRules.Action.SHEAR,
                decide(ShearActionRules.Phase.FINAL_VALIDATION, 2.0, true, false).action());
        assertNotEquals(ShearActionRules.Action.SHEAR,
                decide(ShearActionRules.Phase.FINAL_VALIDATION,
                        ShearActionRules.HARD_SAFETY_DISTANCE + 0.01, true, true).action());
        assertEquals(ShearActionRules.Action.SHEAR,
                ShearActionRules.decide(ShearActionRules.Phase.FINAL_VALIDATION,
                        ELIGIBLE, validGeometry).action());
    }

    private static void assertAbandoned(ShearActionRules.Eligibility eligibility,
                                        ShearActionRules.Reason reason) {
        assertEquals(new ShearActionRules.Decision(ShearActionRules.Action.ABANDON, reason),
                ShearActionRules.decide(ShearActionRules.Phase.TELEGRAPH, eligibility,
                        new ShearActionRules.Geometry(1.7, true, true)));
    }
}
