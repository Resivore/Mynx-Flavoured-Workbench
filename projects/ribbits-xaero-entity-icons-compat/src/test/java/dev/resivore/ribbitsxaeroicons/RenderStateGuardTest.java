package dev.resivore.ribbitsxaeroicons;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RenderStateGuardTest {
    @Test
    void successfulRenderRestoresStateAndDoesNotReportFailure() {
        List<String> order = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();

        boolean result = assertDoesNotThrow(() -> RenderStateGuard.run(
                () -> order.add("render"),
                () -> order.add("restore"),
                failures::add));

        assertTrue(result);
        assertEquals(List.of("render", "restore"), order);
        assertEquals(List.of(), failures);
    }

    @Test
    void exceptionalRenderStillRestoresAndFailsClosedThroughTheSink() {
        List<String> order = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();
        RuntimeException failure = new RuntimeException("render failed");

        boolean result = assertDoesNotThrow(() -> RenderStateGuard.run(
                () -> {
                    order.add("render");
                    throw failure;
                },
                () -> order.add("restore"),
                failures::add));

        assertFalse(result);
        assertEquals(List.of("render", "restore"), order);
        assertEquals(1, failures.size());
        assertSame(failure, failures.getFirst());
    }

    @Test
    void restorationFailureIsReportedInsteadOfEscapingTheIconPath() {
        List<Throwable> failures = new ArrayList<>();
        RuntimeException restoreFailure = new RuntimeException("restore failed");

        assertDoesNotThrow(() -> RenderStateGuard.run(
                () -> { },
                () -> { throw restoreFailure; },
                failures::add));

        assertEquals(1, failures.size());
        assertSame(restoreFailure, failures.getFirst());
    }

    @Test
    void renderFailureRetainsRestorationFailureAsSuppressedEvidence() {
        List<Throwable> failures = new ArrayList<>();
        RuntimeException renderFailure = new RuntimeException("render failed");
        RuntimeException restoreFailure = new RuntimeException("restore failed");

        assertDoesNotThrow(() -> RenderStateGuard.run(
                () -> { throw renderFailure; },
                () -> { throw restoreFailure; },
                failures::add));

        assertEquals(1, failures.size());
        assertSame(renderFailure, failures.getFirst());
        assertEquals(1, renderFailure.getSuppressed().length);
        assertSame(restoreFailure, renderFailure.getSuppressed()[0]);
    }

    @Test
    void diagnosticFailureCannotEscapeTheFailClosedRenderPath() {
        RuntimeException renderFailure = new RuntimeException("render failed");
        RuntimeException diagnosticFailure = new RuntimeException("logging failed");

        assertDoesNotThrow(() -> RenderStateGuard.run(
                () -> { throw renderFailure; },
                () -> { },
                ignored -> { throw diagnosticFailure; }));

        assertEquals(1, renderFailure.getSuppressed().length);
        assertSame(diagnosticFailure, renderFailure.getSuppressed()[0]);
    }
}
