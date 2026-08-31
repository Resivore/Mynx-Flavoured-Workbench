package games.twinhead.moreslabsstairsandwalls.api.material;

import java.util.Set;

public record DerivedGeometrySupport(Status status, Set<BehaviorCapability> missingCapabilities, boolean visualComplete) {
    public enum Status { SUPPORTED_GENERIC, SUPPORTED_WITH_CAPABILITIES, UNSUPPORTED_SOURCE,
        UNSUPPORTED_BEHAVIOR, UNSUPPORTED_VISUAL, UNSUPPORTED_BOTH }
    public enum Geometry { VERTICAL_SLAB, STEP, LAYER }

    public DerivedGeometrySupport {
        missingCapabilities = Set.copyOf(missingCapabilities);
    }

    public DerivedGeometrySupport(Status status, Set<BehaviorCapability> missingCapabilities) {
        this(status, missingCapabilities, status == Status.SUPPORTED_GENERIC || status == Status.SUPPORTED_WITH_CAPABILITIES);
    }

    public boolean supported() { return status == Status.SUPPORTED_GENERIC || status == Status.SUPPORTED_WITH_CAPABILITIES; }
}
