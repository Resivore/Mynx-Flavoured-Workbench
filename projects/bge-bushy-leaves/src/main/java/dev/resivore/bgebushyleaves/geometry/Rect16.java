package dev.resivore.bgebushyleaves.geometry;

import java.util.Optional;

/** An exact nonempty target-space rectangle in block-local sixteenths. */
public record Rect16(int uMin, int uMax, int vMin, int vMax) {
    public Rect16 {
        if (uMin < 0 || uMax > 16 || vMin < 0 || vMax > 16 || uMin >= uMax || vMin >= vMax) {
            throw new IllegalArgumentException("Invalid sixteenth rectangle");
        }
    }
    public Optional<Rect16> intersection(Rect16 other) {
        int minU = Math.max(uMin, other.uMin), maxU = Math.min(uMax, other.uMax);
        int minV = Math.max(vMin, other.vMin), maxV = Math.min(vMax, other.vMax);
        return minU < maxU && minV < maxV ? Optional.of(new Rect16(minU, maxU, minV, maxV)) : Optional.empty();
    }
    public boolean covers(Rect16 other) {
        return uMin <= other.uMin && uMax >= other.uMax && vMin <= other.vMin && vMax >= other.vMax;
    }
}
