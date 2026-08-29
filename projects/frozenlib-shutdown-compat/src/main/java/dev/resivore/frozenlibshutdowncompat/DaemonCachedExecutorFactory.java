package dev.resivore.frozenlibshutdowncompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class DaemonCachedExecutorFactory {
    private DaemonCachedExecutorFactory() {
    }

    public static ExecutorService create() {
        ThreadFactory defaultFactory = Executors.defaultThreadFactory();
        ThreadFactory daemonFactory = task -> {
            Thread thread = defaultFactory.newThread(task);
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newCachedThreadPool(daemonFactory);
    }
}
