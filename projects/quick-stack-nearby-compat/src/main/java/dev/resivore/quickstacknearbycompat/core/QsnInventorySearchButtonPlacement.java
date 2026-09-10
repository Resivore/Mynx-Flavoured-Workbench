package dev.resivore.quickstacknearbycompat.core;

public final class QsnInventorySearchButtonPlacement {
    public static final String QSN_OWNER_ID = "quick-stack-nearby";
    public static final String QSN_ACTION_SLOT_ID = "quick_stack_nearby";
    public static final int BUTTON_SIZE = 18;
    public static final int BUTTON_GAP = 4;
    public static final int QSN_ABOVE_NOTEBOOK_OFFSET = BUTTON_SIZE + BUTTON_GAP;

    private QsnInventorySearchButtonPlacement() {
    }

    public static int preferredX(int leftPos, int imageWidth) {
        return leftPos + imageWidth + BUTTON_GAP;
    }

    public static int notebookY(int topPos, int imageHeight) {
        return topPos + imageHeight - BUTTON_SIZE;
    }

    public static int preferredY(int topPos, int imageHeight) {
        return notebookY(topPos, imageHeight) - QSN_ABOVE_NOTEBOOK_OFFSET;
    }

    public static boolean isQsnActionReservation(String ownerId, String slotId) {
        return QSN_OWNER_ID.equals(ownerId) && QSN_ACTION_SLOT_ID.equals(slotId);
    }

    /**
     * Searches only upward from QSN's owned upper utility slot.  The lower slot remains
     * Notebook's stable location even when Notebook is not installed.
     */
    public static Position firstBottomUpFreePosition(
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            int screenWidth,
            int screenHeight,
            Iterable<Bounds> occupied
    ) {
        int x = preferredX(leftPos, imageWidth);
        if (x < 2 || x + BUTTON_SIZE > screenWidth - 2) {
            return null;
        }
        for (int y = preferredY(topPos, imageHeight); y >= 2; y -= QSN_ABOVE_NOTEBOOK_OFFSET) {
            if (y + BUTTON_SIZE > screenHeight - 2) {
                continue;
            }
            if (isFree(x, y, occupied)) {
                return new Position(x, y);
            }
        }
        return null;
    }

    private static boolean isFree(int x, int y, Iterable<Bounds> occupied) {
        for (Bounds bounds : occupied) {
            if (bounds.visible()
                    && x < bounds.x() + bounds.width()
                    && x + BUTTON_SIZE > bounds.x()
                    && y < bounds.y() + bounds.height()
                    && y + BUTTON_SIZE > bounds.y()) {
                return false;
            }
        }
        return true;
    }

    public record Bounds(int x, int y, int width, int height, boolean visible) {
    }

    public record Position(int x, int y) {
    }
}
