package dev.resivore.dragonbound.client;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DirectionalMaterialResolverTest {
    @Test
    void stoneResolvesTheSameCanonicalExteriorMaterialOnEveryFace() {
        Optional<EnumMap<Direction, String>> resolved = DirectionalMaterialResolver.resolve(
                faces("stone"), material -> true);

        assertTrue(resolved.isPresent());
        for (Direction direction : Direction.values()) {
            assertEquals("stone", resolved.get().get(direction));
        }
    }

    @Test
    void oakLogKeepsEndGrainOnUpAndDownAndBarkOnItsHorizontalExteriorFaces() {
        EnumMap<Direction, List<String>> faces = faces("oak_log_side");
        faces.put(Direction.UP, List.of("oak_log_top"));
        faces.put(Direction.DOWN, List.of("oak_log_top"));

        Optional<EnumMap<Direction, String>> resolved = DirectionalMaterialResolver.resolve(faces, material -> true);

        assertTrue(resolved.isPresent());
        assertEquals("oak_log_top", resolved.get().get(Direction.UP));
        assertEquals("oak_log_top", resolved.get().get(Direction.DOWN));
        for (Direction direction : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            assertEquals("oak_log_side", resolved.get().get(direction));
        }
    }

    @Test
    void northFacingDefaultFurnaceKeepsItsFrontOnNorthAndTheCanonicalSideAndTopElsewhere() {
        EnumMap<Direction, List<String>> faces = faces("furnace_side");
        faces.put(Direction.NORTH, List.of("furnace_front"));
        faces.put(Direction.UP, List.of("furnace_top"));
        faces.put(Direction.DOWN, List.of("furnace_top"));

        Optional<EnumMap<Direction, String>> resolved = DirectionalMaterialResolver.resolve(faces, material -> true);

        assertTrue(resolved.isPresent());
        assertEquals("furnace_front", resolved.get().get(Direction.NORTH));
        assertEquals("furnace_side", resolved.get().get(Direction.SOUTH));
        assertEquals("furnace_side", resolved.get().get(Direction.EAST));
        assertEquals("furnace_side", resolved.get().get(Direction.WEST));
        assertEquals("furnace_top", resolved.get().get(Direction.UP));
        assertEquals("furnace_top", resolved.get().get(Direction.DOWN));
    }

    @Test
    void ambiguousOrUnsafeCullSpecificFacesFailClosedInsteadOfUsingTheFirstQuad() {
        EnumMap<Direction, List<String>> ambiguous = faces("side");
        ambiguous.put(Direction.NORTH, List.of("interior_overlay", "side"));
        assertTrue(DirectionalMaterialResolver.resolve(ambiguous, material -> true).isEmpty());

        EnumMap<Direction, List<String>> unsafe = faces("safe");
        unsafe.put(Direction.UP, List.of("tinted"));
        assertTrue(DirectionalMaterialResolver.resolve(unsafe, material -> !material.equals("tinted")).isEmpty());
    }

    private static EnumMap<Direction, List<String>> faces(String material) {
        EnumMap<Direction, List<String>> faces = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            faces.put(direction, List.of(material));
        }
        return faces;
    }
}
