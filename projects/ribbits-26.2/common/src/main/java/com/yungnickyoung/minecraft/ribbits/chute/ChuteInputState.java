package com.yungnickyoung.minecraft.ribbits.chute;

import java.util.OptionalLong;

/** Connection-local physical-key edge detector, isolated from synthetic Minecraft input. */
public final class ChuteInputState {
    private boolean previousPhysicalJump;
    private long nextSequence = 1L;

    /**
     * Samples the physical key once. {@code maySend} must already include airborne, focus,
     * screen, connection, and equipment checks. The physical state is remembered even when
     * sending is disallowed so closing a screen or walking off a ledge cannot invent an edge.
     */
    public OptionalLong sample(boolean physicalJump, boolean maySend) {
        boolean risingEdge = physicalJump && !this.previousPhysicalJump;
        this.previousPhysicalJump = physicalJump;
        if (!risingEdge || !maySend) {
            return OptionalLong.empty();
        }

        long sequence = this.nextSequence;
        if (this.nextSequence != Long.MAX_VALUE) {
            this.nextSequence++;
        }
        return OptionalLong.of(sequence);
    }

    public void reset() {
        this.previousPhysicalJump = false;
        this.nextSequence = 1L;
    }

    public boolean previousPhysicalJump() {
        return this.previousPhysicalJump;
    }

    public long nextSequence() {
        return this.nextSequence;
    }
}
