package dev.resivore.mapmarkerextension.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.resivore.mapmarkerextension.core.MapMarkerTarget;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class MapMarkerTargetRepositoryTest {
    @Test
    void disconnectClearRemovesAllEphemeralTargets() {
        MapMarkerTargetRepository repository = new MapMarkerTargetRepository();
        MapMarkerTarget target = new MapMarkerTarget(
            "minecraft:overworld",
            "map_marker_extension:woodland_mansion",
            "map_marker_extension:poi_icons/woodland_mansion",
            9,
            25,
            Set.of(4)
        );

        repository.replace(List.of(target));
        assertEquals(List.of(target), repository.snapshot());
        repository.clear();
        assertTrue(repository.snapshot().isEmpty());
    }
}
