package dev.resivore.ribbitsxaeroicons;

import java.util.Objects;
import java.util.function.Consumer;

/** Executes cleanup on both successful and exceptional render exits and converts failure to false. */
public final class RenderStateGuard {
    private RenderStateGuard() {
    }

    public static boolean run(
            ThrowingRunnable render,
            ThrowingRunnable restore,
            Consumer<Throwable> failureSink) {
        Objects.requireNonNull(render, "render");
        Objects.requireNonNull(restore, "restore");
        Objects.requireNonNull(failureSink, "failureSink");

        Throwable failure = null;
        try {
            render.run();
        } catch (Throwable thrown) {
            failure = thrown;
        } finally {
            try {
                restore.run();
            } catch (Throwable thrown) {
                if (failure == null) {
                    failure = thrown;
                } else {
                    failure.addSuppressed(thrown);
                }
            }
        }

        if (failure == null) {
            return true;
        }
        try {
            failureSink.accept(failure);
        } catch (Throwable diagnosticFailure) {
            failure.addSuppressed(diagnosticFailure);
        }
        return false;
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
