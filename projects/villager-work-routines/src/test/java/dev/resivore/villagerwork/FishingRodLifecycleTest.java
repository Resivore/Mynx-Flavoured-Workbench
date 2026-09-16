package dev.resivore.villagerwork;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingRodLifecycleTest {
    @Test void rodIsAbsentWhileNavigatingAndReturningButStableThroughCastWaitAndRetrieve() {
        assertFalse(FishingRodLifecycle.retainsRod(FishingRodLifecycle.Phase.NAVIGATING_TO_BANK));
        assertTrue(FishingRodLifecycle.retainsRod(FishingRodLifecycle.Phase.CAST_TELEGRAPH));
        assertTrue(FishingRodLifecycle.retainsRod(FishingRodLifecycle.Phase.FLOAT_ACTIVE));
        assertTrue(FishingRodLifecycle.retainsRod(FishingRodLifecycle.Phase.RETRIEVING));
        assertFalse(FishingRodLifecycle.retainsRod(FishingRodLifecycle.Phase.RETURNING_TO_BARREL));
        assertFalse(FishingRodLifecycle.retainsRod(FishingRodLifecycle.Phase.CANCELLED));
    }

    @Test void retrievalReturnsToTheBarrelOnlyWhenOwnedFishRemains() {
        assertEquals(FishingRodLifecycle.Phase.RETURNING_TO_BARREL, FishingRodLifecycle.afterRetrieve(true));
        assertEquals(FishingRodLifecycle.Phase.IDLE, FishingRodLifecycle.afterRetrieve(false));
    }
}
