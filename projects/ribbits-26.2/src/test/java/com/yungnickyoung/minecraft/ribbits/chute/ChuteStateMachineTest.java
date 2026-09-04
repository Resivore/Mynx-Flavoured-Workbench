package com.yungnickyoung.minecraft.ribbits.chute;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChuteStateMachineTest {
    @Test
    void descendingDeploysImmediatelyAndAnotherPressNeverClosesIt() {
        ChuteStateMachine state = new ChuteStateMachine();

        assertEquals(ChuteAckState.DEPLOYED, state.press(1L, 10L, true, true, -0.01D));
        assertTrue(state.isDeployed());
        assertEquals(ChuteAckState.DEPLOYED, state.press(2L, 12L, true, true, -0.25D));
        assertTrue(state.isDeployed());
    }

    @Test
    void ascendingPressBuffersUntilApexAndAcknowledgesOriginalSequence() {
        ChuteStateMachine state = new ChuteStateMachine();

        assertEquals(ChuteAckState.PENDING, state.press(41L, 100L, true, true, 0.42D));
        assertEquals(160L, state.pendingExpiryTick());
        assertTrue(state.tick(125L, true, 0.01D).isEmpty());

        ChuteStateMachine.Acknowledgement acknowledgement = state.tick(126L, true, 0.0D).orElseThrow();
        assertEquals(41L, acknowledgement.sequence());
        assertEquals(ChuteAckState.DEPLOYED, acknowledgement.state());
        assertFalse(state.isPending());
        assertTrue(state.isDeployed());
    }

    @Test
    void pendingExpiresAtSixtyTicksAndNeverDeploysLate() {
        ChuteStateMachine state = new ChuteStateMachine();
        assertEquals(ChuteAckState.PENDING, state.press(7L, 20L, true, true, 0.2D));

        assertTrue(state.tick(79L, true, 0.2D).isEmpty());
        ChuteStateMachine.Acknowledgement acknowledgement = state.tick(80L, true, -0.5D).orElseThrow();
        assertEquals(7L, acknowledgement.sequence());
        assertEquals(ChuteAckState.REJECTED, acknowledgement.state());
        assertFalse(state.isPending());
        assertFalse(state.isDeployed());
    }

    @Test
    void sequenceAdvancesBeforeDimensionValidityAndRejectsReplayOrReordering() {
        ChuteStateMachine state = new ChuteStateMachine();

        assertEquals(ChuteAckState.REJECTED, state.press(10L, 1L, false, true, -1.0D));
        assertEquals(10L, state.highestSequence());
        assertEquals(ChuteAckState.REJECTED, state.press(10L, 2L, true, true, -1.0D));
        assertEquals(ChuteAckState.REJECTED, state.press(9L, 3L, true, true, -1.0D));
        assertFalse(state.isDeployed());
    }

    @Test
    void acceptsAtMostOneRequestPerTwoServerTicksWithoutClosingDeployment() {
        ChuteStateMachine state = new ChuteStateMachine();

        assertEquals(ChuteAckState.DEPLOYED, state.press(1L, 30L, true, true, -0.2D));
        assertEquals(ChuteAckState.REJECTED, state.press(2L, 31L, true, true, -0.2D));
        assertTrue(state.isDeployed());
        assertEquals(ChuteAckState.DEPLOYED, state.press(3L, 32L, true, true, -0.2D));
        assertEquals(32L, state.lastAcceptedTick());
    }

    @Test
    void invalidPlayerStateClearsPendingAndDeployedState() {
        ChuteStateMachine pending = new ChuteStateMachine();
        pending.press(3L, 0L, true, true, 0.3D);
        assertEquals(ChuteAckState.REJECTED, pending.tick(1L, false, 0.3D).orElseThrow().state());
        assertFalse(pending.isPending());

        ChuteStateMachine deployed = new ChuteStateMachine();
        deployed.press(3L, 0L, true, true, -0.3D);
        assertTrue(deployed.tick(1L, false, -0.3D).isEmpty());
        assertFalse(deployed.isDeployed());
    }
}
