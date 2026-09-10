package dev.resivore.quickstacknearbycompat.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QsnInventorySearchButtonPlacementTest {
    private static final int LEFT = 40;
    private static final int TOP = 30;
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 166;

    @Test
    void survivalUtilityStackUsesTheSharedLiveBottomRightGeometry() {
        assertEquals(18, QsnInventorySearchButtonPlacement.BUTTON_SIZE);
        assertEquals(4, QsnInventorySearchButtonPlacement.BUTTON_GAP);
        assertEquals(22, QsnInventorySearchButtonPlacement.QSN_ABOVE_NOTEBOOK_OFFSET);
        assertEquals(220, QsnInventorySearchButtonPlacement.preferredX(LEFT, IMAGE_WIDTH));
        assertEquals(178, QsnInventorySearchButtonPlacement.notebookY(TOP, IMAGE_HEIGHT));
        assertEquals(156, QsnInventorySearchButtonPlacement.preferredY(TOP, IMAGE_HEIGHT));
        assertEquals(TOP + IMAGE_HEIGHT - 40,
                QsnInventorySearchButtonPlacement.preferredY(TOP, IMAGE_HEIGHT));
    }

    @Test
    void preferredQsnSlotAndNotebookLowerSlotHaveExactlyFourPixelsBetweenThem() {
        int qsnBottom = QsnInventorySearchButtonPlacement.preferredY(TOP, IMAGE_HEIGHT)
                + QsnInventorySearchButtonPlacement.BUTTON_SIZE;
        int notebookTop = QsnInventorySearchButtonPlacement.notebookY(TOP, IMAGE_HEIGHT);

        assertEquals(QsnInventorySearchButtonPlacement.BUTTON_GAP, notebookTop - qsnBottom);
        assertFalse(overlaps(
                QsnInventorySearchButtonPlacement.preferredY(TOP, IMAGE_HEIGHT),
                notebookTop,
                QsnInventorySearchButtonPlacement.BUTTON_SIZE));
    }

    @Test
    void unrelatedWidgetAtThePreferredSlotMovesQsnUpwardWithoutUsingTheNotebookSlot() {
        QsnInventorySearchButtonPlacement.Position position =
                QsnInventorySearchButtonPlacement.firstBottomUpFreePosition(
                        LEFT, TOP, IMAGE_WIDTH, IMAGE_HEIGHT, 400, 300,
                        List.of(new QsnInventorySearchButtonPlacement.Bounds(220, 156, 18, 18, true)));

        assertEquals(new QsnInventorySearchButtonPlacement.Position(220, 134), position);
    }

    @Test
    void invisibleWidgetsDoNotDisplaceTheAuthoritativePreferredSlot() {
        QsnInventorySearchButtonPlacement.Position position =
                QsnInventorySearchButtonPlacement.firstBottomUpFreePosition(
                        LEFT, TOP, IMAGE_WIDTH, IMAGE_HEIGHT, 400, 300,
                        List.of(new QsnInventorySearchButtonPlacement.Bounds(220, 156, 18, 18, false)));

        assertEquals(new QsnInventorySearchButtonPlacement.Position(220, 156), position);
    }

    @Test
    void unavailableRightSideKeepsTheExistingUpstreamReservation() {
        assertNull(QsnInventorySearchButtonPlacement.firstBottomUpFreePosition(
                LEFT, TOP, IMAGE_WIDTH, IMAGE_HEIGHT, 230, 300, List.of()));
    }

    @Test
    void onlyTheQsnActionReservationReceivesTheSurvivalOverride() {
        assertTrue(QsnInventorySearchButtonPlacement.isQsnActionReservation(
                QsnInventorySearchButtonPlacement.QSN_OWNER_ID,
                QsnInventorySearchButtonPlacement.QSN_ACTION_SLOT_ID));
        assertFalse(QsnInventorySearchButtonPlacement.isQsnActionReservation(
                "another-owner", QsnInventorySearchButtonPlacement.QSN_ACTION_SLOT_ID));
        assertFalse(QsnInventorySearchButtonPlacement.isQsnActionReservation(
                QsnInventorySearchButtonPlacement.QSN_OWNER_ID, "another-slot"));
    }

    private static boolean overlaps(int firstY, int secondY, int size) {
        return firstY < secondY + size && secondY < firstY + size;
    }
}
