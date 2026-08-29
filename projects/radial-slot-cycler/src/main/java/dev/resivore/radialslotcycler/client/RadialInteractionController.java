package dev.resivore.radialslotcycler.client;

import java.util.Objects;

/**
 * Edge-triggered open/close state. A highlighted storage entry is represented
 * by an index greater than zero; index zero is the hotbar anchor and -1 is the
 * center dead zone.
 */
public final class RadialInteractionController {
    private final Mode mode;
    private boolean open;
    private boolean keyDownLastTick;

    public RadialInteractionController(Mode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    public Action update(
            boolean keyDown,
            boolean keyClicked,
            boolean canUse,
            int highlightedIndex
    ) {
        Action action = Action.NONE;
        if (open && !canUse) {
            open = false;
            action = Action.CANCELLED;
        } else if (mode == Mode.HOLD) {
            if (!open && canUse && keyDown && !keyDownLastTick) {
                open = true;
                action = Action.OPENED;
            } else if (open && keyDownLastTick && !keyDown) {
                open = false;
                action = closeAction(highlightedIndex);
            }
        } else if (keyClicked) {
            if (!open && canUse) {
                open = true;
                action = Action.OPENED;
            } else if (open) {
                open = false;
                action = closeAction(highlightedIndex);
            }
        }
        keyDownLastTick = keyDown;
        return action;
    }

    public boolean isOpen() {
        return open;
    }

    public Mode mode() {
        return mode;
    }

    public void forceClosed(boolean keyDown) {
        open = false;
        keyDownLastTick = keyDown;
    }

    private static Action closeAction(int highlightedIndex) {
        if (highlightedIndex > 0) {
            return Action.CONFIRM;
        }
        if (highlightedIndex == 0) {
            return Action.CLOSED_WITHOUT_MUTATION;
        }
        return Action.CANCELLED;
    }

    public enum Mode {
        HOLD,
        TOGGLE
    }

    public enum Action {
        NONE,
        OPENED,
        CONFIRM,
        CLOSED_WITHOUT_MUTATION,
        CANCELLED
    }
}
