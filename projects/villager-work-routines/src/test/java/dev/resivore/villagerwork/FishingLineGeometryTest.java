package dev.resivore.villagerwork;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingLineGeometryTest {
    @Test void finiteVillagerToFloatLineUsesEveryVanillaStyleSegment() {
        List<FishingLineGeometry.Segment> segments = FishingLineGeometry.segments(4.0f, 2.0f, -3.0f);

        assertEquals(FishingLineGeometry.SEGMENT_COUNT, segments.size());
        assertPoint(segments.getFirst().start().position(), 0.0f, 0.25f, 0.0f);
        assertPoint(segments.getLast().end().position(), 4.0f, 2.25f, -3.0f);
        assertFinite(segments);
    }

    @Test void zeroOrNonFiniteLinesDoNotProduceInvalidVertices() {
        assertTrue(FishingLineGeometry.segments(0.0f, 0.0f, 0.0f).isEmpty());
        assertTrue(FishingLineGeometry.segments(Float.NaN, 1.0f, 1.0f).isEmpty());
        assertTrue(FishingLineGeometry.segments(1.0f, Float.POSITIVE_INFINITY, 1.0f).isEmpty());
    }

    private static void assertFinite(List<FishingLineGeometry.Segment> segments) {
        for (FishingLineGeometry.Segment segment : segments) {
            assertTrue(segment.start().position().isFinite());
            assertTrue(segment.end().position().isFinite());
            assertTrue(segment.start().normal().isFinite());
            assertTrue(segment.end().normal().isFinite());
        }
    }

    private static void assertPoint(FishingLineGeometry.Point actual, float x, float y, float z) {
        assertEquals(x, actual.x(), 0.00001f);
        assertEquals(y, actual.y(), 0.00001f);
        assertEquals(z, actual.z(), 0.00001f);
    }
}
