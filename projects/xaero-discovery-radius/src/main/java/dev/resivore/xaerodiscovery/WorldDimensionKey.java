package dev.resivore.xaerodiscovery;

import java.nio.file.Path;
import java.util.Objects;

record WorldDimensionKey(Path dimensionDirectory, String dimensionId) {
    WorldDimensionKey {
        dimensionDirectory = dimensionDirectory.toAbsolutePath().normalize();
        dimensionId = Objects.requireNonNull(dimensionId, "dimensionId");
    }
}
