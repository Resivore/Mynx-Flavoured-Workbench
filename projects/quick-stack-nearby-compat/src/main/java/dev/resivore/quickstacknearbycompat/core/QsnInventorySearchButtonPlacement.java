package dev.resivore.quickstacknearbycompat.core;

public final class QsnInventorySearchButtonPlacement {
    public static final String INVENTORY_SEARCH_MOD_ID = "inventorysearch";
    public static final String QSN_OWNER_ID = "quick-stack-nearby";
    public static final String QSN_ACTION_SLOT_ID = "quick_stack_nearby";
    public static final int BUTTON_SIZE = 12;
    public static final int VERTICAL_OFFSET = BUTTON_SIZE;

    private static final int PLAYER_INVENTORY_BOTTOM_OFFSET = 83;

    private QsnInventorySearchButtonPlacement() {
    }

    public static int playerInventoryBaseY(int topPos, int imageHeight) {
        return topPos + imageHeight - PLAYER_INVENTORY_BOTTOM_OFFSET;
    }

    public static int adjustedY(
            int placementY,
            int playerInventoryBaseY,
            boolean inventorySearchLoaded,
            String ownerId,
            String slotId
    ) {
        if (!inventorySearchLoaded
                || placementY != playerInventoryBaseY
                || !QSN_OWNER_ID.equals(ownerId)
                || !QSN_ACTION_SLOT_ID.equals(slotId)) {
            return placementY;
        }
        return placementY + VERTICAL_OFFSET;
    }
}
