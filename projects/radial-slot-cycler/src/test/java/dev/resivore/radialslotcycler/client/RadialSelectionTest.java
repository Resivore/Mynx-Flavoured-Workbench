package dev.resivore.radialslotcycler.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RadialSelectionTest {
    @Test
    void deadZoneCancelsAndItsBoundarySelects() {
        assertEquals(-1, RadialSelection.fromPointer(0.0D, 0.0D, 7, 20.0D));
        assertEquals(-1, RadialSelection.fromPointer(0.0D, -19.99D, 7, 20.0D));
        assertEquals(0, RadialSelection.fromPointer(0.0D, -20.0D, 7, 20.0D));
    }

    @Test
    void fourEntryVanillaWheelStartsAtTopAndRunsClockwise() {
        assertEquals(0, RadialSelection.fromPointer(0, -50, 4, 20));
        assertEquals(1, RadialSelection.fromPointer(50, 0, 4, 20));
        assertEquals(2, RadialSelection.fromPointer(0, 50, 4, 20));
        assertEquals(3, RadialSelection.fromPointer(-50, 0, 4, 20));
    }

    @Test
    void everyDynamicInventoryExtendedEntryIsReachable() {
        for (int index = 0; index < 7; index++) {
            double angle = RadialSelection.entryAngle(index, 7);
            assertEquals(index, RadialSelection.fromPointer(
                    Math.cos(angle) * 60.0D,
                    Math.sin(angle) * 60.0D,
                    7,
                    20.0D));
        }
    }

    @Test
    void malformedGeometryFailsClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> RadialSelection.fromPointer(1, 1, 0, 20));
        assertThrows(IllegalArgumentException.class,
                () -> RadialSelection.fromPointer(1, 1, 4, -1));
        assertThrows(IllegalArgumentException.class,
                () -> RadialSelection.entryAngle(7, 7));
    }
}
