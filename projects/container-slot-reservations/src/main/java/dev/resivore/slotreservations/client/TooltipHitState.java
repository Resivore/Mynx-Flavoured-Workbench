package dev.resivore.slotreservations.client;

/** Frame-local identity and pointer proof, independent of cell contents and item count. */
public record TooltipHitState(long frame, Object tooltip, Object host, Object screen, double mouseX, double mouseY) {
    public boolean matches(long activeFrame, Object activeTooltip, Object activeHost, Object activeScreen,
                           double currentX, double currentY) {
        return frame == activeFrame && tooltip == activeTooltip && host == activeHost && screen == activeScreen
                && mouseX == currentX && mouseY == currentY;
    }
}
