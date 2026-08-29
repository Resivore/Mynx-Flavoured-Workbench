package dev.resivore.mapmarkerextension.client;

import dev.resivore.mapmarkerextension.core.MapMarkerTarget;
import java.util.List;

public final class MapMarkerTargetRepository {
    private volatile List<MapMarkerTarget> snapshot = List.of();

    public List<MapMarkerTarget> snapshot() {
        return snapshot;
    }

    public void replace(List<MapMarkerTarget> targets) {
        snapshot = List.copyOf(targets);
    }

    public void clear() {
        snapshot = List.of();
    }
}
