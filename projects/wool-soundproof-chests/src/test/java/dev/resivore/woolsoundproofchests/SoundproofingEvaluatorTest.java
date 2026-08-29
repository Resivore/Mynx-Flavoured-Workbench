package dev.resivore.woolsoundproofchests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SoundproofingEvaluatorTest {
    private static final SoundproofingEvaluator.Seal SOLID = new SoundproofingEvaluator.Seal(true, false);
    private static final SoundproofingEvaluator.Seal WOOL = new SoundproofingEvaluator.Seal(true, true);

    @Test
    void floorAndThreeWoolSidesQualify() {
        assertTrue(qualifies(Map.of(
            SoundproofingEvaluator.Face.DOWN, SOLID,
            SoundproofingEvaluator.Face.NORTH, WOOL,
            SoundproofingEvaluator.Face.SOUTH, WOOL,
            SoundproofingEvaluator.Face.EAST, WOOL
        )));
    }

    @Test
    void floorWallAndTwoWoolSidesQualify() {
        assertTrue(qualifies(Map.of(
            SoundproofingEvaluator.Face.DOWN, SOLID,
            SoundproofingEvaluator.Face.NORTH, SOLID,
            SoundproofingEvaluator.Face.SOUTH, WOOL,
            SoundproofingEvaluator.Face.EAST, WOOL
        )));
    }

    @Test
    void fourSealedFacesWithOnlyOneWoolDoNotQualify() {
        assertFalse(qualifies(Map.of(
            SoundproofingEvaluator.Face.DOWN, SOLID,
            SoundproofingEvaluator.Face.NORTH, SOLID,
            SoundproofingEvaluator.Face.SOUTH, SOLID,
            SoundproofingEvaluator.Face.EAST, WOOL
        )));
    }

    @Test
    void threeSealedFacesWithThreeWoolDoNotQualify() {
        assertFalse(qualifies(Map.of(
            SoundproofingEvaluator.Face.NORTH, WOOL,
            SoundproofingEvaluator.Face.SOUTH, WOOL,
            SoundproofingEvaluator.Face.EAST, WOOL
        )));
    }

    @Test
    void fiveSealedFacesWithZeroWoolDoNotQualify() {
        EnumMap<SoundproofingEvaluator.Face, SoundproofingEvaluator.Seal> seals =
            new EnumMap<>(SoundproofingEvaluator.Face.class);
        for (SoundproofingEvaluator.Face face : SoundproofingEvaluator.Face.values()) {
            seals.put(face, SOLID);
        }
        assertFalse(qualifies(seals));
    }

    @Test
    void absentFacesRepresentNonFullBlocksAndDoNotCount() {
        assertFalse(qualifies(Map.of(
            SoundproofingEvaluator.Face.NORTH, WOOL,
            SoundproofingEvaluator.Face.SOUTH, WOOL,
            SoundproofingEvaluator.Face.DOWN, SOLID
        )));
    }

    private static boolean qualifies(Map<SoundproofingEvaluator.Face, SoundproofingEvaluator.Seal> seals) {
        return SoundproofingEvaluator.qualifies(face -> seals.getOrDefault(face, SoundproofingEvaluator.Seal.OPEN));
    }
}
