package dev.resivore.bgeglassculling.client;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GlassCullingBlockStateModelTest {
    @Test
    void nullDirectionDelegatesAndPreservesAnUpstreamCull() {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Direction> received = new AtomicReference<>(Direction.NORTH);
        Predicate<Direction> coherent = GlassCullingBlockStateModel.coherentCullTest(direction -> {
            calls.incrementAndGet();
            received.set(direction);
            return true;
        }, null, null, null);

        assertDoesNotThrow(() -> assertTrue(coherent.test(null)));
        assertEquals(1, calls.get());
        assertNull(received.get());
    }

    @Test
    void nullDirectionPreservesAnUpstreamVisibleResultWithoutReadingABoundary() {
        AtomicInteger calls = new AtomicInteger();
        Predicate<Direction> coherent = GlassCullingBlockStateModel.coherentCullTest(direction -> {
            calls.incrementAndGet();
            assertNull(direction);
            return false;
        }, null, null, null);

        // Null state, position, and level make any neighbor read or BGE boundary evaluation fail.
        assertDoesNotThrow(() -> assertFalse(coherent.test(null)));
        assertEquals(1, calls.get());
    }

    @Test
    void noneligibleDirectionalBoundaryPreservesAnUpstreamCull() {
        AtomicReference<Direction> received = new AtomicReference<>();
        boolean result = GlassCullingBlockStateModel.cullDecision(direction -> {
            received.set(direction);
            return true;
        }, Direction.EAST, direction -> false);

        assertTrue(result);
        assertEquals(Direction.EAST, received.get());
    }

    @Test
    void noneligibleDirectionalBoundaryPreservesAnUpstreamVisibleResult() {
        assertFalse(GlassCullingBlockStateModel.cullDecision(direction -> false,
                Direction.EAST, direction -> false));
    }

    @Test
    void eligibleDirectionalBoundaryBypassesAnUpstreamWholeFaceCull() {
        AtomicInteger upstreamCalls = new AtomicInteger();
        AtomicReference<Direction> evaluated = new AtomicReference<>();

        boolean result = GlassCullingBlockStateModel.cullDecision(direction -> {
            upstreamCalls.incrementAndGet();
            return true;
        }, Direction.EAST, direction -> {
            evaluated.set(direction);
            return true;
        });

        assertFalse(result);
        assertEquals(Direction.EAST, evaluated.get());
        assertEquals(0, upstreamCalls.get());
    }
}
