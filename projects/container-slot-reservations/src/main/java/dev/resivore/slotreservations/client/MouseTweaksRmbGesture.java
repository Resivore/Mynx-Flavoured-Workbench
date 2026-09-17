package dev.resivore.slotreservations.client;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;

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
    private boolean blockedUntilRelease;

    static Mode selectMode(boolean occupied, boolean carrying) {
        if (occupied) return Mode.COLLECTION_SOURCE;
        return carrying ? Mode.DEPOSIT : Mode.INACTIVE;
    }

    /** True only when a native RMB action can be modelled as a one-item placement. */
    static boolean canProjectNativePlaceOne(ItemStack projectedCarried, ItemStack liveCarried, Slot target) {
        if (projectedCarried.isEmpty() || liveCarried.isEmpty()
                || liveCarried.getItem() instanceof BundleItem
                || !ItemStack.isSameItemSameComponents(projectedCarried, liveCarried)
                || liveCarried.getCount() < projectedCarried.getCount()
                || target == null || target.isFake() || !target.isActive()
                || !target.mayPlace(liveCarried)) return false;
        ItemStack existing = target.getItem();
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, liveCarried)) {
            return false;
        }
        int capacity = Math.min(target.getMaxStackSize(liveCarried), liveCarried.getMaxStackSize());
        return existing.getCount() < capacity;
    }

    /**
     * Applies only the relative cursor delta observed across the synchronous native click.
     * The projected cursor is never replaced with the potentially stale live cursor.
     */
    static boolean applyNativeCursorDelta(ItemStack projectedCarried,
                                          ItemStack liveBefore, ItemStack liveAfter) {
        if (ItemStack.matches(liveBefore, liveAfter)) return true;
        if (projectedCarried.isEmpty() || liveBefore.isEmpty()
                || !ItemStack.isSameItemSameComponents(projectedCarried, liveBefore)) return false;
        if (liveAfter.isEmpty()) {
            if (liveBefore.getCount() != 1) return false;
        } else if (!ItemStack.isSameItemSameComponents(liveBefore, liveAfter)
                || liveBefore.getCount() - liveAfter.getCount() != 1) {
            return false;
        }
        projectedCarried.shrink(1);
        return true;
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

    boolean isBlockedUntilRelease() {
        return blockedUntilRelease;
    }

    /** Retains only the carried approach bridge or a gesture which is actually active. */
    boolean retainPanel(boolean preGestureRetention, boolean carrying) {
        return isActive() || (preGestureRetention && carrying);
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
        if (!isActive() || blockedUntilRelease || cell < 0 || cell == lastPanelCell) return false;
        lastPanelCell = cell;
        return true;
    }

    void leavePanelCell() {
        if (isActive()) lastPanelCell = -1;
    }

    void markUnprojectedNativeBoundary() {
        if (isActive() && !blockedUntilRelease && !dispatchedPanelAction) shadowNeedsLiveCarried = true;
    }

    /** Retains gesture ownership while making every later held-RMB action a no-op. */
    void blockUntilRelease() {
        if (!isActive()) return;
        blockedUntilRelease = true;
        shadowNeedsLiveCarried = false;
        lastPanelCell = -1;
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
        blockedUntilRelease = false;
    }
}
