package dev.resivore.quickstacknearbycompat.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QsnInventorySearchButtonPlacementTest {
    private static final int BASE_Y = 100;

    @Test
    void exactFallbackPlacementMovesByTheMinimumFullButtonHeight() {
        assertEquals(
                BASE_Y + 12,
                QsnInventorySearchButtonPlacement.adjustedY(
                        BASE_Y,
                        BASE_Y,
                        true,
                        QsnInventorySearchButtonPlacement.QSN_OWNER_ID,
                        QsnInventorySearchButtonPlacement.QSN_ACTION_SLOT_ID
                )
        );
        assertEquals(12, QsnInventorySearchButtonPlacement.BUTTON_SIZE);
        assertEquals(
                QsnInventorySearchButtonPlacement.BUTTON_SIZE,
                QsnInventorySearchButtonPlacement.VERTICAL_OFFSET
        );
    }

    @Test
    void twelvePixelsIsTheSmallestNonOverlappingVerticalOffset() {
        assertTrue(overlaps(BASE_Y, BASE_Y + 11, QsnInventorySearchButtonPlacement.BUTTON_SIZE));
        assertFalse(overlaps(BASE_Y, BASE_Y + 12, QsnInventorySearchButtonPlacement.BUTTON_SIZE));
    }

    @Test
    void workingSharedSlotPlacementIsNotDoubleShifted() {
        int inventorySearchY = BASE_Y;
        int qsnY = inventorySearchY + QsnInventorySearchButtonPlacement.BUTTON_SIZE + 1;

        assertEquals(BASE_Y + 13, qsnY);
        assertFalse(overlaps(
                inventorySearchY,
                qsnY,
                QsnInventorySearchButtonPlacement.BUTTON_SIZE
        ));
        assertEquals(
                qsnY,
                QsnInventorySearchButtonPlacement.adjustedY(
                        qsnY,
                        BASE_Y,
                        true,
                        QsnInventorySearchButtonPlacement.QSN_OWNER_ID,
                        QsnInventorySearchButtonPlacement.QSN_ACTION_SLOT_ID
                )
        );
    }

    @Test
    void absentInventorySearchAndUnrelatedReservationsRemainUnchanged() {
        assertEquals(
                BASE_Y,
                QsnInventorySearchButtonPlacement.adjustedY(
                        BASE_Y,
                        BASE_Y,
                        false,
                        QsnInventorySearchButtonPlacement.QSN_OWNER_ID,
                        QsnInventorySearchButtonPlacement.QSN_ACTION_SLOT_ID
                )
        );
        assertEquals(
                BASE_Y,
                QsnInventorySearchButtonPlacement.adjustedY(
                        BASE_Y,
                        BASE_Y,
                        true,
                        "another-owner",
                        QsnInventorySearchButtonPlacement.QSN_ACTION_SLOT_ID
                )
        );
        assertEquals(
                BASE_Y,
                QsnInventorySearchButtonPlacement.adjustedY(
                        BASE_Y,
                        BASE_Y,
                        true,
                        QsnInventorySearchButtonPlacement.QSN_OWNER_ID,
                        "another-slot"
                )
        );
    }

    @Test
    void playerInventoryBaseRemainsRelativeToTheScreenGeometry() {
        assertEquals(57, QsnInventorySearchButtonPlacement.playerInventoryBaseY(20, 120));
    }

    private static boolean overlaps(int firstY, int secondY, int size) {
        return firstY < secondY + size && secondY < firstY + size;
    }
}
