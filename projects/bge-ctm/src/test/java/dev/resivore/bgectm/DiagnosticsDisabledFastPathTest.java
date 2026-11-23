package dev.resivore.bgectm;

import dev.resivore.bgectm.continuity.OverlayAttemptContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Architectural proof for the production-default diagnostic fast path. */
final class DiagnosticsDisabledFastPathTest {
    @Test
    void overlayObserverFollowsTheJvmProperties() {
        boolean requested = Boolean.getBoolean("bge_ctm.diagnostics")
                && !Boolean.getBoolean("bge_ctm.diagnostics.disable");
        assertEquals(requested, BgeCtmDiagnostics.enabled());
        if (requested) {
            OverlayAttemptContext.begin(false, true);
            OverlayAttemptContext.connectBlocks(false);
            var attempt = OverlayAttemptContext.end();
            assertTrue(attempt.promoted);
            assertEquals("CONNECT_BLOCKS_REJECT", attempt.reason);
            assertNull(OverlayAttemptContext.current());
            return;
        }
        OverlayAttemptContext.begin(false, true);
        OverlayAttemptContext.connectBlocks(true);
        assertNull(OverlayAttemptContext.current(), "Disabled diagnostics must not set ThreadLocal state");
        assertNull(OverlayAttemptContext.end(), "Disabled diagnostics must not create an Attempt");
    }
}
