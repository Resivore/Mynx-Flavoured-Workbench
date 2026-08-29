package com.yungnickyoung.minecraft.ribbits.network;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PendingEntityActionsTest {
    private static final UUID FIRST = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SECOND = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void queuedActionsReplayInArrivalOrderOnceEntityLoads() {
        PendingEntityActions<String> actions = new PendingEntityActions<>();
        UUID entityId = FIRST;
        List<String> observed = new ArrayList<>();

        actions.executeOrQueue(entityId, null, entity -> observed.add("first:" + entity));
        actions.executeOrQueue(entityId, null, entity -> observed.add("second:" + entity));

        assertEquals(2, actions.pendingActionCount(entityId));
        assertEquals(List.of(), observed);

        actions.onEntityAvailable(entityId, "ribbit");

        assertEquals(List.of("first:ribbit", "second:ribbit"), observed);
        assertEquals(0, actions.pendingActionCount(entityId));
    }

    @Test
    void availableEntityExecutesImmediatelyWithoutQueueing() {
        PendingEntityActions<String> actions = new PendingEntityActions<>();
        UUID entityId = FIRST;
        List<String> observed = new ArrayList<>();

        actions.executeOrQueue(entityId, "ribbit", observed::add);

        assertEquals(List.of("ribbit"), observed);
        assertEquals(0, actions.pendingActionCount(entityId));
    }

    @Test
    void queuedIdDoesNotPreventImmediateExecutionForAnotherId() {
        PendingEntityActions<String> actions = new PendingEntityActions<>();
        UUID missingId = FIRST;
        UUID availableId = SECOND;
        List<String> observed = new ArrayList<>();

        actions.executeOrQueue(missingId, null, entity -> observed.add("missing:" + entity));
        actions.executeOrQueue(availableId, "guitar-ribbit", observed::add);

        assertEquals(List.of("guitar-ribbit"), observed);
        assertEquals(1, actions.pendingActionCount(missingId));
        assertEquals(0, actions.pendingActionCount(availableId));
    }

    @Test
    void disconnectClearPreventsStaleReplay() {
        PendingEntityActions<String> actions = new PendingEntityActions<>();
        UUID entityId = FIRST;
        AtomicInteger executions = new AtomicInteger();

        actions.executeOrQueue(entityId, null, entity -> executions.incrementAndGet());
        actions.clear();
        actions.onEntityAvailable(entityId, "new-session-ribbit");

        assertEquals(0, executions.get());
        assertEquals(0, actions.pendingActionCount(entityId));
    }

    @Test
    void drainedActionsDoNotReplayOnDuplicateLoadNotification() {
        PendingEntityActions<String> actions = new PendingEntityActions<>();
        UUID entityId = FIRST;
        AtomicInteger executions = new AtomicInteger();

        actions.executeOrQueue(entityId, null, entity -> executions.incrementAndGet());
        actions.onEntityAvailable(entityId, "ribbit");
        actions.onEntityAvailable(entityId, "ribbit");

        assertEquals(1, executions.get());
    }
}
