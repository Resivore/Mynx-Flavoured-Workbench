package dev.resivore.matchabeacon.state;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Result of advancing one exact player's durable record by at most one active tick. */
public record LifecycleStep(
        UUID playerId,
        UUID markerId,
        LifecycleDecision decision,
        Optional<SummonRecord> nextRecord
) {
    public LifecycleStep {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(markerId, "markerId");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(nextRecord, "nextRecord");
        nextRecord.ifPresent(record -> {
            if (!playerId.equals(record.playerId())) {
                throw new IllegalArgumentException("Lifecycle result changed player ownership");
            }
            if (!markerId.equals(record.markerId())) {
                throw new IllegalArgumentException("Lifecycle result changed marker ownership");
            }
        });
        boolean terminal = decision == LifecycleDecision.CANCEL || decision == LifecycleDecision.DEPART;
        if (terminal == nextRecord.isPresent()) {
            throw new IllegalArgumentException("Only CANCEL and DEPART may remove a summon record");
        }
    }

    public boolean terminal() {
        return nextRecord.isEmpty();
    }
}
