package dev.resivore.villagerwork;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test void dwellRangeIsExactlyOneHundredEightyThroughFourHundredNineteen() {
        assertEquals(180, FishingRodLifecycle.nextDwellTicks(bound -> 0));
        assertEquals(419, FishingRodLifecycle.nextDwellTicks(bound -> {
            assertEquals(240, bound);
            return bound - 1;
        }));
        assertEquals(100, FishingRodLifecycle.POST_RETRIEVE_COOLDOWN_TICKS);
        assertThrows(IllegalArgumentException.class,
                () -> FishingRodLifecycle.nextDwellTicks(bound -> bound));
    }

    @Test void exactlyOneOfFourRetrieveOutcomesMayRollFishLoot() {
        int productive = 0;
        for (int roll = 0; roll < FishingRodLifecycle.PRODUCTION_GATE_BOUND; roll++) {
            if (FishingRodLifecycle.productiveRetrieve(roll)) productive++;
        }
        assertEquals(4, FishingRodLifecycle.PRODUCTION_GATE_BOUND);
        assertEquals(1, productive);
        assertTrue(FishingRodLifecycle.productiveRetrieve(0));
        assertFalse(FishingRodLifecycle.productiveRetrieve(1));
        assertFalse(FishingRodLifecycle.productiveRetrieve(2));
        assertFalse(FishingRodLifecycle.productiveRetrieve(3));
        assertThrows(IllegalArgumentException.class,
                () -> FishingRodLifecycle.productiveRetrieve(4));
    }
}
