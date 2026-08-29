package dev.aero.shulkertrowel.sound;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementSoundBroadcastScopeTest {
    @Test
    void inactiveScopePreservesVanillaExcludedSource() {
        Object player = new Object();

        assertFalse(PlacementSoundBroadcastScope.isActive());
        assertSame(player, PlacementSoundBroadcastScope.routeExcludedSource(player));
    }

    @Test
    void activeScopeIncludesPlacerByRoutingExclusionToNull() {
        Object player = new Object();

        try (PlacementSoundBroadcastScope ignored = PlacementSoundBroadcastScope.open()) {
            assertTrue(PlacementSoundBroadcastScope.isActive());
            assertNull(PlacementSoundBroadcastScope.routeExcludedSource(player));
        }

        assertFalse(PlacementSoundBroadcastScope.isActive());
        assertSame(player, PlacementSoundBroadcastScope.routeExcludedSource(player));
    }

    @Test
    void nestedScopesAndDoubleCloseCannotLeak() {
        PlacementSoundBroadcastScope outer = PlacementSoundBroadcastScope.open();
        PlacementSoundBroadcastScope inner = PlacementSoundBroadcastScope.open();
        assertTrue(PlacementSoundBroadcastScope.isActive());

        inner.close();
        inner.close();
        assertTrue(PlacementSoundBroadcastScope.isActive());

        outer.close();
        assertFalse(PlacementSoundBroadcastScope.isActive());
    }

    @Test
    void tryWithResourcesClearsScopeAfterException() {
        try {
            try (PlacementSoundBroadcastScope ignored = PlacementSoundBroadcastScope.open()) {
                throw new IllegalStateException("controlled");
            }
        } catch (IllegalStateException expected) {
            // Controlled fixture.
        }

        assertFalse(PlacementSoundBroadcastScope.isActive());
    }

    @Test
    void scopeIsThreadLocal() throws InterruptedException {
        AtomicReference<Object> routed = new AtomicReference<>();
        Object otherThreadPlayer = new Object();

        try (PlacementSoundBroadcastScope ignored = PlacementSoundBroadcastScope.open()) {
            Thread thread = new Thread(() -> routed.set(
                    PlacementSoundBroadcastScope.routeExcludedSource(otherThreadPlayer)
            ));
            thread.start();
            thread.join();
            assertSame(otherThreadPlayer, routed.get());
            assertNull(PlacementSoundBroadcastScope.routeExcludedSource(new Object()));
        }
    }
}
