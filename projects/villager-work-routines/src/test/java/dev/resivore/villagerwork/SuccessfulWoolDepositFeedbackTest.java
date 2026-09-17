package dev.resivore.villagerwork;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuccessfulWoolDepositFeedbackTest {
    @Test void successfulTransferDelaysTheDepositMomentButPreservesEveryAcceptedBarrel() {
        SuccessfulWoolDepositFeedback feedback = new SuccessfulWoolDepositFeedback();
        List<BlockPos> barrels = List.of(new BlockPos(1, 64, 1), new BlockPos(2, 64, 1));

        feedback.scheduleIfSuccessful(3, barrels, 100);

        assertTrue(feedback.pollDue(100 + SuccessfulWoolDepositFeedback.DELAY_TICKS - 1).isEmpty());
        assertEquals(barrels, feedback.pollDue(100 + SuccessfulWoolDepositFeedback.DELAY_TICKS));
    }

    @Test void failedOrNoOpTransferNeverSchedulesADelayedCue() {
        SuccessfulWoolDepositFeedback feedback = new SuccessfulWoolDepositFeedback();
        feedback.scheduleIfSuccessful(0, List.of(new BlockPos(1, 64, 1)), 100);

        assertEquals(0, feedback.pendingCount());
        assertTrue(feedback.pollDue(200).isEmpty());
    }

    @Test void cancellationSuppressesAnAlreadyScheduledDelayedCue() {
        SuccessfulWoolDepositFeedback feedback = new SuccessfulWoolDepositFeedback();
        feedback.scheduleIfSuccessful(1, List.of(new BlockPos(1, 64, 1)), 100);

        feedback.cancel();

        assertTrue(feedback.pollDue(200).isEmpty());
    }
}
