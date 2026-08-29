package dev.resivore.radialslotcycler.core;

/** Pure validation shared by the server receiver and focused fixtures. */
public final class SwapRequestValidator {
    private SwapRequestValidator() {}

    public static Result validate(
            int requestedHotbarSlot,
            int requestedStorageSlot,
            int requestedOrdinarySize,
            int liveSelectedHotbarSlot,
            int liveOrdinarySize,
            boolean inventoryMenuActive,
            boolean spectator
    ) {
        if (spectator) {
            return Result.SPECTATOR;
        }
        if (!inventoryMenuActive) {
            return Result.FOREIGN_MENU_OPEN;
        }
        if (!ColumnLayout.isHotbarSlot(requestedHotbarSlot)) {
            return Result.MALFORMED_HOTBAR_SLOT;
        }
        if (!ColumnLayout.isCompleteOrdinaryLayout(requestedOrdinarySize)) {
            return Result.MALFORMED_ORDINARY_SIZE;
        }
        if (!ColumnLayout.isCompleteOrdinaryLayout(liveOrdinarySize)) {
            return Result.INVALID_LIVE_LAYOUT;
        }
        if (requestedOrdinarySize != liveOrdinarySize) {
            return Result.STALE_ORDINARY_SIZE;
        }
        if (requestedHotbarSlot != liveSelectedHotbarSlot) {
            return Result.STALE_SELECTED_HOTBAR_SLOT;
        }
        if (requestedStorageSlot < 0 || requestedStorageSlot >= liveOrdinarySize) {
            return Result.OUT_OF_RANGE_TARGET;
        }
        if (requestedStorageSlot < net.minecraft.world.entity.player.Inventory.SELECTION_SIZE) {
            return Result.NOT_A_STORAGE_SLOT;
        }
        if (!ColumnLayout.isStorageCandidate(
                requestedHotbarSlot, requestedStorageSlot, liveOrdinarySize)) {
            return Result.WRONG_COLUMN;
        }
        return Result.ACCEPTED;
    }

    public enum Result {
        ACCEPTED,
        SPECTATOR,
        FOREIGN_MENU_OPEN,
        MALFORMED_HOTBAR_SLOT,
        MALFORMED_ORDINARY_SIZE,
        INVALID_LIVE_LAYOUT,
        STALE_ORDINARY_SIZE,
        STALE_SELECTED_HOTBAR_SLOT,
        OUT_OF_RANGE_TARGET,
        NOT_A_STORAGE_SLOT,
        WRONG_COLUMN
    }
}
