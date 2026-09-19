package dev.resivore.bgeglassculling.geometry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class RectSubtractionTest {
    private static final Rect16 FULL = new Rect16(0, 16, 0, 16);

    @Test
    void completeOverlapEmitsNothing() {
        assertEquals(List.of(), RectSubtraction.subtract(FULL, List.of(FULL)));
    }

    @Test
    void halfOverlapKeepsOnlyTheExposedHalf() {
        assertEquals(List.of(new Rect16(0, 16, 8, 16)),
                RectSubtraction.subtract(FULL, List.of(new Rect16(0, 16, 0, 8))));
    }

    @Test
    void independentRegionsAreNotCollapsedIntoOneBoundingRectangle() {
        List<Rect16> visible = RectSubtraction.subtract(FULL, List.of(
                new Rect16(0, 4, 0, 4), new Rect16(12, 16, 12, 16)));
        assertEquals(224, area(visible));
        assertEquals(7, visible.size());
    }

    @Test
    void repeatedAndOverlappingContributionsProduceOneExactUnion() {
        List<Rect16> visible = RectSubtraction.subtract(FULL, List.of(
                new Rect16(0, 8, 0, 8), new Rect16(0, 8, 0, 8),
                new Rect16(4, 12, 4, 12)));
        assertEquals(144, area(visible));
        assertEquals(visible.stream().distinct().count(), visible.size());
    }

    @Test
    void outputOrderIsDeterministicAndContainsNoZeroAreaCell() {
        List<Rect16> first = RectSubtraction.subtract(FULL, List.of(
                new Rect16(4, 12, 4, 12), new Rect16(0, 4, 12, 16)));
        List<Rect16> second = RectSubtraction.subtract(FULL, List.of(
                new Rect16(0, 4, 12, 16), new Rect16(4, 12, 4, 12)));
        assertEquals(first, second);
        assertEquals(176, area(first));
        assertThrows(IllegalArgumentException.class, () -> new Rect16(4, 4, 0, 16));
    }

    private static int area(List<Rect16> rectangles) {
        return rectangles.stream().mapToInt(rectangle ->
                (rectangle.uMax() - rectangle.uMin())
                        * (rectangle.vMax() - rectangle.vMin())).sum();
    }
}
