package dev.resivore.matchabeacon.state;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Pure active-time lifecycle rules. World inspection and side effects remain runtime-owned. */
public final class SummonLifecycle {
    private SummonLifecycle() {
    }

    public static LifecycleStep advance(SummonRecord record, LifecycleObservation observation) {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(observation, "observation");

        if (!observation.playerOnline() || !observation.locationProcessable()) {
            return keep(record, LifecycleDecision.PAUSE);
        }
        if (!observation.campfireValid() || !observation.exactMarkerPresent()) {
            return remove(record, LifecycleDecision.CANCEL);
        }

        if (record.phase() == SummonPhase.APPROACH) {
            if (record.approachTicksRemaining() == 0) {
                return keep(record, LifecycleDecision.WAITING_FOR_VISIT);
            }
            int remaining = record.approachTicksRemaining() - 1;
            SummonRecord updated = record.withApproachTicksRemaining(remaining);
            return keep(updated, remaining == 0 ? LifecycleDecision.ARRIVE : LifecycleDecision.ADVANCE);
        }

        if (!observation.exactTraderPresent()) {
            return keep(record, LifecycleDecision.PAUSE);
        }
        int remaining = record.visitTicksRemaining() - 1;
        if (remaining == 0) {
            return remove(record, LifecycleDecision.DEPART);
        }
        return keep(record.withVisitTicksRemaining(remaining), LifecycleDecision.ADVANCE);
    }

    /** Claims the exact trader created for a single ARRIVE decision and starts its full visit. */
    public static SummonRecord beginVisit(SummonRecord arrivedApproach, UUID exactTraderId) {
        Objects.requireNonNull(arrivedApproach, "arrivedApproach");
        return arrivedApproach.beginVisit(exactTraderId);
    }

    private static LifecycleStep keep(SummonRecord record, LifecycleDecision decision) {
        return new LifecycleStep(record.playerId(), record.markerId(), decision, Optional.of(record));
    }

    private static LifecycleStep remove(SummonRecord record, LifecycleDecision decision) {
        return new LifecycleStep(record.playerId(), record.markerId(), decision, Optional.empty());
    }
}
