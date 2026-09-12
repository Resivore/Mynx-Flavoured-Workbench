package dev.resivore.slotreservations.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ShulkerPanelStateTest {
    @Test void panelClosesImmediatelyOutsideTheHostPanelUnion() {
        var state = new ShulkerPanelState();
        assertFalse(state.retain(true));
        state.open();
        assertTrue(state.isOpen());
        assertFalse(state.retain(false));
        assertFalse(state.isOpen());
        state.open();
        assertTrue(state.retain(true));
        assertTrue(state.isOpen());
    }

    @Test void panelOwnsDragsOnlyWhileThePointerIsInsideThePanel() {
        var state = new ShulkerPanelState();
        state.open();
        assertTrue(state.ownsDrag(true));
        assertTrue(state.releasePointer(true));
        assertFalse(state.ownsDrag(false));
        assertFalse(state.releasePointer(false));
    }

    @Test void closeClearsCaptureAndLifetime() {
        var state = new ShulkerPanelState();
        state.open(); state.close();
        assertFalse(state.isOpen());
        assertFalse(state.ownsDrag(true));
        assertFalse(state.releasePointer(true));
        assertFalse(state.retain(true));
    }

    @Test void rightDragOnlyEmitsWhenItEntersANewCell() {
        var drag = new ShulkerPanel.SecondaryDrag();
        drag.begin(3);
        assertFalse(drag.enter(3));
        assertTrue(drag.enter(4));
        assertFalse(drag.enter(4));
        assertFalse(drag.enter(-1));
        assertTrue(drag.enter(4));
        drag.reset();
        assertTrue(drag.enter(4));
    }
}
