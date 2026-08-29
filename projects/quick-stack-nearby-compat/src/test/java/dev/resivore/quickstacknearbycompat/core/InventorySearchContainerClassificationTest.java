package dev.resivore.quickstacknearbycompat.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventorySearchContainerClassificationTest {
    @Test
    void vanillaInventoryScreenRemainsAllowed() {
        assertFalse(InventorySearchContainerClassification.effectiveIsContainer(
                false,
                true,
                false,
                true
        ));
    }

    @Test
    void inventoryExtendedInventoryScreenIsAllowedOnlyForTheExactCombination() {
        assertFalse(InventorySearchContainerClassification.effectiveIsContainer(
                true,
                true,
                true,
                true
        ));
        assertTrue(InventorySearchContainerClassification.effectiveIsContainer(
                true,
                true,
                false,
                true
        ));
        assertTrue(InventorySearchContainerClassification.effectiveIsContainer(
                true,
                false,
                true,
                true
        ));
    }

    @Test
    void realContainersRemainContainers() {
        assertTrue(InventorySearchContainerClassification.effectiveIsContainer(
                true,
                true,
                true,
                false
        ));
    }

    @Test
    void creativeBehaviorRemainsUnchanged() {
        assertFalse(InventorySearchContainerClassification.effectiveIsContainer(
                false,
                true,
                true,
                false
        ));
    }
}
