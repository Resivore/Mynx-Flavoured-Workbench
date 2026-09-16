package dev.resivore.slotreservations.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deterministic C21 press-time mode and virtual-cell traversal contract. */
final class MouseTweaksRmbGestureTest {
    @Test void occupiedPressLatchesCollectionEvenAfterAnEmptyCellIsEntered() {
        var gesture = new MouseTweaksRmbGesture();
        gesture.begin(MouseTweaksRmbGesture.selectMode(true, false),
                MouseTweaksRmbGesture.OriginRegion.PANEL, 3, false);

        assertEquals(MouseTweaksRmbGesture.Mode.COLLECTION_SOURCE, gesture.mode());
        assertEquals(MouseTweaksRmbGesture.OriginRegion.PANEL, gesture.originRegion());
        assertEquals(3, gesture.originPanelCell());
        assertFalse(gesture.upstreamArmed(), "Empty-cursor source mode is CSR fallback, not MT RHS");
        assertTrue(gesture.enterPanelCell(3), "The direct panel press owns the initial source cell");
        assertFalse(gesture.enterPanelCell(3), "MT's first origin replay must not duplicate the direct press");
        assertTrue(gesture.enterPanelCell(4), "Entering a later empty cell is one distinct event");
        gesture.leavePanelCell();
        gesture.markNativeBoundary();
        assertTrue(gesture.takeShadowNeedsLiveCarried(),
                "Panel-to-ordinary collection crossing records one cursor-sync boundary");
        assertEquals(MouseTweaksRmbGesture.Mode.COLLECTION_SOURCE, gesture.mode(),
                "Later cells and ordinary-slot crossings never reclassify the gesture");
    }

    @Test void emptyPressWithCarriedStackLatchesDepositAcrossOccupiedCellsAndMenuBoundary() {
        var gesture = new MouseTweaksRmbGesture();
        gesture.begin(MouseTweaksRmbGesture.selectMode(false, true),
                MouseTweaksRmbGesture.OriginRegion.MENU, -1, true);

        assertEquals(MouseTweaksRmbGesture.Mode.DEPOSIT, gesture.mode());
        assertEquals(MouseTweaksRmbGesture.OriginRegion.MENU, gesture.originRegion());
        assertTrue(gesture.upstreamArmed());
        assertTrue(gesture.takeShadowNeedsLiveCarried(),
                "Ordinary-menu origins re-sample the cursor before the first panel destination");
        assertTrue(gesture.enterPanelCell(7));
        assertEquals(MouseTweaksRmbGesture.Mode.DEPOSIT, gesture.mode(),
                "An occupied destination cannot flip deposit into collection");
        gesture.markNativeBoundary();
        assertTrue(gesture.takeShadowNeedsLiveCarried(),
                "A panel-to-menu or menu-to-panel transition is a cursor sync boundary");
        assertTrue(gesture.enterPanelCell(8));
        assertEquals(MouseTweaksRmbGesture.Mode.DEPOSIT, gesture.mode());
    }

    @Test void virtualTraversalMatchesMouseTweaksIdentityDeduplicationAndReentry() {
        var gesture = new MouseTweaksRmbGesture();
        gesture.begin(MouseTweaksRmbGesture.Mode.DEPOSIT,
                MouseTweaksRmbGesture.OriginRegion.PANEL, 1, true);

        assertTrue(gesture.enterPanelCell(1));
        assertFalse(gesture.enterPanelCell(1), "Stationary drag events do not spam the same cell");
        assertTrue(gesture.enterPanelCell(2));
        assertFalse(gesture.enterPanelCell(2));
        assertTrue(gesture.enterPanelCell(1), "B -> A re-entry is one new MT 2.31 action");
        gesture.leavePanelCell();
        assertTrue(gesture.enterPanelCell(1), "Leaving to an ordinary/null slot permits later re-entry");
    }

    @Test void releaseCloseOrStaleCancellationResetsEveryModeAndProjectionFlag() {
        var gesture = new MouseTweaksRmbGesture();
        gesture.begin(MouseTweaksRmbGesture.Mode.DEPOSIT,
                MouseTweaksRmbGesture.OriginRegion.PANEL, 9, true);
        gesture.markNativeBoundary();
        gesture.markPanelActionDispatched();
        gesture.reset();

        assertFalse(gesture.isActive());
        assertEquals(MouseTweaksRmbGesture.Mode.INACTIVE, gesture.mode());
        assertEquals(MouseTweaksRmbGesture.OriginRegion.NONE, gesture.originRegion());
        assertEquals(-1, gesture.originPanelCell());
        assertFalse(gesture.upstreamArmed());
        assertFalse(gesture.takeShadowNeedsLiveCarried());
        assertFalse(gesture.hasDispatchedPanelAction());
        assertFalse(gesture.enterPanelCell(9));
    }
}
