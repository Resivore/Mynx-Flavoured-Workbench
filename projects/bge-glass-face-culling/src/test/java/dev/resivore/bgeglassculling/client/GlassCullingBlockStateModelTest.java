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
    void ordinaryDirectionalUpstreamCullingStillReceivesItsDirection() {
        AtomicReference<Direction> received = new AtomicReference<>();
        Predicate<Direction> coherent = GlassCullingBlockStateModel.coherentCullTest(direction -> {
            received.set(direction);
            return true;
        }, null, null, null);

        assertTrue(coherent.test(Direction.EAST));
        assertEquals(Direction.EAST, received.get());
    }
}
