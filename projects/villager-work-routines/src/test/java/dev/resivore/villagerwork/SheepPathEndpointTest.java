package dev.resivore.villagerwork;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SheepPathEndpointTest {
    private static final BlockPos REQUESTED = new BlockPos(10, 64, -20);

    @Test void exactAndAdjacentEndpointsAreNear() {
        assertTrue(SheepPathEndpoint.nearRequested(REQUESTED, REQUESTED));
        assertTrue(SheepPathEndpoint.nearRequested(REQUESTED, REQUESTED.offset(1, 0, 0)));
        assertTrue(SheepPathEndpoint.nearRequested(REQUESTED, REQUESTED.offset(-1, 0, 0)));
        assertTrue(SheepPathEndpoint.nearRequested(REQUESTED, REQUESTED.offset(0, 0, 1)));
        assertTrue(SheepPathEndpoint.nearRequested(REQUESTED, REQUESTED.offset(0, 1, 0)));
        assertTrue(SheepPathEndpoint.nearRequested(REQUESTED, REQUESTED.offset(0, -1, 0)));
    }

    @Test void diagonalAndOneBlockStepEndpointsAreNear() {
        assertTrue(SheepPathEndpoint.nearRequested(REQUESTED, REQUESTED.offset(1, 0, -1)));
        assertTrue(SheepPathEndpoint.nearRequested(REQUESTED, REQUESTED.offset(-1, 1, 0)));
        assertTrue(SheepPathEndpoint.nearRequested(REQUESTED, REQUESTED.offset(0, -1, 1)));
    }

    @Test void distantAndNullEndpointsAreRejected() {
        assertFalse(SheepPathEndpoint.nearRequested(REQUESTED, REQUESTED.offset(2, 0, 0)));
        assertFalse(SheepPathEndpoint.nearRequested(REQUESTED, REQUESTED.offset(0, 0, -2)));
        assertFalse(SheepPathEndpoint.nearRequested(REQUESTED, REQUESTED.offset(0, 2, 0)));
        assertFalse(SheepPathEndpoint.nearRequested(REQUESTED, REQUESTED.offset(1, 1, 1)));
        assertFalse(SheepPathEndpoint.nearRequested(REQUESTED, null));
        assertFalse(SheepPathEndpoint.nearRequested(null, REQUESTED));
    }
}
