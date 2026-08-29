package dev.resivore.radialslotcycler.core;

import org.junit.jupiter.api.Test;

import static dev.resivore.radialslotcycler.core.SwapRequestValidator.Result.ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SwapRequestValidatorTest {
    @Test
    void acceptsDynamicVanillaAndInventoryExtendedCandidates() {
        assertEquals(ACCEPTED, validate(4, 31, 36, 4, 36));
        assertEquals(ACCEPTED, validate(4, 58, 63, 4, 63));
        assertEquals(ACCEPTED, validate(0, 54, 63, 0, 63));
        assertEquals(ACCEPTED, validate(8, 62, 63, 8, 63));
    }

    @Test
    void rejectsAnotherColumnArmorOffhandAndOutOfRangeTargets() {
        assertEquals(SwapRequestValidator.Result.WRONG_COLUMN,
                validate(4, 14, 63, 4, 63));
        assertEquals(SwapRequestValidator.Result.OUT_OF_RANGE_TARGET,
                validate(4, 63, 63, 4, 63));
        assertEquals(SwapRequestValidator.Result.OUT_OF_RANGE_TARGET,
                validate(4, 67, 63, 4, 63));
        assertEquals(SwapRequestValidator.Result.OUT_OF_RANGE_TARGET,
                validate(4, 999, 63, 4, 63));
        assertEquals(SwapRequestValidator.Result.NOT_A_STORAGE_SLOT,
                validate(4, 4, 63, 4, 63));
    }

    @Test
    void rejectsStaleHotbarAndInventorySizeSnapshots() {
        assertEquals(SwapRequestValidator.Result.STALE_SELECTED_HOTBAR_SLOT,
                validate(4, 58, 63, 3, 63));
        assertEquals(SwapRequestValidator.Result.STALE_ORDINARY_SIZE,
                validate(4, 31, 36, 4, 63));
        assertEquals(SwapRequestValidator.Result.INVALID_LIVE_LAYOUT,
                validate(4, 31, 36, 4, 62));
    }

    @Test
    void rejectsMalformedFieldsSpectatorAndForeignMenus() {
        assertEquals(SwapRequestValidator.Result.MALFORMED_HOTBAR_SLOT,
                validate(-1, 13, 36, 4, 36));
        assertEquals(SwapRequestValidator.Result.MALFORMED_HOTBAR_SLOT,
                validate(9, 13, 36, 4, 36));
        assertEquals(SwapRequestValidator.Result.MALFORMED_ORDINARY_SIZE,
                validate(4, 13, -1, 4, 36));
        assertEquals(SwapRequestValidator.Result.OUT_OF_RANGE_TARGET,
                validate(4, -1, 36, 4, 36));
        assertEquals(SwapRequestValidator.Result.SPECTATOR,
                SwapRequestValidator.validate(4, 13, 36, 4, 36, true, true));
        assertEquals(SwapRequestValidator.Result.FOREIGN_MENU_OPEN,
                SwapRequestValidator.validate(4, 13, 36, 4, 36, false, false));
    }

    private static SwapRequestValidator.Result validate(
            int requestedHotbar,
            int requestedTarget,
            int requestedSize,
            int liveHotbar,
            int liveSize
    ) {
        return SwapRequestValidator.validate(
                requestedHotbar,
                requestedTarget,
                requestedSize,
                liveHotbar,
                liveSize,
                true,
                false);
    }
}
