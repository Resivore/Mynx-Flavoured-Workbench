package dev.resivore.bgebushyleaves.geometry;

import net.minecraft.core.Direction;

import java.util.Objects;

/** Geometry-only form of an authoritative BGE surface patch. */
public record PatchFrame(Direction normal, int plane16, Direction.Axis uAxis, Rect16 bounds,
        Direction.Axis vAxis, Direction canonicalFace) {
    public PatchFrame {
        Objects.requireNonNull(normal, "normal"); Objects.requireNonNull(uAxis, "uAxis");
        Objects.requireNonNull(bounds, "bounds"); Objects.requireNonNull(vAxis, "vAxis");
        Objects.requireNonNull(canonicalFace, "canonicalFace");
        if (plane16 < 0 || plane16 > 16 || normal.getAxis() == uAxis || normal.getAxis() == vAxis || uAxis == vAxis) {
            throw new IllegalArgumentException("Invalid BGE patch frame");
        }
    }
}
