package dev.resivore.stacksarestackscontainerfixes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ClientAlignmentCoordinatorTest {
    @Test
    void completeReadinessProbeAlwaysPrecedesMutation() {
        ClientAlignmentCoordinator coordinator = new ClientAlignmentCoordinator();
        List<String> order = new ArrayList<>();

        ClientAlignmentCoordinator.Attempt attempt = coordinator.align(
                new Object(),
                true,
                () -> {
                    order.add("readiness");
                    return new ClientAlignmentCoordinator.HolderReadiness(2, 2);
                },
                ready -> order.add("alignment"));

        assertEquals(List.of("readiness", "alignment"), order);
        assertEquals(ClientAlignmentCoordinator.Outcome.ALIGNED, attempt.outcome());
        assertEquals(new ClientAlignmentCoordinator.HolderReadiness(2, 2), attempt.readiness());
    }

    @Test
    void failedReadinessCannotPartiallyMutateOrRetryTheSameEpoch() {
        ClientAlignmentCoordinator coordinator = new ClientAlignmentCoordinator();
        Object epoch = new Object();
        AtomicInteger probes = new AtomicInteger();
        AtomicInteger mutations = new AtomicInteger();

        ClientAlignmentCoordinator.Attempt failed = coordinator.align(
                epoch,
                true,
                () -> {
                    probes.incrementAndGet();
                    return new ClientAlignmentCoordinator.HolderReadiness(1, 2);
                },
                ready -> mutations.incrementAndGet());
        ClientAlignmentCoordinator.Attempt duplicate = coordinator.align(
                epoch,
                true,
                () -> {
                    probes.incrementAndGet();
                    return new ClientAlignmentCoordinator.HolderReadiness(2, 2);
                },
                ready -> mutations.incrementAndGet());

        assertEquals(ClientAlignmentCoordinator.Outcome.HOLDERS_NOT_READY, failed.outcome());
        assertEquals(ClientAlignmentCoordinator.Outcome.EPOCH_ALREADY_REJECTED, duplicate.outcome());
        assertEquals(1, probes.get());
        assertEquals(0, mutations.get());
    }

    @Test
    void duplicateSuccessSkipsButFreshReconnectEpochAligns() {
        ClientAlignmentCoordinator coordinator = new ClientAlignmentCoordinator();
        Object firstConnection = new Object();
        Object reconnect = new Object();
        AtomicInteger mutations = new AtomicInteger();

        ClientAlignmentCoordinator.Attempt first = alignReady(coordinator, firstConnection, mutations);
        ClientAlignmentCoordinator.Attempt duplicate = alignReady(coordinator, firstConnection, mutations);
        ClientAlignmentCoordinator.Attempt second = alignReady(coordinator, reconnect, mutations);
        ClientAlignmentCoordinator.Attempt oldEpochAgain =
                alignReady(coordinator, firstConnection, mutations);

        assertEquals(ClientAlignmentCoordinator.Outcome.ALIGNED, first.outcome());
        assertEquals(ClientAlignmentCoordinator.Outcome.ALREADY_ALIGNED, duplicate.outcome());
        assertEquals(ClientAlignmentCoordinator.Outcome.ALIGNED, second.outcome());
        assertEquals(ClientAlignmentCoordinator.Outcome.ALREADY_ALIGNED, oldEpochAgain.outcome());
        assertEquals(1, first.epochNumber());
        assertEquals(1, duplicate.epochNumber());
        assertEquals(2, second.epochNumber());
        assertEquals(1, oldEpochAgain.epochNumber());
        assertEquals(2, mutations.get());
    }

    @Test
    void wrongThreadFailsClosedBeforeReadinessOrMutation() {
        ClientAlignmentCoordinator coordinator = new ClientAlignmentCoordinator();
        AtomicInteger probes = new AtomicInteger();
        AtomicInteger mutations = new AtomicInteger();

        ClientAlignmentCoordinator.Attempt result = coordinator.align(
                new Object(),
                false,
                () -> {
                    probes.incrementAndGet();
                    return new ClientAlignmentCoordinator.HolderReadiness(1, 1);
                },
                ready -> mutations.incrementAndGet());

        assertEquals(ClientAlignmentCoordinator.Outcome.WRONG_THREAD, result.outcome());
        assertEquals(0, probes.get());
        assertEquals(0, mutations.get());
    }

    @Test
    void nestedOrConcurrentEpochCannotOverlapAnActiveAlignment() {
        ClientAlignmentCoordinator coordinator = new ClientAlignmentCoordinator();
        Object first = new Object();
        Object second = new Object();
        AtomicInteger nestedMutations = new AtomicInteger();
        AtomicReference<ClientAlignmentCoordinator.Attempt> nested = new AtomicReference<>();

        ClientAlignmentCoordinator.Attempt outer = coordinator.align(
                first,
                true,
                () -> new ClientAlignmentCoordinator.HolderReadiness(1, 1),
                ready -> nested.set(coordinator.align(
                        second,
                        true,
                        () -> new ClientAlignmentCoordinator.HolderReadiness(1, 1),
                        ignored -> nestedMutations.incrementAndGet())));

        assertEquals(ClientAlignmentCoordinator.Outcome.ALIGNED, outer.outcome());
        assertEquals(ClientAlignmentCoordinator.Outcome.OVERLAPPING, nested.get().outcome());
        assertEquals(0, nestedMutations.get());

        ClientAlignmentCoordinator.Attempt later = alignReady(coordinator, second, nestedMutations);
        assertEquals(ClientAlignmentCoordinator.Outcome.ALIGNED, later.outcome());
        assertEquals(1, nestedMutations.get());
    }

    @Test
    void thrownAlignmentIsRememberedAndNeverRetriedInTheSameEpoch() {
        ClientAlignmentCoordinator coordinator = new ClientAlignmentCoordinator();
        Object epoch = new Object();
        AtomicInteger mutations = new AtomicInteger();

        assertThrows(ExpectedAlignmentFailure.class, () -> coordinator.align(
                epoch,
                true,
                () -> new ClientAlignmentCoordinator.HolderReadiness(1, 1),
                ready -> {
                    mutations.incrementAndGet();
                    throw new ExpectedAlignmentFailure();
                }));

        ClientAlignmentCoordinator.Attempt duplicate = alignReady(coordinator, epoch, mutations);
        assertEquals(ClientAlignmentCoordinator.Outcome.EPOCH_ALREADY_REJECTED, duplicate.outcome());
        assertEquals(1, mutations.get());
    }

    private static ClientAlignmentCoordinator.Attempt alignReady(
            ClientAlignmentCoordinator coordinator,
            Object epoch,
            AtomicInteger mutations
    ) {
        return coordinator.align(
                epoch,
                true,
                () -> new ClientAlignmentCoordinator.HolderReadiness(1, 1),
                ready -> mutations.incrementAndGet());
    }

    private static final class ExpectedAlignmentFailure extends RuntimeException {
    }
}
