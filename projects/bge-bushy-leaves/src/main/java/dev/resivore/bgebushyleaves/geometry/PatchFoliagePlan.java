package dev.resivore.bgebushyleaves.geometry;

import java.util.List;

/** Authored, deterministic two-card foliage motif in a BGE patch's own U/V/normal frame. */
public final class PatchFoliagePlan {
    /** Central visual tuning constants; values are in BGE sixteenth units. */
    public static final float OUTWARD_DEPTH_16 = 2.25F;
    public static final float LATERAL_OVERHANG_16 = 0.55F;
    public static final int MINIMUM_SUPPORT_SPAN_16 = 2;
    public static final int FULL_SCALE_SPAN_16 = 4;

    private PatchFoliagePlan() {}

    public static List<Card> plan(PatchFrame patch, long stableSeed) {
        int width = patch.bounds().uMax() - patch.bounds().uMin();
        int height = patch.bounds().vMax() - patch.bounds().vMin();
        int smallest = Math.min(width, height);
        if (smallest < MINIMUM_SUPPORT_SPAN_16) return List.of();
        float scale = Math.min(1.0F, smallest / (float) FULL_SCALE_SPAN_16);
        float overhang = Math.min(LATERAL_OVERHANG_16 * scale, smallest / 5.0F);
        float depth = OUTWARD_DEPTH_16 * scale;
        boolean flip = (mix(stableSeed) & 1L) != 0;
        float low = flip ? 0.20F : 0.28F;
        float high = 1.0F - low;
        Rect16 support = patch.bounds();

        // Two crossing strips share a support rectangle entirely inside the patch. Their visual
        // cards may lean/overhang slightly, but never acquire a full-block attachment region.
        return List.of(
                new Card(0, support, vertices(patch, -overhang, overhang, low, high,
                        flip ? depth : 0.15F, flip ? 0.15F : depth)),
                new Card(1, support, rotatedVertices(patch, low, high, -overhang, overhang,
                        flip ? 0.15F : depth, flip ? depth : 0.15F)));
    }

    private static Vertex[] vertices(PatchFrame patch, float uOverMin, float uOverMax,
            float vLow, float vHigh, float lowDepth, float highDepth) {
        float u0 = patch.bounds().uMin() + uOverMin;
        float u1 = patch.bounds().uMax() + uOverMax;
        float v0 = interpolate(patch.bounds().vMin(), patch.bounds().vMax(), vLow);
        float v1 = interpolate(patch.bounds().vMin(), patch.bounds().vMax(), vHigh);
        return new Vertex[] {new Vertex(u0, v0, lowDepth), new Vertex(u1, v0, highDepth),
                new Vertex(u1, v1, highDepth), new Vertex(u0, v1, lowDepth)};
    }

    private static Vertex[] rotatedVertices(PatchFrame patch, float uLow, float uHigh,
            float vOverMin, float vOverMax, float lowDepth, float highDepth) {
        float u0 = interpolate(patch.bounds().uMin(), patch.bounds().uMax(), uLow);
        float u1 = interpolate(patch.bounds().uMin(), patch.bounds().uMax(), uHigh);
        float v0 = patch.bounds().vMin() + vOverMin;
        float v1 = patch.bounds().vMax() + vOverMax;
        return new Vertex[] {new Vertex(u0, v0, lowDepth), new Vertex(u1, v0, lowDepth),
                new Vertex(u1, v1, highDepth), new Vertex(u0, v1, highDepth)};
    }

    private static float interpolate(int min, int max, float fraction) {
        return min + (max - min) * fraction;
    }
    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        return value;
    }

    public record Card(int ordinal, Rect16 support, Vertex[] vertices) {
        public Card { vertices = vertices.clone(); }
        @Override public Vertex[] vertices() { return vertices.clone(); }
    }
    public record Vertex(float u16, float v16, float outward16) {}
}
