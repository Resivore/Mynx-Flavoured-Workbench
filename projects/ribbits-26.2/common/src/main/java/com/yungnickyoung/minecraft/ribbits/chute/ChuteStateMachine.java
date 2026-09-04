package com.yungnickyoung.minecraft.ribbits.chute;

import java.util.Optional;

/** Pure transient state machine shared by server behavior and focused tests. */
public final class ChuteStateMachine {
    public static final int MIN_ACCEPTED_PACKET_INTERVAL_TICKS = 2;
    public static final int PENDING_LIFETIME_TICKS = 60;

    private long highestSequence = Long.MIN_VALUE;
    private long lastAcceptedTick = Long.MIN_VALUE;
    private long pendingSequence = Long.MIN_VALUE;
    private long pendingExpiryTick = Long.MIN_VALUE;
    private boolean pending;
    private boolean deployed;

    /**
     * Processes one request. Sequence authority advances before all other validation.
     * A stale dimension or rate limit rejects only the request; an invalid player state also
     * clears motion state because the Chute may no longer remain open.
     */
    public ChuteAckState press(
            long sequence,
            long serverTick,
            boolean dimensionMatches,
            boolean playerStateValid,
            double verticalVelocity
    ) {
        if (sequence <= this.highestSequence) {
            return ChuteAckState.REJECTED;
        }
        this.highestSequence = sequence;

        if (!dimensionMatches) {
            return ChuteAckState.REJECTED;
        }
        if (!playerStateValid) {
            this.clearMotionState();
            return ChuteAckState.REJECTED;
        }
        if (this.lastAcceptedTick != Long.MIN_VALUE
                && serverTick - this.lastAcceptedTick < MIN_ACCEPTED_PACKET_INTERVAL_TICKS) {
            return ChuteAckState.REJECTED;
        }
        this.lastAcceptedTick = serverTick;

        if (this.deployed) {
            return ChuteAckState.DEPLOYED;
        }
        if (verticalVelocity <= 0.0D) {
            this.pending = false;
            this.deployed = true;
            return ChuteAckState.DEPLOYED;
        }

        this.pending = true;
        this.pendingSequence = sequence;
        this.pendingExpiryTick = serverTick + PENDING_LIFETIME_TICKS;
        return ChuteAckState.PENDING;
    }

    /** Advances pending deployment and returns a follow-up acknowledgement when one is due. */
    public Optional<Acknowledgement> tick(long serverTick, boolean playerStateValid, double verticalVelocity) {
        if (!playerStateValid) {
            long rejectedSequence = this.pending ? this.pendingSequence : Long.MIN_VALUE;
            this.clearMotionState();
            return rejectedSequence == Long.MIN_VALUE
                    ? Optional.empty()
                    : Optional.of(new Acknowledgement(rejectedSequence, ChuteAckState.REJECTED));
        }
        if (!this.pending) {
            return Optional.empty();
        }
        if (serverTick >= this.pendingExpiryTick) {
            long expiredSequence = this.pendingSequence;
            this.clearMotionState();
            return Optional.of(new Acknowledgement(expiredSequence, ChuteAckState.REJECTED));
        }
        if (verticalVelocity <= 0.0D) {
            long deployedSequence = this.pendingSequence;
            this.pending = false;
            this.deployed = true;
            return Optional.of(new Acknowledgement(deployedSequence, ChuteAckState.DEPLOYED));
        }
        return Optional.empty();
    }

    public void clearMotionState() {
        this.pending = false;
        this.pendingSequence = Long.MIN_VALUE;
        this.pendingExpiryTick = Long.MIN_VALUE;
        this.deployed = false;
    }

    public long highestSequence() {
        return this.highestSequence;
    }

    public long lastAcceptedTick() {
        return this.lastAcceptedTick;
    }

    public long pendingExpiryTick() {
        return this.pendingExpiryTick;
    }

    public boolean isPending() {
        return this.pending;
    }

    public boolean isDeployed() {
        return this.deployed;
    }

    public record Acknowledgement(long sequence, ChuteAckState state) {
    }
}
