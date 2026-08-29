package dev.resivore.quickstacknearbycompat.core;

public final class InventorySearchContainerClassification {
    public static final String INVENTORY_SEARCH_MOD_ID = "inventorysearch";
    public static final String INVENTORY_EXTENDED_MOD_ID = "inventoryextended";

    private InventorySearchContainerClassification() {
    }

    public static boolean effectiveIsContainer(
            boolean upstreamIsContainer,
            boolean inventorySearchLoaded,
            boolean inventoryExtendedLoaded,
            boolean inventoryScreen
    ) {
        return upstreamIsContainer
                && !(inventorySearchLoaded && inventoryExtendedLoaded && inventoryScreen);
    }
}
