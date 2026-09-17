package dev.resivore.villagerwork;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;

/**
 * Delays only the fish-placement portion of a positive claimed-barrel transfer.  The barrel
 * interaction remains immediate, and cancellation clears this bounded per-villager queue.
 */
final class SuccessfulFishDepositFeedback {
    static final int DELAY_TICKS = SuccessfulWoolDepositFeedback.DELAY_TICKS;
    static final float COD_FLOP_VOLUME = 0.90F;

    private record Pending(BlockPos barrel, int dueAt) {}

    private final ArrayDeque<Pending> pending = new ArrayDeque<>();

    void scheduleIfSuccessful(int transferred, BlockPos barrel, int currentTick) {
        if (transferred > 0) pending.addLast(new Pending(barrel.immutable(), currentTick + DELAY_TICKS));
    }

    BlockPos pollDue(int currentTick) {
        return !pending.isEmpty() && pending.peekFirst().dueAt <= currentTick
                ? pending.removeFirst().barrel() : null;
    }

    void cancel() {
        pending.clear();
    }

    int pendingCount() {
        return pending.size();
    }
}
