package dev.resivore.mapmarkerextension.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record MapObservation(
    int mapId,
    boolean filledMap,
    String dimensionId,
    Optional<MapMarkerIdentity> identity,
    Optional<ExternalNativeMapIdentity> externalNativeIdentity,
    List<DecorationObservation> decorations
) {
    public MapObservation {
        Objects.requireNonNull(dimensionId, "dimensionId");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(externalNativeIdentity, "externalNativeIdentity");
        decorations = List.copyOf(decorations);
    }

    public MapObservation(
        int mapId,
        boolean filledMap,
        String dimensionId,
        Optional<MapMarkerIdentity> identity,
        List<DecorationObservation> decorations
    ) {
        this(mapId, filledMap, dimensionId, identity, Optional.empty(), decorations);
    }
}
