package dev.resivore.bgebushyleaves.geometry;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Plans clipped, patch-local projections of genuinely exterior canonical foliage quads. */
public final class ExteriorProjection {
    private ExteriorProjection() {}

    public static List<Projection> plan(Source source, PatchFrame target, List<Rect16> hidden) {
        Objects.requireNonNull(source, "source"); Objects.requireNonNull(target, "target");
        if (source.normal() != target.canonicalFace() || !source.isExterior()) return List.of();
        Rect16 mapped = mapInteriorBounds(source, target);
        if (mapped == null) return List.of();
        List<Projection> result = new ArrayList<>();
        for (Rect16 visible : RectSubtraction.subtract(mapped, hidden)) result.add(new Projection(source, target, visible));
        return List.copyOf(result);
    }

    private static Rect16 mapInteriorBounds(Source source, PatchFrame target) {
        int u0 = Math.max(0, source.uMin16()), u1 = Math.min(16, source.uMax16());
        int v0 = Math.max(0, source.vMin16()), v1 = Math.min(16, source.vMax16());
        if (u0 >= u1 || v0 >= v1) return null;
        Rect16 bounds = target.bounds();
        return new Rect16(map(u0, bounds.uMin(), bounds.uMax()), map(u1, bounds.uMin(), bounds.uMax()),
                map(v0, bounds.vMin(), bounds.vMax()), map(v1, bounds.vMin(), bounds.vMax()));
    }

    private static int map(int canonical, int min, int max) { return min + canonical * (max - min) / 16; }

    public record Source(Direction normal, int plane16, int uMin16, int uMax16, int vMin16, int vMax16) {
        public Source {
            Objects.requireNonNull(normal, "normal");
            if (uMin16 >= uMax16 || vMin16 >= vMax16) throw new IllegalArgumentException("Empty source quad");
        }
        public boolean isExterior() {
            return normal.getAxisDirection() == Direction.AxisDirection.POSITIVE ? plane16 > 16 : plane16 < 0;
        }
        public int normalOffset16() { return plane16 - (normal.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 16 : 0); }
    }

    public record Projection(Source source, PatchFrame target, Rect16 bounds) {
        public Projection { Objects.requireNonNull(source, "source"); Objects.requireNonNull(target, "target"); Objects.requireNonNull(bounds, "bounds"); }
        public int plane16() { return target.plane16() + source.normalOffset16(); }
        public int canonicalU(int targetU) { return (targetU - target.bounds().uMin()) * 16 / (target.bounds().uMax() - target.bounds().uMin()); }
        public int canonicalV(int targetV) { return (targetV - target.bounds().vMin()) * 16 / (target.bounds().vMax() - target.bounds().vMin()); }
    }
}
