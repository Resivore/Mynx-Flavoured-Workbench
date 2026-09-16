package dev.resivore.villagerwork;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure, finite-safe geometry matching Minecraft 26.2's fishing-line segmentation.
 * The renderer owns the vertex-format-specific submission.
 */
public final class FishingLineGeometry {
    public static final int SEGMENT_COUNT = 16;
    private static final float MIN_NORMAL_LENGTH_SQUARED = 1.0e-8f;
    /** The top surface of the 0.25-block square bobber sprite. */
    private static final float BOBBER_ATTACHMENT_HEIGHT = 0.125f;

    private FishingLineGeometry() {}

    public record Point(float x, float y, float z) {
        public boolean isFinite() {
            return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z);
        }
    }

    public record Vertex(Point position, Point normal) {}

    public record Segment(Vertex start, Vertex end) {}

    /**
     * Returns the 16 restrained, slightly sagging segments used by the current vanilla hook renderer.
     * A zero or non-finite endpoint deliberately emits no geometry rather than an invalid normal.
     */
    public static List<Segment> segments(float endX, float endY, float endZ) {
        if (!Float.isFinite(endX) || !Float.isFinite(endY) || !Float.isFinite(endZ)) return List.of();
        float endpointLengthSquared = endX * endX + endY * endY + endZ * endZ;
        if (!Float.isFinite(endpointLengthSquared) || endpointLengthSquared <= MIN_NORMAL_LENGTH_SQUARED)
            return List.of();

        List<Segment> result = new ArrayList<>(SEGMENT_COUNT);
        for (int index = 0; index < SEGMENT_COUNT; index++) {
            Point start = pointAt(endX, endY, endZ, fraction(index));
            Point end = pointAt(endX, endY, endZ, fraction(index + 1));
            float dx = end.x - start.x;
            float dy = end.y - start.y;
            float dz = end.z - start.z;
            float lengthSquared = dx * dx + dy * dy + dz * dz;
            if (!Float.isFinite(lengthSquared) || lengthSquared <= MIN_NORMAL_LENGTH_SQUARED) continue;

            float inverseLength = 1.0f / (float) Math.sqrt(lengthSquared);
            Point normal = new Point(dx * inverseLength, dy * inverseLength, dz * inverseLength);
            result.add(new Segment(new Vertex(start, normal), new Vertex(end,
                    new Point(-normal.x, -normal.y, -normal.z))));
        }
        return List.copyOf(result);
    }

    private static float fraction(int index) {
        return (float) index / SEGMENT_COUNT;
    }

    private static Point pointAt(float endX, float endY, float endZ, float fraction) {
        return new Point(endX * fraction,
                BOBBER_ATTACHMENT_HEIGHT + (endY - BOBBER_ATTACHMENT_HEIGHT)
                        * (fraction * fraction + fraction) * 0.5f,
                endZ * fraction);
    }
}
