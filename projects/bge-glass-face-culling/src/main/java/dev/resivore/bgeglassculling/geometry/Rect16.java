package dev.resivore.bgeglassculling.geometry;

import java.util.Optional;

/** An exact nonempty rectangle in block-local sixteenths. */
public record Rect16(int uMin, int uMax, int vMin, int vMax) {
    public Rect16 {
        if (uMin < 0 || uMax > 16 || vMin < 0 || vMax > 16
                || uMin >= uMax || vMin >= vMax) {
            throw new IllegalArgumentException("Invalid sixteenth rectangle");
        }
    }

    public Optional<Rect16> intersection(Rect16 other) {
        int nextUMin = Math.max(uMin, other.uMin);
        int nextUMax = Math.min(uMax, other.uMax);
        int nextVMin = Math.max(vMin, other.vMin);
        int nextVMax = Math.min(vMax, other.vMax);
        return nextUMin < nextUMax && nextVMin < nextVMax
                ? Optional.of(new Rect16(nextUMin, nextUMax, nextVMin, nextVMax))
                : Optional.empty();
    }

    public boolean covers(Rect16 cell) {
        return uMin <= cell.uMin && uMax >= cell.uMax
                && vMin <= cell.vMin && vMax >= cell.vMax;
    }
}
