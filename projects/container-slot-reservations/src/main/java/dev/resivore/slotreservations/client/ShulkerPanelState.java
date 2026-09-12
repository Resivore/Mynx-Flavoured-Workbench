package dev.resivore.slotreservations.client;

/** Small input/lifetime state machine shared by the live panel and deterministic fixtures. */
public final class ShulkerPanelState {
    private boolean open;

    public void open() {
        open = true;
    }

    /** The panel has no invisible bridge, grace frame, or pointer-capture lifetime. */
    public boolean retain(boolean insideHostOrPanel) {
        if (!open) return false;
        if (!insideHostOrPanel) close();
        return open;
    }

    public boolean ownsDrag(boolean insidePanel) {
        return open && insidePanel;
    }

    public boolean releasePointer(boolean insidePanel) {
        boolean owned = ownsDrag(insidePanel);
        return owned;
    }

    public void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }
}
