package dev.resivore.mapmarkerextension.core;

import java.util.Objects;

public record DecorationObservation(
    String typeId,
    String assetId,
    double worldX,
    double worldZ
) {
    public DecorationObservation {
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(assetId, "assetId");
    }
}
