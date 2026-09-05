package dev.resivore.slotreservations.client;

/** Small input/lifetime state machine shared by the live panel and deterministic fixtures. */
public final class ShulkerPanelState {
    private boolean open;
    private boolean pointerCaptured;
    private int outsideFrames;

    public void open() {
        open = true;
        pointerCaptured = false;
        outsideFrames = 0;
    }

    /** Returns whether the panel remains open; one deliberate outside frame is tolerated. */
    public boolean retain(boolean insideHostPanelOrCorridor) {
        if (!open) return false;
        if (insideHostPanelOrCorridor) {
            outsideFrames = 0;
            return true;
        }
        return outsideFrames++ == 0;
    }

    public void capturePointer() {
        if (open) pointerCaptured = true;
    }

    public boolean ownsDrag(boolean insidePanel) {
        return open && (pointerCaptured || insidePanel);
    }

    public boolean releasePointer(boolean insidePanel) {
        boolean owned = ownsDrag(insidePanel);
        pointerCaptured = false;
        return owned;
    }

    public void close() {
        open = false;
        pointerCaptured = false;
        outsideFrames = 0;
    }

    public boolean isOpen() {
        return open;
    }
}
