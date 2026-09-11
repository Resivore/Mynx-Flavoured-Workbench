package dev.resivore.quickstacknearbycompat.core;

import org.junit.jupiter.api.Test;
import tempeststudios.quickstacknearby.QuickStackMoveEngine;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PopulatedShulkerOuterProtectionTest {
    @Test
    void overlayLocksOnlyTheActionScopedPopulatedCarrierSlots() {
        QuickStackMoveEngine.SlotRule preserved = new QuickStackMoveEngine.SlotRule(false, 3);
        QuickStackMoveEngine.SourceRules user = new QuickStackMoveEngine.SourceRules(Map.of(5, preserved));

        QuickStackMoveEngine.SourceRules overlay = PopulatedShulkerOuterProtection.overlay(user, Set.of(9));

        assertSame(preserved, overlay.slotRules().get(5));
        assertTrue(overlay.isLocked(9));
        assertTrue(overlay.movableCount(9, 64) == 0);
        assertFalse(user.isLocked(9));
    }

    @Test
    void noProtectedSlotsReturnsTheRealUserRulesWithoutPersistingAnything() {
        QuickStackMoveEngine.SourceRules user = new QuickStackMoveEngine.SourceRules(Map.of(
                5, new QuickStackMoveEngine.SlotRule(false, 3)
        ));
        assertSame(user, PopulatedShulkerOuterProtection.overlay(user, Set.of()));
    }
}
