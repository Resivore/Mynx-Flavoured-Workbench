package dev.resivore.mapmarkerextension.core;

import java.util.Objects;
import java.util.Set;

public record MapMarkerTarget(
    String dimensionId,
    String decorationTypeId,
    String decorationAssetId,
    int blockX,
    int blockZ,
    Set<Integer> sourceMapIds
) {
    public MapMarkerTarget {
        Objects.requireNonNull(dimensionId, "dimensionId");
        Objects.requireNonNull(decorationTypeId, "decorationTypeId");
        Objects.requireNonNull(decorationAssetId, "decorationAssetId");
        sourceMapIds = Set.copyOf(sourceMapIds);
    }

    public double renderX() {
        return blockX + 0.5D;
    }

    public double renderZ() {
        return blockZ + 0.5D;
    }
}
