package dev.resivore.strippingtogglefallingtreecompat;

import dev.resivore.strippingtogglefallingtreecompat.client.ToggleSyncTracker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToggleSyncTrackerTest {
    @Test
    void firstObservationAndChangesApplyAndSendExactlyOnce() {
        ToggleSyncTracker tracker = new ToggleSyncTracker();
        assertDecision(tracker.evaluate(false, false), true, true);
        tracker.markSent(false);
        assertDecision(tracker.evaluate(false, false), false, false);
        assertDecision(tracker.evaluate(true, false), true, true);
        tracker.markSent(true);
        assertDecision(tracker.evaluate(true, false), false, false);
        assertDecision(tracker.evaluate(false, false), true, true);
    }

    @Test
    void joinForcesTheCurrentStateEvenWhenUnchanged() {
        ToggleSyncTracker tracker = new ToggleSyncTracker();
        assertDecision(tracker.evaluate(true, false), true, true);
        tracker.markSent(true);
        assertDecision(tracker.evaluate(true, true), true, true);
    }

    @Test
    void unavailableChannelDoesNotConsumeThePendingNetworkChange() {
        ToggleSyncTracker tracker = new ToggleSyncTracker();
        assertDecision(tracker.evaluate(true, false), true, true);
        assertDecision(tracker.evaluate(true, false), false, true);
        tracker.markSent(true);
        assertDecision(tracker.evaluate(true, false), false, false);
    }

    @Test
    void unsentChangeRevertedToTheLastSentStateNeedsNoPacket() {
        ToggleSyncTracker tracker = new ToggleSyncTracker();
        assertDecision(tracker.evaluate(false, false), true, true);
        tracker.markSent(false);
        assertDecision(tracker.evaluate(true, false), true, true);
        assertDecision(tracker.evaluate(false, false), true, false);
    }

    @Test
    void repeatedUnchangedObservationsHaveNoWork() {
        ToggleSyncTracker tracker = new ToggleSyncTracker();
        assertDecision(tracker.evaluate(true, false), true, true);
        tracker.markSent(true);
        for (int i = 0; i < 1_000; i++) {
            assertDecision(tracker.evaluate(true, false), false, false);
        }
    }

    @Test
    void reconnectResetResendsTheCurrentState() {
        ToggleSyncTracker tracker = new ToggleSyncTracker();
        assertDecision(tracker.evaluate(true, false), true, true);
        tracker.markSent(true);
        assertDecision(tracker.evaluate(true, false), false, false);
        tracker.reset();
        assertDecision(tracker.evaluate(true, false), true, true);
    }

    private static void assertDecision(
            int decision,
            boolean applyNativeState,
            boolean sendPending) {
        boolean applies = (decision & ToggleSyncTracker.APPLY_NATIVE_STATE) != 0;
        boolean sends = (decision & ToggleSyncTracker.SEND_PENDING) != 0;
        if (applyNativeState) assertTrue(applies);
        else assertFalse(applies);
        if (sendPending) assertTrue(sends);
        else assertFalse(sends);
    }
}
