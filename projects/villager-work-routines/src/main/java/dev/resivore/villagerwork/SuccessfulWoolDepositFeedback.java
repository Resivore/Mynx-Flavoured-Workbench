package dev.resivore.villagerwork;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Keeps the second half of a successful Shepherd barrel interaction separate from the initial
 * barrel-open cue.  Nothing is scheduled until storage has reported an actual accepted transfer.
 */
final class SuccessfulWoolDepositFeedback {
    static final int DELAY_TICKS = 6;

    private record Pending(BlockPos barrel, int dueAt) {}

    private final ArrayDeque<Pending> pending = new ArrayDeque<>();

    void scheduleIfSuccessful(int transferred, Collection<BlockPos> barrels, int currentTick) {
        if (transferred <= 0) return;
        for (BlockPos barrel : barrels) pending.addLast(new Pending(barrel.immutable(), currentTick + DELAY_TICKS));
    }

    List<BlockPos> pollDue(int currentTick) {
        List<BlockPos> due = new ArrayList<>();
        while (!pending.isEmpty() && pending.peekFirst().dueAt <= currentTick)
            due.add(pending.removeFirst().barrel());
        return List.copyOf(due);
    }

    void cancel() {
        pending.clear();
    }

    int pendingCount() {
        return pending.size();
    }
}
