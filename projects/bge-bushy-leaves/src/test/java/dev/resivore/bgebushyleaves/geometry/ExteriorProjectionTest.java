package dev.resivore.bgebushyleaves.geometry;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class ExteriorProjectionTest {
    private static final PatchFrame NORTH_HALF = new PatchFrame(Direction.NORTH, 0, Direction.Axis.X,
            new Rect16(0, 8, 0, 16), Direction.Axis.Y, Direction.NORTH);
    @Test void exteriorFoliageUsesOnlyThePatchFootprintAndPreservesOutwardDepth() {
        var source = new ExteriorProjection.Source(Direction.NORTH, -2, -2, 18, -2, 18);
        var plan = ExteriorProjection.plan(source, NORTH_HALF, List.of());
        assertEquals(1, plan.size()); assertEquals(new Rect16(0, 8, 0, 16), plan.getFirst().bounds());
        assertEquals(-2, plan.getFirst().plane16()); assertEquals(0, plan.getFirst().canonicalU(0));
        assertEquals(16, plan.getFirst().canonicalU(8));
    }
    @Test void partialContactSubtractsOnlyTheActualCoveredRegion() {
        var source = new ExteriorProjection.Source(Direction.NORTH, -1, 0, 16, 0, 16);
        var plan = ExteriorProjection.plan(source, NORTH_HALF, List.of(new Rect16(0, 4, 0, 16)));
        assertEquals(List.of(new Rect16(4, 8, 0, 16)), plan.stream().map(ExteriorProjection.Projection::bounds).toList());
    }
    @Test void interiorAndWrongCanonicalFaceQuadsFailClosed() {
        assertTrue(ExteriorProjection.plan(new ExteriorProjection.Source(Direction.NORTH, 0, 0, 16, 0, 16), NORTH_HALF, List.of()).isEmpty());
        assertTrue(ExteriorProjection.plan(new ExteriorProjection.Source(Direction.SOUTH, 17, 0, 16, 0, 16), NORTH_HALF, List.of()).isEmpty());
    }
}
