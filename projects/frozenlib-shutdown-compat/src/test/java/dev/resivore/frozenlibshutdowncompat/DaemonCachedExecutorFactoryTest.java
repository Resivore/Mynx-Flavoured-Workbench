package dev.resivore.frozenlibshutdowncompat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class DaemonCachedExecutorFactoryTest {
    @Test
    void createsDefaultStyleDaemonWorkers() throws Exception {
        ExecutorService executor = DaemonCachedExecutorFactory.create();
        try {
            Future<ThreadSnapshot> result = executor.submit(() -> {
                Thread thread = Thread.currentThread();
                return new ThreadSnapshot(thread.isDaemon(), thread.getName(), thread.getPriority());
            });
            ThreadSnapshot snapshot = result.get(5, TimeUnit.SECONDS);

            assertTrue(snapshot.daemon());
            assertTrue(snapshot.name().matches("pool-\\d+-thread-\\d+"));
            assertEquals(Thread.NORM_PRIORITY, snapshot.priority());
        } finally {
            executor.shutdownNow();
        }
    }

    private record ThreadSnapshot(boolean daemon, String name, int priority) {
    }
}
