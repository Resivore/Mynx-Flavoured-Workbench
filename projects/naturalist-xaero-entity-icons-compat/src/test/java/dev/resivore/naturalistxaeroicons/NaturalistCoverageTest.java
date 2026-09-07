package dev.resivore.naturalistxaeroicons;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class NaturalistCoverageTest {
    private static final List<String> EXPECTED = List.of(
            "rhino", "lion", "elephant", "mammoth", "zebra", "giraffe", "hippo", "vulture", "boar", "dragonfly",
            "anglerfish", "ray", "blobfish", "piranha", "alligator", "bass", "lizard", "lizard_tail", "tortoise", "duck",
            "starfish", "clam", "giant_isopod", "jellyfish", "whale", "mole", "rat", "black_bear", "tiger", "komodo_dragon",
            "ostrich", "desert_scorpion", "jungle_scorpion", "great_white_shark", "turkey", "capybara", "hedgehog");

    @Test void exactTargetRosterIsClosedAndComplete() {
        assertEquals(EXPECTED.stream().sorted().toList(), NaturalistModelContracts.targetIds());
        assertEquals(37, NaturalistModelContracts.targetIds().size());
        assertFalse(NaturalistModelContracts.isTargetId("duck_egg"));
        assertFalse(NaturalistModelContracts.isTargetId("dirt_trail"));
    }

    @Test void knownWorkingControlsAreNotClaimed() {
        for (String id : List.of("bear", "bird", "butterfly", "catfish", "caterpillar", "crab", "deer", "firefly", "snake", "snail")) {
            assertTrue(NaturalistModelContracts.isNativeControlId(id));
            assertFalse(NaturalistModelContracts.isTargetId(id));
        }
    }
}
