package dev.resivore.slotreservations.client;

/**
 * Client-only latch for one Mouse Tweaks right-button gesture. It deliberately
 * records the press-time classification instead of deriving a new meaning from
 * every later hovered slot.
 */
final class MouseTweaksRmbGesture {
    enum Mode {
        INACTIVE,
        COLLECTION_SOURCE,
        DEPOSIT
    }

    enum OriginRegion {
        NONE,
        MENU,
        PANEL
    }

    private Mode mode = Mode.INACTIVE;
    private OriginRegion originRegion = OriginRegion.NONE;
    private int originPanelCell = -1;
    private int lastPanelCell = -1;
    private boolean shadowNeedsLiveCarried;
    private boolean dispatchedPanelAction;

    static Mode selectMode(boolean occupied, boolean carrying) {
        if (occupied) return Mode.COLLECTION_SOURCE;
        return carrying ? Mode.DEPOSIT : Mode.INACTIVE;
    }

    void begin(Mode selectedMode, OriginRegion selectedOrigin, int selectedPanelCell) {
        reset();
        if (selectedMode == Mode.INACTIVE) return;
        mode = selectedMode;
        originRegion = selectedOrigin;
        originPanelCell = selectedOrigin == OriginRegion.PANEL ? selectedPanelCell : -1;
        // A native menu-origin action may alter the server cursor before CSR first sees a panel cell.
        shadowNeedsLiveCarried = selectedOrigin == OriginRegion.MENU;
    }

    boolean isActive() {
        return mode != Mode.INACTIVE;
    }

    Mode mode() {
        return mode;
    }

    OriginRegion originRegion() {
        return originRegion;
    }

    int originPanelCell() {
        return originPanelCell;
    }

    /**
     * Mirrors Mouse Tweaks' identity-based per-cell dispatch. The direct CSR
     * press already owns an initial panel cell, so its first upstream replay is
     * intentionally suppressed; a later re-entry remains a new action.
     */
    boolean enterPanelCell(int cell) {
        if (!isActive() || cell < 0 || cell == lastPanelCell) return false;
        lastPanelCell = cell;
        return true;
    }

    void leavePanelCell() {
        if (isActive()) lastPanelCell = -1;
    }

    void markNativeBoundary() {
        if (isActive()) shadowNeedsLiveCarried = true;
    }

    boolean takeShadowNeedsLiveCarried() {
        boolean pending = shadowNeedsLiveCarried;
        shadowNeedsLiveCarried = false;
        return pending;
    }

    boolean hasDispatchedPanelAction() {
        return dispatchedPanelAction;
    }

    void markPanelActionDispatched() {
        dispatchedPanelAction = true;
    }

    void reset() {
        mode = Mode.INACTIVE;
        originRegion = OriginRegion.NONE;
        originPanelCell = -1;
        lastPanelCell = -1;
        shadowNeedsLiveCarried = false;
        dispatchedPanelAction = false;
    }
}
