package dev.resivore.villagerwork;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SuccessfulFishDepositFeedbackTest {
    @Test void successfulTransferQueuesCodFlopForTheSameSixTickDelayAsShepherdFeedback() {
        SuccessfulFishDepositFeedback feedback = new SuccessfulFishDepositFeedback();
        BlockPos barrel = new BlockPos(1, 64, 1);

        feedback.scheduleIfSuccessful(2, barrel, 100);

        assertNull(feedback.pollDue(105));
        assertEquals(barrel, feedback.pollDue(106));
        assertEquals(0, feedback.pendingCount());
    }

    @Test void failedOrNoOpTransferNeverQueuesCodFlop() {
        SuccessfulFishDepositFeedback feedback = new SuccessfulFishDepositFeedback();
        feedback.scheduleIfSuccessful(0, new BlockPos(1, 64, 1), 100);

        assertEquals(0, feedback.pendingCount());
        assertNull(feedback.pollDue(200));
    }

    @Test void cancellationSuppressesAnAlreadyQueuedCodFlop() {
        SuccessfulFishDepositFeedback feedback = new SuccessfulFishDepositFeedback();
        feedback.scheduleIfSuccessful(1, new BlockPos(1, 64, 1), 100);

        feedback.cancel();

        assertNull(feedback.pollDue(200));
    }

    @Test void codFlopUsesTheRequestedModestIncrease() {
        assertEquals(0.90F, SuccessfulFishDepositFeedback.COD_FLOP_VOLUME, 0.000001F);
    }
}
