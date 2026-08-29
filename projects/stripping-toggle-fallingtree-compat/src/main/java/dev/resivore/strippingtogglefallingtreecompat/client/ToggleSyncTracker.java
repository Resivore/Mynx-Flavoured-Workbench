package dev.resivore.strippingtogglefallingtreecompat.client;

public final class ToggleSyncTracker {
    public static final int APPLY_NATIVE_STATE = 1;
    public static final int SEND_PENDING = 1 << 1;

    private boolean hasApplied;
    private boolean lastApplied;
    private boolean hasSent;
    private boolean lastSent;

    public int evaluate(boolean current, boolean force) {
        int decision = 0;
        boolean applyNativeState = force || !hasApplied || current != lastApplied;
        if (applyNativeState) {
            lastApplied = current;
            hasApplied = true;
            decision |= APPLY_NATIVE_STATE;
        }

        if (force || !hasSent || current != lastSent) {
            decision |= SEND_PENDING;
        }
        return decision;
    }

    public void markSent(boolean current) {
        lastSent = current;
        hasSent = true;
    }

    public void reset() {
        hasApplied = false;
        lastApplied = false;
        hasSent = false;
        lastSent = false;
    }
}
