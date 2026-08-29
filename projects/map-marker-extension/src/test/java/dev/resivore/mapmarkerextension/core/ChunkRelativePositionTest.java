package dev.resivore.mapmarkerextension.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

final class ChunkRelativePositionTest {
    @ParameterizedTest
    @CsvSource({
        "0, 0",
        "9, 9",
        "15, 15",
        "16, 0",
        "-1, 15",
        "-7, 9",
        "-16, 0",
        "-17, 15"
    })
    void usesFloorModForAllRequiredCoordinates(int blockCoordinate, int expected) {
        assertEquals(expected, ChunkRelativePosition.relative(blockCoordinate));
    }
}
