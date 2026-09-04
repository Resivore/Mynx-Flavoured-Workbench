package dev.resivore.stacksarestackscontainerfixes;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Serializes alignment and admits at most one attempt for each configuration-listener epoch. */
final class ClientAlignmentCoordinator {
    private final ReferenceQueue<Object> collectedEpochs = new ReferenceQueue<>();
    private final Map<IdentityWeakReference, EpochState> attemptedEpochs = new HashMap<>();
    private Object activeEpoch;
    private long activeEpochNumber;
    private long nextEpochNumber;

    Attempt align(
            Object epoch,
            boolean onExpectedThread,
            Supplier<HolderReadiness> readinessProbe,
            Consumer<ReadyEpoch> alignment
    ) {
        Objects.requireNonNull(epoch, "epoch");
        Objects.requireNonNull(readinessProbe, "readinessProbe");
        Objects.requireNonNull(alignment, "alignment");

        Start start = begin(epoch);
        if (!start.started()) {
            return new Attempt(start.epochNumber(), start.outcome(), null);
        }

        HolderReadiness readiness = null;
        try {
            if (!onExpectedThread) {
                return finish(epoch, Outcome.WRONG_THREAD, null);
            }

            readiness = Objects.requireNonNull(readinessProbe.get(), "readiness");
            if (!readiness.ready()) {
                return finish(epoch, Outcome.HOLDERS_NOT_READY, readiness);
            }

            alignment.accept(new ReadyEpoch(start.epochNumber(), readiness));
            return finish(epoch, Outcome.ALIGNED, readiness);
        } catch (RuntimeException | Error failure) {
            finish(epoch, Outcome.ALIGNMENT_FAILED, readiness);
            throw failure;
        }
    }

    private synchronized Start begin(Object epoch) {
        expungeCollectedEpochs();
        if (activeEpoch != null) {
            return new Start(false, activeEpochNumber, Outcome.OVERLAPPING);
        }

        EpochState previous = attemptedEpochs.get(new IdentityWeakReference(epoch));
        if (previous != null) {
            Outcome duplicate = previous.outcome == Outcome.ALIGNED
                    ? Outcome.ALREADY_ALIGNED
                    : Outcome.EPOCH_ALREADY_REJECTED;
            return new Start(false, previous.epochNumber, duplicate);
        }

        long epochNumber = ++nextEpochNumber;
        activeEpoch = epoch;
        activeEpochNumber = epochNumber;
        attemptedEpochs.put(
                new IdentityWeakReference(epoch, collectedEpochs),
                new EpochState(epochNumber));
        return new Start(true, epochNumber, null);
    }

    private synchronized Attempt finish(Object epoch, Outcome outcome, HolderReadiness readiness) {
        if (activeEpoch != epoch) {
            throw new IllegalStateException("Alignment epoch changed while an attempt was active");
        }
        activeEpoch = null;
        activeEpochNumber = 0;
        EpochState state = attemptedEpochs.get(new IdentityWeakReference(epoch));
        if (state == null) {
            throw new IllegalStateException("Alignment epoch history disappeared while an attempt was active");
        }
        state.outcome = Objects.requireNonNull(outcome, "outcome");
        return new Attempt(state.epochNumber, outcome, readiness);
    }

    private void expungeCollectedEpochs() {
        for (IdentityWeakReference reference;
                (reference = (IdentityWeakReference) collectedEpochs.poll()) != null; ) {
            attemptedEpochs.remove(reference);
        }
    }

    enum Outcome {
        ALIGNED,
        ALREADY_ALIGNED,
        HOLDERS_NOT_READY,
        WRONG_THREAD,
        EPOCH_ALREADY_REJECTED,
        OVERLAPPING,
        ALIGNMENT_FAILED
    }

    record HolderReadiness(int boundHolders, int totalHolders) {
        HolderReadiness {
            if (boundHolders < 0 || totalHolders < 0 || boundHolders > totalHolders) {
                throw new IllegalArgumentException("Invalid holder readiness counts");
            }
        }

        boolean ready() {
            return boundHolders == totalHolders;
        }
    }

    record ReadyEpoch(long epochNumber, HolderReadiness readiness) {
    }

    record Attempt(long epochNumber, Outcome outcome, HolderReadiness readiness) {
    }

    private record Start(boolean started, long epochNumber, Outcome outcome) {
    }

    private static final class EpochState {
        private final long epochNumber;
        private Outcome outcome;

        private EpochState(long epochNumber) {
            this.epochNumber = epochNumber;
        }
    }

    private static final class IdentityWeakReference extends WeakReference<Object> {
        private final int identityHashCode;

        private IdentityWeakReference(Object referent) {
            super(referent);
            identityHashCode = System.identityHashCode(referent);
        }

        private IdentityWeakReference(Object referent, ReferenceQueue<Object> queue) {
            super(referent, queue);
            identityHashCode = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return identityHashCode;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof IdentityWeakReference reference)) {
                return false;
            }
            Object referent = get();
            return referent != null && referent == reference.get();
        }
    }
}
