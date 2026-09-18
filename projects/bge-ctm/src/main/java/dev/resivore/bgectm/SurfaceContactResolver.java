package dev.resivore.bgectm;

import dev.aero.cnmterraincompat.BgeMaterialBindings;
import dev.aero.cnmterraincompat.BgeMaterialBindings.Binding;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.PlaneRelation;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfaceModel;
import dev.aero.cnmterraincompat.BgeSurfaceGeometry.SurfacePatch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Geometry vetoes backed exclusively by each BGE canonical binding's surface contract. */
public final class SurfaceContactResolver {
    private static final int BLOCK_UNITS = 16;
    private static final SurfaceModel UNMANAGED_FULL = unmanagedFullModel();

    private SurfaceContactResolver() {}

    /** Regular CTM: corresponding faces, compatible world planes, and edge contact are required. */
    public static Decision inspect(BlockState source, BlockPos sourcePos, BlockState other,
            BlockPos otherPos, Direction face) {
        return inspectSurfaces(source, sourcePos, other, otherPos, face, null);
    }

    public static Decision inspectWithSourceSurface(BlockState source, BlockPos sourcePos,
            BlockState other, BlockPos otherPos, Direction face, QuadSurface quad) {
        return inspectSurfaces(source, sourcePos, other, otherPos, face,
                Objects.requireNonNull(quad, "quad"));
    }

    /** Standard Overlay uses the same surface-plane compatibility model as ordinary CTM. */
    public static Decision inspectOverlay(BlockState receiver, BlockPos receiverPos,
            BlockState source, BlockPos sourcePos, Direction face, QuadSurface receiverQuad) {
        return inspectSurfaces(receiver, receiverPos, source, sourcePos, face, receiverQuad);
    }

    public static Decision inspectOverlay(BlockState receiver, BlockPos receiverPos,
            BlockState source, BlockPos sourcePos, Direction face) {
        return inspectOverlay(receiver, receiverPos, source, sourcePos, face, null);
    }

    /** State-only fallback is safe only when every face has at most one exposed patch. */
    public static boolean stateDerivedFallbackSafe(BlockState source, BlockState other) {
        Endpoint sourceEndpoint = endpoint(source);
        Endpoint otherEndpoint = endpoint(other);
        return sourceEndpoint.supported() && otherEndpoint.supported()
                && sourceEndpoint.singlePatchPerFace() && otherEndpoint.singlePatchPerFace();
    }

    /** Compatibility helper retained for callers that require one unambiguous face patch. */
    public static Optional<SurfaceDescriptor> describe(BlockState state, BlockPos pos,
            Direction face) {
        List<SurfaceDescriptor> patches = descriptors(endpoint(state), pos, face);
        return patches.size() == 1 ? Optional.of(patches.getFirst()) : Optional.empty();
    }

    /** Every BGE-owned surface patch for a face, in the provider's deterministic order. */
    public static List<SurfaceDescriptor> describeAll(BlockState state, BlockPos pos,
            Direction face) {
        return descriptors(endpoint(state), pos, face);
    }

    /** State-only overlay emission is allowed only for one exact receiver patch. */
    public static Optional<QuadSurface> describeLocalForOverlay(BlockState state, Direction face) {
        Endpoint endpoint = endpoint(state);
        if (!endpoint.supported()) return Optional.empty();
        List<SurfacePatch> patches = endpoint.model().patches(face);
        if (patches.size() != 1) return Optional.empty();
        return Optional.of(quad(patches.getFirst()));
    }

    /**
     * Resolves one rendered quad back to the single authoritative BGE patch that contains it.
     * This is deliberately containment-based: a quad that spans independent compound patches is
     * ambiguous and must not flatten them into one semantic surface.
     */
    public static Optional<SurfaceMatch> matchRenderedSurface(BlockState state, QuadSurface rendered) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(rendered, "rendered");
        Endpoint endpoint = endpoint(state);
        if (!endpoint.supported()) return Optional.empty();
        SurfacePatch match = null;
        for (SurfacePatch patch : endpoint.model().patches(rendered.normal())) {
            if (patch.plane16() != rendered.plane16()
                    || patch.uAxis() != rendered.uAxis() || patch.vAxis() != rendered.vAxis()
                    || patch.uMin16() > rendered.uMin16() || patch.uMax16() < rendered.uMax16()
                    || patch.vMin16() > rendered.vMin16() || patch.vMax16() < rendered.vMax16()) {
                continue;
            }
            if (match != null) return Optional.empty();
            match = patch;
        }
        if (match == null) return Optional.empty();

        int presentationPlane = match.plane16();
        if (match.planeRelation() == PlaneRelation.TERRAIN_HEIGHT_INSET) {
            presentationPlane += match.normal().getAxisDirection()
                    == Direction.AxisDirection.POSITIVE ? 1 : -1;
        }
        QuadSurface presentation = new QuadSurface(rendered.normal(), presentationPlane,
                rendered.uAxis(), rendered.uMin16(), rendered.uMax16(), rendered.vAxis(),
                rendered.vMin16(), rendered.vMax16());
        return Optional.of(new SurfaceMatch(rendered, presentation, match.canonicalFace(),
                match.planeRelation()));
    }

    private static Decision inspectSurfaces(BlockState source, BlockPos sourcePos,
            BlockState other, BlockPos otherPos, Direction face, QuadSurface sourceQuad) {
        Endpoint sourceEndpoint = endpoint(source);
        Endpoint otherEndpoint = endpoint(other);
        if (!sourceEndpoint.participates() && !otherEndpoint.participates()) {
            return Decision.BYPASS_UNRELATED;
        }
        if (!sourceEndpoint.supported() || !otherEndpoint.supported()) {
            return Decision.UNSUPPORTED_GEOMETRY;
        }
        if (sourceQuad != null && sourceQuad.normal() != face) {
            return Decision.INVALID_QUAD_SURFACE;
        }

        List<SurfaceDescriptor> sourceSurfaces = sourceQuad == null
                ? descriptors(sourceEndpoint, sourcePos, face)
                : descriptors(sourceEndpoint, sourcePos, sourceQuad);
        if (sourceSurfaces.isEmpty()) {
            return sourceQuad == null ? Decision.NO_BOUNDARY_CONTACT : Decision.INVALID_QUAD_SURFACE;
        }
        List<SurfaceDescriptor> otherSurfaces = descriptors(otherEndpoint, otherPos, face);
        if (otherSurfaces.isEmpty()) return Decision.NO_BOUNDARY_CONTACT;

        boolean compatiblePlane = false;
        for (SurfaceDescriptor sourceSurface : sourceSurfaces) {
            for (SurfaceDescriptor otherSurface : otherSurfaces) {
                if (!planesCompatible(sourceSurface, otherSurface)) continue;
                compatiblePlane = true;
                if (meetAlongEvaluatedBoundary(sourceSurface, otherSurface)) {
                    return Decision.CONNECT;
                }
            }
        }
        return compatiblePlane ? Decision.NO_BOUNDARY_CONTACT : Decision.NON_COPLANAR;
    }

    /** Face and typed-plane compatibility, kept separate from in-plane contact. */
    public static boolean planesCompatible(SurfaceDescriptor source, SurfaceDescriptor other) {
        if (source.normal() != other.normal()
                || source.canonicalFace() != other.canonicalFace()
                || source.uAxis() != other.uAxis() || source.vAxis() != other.vAxis()) {
            return false;
        }
        if (source.plane16() == other.plane16()) return true;
        if (source.planeRelation() == other.planeRelation()) return false;

        SurfaceDescriptor inset = source.planeRelation() == PlaneRelation.TERRAIN_HEIGHT_INSET
                ? source : other;
        SurfaceDescriptor exact = inset == source ? other : source;
        if (exact.planeRelation() != PlaneRelation.EXACT) return false;
        int outward = inset.normal().getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
        return exact.plane16() == inset.plane16() + outward;
    }

    /** In-plane edge/overlap test after compatible planes have been established. */
    public static boolean meetAlongEvaluatedBoundary(SurfaceDescriptor source,
            SurfaceDescriptor other) {
        if (!planesCompatible(source, other)) return false;
        int normal = coordinate(other.blockPos(), source.normal().getAxis())
                - coordinate(source.blockPos(), source.normal().getAxis());
        int u = coordinate(other.blockPos(), source.uAxis())
                - coordinate(source.blockPos(), source.uAxis());
        int v = coordinate(other.blockPos(), source.vAxis())
                - coordinate(source.blockPos(), source.vAxis());
        if (normal != 0 || Math.abs(u) > 1 || Math.abs(v) > 1 || (u == 0 && v == 0)) {
            return false;
        }
        return meets(source.uBounds(), other.uBounds(), u)
                && meets(source.vBounds(), other.vBounds(), v);
    }

    private static List<SurfaceDescriptor> descriptors(Endpoint endpoint, BlockPos pos,
            Direction face) {
        if (!endpoint.supported()) return List.of();
        return endpoint.model().patches(face).stream()
                .map(patch -> descriptor(patch, pos)).toList();
    }

    private static List<SurfaceDescriptor> descriptors(Endpoint endpoint, BlockPos pos,
            QuadSurface quad) {
        if (!endpoint.supported()) return List.of();
        List<SurfaceDescriptor> result = new ArrayList<>();
        for (SurfacePatch patch : endpoint.model().patches(quad.normal())) {
            if (patch.plane16() != quad.plane16()
                    || patch.uAxis() != quad.uAxis() || patch.vAxis() != quad.vAxis()
                    || !overlaps(patch.uMin16(), patch.uMax16(), quad.uMin16(), quad.uMax16())
                    || !overlaps(patch.vMin16(), patch.vMax16(), quad.vMin16(), quad.vMax16())) {
                continue;
            }
            int uMin = Math.max(patch.uMin16(), quad.uMin16());
            int uMax = Math.min(patch.uMax16(), quad.uMax16());
            int vMin = Math.max(patch.vMin16(), quad.vMin16());
            int vMax = Math.min(patch.vMax16(), quad.vMax16());
            result.add(descriptor(new SurfacePatch(patch.normal(), patch.plane16(),
                    patch.uAxis(), uMin, uMax, patch.vAxis(), vMin, vMax,
                    patch.canonicalFace(), patch.planeRelation()), pos));
        }
        return List.copyOf(result);
    }

    private static SurfaceDescriptor descriptor(SurfacePatch patch, BlockPos pos) {
        Direction.Axis normal = patch.normal().getAxis();
        long plane = (long) coordinate(pos, normal) * BLOCK_UNITS + patch.plane16();
        return new SurfaceDescriptor(patch.normal(), plane, patch.uAxis(),
                world(pos, patch.uAxis(), patch.uMin16(), patch.uMax16()),
                patch.vAxis(), world(pos, patch.vAxis(), patch.vMin16(), patch.vMax16()),
                pos.immutable(), patch.canonicalFace(), patch.planeRelation());
    }

    private static QuadSurface quad(SurfacePatch patch) {
        return new QuadSurface(patch.normal(), patch.plane16(), patch.uAxis(),
                patch.uMin16(), patch.uMax16(), patch.vAxis(), patch.vMin16(), patch.vMax16());
    }

    private static Interval world(BlockPos pos, Direction.Axis axis, int min, int max) {
        long origin = (long) coordinate(pos, axis) * BLOCK_UNITS;
        return new Interval(origin + min, origin + max);
    }

    private static boolean overlaps(long a0, long a1, long b0, long b1) {
        return Math.min(a1, b1) > Math.max(a0, b0);
    }

    private static boolean meets(Interval a, Interval b, int delta) {
        return switch (Integer.signum(delta)) {
            case -1 -> a.min16() == b.max16();
            case 0 -> overlaps(a.min16(), a.max16(), b.min16(), b.max16());
            case 1 -> a.max16() == b.min16();
            default -> false;
        };
    }

    private static Endpoint endpoint(BlockState state) {
        Optional<Binding> binding = BgeMaterialBindings.fromBlock(state.getBlock());
        if (binding.isEmpty()) return new Endpoint(UNMANAGED_FULL, false);
        Binding value = binding.get();
        boolean participates = value.topology() != BgeMaterialBindings.Topology.CANONICAL_ROOT;
        if (participates && !CanonicalAppearanceResolver.inspect(state).inherited()) {
            return Endpoint.unsupported();
        }
        return new Endpoint(value.surfaceModel(state), participates);
    }

    private static SurfaceModel unmanagedFullModel() {
        List<SurfacePatch> patches = new ArrayList<>();
        for (Direction face : Direction.values()) {
            Direction.Axis[] axes = inPlaneAxes(face.getAxis());
            int plane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 16 : 0;
            patches.add(new SurfacePatch(face, plane, axes[0], 0, 16, axes[1], 0, 16,
                    face, PlaneRelation.EXACT));
        }
        return new SurfaceModel(patches, Optional.empty());
    }

    private static Direction.Axis[] inPlaneAxes(Direction.Axis normal) {
        return switch (normal) {
            case X -> new Direction.Axis[] {Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
    }

    private static int coordinate(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    private record Endpoint(SurfaceModel model, boolean participates) {
        private Endpoint {
            Objects.requireNonNull(model, "model");
        }

        private static Endpoint unsupported() {
            return new Endpoint(SurfaceModel.unsupported(
                    "Canonical appearance policy rejected this bound surface."), true);
        }

        private boolean supported() {
            return model.supported();
        }

        private boolean singlePatchPerFace() {
            if (!supported()) return false;
            for (Direction face : Direction.values()) {
                if (model.patches(face).size() > 1) return false;
            }
            return true;
        }
    }

    public enum Decision {
        BYPASS_UNRELATED(true),
        CONNECT(true),
        UNSUPPORTED_GEOMETRY(false),
        INVALID_QUAD_SURFACE(false),
        NON_COPLANAR(false),
        NO_BOUNDARY_CONTACT(false);

        private final boolean allow;

        Decision(boolean allow) {
            this.allow = allow;
        }

        public boolean allowsOriginal() {
            return allow;
        }
    }

    public record Interval(long min16, long max16) {
        public Interval {
            if (min16 >= max16) throw new IllegalArgumentException("Empty interval");
        }
    }

    public record SurfaceDescriptor(Direction normal, long plane16, Direction.Axis uAxis,
            Interval uBounds, Direction.Axis vAxis, Interval vBounds, BlockPos blockPos,
            Direction canonicalFace, PlaneRelation planeRelation) {
        public SurfaceDescriptor {
            Objects.requireNonNull(normal, "normal");
            Objects.requireNonNull(uAxis, "uAxis");
            Objects.requireNonNull(uBounds, "uBounds");
            Objects.requireNonNull(vAxis, "vAxis");
            Objects.requireNonNull(vBounds, "vBounds");
            blockPos = Objects.requireNonNull(blockPos, "blockPos").immutable();
            Objects.requireNonNull(canonicalFace, "canonicalFace");
            Objects.requireNonNull(planeRelation, "planeRelation");
        }
    }

    public record QuadSurface(Direction normal, int plane16, Direction.Axis uAxis,
            int uMin16, int uMax16, Direction.Axis vAxis, int vMin16, int vMax16) {
        public QuadSurface {
            if (normal == null || uAxis == null || vAxis == null
                    || normal.getAxis() == uAxis || normal.getAxis() == vAxis || uAxis == vAxis
                    || plane16 < 0 || plane16 > 16 || uMin16 < 0 || uMax16 > 16
                    || uMin16 >= uMax16 || vMin16 < 0 || vMax16 > 16
                    || vMin16 >= vMax16) {
                throw new IllegalArgumentException("Invalid block-local quad surface");
            }
        }
    }

    /** Physical contact data plus the nominal plane on which Continuity presents an overlay. */
    public record SurfaceMatch(QuadSurface physical, QuadSurface presentation,
            Direction canonicalFace, PlaneRelation planeRelation) {
        public SurfaceMatch {
            Objects.requireNonNull(physical, "physical");
            Objects.requireNonNull(presentation, "presentation");
            Objects.requireNonNull(canonicalFace, "canonicalFace");
            Objects.requireNonNull(planeRelation, "planeRelation");
        }
    }
}
