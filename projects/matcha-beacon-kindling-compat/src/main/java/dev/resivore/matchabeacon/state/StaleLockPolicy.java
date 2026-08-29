package dev.resivore.matchabeacon.state;

import java.util.Objects;
import java.util.Optional;

/** Join-time policy for a legacy Matcha SummonedTrader tag. */
public final class StaleLockPolicy {
    private StaleLockPolicy() {
    }

    public static boolean shouldClearMatchaLock(
            boolean hasSummonedTraderTag,
            Optional<SummonRecord> trackedSummon
    ) {
        Objects.requireNonNull(trackedSummon, "trackedSummon");
        return hasSummonedTraderTag && trackedSummon.isEmpty();
    }
}
