package dev.resivore.mapmarkerextension.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MapObservation(
    int mapId,
    boolean filledMap,
    String dimensionId,
    Optional<MapMarkerIdentity> identity,
    List<DecorationObservation> decorations
) {
    public MapObservation {
        Objects.requireNonNull(dimensionId, "dimensionId");
        Objects.requireNonNull(identity, "identity");
        decorations = List.copyOf(decorations);
    }
}
