package dev.resivore.slotreservations.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ShulkerPanelStateTest {
    @Test void openingAndReentryResetTheSingleGraceFrame() {
        var state = new ShulkerPanelState();
        assertFalse(state.retain(true));
        state.open();
        assertTrue(state.isOpen());
        assertTrue(state.retain(false));
        assertTrue(state.retain(true));
        assertTrue(state.retain(false));
        assertFalse(state.retain(false));
    }

    @Test void panelOriginatedPointerSequencesStayOwnedUntilRelease() {
        var state = new ShulkerPanelState();
        state.open();
        state.capturePointer();
        assertTrue(state.ownsDrag(false));
        assertTrue(state.releasePointer(false));
        assertFalse(state.ownsDrag(false));
        assertTrue(state.ownsDrag(true));
        assertTrue(state.releasePointer(true));
    }

    @Test void closeClearsCaptureAndLifetime() {
        var state = new ShulkerPanelState();
        state.open(); state.capturePointer(); state.close();
        assertFalse(state.isOpen());
        assertFalse(state.ownsDrag(true));
        assertFalse(state.releasePointer(true));
        assertFalse(state.retain(true));
    }
}
